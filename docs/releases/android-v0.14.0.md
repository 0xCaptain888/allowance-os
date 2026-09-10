# Allowance OS Android / SDK v0.14.0

Date: September 10, 2026  
Status: Devnet QA build; not production signed

## Headline

Delegated Settlement v2 is no longer only source-tested. The dedicated Program is deployed on Solana Devnet, its dumped binary exactly matches the public Ubuntu CI artifact, and a complete public matrix proves approve-once / settle-later behavior plus safety and recovery controls.

## Public v2 proof

- Program: `7zARKWKDLawLgR7qokvQdkAv6ye2cXGNEvNQswBd6xvL`
- Authority-free later charge: executor + verifier sign; the authority does not sign
- Exactly `1,000,000` raw project test-token units move per accepted charge
- BLOCKED: Custom Error `6`, zero token movement, no Evidence Record
- FROZEN: immutable bad-result Evidence Record PDA
- Dual-signature unfreeze
- Executor and verifier rotation followed by another authority-free charge
- REVOKED: final Program state and SPL delegate removal
- Deployed binary SHA-256 exactly matches public Ubuntu CI artifact: `45a3996ee011587e394951ee344742290c0aa7d0d132d972cb0168360df191dc`

The full record is `evidence/live-devnet-v2.json`.

## Android changes

- bumped version to `0.14.0` / code `15`;
- replaced the obsolete source-only v2 state with `DEVNET VERIFIED`;
- added direct Explorer entry points for the deployed v2 Program, authority-free charge, and revoke/delegate-removal proof;
- added an explicit delegated-v2 truth-boundary row;
- retained the honest limitation that not every v2 control encoder is wired to a live mobile broadcast flow.

## Runner reliability

- confirmation uses HTTP polling instead of requiring WebSocket `signatureSubscribe`;
- `DELEGATED_ALLOWANCE` and `CREATE_DELEGATED_SIGNATURE` support safe resume;
- resume validates existing actors and assets and derives the next nonce from onchain state;
- `devnet:delegated-matrix` verifies the full public control matrix.

## Truth boundary

The deployment is on Solana Devnet and uses a project-created test mint, not canonical USDC. The Program remains upgradeable under a single development authority and has not received an independent audit. This release does not claim Mainnet or production readiness. The APK is a debug-signed QA artifact, not a dApp Store production release.

## Public QA artifact

- Release: `android-test-v0.14.0`
- APK: `allowance-os-0.14.0-debug.apk`
- Size: `18,813,650` bytes
- CI SHA-256: `d5fae3fecefc5260460ad4e22284e1e08375bf06a41cdb1b096198cb6c7a25ef`
- GitHub Actions run: `34474593759`

The local and CI debug APK hashes differ because each environment uses its own debug signing key.
