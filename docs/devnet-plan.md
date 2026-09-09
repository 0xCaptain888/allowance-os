# Devnet Integration Record

The repository has a strict adapter boundary. `SimulatedAllowanceAdapter` is used for local replay; live transactions require explicit Devnet configuration and a signer outside the repository.

## Completed P0 sequence

1. Defined the native Borsh account layout for the allowance policy and evidence state.
2. Implemented create, charge, evidence-freeze, and revoke instructions.
3. Stored the canonical policy and required-evidence hashes onchain.
4. Enforced authority, mint, merchant, program, per-charge cap, period cap, expiry, and evidence boundaries in the Program.
5. Created a six-decimal Devnet test mint and payer/merchant token accounts. It is explicitly not canonical USDC.
6. Connected the Android client through Mobile Wallet Adapter and recorded a real Phantom Memo proof.
7. Upgraded the Program to invoke the SPL Token Program only after a VERIFIED decision.
8. Recorded a fresh public sequence: VERIFIED transfers `1,000,000` raw units; BLOCKED and FROZEN transfer zero; REVOKED persists containment.
9. Saved the machine-readable record in `evidence/live-devnet-program.json` and added independent browser/Android RPC verification.

## Non-negotiable safety boundary

The adapter must never turn a local receipt into a live claim. A receipt is live only when it contains a confirmed Solana signature and a blockhash/slot that can be independently queried.
