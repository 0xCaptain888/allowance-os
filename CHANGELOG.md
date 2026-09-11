# Changelog

## Android / SDK v0.17.0 — 2026-09-11

- Rebuilt Android around an English-first fintech control-center hierarchy instead of a long hackathon dashboard.
- Added an AO Monogram product header, a high-signal active-allowance hero, and dedicated wallet/session status.
- Reduced duplicate Home content so budget, next charge, anomaly state, and latest payment decision appear first.
- Replaced Unicode navigation marks with accessible Material icons and renamed the final destination to Wallet.
- Turned policy review into a visible Service → Budget → Evidence → Review sequence.
- Redesigned VERIFIED, BLOCKED, FROZEN, and REVOKED results around user consequences such as wallet not opened and zero funds moved.
- Removed decorative gradients from core cards, adopted the charcoal / ivory / lime / cobalt AO palette, and standardized spacing, radii, and 48dp-or-larger touch targets.
- Added a centered 760dp content boundary for more credible tablet, landscape, and judge-screen presentation.
- Kept Chinese as an optional saved preference while making English the default on a fresh install.

This is a UX and information-architecture release. Protocol behavior, wallet custody boundaries, and existing Devnet evidence are unchanged.

## Android / SDK v0.16.1 — 2026-09-11

- Replaced the generic launcher artwork with the AO Monogram brand mark across the Android launcher, public Demo favicon, Demo header, and README.
- Updated the launcher background palette to the new charcoal / ivory / lime / cobalt system.
- Built and verified a new debug-signed Devnet QA APK as version code `18` / version name `0.16.1`.

The new Logo is a visual identity refresh only: protocol behavior, wallet boundaries, Devnet disclosures, and the evidence model are unchanged.

## Android / SDK v0.16.0 — 2026-09-11

- Added a live Delegated Settlement v2 control surface in Android: RPC inspection, explicit transaction review, MWA broadcast, confirmation, and post-broadcast state refresh.
- Added authority-bound Pause, Unpause, and Revoke actions with a fail-closed wallet identity check; a non-authority wallet is blocked before MWA is invoked.
- Added v2 control signatures and resulting state to the local Activity audit trail.
- Added deployed v2 allowance constants and an RPC decoder path that rejects an unexpected Program owner or malformed state.
- Added Android unit/build verification for the new control path and bumped Android to version code `17` / version name `0.16.0`.

The public evidence allowance is already revoked, so the new controls are an honest integration path for a compatible authority-owned Devnet allowance rather than a claim that a fresh control transaction was broadcast from this QA build. The APK remains debug-signed and Devnet-only.

## Android / SDK v0.15.0 — 2026-09-10

- Turned AlphaBrief into a complete live commercial chain: purchase, substantive report delivery, deterministic evidence hash, independent verification, Delegated Settlement v2 settlement, notification intent, and bad-output freeze.
- Published a real authority-free AlphaBrief settlement of `2,000,000` raw project test-token units and a later verifier freeze with zero token movement.
- Added `evidence/live-alphabrief-v2.json`, the delivered report, an independent verifier, a reproducible live runner, and two new TypeScript tests.
- Rebuilt Android Home as Daily Habits with upcoming charges, today/week spend, budget pressure, merchant anomaly alerts, one-tap local Pause, delivery/payment timeline, Android notifications, and an exportable weekly safety report.
- Added three Android Daily Habits tests and bumped Android to version code `16` / version name `0.15.0`.
- Updated the public Demo to lead with the real AlphaBrief commerce chain and a browser-safe Daily Habits preview.
- Kept boundaries explicit: browser and local dashboard actions do not move funds; Pause is local until an authority broadcasts the v2 pause instruction; the live asset is a project-created Devnet test mint, not canonical USDC.

Release tag: `android-test-v0.15.0`. The published APK is debug-signed for Devnet QA; no production-signed release is claimed.

## Android / SDK v0.14.0 — 2026-09-10

