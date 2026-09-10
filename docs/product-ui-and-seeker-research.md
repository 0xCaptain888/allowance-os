# Product UI and Seeker integration research

Research checkpoint: 2026-09-10.

## What changed in v0.9.0

Allowance OS is no longer organized as a single ResearchPulse judge demo. The Android app and public site now use a commercial product structure:

```text
Home → Services → Allowances → Activity → Evidence
```

The design emphasizes three jobs a real customer needs to complete quickly:

1. understand total controlled spend and current risk;
2. choose or review a service-specific allowance;
3. verify why a charge passed, was blocked, or was frozen.

## Open-source UI references

These repositories were studied for patterns, not copied as a theme or dependency:

| Reference | Why it is useful | What Allowance OS adopted | License/status checked |
| --- | --- | --- | --- |
| [android/nowinandroid](https://github.com/android/nowinandroid) | Production-grade Compose navigation, state, spacing, and adaptive surfaces | Clear five-destination information architecture and reusable composables | Apache-2.0; active at the research checkpoint |
| [Ivy-Apps/ivy-wallet](https://github.com/Ivy-Apps/ivy-wallet) | Strong personal-finance hierarchy and budget-first mental model | Budget overview, allowance cards, and spend-first language | GPL-3.0; archived, used only as visual/product research |
| [ritesh-kanwar/Cashiro](https://github.com/ritesh-kanwar/Cashiro) | Modern finance timeline and privacy-first product framing | Activity hierarchy and readable transaction/audit summaries | AGPL-3.0; active at the research checkpoint |
| [TheChance101/beep-beep](https://github.com/TheChance101/beep-beep) | Polished Compose card, navigation, and status patterns | Compact status surfaces and stronger card rhythm | Apache-2.0; reference only |

The resulting visual system is original: deep navy surfaces, restrained violet/cyan gradients, high-contrast budget values, service identity tiles, softer typography, and fewer all-caps blocks.

## Commercial templates

`CommercialCatalog.kt` defines five reusable policies:

| Template | Customer job | Core boundary | Evidence required |
| --- | --- | --- | --- |
| AgentCloud | AI Agent subscription | metered run fee + monthly cap | signed run receipt + output hash |
| AlphaBrief | paid research report | price per report + weekly cap | report URI + content hash |
| SignalWire | trading signal subscription | price per signal + publisher lock | timestamped signal hash + publisher signature |
| AutoPilot | automated trading bot | fee budget separated from principal | strategy ID + order receipt + verifier hash |
| DataPipe | paid API | metered request fee + monthly cap | usage batch root + signed metering receipt |

These are `READY TEMPLATE` demonstrations of Allowance OS policy composition. They are not claims that the named generic merchants operate production services.

## Seeker ecosystem research

Solana Mobile's official article, [Seeker Season: 10 dApps You Should Download Right Now](https://solanamobile.com/blog/seeker-season-10-dapps-you-should-download-right-now), was used as the primary discovery source. The article explicitly features or references Parallel Colony, Backpack, Helium Mobile, Amp Pay, Moonwalk Fitness, and Perena.

The article is a featured-app editorial list, not a measured download ranking. Allowance OS therefore describes these apps as **featured by Solana Mobile**, not “the most downloaded apps.”

## Integration blueprints

The app includes an honest `Seeker Integration Lab` with the following designs:

- Helium Mobile: recurring plan and add-on data allowances;
- Parallel Colony: season pass, AI actions, and bounded item budgets;
- Amp Pay: merchant subscriptions and repeat-purchase allowances;
- Moonwalk Fitness: challenge entry and membership allowances;
- Perena: program-bound automation/service-fee allowances.

Every card is labeled `BLUEPRINT · UNOFFICIAL`. A third-party integration can only become `LIVE` after the app or protocol:

1. calls the Allowance OS pre-flight adapter;
2. binds the intended merchant, token, program, amount, and expiry;
3. returns a signed fulfillment or execution receipt;
4. supplies evidence that the independent verifier can recompute;
5. completes an end-to-end transaction authorized by the third party or a public integration contract.

This boundary prevents a UI concept from being confused with a partnership or production integration.
