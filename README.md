# Allowance OS

> Approve once. Enforce every charge. Revoke anytime.

**[Open the public Judge Demo](https://0xcaptain888.github.io/allowance-os/)** · **[Android MWA client](mobile/android)** · **[Two-minute judge guide](docs/judge-guide.md)**

**[Download versioned Android releases](https://github.com/0xCaptain888/allowance-os/releases)** · **[Product maturity audit](docs/product-maturity-audit.md)** · **[Changelog](CHANGELOG.md)** · **[Release process](docs/android-release-process.md)**

Allowance OS is a Solana Mobile payment-authorization layer for Web3 services. It turns an open-ended wallet approval into a human-readable allowance with merchant, token, program, per-charge, period, expiry, and evidence boundaries—then applies that same model to AI subscriptions, paid research, trading signals, automation bots, and metered APIs.

**v0.16.0 keeps AlphaBrief as a complete commercial proof and adds live v2 mobile controls:** a real 508-word report is delivered, content-hashed, checked by an independent verifier, settled by Delegated Settlement v2 without the subscriber signing the charge, and followed by a bad-output attempt that freezes the allowance with zero token movement. Android now presents the result through a Daily Habits safety dashboard and authority-bound Pause/Unpause/Revoke review and broadcast.

**Delegated Settlement v2 is Devnet deployed and matrix-verified:** a user approves an allowance-scoped SPL delegate PDA once; later `ChargeDelegated` settlement requires the configured executor and independent verifier, enforces sequential nonces plus per-charge/period/lifetime caps, and does not include the user authority as a signer. Every accepted charge or freeze creates an immutable evidence PDA keyed by the full evidence hash. Read the [v2 specification](docs/delegated-settlement-v2.md) and [machine-readable live evidence](evidence/live-devnet-v2.json).

The dedicated v2 Program ID is [`7zARKWKDLawLgR7qokvQdkAv6ye2cXGNEvNQswBd6xvL`](https://explorer.solana.com/address/7zARKWKDLawLgR7qokvQdkAv6ye2cXGNEvNQswBd6xvL?cluster=devnet). Its [deployment transaction](https://explorer.solana.com/tx/21rb9fCR8Put2UDi3ANR1V5uzqLbjadozPsfcf7t3YxAV9YLuwSv7v7fNX4s66GrFNVc1RZfvAYiApBZxXCBgy7x?cluster=devnet) is finalized. The existing `DJz…WRcuE` deployment remains the intentionally separate v1 proof.

The deployed `130,280`-byte Program dumps to SHA-256 `45a3996ee011587e394951ee344742290c0aa7d0d132d972cb0168360df191dc`, exactly matching the public Ubuntu artifact from GitHub Actions run `34451107078`. The macOS artifact has a different host-specific digest, so no cross-platform reproducibility claim is made. See [source-build evidence](evidence/delegated-v2-source-build.json) and [deployment evidence](evidence/live-devnet-v2.json).

> **Pre-production disclosure:** v2 is **DEVNET DEPLOYED · FULL CONTROL MATRIX VERIFIED**. It uses a project-created Devnet test mint, not canonical USDC; the upgrade authority is still a single development key; and the Program has not received an independent audit. This is strong public hackathon evidence, not a Mainnet or production-readiness claim. See the [maturity audit](docs/product-maturity-audit.md).

## Commercial product proof

v0.16.0 keeps the five-surface product—**Home → Services → Allowances → Activity → Evidence**—but stops presenting every commercial example as equivalent. AlphaBrief is the live reference integration; the other four remain reusable templates.

| Ready template | Commercial use | Payment boundary | Required evidence |
| --- | --- | --- | --- |
| AgentCloud | AI Agent subscription | 0.50 USDC/run; 12 USDC/month | signed run receipt + output hash |
| AlphaBrief | paid research reports | 2 USDC/report; 8 USDC/week | report URI + content hash |
| SignalWire | trading signals | 0.25 USDC/signal; 5 USDC/day | signal hash + publisher signature |
| AutoPilot | automated trading bot | 1 USDC/fee; 20 USDC/week | strategy ID + order receipt + verifier hash |
| DataPipe | paid API | 0.05 USDC/batch; 10 USDC/month | usage root + metering receipt |

The Android app can filter these services, apply any template, inspect its merchant/program/evidence boundaries, and replay `VERIFIED`, `BLOCKED`, and `FROZEN` requests against its own limits. Only AlphaBrief currently carries a full delivery → verification → settlement → bad-output freeze proof.

### Live AlphaBrief commercial chain

```text
purchase service
  → deliver a sourced 508-word report
  → SHA-256 content + settlement evidence hashes
  → independent verifier passes 8 delivery checks
  → v2 executor + verifier settle 2.0 project test tokens
  → Android delivery notification + activity timeline
  → later bad output fails 3 checks
  → verifier freezes allowance; zero tokens move
```

| Step | Public proof | Result |
| --- | --- | --- |
| Purchase | [`4PoT…KcfB`](https://explorer.solana.com/tx/4PoTejEJfrkJcNiCXSyvmypejWPW4dDh3C49Mi9GgkXpiZhK4NBgq5BGcJwuNM6BXCg725YcPJLEk5UN7BhbKcfB?cluster=devnet) | Dedicated AlphaBrief v2 allowance created |
| Delivery | [Risk brief](examples/alphabrief/deliveries/solana-mobile-commerce-risk-2026-09-10.md) | 508 words, required sections, and 3 captured sources |
| Independent verifier | Evidence `d6cb…345c` · PDA [`HXxs…h5rs`](https://explorer.solana.com/address/HXxs5MGfLGLZrgkep5XTu5aA8CCQyJnS56faijGLh5rs?cluster=devnet) | 8/8 checks pass |
| Automatic v2 settlement | [`5xSy…K1qB`](https://explorer.solana.com/tx/5xSyTrMr1iZ47VJcZKfLjLmE5E1XrzYABhWD7fJJsgz7SbKnhqewvHLNNBo3Lcs5uDNfVxmrwzVPMm4yZtY4K1qB?cluster=devnet) | Executor + verifier sign; subscriber authority absent; `2,000,000` raw units move |
| Bad output | Evidence `8d8b…1733` · [`39oy…WHSG`](https://explorer.solana.com/tx/39oy73khQh35jYZdrHtxXXxJnmFPpPRfRFQBD1gTAwthvq8GMMKfoDrhxedVnFERyXDCyexL11HYotmuWSG7WHSG?cluster=devnet) | 3 checks fail; allowance becomes FROZEN; zero token movement |

The complete record is [`evidence/live-alphabrief-v2.json`](evidence/live-alphabrief-v2.json). The settled asset is a project-created 6-decimal Solana Devnet test mint, **not canonical USDC**.

The **Seeker Integration Lab** also maps Allowance OS to apps featured by Solana Mobile, including Helium Mobile, Parallel Colony, Amp Pay, Moonwalk Fitness, and Perena. Every third-party card is explicitly labeled `BLUEPRINT · UNOFFICIAL`; it does not imply partnership or live integration. See [the UI and Seeker research note](docs/product-ui-and-seeker-research.md).

## Judge path

| Surface | What it proves | Status |
| --- | --- | --- |
| [Public Judge Demo](https://0xcaptain888.github.io/allowance-os/) | Instant `VERIFIED` / `BLOCKED` / `FROZEN` policy replay | GitHub Pages deployment |
| `mobile/android` | Bilingual native Android product with Daily Habits, upcoming charges, today/week spend, budget/anomaly alerts, local pause, delivery/payment timeline, notifications, weekly report, MWA, public v2 evidence, and authority-bound live v2 control review/broadcast | v0.16.0; 18 Android tests |
| `program/` | Backward-compatible v1 plus deployed Delegated Settlement v2 with PDA authority, executor/verifier separation, immutable evidence records, governed role rotation, three-level caps, period rollover, recovery and SPL revoke | Devnet deployed; 20 Rust tests; full control matrix verified |
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

## Live Delegated Settlement v2 matrix

Program: [`7zARK…xvL`](https://explorer.solana.com/address/7zARKWKDLawLgR7qokvQdkAv6ye2cXGNEvNQswBd6xvL?cluster=devnet) · Allowance: [`9CC1…jcbk`](https://explorer.solana.com/address/9CC1YRBNYkDB1iBSZ4FgWcZvkxatrJQ7akcfXQcAjcbk?cluster=devnet)

| Proof | Public transaction | What it proves |
| --- | --- | --- |
| DEPLOYED | [`21rb…gy7x`](https://explorer.solana.com/tx/21rb9fCR8Put2UDi3ANR1V5uzqLbjadozPsfcf7t3YxAV9YLuwSv7v7fNX4s66GrFNVc1RZfvAYiApBZxXCBgy7x?cluster=devnet) | Finalized v2 deployment; dumped binary exactly matches the public Ubuntu CI artifact |
| APPROVE ONCE | [`vpVb…VEax`](https://explorer.solana.com/tx/vpVbZD3WhHnBTa9MnWLWGmph8YsWDTErYdV9JENUadtvTQjxyj2md4aXt42KQtsB4xSX51r9K6yiRTG2jTFVEax?cluster=devnet) | Authority creates the bounded delegated allowance and SPL delegate PDA |
| SETTLE LATER | [`54Cj…YbU4`](https://explorer.solana.com/tx/54CjSJcs6QCkcr1SF3W8yJd1imEg1uCyZ2YS6qJp6AYougxoJwM5pfYcXjWkdTphcVDz1e7aNPswHvCYjviPYbU4?cluster=devnet) | Executor + verifier settle `1,000,000` raw units; authority is not a signer |
| BLOCKED | [`4gre…PVDk`](https://explorer.solana.com/tx/4gre7JQdqsfHb7f93NktFrwdsbLuUqucFm2GZB7Rq9Q3ZYhRX3uh3D771csrHR4htJLNq2DeFhLqHkj4yx91PVDk?cluster=devnet) | Custom Error `6`; zero tokens move and no evidence account is created |
| FROZEN | [`381K…BZS6`](https://explorer.solana.com/tx/381KRkTfUncSY7X1Rh2UN2aoqTWRTBgjVB7QPaKwwcphHoJRB1zTEn4DR1zXrw3NroqZREHt94fdurJ6xkaQBZS6?cluster=devnet) | Verifier persists an immutable bad-result Evidence Record PDA |
| ROLE ROTATION | [`4H1Q…2yjR`](https://explorer.solana.com/tx/4H1QniLQ46bDKGAzwL2QzzzQcr97em8n38JzdNFDaKsURPH1Sv9f8haTTNE5MVV5pqjHVYMVFXkcLTDZ7S9s2yjR?cluster=devnet) | Rotated executor + verifier settle another charge without the authority |
| REVOKED | [`2y4m…X1YWU`](https://explorer.solana.com/tx/2y4micc34Z3GtcRRdcZSTHPmmojd24oGhp58RtH7g6cYDj9A5KrmzBpvfsXBuJvmu38gkxxxugMG6HjbNLkX1YWU?cluster=devnet) | Terminal state persists and the SPL delegate is removed |

The full record also publishes pause, unpause, dual-signature unfreeze, actor identities, policy/evidence hashes, Evidence Record PDAs, token deltas, and final decoded state in [`evidence/live-devnet-v2.json`](evidence/live-devnet-v2.json). The asset remains a project-created Devnet test mint, not canonical USDC.

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
- a Daily Habits dashboard with upcoming charge projections, today/week verified spend, budget pressure, merchant anomaly alerts, one-tap local Pause, and a delivered-service/payment timeline;
- Android local notifications for upcoming requests, settlement, budget pressure, merchant anomalies, bad-output freeze, and pause/resume;
- an exportable weekly allowance safety report derived only from settlement-class audit events, never from simulated VERIFIED replays;
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
mobile/android/app/build/outputs/apk/debug/allowance-os-0.16.0-debug.apk
```

Latest clean local v0.16.0 debug build SHA-256: `5f4f6b9d16396ae5076737866ccac74ef2a68ca8422845c85607f69a7d93186c` (`19,134,977` bytes). The machine-readable build record is [`evidence/android-build.json`](evidence/android-build.json).

Latest public QA asset: [`android-test-v0.15.0`](https://github.com/0xCaptain888/allowance-os/releases/tag/android-test-v0.15.0), CI SHA-256 `a446a1d5ebdd62d8695d34ef5885b22da1ac6aab5d374f06556890ede92cb212`. Local and CI debug hashes differ because each environment uses its own debug signing key. `android-v*` is reserved for protected production-signed releases; `android-test-v*` assets are explicitly labelled as Devnet QA builds.

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
- Independent AlphaBrief delivery verifier with merchant, ID, timestamp, freshness, substance, section, source-count, and source-URL checks.
- Reproducible live AlphaBrief runner that creates a fresh allowance, settles accepted delivery, freezes bad output, and asserts exact token deltas.
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
npm run devnet:alphabrief-live
```

Reproduce the live Devnet settlement run with a funded test keypair and compatible token accounts:

```bash
SOLANA_KEYPAIR=/absolute/path/to/devnet-keypair.json \
TOKEN_MINT=<devnet-mint> \
SOURCE_TOKEN_ACCOUNT=<authority-owned-token-account> \
MERCHANT_TOKEN_ACCOUNT=<merchant-owned-token-account> \
npm run devnet:live
```

Reproduce the decisive approve-once / settle-later proof with separate authority, executor, and verifier keypairs:

```bash
DELEGATED_PROGRAM_ID=7zARKWKDLawLgR7qokvQdkAv6ye2cXGNEvNQswBd6xvL \
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

There are five intentionally separate evidence levels:

1. **SIMULATED** — browser and TypeScript policy replay; never a chain claim.
2. **WALLET-BROADCAST DEVNET PROOF** — real MWA authorization and Memo transaction returned by Phantom; the browser and Android app can independently query its Devnet confirmation status and slot.
3. **PROGRAM-ENFORCED STATE TRANSITIONS** — deployed Rust Program with public create, verified, blocked, frozen, and revoke evidence.
4. **SPL-TOKEN SETTLEMENT** — the VERIFIED transaction invokes the SPL Token Program and transfers exactly `1,000,000` raw units; BLOCKED and FROZEN are publicly shown to transfer zero.
5. **DELEGATED V2 CONTROL MATRIX** — a separate Devnet Program proves approve-once settlement without the authority signer, immutable Evidence Record PDAs, pause/freeze/recovery, role rotation, and SPL-delegate removal.

The v1 Program remains `DJzPBS7FreCcWWGkApzznGcKq9T7Da38GpKFtpxWRcuE`; the deployed v2 Program is `7zARKWKDLawLgR7qokvQdkAv6ye2cXGNEvNQswBd6xvL`. Both retain single-key upgrade authorities for hackathon iteration. They are deliberately separate so the older v1 evidence is never misattributed to v2.

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
