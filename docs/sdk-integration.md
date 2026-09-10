# Merchant SDK integration

Allowance OS v0.12.0 includes a reference TypeScript SDK for bounded repeat payments. It is intentionally signer-agnostic: wallet credentials stay in Mobile Wallet Adapter, Seed Vault, or an application-controlled signer adapter.

Receipt schema v3 uses base-10 integer strings in the mint's smallest unit. A policy binds the display symbol, exact mint address, decimals, per-charge raw amount, period raw amount, and raw amount already spent. For a six-decimal token, `"2000000"` represents two tokens. Decimal strings, scientific notation, negative values, and values above Solana `u64` are rejected.

Receipt v2 used display-unit numbers and must not be deserialized as v3. Consumers must branch on `receiptVersion` and reject unknown versions.

## AlphaBrief flow

```text
report content
  -> SHA-256 evidence hash
  -> five-minute ChargeRequest (requestId + nonce)
  -> deterministic policy evaluation
  -> VERIFIED receipt
  -> HMAC-signed merchant webhook
  -> report unlock
```

Run the complete example:

```bash
npm run demo:alphabrief
```

The example also proves two failure-safe behaviors:

- retrying the exact same `requestId` returns the stored receipt and does not advance spend again;
- submitting the same evidence under a new request is `BLOCKED` with `evidence_replayed`.

## SDK surface

```ts
const client = new AllowanceOS(process.env.ALLOWANCE_WEBHOOK_SECRET!);

client.createAllowance(policy);
const result = client.requestCharge(request);
const unlocked = client.verifyEvidence(result.receipt, request.evidenceHash);
client.revokeAllowance(policy.allowanceId);
```

The minimum money fields are:

```ts
const policy = {
  token: 'USDC',
  tokenMint: 'exact-mint-address',
  tokenDecimals: 6,
  perChargeRaw: '2000000',
  periodCapRaw: '8000000',
  spentInPeriodRaw: '0',
};

const request = {
  token: 'USDC',
  tokenMint: policy.tokenMint,
  amountRaw: '2000000',
};
```

Policy, receipt, idempotency, and webhook hashes use recursively canonical JSON. Nested keys are sorted, nested values remain covered by the digest, and non-finite numbers are rejected rather than serialized ambiguously.

The merchant must verify `webhookSignature` before fulfilling or unlocking a paid resource. Production deployments should persist policies, nonces, evidence hashes, idempotency records, and receipts in a transactional database rather than the included in-memory reference runtime.

## Live Solana adapter

`SolanaDevnetAdapter` accepts an injected `SolanaInstructionExecutor`. Android can implement the executor through MWA; a backend can implement it through a protected signer. The SDK never accepts or serializes private keys.
