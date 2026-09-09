# Allowance OS

> Approve once. Enforce every charge. Revoke anytime.

**[Open the public Judge Demo](https://0xcaptain888.github.io/allowance-os/)** · **[Android MWA client](mobile/android)** · **[Two-minute judge guide](docs/judge-guide.md)**

Allowance OS is a Solana Mobile control center for safe on-chain subscriptions and autonomous-agent spending. It turns an open-ended wallet approval into a human-readable allowance with merchant, token, program, per-charge, period, expiry, and evidence boundaries.

## Judge path

| Surface | What it proves | Status |
| --- | --- | --- |
| [Public Judge Demo](https://0xcaptain888.github.io/allowance-os/) | Instant `VERIFIED` / `BLOCKED` / `FROZEN` policy replay | GitHub Pages deployment |
| `mobile/android` | Bilingual native Android control center, adjustable policy studio, period-cap enforcement, persistent activity audit, MWA authorization, and direct Devnet RPC verification | v0.8.0; SPL settlement verification added |
| `program/` | Native Solana create / SPL-token charge / evidence-freeze / revoke logic | Deployed on Devnet; 5 Rust tests passed |
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

- Chinese and English product interfaces with an in-app language switch;
- four judge-ready surfaces: Overview, Policy Studio, Activity Log, and Evidence Center;
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
mobile/android/app/build/outputs/apk/debug/allowance-os-0.8.0-debug.apk
```

SHA-256: `ae5d36a631046b80093230bb101e956ae70d2c65c2626791d815fee5554ce8c2`

See [`mobile/README.md`](mobile/README.md) for phone setup and [`docs/judge-guide.md`](docs/judge-guide.md) for the two-minute evaluation path.

## Core engine

- Deterministic policy and policy-hash generation.
- Per-charge and period caps.
- Merchant, token, program, expiry, allowance-ID, and evidence checks.
- Human-readable receipts for `VERIFIED`, `BLOCKED`, `FROZEN`, and `REVOKED`.
- Native Solana instruction source for create, SPL-token CPI charge, evidence freeze, and revoke.
- Reproducible Rust dependency lockfile and five passing native program tests.
- Standard Android and Seeker device profiles.

Run the TypeScript verifier:

```bash
npm install
npm run typecheck
npm test
npm run demo
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

The deployed Rust Program ID is `DJzPBS7FreCcWWGkApzznGcKq9T7Da38GpKFtpxWRcuE`. Its upgrade authority remains the deployment wallet for hackathon iteration; this is disclosed rather than presented as immutable production infrastructure.

## Device modes

| Capability | Standard Android + Phantom | Seeker |
| --- | --- | --- |
| Mobile Wallet Adapter | Available | Available |
| Real Devnet signing | Available | Available |
| Seed Vault | Seeker only | Available |
| Genesis Token | Not available | Available |
| `SEEKER VERIFIED` label | Never shown | Requires real validation |

This allows meaningful real-device testing now without pretending that a standard Android handset supplies Seeker hardware capabilities. The Android app also exposes a portable receipt: its `SIMULATED` or `LIVE_DEVNET_PROOF` label and SHA-256 fingerprint make the evidence boundary explicit when a judge copies results out of the app.
