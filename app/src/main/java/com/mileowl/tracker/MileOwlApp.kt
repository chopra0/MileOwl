package com.mileowl.tracker

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.mileowl.tracker.data.db.MileOwlDatabase
import com.mileowl.tracker.data.repository.TripRepository
import com.mileowl.tracker.service.ActivityTransitionHelper
import com.mileowl.tracker.service.DriveMonitorService
import com.mileowl.tracker.util.Constants
import com.mileowl.tracker.util.PreferencesManager

class AppContainer(application: Application) {
    private val database = MileOwlDatabase.getInstance(application)
    val tripRepository = TripRepository(
        database.tripDao(),
        database.savedLocationDao(),
        database.vehicleDao(),
        database.frequentDriveDao()
    )
    val preferencesManager = PreferencesManager(application)
}

class MileOwlApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannels()
        registerTransitionsIfReady()
    }

    /**
     * Register for activity transitions every time the app process starts,
     * but only if auto-detection is enabled in preferences.
     * This ensures the driving detector stays alive even if Android killed
     * and restarted the process in the background.
     */
    private fun registerTransitionsIfReady() {
        // Check if auto-detection is enabled
        if (!container.preferencesManager.isAutoDetectionEnabledSync()) {
            Log.d("MileOwlApp", "Auto-detection disabled — skipping registration")
            return
        }

        val hasFineLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasActivityRecognition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (hasFineLocation && hasActivityRecognition) {
            // Register transitions directly first (works even if service fails)
            try {
                ActivityTransitionHelper.registerTransitions(this)
                Log.d("MileOwlApp", "Activity transitions registered")
            } catch (e: Exception) {
                Log.w("MileOwlApp", "Could not register activity transitions", e)
            }

            // Then start the always-on monitor service (adds speed-based backup detection)
            try {
                DriveMonitorService.start(this)
                Log.d("MileOwlApp", "Drive monitor service started")
            } catch (e: Exception) {
                Log.w("MileOwlApp", "Could not start monitor service", e)
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertsChannel = NotificationChannel(
                Constants.ALERTS_CHANNEL_ID,
                Constants.ALERTS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts about tracking issues like Battery Saver mode"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(alertsChannel)
        }
    }
}
