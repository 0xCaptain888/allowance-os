# Product maturity audit

Date: September 10, 2026

Allowance OS is a strong hackathon proof and an increasingly credible Android reference client. It is not yet a production payment product. This audit deliberately separates shipped behavior from the architecture and operations a real commercial launch still requires.

## Product scorecard

| Area | Current maturity | What is credible now | Remaining launch gate |
| --- | ---: | --- | --- |
| Core policy idea | 8.5/10 | Merchant, exact mint, decimals, integer raw amount, program, period and evidence boundaries | Validate the model with real merchants and users |
| Android UX | 7.5/10 | Bilingual five-surface app, service templates, MWA, evidence inspection, JSON audit export, and v2 state/control codecs | Instrumented wallet flows, accessibility/device matrix and usability sessions |
| Wallet security | 7.5/10 | No private-key custody; fail-closed Keystore session restoration and stale-identity clearing in v0.11.1 | Instrumented recovery tests, external mobile security review and compromised-device response |
| Onchain enforcement | 7.5/10 | Public v1 Devnet settlement plus compiled/tested v2 delegate PDA, immutable evidence history, role rotation, period rollover and recovery source | Deploy/verify v2, publish authority-free charge evidence, audit and multisig |
| Merchant platform | 7.5/10 | Integer raw-unit SDK, canonical hashes, restart-safe reference persistence, idempotency, evidence replay protection, webhook replay windows and secret rotation | Transactional database, dashboard, delivery queue, rate limits and merchant authentication |
| Reliability | 6/10 | 59 source/unit tests, deterministic verifier, configurable mobile RPC with bounded retries, and an incident runbook | Instrumentation/E2E tests, multi-endpoint production failover, offline UX and operational monitoring |
| Distribution | 5/10 | Production and debug release channels are separated; production tags require protected signing | Configure the protected key, produce the first signed APK, capture screenshots, then complete Publisher Portal review |
| Legal/support | 4/10 | Privacy disclosure, responsible-disclosure policy, incident runbook and public issue tracker | Terms, jurisdiction review, staffed support SLA and production token/risk disclosures |

## P0 — required before calling it a mature payment app

1. **Deploy and prove Delegated Settlement v2.** The source now implements an allowance-scoped SPL delegate PDA, authority-free later charges, four-way actor separation, sequential nonces, immutable evidence PDAs, period/lifetime accounting, governed role rotation, and recovery. The deployed v0.9 Program still requires the user signer, so v2 must be deployed and demonstrated with public create, later charge, evidence-account decoding, pause, freeze, rotation, unfreeze, and revoke transactions before the product promise is considered live.
2. **Verify the hardened Program.** Publish the v2 binary hash, matching local build, stable instruction specification, and machine-readable evidence; rotate upgrade authority to a multisig/timelock after iteration.
3. **Complete live v2 Android integration.** Android now strictly decodes v2 state and encodes pause/unpause/revoke/operator-rotation instructions. After deployment, add live account selection, transaction review, MWA broadcast, confirmation, and post-state inspection while distinguishing user, executor, verifier, and merchant roles.
4. **Operationalize recovery and verification.** Define incident handling, a lost-verifier emergency path, evidence-account retention economics, and delay/quorum policy for higher-value verification and unfreeze decisions.
5. **Use production assets deliberately.** Configure the canonical mint, decimals, merchant token accounts and network. Never infer decimals or present a project-created test token as USDC.
6. **Production-sign and review the Android APK.** Use one protected signing identity, test update compatibility, run static/mobile security review, and submit the exact reviewed hash.

## P1 — required for credible early access

- first-run onboarding that explains Devnet, simulation, wallet proof and actual settlement, with a decline/exit route;
- transaction review that always shows network, asset movement, merchant, program, amount, cap and evidence before opening a wallet;
- instrumented tests for Keystore loss, wallet account switching and reconnect-token expiry, plus a separate onchain revoke action;
- transactional multi-writer merchant storage instead of the atomic JSON reference store;
- durable webhook replay cache, secret management, delivery retry/dead-letter queue and observability;
- multi-endpoint RPC health/failover, offline and partial-failure UX, plus readable error recovery;
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
Delegated settlement design + threat model [complete in source]
  → Program v2 implementation and tests [complete in source]
  → new Devnet deployment + verified binary [next external gate]
  → Android live create / inspect / pause / revoke integration
  → merchant SDK backed by transactional production storage
  → production signing + device QA
  → limited merchant/user pilot
  → dApp Store submission
```
