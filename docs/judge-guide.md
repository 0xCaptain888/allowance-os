# Two-Minute Judge Guide

## 1. Understand the product in 20 seconds

Allowance OS is the missing control plane between a one-time wallet approval and repeated subscription or autonomous-agent charges.

The policy binds:

```text
merchant + token + program + per-charge cap + period cap + expiry + evidence
```

## 2. Replay the safety matrix in 40 seconds

Open `site/index.html` and run:

- `VERIFIED`: all identity, budget, program, and evidence checks pass;
- `BLOCKED`: the requested amount exceeds the allowance before any wallet request;
- `FROZEN`: merchant identity or evidence changed after authorization;
- `REVOKED`: the user terminates future authorization.

Every browser replay is visibly labelled as simulated. Then scroll to **Program-enforced evidence** and click **Verify the full matrix via Solana RPC**. The page independently checks five public signatures plus the final allowance account.

## 3. Verify the native path in 60 seconds

Build or install the Android app from `mobile/android`:

1. Use the header switch to inspect the complete Chinese or English interface.
2. On **Overview**, tap **Connect Phantom / MWA Wallet** and approve Solana Devnet authorization.
3. Tap **Judge mode** to generate the three policy outcomes and open **Activity**; the audit trail should contain VERIFIED, BLOCKED, and FROZEN decisions.
4. On **Policy**, change the requested amount, current-period spend, merchant identity, and evidence availability.
5. Run `BLOCKED` or `FROZEN` and confirm Phantom is not opened.
6. Run `VERIFIED`, then publish the real Devnet authorization proof.
7. Approve the transaction in Phantom.
8. On **Evidence**, run **Independent RPC Verification** and inspect confirmation status plus slot without leaving the app.
9. Copy the portable JSON receipt and its SHA-256 fingerprint, then open the signature in Solana Explorer for a second source.
10. Open **Activity** to inspect the persistent local audit trail and decision counters.
11. Test **Reconnect**, **Forget local wallet session**, and **Revoke & deauthorize** without exposing a wallet key.

## 4. Inspect the real Program path

The deployed Devnet Program is [`DJzPBS7FreCcWWGkApzznGcKq9T7Da38GpKFtpxWRcuE`](https://explorer.solana.com/address/DJzPBS7FreCcWWGkApzznGcKq9T7Da38GpKFtpxWRcuE?cluster=devnet).

| State | Transaction | Expected verification |
| --- | --- | --- |
| CREATED | [`okfc…2bTQ`](https://explorer.solana.com/tx/okfc2Z9S2ehRgLxtVrRKwZoB3KPJJWTJf7Zz6xDdTkwMvneCfJ3qzV42p4hWUeZ3NTQzwoZaS4MLeUtgy5K2bTQ?cluster=devnet) | Policy and required evidence hash stored |
| VERIFIED | [`4fMA…JNfJ`](https://explorer.solana.com/tx/4fMAWn5T4gAjJz5ragh66eaNjk7hXfxWhjdSx2ojkDK4GAwNWhaZoNNdGKsvwngJXcz3LHN7HEWAnQWsP7f9JNfJ?cluster=devnet) | Spend advances to `1,000,000` |
| BLOCKED | [`5r8C…7QV5`](https://explorer.solana.com/tx/5r8Cd9ajUmWoSmVsMar5S5wdxrNL6C8aFfQ58GBpCGqJEGJGQUdhDLbKBPUqsURbQF92UWM3DmRR5Lnb1KA77QV5?cluster=devnet) | Transaction fails with Custom Error `6`; spend does not change |
| FROZEN | [`3qHE…vcWF`](https://explorer.solana.com/tx/3qHEpGrhwoVFkJfQj3ndr5bvKmebnTtJCE86bP5SHZo1Xx7wzMis8XNcb5dHYb7FWm8tajFaGrGG8ygmVWcJvcWF?cluster=devnet) | Mismatched evidence persists `frozen = true` |
| REVOKED | [`5gVQ…CBr7`](https://explorer.solana.com/tx/5gVQm2depgBZrMrMZc84ZGdvVVGfuwmmYeSyh5G9q1PbzTsNhfxoXyEdQ7ZsNGdcFAaWyPzcdesrQkYmSbT9CBr7?cluster=devnet) | Authority persists `revoked = true` |

## Evidence labels

| Label | Meaning |
| --- | --- |
| SIMULATED | Deterministic local policy replay |
| LIVE DEVNET PROOF | Real wallet-broadcast Memo authorization evidence |
| PROGRAM ENFORCEMENT | Real deployed Program and state transitions, without token movement |
| SPL TOKEN SETTLEMENT | Only after an explicit token-transfer CPI is added |

The current submission does not conflate these levels.
