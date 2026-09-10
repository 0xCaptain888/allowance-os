# Allowance OS Android Client

`mobile/android` is a native Kotlin/Jetpack Compose dApp that connects to Phantom or another compatible wallet through Solana Mobile Wallet Adapter.

## Build status

Verified locally on September 10, 2026:

```text
./gradlew testDebugUnitTest assembleDebug
BUILD SUCCESSFUL
12 Android policy tests passed
Artifact: app/build/outputs/apk/debug/allowance-os-0.11.0-debug.apk
APK SHA-256: 14d5ed6d41b47a5a7a921cb2e5f193c5fba4effc4911191e830be96926710fd2
```

v0.11.0 encrypts the MWA reconnect token with Android Keystore, adds explicit review before broadcasting a non-payment Memo proof, persists the selected language, and clearly separates wallet-session disconnect from onchain allowance revocation. It retains the AlphaBrief flow, five commercial templates, Seeker Integration Lab, deployed Program evidence boundary, and direct Devnet verification.

The checksum belongs to the local debug build and may change after any source or dependency update.

## Build

Requirements:

- Android Studio Quail 4 or later;
- Android SDK platform `android-37.0`;
- JDK 21 for the Gradle 8.13 build;
- Android phone with Phantom or another MWA-compatible wallet.

```bash
cd mobile/android
JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" \
  ./gradlew testDebugUnitTest assembleDebug
```

## Install on a standard Android phone

1. In Android settings, enable Developer options.
2. Enable USB debugging.
3. Connect the phone to the Mac with a data-capable USB cable.
4. Approve the phone's **Allow USB debugging?** prompt.
5. Confirm the device appears:

```bash
~/Library/Android/sdk/platform-tools/adb devices -l
```

6. Install the debug APK:

```bash
~/Library/Android/sdk/platform-tools/adb install -r \
  app/build/outputs/apk/debug/allowance-os-0.11.0-debug.apk
```

7. Open **Allowance OS** on the phone.

## Live MWA test

1. Put Phantom on Solana Devnet and use a dedicated test wallet.
2. Tap **Connect Phantom / MWA Wallet**.
3. Approve authorization in Phantom.
4. Return to Allowance OS and confirm that the public key appears.
5. Tap `BLOCKED`; confirm no wallet request opens.
6. Tap `FROZEN`; confirm no wallet request opens.
7. Tap `VERIFIED`.
8. Tap **Review and publish Devnet Memo**, review the `0 USDC` asset movement disclosure, and continue.
9. Inspect the Memo transaction in Phantom and approve it.
10. Open the returned signature in Solana Explorer.
11. Tap **Disconnect wallet and deauthorize MWA** to remove the reconnect session. This does not revoke an onchain allowance.

## Production-signed build

The production signing key must live outside the repository. Provide the four signing values only through the local process environment, then run:

```bash
export ALLOWANCE_OS_STORE_FILE='/absolute/path/to/allowance-os-dappstore.jks'
export ALLOWANCE_OS_STORE_PASSWORD='enter-locally'
export ALLOWANCE_OS_KEY_ALIAS='allowance-os-dappstore'
export ALLOWANCE_OS_KEY_PASSWORD='enter-locally'
npm run android:release
```

Do not paste signing passwords into chat, shell history, source files, GitHub issues, or CI logs. The build fails closed when any signing value is absent; it never silently emits an unsigned or debug-signed production APK.

Recorded live proof: [`evidence/live-devnet-memo.json`](../evidence/live-devnet-memo.json), transaction signature `4w1cjWABu9L9NGMe4NrRTkqFxZVnJBsdket94ifiuKrGaMsMDnYquFpirq4kte4hsCxRuT6Jo79U8zvKNgzQ3B9k`.

The live Memo proves that the policy-approved request reached a real wallet and was broadcast. It is deliberately labelled as authorization evidence, not a deployed allowance settlement.

## Device capability boundary

Standard Android is a valid MWA test mode:

```text
Wallet authorization: AVAILABLE
Seed Vault: SEEKER_ONLY
Genesis Token: NOT_AVAILABLE
Live Devnet proof: AVAILABLE
```

A standard Android phone must never show `SEEKER VERIFIED`. Real Seed Vault and Genesis Token validation require compatible Solana Mobile hardware.
