# Project Status — September 10, 2026

## Complete locally

- TypeScript policy engine and independent verifier.
- `VERIFIED`, `BLOCKED`, `FROZEN`, and `REVOKED` flows.
- Browser judge replay.
- Native Solana instruction source for create, SPL-token CPI charge, evidence freeze, and revoke.
- Delegated Settlement v2 source with an allowance-scoped SPL delegate PDA; later charges require executor + verifier signatures and omit the user authority.
- V2 sequential nonce, per-charge/period/lifetime caps, deterministic period rollover, pause, evidence-bound freeze, dual-signature unfreeze, and SPL delegate removal on revoke.
- Matching TypeScript v2 instruction builders with stable Borsh discriminants and explicit account/signer ordering.
- Policy schema and SDK-facing instruction model.
- Explicit standard Android and Seeker capability profiles.
- Native Kotlin/Compose Android client.
- Commercial Services catalog with five reusable allowance templates.
- Seeker Integration Lab with explicitly unofficial blueprints for apps featured by Solana Mobile.
- Period-aware policy studio with current-period spend input and projected cap meter.
- Judge mode that records VERIFIED, BLOCKED, and FROZEN outcomes in a persistent local Activity Log.
- Portable JSON receipt export with a SHA-256 fingerprint and explicit evidence level.
- Portable JSON Activity audit export with its own SHA-256 fingerprint.
- Direct Solana Devnet RPC verification for the current or recorded live signature.
- Android Evidence Center verification of the complete Program transaction matrix and final account state.
- Android Delegated Settlement v2 state decoder plus pause, unpause, revoke, executor-rotation, and verifier-rotation instruction encoders.
- Configurable Android Devnet RPC endpoint with bounded retries.
- Public judge demo with read-only live confirmation, slot, and execution-error checks.
- Official Solana Mobile Wallet Adapter 2.0.7 integration.
- Phantom/MWA authorize, `signAndSendTransactions`, explorer evidence, and deauthorize implementation.
- Android Keystore AES-GCM encryption for the MWA reconnect token, with migration from the legacy plaintext preference.
- Explicit wallet-action review and distinct MWA disconnect versus onchain-revoke language.
- Persistent Chinese/English preference, cleartext-traffic denial, and adaptive launcher icon.
- Fail-closed production signing configuration and local signed-release builder.
- Android policy tests and successful debug APK build.
- Twenty-four passing TypeScript tests, including malformed-amount, invalid-budget, idempotency, persistence/restart, webhook rotation/timestamp/replay, tamper, live-adapter, evidence PDA, and delegated instruction coverage.
- Atomic JSON-backed single-process merchant reference storage behind a pluggable runtime store.
- Stateful webhook verification with time tolerance, replay rejection, and overlapping-secret rotation.
- npm dependency audit: 0 known vulnerabilities after compatible transitive overrides.
- Native Solana Program v0.2.0 source compiled with 20 passing Rust tests, including v1 compatibility, delegated settlement safety boundaries, immutable evidence accounts, role rotation, and evidence-bound freeze validation.
- Solana SBF v2 binary built locally and in GitHub CI with pinned `cargo-build-sbf 4.3.0`; both artifacts are 130,280 bytes. macOS SHA-256: `e0eb4726bdfda25fa2a377f347b9590087072e4ad1d9e455598760f6b865e7d6`; Ubuntu CI SHA-256: `45a3996ee011587e394951ee344742290c0aa7d0d132d972cb0168360df191dc`.
- Dedicated v2 Program deployed to Solana Devnet at `7zARKWKDLawLgR7qokvQdkAv6ye2cXGNEvNQswBd6xvL`.
- Dumped v2 Program bytes exactly match the public Ubuntu CI artifact at SHA-256 `45a3996ee011587e394951ee344742290c0aa7d0d132d972cb0168360df191dc`.
- Real approve-once / settle-later task transfers `1,000,000` raw test-token units with executor + verifier signatures and no authority signature.
- Public v2 matrix verifies BLOCKED zero movement, pause/unpause, immutable-evidence freeze, dual-signature unfreeze, executor/verifier rotation, rotated-role settlement, and terminal revoke with SPL delegate removal.
- Upgradeable Program deployed to Solana Devnet at `DJzPBS7FreCcWWGkApzznGcKq9T7Da38GpKFtpxWRcuE`.
- Real Devnet allowance account with public `CREATED`, `VERIFIED`, `BLOCKED`, `FROZEN`, and `REVOKED` evidence.
- VERIFIED transfers exactly `1,000,000` raw SPL-token units through CPI; source balance changes `20 → 19`, merchant `0 → 1`.
- BLOCKED fails before CPI with Custom Error `6`; FROZEN and REVOKED persist without moving tokens.
- Local and deployed Program binaries independently matched at SHA-256 `456f803ffa64b8f37954e973bc1fedef8d0cfa070ba2d54cbe34c4a19cc37346`.
- Onchain evidence mismatch persists `frozen = true`; revoke persists `revoked = true`.
- Rust formatting and multi-job CI workflow.

