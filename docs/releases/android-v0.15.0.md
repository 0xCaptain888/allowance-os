# Allowance OS Android / SDK v0.15.0

Date: September 10, 2026
Status: local Devnet QA build verified; public test release pending

## Headline

AlphaBrief is no longer one of five equivalent templates. It is a complete, publicly verifiable service-commerce chain:

```text
purchase → deliver report → evidence hash → independent verifier
→ Delegated Settlement v2 → Android notification
→ bad output → FROZEN with zero token movement
```

## Public commercial proof

- real 508-word report with required sections and 3 captured sources;
- independent verifier passes 8 checks on accepted output;
- accepted content hash: `e9c1a18e…e5841e`;
- accepted evidence hash: `d6cb3d5b…4a345c`;
- executor + verifier settle `2,000,000` raw test-token units without the subscriber authority signing the charge;
- bad output fails substance, sections, and source-count checks;
- verifier freezes the allowance and token movement remains zero;
- complete record: `evidence/live-alphabrief-v2.json`.

## Daily Habits

- upcoming charge projection with evidence requirement;
- today and seven-day verified spend, excluding simulated policy replays;
- budget pressure and merchant anomaly alerts;
- one-tap local Pause/Resume with explicit non-onchain disclosure;
- delivered-service and payment timeline;
- Android notifications for upcoming requests, settlement, budget, anomalies, freezes, and pause;
- exportable weekly allowance safety report.

## Verification

- TypeScript: 26 passed;
- Android: 18 passed;
- Rust Program: 20 passed;
- npm audit: 0 vulnerabilities;
- local APK: `allowance-os-0.15.0-debug.apk`;
- local SHA-256: `2feaad9180dee1b3c4c72796ed7a3789d9f21bb16ed94c5c5c20fa7d1ead6a05`.

## Truth boundary

The settlement is real Solana Devnet behavior using a project-created 6-decimal test mint, not canonical USDC. Browser Daily Habits controls are read-only/local previews. Android Pause currently blocks app-side requests and is not presented as an onchain pause transaction. The APK is debug-signed and not a production dApp Store release.
