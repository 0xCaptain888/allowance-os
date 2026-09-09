# Project Status — September 9, 2026

## Complete locally

- TypeScript policy engine and independent verifier.
- `VERIFIED`, `BLOCKED`, `FROZEN`, and `REVOKED` flows.
- Browser judge replay.
- Native Solana instruction source for create, charge, evidence freeze, and revoke.
- Policy schema and SDK-facing instruction model.
- Explicit standard Android and Seeker capability profiles.
- Native Kotlin/Compose Android client.
- Period-aware policy studio with current-period spend input and projected cap meter.
- Judge mode that records VERIFIED, BLOCKED, and FROZEN outcomes in a persistent local Activity Log.
- Portable JSON receipt export with a SHA-256 fingerprint and explicit evidence level.
- Direct Solana Devnet RPC verification for the current or recorded live signature.
- Android Evidence Center verification of the complete Program transaction matrix and final account state.
- Public judge demo with read-only live confirmation, slot, and execution-error checks.
- Official Solana Mobile Wallet Adapter 2.0.7 integration.
- Phantom/MWA authorize, `signAndSendTransactions`, explorer evidence, and deauthorize implementation.
- Android policy tests and successful debug APK build.
- Eight passing TypeScript tests.
- Native Solana program compiled with five passing Rust tests.
- Upgradeable Program deployed to Solana Devnet at `DJzPBS7FreCcWWGkApzznGcKq9T7Da38GpKFtpxWRcuE`.
- Real Devnet allowance account with public `CREATED`, `VERIFIED`, `BLOCKED`, `FROZEN`, and `REVOKED` evidence.
- Onchain evidence mismatch persists `frozen = true`; revoke persists `revoked = true`.
- Rust formatting and multi-job CI workflow.

## Verified Android v0.7.0 build

```text
Artifact: mobile/android/app/build/outputs/apk/debug/allowance-os-0.7.0-debug.apk
Size: 18 MB
Version code: 7
Version name: 0.7.0
Android tests: 6 passed, 0 failed
SHA-256: 0d0b0553d671f90884f5c99028ce33c46f8e7a95558cb059eecefd101be76e2c
```

## External steps still required

- Add SPL-token CPI settlement and record create, charge, and revoke transactions.
- Test Seed Vault and Genesis Token on actual compatible Solana Mobile hardware.

The wallet/Memo evidence is recorded separately from program enforcement. Program deployment and its real state transitions are recorded in `evidence/live-devnet-program.json`. SPL-token settlement remains intentionally pending and is never claimed by the repository.
