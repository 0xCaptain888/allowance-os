# Allowance OS Android v0.17.3

## QA release · 2026-09-12

v0.17.3 is a reliability and truthfulness pass over the v0.17 fintech control-center rebuild.

- Service-scoped audit accounting prevents AlphaBrief settlement from appearing as spend for another selected service.
- The current Devnet experience consistently labels the project-created mint as `TEST`; canonical USDC remains a production target only.
- Notification permission is requested contextually, not at launch. Denial never blocks the in-app safety check.
- Persistent in-app feedback confirms wallet authorization, policy changes, daily checks, pause/resume, refresh, Memo broadcast, clipboard actions, and audit clearing.
- Home shows `LOCAL PAUSE ON` prominently and offers Resume next to the status.
- Delegated v2 controls are disabled until live state is loaded, the authority wallet is connected, and the allowance is not terminally revoked. Revoked evidence is inspect-only.
- Allowance center shows inline Memo broadcast success with signature and explorer access.
- Wallet advanced Program/v2/truth-boundary sections are collapsed by default for a shorter judge path.

## Verification

- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`
- `npm run typecheck`
- `npm test`

This is a debug-signed Solana Devnet QA build. It does not claim canonical USDC settlement, Mainnet deployment, production signing, or an independent security audit.
