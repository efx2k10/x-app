# EFX

A minimal Android wrapper around [x.com](https://x.com) with a built-in menu,
user style/script injection, a mobile/desktop switch and self-updates from
GitHub Releases.

The app is not affiliated with X Corp. "X" and "Twitter" are trademarks of
their respective owners; the name and icon of this project do not use them.

## Features

- Full-screen WebView with system back navigation, downloads, file uploads,
  geolocation and fullscreen video.
- Menu gesture: **two-finger double tap** anywhere on the page.
  Fallback: **three-finger single tap**.
- Settings: mobile/desktop site version, custom CSS (applied to every page)
  and custom JavaScript (executed once per page load).
- Manual update check against GitHub Releases: the APK is downloaded,
  verified against `checksums.txt` (SHA-256, fail-closed) and checked to be
  signed with the same certificate as the installed app before installation.
- English and Russian UI, follows the system language.

## Requirements

- JDK 17
- Android SDK with platform 34 and build-tools
- Android 7.0 (API 24) or newer on the device

`local.properties` with `sdk.dir=...` is needed for local builds (it is
git-ignored). Everything else is configured in the project.

## Build

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

The version is derived from `-PversionName` (default `1.0.0`):

```bash
./gradlew :app:assembleRelease -PversionName=1.2.3
```

The version code is always computed from the name:
`major * 10000 + minor * 100 + patch` (major ≤ 2000, minor ≤ 99, patch ≤ 99).
An explicit `-PversionCode` is accepted only when it matches that formula.

### Signing

Release signing is configured from the environment and is skipped when the
values are missing (an unsigned APK is produced instead):

```bash
KEYSTORE_PATH=/absolute/path/to/keystore.jks \
KEYSTORE_PASSWORD=... \
KEY_ALIAS=... \
KEY_PASSWORD=... \
./gradlew :app:assembleRelease -PversionName=1.2.3
```

Never commit a keystore. `*.jks` and `*.keystore` are ignored by git.

### Update source

The update checker reads releases from `efx2k10/x-app`. The values live in
`gradle.properties`:

```properties
github.owner=efx2k10
github.repo=x-app
```

Forks should change them to their own repository. The release workflow injects
the values from the repository itself, so CI builds are always configured. If
the placeholders are left in place, the app shows "update source is not
configured" instead of failing silently.

## Release

Pushing a tag is enough:

```bash
git tag v1.2.3
git push origin v1.2.3
```

The workflow in `.github/workflows/release.yml`:

1. validates the tag and derives the version code;
2. runs unit tests;
3. builds and signs the release APK;
4. verifies the signature, `versionCode` and `versionName` with
   `apksigner`/`aapt2`;
5. publishes `app-<version>.apk` and `checksums.txt` to the GitHub release.

Required repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`.

## Project layout

- `app` – Compose UI, activity, WebView wiring, localization.
- `core:settings` – DataStore-backed settings (site mode, injection,
  onboarding flag).
- `core:webview` – WebView setup, navigation policy, gesture detector, CSS/JS
  injection, downloads. No Compose or app dependencies.
- `core:update-checker` – GitHub API, downloader, checksum and signature
  verification, installer. Exposes state only; the app localizes errors.

## Known limitations

- Google sign-in inside a WebView is blocked by Google. Use a password or an
  existing session.
- The site may occasionally challenge the WebView user agent; switching to
  desktop mode helps.
- Updates are sideloaded: the user must allow installing unknown apps for
  this app once.
