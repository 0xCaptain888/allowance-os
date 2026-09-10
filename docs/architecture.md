# Allowance OS Architecture

## Product boundary

Allowance OS is a pre-production mobile control plane for bounded spending. It does not custody keys and it does not silently submit live transactions. The policy engine, wallet authorization, deployed Program enforcement, and SPL-token settlement are separate, auditable layers.

The current deployed Program requires the authority signer for `Charge`. That is useful public enforcement evidence, but it is not yet the final “approve once, charge later” architecture.

Delegated Settlement v2 now implements that missing architecture in compiled and tested source. A user signs `CreateDelegated` once, which delegates a hard lifetime amount to an allowance-scoped PDA. Later settlement requires separate executor and verifier signatures; the Program signs the SPL transfer as the PDA after enforcing the policy. The user authority is not an account in `ChargeDelegated`. Status: **SOURCE TESTED · NOT DEPLOYED**.

```text
Seeker Android App
  ├─ Allowance cards and history
  ├─ MWA / Seed Vault authorization
  ├─ Native revoke / pause action
  └─ Risk and receipt presentation
          │
          ▼
Allowance Policy Engine
  ├─ merchant identity
  ├─ token and program boundary
  ├─ per-charge cap
  ├─ period cap
  ├─ expiry / revoke
  └─ evidence binding
          │
          ├── VERIFIED → Program validates policy, then transfers SPL tokens by CPI
          ├── BLOCKED  → local preflight or Program rejects the request
          └── FROZEN   → Program persists evidence mismatch for containment
                              │
                              └── REVOKED → authority terminates the allowance
```

## Delegated Settlement v2

```text
User + source token account
  └─ CreateDelegated
       ├─ commit merchant / executor / verifier / mint / source
       ├─ commit per-charge / period / lifetime / expiry policy
       └─ SPL Approve(lifetime_cap, allowance-scoped delegate PDA)
                              │
                              ▼
Merchant delivers output → evidence hash
                              │
                Executor signer + Verifier signer
                              │
                              ▼
                      ChargeDelegated
       ├─ actor identities and signatures
       ├─ sequential nonce and evidence presence/replay
       ├─ paused / frozen / revoked / expiry state
       ├─ source / mint / merchant token destination
       └─ per-charge / rolling-period / lifetime caps
                              │
                              ▼
             invoke_signed(SPL Token transfer, PDA)
```

The verifier can persist `FROZEN` with the rejected result's evidence hash. Unfreeze requires both the authority and verifier. Revoke requires the authority, invokes SPL Token `Revoke`, and makes the Program state terminal. See [`delegated-settlement-v2.md`](delegated-settlement-v2.md) for the exact state layout, instruction discriminants, and deployment gate.

## Receipt contract

Every decision emits a receipt containing the allowance ID, policy hash, evidence hash, state, reason codes, and (only after a live integration) an explorer-verifiable transaction hash.

The local demo deliberately uses `simulated:` hashes. It must never present them as live chain evidence.

## Deployed Solana adapter

The deployed v1 adapter has four explicit calls:

```text
authorizeAllowance(policy)  → MWA signature and wallet-broadcast evidence
createAllowance(policy)     → deployed Program commits policy + evidence hash
executeCharge(request)      → VERIFIED SPL transfer, BLOCKED rejection, or persistent FROZEN state
revokeAllowance(allowance)  → authority persists REVOKED state
```

Program ID: `DJzPBS7FreCcWWGkApzznGcKq9T7Da38GpKFtpxWRcuE` on Solana Devnet.

The same policy hash is exposed to the independent verifier. A UI-only cap is not considered a security boundary. For VERIFIED, the Program validates the authority, mint, source owner, merchant owner, per-charge cap, period cap, expiry, and required evidence hash before invoking the SPL Token Program. The allowed Program ID is committed in the policy state and exposed to independent verification. BLOCKED and FROZEN return before CPI, so they move no tokens. The recorded mint is a project-created Devnet test token, not canonical USDC.

The v2 source replaces recurring authority signatures with an SPL delegate PDA and adds explicit executor/verifier identities, a lifetime cap, deterministic period rollover, sequential nonces, and onchain pause/freeze/unfreeze controls. These v2 properties are not attributed to the deployed v1 binary.

## Merchant delivery plane

```text
merchant output -> evidence hash -> expiring request ID + nonce
  -> SDK policy decision -> receipt v2 -> signed webhook -> fulfillment unlock
                           -> idempotent retry returns stored receipt
                           -> reused evidence under new request is BLOCKED
```

The SDK reference runtime is in-memory for reproducible judging. Production deployments must persist its idempotency, nonce, evidence, policy, and receipt records transactionally.
