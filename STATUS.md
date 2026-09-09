# Project Status — September 9, 2026

## Complete locally

- TypeScript policy engine and independent verifier.
- `VERIFIED`, `BLOCKED`, `FROZEN`, and `REVOKED` flows.
- Browser judge replay.
- Native Solana instruction source for create, charge, and revoke.
- Policy schema and SDK-facing instruction model.
- Explicit standard Android and Seeker capability profiles.
- Native Kotlin/Compose Android client.
- Official Solana Mobile Wallet Adapter 2.0.7 integration.
- Phantom/MWA authorize, `signAndSendTransactions`, explorer evidence, and deauthorize implementation.
- Android policy tests and successful debug APK build.
- Eight passing TypeScript tests.
- Native Solana program compiled with two passing Rust tests.
- Rust formatting and multi-job CI workflow.

## Verified Android build

```text
Artifact: mobile/android/app/build/outputs/apk/debug/app-debug.apk
Size: 18 MB
SHA-256: 0964730f20cd551483d946ab419fd8ee6952095a7afcc0f0decc7e1354f83e98
```

## External steps still required

- Publicly record the connected Devnet wallet and its real Memo authorization proof.
- Generate a reviewed program keypair and deploy the allowance program to Devnet.
- Add SPL-token CPI settlement and record create, charge, and revoke transactions.
- Test Seed Vault and Genesis Token on actual compatible Solana Mobile hardware.

The live wallet and Memo evidence are recorded in `evidence/live-devnet-wallet.json` and `evidence/live-devnet-memo.json`. Program deployment and SPL-token settlement remain intentionally separate pending work.
