# Allowance OS Architecture

## Product boundary

Allowance OS is a mobile control plane for delegated spending. It does not custody keys and it does not silently submit live transactions. The policy engine, wallet authorization, deployed Program enforcement, and future token settlement are separate, auditable layers.

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
          ├── VERIFIED → Program records matching evidence and spend
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
executeCharge(request)      → VERIFIED, BLOCKED, or persistent FROZEN state
revokeAllowance(allowance)  → authority persists REVOKED state
```

Program ID: `DJzPBS7FreCcWWGkApzznGcKq9T7Da38GpKFtpxWRcuE` on Solana Devnet.

The same policy hash is exposed to the independent verifier. A UI-only cap is not considered a security boundary. SPL-token transfer CPI remains deliberately separate and is not claimed by the current evidence.
