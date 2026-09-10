# Product maturity audit

Date: September 10, 2026

Allowance OS is a strong hackathon proof and an increasingly credible Android reference client. It is not yet a production payment product. This audit deliberately separates shipped behavior from the architecture and operations a real commercial launch still requires.

## Product scorecard

| Area | Current maturity | What is credible now | Remaining launch gate |
| --- | ---: | --- | --- |
| Core policy idea | 8/10 | Merchant, token, program, amount, period and evidence boundaries | Validate the model with real merchants and users |
| Android UX | 7/10 | Bilingual five-surface app, service templates, MWA and evidence inspection | Onboarding, accessibility/device matrix and usability sessions |
| Wallet security | 7/10 | No private-key custody; MWA token encrypted with Android Keystore in v0.11 | External mobile security review and compromised-device response |
| Onchain enforcement | 5/10 | Public Devnet transitions and SPL-token settlement | Delegate/PDA recurring-charge architecture, period rollover, recovery and multisig |
| Merchant platform | 6/10 | SDK v2, idempotency, evidence replay protection and signed webhooks | Durable database, dashboard, key rotation, rate limits and merchant authentication |
| Reliability | 5/10 | Unit tests and deterministic verifier | Instrumentation/E2E tests, RPC failover, offline UX and operational monitoring |
| Distribution | 4/10 | Listing pack and release-signing build path | Protected production key, signed APK, screenshots, Publisher Portal and review |
| Legal/support | 3/10 | Privacy disclosure and public issue tracker | Terms, jurisdiction review, incident response, support SLA and token/risk disclosures |

## P0 — required before calling it a mature payment app

1. **Implement real delegated settlement.** The deployed v0.9 Devnet Program requires the authority signer during `Charge`. That proves policy enforcement, but it does not yet deliver the product promise of approving once and permitting bounded later charges. Introduce a user-approved SPL delegate or program-owned escrow/PDA, merchant-signed charge requests, nonce accounts, and authority-free settlement within the committed limits.
2. **Deploy the hardened Program.** Deploy the source that includes replay protection, verify its binary, rotate upgrade authority to a multisig/timelock, and publish an IDL or stable instruction specification.
3. **Implement period accounting.** Store period start/duration and safely roll windows onchain. The present Program stores cumulative `spent_in_period` without a reset instruction.
4. **Ship onchain recovery.** Distinguish pause, freeze, revoke and unfreeze. Recovery must require an auditable user or guardian decision; it cannot remain a UI label.
5. **Use production assets deliberately.** Configure the canonical mint, decimals, merchant token accounts and network. Never infer decimals or present a project-created test token as USDC.
6. **Production-sign and review the Android APK.** Use one protected signing identity, test update compatibility, run static/mobile security review, and submit the exact reviewed hash.

## P1 — required for credible early access

- first-run onboarding that explains Devnet, simulation, wallet proof and actual settlement;
- transaction review that always shows network, asset movement, merchant, program, amount, cap and evidence before opening a wallet;
- encrypted reconnect tokens, explicit wallet disconnect, and a separate onchain revoke action;
- durable merchant storage instead of in-memory SDK maps;
- webhook timestamp tolerance, replay cache, secret rotation and delivery retry policy;
- RPC failover/backoff, offline and partial-failure UX, plus readable error recovery;
- Android instrumentation tests covering MWA return paths, process death and app upgrades;
- TalkBack labels, dynamic font testing, contrast audit, reduced-motion behavior and tablet/foldable layouts;
- export/delete controls for local activity and a clear retention policy;
- merchant verification or domain binding so human-readable identities are not arbitrary labels;
- support channel, incident-response procedure and a public status/security contact.

## P2 — commercial expansion after safety gates

- merchant console and signed integration credentials;
- allowance analytics without invasive user tracking;
- multiple active allowances, search, pause schedules and renewal reminders;
- stable API/SDK versioning, sample apps and integration certification;
- multi-verifier or oracle policy for high-value delivery evidence;
- Mainnet beta with strict value caps and an opt-in cohort;
- independent audit and responsible-disclosure or bug-bounty program.

## Language that must remain precise

- `Disconnect wallet` means removing the MWA session; it does **not** revoke an onchain allowance.
- `Devnet Memo proof` proves a wallet authorized a message; it does **not** move USDC.
- `Program-enforced settlement` refers only to the public recorded Program transactions.
- Browser and judge-mode receipts are `SIMULATED`.
- The current deployment is not a production recurring-payment protocol.

## Recommended next build order

```text
Delegated settlement design + threat model
  → Program v2 implementation and tests
  → new Devnet deployment + verified binary
  → Android create / inspect / pause / revoke integration
  → merchant SDK backed by durable storage
  → production signing + device QA
  → limited merchant/user pilot
  → dApp Store submission
```
