# Allowance OS Android v0.17.4

## QA hotfix · 2026-09-12

v0.17.4 supersedes v0.17.3 for all Android device testing.

- Restores Solflare and compatible Mobile Wallet Adapter authorization.
- Supplies `favicon-ao-v017.svg` as a URI relative to the public `identityUri`, satisfying MWA's `iconRelativeUri` contract.
- Adds a regression test that verifies the identity URI is absolute, the icon URI is relative, and the combined public URL resolves correctly.
- Preserves all v0.17.3 policy, evidence, UI, commercial-flow, and Delegated Settlement v2 behavior.

## Verification

- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`
- `npm run typecheck`
- `npm test`
- `npm run preflight`

This is a debug-signed Solana Devnet QA build. It does not claim canonical USDC settlement, Mainnet deployment, production signing, or an independent security audit.
