# Project Status — September 10, 2026

## Complete locally

- TypeScript policy engine and independent verifier.
- `VERIFIED`, `BLOCKED`, `FROZEN`, and `REVOKED` flows.
- Browser judge replay.
- Native Solana instruction source for create, SPL-token CPI charge, evidence freeze, and revoke.
- Policy schema and SDK-facing instruction model.
- Explicit standard Android and Seeker capability profiles.
- Native Kotlin/Compose Android client.
- Commercial Services catalog with five reusable allowance templates.
- Seeker Integration Lab with explicitly unofficial blueprints for apps featured by Solana Mobile.
- Period-aware policy studio with current-period spend input and projected cap meter.
- Judge mode that records VERIFIED, BLOCKED, and FROZEN outcomes in a persistent local Activity Log.
- Portable JSON receipt export with a SHA-256 fingerprint and explicit evidence level.
- Direct Solana Devnet RPC verification for the current or recorded live signature.
- Android Evidence Center verification of the complete Program transaction matrix and final account state.
- Public judge demo with read-only live confirmation, slot, and execution-error checks.
- Official Solana Mobile Wallet Adapter 2.0.7 integration.
- Phantom/MWA authorize, `signAndSendTransactions`, explorer evidence, and deauthorize implementation.
- Android policy tests and successful debug APK build.
- Fourteen passing TypeScript tests, including idempotency, stale-request, signed-webhook, tamper, live-adapter, and replay coverage.
- npm dependency audit: 0 known vulnerabilities after compatible transitive overrides.
- Native Solana program source compiled with six passing Rust tests, including duplicate-evidence rejection.
- Upgradeable Program deployed to Solana Devnet at `DJzPBS7FreCcWWGkApzznGcKq9T7Da38GpKFtpxWRcuE`.
- Real Devnet allowance account with public `CREATED`, `VERIFIED`, `BLOCKED`, `FROZEN`, and `REVOKED` evidence.
- VERIFIED transfers exactly `1,000,000` raw SPL-token units through CPI; source balance changes `20 → 19`, merchant `0 → 1`.
- BLOCKED fails before CPI with Custom Error `6`; FROZEN and REVOKED persist without moving tokens.
- Local and deployed Program binaries independently matched at SHA-256 `456f803ffa64b8f37954e973bc1fedef8d0cfa070ba2d54cbe34c4a19cc37346`.
- Onchain evidence mismatch persists `frozen = true`; revoke persists `revoked = true`.
- Rust formatting and multi-job CI workflow.

## Android v0.10.0 build

```text
Artifact: mobile/android/app/build/outputs/apk/debug/allowance-os-0.10.0-debug.apk
Size: 18,975,074 bytes
Version code: 10
Version name: 0.10.0
Android tests: 10 passed, 0 failed
SHA-256: 8fc1b16e2ecae5bd451bc2110a8093c6f0b0992f8ce87ee00524a9e72c455be3
```

## External hardware step still required

- Test Seed Vault and Genesis Token on actual compatible Solana Mobile hardware.
- Build a production-signed APK and submit it through the Solana dApp Store Publisher Portal.
- Upgrade the deployed Devnet Program after the ETJL7fK6… upgrade-authority signer is available; until then the live evidence remains the disclosed v0.9.0 binary.

The wallet/Memo evidence is recorded separately from program enforcement. Program deployment, real state transitions, and the SPL-token CPI settlement are recorded in `evidence/live-devnet-program.json`. The token is a project-created Devnet test mint and is not presented as canonical USDC.
