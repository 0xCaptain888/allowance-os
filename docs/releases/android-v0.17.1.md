# Allowance OS Android / SDK v0.17.1

Date: September 12, 2026  
Status: debug-signed Solana Devnet QA patch

## Solflare Wallet entry point

- The Home page wallet authorization card now leads with **Solflare Wallet / MWA Wallet** instead of Phantom.
- The connection implementation remains generic MWA, so Solflare or another compatible MWA wallet can handle the authorization request.
- Local-session copy is provider-neutral and continues to make clear that forgetting a session does not change wallet keys, assets, or onchain state.

## Verification

- TypeScript typecheck: passed;
- Android unit/build command: `testDebugUnitTest assembleDebug` completed successfully;
- APK: `allowance-os-0.17.1-debug.apk`;
- size: `18,867,348 bytes`;
- SHA-256: `25088bc5f39d345ddb512d22a97e138630a2ab5d642c3c88e23b95fe9b6819c5`;
- public QA release: [`android-test-v0.17.1`](https://github.com/0xCaptain888/allowance-os/releases/tag/android-test-v0.17.1).

## Truth boundary

This is a debug-signed Solana Devnet QA build. It does not claim Mainnet deployment, canonical USDC settlement, production signing, or a security audit. The Home label change does not alter wallet custody or the MWA authorization flow.
