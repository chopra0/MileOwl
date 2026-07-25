package com.mileowl.tracker.util

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mileowl.tracker.MainActivity
import com.mileowl.tracker.R

/**
 * Checks tracking-related conditions and fires/dismisses system notifications.
 * Called on every ON_RESUME from HomeScreen.
 */
object TrackingAlertHelper {

    data class TrackingIssues(
        val missingLocation: Boolean = false,
        val missingActivityRecognition: Boolean = false,
        val missingBackgroundLocation: Boolean = false,
        val gpsDisabled: Boolean = false,
        val batterySaverOn: Boolean = false,
        val batteryOptimizationRestricted: Boolean = false,
        val playServicesUnavailable: Boolean = false
    ) {
        val hasAnyIssue: Boolean
            get() = missingLocation || missingActivityRecognition || missingBackgroundLocation ||
                    gpsDisabled || batterySaverOn || batteryOptimizationRestricted || playServicesUnavailable

        val hasPermissionIssue: Boolean
            get() = missingLocation || missingActivityRecognition || missingBackgroundLocation
    }

    fun checkIssues(context: Context): TrackingIssues {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasActivityRecognition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val hasBackgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val batterySaverOn = pm.isPowerSaveMode
        val batteryOptimizationRestricted = !pm.isIgnoringBatteryOptimizations(context.packageName)

        val playServicesAvailable = com.mileowl.tracker.service.ActivityTransitionHelper
            .isPlayServicesAvailable(context)

        return TrackingIssues(
            missingLocation = !hasFineLocation,
            missingActivityRecognition = !hasActivityRecognition,
            missingBackgroundLocation = !hasBackgroundLocation,
            gpsDisabled = !gpsEnabled,
            batterySaverOn = batterySaverOn,
            batteryOptimizationRestricted = batteryOptimizationRestricted,
            playServicesUnavailable = !playServicesAvailable
        )
    }

    /**
     * Fire or dismiss notifications based on current issues.
     */
    fun syncNotifications(context: Context, issues: TrackingIssues) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Permission alert notification
        if (issues.hasPermissionIssue) {
            val reasons = mutableListOf<String>()
            if (issues.missingLocation) reasons.add("Location")
            if (issues.missingActivityRecognition) reasons.add("Activity Recognition")
            if (issues.missingBackgroundLocation) reasons.add("Background Location")

            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 1, appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, Constants.ALERTS_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Permissions needed")
                .setContentText("Missing: ${reasons.joinToString(", ")}. Tap to fix.")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("MileOwl is missing ${reasons.joinToString(", ")} permission${if (reasons.size > 1) "s" else ""}. Trip tracking may not work correctly. Tap to open the app and fix.")
                )
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            nm.notify(Constants.PERMISSION_ALERT_NOTIFICATION_ID, notification)
        } else {
            nm.cancel(Constants.PERMISSION_ALERT_NOTIFICATION_ID)
        }

        // GPS disabled notification
        if (issues.gpsDisabled) {
            val locationSettingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 2, locationSettingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, Constants.ALERTS_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Location services off")
                .setContentText("Turn on GPS for MileOwl to track trips. Tap to open settings.")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            nm.notify(Constants.GPS_DISABLED_NOTIFICATION_ID, notification)
        } else {
            nm.cancel(Constants.GPS_DISABLED_NOTIFICATION_ID)
        }

        // Battery saver notification (handled by PowerSaveReceiver too, but sync here for ON_RESUME)
        if (issues.batterySaverOn) {
            val settingsIntent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 3, settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, Constants.ALERTS_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Battery Saver is on")
                .setContentText("MileOwl may not detect trips automatically. Tap to disable.")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            nm.notify(Constants.POWER_SAVE_NOTIFICATION_ID, notification)
        } else {
            nm.cancel(Constants.POWER_SAVE_NOTIFICATION_ID)
        }
    }

    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
    }

    fun openLocationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
    }

    fun openBatterySaverSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
    }

    fun requestBatteryExemption(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
    }
}
