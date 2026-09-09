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

Every browser replay is visibly labelled as simulated. Then scroll to **Program-enforced SPL settlement** and click **Verify state + token settlement via Solana RPC**. The page independently checks five public signatures, the final allowance account, and VERIFIED/BLOCKED/FROZEN token-balance deltas.

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
| CREATED | [`kmei…rHYH`](https://explorer.solana.com/tx/kmeiVR39tEkX1Rgo4pkNHz784anFvi3eX1J3YYeyry6pAPHKwvJNsEMaxqSLo1grJkZ5fSrNmKoQrMiVdTnrHYH?cluster=devnet) | Policy, merchant, mint, and required evidence hash stored |
| VERIFIED | [`22xK…SPUU`](https://explorer.solana.com/tx/22xKvkfk2YwEV6mVQXmhGeGFWSSTfvE9FGPLGJ7McSf93DpxuntZYLE8mjdv7SugEmMoo3rvswKa7eMfzf9nSPUU?cluster=devnet) | SPL CPI moves `1,000,000` raw units; source `20 → 19`, merchant `0 → 1` |
| BLOCKED | [`EU6b…vted`](https://explorer.solana.com/tx/EU6bUcBTdpxCMu9rRBhDqQjPwnvPLtGmDtKepZnRZK5rKnRdeiWGqVvpKerPRAheddBgkhmpQgNdcTVZCCgvted?cluster=devnet) | Transaction fails with Custom Error `6`; token delta is zero |
| FROZEN | [`5REq…MLpu`](https://explorer.solana.com/tx/5REqbiziiN5bAWPDYDMSq7TgS3rvMVUeW9UmqfMdzpfCPxYctWxeQa83beR14tHbSSdTRWg1P7jbWasE5U37MLpu?cluster=devnet) | Mismatched evidence persists `frozen = true`; token delta is zero |
| REVOKED | [`5Mpt…qysu`](https://explorer.solana.com/tx/5MpttoR2mfpDXhWq1B3nr6AWHC9AFCTsQ7626EXDEka6nWzgF3GdJGZnTxk9BmU4sBxiLjAAbbES9GXheGCqqysu?cluster=devnet) | Authority persists `revoked = true`; token delta is zero |

## Evidence labels

| Label | Meaning |
| --- | --- |
| SIMULATED | Deterministic local policy replay |
| LIVE DEVNET PROOF | Real wallet-broadcast Memo authorization evidence |
| PROGRAM ENFORCEMENT | Real deployed Program and persistent state transitions |
| SPL TOKEN SETTLEMENT | Real VERIFIED CPI transfer using a project-created Devnet test mint; not canonical USDC |

The current submission does not conflate these levels.
