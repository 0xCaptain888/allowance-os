# Security policy

Allowance OS is a pre-production Solana Devnet project. Do not use it to protect Mainnet funds.

## Reporting a vulnerability

Please use GitHub's private vulnerability reporting for `0xCaptain888/allowance-os` when available. If private reporting is unavailable, open a minimal issue that contains no exploit details and ask the maintainer for a private channel.

Include the affected commit/version, component, impact, reproduction conditions, and whether funds or credentials may be at risk. Do not publish wallet secrets, signing keys, webhook secrets, private RPC URLs, or working exploits.

## Response targets

- acknowledge a complete report within 3 business days;
- classify severity and affected versions within 7 business days;
- publish a fix or mitigation plan before detailed public disclosure when practical.

These are project targets, not a commercial SLA. There is no bug bounty unless a separate written program says otherwise.

## Supported versions

Only the latest commit on `main` and the newest production-signed release are eligible for fixes. Debug APKs and historical Devnet deployments are evidence artifacts, not supported production releases.

## Explicit boundaries

- the deployed v1 Devnet Program is not Delegated Settlement v2;
- browser receipts and local Android policy replays are simulated;
- the project-created Devnet mint is not canonical USDC;
- no maintainer will ask for a seed phrase or private key.
