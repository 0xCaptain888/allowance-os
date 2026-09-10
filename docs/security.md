# Contract and product security notes

## Enforced today

- merchant, token mint, allowed program, per-charge cap, period cap, expiry, authority, and evidence binding;
- SPL Token CPI occurs only after every Program check passes;
- identity/evidence mismatch freezes the allowance;
- authority can revoke the allowance;
- SDK receipt v3 binds every request to a unique request ID, nonce, issue time, expiry, exact mint, integer raw amount, evidence URI, evidence type, and evidence hash;
- identical request retries are idempotent;
- evidence reuse under a new SDK request is blocked;
- the v2 Rust source creates a unique PDA for every accepted/frozen evidence hash, so any historical reuse fails because the record already exists.

## Important deployment boundary

The original v1 Program remains a separate historical proof. Delegated Settlement v2 is deployed at `7zARKWKDLawLgR7qokvQdkAv6ye2cXGNEvNQswBd6xvL`; its dumped binary exactly matches the public Ubuntu CI artifact and its full Devnet control matrix is recorded in `evidence/live-devnet-v2.json`. This does not convert the v1 binary into v2 and does not imply Mainnet readiness.

## Delegated Settlement v2 — Devnet verified

- `CreateDelegated` approves only `lifetime_cap` tokens to a PDA derived from the allowance account;
- `ChargeDelegated` requires exact executor and verifier signatures but does not require the user authority;
- source token, mint, merchant destination, delegate PDA, sequential nonce, evidence hash, expiry, and all three caps are checked before CPI;
- the rolling period resets deterministically from the onchain clock while lifetime spend never resets;
- authority, merchant, executor, and verifier must be four distinct identities;
- charge and freeze atomically create immutable evidence records keyed by allowance plus the complete evidence hash;
- pre-funded but uninitialized evidence PDAs are safely allocated and assigned, preventing dusting-based address reservation;
- pause is user-controlled; freeze is verifier-controlled and stores the bad-result evidence hash;
- unfreeze requires both user and verifier; terminal revoke removes the SPL delegate through CPI;
- executor rotation requires the authority; verifier rotation requires the authority plus current verifier;
- legacy v1 instruction discriminants remain stable.

Remaining v2 limits: the verifier is a single configured signer rather than a quorum; evidence records intentionally remain rent-funded accounts with no pruning policy; the live asset is a project-created Devnet test mint rather than canonical USDC; the upgrade authority is a single development key; Android does not yet broadcast every v2 control; and the Program has not been independently audited.

## Production requirements

- move upgrade authority to a multisig or timelocked governance account;
- use a canonical production stablecoin mint and explicitly configured token decimals;
- persist nonce/idempotency state transactionally;
- independently audit the v2 period-window accounting, evidence-account creation, CPI authority, and recovery implementation;
- define delayed or quorum-based unfreeze governance for higher-value allowances;
- protect webhook secrets in a managed secret store and rotate them;
- use a dedicated dApp Store release signing key;
- complete an independent Program and Android security review.

## Threat model

| Threat | Current response | Remaining production work |
| --- | --- | --- |
| Lookalike merchant | FROZEN before settlement | merchant registry and human-readable domain binding |
| Overspend | V1 and v2 live BLOCKED; v2 adds three-level caps and rolling periods | monitor cap invariants and audit arithmetic |
| Missing or changed delivery | V1 and v2 live FROZEN; v2 separates verifier and records bad evidence | verifier quorum for high-value services |
| Network retry | stored receipt returned idempotently | transactional database |
| Evidence replay | SDK/Android/browser blocked; deployed v2 enforces nonce plus immutable per-hash Evidence PDA history | monitor evidence-account creation failures |
| Compromised executor | v2 requires a separate verifier signature, bounded policy, and authority-controlled rotation | incident procedure, verifier quorum and rate alerts |
| Compromised verifier | cannot change destination/caps; user can pause/revoke; rotation needs user + current verifier | emergency rotation design for a lost verifier and quorum |
| Compromised upgrader | disclosed single upgrade authority | multisig/timelock |
| Lost mobile session | local forget + MWA deauthorize | account recovery UX |
