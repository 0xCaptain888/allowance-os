# Allowance OS Android v0.11.1

## Wallet-session and release-boundary hardening

- A restored wallet is now considered connected only when both its public identity and encrypted MWA reconnect token are available.
- Stale wallet identities are cleared when Android Keystore recovery fails, and the UI explicitly requests reauthorization.
- Encrypted session persistence now fails closed instead of silently presenting an unsaved session as durable.
- Wallet disconnection is named separately from onchain allowance revocation in application code.
- StateFlow collection is lifecycle-aware.
- The displayed app version is read from the build configuration instead of being hardcoded.
- Production tags now require protected release signing and create release APKs; debug builds use a separate, explicitly labelled test-release workflow.
- The production build script discovers the Gradle-generated APK dynamically instead of hardcoding a versioned path.

## Truth boundary

This release remains a Solana Devnet pre-production client. Delegated Settlement v2 is compiled and tested in source but is not yet deployed or integrated into the Android transaction flow.
