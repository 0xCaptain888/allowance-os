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
- Official Solana Mobile Wallet Adapter 2.0.7 integration.
- Phantom/MWA authorize, `signAndSendTransactions`, explorer evidence, and deauthorize implementation.
- Android policy tests and successful debug APK build.
- Eight passing TypeScript tests.
- Native Solana program compiled with two passing Rust tests.
- Rust formatting and multi-job CI workflow.

## Last verified Android build

```text
Artifact: mobile/android/app/build/outputs/apk/debug/app-debug.apk
Size: 18 MB
SHA-256: b3d7d1119bcf4919af70fbae424cac252fa02cc7b2e82e51848c59eea14acb0b
```

Source is now v0.5.0. The v0.5.0 APK still needs a fresh Gradle/Android Studio build; the last verified artifact above is v0.4.0.

## External steps still required

- Publicly record the connected Devnet wallet and its real Memo authorization proof.
- Generate a reviewed program keypair and deploy the allowance program to Devnet.
- Add SPL-token CPI settlement and record create, charge, and revoke transactions.
- Test Seed Vault and Genesis Token on actual compatible Solana Mobile hardware.

The live wallet and Memo evidence are recorded in `evidence/live-devnet-wallet.json` and `evidence/live-devnet-memo.json`. Program deployment and SPL-token settlement remain intentionally separate pending work.
