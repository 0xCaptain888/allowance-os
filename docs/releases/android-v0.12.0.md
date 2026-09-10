# Allowance OS Android / SDK v0.12.0

## Integer money and canonical evidence

- SDK policies bind the token symbol, exact mint address, decimals, and integer raw-unit limits.
- Requests and receipts carry `amountRaw`; policy accounting uses `perChargeRaw`, `periodCapRaw`, and `spentInPeriodRaw`.
- Merchant receipts are schema v3; consumers must reject or explicitly migrate v2 payloads.
- Malformed, decimal, negative, scientific-notation, and Solana `u64`-overflow values fail closed.
- A matching display symbol cannot bypass an exact mint mismatch.
- Canonical JSON recursively sorts nested keys and includes every nested value in policy, receipt, idempotency, and webhook hashes.
- Webhook HMAC signatures use the same canonical representation.
- Android explicitly labels its sliders as a local simulation rather than authoritative accounting.

## Wallet and distribution hardening retained

- Wallet restoration requires both a public identity and a valid encrypted MWA reconnect token.
- Stale identity after Android Keystore failure is removed and requires reauthorization.
- Production tags require protected release signing; debug QA builds use `android-test-v*`.

## Truth boundary

The Android sliders still use display values because the real Delegated Settlement v2 transaction flow is not yet deployed or integrated. No slider value is presented as chain state. The v2 Program itself and its TypeScript instruction builders already use integer `u64`/`bigint` amounts.
