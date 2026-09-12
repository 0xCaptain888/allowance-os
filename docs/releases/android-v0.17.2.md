# Allowance OS Android / SDK v0.17.2

Date: September 12, 2026  
Status: debug-signed Solana Devnet QA interaction fix

## What was fixed

- Local policy replays (`VERIFIED`, `BLOCKED`, and `FROZEN`) remain usable while live app-side requests are paused.
- A visible **Reset interactive demo** button clears a persisted local Pause and restores safe policy inputs.
- The Allowance center shows a prominent paused-state explanation and Resume action.
- The UI explains that “limit” means the selected service's policy cap, not an insufficient wallet balance; `BLOCKED` intentionally submits an oversized request.
- Amount and period-spend sliders are continuous, keeping low-cap templates such as DataPipe testable.
- One-tap actions restore the exact per-charge cap and clear period spend.
- MWA connect, broadcast, disconnect, and v2 control calls now recover after a 90-second timeout or unexpected exception instead of leaving an input-blocking loading overlay.
- Remaining wallet-session copy now leads with Solflare Wallet while retaining support for any compatible MWA wallet.

## Verification

- Android command: `./gradlew testDebugUnitTest assembleDebug`;
- result: `BUILD SUCCESSFUL`;
- tests: `19` passed, `0` failed;
- APK: `allowance-os-0.17.2-debug.apk`;
- size: `19,173,846 bytes`;
- SHA-256: `74f39f01795e19c6fa8ce8084d8481e3c29a5d0579cb06ef4e0bb498bfd3fdc1`.

## Truth boundary

This is a debug-signed Solana Devnet QA build. Policy outcomes on the interactive screen are local replays; real wallet Memo proof and public Program evidence remain explicitly identified separately. This release does not claim Mainnet deployment, canonical USDC settlement, production signing, or a security audit.
