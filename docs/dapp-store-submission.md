# Solana dApp Store submission pack

Status: **prepared, not submitted**.

Official publishing guidance requires a release-ready signed APK, listing metadata, preview assets, a publisher account/wallet, and policy review. The app's current debug APK is for testing and must not be submitted as the production release.

## Listing draft

- App name: `Allowance OS`
- Package: `com.captain.allowanceos`
- Category: Finance / Developer Tools
- Short description: `Approve bounded Web3 service budgets, verify every charge, and revoke anytime.`
- Full description: `Allowance OS is a local-first Solana Mobile authorization layer for AI subscriptions, paid research, trading signals, automation services, and metered APIs. Users choose a merchant, token, program, per-charge cap, period cap, expiry, and required delivery evidence. Requests are VERIFIED, BLOCKED, or FROZEN before payment. The Android client supports MWA, public Devnet verification, reusable service templates, signed receipt export, and explicit revocation.`
- Support URL: `https://github.com/0xCaptain888/allowance-os/issues`
- Website: `https://0xcaptain888.github.io/allowance-os/`
- Privacy policy: `docs/privacy-policy.md`
- Version: `0.11.0` / code `11`

## Required preview asset plan

Capture these from the final signed release on a supported Android device:

1. Home: connected wallet and selected service;
2. Services: five commercial templates;
3. Allowance: adjustable policy and VERIFIED result;
4. AlphaBrief: report unlocked after content-hash verification;
5. Replay attack: evidence reuse BLOCKED;
6. Evidence: live Devnet matrix and truth boundary;
7. Activity: local audit trail;
8. Settings/session controls: deauthorize and forget local session.

## Permissions and data safety

- Internet: required for wallet association and Solana RPC reads/broadcasts.
- No location, contacts, camera, microphone, SMS, or storage permission requested.
- No seed phrase or private key access.
- Public address, wallet label, auth token, signatures, preferences, and audit events remain in Android app-local storage.

## External completion checklist

- create/complete Publisher Portal account and KYC/KYB;
- generate and securely back up a dedicated release signing key;
- supply its path, alias, and passwords only through the local signing environment described in `mobile/README.md`;
- build a signed release APK with the same key used for every future update;
- capture final device screenshots and icon assets;
- fund the publisher wallet for upload and onchain publishing costs;
- review the current Publisher Policy and Developer Agreement;
- submit through the Publisher Portal and complete all wallet signatures;
- test the submitted build on actual Seeker hardware when available.

Official references:

- https://docs.solanamobile.com/dapp-store/submit-new-app
- https://docs.solanamobile.com/dapp-store/publisher-policy
- https://docs.solanamobile.com/dapp-store/publishing_releases
