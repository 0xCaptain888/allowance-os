# Changelog

All notable Allowance OS Android releases are recorded here. Installable APK files and checksums are attached to the matching [GitHub Releases](https://github.com/0xCaptain888/allowance-os/releases).

## Android / SDK v0.12.0 — 2026-09-10 (release candidate)

- Replaced SDK floating-point payment fields with mint-bound base-10 integer raw-unit strings.
- Bumped merchant receipts from schema v2 to v3 so consumers cannot silently mix numeric display amounts with raw-unit strings.
- Added exact token mint and decimals binding to policies, requests, checks, and receipts.
- Rejects zero, negative, decimal, scientific-notation, malformed, and `u64`-overflow raw amounts.
- Freezes policies with invalid token metadata or invalid raw budget boundaries.
- Replaced shallow JSON hashing with recursively canonical JSON that covers nested values and ignores object-key insertion order.
- Uses canonical request hashes for idempotency and canonical payloads for webhook HMAC signatures.
- Labels Android sliders and projected spend as local demonstrations rather than authoritative chain accounting.
- Expanded TypeScript coverage from 18 to 21 tests.

Release status: source and local debug build verified; production tag not created.

## Android v0.11.1 — 2026-09-10 (release candidate)

- A restored wallet now requires both a stored public identity and a valid encrypted MWA reconnect token.
- Keystore recovery failure clears stale public connection state and explicitly requires reauthorization.
- Encrypted session writes and public-identity writes fail closed instead of silently claiming a durable session.
- Renamed the internal wallet action from `revoke` to `disconnectWalletSession` so it cannot be confused with Program revocation.
- Added lifecycle-aware StateFlow collection and a regression test for wallet restoration truth.
- Read the visible app version from Gradle-generated build metadata.
- Reserved `android-v*` tags for protected production-signed APKs and introduced `android-test-v*` for clearly labelled debug QA builds.
- Removed the hardcoded APK version from the production build script.

Release status: source and local debug build verified; production tag not created.

## Unreleased — Delegated Settlement v2 source

- Added backward-compatible Program v2 instructions without changing legacy v1 Borsh discriminants.
- Added an allowance-scoped SPL delegate PDA so later settlement does not require the user authority signer.
- Added four-way authority/merchant/executor/verifier separation, sequential nonces, and per-charge, rolling-period, and lifetime caps.
- Added immutable Evidence Record PDAs keyed by allowance plus the complete evidence hash; accepted charges and freezes now preserve full historical replay evidence onchain.
- Safely initializes a system-owned Evidence PDA even if an attacker pre-funds it, preventing predictable-address dusting from becoming a denial-of-service vector.
- Added authority-controlled executor rotation and authority+current-verifier controlled verifier rotation without changing legacy discriminants.
- Added user pause/unpause, verifier evidence-bound freeze, authority+verifier unfreeze, and terminal revoke that removes the SPL Token delegation.
- Added matching TypeScript instruction builders and expanded coverage to 22 TypeScript and 20 Rust tests.
- Produced a current `130,280`-byte macOS Solana SBF binary with SHA-256 `e0eb4726bdfda25fa2a377f347b9590087072e4ad1d9e455598760f6b865e7d6`; independent CI builds remain separately recorded without claiming cross-platform byte reproducibility.
- Added a pinned CI SBF build job that uploads the deployable `.so` as a workflow artifact.
- Added the full v2 architecture, account model, threat boundary, deployment checklist, and explicit `SOURCE TESTED · NOT DEPLOYED` label.

## Android v0.11.0 — 2026-09-10

- Encrypted the MWA reconnect token with an app-scoped Android Keystore AES-GCM key and added migration from the legacy plaintext preference.
- Added explicit review dialogs that distinguish a non-payment Devnet Memo proof from USDC movement and distinguish MWA disconnect from onchain revocation.
- Persisted the Chinese/English language choice across restarts.
- Rejected zero, negative, non-finite and invalid-budget requests in both TypeScript and Android policy engines.
- Disabled cleartext Android traffic and added an adaptive launcher icon.
- Removed the portrait-only restriction, added back-to-home navigation, and improved small-label readability for tablets and foldables.
- Added a fail-closed production signing configuration and `npm run android:release` build path.
- Added a candid product maturity audit and corrected browser revoke semantics.

Release tag: `android-v0.11.0`

## Android v0.10.0 — 2026-09-10

- Added a complete AlphaBrief paid-research flow across the SDK, Android app, and public Demo.
- Added request IDs, nonces, five-minute expiry, evidence URI/type, and v2 portable receipts.
- Added idempotent retries, evidence-replay rejection, and HMAC-signed merchant webhooks.
- Added Rust duplicate-evidence rejection as Custom Error `14` with a sixth native test.
- Added security, privacy, SDK integration, and dApp Store submission documentation.
- Increased TypeScript coverage from 8 to 14 tests and Android coverage from 8 to 10 tests.

Release tag: `android-v0.10.0`

## Android v0.9.0 — 2026-09-10

- Reframed Allowance OS as a reusable Web3 payment-authorization layer.
- Added five commercial allowance templates: AI Agent subscriptions, research reports, trading signals, automated trading bots, and paid APIs.
- Added service filtering, template application, service-specific policy limits, and projected period spend.
- Added the Seeker Integration Lab with explicitly unofficial blueprints for apps featured by Solana Mobile.
- Rebuilt the Android visual system and navigation around Home, Services, Allowances, Activity, and Evidence.
- Redesigned the public GitHub Pages demo with the same commercial catalog.
- Increased Android policy coverage from six to eight tests.

Release tag: `android-v0.9.0`

## Android v0.8.0 — 2026-09-09

- Added real Program-enforced SPL-token settlement on Solana Devnet.
- VERIFIED transfers exactly `1,000,000` raw test-token units through CPI.
- BLOCKED and FROZEN paths publicly demonstrate zero token movement.
- Added in-app Program matrix verification and public balance-delta verification.
- Preserved MWA connect, reconnect, local-session reset, and deauthorization controls.

Release tag: `android-v0.8.0`

## Android v0.7.0 — 2026-09-09

- Added deployed Program evidence boundaries and five-transaction verification.
- Added direct Solana RPC verification, period-cap controls, Activity Log, and portable receipt fingerprints.

No archived APK was available when the permanent release process was introduced.

## Release integrity rule

Starting with v0.11.1, an Android production version is not considered published until all of the following exist:

1. versioned source commit;
2. protected `android-vX.Y.Z` Git tag;
3. GitHub Release;
4. production-signed versioned APK asset;
5. `SHA256SUMS.txt`;
6. test/build result;
7. changelog entry.
