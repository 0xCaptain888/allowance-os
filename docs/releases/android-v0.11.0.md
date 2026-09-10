# Allowance OS Android v0.11.0

Date: September 10, 2026

## Trust-boundary release

- Android Keystore AES-GCM protection for the MWA reconnect token;
- migration away from the legacy plaintext reconnect-token preference;
- explicit `0 USDC` review before a Devnet Memo proof opens the wallet;
- clear separation between MWA disconnect and onchain allowance revocation;
- persisted Chinese/English language choice;
- positive finite amount and valid-budget enforcement in Android and TypeScript;
- cleartext network traffic disabled;
- adaptive Allowance OS launcher icon;
- resizable Android activity, back-to-home navigation, and improved small-label readability;
- production signing configuration and fail-closed local release builder;
- public product maturity audit and corrected browser truth labels.

## Verification target

```text
TypeScript: 16 tests
Android: 12 tests
Rust: 6 tests
Production APK: not created until the owner generates and protects the release key
```

The GitHub Actions asset for this tag remains a reproducible debug APK for hackathon testing. It is not the production-signed dApp Store artifact. The production APK must be built locally with the protected long-lived signing identity and verified separately.
