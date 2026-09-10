//! Native Solana policy boundary for Allowance OS.
//!
//! The program stores an allowance, rejects budget violations, freezes an
//! allowance when submitted evidence does not match the committed evidence,
//! and lets the authority revoke it. A VERIFIED charge transfers the configured
//! SPL token through CPI only after every enforced policy and evidence check.

use borsh::{BorshDeserialize, BorshSerialize};
use solana_program::{
    account_info::{next_account_info, AccountInfo},
    clock::Clock,
    entrypoint,
    entrypoint::ProgramResult,
    msg,
    program::{invoke, invoke_signed},
    program_error::ProgramError,
    program_pack::Pack,
    pubkey::Pubkey,
    sysvar::Sysvar,
};
use spl_token::state::Account as TokenAccount;

// Devnet program id derived from the local deployment keypair. The matching
// explorer address and deployment transaction are recorded after deployment.
solana_program::declare_id!("DJzPBS7FreCcWWGkApzznGcKq9T7Da38GpKFtpxWRcuE");

pub const STATE_SIZE: usize = 1 + (32 * 4) + (8 * 3) + 8 + 1 + 1 + (32 * 3);
pub const DELEGATED_STATE_SIZE: usize = 332;
pub const DELEGATE_SEED: &[u8] = b"allowance-delegate";

#[derive(Debug, borsh_derive::BorshSerialize, borsh_derive::BorshDeserialize, PartialEq, Eq)]
pub struct AllowanceState {
    pub version: u8,
    pub authority: Pubkey,
    pub merchant: Pubkey,
    pub token_mint: Pubkey,
    pub allowed_program: Pubkey,
    pub per_charge: u64,
    pub period_cap: u64,
    pub spent_in_period: u64,
    pub expires_at: i64,
    pub revoked: bool,
    pub frozen: bool,
    pub policy_hash: [u8; 32],
    pub required_evidence_hash: [u8; 32],
    pub last_evidence_hash: [u8; 32],
}

/// Version 2 allowance state for approve-once, policy-bounded settlement.
///
/// The user's SPL token account delegates `lifetime_cap` tokens to a PDA that
/// only this Program can sign for. Later charges require the configured
/// executor and independent verifier, but do not require the user authority.
#[derive(
    Debug, borsh_derive::BorshSerialize, borsh_derive::BorshDeserialize, PartialEq, Eq, Clone,
)]
pub struct AllowanceStateV2 {
    pub version: u8,
    pub authority: Pubkey,
    pub merchant: Pubkey,
    pub executor: Pubkey,
    pub verifier: Pubkey,
    pub token_mint: Pubkey,
    pub source_token: Pubkey,
    pub per_charge: u64,
    pub period_cap: u64,
    pub lifetime_cap: u64,
    pub spent_in_period: u64,
    pub spent_lifetime: u64,
    pub period_started_at: i64,
    pub period_seconds: i64,
    pub expires_at: i64,
    pub next_nonce: u64,
    pub paused: bool,
    pub revoked: bool,
    pub frozen: bool,
    pub policy_hash: [u8; 32],
    pub last_evidence_hash: [u8; 32],
}

#[derive(Debug, borsh_derive::BorshSerialize, borsh_derive::BorshDeserialize, PartialEq, Eq)]
pub enum AllowanceInstruction {
    Create {
        merchant: Pubkey,
        token_mint: Pubkey,
        allowed_program: Pubkey,
        per_charge: u64,
        period_cap: u64,
        expires_at: i64,
        policy_hash: [u8; 32],
        required_evidence_hash: [u8; 32],
    },
    Charge {
        amount: u64,
        evidence_hash: [u8; 32],
    },
    Revoke,
    CreateDelegated {
        merchant: Pubkey,
        executor: Pubkey,
        verifier: Pubkey,
        token_mint: Pubkey,
        source_token: Pubkey,
        per_charge: u64,
        period_cap: u64,
        lifetime_cap: u64,
        period_seconds: i64,
        expires_at: i64,
        policy_hash: [u8; 32],
    },
    ChargeDelegated {
        amount: u64,
        nonce: u64,
        evidence_hash: [u8; 32],
    },
    PauseDelegated,
    UnpauseDelegated,
    FreezeDelegated {
        evidence_hash: [u8; 32],
    },
    UnfreezeDelegated,
    RevokeDelegated,
}

#[derive(Debug, PartialEq, Eq)]
pub enum AllowanceError {
    AlreadyInitialized,
    InvalidAuthority,
    InvalidPolicy,
    Expired,
    Revoked,
    PerChargeLimitExceeded,
    PeriodCapExceeded,
    InvalidAccountOwner,
    EvidenceMissing,
    Frozen,
    InvalidTokenProgram,
    InvalidTokenAccount,
    InvalidMerchantAccount,
    DuplicateEvidence,
    InvalidExecutor,
    InvalidVerifier,
    InvalidNonce,
    Paused,
    LifetimeCapExceeded,
    InvalidDelegatePda,
    InvalidSourceToken,
    InvalidStateVersion,
}

