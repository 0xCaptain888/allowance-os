# Delegated Settlement v2

Status: **SOURCE TESTED · NOT DEPLOYED**  
Program crate: `allowance-os-program 0.2.0`  
Declared v2 Program ID: `7zARKWKDLawLgR7qokvQdkAv6ye2cXGNEvNQswBd6xvL`
State version: `2`  
Allowance state size: `332` bytes
Evidence record size: `82` bytes

## Why v2 exists

The public v1 Devnet evidence proves that an onchain Program can enforce a merchant, mint, charge cap, period cap, expiry, and evidence hash before an SPL-token transfer. However, v1 requires the user authority to sign every `Charge`. That is useful enforcement evidence, but it is not recurring settlement.

v2 implements the missing authorization boundary:

```text
User signs CreateDelegated once
  → SPL Token delegates lifetime_cap to an allowance-scoped PDA
  → policy and actor identities are committed in AllowanceStateV2

Later fulfillment
  → Executor signs a ChargeDelegated request
  → independent Verifier signs the same transaction
  → Program checks state, actors, nonce, evidence and all caps
  → Program signs as the delegate PDA
  → SPL Token transfers to the committed merchant account
  → user signature is not required
```

The PDA is derived from:

```text
["allowance-delegate", allowance_account_pubkey]
```

Each allowance therefore receives a distinct delegate authority. The PDA cannot initiate arbitrary transfers: it can sign only while the Allowance OS Program is executing, and the Program releases a transfer only after its committed policy checks pass.

Every accepted charge and every verifier freeze also creates a separate evidence record PDA:

```text
["allowance-evidence", allowance_account_pubkey, evidence_hash]
```

Because the complete 32-byte evidence hash is part of the PDA address, reusing any historical hash for the same allowance attempts to recreate an existing account and fails closed. The record stores the allowance, evidence hash, event kind, nonce, and onchain timestamp. A system-owned PDA that has merely been pre-funded is safely topped up, allocated, and assigned by the Program, so address dusting cannot reserve an upcoming evidence hash. `last_evidence_hash` remains a convenient state pointer; it is not the only replay boundary.

The executor funds the charge evidence record; the verifier funds a freeze record. This makes evidence-retention cost explicit and keeps the user offline during later settlement. Production pricing must include that rent cost.

## Roles

| Role | Authority |
| --- | --- |
| User authority | Creates, pauses, unpauses, rotates the executor, co-signs verifier rotation/unfreeze, and permanently revokes the allowance |
| Executor | Requests a bounded settlement after service delivery |
| Verifier | Independently attests a valid result or freezes the allowance with a bad-result evidence hash |
| Merchant | Owns the only accepted destination token account |
| Delegate PDA | Performs the SPL-token CPI after Program validation; possesses no private key |

Authority, merchant, executor, and verifier must be four distinct public keys. These checks prevent one identity from simultaneously owning funds, receiving funds, executing settlement, and approving delivery.

## Onchain state

`AllowanceStateV2` commits:

```text
version
authority
merchant
executor
verifier
token_mint
source_token
per_charge
period_cap
lifetime_cap
spent_in_period
spent_lifetime
period_started_at
period_seconds
expires_at
next_nonce
paused
revoked
frozen
policy_hash
last_evidence_hash
```

Each `EvidenceRecordV2` independently commits:

```text
version
allowance
evidence_hash
kind        # 1 = accepted charge, 2 = freeze
nonce
created_at
```

The three budget levels have distinct purposes:

- `per_charge` prevents one unexpectedly large charge;
- `period_cap` limits aggregate spend in a deterministic rolling window;
- `lifetime_cap` bounds both Program accounting and the SPL Token delegated amount.

At or after `period_started_at + period_seconds`, the next valid charge starts a new period at the current onchain clock and resets `spent_in_period` before applying the amount. `spent_lifetime` never resets.

## Stable instruction surface

Legacy Borsh discriminants remain unchanged:

| Discriminant | Instruction | Version |
| ---: | --- | --- |
| `0` | `Create` | v1 |
| `1` | `Charge` | v1 |
| `2` | `Revoke` | v1 |
| `3` | `CreateDelegated` | v2 |
| `4` | `ChargeDelegated` | v2 |
| `5` | `PauseDelegated` | v2 |
| `6` | `UnpauseDelegated` | v2 |
| `7` | `FreezeDelegated { evidence_hash }` | v2 |
| `8` | `UnfreezeDelegated` | v2 |
| `9` | `RevokeDelegated` | v2 |
| `10` | `RotateExecutor { new_executor }` | v2 |
| `11` | `RotateVerifier { new_verifier }` | v2 |

