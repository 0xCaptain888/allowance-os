# Devnet Integration Plan

The repository now has a strict adapter boundary. `SimulatedAllowanceAdapter` is used for local replay; `SolanaDevnetAdapter` intentionally refuses to broadcast until wallet and program configuration are present.

## P0 sequence

1. Define the Anchor account layout for `Allowance` and `Receipt`.
2. Implement `create_allowance`, `charge_allowance`, and `revoke_allowance` instructions.
3. Store the canonical policy hash on the allowance account.
4. Verify the same caps and merchant/program boundaries in the program.
5. Add a Devnet USDC mint and a test payer/merchant wallet.
6. Connect the Android client through MWA / Seed Vault.
7. Broadcast one small charge and save its explorer URL in `evidence/live-devnet.json`.
8. Keep any missing live proof marked as `DESIGN` rather than simulated evidence.

## Non-negotiable safety boundary

The adapter must never turn a local receipt into a live claim. A receipt is live only when it contains a confirmed Solana signature and a blockhash/slot that can be independently queried.
