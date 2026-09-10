# Contract and product security notes

## Enforced today

- merchant, token mint, allowed program, per-charge cap, period cap, expiry, authority, and evidence binding;
- SPL Token CPI occurs only after every Program check passes;
- identity/evidence mismatch freezes the allowance;
- authority can revoke the allowance;
- SDK receipt v3 binds every request to a unique request ID, nonce, issue time, expiry, exact mint, integer raw amount, evidence URI, evidence type, and evidence hash;
- identical request retries are idempotent;
- evidence reuse under a new SDK request is blocked;
- the Rust source rejects the last accepted evidence hash with Custom Error `14` (`DuplicateEvidence`).

## Important deployment boundary

The currently published Devnet evidence predates the Rust replay-protection and Delegated Settlement v2 source changes. The deployed Program remains the verified v0.9.0 evidence binary until the upgrade-authority wallet is available and a new binary is deployed and independently hashed. The TypeScript SDK and Android/browser reference flows enforce replay protection now; the repository does not claim that the old Devnet binary implements v2.

## Delegated Settlement v2 — source tested

- `CreateDelegated` approves only `lifetime_cap` tokens to a PDA derived from the allowance account;
- `ChargeDelegated` requires exact executor and verifier signatures but does not require the user authority;
- source token, mint, merchant destination, delegate PDA, sequential nonce, evidence hash, expiry, and all three caps are checked before CPI;
- the rolling period resets deterministically from the onchain clock while lifetime spend never resets;
- pause is user-controlled; freeze is verifier-controlled and stores the bad-result evidence hash;
- unfreeze requires both user and verifier; terminal revoke removes the SPL delegate through CPI;
- legacy v1 instruction discriminants remain stable.

Remaining v2 limits: only the last evidence hash is stored onchain, so the independent verifier must durably reject older evidence reuse; the verifier is a single configured signer rather than a quorum; token decimals and canonical asset selection remain deployment configuration; and the v2 source has not yet been deployed or independently audited.

## Production requirements

- move upgrade authority to a multisig or timelocked governance account;
- use a canonical production stablecoin mint and explicitly configured token decimals;
- persist nonce/idempotency state transactionally;
- deploy and independently verify the v2 period-window accounting and recovery implementation;
- define delayed or quorum-based unfreeze governance for higher-value allowances;
- protect webhook secrets in a managed secret store and rotate them;
- use a dedicated dApp Store release signing key;
- complete an independent Program and Android security review.

## Threat model

| Threat | Current response | Remaining production work |
| --- | --- | --- |
| Lookalike merchant | FROZEN before settlement | merchant registry and human-readable domain binding |
| Overspend | V1 live BLOCKED; v2 source adds three-level caps and rolling periods | deploy v2 and monitor cap invariants |
| Missing or changed delivery | V1 live FROZEN; v2 source separates verifier and records bad evidence | verifier quorum for high-value services |
| Network retry | stored receipt returned idempotently | transactional database |
| Evidence replay | SDK/Android/browser blocked; v2 source enforces sequential nonce and duplicate last hash | deploy v2 and retain a durable global verifier replay set or accumulator |
| Compromised executor | v2 requires a separate verifier signature and bounded policy | verifier quorum, key rotation, rate alerts |
| Compromised verifier | cannot change destination/caps; user can pause/revoke; unfreeze needs user | verifier rotation and quorum |
| Compromised upgrader | disclosed single upgrade authority | multisig/timelock |
| Lost mobile session | local forget + MWA deauthorize | account recovery UX |
