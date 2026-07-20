# 🦉 MileOwl — IRS-Compliant Mileage Tracker

A free, open-source Android app for tracking business mileage with automatic trip detection. Built for small business owners who need IRS-compliant mileage logs without paying monthly subscription fees.

## Features

### Automatic Trip Detection
- Uses Android Activity Recognition API to detect when you start/stop driving
- Background GPS tracking via foreground service with live distance notification
- Zero manual effort — just drive and MileOwl handles the rest
- Manual trip start/stop button for edge cases

### Trip Classification
- Swipe right → **Business** / Swipe left → **Personal** (on trip list)
- Add business purpose and client/destination for each trip
- Default classification option in settings

### IRS-Compliant Reports
- Export CSV reports with all IRS-required fields:
  - Date, start/end addresses, business purpose, miles, duration, classification
- Summary section with total business miles and deduction amount
- Date range filtering
- Share via email, Google Drive, or any Android share target

### Dashboard
- Current month stats: business miles, personal miles, total trips
- Year-to-date business miles and IRS deduction calculation
- Live tracking indicator
- Recent trips at a glance

### Saved Locations
- Save frequently visited locations (home, office, client sites)
- Trips auto-tagged when starting/ending near a saved location
- Customizable detection radius per location

### Settings
- Configurable IRS mileage rate (default: $0.70/mile for 2026)
- Vehicle information for report headers
- Auto-detection toggle
- GPS accuracy preference
- Default trip classification

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM with ViewModels
- **Database:** Room (local SQLite)
- **Location:** Google Play Services FusedLocationProviderClient
- **Activity Detection:** Activity Recognition Transition API
- **Preferences:** DataStore Preferences
- **Navigation:** Jetpack Navigation Compose
- **Min SDK:** 26 (Android 8.0)

## Build Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 35

### Steps

1. Clone the repository:
```bash
git clone https://github.com/YOUR_USERNAME/MileOwl.git
cd MileOwl
```

2. Open in Android Studio or build from command line:
```bash
# Generate Gradle wrapper (if not present)
gradle wrapper --gradle-version 8.7

# Build debug APK
./gradlew assembleDebug
```

3. Install on your device:
```bash
./gradlew installDebug
```

### First Launch
1. Grant all requested permissions (location, activity recognition, notifications)
2. For background location: Go to Settings → Apps → MileOwl → Permissions → Location → "Allow all the time"
3. Go to Settings tab and enter your vehicle info
4. Start driving — MileOwl will auto-detect trips

## IRS Compliance Notes

The IRS requires the following for each business trip to claim the standard mileage deduction:

| Field | MileOwl Coverage |
|-------|-----------------|
| **Date of trip** | ✅ Auto-recorded |
| **Destination** | ✅ Auto-geocoded from GPS |
| **Business purpose** | ⚠️ User must enter after trip |
| **Miles driven** | ✅ Auto-calculated from GPS |
| **Total miles for year** | ✅ YTD dashboard + report |

**Important:** You must classify each trip and add a business purpose for full IRS compliance. The app reminds you of unclassified trips on the Reports screen.

### §179 Vehicle Deduction
If you're using this vehicle primarily for business (like a delivery vehicle for your LLC), keep your business use percentage above 50% to qualify for §179 deductions. MileOwl's reports show your business vs personal mileage ratio.

## Permissions

| Permission | Why |
|-----------|-----|
| `ACCESS_FINE_LOCATION` | GPS tracking during trips |
| `ACCESS_BACKGROUND_LOCATION` | Track trips when app is in background |
| `ACTIVITY_RECOGNITION` | Auto-detect when driving starts/stops |
| `FOREGROUND_SERVICE` | Keep GPS running during active trip |
| `POST_NOTIFICATIONS` | Show tracking notification |
| `RECEIVE_BOOT_COMPLETED` | Re-register activity detection after reboot |

## Privacy

- **All data stays on your device** — no cloud sync, no analytics, no tracking
- Location data is stored locally in an SQLite database
- CSV exports are generated locally and shared only when you explicitly choose to
- No internet connection required (except for reverse geocoding addresses)

## Project Structure

```
app/src/main/java/com/mileowl/tracker/
├── MileOwlApp.kt              # Application + DI container
├── MainActivity.kt            # Entry point + permission handling
├── data/
│   ├── model/                  # Trip, LocationPoint, SavedLocation, TripClassification
│   ├── db/                     # Room database, DAOs, TypeConverters
│   └── repository/             # TripRepository
├── service/
│   ├── TripTrackingService.kt  # Foreground service for GPS tracking
│   ├── ActivityTransition*.kt  # Auto-detect driving
│   └── BootReceiver.kt        # Re-register after reboot
├── ui/
│   ├── theme/                  # Material 3 theme (teal/owl)
│   ├── navigation/             # Bottom nav + routes
│   ├── home/                   # Dashboard
│   ├── trips/                  # Trip list with swipe gestures
│   ├── tripdetail/             # Trip editing
│   ├── locations/              # Saved locations management
│   ├── report/                 # CSV export + stats
│   └── settings/               # App configuration
└── util/
    ├── Constants.kt
    ├── DistanceCalculator.kt
    ├── GeocoderHelper.kt
    ├── PreferencesManager.kt
    └── CsvExporter.kt
```

## License

MIT License — see [LICENSE](LICENSE)

## Contributing

Pull requests welcome. For major changes, open an issue first.

---

*Built by a 🦉 for PSA Imports LLC*
