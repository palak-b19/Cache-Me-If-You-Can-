# Cache-Me-If-You-Can – Copilot Instructions
Focused guidance for AI agents working in this repo.

## Big Picture
- Single-module Android app (`app/`) built with Kotlin 1.9, Gradle 8.2, and SDK 34; `MainActivity` only forwards to `ui/AppListActivity.kt` where the real UX lives.
- Core flow: enumerate launchable packages with `PackageManager`, wrap them in `models/AppInfo`, list them via `AppListAdapter`, and drill into `AppDetailActivity` to show permissions plus a link to Android's settings page.

## UI + Navigation
- Activities sit under `com.example.ussdemoproject.ui`; view binding is standard (see `ActivityAppListBinding`/`ActivityAppDetailBinding`) and should be preferred over manual `findViewById`.
- `AppListAdapter` builds explicit intents with extras `appName` and `packageName`; do not rename these keys because `AppDetailActivity` reads them directly.
- Recycler card layout is locked in `res/layout/item_app.xml` (ConstraintLayout shell + icon + text column); follow this pattern when adding list rows.

## Data + Permissions
- `AppInfo` is the shared contract between listing and detail screens; extend or reuse it instead of creating parallel DTOs.
- Distinguish `PackageManager.GET_META_DATA` (used while listing) from `GET_PERMISSIONS` (used while inspecting); keep both wrapped in `try/catch` and default to empty collections to avoid crashes on missing packages.
- The manifest already claims `android.permission.QUERY_ALL_PACKAGES`; any new package-level inspection must clearly justify or constrain that scope.

## Layout / Binding Conventions
- `buildFeatures { viewBinding = true; dataBinding = true }`, but data binding layouts are only used where a `<layout>` root exists (`activity_app_list.xml`); stick with generated `*Binding` classes elsewhere.
- Layout filenames stay in snake_case with a screen or adapter prefix (`activity_app_detail`, `item_permission`); colors/themes live under `res/values` (`colors.xml`, `themes.xml`).

## Build + Run
- From repo root run `./gradlew assembleDebug` (mac/linux) or `./gradlew.bat assembleDebug` (Windows) to build; Android Studio users can import the Gradle project directly.
- `minSdk=26`, `targetSdk=34`, and `compileOptions`/`kotlinOptions` both expect Java/Kotlin 17—keep new code compatible.

## Testing + Debugging
- `./gradlew test` executes unit tests; `./gradlew connectedAndroidTest` needs an emulator/device and will exercise the default template tests in `app/src/androidTest`.
- When debugging package queries, verify permissions on a device with enough installed apps; emulators may hide packages unless you side-load more APKs.

## When Adding Features
- Prefer injecting new UI behind additional activities/fragments under `ui/` and wire them from `MainActivity` or the adapter as needed.
- Keep expensive `PackageManager` work off the main thread if you expand functionality; currently it runs inline in `AppListActivity`, so batch updates should use coroutines + `lifecycleScope`.
- Maintain explicit navigation extras and document any new keys alongside their consumers.
