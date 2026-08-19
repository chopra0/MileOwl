# 🦉 MileOwl

**Free Android mileage tracker** with automatic trip detection, business/personal classification, and IRS-compliant CSV export.

No subscriptions. No ads. All data stays on your device.

## Features

### Automatic Trip Detection
- Uses Android Activity Recognition API to detect when you're driving
- Background GPS tracking via foreground service with live distance notification
- Re-registers activity transitions after device reboot
- Auto-discards ghost trips under 0.1 miles (false detection filter)

### Trip Management
- **Swipe to classify**: Swipe right for Business, left for Personal — on both the Home dashboard and Trips list
- **Multi-select delete**: Long-press to select, bulk-delete ghost or unwanted trips
- **Purpose categories**: Business, Customer Visit, Meeting, Delivery, Errand/Supplies, and more
- Auto-maps purpose to IRS classification
- Add notes and client/destination names for each trip
- Track parking costs and tolls per trip
- **CSV import**: Migrate trip history from MileIQ or QuickBooks

### Multi-Vehicle Support
- Manage multiple vehicles (e.g., business Telluride vs personal Mazda)
- **Vehicle selection popup** on classify when 2+ vehicles are saved
- "Always use this vehicle" option to skip the popup (resettable in Settings)
- Set a default vehicle for new trips
- Filter trips and reports by vehicle

### Frequent Drives
- Save common routes as templates
- One-tap trip logging from a template
- Stores start/end locations, estimated distance, and default purpose

### Dashboard
- Monthly business/personal miles and trip counts
- Year-to-date business miles and IRS deduction total
- Real-time tracking status indicator
- Unclassified trips queue with swipe-to-classify cards
- Quick-access recent trips

### IRS-Compliant Export
- CSV report with all IRS-required fields:
  - Date, start/end addresses, purpose category, business notes, client/destination
  - Miles, duration, classification, IRS rate, deduction amount
  - Parking costs, tolls, vehicle name
- Filter by date range
- Share via email, Google Drive, or any Android share target

### Saved Locations
- Tag frequently visited places (home, office, client sites)
- Auto-recognize when trips start/end near saved locations
- Configurable radius for each location

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM with ViewModels
- **Database**: Room (SQLite)
- **Preferences**: DataStore
- **Location**: FusedLocationProviderClient
- **Activity Detection**: Activity Recognition Transition API
- **DI**: Manual (AppContainer pattern — no Hilt/Dagger)

## Build & Install

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 35
- JDK 17

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/chopra0/MileOwl.git
   ```
2. Open in Android Studio
3. Sync Gradle
4. Run on device or emulator (API 26+)

### Optional: Google Maps Route View
To enable the route map on trip details:
1. Get a [Google Maps API key](https://developers.google.com/maps/documentation/android-sdk/get-api-key)
2. Add to `local.properties`: `MAPS_API_KEY=your_key_here`
3. Uncomment the maps-compose dependency in `app/build.gradle.kts`

## Android Permissions

| Permission | Purpose |
|---|---|
| `ACCESS_FINE_LOCATION` | GPS tracking during trips |
| `ACCESS_COARSE_LOCATION` | Approximate location fallback |
| `ACCESS_BACKGROUND_LOCATION` | Track trips when app is backgrounded |
| `ACTIVITY_RECOGNITION` | Auto-detect vehicle activity |
| `FOREGROUND_SERVICE` | Persistent tracking notification |
| `RECEIVE_BOOT_COMPLETED` | Re-register activity detection after reboot |
| `POST_NOTIFICATIONS` | Tracking status notifications (Android 13+) |

## IRS Compliance Notes

The IRS requires "contemporaneous" mileage records — meaning you should record trips at or near the time they occur. MileOwl handles this automatically via GPS.

**Required fields per IRS Publication 463:**
- Date of the trip
- Destination (city, town, or area)
- Business purpose
- Miles driven

MileOwl captures all four automatically and includes them in the CSV export.

**2026 IRS Standard Mileage Rate**: $0.70/mile (configurable in Settings)

## Project Structure

```
app/src/main/java/com/mileowl/tracker/
├── MileOwlApp.kt          # Application + DI container
├── MainActivity.kt         # Permission flow + entry point
├── data/
│   ├── model/              # Trip, Vehicle, FrequentDrive, TripPurpose, etc.
│   ├── db/                 # Room database, DAOs, migrations
│   └── repository/         # TripRepository
├── service/
│   ├── TripTrackingService.kt      # Foreground GPS service
│   ├── DriveMonitorService.kt      # Always-on drive detection listener
│   ├── ActivityTransitionHelper.kt  # Activity Recognition setup
│   ├── ActivityTransitionReceiver.kt # Broadcast receiver
│   ├── BootReceiver.kt             # Re-register on boot
│   └── PowerSaveReceiver.kt        # Battery optimization alerts
├── ui/
│   ├── theme/              # Material 3 colors, typography
│   ├── navigation/         # NavGraph + bottom navigation
│   ├── common/             # Shared dialogs (VehicleSelection, AddVehicle)
│   ├── home/               # Dashboard + swipe-to-classify queue
│   ├── trips/              # Trip list with swipe classify + multi-select delete
│   ├── tripdetail/         # Trip detail with purpose picker
│   ├── locations/          # Saved locations management
│   ├── frequentdrives/     # Frequent drive templates
│   ├── report/             # IRS report generation
│   ├── permission/         # Permission onboarding flow
│   └── settings/           # App settings + vehicle management
└── util/
    ├── Constants.kt
    ├── CsvExporter.kt      # IRS-compliant CSV generation
    ├── CsvImporter.kt      # MileIQ/QuickBooks import
    ├── DebugLog.kt         # On-device debug logging
    ├── DistanceCalculator.kt
    ├── GeocoderHelper.kt
    ├── PreferencesManager.kt
    └── TrackingAlertHelper.kt
```

## License

© 2026 PSA Imports LLC. All rights reserved.

Built with 🦉 for PSA Imports LLC
