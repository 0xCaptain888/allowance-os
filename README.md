# Allowance OS

> Approve once. Enforce every charge. Revoke anytime.

**[Open the public Judge Demo](https://0xcaptain888.github.io/allowance-os/)** · **[Android MWA client](mobile/android)** · **[Two-minute judge guide](docs/judge-guide.md)**

Allowance OS is a Solana Mobile control center for safe on-chain subscriptions and autonomous-agent spending. It turns an open-ended wallet approval into a human-readable allowance with merchant, token, program, per-charge, period, expiry, and evidence boundaries.

## Judge path

| Surface | What it proves | Status |
| --- | --- | --- |
| [Public Judge Demo](https://0xcaptain888.github.io/allowance-os/) | Instant `VERIFIED` / `BLOCKED` / `FROZEN` policy replay | GitHub Pages deployment |
| `mobile/android` | Bilingual native Android control center, adjustable policy studio, period-cap enforcement, persistent activity audit, MWA authorization, and direct Devnet RPC verification | v0.7.0 compiled; 6 Android tests passed |
| `program/` | Native Solana create / charge / evidence-freeze / revoke logic | Deployed on Devnet; 5 Rust tests passed |
| Solana Explorer | Connected Devnet wallet and wallet-broadcast authorization proof | Live signature captured |
| [Deployed allowance program](https://explorer.solana.com/address/DJzPBS7FreCcWWGkApzznGcKq9T7Da38GpKFtpxWRcuE?cluster=devnet) | Program-enforced state transitions | Live `VERIFIED`, `BLOCKED`, `FROZEN`, and `REVOKED` evidence |
| SPL-token settlement | Token movement through CPI | Not yet implemented; never claimed |

The repository never labels a simulated receipt as a real transaction. The Android client only shows a Solana Explorer link after a wallet returns an actual Devnet signature. Program-enforced transitions and token settlement are also disclosed as separate evidence levels.

## Live Devnet proof matrix

Program: [`DJzPBS7FreCcWWGkApzznGcKq9T7Da38GpKFtpxWRcuE`](https://explorer.solana.com/address/DJzPBS7FreCcWWGkApzznGcKq9T7Da38GpKFtpxWRcuE?cluster=devnet)

Allowance account: [`4AzXfvZ6Ks2QFZFPTyAAzLL3vUWjzqUJguBvNUeoed9E`](https://explorer.solana.com/address/4AzXfvZ6Ks2QFZFPTyAAzLL3vUWjzqUJguBvNUeoed9E?cluster=devnet)

| Outcome | Public proof | What changed |
| --- | --- | --- |
| CREATED | [`okfc…2bTQ`](https://explorer.solana.com/tx/okfc2Z9S2ehRgLxtVrRKwZoB3KPJJWTJf7Zz6xDdTkwMvneCfJ3qzV42p4hWUeZ3NTQzwoZaS4MLeUtgy5K2bTQ?cluster=devnet) | Policy and required evidence hash committed onchain |
| VERIFIED | [`4fMA…JNfJ`](https://explorer.solana.com/tx/4fMAWn5T4gAjJz5ragh66eaNjk7hXfxWhjdSx2ojkDK4GAwNWhaZoNNdGKsvwngJXcz3LHN7HEWAnQWsP7f9JNfJ?cluster=devnet) | Matching evidence increments `spentInPeriod` to `1,000,000` |
| BLOCKED | [`5r8C…7QV5`](https://explorer.solana.com/tx/5r8Cd9ajUmWoSmVsMar5S5wdxrNL6C8aFfQ58GBpCGqJEGJGQUdhDLbKBPUqsURbQF92UWM3DmRR5Lnb1KA77QV5?cluster=devnet) | Over-cap request fails with Program Custom Error `6`; spend is unchanged |
| FROZEN | [`3qHE…vcWF`](https://explorer.solana.com/tx/3qHEpGrhwoVFkJfQj3ndr5bvKmebnTtJCE86bP5SHZo1Xx7wzMis8XNcb5dHYb7FWm8tajFaGrGG8ygmVWcJvcWF?cluster=devnet) | Evidence mismatch persists `frozen = true`; spend is unchanged |
| REVOKED | [`5gVQ…CBr7`](https://explorer.solana.com/tx/5gVQm2depgBZrMrMZc84ZGdvVVGfuwmmYeSyh5G9q1PbzTsNhfxoXyEdQ7ZsNGdcFAaWyPzcdesrQkYmSbT9CBr7?cluster=devnet) | Authority persists `revoked = true` after containment |

The complete machine-readable record is [`evidence/live-devnet-program.json`](evidence/live-devnet-program.json). These transactions prove policy state transitions, not an SPL-token transfer.

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
mobile/android/app/build/outputs/apk/debug/allowance-os-0.7.0-debug.apk
```

SHA-256: `0d0b0553d671f90884f5c99028ce33c46f8e7a95558cb059eecefd101be76e2c`

See [`mobile/README.md`](mobile/README.md) for phone setup and [`docs/judge-guide.md`](docs/judge-guide.md) for the two-minute evaluation path.

## Core engine

- Deterministic policy and policy-hash generation.
- Per-charge and period caps.
- Merchant, token, program, expiry, allowance-ID, and evidence checks.
- Human-readable receipts for `VERIFIED`, `BLOCKED`, `FROZEN`, and `REVOKED`.
- Native Solana instruction source for create, charge, evidence freeze, and revoke.
- Reproducible Rust dependency lockfile and five passing native program tests.
- Standard Android and Seeker device profiles.

Run the TypeScript verifier:

```bash
npm install
npm run typecheck
npm test
npm run demo
```

Reproduce the live Devnet state-transition run with a funded test keypair:

```bash
SOLANA_KEYPAIR=/absolute/path/to/devnet-keypair.json npm run devnet:live
```

## Truth boundary

There are three intentionally separate evidence levels:

1. **SIMULATED** — browser and TypeScript policy replay; never a chain claim.
2. **WALLET-BROADCAST DEVNET PROOF** — real MWA authorization and Memo transaction returned by Phantom; the browser and Android app can independently query its Devnet confirmation status and slot.
3. **PROGRAM-ENFORCED STATE TRANSITIONS** — deployed Rust Program with public create, verified, blocked, frozen, and revoke evidence.
4. **SPL-TOKEN SETTLEMENT** — token transfer through CPI; still pending and never implied by state-transition evidence.

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
