# Allowance OS Android / SDK v0.16.0

Date: September 11, 2026  
Status: public debug-signed Devnet QA release candidate

## Live v2 control surface

Android now includes the complete client-side path for delegated v2 controls:

```text
inspect allowance through RPC
→ review network, Program, authority, and action
→ request MWA wallet signature
→ broadcast Pause / Unpause / Revoke
→ record transaction signature
→ re-read and display post-state
```

Safety boundaries:

- only the configured v2 authority address can reach the wallet signing step;
- a different connected wallet is rejected before MWA is invoked;
- the allowance account owner is checked against the deployed v2 Program ID;
- the public evidence allowance is already revoked, so no new public control transaction is claimed by this release;
- all actions are Solana Devnet operations and may consume a small amount of Devnet SOL;
- the app accepts a comma-separated `allowanceOsSolanaRpcUrls` Gradle property and fails over across distinct HTTP RPC endpoints;
- no private key is held by the app.

## Verification

- TypeScript: 26 passed;
- Android unit tests: 18 passed;
- Rust Program: 20 passed in the latest CI matrix;
- `npm run preflight`: all checks READY;
- `./gradlew testDebugUnitTest assembleDebug --no-daemon`: successful.
- local APK: `allowance-os-0.16.0-debug.apk`, SHA-256 `5f4f6b9d16396ae5076737866ccac74ef2a68ca8422845c85607f69a7d93186c`.

## Truth boundary

This is a debug-signed QA build, not a production dApp Store release. The deployed Program and public commercial proofs remain Solana Devnet evidence using a project-created test mint, not canonical USDC or a Mainnet production claim. A real control transaction still requires the authority wallet and an allowance that is not already terminally revoked.