- Deployed Delegated Settlement v2 to Solana Devnet at `7zARKWKDLawLgR7qokvQdkAv6ye2cXGNEvNQswBd6xvL`.
- Dumped the deployed Program and exactly matched its `130,280` bytes to the public Ubuntu CI artifact at SHA-256 `45a3996ee011587e394951ee344742290c0aa7d0d132d972cb0168360df191dc`.
- Published a real `CreateDelegated` plus later `ChargeDelegated` flow signed by executor and verifier without the user authority; `1,000,000` raw test-token units moved.
- Published a full control matrix for BLOCKED, pause/unpause, immutable-evidence freeze, dual-signature unfreeze, executor/verifier rotation, rotated-role settlement, and revoke with SPL delegate removal.
- Added safe runner resume and HTTP-only confirmation polling for private RPC providers without WebSocket `signatureSubscribe`.
- Added machine-readable `evidence/live-devnet-v2.json`, updated the public Judge Demo, and surfaced v2 proof links in Android.
- Bumped Android to version code `15` / version name `0.14.0`.

This remains a debug-signed Devnet QA milestone using a project-created test mint, not canonical USDC. It is not an audit, Mainnet deployment, or production-readiness claim.

All notable Allowance OS Android releases are recorded here. Installable APK files and checksums are attached to the matching [GitHub Releases](https://github.com/0xCaptain888/allowance-os/releases).

## Android / SDK v0.13.0 — 2026-09-10 (public test release)

- Added atomic JSON-backed reference persistence behind a pluggable merchant runtime store.
- Preserves policies, spend, idempotency, nonces, used evidence, and revocation across process restarts.
- Added merchant webhook timestamp tolerance, event replay rejection, and overlapping-secret rotation.
- Added an Android Delegated Settlement v2 state decoder and pause/unpause/revoke/operator-rotation instruction encoders.
- Added JSON export and SHA-256 fingerprinting for the device-local Activity audit trail.
- Added configurable Android Devnet RPC selection and bounded retries.
- Added a private-RPC option to the manual v2 GitHub deployment workflow.
- Added a responsible-disclosure policy, operational runbook, and machine-readable evidence index.
- Expanded coverage to 24 TypeScript, 15 Android, and 20 Rust tests.

Release tag: `android-test-v0.13.0`. The published APK is debug-signed for Devnet QA; it predates the live v2 deployment milestone, and no production tag has been created.

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

## Delegated Settlement v2 source and Devnet deployment

- Added backward-compatible Program v2 instructions without changing legacy v1 Borsh discriminants.
- Assigned a dedicated v2 Program identity so new deployment evidence cannot be confused with the live v1 binary.
- Added an allowance-scoped SPL delegate PDA so later settlement does not require the user authority signer.
- Added four-way authority/merchant/executor/verifier separation, sequential nonces, and per-charge, rolling-period, and lifetime caps.
- Added immutable Evidence Record PDAs keyed by allowance plus the complete evidence hash; accepted charges and freezes now preserve full historical replay evidence onchain.
- Safely initializes a system-owned Evidence PDA even if an attacker pre-funds it, preventing predictable-address dusting from becoming a denial-of-service vector.
- Added authority-controlled executor rotation and authority+current-verifier controlled verifier rotation without changing legacy discriminants.
- Added user pause/unpause, verifier evidence-bound freeze, authority+verifier unfreeze, and terminal revoke that removes the SPL Token delegation.
- Added matching TypeScript instruction builders and expanded coverage to 22 TypeScript and 20 Rust tests.
- Produced current `130,280`-byte Solana SBF binaries on macOS (`e0eb4726…e7d6`) and Ubuntu CI (`45a3996e…91dc`); both host-specific hashes are recorded without claiming cross-platform byte reproducibility.
- Added a pinned CI SBF build job that uploads the deployable `.so` as a workflow artifact.
- Added the full v2 architecture, account model, threat boundary, deployment checklist, and precise Devnet verification boundary.

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
