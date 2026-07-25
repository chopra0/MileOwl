package com.mileowl.tracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.mileowl.tracker.data.db.MileOwlDatabase
import com.mileowl.tracker.data.repository.TripRepository
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