## Android v0.14.0 local build

```text
Artifact: mobile/android/app/build/outputs/apk/debug/allowance-os-0.14.0-debug.apk
Size: 18,813,650 bytes
Version code: 15
Version name: 0.14.0
Android tests: 15 passed, 0 failed
SHA-256: 3aeb6479969d116d637858031bd8de07f0818c2e04f92ba447b5870919c51dcf
```

Published GitHub Actions APK:

```text
Artifact: allowance-os-0.13.0-debug.apk
Size: 18,813,650 bytes
SHA-256: d9d5f8e57ed358e09a780ca8d4097437fdb618a453a3beee72d67854a72c07b2
Authority: SHA256SUMS.txt attached to android-test-v0.13.0
```

Public QA release: https://github.com/0xCaptain888/allowance-os/releases/tag/android-test-v0.13.0

The v0.13.0 test release and its checksum were produced by GitHub Actions run `34462245120`. Local and CI debug APK hashes differ because they use different debug signing keys. Production distribution still requires one protected release signing key.

## External and production steps still required

- Test Seed Vault and Genesis Token on actual compatible Solana Mobile hardware.
- Build a production-signed APK and submit it through the Solana dApp Store Publisher Portal.
- Test all live mobile control broadcasts on physical devices and validate Seed Vault / Genesis Token on compatible Solana Mobile hardware.

The wallet/Memo evidence is recorded separately from program enforcement. Program deployment, real state transitions, and the SPL-token CPI settlement are recorded in `evidence/live-devnet-program.json`. The token is a project-created Devnet test mint and is not presented as canonical USDC.

The v2 source artifacts are recorded in `evidence/delegated-v2-source-build.json`; deployment, exact Ubuntu artifact match, authority-free settlement, recovery controls, role rotation, and revoke evidence are recorded in `evidence/live-devnet-v2.json`. The macOS and Ubuntu source-build hashes differ, so only the exact Ubuntu artifact is claimed to match the deployed binary.

## Production architecture gate

Delegated Settlement v2 is now **DEVNET DEPLOYED · FULL CONTROL MATRIX VERIFIED**. Its allowance-scoped SPL delegate PDA implements authority-free later settlement with executor/verifier authorization, sequential nonces, period rollover, recovery, role rotation, immutable evidence, and lifetime-bounded delegation. Android exposes the public v2 evidence and has tested state/control codecs, but not every v2 control is wired to live mobile broadcasting. A mature launch still requires canonical production assets, transactional production storage, independent review, multisig/timelocked upgrade authority, protected Android signing, physical-device QA, and Mainnet rollout controls. See `docs/delegated-settlement-v2.md`, `docs/operations.md`, and `docs/product-maturity-audit.md`.