impl From<AllowanceError> for ProgramError {
    fn from(error: AllowanceError) -> Self {
        ProgramError::Custom(match error {
            AllowanceError::AlreadyInitialized => 1,
            AllowanceError::InvalidAuthority => 2,
            AllowanceError::InvalidPolicy => 3,
            AllowanceError::Expired => 4,
            AllowanceError::Revoked => 5,
            AllowanceError::PerChargeLimitExceeded => 6,
            AllowanceError::PeriodCapExceeded => 7,
            AllowanceError::InvalidAccountOwner => 8,
            AllowanceError::EvidenceMissing => 9,
            AllowanceError::Frozen => 10,
            AllowanceError::InvalidTokenProgram => 11,
            AllowanceError::InvalidTokenAccount => 12,
            AllowanceError::InvalidMerchantAccount => 13,
            AllowanceError::DuplicateEvidence => 14,
            AllowanceError::InvalidExecutor => 15,
            AllowanceError::InvalidVerifier => 16,
            AllowanceError::InvalidNonce => 17,
            AllowanceError::Paused => 18,
            AllowanceError::LifetimeCapExceeded => 19,
            AllowanceError::InvalidDelegatePda => 20,
            AllowanceError::InvalidSourceToken => 21,
            AllowanceError::InvalidStateVersion => 22,
        })
    }
}

#[derive(Debug, PartialEq, Eq)]
enum ChargeOutcome {
    Accepted { spent_after: u64 },
    FreezeEvidenceMismatch,
}

#[derive(Debug, PartialEq, Eq)]
struct DelegatedChargeOutcome {
    spent_in_period: u64,
    spent_lifetime: u64,
    period_started_at: i64,
    next_nonce: u64,
}

fn evaluate_delegated_charge(
    state: &AllowanceStateV2,
    now: i64,
    amount: u64,
    nonce: u64,
    evidence_hash: [u8; 32],
) -> Result<DelegatedChargeOutcome, AllowanceError> {
    if state.revoked {
        return Err(AllowanceError::Revoked);
    }
    if state.frozen {
        return Err(AllowanceError::Frozen);
    }
    if state.paused {
        return Err(AllowanceError::Paused);
    }
    if state.expires_at <= now {
        return Err(AllowanceError::Expired);
    }
    if nonce != state.next_nonce {
        return Err(AllowanceError::InvalidNonce);
    }
    if amount == 0 || amount > state.per_charge {
        return Err(AllowanceError::PerChargeLimitExceeded);
    }
    if evidence_hash == [0; 32] {
        return Err(AllowanceError::EvidenceMissing);
    }
    if state.last_evidence_hash != [0; 32] && evidence_hash == state.last_evidence_hash {
        return Err(AllowanceError::DuplicateEvidence);
    }

    let (period_started_at, spent_before) = if now
        >= state
            .period_started_at
            .checked_add(state.period_seconds)
            .ok_or(AllowanceError::InvalidPolicy)?
    {
        (now, 0)
    } else {
        (state.period_started_at, state.spent_in_period)
    };
    let spent_in_period = spent_before
        .checked_add(amount)
        .ok_or(AllowanceError::PeriodCapExceeded)?;
    if spent_in_period > state.period_cap {
        return Err(AllowanceError::PeriodCapExceeded);
    }
    let spent_lifetime = state
        .spent_lifetime
        .checked_add(amount)
        .ok_or(AllowanceError::LifetimeCapExceeded)?;
    if spent_lifetime > state.lifetime_cap {
        return Err(AllowanceError::LifetimeCapExceeded);
    }

    Ok(DelegatedChargeOutcome {
        spent_in_period,
        spent_lifetime,
        period_started_at,
        next_nonce: state
            .next_nonce
            .checked_add(1)
            .ok_or(AllowanceError::InvalidNonce)?,
    })
}

fn validate_delegated_actors(
    state: &AllowanceStateV2,
    executor: &Pubkey,
    executor_signed: bool,
    verifier: &Pubkey,
    verifier_signed: bool,
) -> Result<(), AllowanceError> {
    if !executor_signed || state.executor != *executor {
        return Err(AllowanceError::InvalidExecutor);
    }
    if !verifier_signed || state.verifier != *verifier {
        return Err(AllowanceError::InvalidVerifier);
    }
    Ok(())
}

fn validate_freeze_evidence(
    state: &AllowanceStateV2,
    evidence_hash: [u8; 32],
) -> Result<(), AllowanceError> {
    if state.revoked {
        return Err(AllowanceError::Revoked);
    }
    if state.frozen {
        return Err(AllowanceError::Frozen);
    }
    if evidence_hash == [0; 32] {
        return Err(AllowanceError::EvidenceMissing);
    }
    if state.last_evidence_hash != [0; 32] && evidence_hash == state.last_evidence_hash {
        return Err(AllowanceError::DuplicateEvidence);
    }
    Ok(())
}

