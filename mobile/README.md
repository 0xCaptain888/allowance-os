# Seeker Android Client Boundary

The first mobile milestone is intentionally documented separately from the browser judge console.

## Native flow

```text
Open allowance card
  → MWA authorize
  → Seed Vault confirmation
  → policy hash committed
  → native push before charge
  → biometric revoke / pause
```

The Android implementation must never receive a private key or an Agent API secret. It receives wallet authorization through MWA and keeps only non-sensitive allowance metadata locally.

## Standard Android fallback

You do not need a Seeker to test the MWA path. Install a compatible mobile Solana wallet, switch it to Devnet, and connect through MWA. The app should show:

```text
Wallet authorization: AVAILABLE
Seed Vault: SEEKER_ONLY
Genesis Token: NOT_AVAILABLE
```

That is a valid test mode. Do not display `SEEKER VERIFIED` on a non-Seeker phone.

## Device test requirement

The browser replay is available without a Seeker. Real Genesis Token / Seed Vault validation requires a compatible Android device or an approved emulator path; the repository will keep that distinction explicit.
