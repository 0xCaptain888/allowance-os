# Android release process

Every installable Allowance OS iteration must remain downloadable and independently checkable after temporary GitHub Actions artifacts expire.

## Required version changes

Before release:

1. update `versionCode` and `versionName` in `mobile/android/app/build.gradle`;
2. update the visible version in the Android header;
3. add a dated section to `CHANGELOG.md`;
4. update `evidence/android-build.json` after the final local build;
5. run TypeScript, Android, and Rust checks as appropriate;
6. commit and push `main`;
7. create and push an annotated `android-vX.Y.Z` tag.

## Automated GitHub archive

Pushing the version tag triggers `.github/workflows/android-release.yml`. The workflow:

- validates that the tag matches Android `versionName`;
- runs all Android unit tests;
- builds the debug APK;
- generates `SHA256SUMS.txt`;
- creates a GitHub Release;
- uploads the APK and checksum as permanent release assets.

The CI-generated `SHA256SUMS.txt` is the checksum authority for the public download. Local and CI debug APK hashes can differ because Gradle creates environment-specific debug signing keys. Production builds must use one protected release signing key to provide stable signer identity across updates.

Example:

```bash
git tag -a android-v0.11.0 -m "Allowance OS Android v0.11.0"
git push origin android-v0.11.0
```

## Truth boundary

The automated asset is a debug APK intended for testing, judging, and reproducibility. A production release must use a protected signing key, a release build type, a privacy/security review, and an app-store distribution process.

The repository now provides a fail-closed production path:

```bash
ALLOWANCE_OS_STORE_FILE=/absolute/path/to/allowance-os-dappstore.jks \
ALLOWANCE_OS_STORE_PASSWORD='set-locally' \
ALLOWANCE_OS_KEY_ALIAS=allowance-os-dappstore \
ALLOWANCE_OS_KEY_PASSWORD='set-locally' \
npm run android:release
```

`android:release` runs Android tests, creates the signed release APK, and writes `SHA256SUMS.txt`. It refuses to build when a signing value is missing. Never commit the keystore or any password.