fn evaluate_charge(
    state: &AllowanceState,
    now: i64,
    amount: u64,
    evidence_hash: [u8; 32],
) -> Result<ChargeOutcome, AllowanceError> {
    if state.revoked {
        return Err(AllowanceError::Revoked);
    }
    if state.frozen {
        return Err(AllowanceError::Frozen);
    }
    if state.expires_at <= now {
        return Err(AllowanceError::Expired);
    }
    if amount == 0 || amount > state.per_charge {
        return Err(AllowanceError::PerChargeLimitExceeded);
    }
    let spent_after = state
        .spent_in_period
        .checked_add(amount)
        .ok_or(AllowanceError::PeriodCapExceeded)?;
    if spent_after > state.period_cap {
        return Err(AllowanceError::PeriodCapExceeded);
    }
    if evidence_hash == [0; 32] {
        return Err(AllowanceError::EvidenceMissing);
    }
    if state.last_evidence_hash != [0; 32] && evidence_hash == state.last_evidence_hash {
        return Err(AllowanceError::DuplicateEvidence);
    }
    if evidence_hash != state.required_evidence_hash {
        return Ok(ChargeOutcome::FreezeEvidenceMismatch);
    }
    Ok(ChargeOutcome::Accepted { spent_after })
}

entrypoint!(process_instruction);

