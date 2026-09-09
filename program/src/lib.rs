//! Minimal native Solana program boundary for Allowance OS.
//!
//! This program enforces the spending policy on-chain. The first version records
//! allowance state and rejects invalid charges. SPL-USDC transfer CPI is kept as
//! a separate adapter step so the policy cannot silently become a UI-only check.

use borsh::{BorshDeserialize, BorshSerialize};
use solana_program::{
    account_info::{next_account_info, AccountInfo},
    clock::Clock,
    entrypoint,
    entrypoint::ProgramResult,
    msg,
    program_error::ProgramError,
    pubkey::Pubkey,
    sysvar::Sysvar,
};

// Placeholder Devnet program id. Replace it with the generated deploy keypair
// before deployment; no live address is claimed by the repository yet.
solana_program::declare_id!("9q4QgGtd4ZHo4dpca8hFDTAAque2ykqhiyziUcgGaiKQ");

const STATE_SIZE: usize = 8 + 32 + 32 + 32 + 32 + 8 + 8 + 8 + 8 + 1 + 32;

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
    pub policy_hash: [u8; 32],
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
        })
    }
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
        } => {
            if per_charge == 0
                || period_cap < per_charge
                || expires_at <= Clock::get()?.unix_timestamp
                || policy_hash == [0; 32]
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
                policy_hash,
            };
            write_state(allowance, &state)?;
            msg!("Allowance created");
        }
        AllowanceInstruction::Charge {
            amount,
            evidence_hash,
        } => {
            let mut state = read_state(allowance)?;
            if state.authority != *authority.key {
                return Err(AllowanceError::InvalidAuthority.into());
            }
            if state.revoked {
                return Err(AllowanceError::Revoked.into());
            }
            if state.expires_at <= Clock::get()?.unix_timestamp {
                return Err(AllowanceError::Expired.into());
            }
            if amount == 0 || amount > state.per_charge {
                return Err(AllowanceError::PerChargeLimitExceeded.into());
            }
            if state
                .spent_in_period
                .checked_add(amount)
                .ok_or(AllowanceError::PeriodCapExceeded)?
                > state.period_cap
            {
                return Err(AllowanceError::PeriodCapExceeded.into());
            }
            if evidence_hash == [0; 32] {
                return Err(AllowanceError::EvidenceMissing.into());
            }
            state.spent_in_period = state
                .spent_in_period
                .checked_add(amount)
                .ok_or(AllowanceError::PeriodCapExceeded)?;
            write_state(allowance, &state)?;
            msg!("Allowance charge accepted; payment CPI is adapter-owned");
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
}
