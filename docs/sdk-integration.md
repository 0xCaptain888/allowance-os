# Merchant SDK integration

Allowance OS v0.11.1 includes a reference TypeScript SDK for bounded repeat payments. It is intentionally signer-agnostic: wallet credentials stay in Mobile Wallet Adapter, Seed Vault, or an application-controlled signer adapter.

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

The merchant must verify `webhookSignature` before fulfilling or unlocking a paid resource. Production deployments should persist policies, nonces, evidence hashes, idempotency records, and receipts in a transactional database rather than the included in-memory reference runtime.

## Live Solana adapter

`SolanaDevnetAdapter` accepts an injected `SolanaInstructionExecutor`. Android can implement the executor through MWA; a backend can implement it through a protected signer. The SDK never accepts or serializes private keys.
