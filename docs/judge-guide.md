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
10. Open **Activity**, export the complete audit trail as JSON, and compare its displayed SHA-256 fingerprint before sharing it with another reviewer.
11. Test **Reconnect**, **Forget local wallet session**, and **Revoke & deauthorize** without exposing a wallet key.

The Android v0.14 source contains a strict 332-byte Delegated Allowance v2 state decoder plus pause, unpause, revoke, executor-rotation, and verifier-rotation instruction encoders. Its Evidence page links directly to the deployed v2 Program and authority-free settlement. The codecs are tested; not every v2 authority control is wired to mobile broadcasting yet.

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

## 5. Verify approve-once / settle-later v2

The dedicated v2 Program is [`7zARK…xvL`](https://explorer.solana.com/address/7zARKWKDLawLgR7qokvQdkAv6ye2cXGNEvNQswBd6xvL?cluster=devnet).

| Proof | Transaction | Expected verification |
| --- | --- | --- |
| DEPLOYED | [`21rb…gy7x`](https://explorer.solana.com/tx/21rb9fCR8Put2UDi3ANR1V5uzqLbjadozPsfcf7t3YxAV9YLuwSv7v7fNX4s66GrFNVc1RZfvAYiApBZxXCBgy7x?cluster=devnet) | Finalized deployment; dumped binary matches public Ubuntu CI artifact |
| APPROVE ONCE | [`vpVb…VEax`](https://explorer.solana.com/tx/vpVbZD3WhHnBTa9MnWLWGmph8YsWDTErYdV9JENUadtvTQjxyj2md4aXt42KQtsB4xSX51r9K6yiRTG2jTFVEax?cluster=devnet) | Authority creates the v2 allowance and bounded SPL delegate |
| SETTLE LATER | [`54Cj…YbU4`](https://explorer.solana.com/tx/54CjSJcs6QCkcr1SF3W8yJd1imEg1uCyZ2YS6qJp6AYougxoJwM5pfYcXjWkdTphcVDz1e7aNPswHvCYjviPYbU4?cluster=devnet) | Executor + verifier signatures; authority absent; exactly `1,000,000` raw units move |
| BLOCKED | [`4gre…PVDk`](https://explorer.solana.com/tx/4gre7JQdqsfHb7f93NktFrwdsbLuUqucFm2GZB7Rq9Q3ZYhRX3uh3D771csrHR4htJLNq2DeFhLqHkj4yx91PVDk?cluster=devnet) | Custom Error `6`, zero movement, no Evidence Record |
| FROZEN | [`381K…BZS6`](https://explorer.solana.com/tx/381KRkTfUncSY7X1Rh2UN2aoqTWRTBgjVB7QPaKwwcphHoJRB1zTEn4DR1zXrw3NroqZREHt94fdurJ6xkaQBZS6?cluster=devnet) | Verifier persists immutable rejected-result evidence |
| ROTATED | [`4H1Q…2yjR`](https://explorer.solana.com/tx/4H1QniLQ46bDKGAzwL2QzzzQcr97em8n38JzdNFDaKsURPH1Sv9f8haTTNE5MVV5pqjHVYMVFXkcLTDZ7S9s2yjR?cluster=devnet) | Rotated executor + verifier settle without authority |
| REVOKED | [`2y4m…X1YWU`](https://explorer.solana.com/tx/2y4micc34Z3GtcRRdcZSTHPmmojd24oGhp58RtH7g6cYDj9A5KrmzBpvfsXBuJvmu38gkxxxugMG6HjbNLkX1YWU?cluster=devnet) | Final state is revoked/paused and SPL delegate is removed |

The source/merchant asset is a project-created Devnet test mint, not canonical USDC. Read [`evidence/live-devnet-v2.json`](../evidence/live-devnet-v2.json) for all actors, PDAs, hashes, pause/recovery/rotation transactions, and final decoded state.
