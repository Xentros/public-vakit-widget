# Changelog

All notable changes to this project will be documented in this file.

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
