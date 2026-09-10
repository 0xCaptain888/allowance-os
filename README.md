# Allowance OS

> Approve once. Enforce every charge. Revoke anytime.

**[Open the public Judge Demo](https://0xcaptain888.github.io/allowance-os/)** · **[Android MWA client](mobile/android)** · **[Two-minute judge guide](docs/judge-guide.md)**

**[Download versioned Android releases](https://github.com/0xCaptain888/allowance-os/releases)** · **[Product maturity audit](docs/product-maturity-audit.md)** · **[Changelog](CHANGELOG.md)** · **[Release process](docs/android-release-process.md)**

Allowance OS is a Solana Mobile payment-authorization layer for Web3 services. It turns an open-ended wallet approval into a human-readable allowance with merchant, token, program, per-charge, period, expiry, and evidence boundaries—then applies that same model to AI subscriptions, paid research, trading signals, automation bots, and metered APIs.

**v0.13.0 hardens the path from demo to operated product:** the merchant runtime now supports atomic restart-safe reference persistence; webhook verification adds timestamp tolerance, event replay rejection, and overlapping-secret rotation; Android adds strict v2 state decoding, authority-control encoders, bounded RPC retries, and portable Activity audit exports. These additions remain honestly separated from live v2 deployment claims.

**Delegated Settlement v2 is now source-tested:** a user approves an allowance-scoped SPL delegate PDA once; later `ChargeDelegated` settlement requires the configured executor and independent verifier, enforces sequential nonces plus per-charge/period/lifetime caps, and does not include the user authority as a signer. Every accepted charge or freeze creates an immutable evidence PDA keyed by the full evidence hash. Pause, evidence-bound freeze, dual-signature unfreeze, terminal token-delegate revoke, and governed executor/verifier rotation are implemented. Read the [v2 specification](docs/delegated-settlement-v2.md).

The dedicated v2 Program ID is `7zARKWKDLawLgR7qokvQdkAv6ye2cXGNEvNQswBd6xvL`. Until its deployment transaction is published, it remains a source/build identity rather than live chain evidence; the existing `DJz…WRcuE` deployment remains the v1 proof.

The current v2 source produces `130,280`-byte Solana SBF binaries with pinned `cargo-build-sbf 4.3.0`. The macOS SHA-256 is `e0eb4726bdfda25fa2a377f347b9590087072e4ad1d9e455598760f6b865e7d6`; independent Ubuntu CI run `34451107078` produced `45a3996ee011587e394951ee344742290c0aa7d0d132d972cb0168360df191dc`. The bytes differ across hosts, so no cross-platform byte-for-byte reproducibility claim is made. See [source-build evidence](evidence/delegated-v2-source-build.json); this is build evidence, not deployment evidence.

> **Pre-production disclosure:** v2 is **SOURCE TESTED · NOT DEPLOYED**. The repository proves policy evaluation, real MWA signing, deployed v1 Devnet state transitions, and a recorded v1 SPL-token transfer. The current deployed Program still requires the authority signer for `Charge`; do not interpret the v2 source as live recurring settlement until new public transactions and a matching binary hash are published. See the [maturity audit](docs/product-maturity-audit.md).

## Commercial product proof

v0.13.0 keeps the five-surface product—**Home → Services → Allowances → Activity → Evidence**—while adding restart-safe merchant behavior, exportable audit evidence, and mobile v2 protocol readiness instead of adding another static use-case card.

| Ready template | Commercial use | Payment boundary | Required evidence |
| --- | --- | --- | --- |
| AgentCloud | AI Agent subscription | 0.50 USDC/run; 12 USDC/month | signed run receipt + output hash |
| AlphaBrief | paid research reports | 2 USDC/report; 8 USDC/week | report URI + content hash |
| SignalWire | trading signals | 0.25 USDC/signal; 5 USDC/day | signal hash + publisher signature |
| AutoPilot | automated trading bot | 1 USDC/fee; 20 USDC/week | strategy ID + order receipt + verifier hash |
| DataPipe | paid API | 0.05 USDC/batch; 10 USDC/month | usage root + metering receipt |

The Android app can filter these services, apply any template, inspect its merchant/program/evidence boundaries, and replay `VERIFIED`, `BLOCKED`, and `FROZEN` requests against its own limits.

The **Seeker Integration Lab** also maps Allowance OS to apps featured by Solana Mobile, including Helium Mobile, Parallel Colony, Amp Pay, Moonwalk Fitness, and Perena. Every third-party card is explicitly labeled `BLUEPRINT · UNOFFICIAL`; it does not imply partnership or live integration. See [the UI and Seeker research note](docs/product-ui-and-seeker-research.md).

## Judge path

| Surface | What it proves | Status |
| --- | --- | --- |
| [Public Judge Demo](https://0xcaptain888.github.io/allowance-os/) | Instant `VERIFIED` / `BLOCKED` / `FROZEN` policy replay | GitHub Pages deployment |
| `mobile/android` | Bilingual native Android product with commercial catalog, AlphaBrief unlock/replay lab, fail-closed encrypted MWA session, JSON audit export, v2 state/control codecs, bounded RPC retry, and direct Devnet verification | v0.13.0; 15 Android tests |
| `program/` | Backward-compatible v1 plus Delegated Settlement v2 with PDA authority, executor/verifier separation, immutable evidence records, governed role rotation, three-level caps, period rollover, recovery and SPL revoke | v2 source tested with 20 Rust tests; not deployed |
| Solana Explorer | Connected Devnet wallet and wallet-broadcast authorization proof | Live signature captured |
| [Deployed allowance program](https://explorer.solana.com/address/DJzPBS7FreCcWWGkApzznGcKq9T7Da38GpKFtpxWRcuE?cluster=devnet) | Program-enforced state transitions and settlement | Live `VERIFIED`, `BLOCKED`, `FROZEN`, and `REVOKED` evidence |
| SPL-token settlement | Token movement through CPI | Live Devnet transfer with independently readable pre/post balances |

The repository never labels a simulated receipt as a real transaction. The Android client only shows a Solana Explorer link after a wallet returns an actual Devnet signature. Program-enforced transitions and token settlement are also disclosed as separate evidence levels.

## Live Devnet proof matrix

Program: [`DJzPBS7FreCcWWGkApzznGcKq9T7Da38GpKFtpxWRcuE`](https://explorer.solana.com/address/DJzPBS7FreCcWWGkApzznGcKq9T7Da38GpKFtpxWRcuE?cluster=devnet)

Allowance account: [`BRqgbZzdZPrueoWEcjusZ49etotWfNGtu2Bs1HiQ7E5x`](https://explorer.solana.com/address/BRqgbZzdZPrueoWEcjusZ49etotWfNGtu2Bs1HiQ7E5x?cluster=devnet)

Devnet test mint: [`3KVq4nkUnb7GS7DjYCaGR7JJGAhDG1YPsz84xxThn5de`](https://explorer.solana.com/address/3KVq4nkUnb7GS7DjYCaGR7JJGAhDG1YPsz84xxThn5de?cluster=devnet) (6 decimals; project-created test token, not canonical USDC)

| Outcome | Public proof | What changed |
| --- | --- | --- |
| CREATED | [`kmei…rHYH`](https://explorer.solana.com/tx/kmeiVR39tEkX1Rgo4pkNHz784anFvi3eX1J3YYeyry6pAPHKwvJNsEMaxqSLo1grJkZ5fSrNmKoQrMiVdTnrHYH?cluster=devnet) | Policy, mint, merchant, and required evidence hash committed onchain |
| VERIFIED | [`22xK…SPUU`](https://explorer.solana.com/tx/22xKvkfk2YwEV6mVQXmhGeGFWSSTfvE9FGPLGJ7McSf93DpxuntZYLE8mjdv7SugEmMoo3rvswKa7eMfzf9nSPUU?cluster=devnet) | Matching evidence invokes SPL Token CPI; source `20 → 19`, merchant `0 → 1` |
| BLOCKED | [`EU6b…vted`](https://explorer.solana.com/tx/EU6bUcBTdpxCMu9rRBhDqQjPwnvPLtGmDtKepZnRZK5rKnRdeiWGqVvpKerPRAheddBgkhmpQgNdcTVZCCgvted?cluster=devnet) | Over-cap request fails with Program Custom Error `6`; both token balances remain unchanged |
| FROZEN | [`5REq…MLpu`](https://explorer.solana.com/tx/5REqbiziiN5bAWPDYDMSq7TgS3rvMVUeW9UmqfMdzpfCPxYctWxeQa83beR14tHbSSdTRWg1P7jbWasE5U37MLpu?cluster=devnet) | Evidence mismatch persists `frozen = true`; both token balances remain unchanged |
| REVOKED | [`5Mpt…qysu`](https://explorer.solana.com/tx/5MpttoR2mfpDXhWq1B3nr6AWHC9AFCTsQ7626EXDEka6nWzgF3GdJGZnTxk9BmU4sBxiLjAAbbES9GXheGCqqysu?cluster=devnet) | Authority persists `revoked = true`; no token movement |

The complete machine-readable record is [`evidence/live-devnet-program.json`](evidence/live-devnet-program.json). The VERIFIED transaction contains the SPL Token Program inner instruction and public pre/post token balances; BLOCKED and FROZEN move zero tokens.

## Why this matters

Wallets are good at approving one transaction. Autonomous agents and subscriptions need a durable answer to a harder question: **what may charge me later, how much, how often, through which program, and what proof must exist before funds move?**

Allowance OS makes that policy visible and independently verifiable:

```text
User policy
  → deterministic preflight
      → BLOCKED before wallet invocation
      → FROZEN on identity/evidence mismatch
      → VERIFIED request opens MWA
          → Phantom / compatible wallet confirmation
          → real Solana Devnet signature
  → MWA session may be disconnected locally
  → onchain REVOKED requires a separate authority-signed Program instruction
```

The source-tested v2 settlement path removes the recurring user signature:

```text
User signs CreateDelegated once
  → SPL delegate PDA receives a hard lifetime cap
  → Executor requests a later charge
  → independent Verifier attests delivery
  → Program enforces nonce + per-charge + period + lifetime policy
  → immutable Evidence Record PDA commits hash + allowance + nonce + time
  → PDA signs SPL transfer; user is offline
```

## Native Android MWA client

The Android app is not a mockup. It uses Solana Mobile's official `mobile-wallet-adapter-clientlib-ktx:2.0.7` and implements:

- Chinese and English product interfaces with a persisted in-app language switch;
- Android Keystore encryption for the MWA reconnect token, including migration from the legacy plaintext preference;
- a pre-sign review that clearly shows network, payload, and `0 USDC` asset movement for the current Memo proof;
- separate language and controls for disconnecting MWA versus revoking an onchain allowance;
- five product surfaces: Home, Services, Allowances, Activity, and Evidence;
- five reusable commercial templates for AI Agent subscriptions, research reports, trading signals, automated trading bots, and paid APIs;
- an AlphaBrief reference purchase that exposes request ID, nonce, expiry, content hash, unlock state, and evidence replay rejection;
- an explicitly unofficial Seeker integration lab covering service subscriptions, games, commerce, fitness, and DeFi automation;
- persistent Activity Log with decision counts, wallet events, and error history;
- portable JSON Activity Log export with a SHA-256 audit fingerprint;
- `Connect Phantom / MWA Wallet`;
- reconnect, balance refresh, address copy, and safe local-session reset;
- Solana Devnet authorization;
- wallet public-key and SOL-balance display;
- deterministic policy hash;
- adjustable amount, current-period spend, merchant identity, and evidence inputs;
- period-cap enforcement that blocks a request when `spentInPeriod + amount` would exceed the allowance;
- one-tap judge mode that records VERIFIED, BLOCKED, and FROZEN decisions in the local activity trail;
- portable JSON receipt with a SHA-256 fingerprint for cross-surface evidence binding;
- direct `getSignatureStatuses` verification of either the current device proof or the repository's recorded live proof;
- in-app verification of the five Program transactions plus final frozen/revoked allowance state;
- strict Delegated Settlement v2 state decoding plus tested pause, unpause, revoke, executor-rotation, and verifier-rotation instruction encoders;
- configurable Devnet RPC selection with bounded retries;
- persistence of the last wallet-broadcast signature across app restarts;
- `VERIFIED`, `BLOCKED`, and `FROZEN` policy replay before wallet invocation;
- real `signAndSendTransactions` for a Devnet Memo authorization proof;
- Solana Explorer evidence link;
- `deauthorize` and `REVOKED` state;
- honest standard-Android capability labels for Seed Vault and Genesis Token.

The first live device checkpoint is recorded in [`evidence/live-devnet-wallet.json`](evidence/live-devnet-wallet.json). The resulting wallet-broadcast Memo proof is recorded in [`evidence/live-devnet-memo.json`](evidence/live-devnet-memo.json). The proof is a real authorization transaction, not a deployed allowance-program settlement.

Build it with JDK 21:

```bash
cd mobile/android
./gradlew testDebugUnitTest assembleDebug
```

The resulting APK is:

```text
mobile/android/app/build/outputs/apk/debug/allowance-os-0.13.0-debug.apk
```

Local v0.13.0 debug build SHA-256: `a35e6d3d7182b90c20cf814417018a13420ff19d4c8a833e2827928c48d77a03`

Latest public QA asset: [`android-test-v0.13.0`](https://github.com/0xCaptain888/allowance-os/releases/tag/android-test-v0.13.0), CI SHA-256 `d9d5f8e57ed358e09a780ca8d4097437fdb618a453a3beee72d67854a72c07b2`. Local and CI debug hashes differ because each environment uses its own debug signing key. `android-v*` is reserved for protected production-signed releases; `android-test-v*` assets are explicitly labelled as Devnet QA builds.

See [`mobile/README.md`](mobile/README.md) for phone setup and [`docs/judge-guide.md`](docs/judge-guide.md) for the two-minute evaluation path.

## Core engine

- Deterministic policy and policy-hash generation.
- Per-charge and period caps.
- Merchant, token, program, expiry, allowance-ID, and evidence checks.
- Human-readable receipts for `VERIFIED`, `BLOCKED`, `FROZEN`, and `REVOKED`.
- SDK receipt v3 with exact mint metadata, integer raw-unit accounting, request IDs, nonces, expiry, idempotent retries, evidence-replay rejection, and canonical HMAC-signed merchant webhooks.
- Pluggable runtime persistence with an atomic JSON single-process reference store that survives restarts without losing spend, idempotency, nonce, replay, or revocation state.
- Stateful webhook verification with timestamp tolerance, event replay rejection, and overlapping old/new secrets for safe rotation.
- A signer-agnostic live adapter that accepts MWA or protected server executors without accepting wallet secrets.
- End-to-end AlphaBrief paid-content integration in TypeScript, Android, and the browser Demo.
- Backward-compatible Delegated Settlement v2 instruction builders for TypeScript and Rust.
- Allowance-scoped SPL delegate PDA, four-way actor separation, sequential nonce, deterministic period rollover, three-level caps, immutable per-evidence PDAs, pause, evidence-bound freeze, dual-signature unfreeze, governed executor/verifier rotation, and delegate-removing revoke.
- Reproducible Rust dependency lockfile and 20 passing native Program source tests.
- Standard Android and Seeker device profiles.

Run the TypeScript verifier:

```bash
npm install
npm run typecheck
npm test
npm run demo
npm run demo:alphabrief
```

Reproduce the live Devnet settlement run with a funded test keypair and compatible token accounts:

```bash
SOLANA_KEYPAIR=/absolute/path/to/devnet-keypair.json \
TOKEN_MINT=<devnet-mint> \
SOURCE_TOKEN_ACCOUNT=<authority-owned-token-account> \
MERCHANT_TOKEN_ACCOUNT=<merchant-owned-token-account> \
npm run devnet:live
```

After a v2 Program is deployed, reproduce the decisive approve-once / settle-later proof with separate authority, executor, and verifier keypairs:

```bash
DELEGATED_PROGRAM_ID=<v2-program-id> \
SOLANA_KEYPAIR=/absolute/path/to/authority.json \
EXECUTOR_KEYPAIR=/absolute/path/to/executor.json \
VERIFIER_KEYPAIR=/absolute/path/to/verifier.json \
TOKEN_MINT=<devnet-mint> \
SOURCE_TOKEN_ACCOUNT=<authority-token-account> \
MERCHANT=<merchant-wallet> \
MERCHANT_TOKEN_ACCOUNT=<merchant-token-account> \
npm run devnet:delegated
```

The second transaction is fee-paid and signed by the executor plus verifier; the authority keypair is deliberately absent. The runner fails unless the token deltas, nonce, lifetime spend, actor separation, v2 state, and immutable Evidence Record PDA all match. It cannot run accidentally against an unspecified deployment because `DELEGATED_PROGRAM_ID` is mandatory.

## Truth boundary

There are four intentionally separate evidence levels:

1. **SIMULATED** — browser and TypeScript policy replay; never a chain claim.
2. **WALLET-BROADCAST DEVNET PROOF** — real MWA authorization and Memo transaction returned by Phantom; the browser and Android app can independently query its Devnet confirmation status and slot.
3. **PROGRAM-ENFORCED STATE TRANSITIONS** — deployed Rust Program with public create, verified, blocked, frozen, and revoke evidence.
4. **SPL-TOKEN SETTLEMENT** — the VERIFIED transaction invokes the SPL Token Program and transfers exactly `1,000,000` raw units; BLOCKED and FROZEN are publicly shown to transfer zero.

The deployed Rust Program ID is `DJzPBS7FreCcWWGkApzznGcKq9T7Da38GpKFtpxWRcuE`. Its upgrade authority remains the deployment wallet for hackathon iteration; this is disclosed rather than presented as immutable production infrastructure. The deployed binary is still the v0.9.0 evidence build. Delegated Settlement v2 and replay protection are source-tested but are not claimed for that older deployed binary.

See the [Delegated Settlement v2 specification](docs/delegated-settlement-v2.md), [merchant SDK guide](docs/sdk-integration.md), [operations runbook](docs/operations.md), [security policy](SECURITY.md), [security notes](docs/security.md), [privacy policy](docs/privacy-policy.md), and [prepared dApp Store submission pack](docs/dapp-store-submission.md).

## Device modes

| Capability | Standard Android + Phantom | Seeker |
| --- | --- | --- |
| Mobile Wallet Adapter | Available | Available |
| Real Devnet signing | Available | Available |
| Seed Vault | Seeker only | Available |
| Genesis Token | Not available | Available |
| `SEEKER VERIFIED` label | Never shown | Requires real validation |

This allows meaningful real-device testing now without pretending that a standard Android handset supplies Seeker hardware capabilities. The Android app also exposes a portable receipt: its `SIMULATED` or `LIVE_DEVNET_PROOF` label and SHA-256 fingerprint make the evidence boundary explicit when a judge copies results out of the app.