`src/delegated-protocol.ts` provides matching TypeScript instruction builders and account ordering. The tests assert the discriminants, payload sizes, signer roles, delegate/evidence PDA derivation, rotation payloads, and the absence of the user authority from `ChargeDelegated`.

## State transitions

```text
ACTIVE ──authority──> PAUSED ──authority──> ACTIVE
  │                       │
  ├──verifier + evidence──> FROZEN
  │                           │
  │              authority + verifier
  │                           │
  │                           └──> ACTIVE
  │
  └──authority──> REVOKED (terminal; SPL delegate removed)
```

`RevokeDelegated` performs an SPL Token `Revoke` CPI and then persists `revoked = true` and `paused = true`. Revocation is therefore both a Program state transition and removal of the underlying token delegation.

## Fail-closed checks

A delegated charge is rejected before CPI when any of the following is false:

- exact executor signature;
- exact independent verifier signature;
- version-2 state and Program ownership;
- exact source token account and token mint;
- destination token account owned by the committed merchant;
- exact delegate PDA;
- active, unpaused, unfrozen, unrevoked, unexpired state;
- sequential nonce;
- positive amount within per-charge, current-period, and lifetime caps;
- nonzero evidence hash;
- correct, uninitialized Evidence Record PDA derived from the allowance and complete evidence hash;
- exact System Program account used to create that record atomically.

The verifier remains responsible for deciding whether fulfillment is valid. Historical evidence uniqueness is enforced onchain: the sequential nonce stops transaction replay, while the one-account-per-hash PDA prevents reuse of any earlier evidence hash for that allowance. Evidence record creation and token transfer occur in the same Solana transaction, so either both persist or both roll back.

## Recovery model

- `PauseDelegated`: user-only temporary containment.
- `FreezeDelegated`: verifier-only containment with the rejected result's evidence hash persisted onchain.
- `UnfreezeDelegated`: requires both user and verifier signatures.
- `RotateExecutor`: requires the user authority and preserves four-way role separation.
- `RotateVerifier`: requires both the user authority and current verifier, then preserves four-way role separation.
- `RevokeDelegated`: user-only terminal action that removes the SPL delegate.

The dual-signature unfreeze prevents either a compromised verifier or a careless user interface from silently restoring a disputed allowance alone.

## Verification completed

Rust source tests cover:

- v1 instruction discriminant compatibility;
- exact v2 state size;
- allowance-scoped delegate PDA derivation;
- allowance-and-full-hash Evidence Record PDA derivation and exact record size;
- authority-free charge evaluation;
- exact executor and verifier signatures;
- sequential nonces and duplicate evidence;
- per-charge, rolling-period, and lifetime caps;
- deterministic period rollover;
- paused, frozen, and revoked behavior;
- fresh evidence requirements and terminal-state protection for verifier freezes;
- executor/verifier rotation rules and role separation.

The current suite passes `20` Rust tests and `22` TypeScript tests. TypeScript additionally verifies production-facing payload sizes, account order, signer/writable flags, discriminants, both PDA families, rotation payloads, malformed hashes, and integer ranges.

The source was compiled to Solana SBF artifacts using pinned `cargo-build-sbf 4.3.0`. The local macOS builder used platform-tools `v1.57` and Rust `1.95.0`:

```text
macOS: 130,280 bytes · e0eb4726bdfda25fa2a377f347b9590087072e4ad1d9e455598760f6b865e7d6
Ubuntu CI: generated independently for the current pushed source revision
```

No byte-for-byte cross-platform reproducibility claim is made. The exact `.so` chosen for deployment must be hashed and compared with the deployed binary. The machine-readable record is [`evidence/delegated-v2-source-build.json`](../evidence/delegated-v2-source-build.json). Generated `.so` files and all build keypairs remain ignored local artifacts.

## Deployment gate

This document describes compiled and tested source, not a live v2 deployment. Before changing the status to `DEVNET VERIFIED`, the project must publish:

1. the deployment transaction for the declared v2 Program ID;
2. the deploy/upgrade transaction;
3. a `CreateDelegated` transaction showing the SPL approval;
4. a later `ChargeDelegated` transaction signed by executor and verifier without the user;
5. source and merchant token balance deltas;
6. the created Evidence Record PDA decoded from chain data;
7. BLOCKED, FROZEN, PAUSED, UNFROZEN, role-rotation, and REVOKED transactions;
8. the deployed binary hash and matching local build hash;
9. updated machine-readable evidence.

Until those artifacts exist, the public v1 Devnet matrix remains the only claimed live Program evidence.
