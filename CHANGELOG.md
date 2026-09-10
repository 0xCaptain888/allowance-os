# Changelog

All notable Allowance OS Android releases are recorded here. Installable APK files and checksums are attached to the matching [GitHub Releases](https://github.com/0xCaptain888/allowance-os/releases).

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

Starting with v0.9.0, an Android version is not considered published until all of the following exist:

1. versioned source commit;
2. `android-vX.Y.Z` Git tag;
3. GitHub Release;
4. versioned APK asset;
5. `SHA256SUMS.txt`;
6. test/build result;
7. changelog entry.