pub fn process_instruction(
    program_id: &Pubkey,
    accounts: &[AccountInfo],
    input: &[u8],
) -> ProgramResult {
    let instruction = AllowanceInstruction::try_from_slice(input)
        .map_err(|_| ProgramError::InvalidInstructionData)?;
    let account_info_iter = &mut accounts.iter();

    match instruction {
        AllowanceInstruction::Create {
            merchant,
            token_mint,
            allowed_program,
            per_charge,
            period_cap,
            expires_at,
            policy_hash,
            required_evidence_hash,
        } => {
            let authority = next_account_info(account_info_iter)?;
            let allowance = next_account_info(account_info_iter)?;
            require_signer(authority, AllowanceError::InvalidAuthority)?;
            require_program_account(allowance, program_id)?;
            if per_charge == 0
                || period_cap < per_charge
                || expires_at <= Clock::get()?.unix_timestamp
                || policy_hash == [0; 32]
                || required_evidence_hash == [0; 32]
            {
                return Err(AllowanceError::InvalidPolicy.into());
            }
            if allowance.data_len() < STATE_SIZE {
                return Err(ProgramError::AccountDataTooSmall);
            }
            let current = read_state(allowance)?;
            if current.version != 0 {
                return Err(AllowanceError::AlreadyInitialized.into());
            }
            let state = AllowanceState {
                version: 1,
                authority: *authority.key,
                merchant,
                token_mint,
                allowed_program,
                per_charge,
                period_cap,
                spent_in_period: 0,
                expires_at,
                revoked: false,
                frozen: false,
                policy_hash,
                required_evidence_hash,
                last_evidence_hash: [0; 32],
            };
            write_state(allowance, &state)?;
            msg!("Allowance created");
        }
        AllowanceInstruction::Charge {
            amount,
            evidence_hash,
        } => {
            let authority = next_account_info(account_info_iter)?;
            let allowance = next_account_info(account_info_iter)?;
            require_signer(authority, AllowanceError::InvalidAuthority)?;
            require_program_account(allowance, program_id)?;
            let source_token = next_account_info(account_info_iter)?;
            let merchant_token = next_account_info(account_info_iter)?;
            let token_program = next_account_info(account_info_iter)?;
            let mut state = read_state(allowance)?;
            if state.authority != *authority.key {
                return Err(AllowanceError::InvalidAuthority.into());
            }
            if token_program.key != &spl_token::id() {
                return Err(AllowanceError::InvalidTokenProgram.into());
            }
            if source_token.owner != token_program.key || merchant_token.owner != token_program.key
            {
                return Err(AllowanceError::InvalidTokenAccount.into());
            }
            let source_state = TokenAccount::unpack(&source_token.data.borrow())
                .map_err(|_| AllowanceError::InvalidTokenAccount)?;
            let merchant_state = TokenAccount::unpack(&merchant_token.data.borrow())
                .map_err(|_| AllowanceError::InvalidTokenAccount)?;
            if source_state.owner != *authority.key
                || source_state.mint != state.token_mint
                || merchant_state.mint != state.token_mint
            {
                return Err(AllowanceError::InvalidTokenAccount.into());
            }
            if merchant_state.owner != state.merchant {
                return Err(AllowanceError::InvalidMerchantAccount.into());
            }
            match evaluate_charge(&state, Clock::get()?.unix_timestamp, amount, evidence_hash)? {
                ChargeOutcome::Accepted { spent_after } => {
                    let transfer = spl_token::instruction::transfer(
                        token_program.key,
                        source_token.key,
                        merchant_token.key,
                        authority.key,
                        &[],
                        amount,
                    )?;
                    invoke(
                        &transfer,
                        &[
                            source_token.clone(),
                            merchant_token.clone(),
                            authority.clone(),
                            token_program.clone(),
                        ],
                    )?;
                    state.spent_in_period = spent_after;
                    state.last_evidence_hash = evidence_hash;
                    write_state(allowance, &state)?;
                    msg!("Allowance charge VERIFIED; SPL token transfer completed");
                }
                ChargeOutcome::FreezeEvidenceMismatch => {
                    state.frozen = true;
                    state.last_evidence_hash = evidence_hash;
                    write_state(allowance, &state)?;
                    msg!("Allowance FROZEN: evidence hash mismatch");
                }
            }
        }
        AllowanceInstruction::Revoke => {
            let authority = next_account_info(account_info_iter)?;
            let allowance = next_account_info(account_info_iter)?;
            require_signer(authority, AllowanceError::InvalidAuthority)?;
            require_program_account(allowance, program_id)?;
            let mut state = read_state(allowance)?;
            if state.authority != *authority.key {
                return Err(AllowanceError::InvalidAuthority.into());
            }
            state.revoked = true;
            write_state(allowance, &state)?;
            msg!("Allowance revoked");
        }
        AllowanceInstruction::CreateDelegated {
            merchant,
            executor,
            verifier,
            token_mint,
            source_token,
            per_charge,
            period_cap,
            lifetime_cap,
            period_seconds,
            expires_at,
            policy_hash,
        } => {
            let authority = next_account_info(account_info_iter)?;
            let allowance = next_account_info(account_info_iter)?;
            let source_token_account = next_account_info(account_info_iter)?;
            let delegate_pda = next_account_info(account_info_iter)?;
            let token_program = next_account_info(account_info_iter)?;
            require_signer(authority, AllowanceError::InvalidAuthority)?;
            require_program_account(allowance, program_id)?;

            let now = Clock::get()?.unix_timestamp;
            if merchant == Pubkey::default()
                || executor == Pubkey::default()
                || verifier == Pubkey::default()
                || token_mint == Pubkey::default()
                || source_token == Pubkey::default()
                || executor == verifier
                || verifier == merchant
                || per_charge == 0
                || period_cap < per_charge
                || lifetime_cap < period_cap
                || period_seconds <= 0
                || expires_at <= now
                || policy_hash == [0; 32]
            {
                return Err(AllowanceError::InvalidPolicy.into());
            }
            if allowance.data_len() < DELEGATED_STATE_SIZE {
                return Err(ProgramError::AccountDataTooSmall);
            }
            if allowance.data.borrow().first().copied().unwrap_or_default() != 0 {
                return Err(AllowanceError::AlreadyInitialized.into());
            }
            if token_program.key != &spl_token::id() {
                return Err(AllowanceError::InvalidTokenProgram.into());
            }
            if source_token_account.key != &source_token
                || source_token_account.owner != token_program.key
            {
                return Err(AllowanceError::InvalidSourceToken.into());
            }
            let source_state = TokenAccount::unpack(&source_token_account.data.borrow())
                .map_err(|_| AllowanceError::InvalidTokenAccount)?;
            if source_state.owner != *authority.key || source_state.mint != token_mint {
                return Err(AllowanceError::InvalidTokenAccount.into());
            }
            let (expected_delegate, _) = delegate_address(program_id, allowance.key);
            if delegate_pda.key != &expected_delegate {
                return Err(AllowanceError::InvalidDelegatePda.into());
            }

            let approve = spl_token::instruction::approve(
                token_program.key,
                source_token_account.key,
                delegate_pda.key,
                authority.key,
                &[],
                lifetime_cap,
            )?;
            invoke(
                &approve,
                &[
                    source_token_account.clone(),
                    delegate_pda.clone(),
                    authority.clone(),
                    token_program.clone(),
                ],
            )?;

            let state = AllowanceStateV2 {
                version: 2,
                authority: *authority.key,
                merchant,
                executor,
                verifier,
                token_mint,
                source_token,
                per_charge,
                period_cap,
                lifetime_cap,
                spent_in_period: 0,
                spent_lifetime: 0,
                period_started_at: now,
                period_seconds,
                expires_at,
                next_nonce: 0,
                paused: false,
                revoked: false,
                frozen: false,
                policy_hash,
                last_evidence_hash: [0; 32],
            };
            write_state_v2(allowance, &state)?;
            msg!("Delegated allowance created; SPL delegate approved");
        }
        AllowanceInstruction::ChargeDelegated {
            amount,
            nonce,
            evidence_hash,
        } => {
            let executor = next_account_info(account_info_iter)?;
            let verifier = next_account_info(account_info_iter)?;
            let allowance = next_account_info(account_info_iter)?;
            let source_token = next_account_info(account_info_iter)?;
            let merchant_token = next_account_info(account_info_iter)?;
            let delegate_pda = next_account_info(account_info_iter)?;
            let token_program = next_account_info(account_info_iter)?;
            require_program_account(allowance, program_id)?;

            let mut state = read_state_v2(allowance)?;
            validate_delegated_actors(
                &state,
                executor.key,
                executor.is_signer,
                verifier.key,
                verifier.is_signer,
            )?;
            if token_program.key != &spl_token::id() {
                return Err(AllowanceError::InvalidTokenProgram.into());
            }
            if source_token.key != &state.source_token
                || source_token.owner != token_program.key
                || merchant_token.owner != token_program.key
            {
                return Err(AllowanceError::InvalidSourceToken.into());
            }
            let source_state = TokenAccount::unpack(&source_token.data.borrow())
                .map_err(|_| AllowanceError::InvalidTokenAccount)?;
            let merchant_state = TokenAccount::unpack(&merchant_token.data.borrow())
                .map_err(|_| AllowanceError::InvalidTokenAccount)?;
            if source_state.owner != state.authority
                || source_state.mint != state.token_mint
                || merchant_state.mint != state.token_mint
            {
                return Err(AllowanceError::InvalidTokenAccount.into());
            }
            if merchant_state.owner != state.merchant {
                return Err(AllowanceError::InvalidMerchantAccount.into());
            }
            let (expected_delegate, bump) = delegate_address(program_id, allowance.key);
            if delegate_pda.key != &expected_delegate {
                return Err(AllowanceError::InvalidDelegatePda.into());
            }

            let outcome = evaluate_delegated_charge(
                &state,
                Clock::get()?.unix_timestamp,
                amount,
                nonce,
                evidence_hash,
            )?;
            let transfer = spl_token::instruction::transfer(
                token_program.key,
                source_token.key,
                merchant_token.key,
                delegate_pda.key,
                &[],
                amount,
            )?;
            let bump_seed = [bump];
            let signer_seeds: &[&[u8]] = &[DELEGATE_SEED, allowance.key.as_ref(), &bump_seed];
            invoke_signed(
                &transfer,
                &[
                    source_token.clone(),
                    merchant_token.clone(),
                    delegate_pda.clone(),
                    token_program.clone(),
                ],
                &[signer_seeds],
            )?;

            state.spent_in_period = outcome.spent_in_period;
            state.spent_lifetime = outcome.spent_lifetime;
            state.period_started_at = outcome.period_started_at;
            state.next_nonce = outcome.next_nonce;
            state.last_evidence_hash = evidence_hash;
            write_state_v2(allowance, &state)?;
            msg!("Delegated charge VERIFIED; user signature not required");
        }
        AllowanceInstruction::PauseDelegated => {
            let authority = next_account_info(account_info_iter)?;
            let allowance = next_account_info(account_info_iter)?;
            require_signer(authority, AllowanceError::InvalidAuthority)?;
            require_program_account(allowance, program_id)?;
            let mut state = read_state_v2(allowance)?;
            require_authority(&state, authority)?;
            state.paused = true;
            write_state_v2(allowance, &state)?;
            msg!("Delegated allowance paused");
        }
        AllowanceInstruction::UnpauseDelegated => {
            let authority = next_account_info(account_info_iter)?;
            let allowance = next_account_info(account_info_iter)?;
            require_signer(authority, AllowanceError::InvalidAuthority)?;
            require_program_account(allowance, program_id)?;
            let mut state = read_state_v2(allowance)?;
            require_authority(&state, authority)?;
            if state.revoked {
                return Err(AllowanceError::Revoked.into());
            }
            state.paused = false;
            write_state_v2(allowance, &state)?;
            msg!("Delegated allowance unpaused");
        }
        AllowanceInstruction::FreezeDelegated { evidence_hash } => {
            let verifier = next_account_info(account_info_iter)?;
            let allowance = next_account_info(account_info_iter)?;
            require_signer(verifier, AllowanceError::InvalidVerifier)?;
            require_program_account(allowance, program_id)?;
            let mut state = read_state_v2(allowance)?;
            if state.verifier != *verifier.key {
                return Err(AllowanceError::InvalidVerifier.into());
            }
            validate_freeze_evidence(&state, evidence_hash)?;
            state.frozen = true;
            state.last_evidence_hash = evidence_hash;
            write_state_v2(allowance, &state)?;
            msg!("Delegated allowance frozen by verifier with evidence");
        }
        AllowanceInstruction::UnfreezeDelegated => {
            let authority = next_account_info(account_info_iter)?;
            let verifier = next_account_info(account_info_iter)?;
            let allowance = next_account_info(account_info_iter)?;
            require_signer(authority, AllowanceError::InvalidAuthority)?;
            require_signer(verifier, AllowanceError::InvalidVerifier)?;
            require_program_account(allowance, program_id)?;
            let mut state = read_state_v2(allowance)?;
            require_authority(&state, authority)?;
            if state.verifier != *verifier.key {
                return Err(AllowanceError::InvalidVerifier.into());
            }
            if state.revoked {
                return Err(AllowanceError::Revoked.into());
            }
            state.frozen = false;
            write_state_v2(allowance, &state)?;
            msg!("Delegated allowance unfrozen by authority and verifier");
        }
        AllowanceInstruction::RevokeDelegated => {
            let authority = next_account_info(account_info_iter)?;
            let allowance = next_account_info(account_info_iter)?;
            let source_token = next_account_info(account_info_iter)?;
            let token_program = next_account_info(account_info_iter)?;
            require_signer(authority, AllowanceError::InvalidAuthority)?;
            require_program_account(allowance, program_id)?;
            let mut state = read_state_v2(allowance)?;
            require_authority(&state, authority)?;
            if token_program.key != &spl_token::id() {
                return Err(AllowanceError::InvalidTokenProgram.into());
            }
            if source_token.key != &state.source_token || source_token.owner != token_program.key {
                return Err(AllowanceError::InvalidSourceToken.into());
            }
            let source_state = TokenAccount::unpack(&source_token.data.borrow())
                .map_err(|_| AllowanceError::InvalidTokenAccount)?;
            if source_state.owner != *authority.key || source_state.mint != state.token_mint {
                return Err(AllowanceError::InvalidTokenAccount.into());
            }
            let revoke = spl_token::instruction::revoke(
                token_program.key,
                source_token.key,
                authority.key,
                &[],
            )?;
            invoke(
                &revoke,
                &[
                    source_token.clone(),
                    authority.clone(),
                    token_program.clone(),
                ],
            )?;
            state.revoked = true;
            state.paused = true;
            write_state_v2(allowance, &state)?;
            msg!("Delegated allowance revoked; SPL delegate removed");
        }
    }
    Ok(())
}

