package com.mileowl.tracker.ui.frequentdrives

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mileowl.tracker.MileOwlApp
import com.mileowl.tracker.data.model.FrequentDrive
import com.mileowl.tracker.data.model.Trip
import com.mileowl.tracker.data.model.TripClassification
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FrequentDrivesViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MileOwlApp
    private val repo = app.container.tripRepository

    val frequentDrives: StateFlow<List<FrequentDrive>> = repo.getAllFrequentDrives()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Creates a new Trip from a FrequentDrive template with the current timestamp.
     * Returns the new trip's ID so the caller can navigate to TripDetail.
     */
    suspend fun logTrip(drive: FrequentDrive): Long {
        val now = System.currentTimeMillis()
        val classification = drive.defaultPurpose?.toClassification() ?: TripClassification.UNCLASSIFIED
        val trip = Trip(
            startTime = now,
            endTime = now,
            startLatitude = drive.startLatitude,
            startLongitude = drive.startLongitude,
            endLatitude = drive.endLatitude,
            endLongitude = drive.endLongitude,
            startAddress = drive.startAddress,
            endAddress = drive.endAddress,
            distanceMiles = drive.estimatedDistanceMiles,
            durationMinutes = 0,
            classification = classification,
            tripPurpose = drive.defaultPurpose,
            vehicleId = drive.defaultVehicleId,
            isActive = false
        )
        return repo.insertTrip(trip)
    }

    fun deleteFrequentDrive(drive: FrequentDrive) {
        viewModelScope.launch {
            repo.deleteFrequentDrive(drive)
        }
    }
}
