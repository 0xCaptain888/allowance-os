# Allowance OS Android / SDK v0.16.1

Date: September 11, 2026  
Status: public debug-signed Devnet QA release candidate

## AO Monogram branding refresh

This release applies the approved AO Monogram to:

- Android launcher foreground and adaptive background;
- public Judge Demo favicon and navigation header;
- repository README brand preview.

Protocol behavior, wallet boundaries, Devnet evidence, and the v2 control surface are unchanged from v0.16.0.

## Verification

- TypeScript: 26 passed;
- Android unit tests: 18 passed;
- `npm run preflight`: all checks READY;
- `npm run android:build`: successful;
- APK: `allowance-os-0.16.1-debug.apk`;
- the exact local APK SHA-256 is recorded in [`evidence/android-build.json`](../../evidence/android-build.json).

This remains a debug-signed Devnet QA build, not a production dApp Store release.