fn require_signer(account: &AccountInfo, error: AllowanceError) -> ProgramResult {
    if !account.is_signer {
        return Err(error.into());
    }
    Ok(())
}

fn require_program_account(account: &AccountInfo, program_id: &Pubkey) -> ProgramResult {
    if account.owner != program_id {
        return Err(AllowanceError::InvalidAccountOwner.into());
    }
    Ok(())
}

fn require_authority(state: &AllowanceStateV2, authority: &AccountInfo) -> ProgramResult {
    if state.authority != *authority.key {
        return Err(AllowanceError::InvalidAuthority.into());
    }
    Ok(())
}

pub fn delegate_address(program_id: &Pubkey, allowance: &Pubkey) -> (Pubkey, u8) {
    Pubkey::find_program_address(&[DELEGATE_SEED, allowance.as_ref()], program_id)
}

fn read_state(account: &AccountInfo) -> Result<AllowanceState, ProgramError> {
    let mut data = &account.data.borrow()[..];
    AllowanceState::deserialize(&mut data).map_err(|_| ProgramError::InvalidAccountData)
}

fn write_state(account: &AccountInfo, state: &AllowanceState) -> ProgramResult {
    let mut data = account.data.borrow_mut();
    state
        .serialize(&mut &mut data[..])
        .map_err(|_| ProgramError::AccountDataTooSmall)
}

