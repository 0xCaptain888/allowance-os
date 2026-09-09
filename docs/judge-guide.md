# Judge Guide

## 90-second path

1. Open the browser demo.
2. Read the active allowance: ResearchPulse, 2 USDC weekly, 8 USDC period cap.
3. Run **Normal charge** → `VERIFIED` and a simulated receipt.
4. Run **Over the cap** → `BLOCKED` before broadcast.
5. Run **Merchant changed** → `FROZEN` because the destination identity changed.
6. Click **Revoke allowance** → `REVOKED`.

## Truth boundary

This repository currently contains a deterministic simulator. MWA, Seed Vault, Solana Devnet, and live explorer links are intentionally not claimed until the adapter has produced a real signature and transaction receipt.

## What makes this mobile-native

The target product is a Seeker Android app: the browser page is only the judge-facing replay console. In the next milestone the same allowance card is authorized with MWA / Seed Vault and revoked from the device.
