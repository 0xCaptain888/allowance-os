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
- Direct Solana Devnet RPC verification for the current or recorded live signature.
- Android Evidence Center verification of the complete Program transaction matrix and final account state.
- Public judge demo with read-only live confirmation, slot, and execution-error checks.
- Official Solana Mobile Wallet Adapter 2.0.7 integration.
- Phantom/MWA authorize, `signAndSendTransactions`, explorer evidence, and deauthorize implementation.
- Android Keystore AES-GCM encryption for the MWA reconnect token, with migration from the legacy plaintext preference.
- Explicit wallet-action review and distinct MWA disconnect versus onchain-revoke language.
- Persistent Chinese/English preference, cleartext-traffic denial, and adaptive launcher icon.
- Fail-closed production signing configuration and local signed-release builder.
- Android policy tests and successful debug APK build.
- Eighteen passing TypeScript tests, including malformed-amount, invalid-budget, idempotency, stale-request, signed-webhook, tamper, live-adapter, replay, and delegated instruction coverage.
- npm dependency audit: 0 known vulnerabilities after compatible transitive overrides.
- Native Solana Program v0.2.0 source compiled with 16 passing Rust tests, including v1 compatibility, delegated settlement safety boundaries, and evidence-bound freeze validation.
- Solana SBF v2 binary built locally and in GitHub CI with pinned `cargo-build-sbf 4.3.0`; both artifacts are 92,704 bytes. macOS SHA-256: `9c36a9aaa91f40a9797c99876015ebcd2aaf284f796166719e2df1f19ee34fd5`; Ubuntu CI SHA-256: `5aa5d33d45557266800dd3941731eb8e607d47c0030d6fba3b5a4ec41c19b02e`.
- Upgradeable Program deployed to Solana Devnet at `DJzPBS7FreCcWWGkApzznGcKq9T7Da38GpKFtpxWRcuE`.
- Real Devnet allowance account with public `CREATED`, `VERIFIED`, `BLOCKED`, `FROZEN`, and `REVOKED` evidence.
- VERIFIED transfers exactly `1,000,000` raw SPL-token units through CPI; source balance changes `20 → 19`, merchant `0 → 1`.
- BLOCKED fails before CPI with Custom Error `6`; FROZEN and REVOKED persist without moving tokens.
- Local and deployed Program binaries independently matched at SHA-256 `456f803ffa64b8f37954e973bc1fedef8d0cfa070ba2d54cbe34c4a19cc37346`.
- Onchain evidence mismatch persists `frozen = true`; revoke persists `revoked = true`.
- Rust formatting and multi-job CI workflow.

## Android v0.11.0 local build

```text
Artifact: mobile/android/app/build/outputs/apk/debug/allowance-os-0.11.0-debug.apk
Size: 19,010,740 bytes
Version code: 11
Version name: 0.11.0
Android tests: 12 passed, 0 failed
SHA-256: 14d5ed6d41b47a5a7a921cb2e5f193c5fba4effc4911191e830be96926710fd2
```

Published GitHub Actions APK:

```text
Artifact: allowance-os-0.11.0-debug.apk
Size: 18,797,262 bytes
SHA-256: a1f8aac16f8e697297be292e414ed5fffdf27e3e7f8cc6e5068d75699b25390e
Authority: SHA256SUMS.txt attached to android-v0.11.0
```

The v0.11.0 tag workflow completed successfully on September 10, 2026. Local and CI debug APK hashes differ because they use different debug signing keys. Production distribution still requires one protected release signing key.

## External hardware step still required

- Test Seed Vault and Genesis Token on actual compatible Solana Mobile hardware.
- Build a production-signed APK and submit it through the Solana dApp Store Publisher Portal.
- Upgrade the deployed Devnet Program after the ETJL7fK6… upgrade-authority signer is available; until then the live evidence remains the disclosed v0.9.0 binary.

The wallet/Memo evidence is recorded separately from program enforcement. Program deployment, real state transitions, and the SPL-token CPI settlement are recorded in `evidence/live-devnet-program.json`. The token is a project-created Devnet test mint and is not presented as canonical USDC.

The v2 SBF artifacts are recorded separately in `evidence/delegated-v2-source-build.json`. They prove buildability only; no v2 deployment or authority-free settlement transaction is claimed yet. The macOS and Ubuntu hashes differ, so the exact artifact chosen for deployment must be hashed and matched to the deployed binary.

## Production architecture gate

Delegated Settlement v2 is now **SOURCE TESTED · NOT DEPLOYED**. Its allowance-scoped SPL delegate PDA implements authority-free later settlement with executor/verifier authorization, sequential nonces, period rollover, recovery, and lifetime-bounded delegation. The current deployed v0.9 Program still requires the authority signer for every `Charge`. A mature launch therefore still requires v2 deployment and public transaction evidence, Android integration, a durable verifier/evidence store, independent review, and multisig/timelocked upgrade authority. See `docs/delegated-settlement-v2.md` and `docs/product-maturity-audit.md`.
