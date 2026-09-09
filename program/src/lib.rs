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
    program::invoke,
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
        })
    }
}

#[derive(Debug, PartialEq, Eq)]
enum ChargeOutcome {
    Accepted { spent_after: u64 },
    FreezeEvidenceMismatch,
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
    let authority = next_account_info(account_info_iter)?;
    let allowance = next_account_info(account_info_iter)?;

    if !authority.is_signer {
        return Err(AllowanceError::InvalidAuthority.into());
    }
    if allowance.owner != program_id {
        return Err(AllowanceError::InvalidAccountOwner.into());
    }

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
            let mut state = read_state(allowance)?;
            if state.authority != *authority.key {
                return Err(AllowanceError::InvalidAuthority.into());
            }
            state.revoked = true;
            write_state(allowance, &state)?;
            msg!("Allowance revoked");
        }
    }
    Ok(())
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
}
