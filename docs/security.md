# Contract and product security notes

## Enforced today

- merchant, token mint, allowed program, per-charge cap, period cap, expiry, authority, and evidence binding;
- SPL Token CPI occurs only after every Program check passes;
- identity/evidence mismatch freezes the allowance;
- authority can revoke the allowance;
- SDK v2 binds every request to a unique request ID, nonce, issue time, expiry, evidence URI, evidence type, and evidence hash;
- identical request retries are idempotent;
- evidence reuse under a new SDK request is blocked;
- the Rust source rejects the last accepted evidence hash with Custom Error `14` (`DuplicateEvidence`).

## Important deployment boundary

The currently published Devnet evidence predates the Rust replay-protection source change. The deployed Program remains the verified v0.9.0 evidence binary until the upgrade-authority wallet is available and a new binary is deployed and independently hashed. The TypeScript SDK and Android/browser reference flows enforce replay protection now; the repository does not claim that the old Devnet binary does.

## Production requirements

- move upgrade authority to a multisig or timelocked governance account;
- use a canonical production stablecoin mint and explicitly configured token decimals;
- persist nonce/idempotency state transactionally;
- add a period-window reset instruction or epoch-based accounting;
- add a pause/recovery flow with a delayed, auditable unfreeze decision;
- protect webhook secrets in a managed secret store and rotate them;
- use a dedicated dApp Store release signing key;
- complete an independent Program and Android security review.

## Threat model

| Threat | Current response | Remaining production work |
| --- | --- | --- |
| Lookalike merchant | FROZEN before settlement | merchant registry and human-readable domain binding |
| Overspend | BLOCKED before wallet/Program transfer | durable period rollover |
| Missing or changed delivery | FROZEN | external verifier quorum for high-value services |
| Network retry | stored receipt returned idempotently | transactional database |
| Evidence replay | SDK/Android/browser blocked; Rust source rejects duplicate last hash | deploy upgrade and retain a larger replay set or nonce account |
| Compromised upgrader | disclosed single upgrade authority | multisig/timelock |
| Lost mobile session | local forget + MWA deauthorize | account recovery UX |
