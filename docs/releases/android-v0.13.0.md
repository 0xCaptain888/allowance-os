# Allowance OS Android / SDK v0.13.0

Date: September 10, 2026

## Merchant durability and webhook safety

- added a pluggable `RuntimeStateStore` boundary;
- added atomic single-process JSON persistence for policies, idempotency records, nonces, evidence hashes, spending, and revocation state;
- added timestamp-tolerant webhook verification, replay rejection, and overlapping-secret rotation;
- documented the boundary between this local reference store and a production transactional database.

## Android delegated-settlement readiness

- added a strict 332-byte Delegated Settlement v2 state decoder;
- validates version, actor identities, three budget caps, spend invariants, nonce, lifecycle flags, and evidence hashes;
- added native Android instruction encoders for pause, unpause, revoke, executor rotation, and verifier rotation;
- added an Evidence screen capability card that keeps these source-tested controls visibly separate from live deployment claims;
- added portable JSON export and SHA-256 fingerprinting for the local Activity audit trail;
- added configurable Devnet RPC selection with bounded retry behavior.

## Verification

```text
TypeScript: 24 passed, 0 failed
Android: 15 passed, 0 failed
Rust: 20 passed, 0 failed (unchanged v2 Program suite)
APK: mobile/android/app/build/outputs/apk/debug/allowance-os-0.13.0-debug.apk
Size: 18,813,650 bytes
SHA-256: a35e6d3d7182b90c20cf814417018a13420ff19d4c8a833e2827928c48d77a03
```

## Public QA release

The `android-test-v0.13.0` GitHub Actions release publishes the same source version as a CI debug build:

```text
Artifact: allowance-os-0.13.0-debug.apk
Size: 18,813,650 bytes
CI SHA-256: d9d5f8e57ed358e09a780ca8d4097437fdb618a453a3beee72d67854a72c07b2
Workflow run: 34462245120
```

Release: https://github.com/0xCaptain888/allowance-os/releases/tag/android-test-v0.13.0

The local and CI hashes differ because the two builds use different Android debug signing keys; both remain non-production artifacts.

## Truth boundary

This is a debug-signed Devnet QA build. The v2 Android codecs are tested, but Delegated Settlement v2 is still not deployed and the controls are not presented as live. A production release still requires protected signing, device QA, policy review, and dApp Store submission.
