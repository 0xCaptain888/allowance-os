# Allowance OS Android / SDK v0.17.0

Date: September 11, 2026  
Status: debug-signed Solana Devnet QA release

## Fintech control-center UI

This release rebuilds the Android presentation around what a user and judge need to understand first:

```text
protected allowance
→ current and remaining budget
→ next evidence-gated charge
→ latest VERIFIED / BLOCKED / FROZEN decision
→ wallet and independent onchain verification
```

Highlights:

- AO Monogram header and consistent charcoal / ivory / lime / cobalt design tokens;
- English-first fresh-install experience with saved Chinese preference;
- simplified Home control center with duplicate dashboard cards removed;
- accessible Material icon navigation and 48dp-or-larger interactive controls;
- Service → Budget → Evidence → Review allowance sequence;
- consequence-led decision surfaces explaining whether the wallet opened and whether funds moved;
- dedicated Wallet & Verification destination;
- centered content boundary for tablet, landscape, and judge-screen layouts.
- matching responsive Judge Demo with the same AO palette, protected-allowance hierarchy, state semantics, and mobile navigation;
- preserved browser-side policy controls and direct Solana RPC verification with explicit simulation/live-evidence boundaries.

## Verification

- TypeScript: 26 passed;
- Android unit tests: 18 passed;
- `npm run preflight`: all checks READY;
- `npm run android:build`: successful;
- APK: `allowance-os-0.17.0-debug.apk`;
- exact size and SHA-256: [`evidence/android-build.json`](../../evidence/android-build.json).
- public QA release: [`android-test-v0.17.0`](https://github.com/0xCaptain888/allowance-os/releases/tag/android-test-v0.17.0).

## Truth boundary

This is a debug-signed Devnet QA build. The UI redesign does not change the custody model, create a Mainnet deployment, convert the project test mint into canonical USDC, or replace the need for an independent security audit.
