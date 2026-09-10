# Allowance OS privacy policy

Last updated: September 10, 2026

Allowance OS is a local-first Android reference application. It does not operate an account system, advertising SDK, analytics SDK, or hosted user database.

## Data processed

The app may process and store locally:

- the public address and label returned by a wallet authorization;
- the MWA authorization token required to reconnect to that wallet;
- the latest public Devnet transaction signature;
- locally generated policy decisions and audit events;
- selected service template and language preference.

The app queries public Solana Devnet RPC endpoints for balances, transaction status, transaction data, and Program account state. Public wallet addresses and transaction signatures submitted to a blockchain are public by design.

## Data not collected

Allowance OS does not request, read, transmit, or store seed phrases or private keys. Wallet signing remains inside the compatible wallet application or supported secure hardware.

## User controls

Users can deauthorize the wallet, forget the local wallet session, and clear the local activity log from inside the app. Uninstalling the app removes its application-local data. Onchain records cannot be deleted because they are public blockchain data.

## Third parties

Wallet applications and Solana RPC providers process data under their own policies. The repository's Seeker app cards are labeled unofficial blueprints and do not represent integrations or data sharing partnerships.

## Contact

Open a privacy or deletion-support issue in the public repository: https://github.com/0xCaptain888/allowance-os/issues
