# Allowance OS Android v0.18.0

Released: 2026-09-12

## Headline

v0.18.0 turns the native wallet checkpoint into a real, independently verifiable commercial micro-settlement and gives judges one explicit end-to-end run.

## Judge Run

One tap now:

1. clears the device-local safety Pause and selects the AlphaBrief baseline;
2. replays `VERIFIED`, `BLOCKED`, and `FROZEN` locally;
3. checks the published AlphaBrief settlement and freeze signatures through Solana Devnet RPC;
4. verifies the deployed Program state and token-balance matrix;
5. pauses exactly once for a Solflare/MWA approval;
6. broadcasts `0.00001 Devnet SOL` to the AlphaBrief merchant with the accepted report evidence hash in the same transaction;
7. waits for confirmation and verifies the merchant lamport balance increase.

The app never automates or disguises wallet approval.

## UX changes

- wallet access is present in the compact Header and the disconnected Home hero;
- transient success feedback closes automatically after four seconds;
- body copy and dividers have stronger recording-safe contrast;
- the app version appears in Evidence rather than the primary Header;
- Portable Receipt shows a summary first and raw JSON only on demand;
- `Sync public proofs` is now `Verify live AlphaBrief proof`.

## Evidence boundary

- `SIMULATED`: deterministic local policy replay;
- `LIVE DEVNET RPC`: read-only verification of public transactions, Program state, and balance deltas;
- `LIVE MOBILE SETTLEMENT`: an MWA-approved SOL transfer plus evidence Memo;
- `DELEGATED V2`: the existing public project-test-token settlement and control matrix.

This APK is debug-signed and Devnet-only. The `0.00001 SOL` transfer uses Devnet SOL; delegated-v2 evidence uses a project-created test mint, not canonical USDC. The Program is unaudited and no Mainnet or production-readiness claim is made.
