# Bounds

A geo-fencing app blocker for Android. Users define "zones" (a location + radius on a map) and specify which apps to block when they enter that zone. Supports time-sensitive blocking windows and a background service that enforces the rules.

## Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Maps**: Google Maps Compose (`maps-compose`)
- **Min SDK**: 24 (Android 7.0), Target SDK: 36
- **Build**: Gradle (Kotlin DSL)

## Project structure

```
app/src/main/java/com/example/bounds/
├── MainActivity.kt              # Entry point, top-level nav (Zones / Current tabs)
├── model/Models.kt              # Zone and App data classes
├── service/AppBlockingService.kt # Foreground service that enforces blocking
├── util/AppBlockingManager.kt   # Logic for managing app blocking state
├── ui/
│   ├── screens/
│   │   ├── HomeScreen.kt        # Zone list ("Your Sacred Spaces")
│   │   ├── AddZoneScreen.kt     # Create / edit a zone
│   │   └── CurrentScreen.kt    # Active blocking status
│   ├── components/
│   │   ├── ZoneCard.kt          # Zone list item
│   │   ├── AppChip.kt           # App selection chip
│   │   └── MapAndSlider.kt      # Map picker + radius slider
│   └── theme/                   # Color, Type, Theme
```

## Features

- **Zones tab** — create, edit, enable/disable, and delete geo-fenced blocking zones
- **Current tab** — manually lock Instagram for a chosen duration; each lock records an analytics event
- **Analytics tab** — session count, total time blocked, top blocked app, 7-day bar chart, and recent sessions list
- **Settings** — accessible via the ⚙️ cog in the top-right of every main screen:
  - Theme picker: System / Light / Dark
  - Grace timer slider (0–60 s delay before blocking activates)
  - Delete analytics data (with confirmation dialog)

## Important notes

- **Google Maps API key** is required for the map UI. Add it to `app/src/main/res/values/secrets.xml` or `local.properties` (see [Maps SDK setup](https://developers.google.com/maps/documentation/android-sdk/get-api-key)).
- **App state is in-memory only** — zones and analytics events are lost when the app is killed. Persistence (Room/DataStore) has not been implemented yet.
- The `AppBlockingService` currently hardcodes "Instagram" in notification text — this needs to be wired up to the actual blocked app list.
- The "Reclaimed Time" stat on the Zones home screen is hardcoded to "12h".

## Building

```bash
./gradlew assembleDebug      # build debug APK
./gradlew test               # run unit tests
```

Note: Android apps cannot be run directly on Replit — install the APK on a physical device or emulator.

## User preferences

<!-- Add any preferences here -->
