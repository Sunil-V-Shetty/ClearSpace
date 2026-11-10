# ClearSpace completion

Keep the existing Compose bento UI and app/core/design architecture. Complete the on-device cleaner and diagnostics experience using actual Android data.

- [x] Finish Insights and Reclaimable with scan results, deduplicated size estimates, permission/empty/error states and links to review tools.
- [x] Add device readings and guided display/touch/speaker/vibration checks, with honest unknown/unsupported results.
- [x] Fix full-content duplicate matching, media aliases, archive-before-delete, recovery and failure reporting.
- [x] Make permission requests contextual, expose settings actions, remove misleading boost claims and placeholders.
- [x] Add meaningful unit and instrumented tests; run on the installed emulator, inspect screenshots, fix failures.
- [x] Build debug and unsigned release APKs, run lint, document verified coverage and any physical-hardware limits.

Reference patterns: Files by Google cleanup/review categories and Samsung Members diagnostics. No cloud account, paid features, antivirus claims, or OEM-only battery capacity estimates are implied. User has authorized implementation and end-to-end fixes.

## Verification — 2026-10-09

- `./gradlew :app:assembleDebug testDebugUnitTest lintDebug :app:assembleRelease --offline`: successful; 11 JVM tests, no failures. Debug and minified unsigned release APKs produced.
- `./gradlew :app:connectedDebugAndroidTest --offline`: 11 tests passed on Pixel_10a ARM64 emulator, Android 17 (API 37). Covers Tools navigation, guided display/touch checks, saved state, theme persistence, Quick Scan, recoverable cleanup, cancellation, failure paths and full-content duplicate detection.
- Visual inspection: Home, Tools, Diagnostics and Insights permission/empty-result layouts. Native audio/photo/video permission acceptance returned successfully to a completed Insights scan.
- Lint: zero errors; 53 app, 2 core and 6 design warnings remain (dependency updates, resource/plural/icon recommendations, package visibility and minor style guidance). No lint baseline or blanket suppression added.
- Both staged and unstaged diffs pass `git diff --check`.

## Distribution gates still required

This is an installable evaluation build, not a claim of universal device or store certification. Validate actual speaker/vibration/touch/battery behavior on physical phones, test OEM/older Android/SD-card and partial-permission combinations, and verify cross-app native media-write consent on those devices. Instrumented approval/cancellation tests exercise the coordinator; they do not automate every OEM consent dialog. App uninstall or clearing app data removes its private recovery copies.

Production signing is not configured. The release APK is unsigned; production distribution also needs the owner's signing key and any applicable store review of optional all-files access. No production key, publishing or remote changes were made.

## Subsequent UI and artwork redesign

The UI uses a shared Material 3 design system. The open-source icon license is bundled in the app and available in Settings. Full regression before the palette-only change (2026-11-10): 13 JVM tests and 17 device tests passed, with zero lint errors and successful debug/unsigned-release builds. See the sky-blue verification below for checks on the current palette.

Home now prioritizes cleanup review and junk/duplicate/large-file shortcuts, followed by Trash and file browsing. Storage is a compact context card, while Quick Scan and diagnostics are secondary tools. Clean and Tools have distinct navigation icons. The review screen presents measured, deduplicated candidate sizes, contextual permission requests and an explicit no-suggestions state. Workflow references: [Files by Google](https://support.google.com/files/answer/9713869) and [SD Maid](https://github.com/d4rken-org/sdmaid-se); no third-party branding or assets were copied.

Final command: `./gradlew :app:connectedDebugAndroidTest :app:testDebugUnitTest --rerun :core:testDebugUnitTest --rerun :design:testDebugUnitTest --rerun lintDebug :app:assembleDebug :app:assembleRelease --offline` — BUILD SUCCESSFUL in 2m 37s. All 17 device tests passed, including visible cleanup review, shortcut navigation and the real-file deletion/restoration journey. All 13 JVM tests passed, including updated light/dark cleanup-card contrast. Lint reports zero errors and 50 warnings (48 app, 2 core; none in design).

Manually inspected the new Home and review/permission/result screens, dark theme and 160% text size. Restored the original text scale and light theme and left the app open on the emulator. Screenshots are in `app/build/reports/visual-checks/clean-*.png`. The debug APK is signed and installable; release remains unsigned. Physical hardware and OEM-specific behavior still require phone testing.

## GitHub layout cross-check — 2026-11-10

Inspected the actual screenshots and source README/license information for [Fossify File Manager](https://github.com/FossifyOrg/File-Manager) and [Material Files](https://github.com/zhanghai/MaterialFiles). Adapted their storage-row and compact-list patterns into independent ClearSpace Compose components. No third-party app code, branding or reference screenshots are included in the APK.

Shortened the cleanup card, reduced tool-row padding and badges, and added reusable category rows with right-aligned measured file sizes and decorative capacity bars. Before a scan, rows offer "View files" rather than implying an empty measurement. Home and Media overview share the component. The large-text visual check caught split words in the three-column shortcut grid; at large font scales or narrow widths it now switches to full-width action rows.

Final full verification: BUILD SUCCESSFUL in 2m 12s; 17 device tests and 13 freshly rerun JVM tests passed, zero failures/skips. Debug and minified unsigned release builds succeeded; lint reports zero errors and the same 50 existing warnings. Manually inspected normal, dark and 160% text layouts and restored normal text size. Screenshots: `app/build/reports/visual-checks/github-refined-*.png`. The deleted optional design Markdown file was not recreated.

## Preferred sky-blue theme — 2026-11-10

User preference: sky blue. Replaced the teal/mint brand roles with blue actions, pale sky-blue containers, cool neutral surfaces and matching dark-mode roles. Updated in-app mark, adaptive/legacy launcher artwork and light/dark splash colors. The launcher uses a sky-blue background and dark-blue symbol; the monochrome themed-icon resource remains OS-tinted.

Both existing light/dark contrast tests passed. Targeted device tests for theme persistence/activity recreation and cleanup-shortcut navigation passed (2 tests, zero failures/skips). Debug and minified unsigned release APKs built successfully; lint has zero errors and 50 existing warnings. This color-only change was checked with those focused UI tests rather than rerunning the unchanged file-operation suite.

Manually inspected both blue themes and the updated launcher icon; left the app open in light mode. Previews: `app/build/reports/visual-checks/sky-blue-home.png`, `sky-blue-dark.png`, and `sky-blue-launcher.png`.
