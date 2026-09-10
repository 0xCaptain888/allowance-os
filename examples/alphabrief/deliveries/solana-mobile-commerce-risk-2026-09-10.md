# AlphaBrief — Solana Mobile Recurring Commerce Risk Brief

## Executive Summary

Solana Mobile applications are increasingly able to sell subscriptions, research, automated agent work, signals, and metered APIs. The user-experience problem is no longer whether a wallet can sign one payment. It is whether a user can approve a bounded commercial relationship once and still retain understandable control over every later charge. A safe design must bind the merchant, token mint, destination program, per-charge limit, rolling-period limit, lifetime limit, expiry, and proof of delivery. It must also separate the party requesting payment from the party checking whether the service was actually delivered.

Allowance OS Delegated Settlement v2 implements this boundary on Solana Devnet. The user creates an allowance and grants an allowance-scoped Program PDA a hard SPL-token delegation. Later, an executor requests settlement and an independent verifier attests the delivered output. The Program checks the sequential nonce, evidence uniqueness, actor identities, destination, and all budget limits before transferring tokens. The authority wallet is not required to sign the later charge.

## Risk Findings

The highest-risk failure is a technically successful service call that produces commercially useless output. Treating transport success as delivery would let an agent or merchant collect payment for empty, stale, malformed, or substituted content. AlphaBrief therefore hashes the exact report bytes and submits a verification record containing freshness, minimum-content, required-section, source-count, source-URI, and merchant-binding checks. Only a passing verification record becomes the evidence hash for settlement.

A second risk is silent budget drift. A user may tolerate a two-dollar report while still rejecting a ten-dollar report or five reports in one week. Per-charge, period, and lifetime limits must be enforced using integer raw token units. A third risk is compromised operations: an executor should not also be the verifier, and neither should be the merchant or user authority. Finally, revocation must remove the underlying SPL Token delegate rather than only changing an application label.

## Recommendations

For an early-access launch, keep value caps low, use a canonical asset only after mint and decimals are explicitly reviewed, and publish a human-readable upcoming-charge schedule. Notify users after every settlement, freeze, role change, and budget threshold crossing. Provide one-tap pause before offering irreversible revoke. Generate a weekly safety report containing total spend, prevented spend, merchant anomalies, evidence failures, allowances approaching their caps, and links to public chain evidence.

Higher-value services should move beyond a single verifier toward quorum or delayed settlement. Upgrade authority should move from a development key to multisig or timelocked governance. The Android client should preserve the distinction between disconnecting a wallet session, pausing an allowance, and revoking the underlying token delegation. Production release signing, device testing, independent Program review, and operational monitoring remain mandatory before Mainnet use.

## Sources

- Solana Mobile Wallet Adapter documentation: wallet authorization and transaction-signing model.
- Solana Program Derived Address documentation: deterministic Program-controlled authority without a private key.
- SPL Token documentation: delegated token authority and revoke behavior.
- Allowance OS public v2 Devnet evidence: authority-free later settlement, immutable evidence records, freeze, recovery, role rotation, and delegate removal.
