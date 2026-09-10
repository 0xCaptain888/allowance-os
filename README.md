# Allowance OS

> Approve once. Enforce every charge. Revoke anytime.

**[Open the public Judge Demo](https://0xcaptain888.github.io/allowance-os/)** · **[Android MWA client](mobile/android)** · **[Two-minute judge guide](docs/judge-guide.md)**

**[Download versioned Android releases](https://github.com/0xCaptain888/allowance-os/releases)** · **[Product maturity audit](docs/product-maturity-audit.md)** · **[Changelog](CHANGELOG.md)** · **[Release process](docs/android-release-process.md)**

Allowance OS is a Solana Mobile payment-authorization layer for Web3 services. It turns an open-ended wallet approval into a human-readable allowance with merchant, token, program, per-charge, period, expiry, and evidence boundaries—then applies that same model to AI subscriptions, paid research, trading signals, automation bots, and metered APIs.

**v0.11.0 hardens the mobile trust boundary:** the MWA reconnect token is encrypted with Android Keystore, language choice persists, malformed amounts fail closed, wallet publishing requires a clear review step, MWA disconnect is no longer presented as onchain revocation, cleartext traffic is disabled, and a production-signing build path is prepared. The complete AlphaBrief merchant loop from v0.10 remains available.

> **Pre-production disclosure:** the repository proves policy evaluation, real MWA signing, deployed Devnet state transitions, and a recorded SPL-token transfer. The current Program still requires the authority signer for `Charge`, so it is not yet an autonomous “approve once, charge later” production protocol. See the [maturity audit](docs/product-maturity-audit.md).

## Commercial product proof

v0.11.0 keeps the five-surface product—**Home → Services → Allowances → Activity → Evidence**—while tightening session security and transaction truthfulness instead of adding another static use-case card.

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
| `mobile/android` | Bilingual native Android product with commercial catalog, AlphaBrief unlock/replay lab, encrypted MWA session, explicit action review, persistent activity audit, and direct Devnet RPC verification | v0.11.0; 12 Android tests |
| `program/` | Native Solana create / SPL-token charge / evidence-freeze / duplicate-evidence / revoke source | Existing v0.9.0 binary deployed; hardened source has 6 Rust tests |
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
  → REVOKED through MWA deauthorization
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
mobile/android/app/build/outputs/apk/debug/allowance-os-0.11.0-debug.apk
```

Local build SHA-256: `8fc1b16e2ecae5bd451bc2110a8093c6f0b0992f8ce87ee00524a9e72c455be3`

Published [`android-v0.10.0`](https://github.com/0xCaptain888/allowance-os/releases/tag/android-v0.10.0) CI asset SHA-256: `c583ddba4ee5f2596649b98133b67cf36fc2a9ea43e514ea104616e2c765af18`. The debug APKs use environment-specific debug signing keys, so the Release asset and its attached `SHA256SUMS.txt` are authoritative for public downloads.

See [`mobile/README.md`](mobile/README.md) for phone setup and [`docs/judge-guide.md`](docs/judge-guide.md) for the two-minute evaluation path.

## Core engine

- Deterministic policy and policy-hash generation.
- Per-charge and period caps.
- Merchant, token, program, expiry, allowance-ID, and evidence checks.
- Human-readable receipts for `VERIFIED`, `BLOCKED`, `FROZEN`, and `REVOKED`.
- SDK v2 request IDs, nonces, request expiry, idempotent retries, evidence-replay rejection, and HMAC-signed merchant webhooks.
- A signer-agnostic live adapter that accepts MWA or protected server executors without accepting wallet secrets.
- End-to-end AlphaBrief paid-content integration in TypeScript, Android, and the browser Demo.
- Native Solana instruction source for create, SPL-token CPI charge, evidence freeze, and revoke.
- Reproducible Rust dependency lockfile and six passing native program source tests.
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

## Truth boundary

There are four intentionally separate evidence levels:

1. **SIMULATED** — browser and TypeScript policy replay; never a chain claim.
2. **WALLET-BROADCAST DEVNET PROOF** — real MWA authorization and Memo transaction returned by Phantom; the browser and Android app can independently query its Devnet confirmation status and slot.
3. **PROGRAM-ENFORCED STATE TRANSITIONS** — deployed Rust Program with public create, verified, blocked, frozen, and revoke evidence.
4. **SPL-TOKEN SETTLEMENT** — the VERIFIED transaction invokes the SPL Token Program and transfers exactly `1,000,000` raw units; BLOCKED and FROZEN are publicly shown to transfer zero.

The deployed Rust Program ID is `DJzPBS7FreCcWWGkApzznGcKq9T7Da38GpKFtpxWRcuE`. Its upgrade authority remains the deployment wallet for hackathon iteration; this is disclosed rather than presented as immutable production infrastructure. The deployed binary is still the v0.9.0 evidence build; replay protection is proven in v0.10.0 SDK/Android/browser behavior and Rust source tests, but is not claimed for that older deployed binary.

See the [merchant SDK guide](docs/sdk-integration.md), [security notes](docs/security.md), [privacy policy](docs/privacy-policy.md), and [prepared dApp Store submission pack](docs/dapp-store-submission.md).

## Device modes

| Capability | Standard Android + Phantom | Seeker |
| --- | --- | --- |
| Mobile Wallet Adapter | Available | Available |
| Real Devnet signing | Available | Available |
| Seed Vault | Seeker only | Available |
| Genesis Token | Not available | Available |
| `SEEKER VERIFIED` label | Never shown | Requires real validation |

This allows meaningful real-device testing now without pretending that a standard Android handset supplies Seeker hardware capabilities. The Android app also exposes a portable receipt: its `SIMULATED` or `LIVE_DEVNET_PROOF` label and SHA-256 fingerprint make the evidence boundary explicit when a judge copies results out of the app.
