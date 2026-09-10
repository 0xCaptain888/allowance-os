# AlphaBrief — Solana Mobile Commerce Snapshot

Allowance-based payments reduce repeated wallet prompts without giving a merchant unlimited authority. The report is delivered first, hashed, policy-checked, and unlocked only after a VERIFIED receipt exists.

## Findings

- Reusable allowances need merchant, token, program, amount, period, expiry, and evidence boundaries.
- Request IDs and nonces make retries idempotent while preventing replay under a new request.
- Signed webhooks let the merchant unlock content without trusting a screenshot or browser state.
