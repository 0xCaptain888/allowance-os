# Allowance OS Architecture

## Product boundary

Allowance OS is a mobile control plane for delegated spending. It does not custody keys and it does not silently submit live transactions. The policy engine, wallet authorization, deployed Program enforcement, and SPL-token settlement are separate, auditable layers.

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

## Receipt contract

Every decision emits a receipt containing the allowance ID, policy hash, evidence hash, state, reason codes, and (only after a live integration) an explorer-verifiable transaction hash.

The local demo deliberately uses `simulated:` hashes. It must never present them as live chain evidence.

## Deployed Solana adapter

The adapter will be implemented behind the current engine with three explicit calls:

```text
authorizeAllowance(policy)  → MWA signature and wallet-broadcast evidence
createAllowance(policy)     → deployed Program commits policy + evidence hash
executeCharge(request)      → VERIFIED SPL transfer, BLOCKED rejection, or persistent FROZEN state
revokeAllowance(allowance)  → authority persists REVOKED state
```

Program ID: `DJzPBS7FreCcWWGkApzznGcKq9T7Da38GpKFtpxWRcuE` on Solana Devnet.

The same policy hash is exposed to the independent verifier. A UI-only cap is not considered a security boundary. For VERIFIED, the Program validates the authority, mint, source owner, merchant owner, per-charge cap, period cap, expiry, and required evidence hash before invoking the SPL Token Program. The allowed Program ID is committed in the policy state and exposed to independent verification. BLOCKED and FROZEN return before CPI, so they move no tokens. The recorded mint is a project-created Devnet test token, not canonical USDC.

## Merchant delivery plane

```text
merchant output -> evidence hash -> expiring request ID + nonce
  -> SDK policy decision -> receipt v2 -> signed webhook -> fulfillment unlock
                           -> idempotent retry returns stored receipt
                           -> reused evidence under new request is BLOCKED
```

The SDK reference runtime is in-memory for reproducible judging. Production deployments must persist its idempotency, nonce, evidence, policy, and receipt records transactionally.
