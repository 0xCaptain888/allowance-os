# Two-Minute Judge Guide

> Current QA build: **v0.18.0**. It adds one-tap Judge Run and a real wallet-approved `0.00001 Devnet SOL` AlphaBrief micro-settlement with evidence-hash binding and RPC balance-delta verification. The separate delegated-v2 proof still uses a project-created `TEST` mint, not canonical USDC.

## 1. Understand the product in 20 seconds

Allowance OS is the missing control plane between a one-time wallet approval and repeated subscription or autonomous-agent charges.

The policy binds:

```text
merchant + token + program + per-charge cap + period cap + expiry + evidence
```

The decisive commercial proof is AlphaBrief: a real report is delivered, independently verified, settled through v2, then a later bad output is frozen with zero token movement.

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
2. Connect from the Header or the Home hero and approve Solana Devnet authorization in Solflare. No scrolling is required.
3. On **Daily Habits**, inspect upcoming charges, today/week spend, budget pressure, merchant anomaly status, and the weekly safety report.
4. Tap **Verify live AlphaBrief proof** to link the real AlphaBrief settlement and freeze signatures into the device timeline and trigger local delivery/freeze notifications.
5. Use **One-tap Pause** and confirm `LOCAL PAUSE ON` appears at the top of the control-center card. It is a local request stop—not an onchain pause transaction. Then tap **Resume** or **Reset interactive demo** before testing live requests.
6. On Home, tap `VERIFIED`, `BLOCKED`, and `FROZEN`; each local replay must update the decision card even while live requests are paused. `BLOCKED` deliberately exceeds the policy cap; it does not mean the wallet balance is insufficient.
7. In **Allowance center**, change the requested amount, current-period spend, merchant identity, and evidence availability. Use **Use charge cap** and **Reset period spend** to restore a passing baseline.
8. Run `BLOCKED` or `FROZEN` and confirm the wallet is not opened.
9. For the fastest complete path, tap **Start Judge Run**. It clears local Pause, runs the three local outcomes, verifies both public evidence layers, then pauses once for an explicit wallet approval.
10. Review the exact recipient and `0.00001 Devnet SOL` amount in Solflare, approve, and wait for the App to confirm both the transaction and merchant balance delta.
11. On **Evidence**, inspect the settlement summary, expand raw receipt JSON only if needed, and open the transaction or advanced Program/v2 evidence.

The Android v0.16 source contains a strict 332-byte Delegated Allowance v2 state decoder plus authority-bound Pause, Unpause, and Revoke review/broadcast controls. Its Evidence and Activity pages link directly to the AlphaBrief allowance, accepted/frozen evidence hashes, and public transactions. Create, rotation, freeze/unfreeze, and delegated settlement remain operator flows; the public evidence allowance is already revoked.

## 4. Verify the real AlphaBrief chain

| Step | Proof | Expected result |
| --- | --- | --- |
| Purchase | [`4PoT…KcfB`](https://explorer.solana.com/tx/4PoTejEJfrkJcNiCXSyvmypejWPW4dDh3C49Mi9GgkXpiZhK4NBgq5BGcJwuNM6BXCg725YcPJLEk5UN7BhbKcfB?cluster=devnet) | Dedicated v2 allowance exists |
| Delivery | [`examples/alphabrief/deliveries/...md`](../examples/alphabrief/deliveries/solana-mobile-commerce-risk-2026-09-10.md) | 508 words, required sections, 3 sources |
| Settle | [`5xSy…K1qB`](https://explorer.solana.com/tx/5xSyTrMr1iZ47VJcZKfLjLmE5E1XrzYABhWD7fJJsgz7SbKnhqewvHLNNBo3Lcs5uDNfVxmrwzVPMm4yZtY4K1qB?cluster=devnet) | Executor + verifier; subscriber absent; 2,000,000 raw units move |
| Freeze | [`39oy…WHSG`](https://explorer.solana.com/tx/39oy73khQh35jYZdrHtxXXxJnmFPpPRfRFQBD1gTAwthvq8GMMKfoDrhxedVnFERyXDCyexL11HYotmuWSG7WHSG?cluster=devnet) | Bad output creates freeze evidence; zero token movement |

Read [`evidence/live-alphabrief-v2.json`](../evidence/live-alphabrief-v2.json) for every actor, hash, PDA, check, and balance delta.

## 5. Inspect the real Program path

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
| LIVE DEVNET PROOF | Historical wallet-broadcast Memo authorization evidence; never relabelled as settlement |
| LIVE MOBILE SETTLEMENT | MWA-approved `0.00001 Devnet SOL` transfer plus accepted evidence hash, verified by RPC |
| LIVE DEVNET RPC | Read-only checks of public signatures, Program state, and balance deltas |
| PROGRAM ENFORCEMENT | Real deployed Program and persistent state transitions |
| SPL TOKEN SETTLEMENT | Real VERIFIED CPI transfer using a project-created Devnet test mint; not canonical USDC |

The current submission does not conflate these levels.

## 6. Verify approve-once / settle-later v2

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
