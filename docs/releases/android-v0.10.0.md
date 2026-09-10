# Allowance OS Android v0.10.0

Release date: September 10, 2026

## What changed

- added an end-to-end AlphaBrief paid research integration;
- content is hashed before payment and unlocks only after VERIFIED;
- added request ID, nonce, five-minute expiry, evidence URI, and evidence hash to portable receipts;
- added idempotent retry behavior and an interactive evidence-replay rejection;
- expanded Android policy coverage to 10 tests;
- added merchant SDK, signed webhook, privacy, security, and dApp Store submission documentation.

## Published GitHub Release asset

```text
Artifact: allowance-os-0.10.0-debug.apk
Size: 18,778,352 bytes
SHA-256: c583ddba4ee5f2596649b98133b67cf36fc2a9ea43e514ea104616e2c765af18
Checksum authority: SHA256SUMS.txt attached to android-v0.10.0
```

## Local verification build

```text
Artifact: allowance-os-0.10.0-debug.apk
Size: 18,975,074 bytes
Android tests: 10 passed, 0 failed
SHA-256: 8fc1b16e2ecae5bd451bc2110a8093c6f0b0992f8ce87ee00524a9e72c455be3
```

This is a debug APK for testing and judging, not a production-signed dApp Store release.

The local and CI hashes differ because each environment uses its own Android debug signing key. The public Release asset must be checked against its attached `SHA256SUMS.txt`; a production release will use one protected release signing key.
