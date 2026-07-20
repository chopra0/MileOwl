package com.mileowl.tracker

import android.app.Application
import com.mileowl.tracker.data.db.MileOwlDatabase
import com.mileowl.tracker.data.repository.TripRepository
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
    }
}
