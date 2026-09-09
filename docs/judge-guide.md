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
3. On **Policy**, change the requested amount, merchant identity, and evidence availability.
4. Run `BLOCKED` or `FROZEN` and confirm Phantom is not opened.
5. Run `VERIFIED`, then publish the real Devnet authorization proof.
6. Approve the transaction in Phantom.
7. On **Evidence**, inspect the four-layer verification pipeline and open the signature in Solana Explorer.
8. Test **Reconnect**, **Forget local wallet session**, and **Revoke & deauthorize** without exposing a wallet key.

## Evidence labels

| Label | Meaning |
| --- | --- |
| SIMULATED | Deterministic local policy replay |
| LIVE DEVNET PROOF | Real wallet-broadcast Memo authorization evidence |
| PROGRAM SETTLEMENT | Only after the Rust program is deployed and executes a token charge |

The current submission does not conflate these levels.
