# Allowance OS

Allowance OS is a Seeker-native control center for safe on-chain subscriptions and autonomous-agent spending.

> Approve once. Enforce every charge. Revoke anytime.

## What exists in this first slice

- A deterministic allowance policy engine.
- Human-readable receipts for `VERIFIED`, `BLOCKED`, `FROZEN`, and `REVOKED`.
- Per-charge and period caps.
- Merchant, token, program, expiry, and evidence checks.
- A judge-facing browser simulator in `site/index.html`.
- Explicit adapter boundary for MWA, Seed Vault, and a future Solana spend-permission program.
- Native Solana program source in `program/` with create, charge, and revoke instructions.
- Explicit device profiles for standard Android MWA and Seeker + Seed Vault.

## Run locally

```bash
npm install
npm run typecheck
npm test
npm run demo
```

Open `site/index.html` directly for the browser demo, or serve the directory with any static server.

## Truth boundary

The current slice is a local deterministic simulator. It does not claim that a Solana transaction was broadcast. The next integration layer will add MWA / Seed Vault authorization and a Devnet spend-permission adapter; receipts will only be marked live after an explorer-verifiable transaction exists.

## Planned Solana integration

```text
Mobile app → MWA / Seed Vault → Allowance Program → USDC/SKR transfer
                              ↘ Receipt / revoke / freeze
```

The on-chain program will enforce the same policy hash as the local verifier so a UI cannot silently widen an allowance.

The Rust program currently has a placeholder Devnet program ID and is not deployed. Replace the ID only as part of a reviewed deployment; local receipts must never be copied into live evidence.
