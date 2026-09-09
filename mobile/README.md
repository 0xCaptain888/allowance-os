# Allowance OS Android Client

`mobile/android` is a native Kotlin/Jetpack Compose dApp that connects to Phantom or another compatible wallet through Solana Mobile Wallet Adapter.

## Build status

Verified locally on September 9, 2026:

```text
./gradlew testDebugUnitTest assembleDebug
BUILD SUCCESSFUL
4 Android policy tests passed
APK SHA-256: 0964730f20cd551483d946ab419fd8ee6952095a7afcc0f0decc7e1354f83e98
```

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
  app/build/outputs/apk/debug/app-debug.apk
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
8. Tap **Publish real Devnet authorization proof**.
9. Inspect the Memo transaction in Phantom and approve it.
10. Open the returned signature in Solana Explorer.
11. Tap **Revoke & deauthorize** to remove the MWA authorization.

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
