package com.mileowl.tracker.util

object Constants {
    const val IRS_RATE_2026 = 0.70
    const val LOCATION_UPDATE_INTERVAL_MS = 10_000L
    const val LOCATION_FASTEST_INTERVAL_MS = 5_000L
    const val DEFAULT_SAVED_LOCATION_RADIUS_METERS = 200f
    const val NOTIFICATION_CHANNEL_ID = "mileowl_tracking"
    const val NOTIFICATION_CHANNEL_NAME = "Trip Tracking"
    const val TRACKING_NOTIFICATION_ID = 1001
    const val ACTION_START_TRACKING = "com.mileowl.tracker.START_TRACKING"
    const val ACTION_STOP_TRACKING = "com.mileowl.tracker.STOP_TRACKING"
    const val EXTRA_IS_MANUAL = "extra_is_manual"
    const val METERS_PER_MILE = 1609.344
    const val ACTIVITY_TRANSITION_REQUEST_CODE = 100
    const val LOCATION_PERMISSION_REQUEST_CODE = 101
    const val ACTIVITY_RECOGNITION_REQUEST_CODE = 102
}
