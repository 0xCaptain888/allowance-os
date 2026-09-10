# Operations and incident runbook

Status: **pre-production reference process**.

## Failure classes

| Failure | User-visible response | Operator action |
| --- | --- | --- |
| RPC timeout or rate limit | keep request pending; never infer success | query the signature through another configured RPC before retrying |
| wallet callback lost | show reauthorization/recovery path | query any returned signature; never create a second request with a new idempotency key automatically |
| webhook delivery failure | settlement remains recorded but fulfillment is pending | retry the same signed event ID with exponential backoff |
| webhook replay or stale timestamp | reject without fulfillment | retain the event ID until outside the replay window and investigate sender clock/configuration |
| executor compromise | pause allowances and rotate executor | preserve incident evidence; do not unfreeze without required authority/verifier approval |
| verifier compromise | pause or revoke; do not trust new evidence | rotate through the governed path or use the documented emergency design process |
| local Android Keystore/session loss | clear stale public identity and require MWA authorization | do not claim the onchain allowance was revoked |

## RPC policy

The Android client uses a configurable Devnet RPC URL with bounded retries. A custom endpoint can be supplied as the Gradle property `allowanceOsSolanaRpcUrl`; values compiled into an APK are public and must not contain a secret API credential. Production clients should call a protected relay or use a provider configuration designed for public mobile clients.

The manual v2 deployment workflow accepts the encrypted GitHub Secret `SOLANA_DEVNET_RPC_URL`. This is intended for a rate-limited private Devnet endpoint and is masked in workflow logs.

## Merchant state

`JsonFileRuntimeStateStore` is an atomic, single-process reference implementation that survives restarts and preserves policy, idempotency, nonce, evidence, spending, and revocation state. It is not a multi-writer database. Production deployments must use transactional storage with backups, access control, encryption, and migration discipline.

## Webhooks

`WebhookVerifier` provides:

- constant-time HMAC verification;
- timestamp tolerance;
- in-process event-ID replay rejection;
- overlapping old/new secrets for rotation.

Production delivery additionally needs durable replay storage, a retry queue, dead-letter handling, observability, and per-merchant authentication/rate limits.

## Release incident rule

If an APK signing key may be compromised, stop publishing updates under that identity, preserve evidence, notify users through the repository and website, and follow the dApp Store's current key/release recovery process. Never silently replace a release checksum.
