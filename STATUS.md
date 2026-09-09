# Project Status — September 9, 2026

## Complete locally

- TypeScript policy engine and independent verifier.
- `VERIFIED`, `BLOCKED`, `FROZEN`, and `REVOKED` flows.
- Browser judge replay.
- Native Solana instruction source for create, charge, and revoke.
- Policy schema and SDK-facing instruction model.
- Explicit standard Android and Seeker capability profiles.
- Native Kotlin/Compose Android client.
- Period-aware policy studio with current-period spend input and projected cap meter.
- Judge mode that records VERIFIED, BLOCKED, and FROZEN outcomes in a persistent local Activity Log.
- Portable JSON receipt export with a SHA-256 fingerprint and explicit evidence level.
- Direct Solana Devnet RPC verification for the current or recorded live signature.
- Public judge demo with read-only live confirmation, slot, and execution-error checks.
- Official Solana Mobile Wallet Adapter 2.0.7 integration.
- Phantom/MWA authorize, `signAndSendTransactions`, explorer evidence, and deauthorize implementation.
- Android policy tests and successful debug APK build.
- Eight passing TypeScript tests.
- Native Solana program compiled with two passing Rust tests.
- Rust formatting and multi-job CI workflow.

## Verified Android v0.6.0 build

```text
Artifact: mobile/android/app/build/outputs/apk/debug/allowance-os-0.6.0-debug.apk
Size: 18 MB
Version code: 6
Version name: 0.6.0
Android tests: 6 passed, 0 failed
SHA-256: dc4737acebf2f12c00b4664d63b4719e1ca11ad13a8d9e1240aba6dfcdf58370
```

## External steps still required

- Generate a reviewed program keypair and deploy the allowance program to Devnet.
- Add SPL-token CPI settlement and record create, charge, and revoke transactions.
- Test Seed Vault and Genesis Token on actual compatible Solana Mobile hardware.

The live wallet and Memo evidence are recorded in `evidence/live-devnet-wallet.json` and `evidence/live-devnet-memo.json`. Program deployment and SPL-token settlement remain intentionally separate pending work.
