# Allowance OS Architecture

## Product boundary

Allowance OS is a mobile control plane for delegated spending. It does not custody keys and it does not silently submit live transactions. The first slice models the policy and receipt boundary; the Solana adapter is a separate integration.

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
          ├── VERIFIED → submit through Solana allowance program
          ├── BLOCKED  → reject before broadcast
          └── FROZEN   → stop settlement and require review
```

## Receipt contract

Every decision emits a receipt containing the allowance ID, policy hash, evidence hash, state, reason codes, and (only after a live integration) an explorer-verifiable transaction hash.

The local demo deliberately uses `simulated:` hashes. It must never present them as live chain evidence.

## Planned Solana adapter

The adapter will be implemented behind the current engine with three explicit calls:

```text
authorizeAllowance(policy)  → MWA / Seed Vault signature
executeCharge(request)      → allowance program instruction
revokeAllowance(allowance)  → on-chain deauthorize / revoke instruction
```

The same policy hash must be checked by the program and the independent verifier. A UI-only cap is not considered a security boundary.