fn read_state_v2(account: &AccountInfo) -> Result<AllowanceStateV2, ProgramError> {
    let mut data = &account.data.borrow()[..];
    let state =
        AllowanceStateV2::deserialize(&mut data).map_err(|_| ProgramError::InvalidAccountData)?;
    if state.version != 2 {
        return Err(AllowanceError::InvalidStateVersion.into());
    }
    Ok(state)
}

fn write_state_v2(account: &AccountInfo, state: &AllowanceStateV2) -> ProgramResult {
    let mut data = account.data.borrow_mut();
    state
        .serialize(&mut &mut data[..])
        .map_err(|_| ProgramError::AccountDataTooSmall)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn instruction_round_trip_is_stable() {
        let instruction = AllowanceInstruction::Charge {
            amount: 2,
            evidence_hash: [7; 32],
        };
        let encoded = instruction.try_to_vec().unwrap();
        assert_eq!(
            AllowanceInstruction::try_from_slice(&encoded).unwrap(),
            instruction
        );
    }

    #[test]
    fn legacy_instruction_discriminants_remain_stable() {
        let create = AllowanceInstruction::Create {
            merchant: Pubkey::new_unique(),
            token_mint: Pubkey::new_unique(),
            allowed_program: Pubkey::new_unique(),
            per_charge: 1,
            period_cap: 2,
            expires_at: 3,
            policy_hash: [4; 32],
            required_evidence_hash: [5; 32],
        };
        let charge = AllowanceInstruction::Charge {
            amount: 1,
            evidence_hash: [6; 32],
        };
        assert_eq!(create.try_to_vec().unwrap()[0], 0);
        assert_eq!(charge.try_to_vec().unwrap()[0], 1);
        assert_eq!(AllowanceInstruction::Revoke.try_to_vec().unwrap(), vec![2]);

        let delegated = AllowanceInstruction::CreateDelegated {
            merchant: Pubkey::new_unique(),
            executor: Pubkey::new_unique(),
            verifier: Pubkey::new_unique(),
            token_mint: Pubkey::new_unique(),
            source_token: Pubkey::new_unique(),
            per_charge: 1,
            period_cap: 2,
            lifetime_cap: 3,
            period_seconds: 60,
            expires_at: 100,
            policy_hash: [7; 32],
        };
        assert_eq!(delegated.try_to_vec().unwrap()[0], 3);
    }

    fn active_state() -> AllowanceState {
        AllowanceState {
            version: 1,
            authority: Pubkey::new_unique(),
            merchant: Pubkey::new_unique(),
            token_mint: Pubkey::new_unique(),
            allowed_program: Pubkey::new_unique(),
            per_charge: 2_000_000,
            period_cap: 8_000_000,
            spent_in_period: 1_000_000,
            expires_at: 2_000_000_000,
            revoked: false,
            frozen: false,
            policy_hash: [3; 32],
            required_evidence_hash: [7; 32],
            last_evidence_hash: [0; 32],
        }
    }

    fn active_delegated_state() -> AllowanceStateV2 {
        AllowanceStateV2 {
            version: 2,
            authority: Pubkey::new_unique(),
            merchant: Pubkey::new_unique(),
            executor: Pubkey::new_unique(),
            verifier: Pubkey::new_unique(),
            token_mint: Pubkey::new_unique(),
            source_token: Pubkey::new_unique(),
            per_charge: 2_000_000,
            period_cap: 5_000_000,
            lifetime_cap: 12_000_000,
            spent_in_period: 1_000_000,
            spent_lifetime: 3_000_000,
            period_started_at: 1_900_000_000,
            period_seconds: 86_400,
            expires_at: 2_000_000_000,
            next_nonce: 4,
            paused: false,
            revoked: false,
            frozen: false,
            policy_hash: [3; 32],
            last_evidence_hash: [2; 32],
        }
    }

    #[test]
    fn matching_evidence_is_verified() {
        assert_eq!(
            evaluate_charge(&active_state(), 1_900_000_000, 1_000_000, [7; 32]),
            Ok(ChargeOutcome::Accepted {
                spent_after: 2_000_000
            })
        );
    }

    #[test]
    fn mismatched_evidence_freezes_without_spending() {
        assert_eq!(
            evaluate_charge(&active_state(), 1_900_000_000, 1_000_000, [9; 32]),
            Ok(ChargeOutcome::FreezeEvidenceMismatch)
        );
    }

    #[test]
    fn excessive_charge_is_blocked_before_evidence_handling() {
        assert_eq!(
            evaluate_charge(&active_state(), 1_900_000_000, 3_000_000, [9; 32]),
            Err(AllowanceError::PerChargeLimitExceeded)
        );
    }

    #[test]
    fn duplicate_evidence_is_rejected_before_transfer() {
        let mut state = active_state();
        state.last_evidence_hash = [7; 32];
        assert_eq!(
            evaluate_charge(&state, 1_900_000_000, 1_000_000, [7; 32]),
            Err(AllowanceError::DuplicateEvidence)
        );
    }

    #[test]
    fn delegated_state_serializes_to_declared_size() {
        assert_eq!(
            active_delegated_state().try_to_vec().unwrap().len(),
            DELEGATED_STATE_SIZE
        );
    }

    #[test]
    fn delegated_charge_succeeds_without_user_authority_input() {
        let state = active_delegated_state();
        let outcome = evaluate_delegated_charge(
            &state,
            state.period_started_at + 60,
            1_000_000,
            state.next_nonce,
            [8; 32],
        )
        .unwrap();
        assert_eq!(outcome.spent_in_period, 2_000_000);
        assert_eq!(outcome.spent_lifetime, 4_000_000);
        assert_eq!(outcome.next_nonce, 5);
    }

    #[test]
    fn delegated_actors_require_exact_executor_and_verifier_signatures() {
        let state = active_delegated_state();
        assert_eq!(
            validate_delegated_actors(&state, &state.executor, true, &state.verifier, true),
            Ok(())
        );
        assert_eq!(
            validate_delegated_actors(&state, &Pubkey::new_unique(), true, &state.verifier, true),
            Err(AllowanceError::InvalidExecutor)
        );
        assert_eq!(
            validate_delegated_actors(&state, &state.executor, true, &state.verifier, false),
            Err(AllowanceError::InvalidVerifier)
        );
        assert_eq!(
            validate_delegated_actors(&state, &state.executor, true, &Pubkey::new_unique(), true),
            Err(AllowanceError::InvalidVerifier)
        );
    }

    #[test]
    fn delegated_nonce_and_evidence_replay_are_rejected() {
        let state = active_delegated_state();
        assert_eq!(
            evaluate_delegated_charge(
                &state,
                state.period_started_at + 60,
                1,
                state.next_nonce + 1,
                [8; 32]
            ),
            Err(AllowanceError::InvalidNonce)
        );
        assert_eq!(
            evaluate_delegated_charge(
                &state,
                state.period_started_at + 60,
                1,
                state.next_nonce,
                state.last_evidence_hash
            ),
            Err(AllowanceError::DuplicateEvidence)
        );
    }

    #[test]
    fn delegated_caps_are_enforced() {
        let state = active_delegated_state();
        assert_eq!(
            evaluate_delegated_charge(
                &state,
                state.period_started_at + 60,
                state.per_charge + 1,
                state.next_nonce,
                [8; 32]
            ),
            Err(AllowanceError::PerChargeLimitExceeded)
        );

        let mut period_full = state.clone();
        period_full.spent_in_period = period_full.period_cap;
        assert_eq!(
            evaluate_delegated_charge(
                &period_full,
                period_full.period_started_at + 60,
                1,
                period_full.next_nonce,
                [8; 32]
            ),
            Err(AllowanceError::PeriodCapExceeded)
        );

        let mut lifetime_full = state;
        lifetime_full.spent_lifetime = lifetime_full.lifetime_cap;
        assert_eq!(
            evaluate_delegated_charge(
                &lifetime_full,
                lifetime_full.period_started_at + 60,
                1,
                lifetime_full.next_nonce,
                [8; 32]
            ),
            Err(AllowanceError::LifetimeCapExceeded)
        );
    }

    #[test]
    fn delegated_period_rolls_over_deterministically() {
        let state = active_delegated_state();
        let rollover_at = state.period_started_at + state.period_seconds;
        let outcome =
            evaluate_delegated_charge(&state, rollover_at, 500_000, state.next_nonce, [8; 32])
                .unwrap();
        assert_eq!(outcome.period_started_at, rollover_at);
        assert_eq!(outcome.spent_in_period, 500_000);
    }

    #[test]
    fn delegated_safety_states_block_charges() {
        let base = active_delegated_state();
        for (state, expected) in [
            (
                {
                    let mut state = base.clone();
                    state.paused = true;
                    state
                },
                AllowanceError::Paused,
            ),
            (
                {
                    let mut state = base.clone();
                    state.frozen = true;
                    state
                },
                AllowanceError::Frozen,
            ),
            (
                {
                    let mut state = base.clone();
                    state.revoked = true;
                    state
                },
                AllowanceError::Revoked,
            ),
        ] {
            assert_eq!(
                evaluate_delegated_charge(
                    &state,
                    state.period_started_at + 60,
                    1,
                    state.next_nonce,
                    [8; 32]
                ),
                Err(expected)
            );
        }
    }

    #[test]
    fn delegated_freeze_requires_fresh_evidence_and_active_state() {
        let state = active_delegated_state();
        assert_eq!(validate_freeze_evidence(&state, [8; 32]), Ok(()));
        assert_eq!(
            validate_freeze_evidence(&state, [0; 32]),
            Err(AllowanceError::EvidenceMissing)
        );
        assert_eq!(
            validate_freeze_evidence(&state, state.last_evidence_hash),
            Err(AllowanceError::DuplicateEvidence)
        );
        let mut frozen = state.clone();
        frozen.frozen = true;
        assert_eq!(
            validate_freeze_evidence(&frozen, [8; 32]),
            Err(AllowanceError::Frozen)
        );
        let mut revoked = state;
        revoked.revoked = true;
        assert_eq!(
            validate_freeze_evidence(&revoked, [8; 32]),
            Err(AllowanceError::Revoked)
        );
    }

    #[test]
    fn delegate_pda_is_deterministic_and_allowance_scoped() {
        let program_id = Pubkey::new_unique();
        let allowance_a = Pubkey::new_unique();
        let allowance_b = Pubkey::new_unique();
        let (delegate_a, bump_a) = delegate_address(&program_id, &allowance_a);
        let (delegate_a_again, bump_a_again) = delegate_address(&program_id, &allowance_a);
        let (delegate_b, _) = delegate_address(&program_id, &allowance_b);
        assert_eq!((delegate_a, bump_a), (delegate_a_again, bump_a_again));
        assert_ne!(delegate_a, delegate_b);
        assert_ne!(delegate_a, allowance_a);
    }
}
