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

Every browser hash is visibly labelled as simulated.

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

## Evidence labels

| Label | Meaning |
| --- | --- |
| SIMULATED | Deterministic local policy replay |
| LIVE DEVNET PROOF | Real wallet-broadcast Memo authorization evidence |
| PROGRAM SETTLEMENT | Only after the Rust program is deployed and executes a token charge |

The current submission does not conflate these levels.
