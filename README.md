# ClearSpace

Android storage cleanup app built with Kotlin, Jetpack Compose, Hilt, Room and DataStore.

## Architecture

The module boundaries follow the local `liciousdeliveryapp` reference: shared platform/data code in `core`, reusable UI in `design`, and application composition and feature screens in `app`.

| Module | Owns | Project dependencies |
| --- | --- | --- |
| `app` | Application/activity, navigation, permissions UI, feature screens and ViewModels | `core`, `design` |
| `core` | Storage scanners, models, repositories, Room database/DAOs, preferences, platform helpers and storage formatting | None |
| `design` | Shared Compose components, semantic theme, vector artwork and native status indicators | `core` |
| `buildSrc` | SDK/app configuration, plugin IDs and grouped dependency helpers | Build tooling only |

Feature state flows from repositories/platform readers through Hilt ViewModels to Compose screens. Navigation and permission prompts stay in `app`. The `ThemeViewModel` also stays in `app`; reusable theme rendering lives in `design`.

Library modules must not depend on `app`. Resources use their owning module's `R` class because non-transitive resources are enabled. Existing Kotlin package names are retained during the module extraction to avoid unrelated API churn.

The app ID, database filename/version/migration, DataStore name and navigation routes are preserved. This migration changes source ownership, not cleanup behavior. Delivery-specific networking, authentication, analytics and product workflows from the reference are not dependencies of ClearSpace.

## Local setup and verification

Open the repository root in Android Studio. Use JDK 21 for Gradle and install Android SDK 36. Set `sdk.dir` in your untracked `local.properties` if Android Studio has not already done so. The checked-in Gradle wrapper selects Gradle 9.2.1; Java source compatibility remains 11.

```sh
./gradlew :app:assembleDebug testDebugUnitTest
./gradlew lintDebug
./gradlew :app:assembleRelease
```

The first build needs access to Google Maven and Maven Central. Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. Android Studio builds can also use `app/build/intermediates/apk/debug`; the APK listing redirect identifies that output. Release signing is not configured; the release task produces `app/build/outputs/apk/release/app-release-unsigned.apk`.

For device verification, install the debug build and check Home, media permissions, Quick Scan, cleanup screens, Trash and the dark theme. File operations and Android permission dialogs need device/emulator verification in addition to JVM tests.

## Features

- Clean: compact storage summary, prominent cleanup review, direct junk/duplicate/large-file shortcuts, Trash and file browsing. Quick Scan and diagnostics are secondary tools. Quick Scan refreshes storage/memory data and clears this app's cache; it does not claim to boost other apps or reclaim their private memory.
- Cleanup: junk review, full SHA-256 duplicate matching, large files, photos/videos/downloads and unused-app usage review.
- Insights and Reclaimable: accessible-file analysis with deduplicated review estimates and links to the matching cleanup tools. Estimates count candidates, not guaranteed savings; recovery copies consume storage until removed from Trash.
- Device diagnostics: Android-reported battery condition/temperature/voltage, memory/storage, device/software, network and sensor availability. Guided display, nine-point touch, speaker and vibration checks record the user's observation. Unsupported readings stay unavailable; no battery-capacity or sensor-health inference is made.
- Settings: persisted dark theme, contextual media permission requests, optional all-files access and Android app settings.
- Trash: verified recovery copy before deletion, byte-count-checked restoration, 30-day retention, and explicit confirmation for permanent removal. Android write consent is requested before deleting protected media; cancellation creates no recovery copy.

Layout references: [Fossify File Manager](https://github.com/FossifyOrg/File-Manager) for storage/category rows and [Material Files](https://github.com/zhanghai/MaterialFiles) for compact utility lists. ClearSpace implements these patterns in its own Compose components; third-party app code, branding and screenshot assets are not bundled. Cleanup shortcuts switch to full-width rows on narrow screens or with large text.

No account, server, tracking SDK or internet permission is required. Recovery files and database are excluded from Android backup/device transfer. The UI uses a shared Material 3 sky-blue design system, grouped task rows and native progress/success feedback instead of GIFs. Vector launcher, splash and in-app artwork adapt Google's Apache-2.0 Material icon; the license is bundled and available in Settings. Feature references: [Files by Google](https://support.google.com/files/answer/9848742?hl=en) and [Samsung diagnostics](https://www.samsung.com/us/support/answer/ANS10003632/).

## End-to-end verification

With an emulator/device connected:

```sh
./gradlew :app:connectedDebugAndroidTest
```

Tests cover every Tools destination, Quick Scan without media access, display and touchscreen checks, activity recreation, theme persistence, failed/canceled cleanup, exact-byte restoration, empty files, permanent recovery removal and duplicate files with identical prefixes/different tails. Test files use unique names and are removed after each test.

Emulator readings do not validate a physical phone's speaker, vibrator, battery or touch hardware. Android/OEM permissions and storage access differ; test actual hardware before distributing a production build. Release signing requires the application's production key; the debug APK is installable for local evaluation.
