# Changelog

All notable changes to this project will be documented in this file.

## [1.0.5] - 2026-09-14

### Fixed
- City search now substring + fuzzy (Levenshtein): `Pa`→Paris, `Fra`/`Fank`→Frankfurt. `api/search` exact fallback to client-side cached browse data with background prefetch.

### Changed
- Auto-name when adding city (label empty) now picks the part of `region · country · city` closest to the search query (e.g. `HESSEN \ ALMANYA \ FRANKFURT` with query `Frank` → `FRANKFURT` not `HESSEN`), prefilled in dialog but editable.

## [1.0.4] - 2026-09-10

### Changed
- App icon replaced with `Store_logo.png` (green mosque/compass, adaptive `store_logo_fg` + monochrome, legacy `mipmap-*` 48-192 px).
- Alarm and preview sounds now play **once** (`SoundManager isLooping false`, `onCompletion` auto-stop); preview auto-resets `❚❚→▶`, alarm service `stopSelf()` on completion (fallback 3 min).

## [1.0.3] - 2026-09-10

### Fixed
- Removed spurious `FOREGROUND_SERVICE_SPECIAL_USE` permission (kept `FOREGROUND_SERVICE` + `MEDIA_PLAYBACK` only for `PrayerAlarmService`).

### Changed
- Bumped `versionCode 4`, `versionName 1.0.3`, `targetSdk/compileSdk 36` with edge-to-edge `WindowInsets(0)` de-duplication and `enableEdgeToEdge()`.

## [1.0.2] - 2026-09-10

### Added
- Privacy policy at `https://xentros.github.io/public-vakit-widget/privacy.html` (`docs/privacy.html`, in-app Settings → About) and proprietary `LICENSE.md`.
- Backup rules (`backup_rules.xml` / `data_extraction_rules.xml`, `enableOnBackInvokedCallback`, `usesCleartextTraffic=false`, monochrome icon).

### Changed
- Azan sounds `azan_v1..v4` (metadata stripped) replace placeholders `Chime/Call/Dawn` → `Azan V1-V4`; `MAX_SAVED_CITIES 3→5`; `README` updated; signing via env/`~/.gradle/gradle.properties`.
- `VakitApplication` `runBlocking` removed (async locale load, no ANR on widget/broadcast).

### Fixed
- `targetSdk 35→36` (Play requires 36 since 2026-08-31), widget preview encoding, alarm code collision `10000+`, `notificationId==0→1`.

## [Unreleased]

### Added
- **Qibla tab** between Prayer Times and Settings: a simple compass screen that
  shows the direction to the Kaaba. Uses the device location (with runtime
  permission) and the magnetic/rotation sensors; the rotating dial keeps north
  fixed and a red marker points toward the Qibla.
- App background image (`android_background.png`) shown behind all screens.

### Changed
- Home screen prayer list is more compact (reduced row height and spacing) so
  all six prayers fit on one screen without scrolling.
- Removed the refresh button from the Prayer Times screen; refreshing is now
  done once from Settings &rarr; Data &rarr; Refresh now (the main page updates
  automatically).

### Removed
- The widget tap interaction and its temporary countdown ("go time") view.
  Tapping the widget now does nothing, as requested.

## [1.0.0] - 2026-08-18

### Fixed
- **"Update widget" only worked once per app open.** Glance runs `provideGlance` only once per widget session; later `update`/`updateAll` calls only recompose the still-running composition. The widget content previously read its data once at the top of `provideGlance`, so every later recomposition rendered identical RemoteViews which Glance skipped. The composition now observes its data sources (settings, prayer times, widget state) as reactive flows via `collectAsState`, so every update re-renders with fresh data.
- Widget updates are now dispatched per placed widget (`updateAllReal`), with a fallback to platform widget ids, so one failing instance can no longer abort the remaining widgets.
- Tap-to-countdown now also applies reliably while the widget session is still alive (`WidgetStateStore.stateFlow`).

### Changed
- Maximum saved cities raised from 3 to 5.
- Widget spacing: name and time are now vertically centered around the icon with fixed gaps for every widget size.
- Transparent background (`0%` opacity) now updates reliably (alpha floor clamp, `TRANSPARENT_ALPHA_FLOOR`).
- Tap-to-countdown auto-return delay increased from 5s to 10s; the auto-return worker only updates the toggled widget instead of all widgets.
- "Update widget" button added in Settings &rarr; Data.
- Settings tab split into sections (Location / Appearance / Data / About) with a manual refresh button.

### Added
- Widget appearance settings: text/icon color, background color, background opacity.
- Language selection (System / English / Deutsch / Türkçe).
- Prayer alarms with selectable sound (Default / Chime / Call / Dawn / Custom).
- City management with saved locations and active city switching.

### Security
- Release signing keystore is kept out of version control (see `.gitignore`); signing credentials live in the untracked `keystore.properties`.

### Infrastructure
- GitHub repository created at `Xentros/vakit-widget` (private).
- Signed release AAB build configured and produced for a future Play Store release.
