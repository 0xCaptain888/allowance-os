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
- Android Keystore AES-GCM encryption for the MWA reconnect token, with migration from the legacy plaintext preference.
- Explicit wallet-action review and distinct MWA disconnect versus onchain-revoke language.
- Persistent Chinese/English preference, cleartext-traffic denial, and adaptive launcher icon.
- Fail-closed production signing configuration and local signed-release builder.
- Android policy tests and successful debug APK build.
- Sixteen passing TypeScript tests, including malformed-amount, invalid-budget, idempotency, stale-request, signed-webhook, tamper, live-adapter, and replay coverage.
- npm dependency audit: 0 known vulnerabilities after compatible transitive overrides.
- Native Solana program source compiled with six passing Rust tests, including duplicate-evidence rejection.
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

Previous published GitHub Actions APK:

```text
Artifact: allowance-os-0.10.0-debug.apk
Size: 18,778,352 bytes
SHA-256: c583ddba4ee5f2596649b98133b67cf36fc2a9ea43e514ea104616e2c765af18
Authority: SHA256SUMS.txt attached to android-v0.10.0
```

The public v0.10.0 APK remains the latest downloadable GitHub Release until the v0.11.0 tag workflow completes. Local and CI debug APK hashes can differ because they use different debug signing keys. Production distribution requires one protected release signing key.

## External hardware step still required

- Test Seed Vault and Genesis Token on actual compatible Solana Mobile hardware.
- Build a production-signed APK and submit it through the Solana dApp Store Publisher Portal.
- Upgrade the deployed Devnet Program after the ETJL7fK6… upgrade-authority signer is available; until then the live evidence remains the disclosed v0.9.0 binary.

The wallet/Memo evidence is recorded separately from program enforcement. Program deployment, real state transitions, and the SPL-token CPI settlement are recorded in `evidence/live-devnet-program.json`. The token is a project-created Devnet test mint and is not presented as canonical USDC.

## Production architecture gate

The current deployed Program requires the authority signer for every `Charge`. It therefore proves bounded onchain enforcement but does not yet implement autonomous post-approval recurring charges. A mature launch requires a user-approved SPL delegate or Program-owned vault/PDA, merchant/executor authorization, nonce accounts, period rollover, recovery, and multisig/timelocked upgrade authority. See `docs/product-maturity-audit.md`.
