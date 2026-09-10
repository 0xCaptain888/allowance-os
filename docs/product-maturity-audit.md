# Product maturity audit

Date: September 10, 2026

Allowance OS is a strong hackathon proof and an increasingly credible Android reference client. It is not yet a production payment product. This audit deliberately separates shipped behavior from the architecture and operations a real commercial launch still requires.

## Product scorecard

| Area | Current maturity | What is credible now | Remaining launch gate |
| --- | ---: | --- | --- |
| Core policy idea | 8.5/10 | Merchant, exact mint, decimals, integer raw amount, program, period and evidence boundaries | Validate the model with real merchants and users |
| Android UX | 8/10 | Bilingual five-surface app, service templates, MWA, evidence inspection, JSON audit export, v2 state/control codecs, and public v2 proof links | Live mobile broadcasting for all v2 controls, instrumented wallet flows, accessibility/device matrix and usability sessions |
| Wallet security | 7.5/10 | No private-key custody; fail-closed Keystore session restoration and stale-identity clearing in v0.11.1 | Instrumented recovery tests, external mobile security review and compromised-device response |
| Onchain enforcement | 9/10 | Public v1 proof plus deployed v2 authority-free settlement, exact binary match, immutable evidence, fail-closed block, pause/freeze/recovery, role rotation, and delegate-removing revoke | Independent audit, canonical asset deployment, multisig/timelock, monitored Mainnet rollout |
| Merchant platform | 7.5/10 | Integer raw-unit SDK, canonical hashes, restart-safe reference persistence, idempotency, evidence replay protection, webhook replay windows and secret rotation | Transactional database, dashboard, delivery queue, rate limits and merchant authentication |
| Reliability | 7/10 | 59 source/unit tests, deterministic verifier, configurable mobile RPC with bounded retries, HTTP-only deployment confirmation, safe runner resume, and an incident runbook | Instrumentation/E2E tests, multi-endpoint production failover, offline UX and operational monitoring |
| Distribution | 5/10 | Production and debug release channels are separated; production tags require protected signing | Configure the protected key, produce the first signed APK, capture screenshots, then complete Publisher Portal review |
| Legal/support | 4/10 | Privacy disclosure, responsible-disclosure policy, incident runbook and public issue tracker | Terms, jurisdiction review, staffed support SLA and production token/risk disclosures |

## P0 — required before calling it a mature payment app

1. **Complete live v2 Android integration.** Android strictly decodes v2 state, encodes pause/unpause/revoke/operator-rotation instructions, and exposes public evidence. Add live allowance selection, transaction review, MWA broadcast, confirmation, and post-state inspection while distinguishing user, executor, verifier, and merchant roles.
2. **Harden governance and verification.** The deployed binary matches the public Ubuntu artifact, but the upgrade authority must move to a multisig/timelock after iteration and the Program needs independent review.
3. **Deploy production asset configuration deliberately.** The public proof uses a project-created Devnet test mint. Canonical mint, decimals, merchant accounts, value caps, and network configuration must be reviewed before any Mainnet beta.
4. **Operationalize recovery and verification.** Define incident handling, a lost-verifier emergency path, evidence-account retention economics, and delay/quorum policy for higher-value verification and unfreeze decisions.
5. **Production-sign and review the Android APK.** Use one protected signing identity, test update compatibility, run static/mobile security review, and submit the exact reviewed hash.

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
Delegated settlement design + threat model [complete]
  → Program v2 implementation and tests [complete]
  → Devnet deployment + exact binary match + full control matrix [complete]
  → Android live create / inspect / pause / revoke integration [next product gate]
  → merchant SDK backed by transactional production storage
  → production signing + device QA
  → limited merchant/user pilot
  → dApp Store submission
```
