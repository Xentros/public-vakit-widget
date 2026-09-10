# Vakit Widget

A minimal Android app + home-screen widget showing Islamic prayer times
(Imsak, Sunrise, Dhuhr, Asr, Maghrib, Isha) for the Diyanet (Presidency of
Religious Affairs, Türkiye) data source.

## Features (v1)

- Glance home-screen widget (4 responsive sizes: 200×52 / 300×68 / 420×84 / 560×104 dp, `resizeMode=horizontal|vertical`, highlights next prayer, `updatePeriodMillis=0`, no tap action)
- Up to 5 saved cities — reorderable, renameable, set active; first or explicitly active is displayed
- Prayer alarms (master switch + per-prayer toggles; `AlarmManager.setExactAndAllowWhileIdle` with fallback `setWindow(15min)` if `canScheduleExactAlarms()==false`; scheduled today+tomorrow, recreated on boot/timezone change)
- Alarm sounds: 4 built-in Azans (Azan V1 default `azan_v1.mp3` + V2/V3/V4 `azan_v2..v4`) + custom audio file picker (`OpenDocument audio/*`, persistable URI) with play/stop preview, looping `MediaPlayer` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- Widget appearance: foreground (text/icon) color, background color, background opacity 0–100% (transparent floor)
- Qibla compass tab (magnetic sensors `ACCELEROMETER+MAGNETIC_FIELD`, low-pass 0.15, bearing to Kaaba 21.4225/39.8262, `ACCESS_COARSE_LOCATION` via `getLastKnownLocation`)
- System / EN / DE / TR localization (`LocaleHelper`, per-app language, `MainActivity.recreate()` on change)
- Full-screen background image (`android_background.png`, `ContentScale.Crop`) + edge-to-edge (`enableEdgeToEdge()`, transparent `Scaffold`, outer handles insets, inner `contentWindowInsets=0`)
- Offline JSON cache in DataStore (`PrayerCache` per `locationId`, no Room); widget never does network; stale-data banner
- Data tools: Refresh now / Update widget without re-adding

## Tech stack

- Kotlin 2.2.20, Jetpack Compose Material 3 (BOM 2025.09.00), Jetpack Glance 1.1.1
- Coroutines / Flow, manual DI (`AppContainer`)
- DataStore preferences (settings + JSON cache, no Room)
- Retrofit 2.11.0 + kotlinx.serialization 1.9.0 + OkHttp 4.12.0
- `AlarmManager` (exact alarms + inexact `setWindow` for widget highlight/daily `00:05` maintenance via `MaintenanceReceiver`/`MaintenanceRunner`), DataStore `Flow` → Glance recomposition
- `minSdk 26`, `targetSdk 36`, `compileSdk 36`, `versionCode 3`, `versionName 1.0.2`

## Data source

API base: `https://prayertimes.api.abdus.dev/`
Endpoints used: `/api/diyanet/countries`, `/api/diyanet/countries/{country}/cities`,
`/api/diyanet/locations`, `/api/diyanet/search`, `/api/diyanet/prayertimes`.

The API returns local times as `HH:mm` without a timezone, so the app resolves
the IANA timezone from the Diyanet country name (`CountryTimeZones`).

## Build

```
gradlew.bat :app:assembleDebug            # debug APK
gradlew.bat :app:bundleRelease            # signed AAB (needs keystore.properties or ~/.gradle/gradle.properties)
gradlew.bat :app:testDebugUnitTest
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
Release AAB: `app/build/outputs/bundle/release/app-release.aab`

Privacy policy: `https://xentros.github.io/public-vakit-widget/privacy.html` (`docs/privacy.html`, in-app Settings → About)

## Permissions

- `INTERNET` – fetch prayer times
- `ACCESS_COARSE_LOCATION` – approximate location, optional only for Qibla bearing (on-device, not sent)
- `POST_NOTIFICATIONS` – alarm notifications (Android 13+)
- `SCHEDULE_EXACT_ALARM` – punctual alarms (user opt-in, fallback to inexact 15-min window)
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK` – play alarm sounds
- `RECEIVE_BOOT_COMPLETED` – recreate alarms/widget schedules after reboot (`MY_PACKAGE_REPLACED`/`TIME_SET`/`TIMEZONE_CHANGED`)
