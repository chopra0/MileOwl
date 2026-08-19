package com.mileowl.tracker.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mileowl.tracker.MileOwlApp
import com.mileowl.tracker.data.model.Trip
import com.mileowl.tracker.data.model.TripClassification
import com.mileowl.tracker.data.model.TripPurpose
import com.mileowl.tracker.data.model.Vehicle
import com.mileowl.tracker.service.TripTrackingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class PendingClassification(
    val trip: Trip,
    val classification: TripClassification
)

data class HomeUiState(
    val isTracking: Boolean = false,
    val currentTripMiles: Double = 0.0,
    val monthBusinessMiles: Double = 0.0,
    val monthPersonalMiles: Double = 0.0,
    val monthTotalTrips: Int = 0,
    val ytdBusinessMiles: Double = 0.0,
    val ytdDeduction: Double = 0.0,
    val irsRate: Double = 0.70,
    val recentTrips: List<Trip> = emptyList(),
    val unclassifiedTrips: List<Trip> = emptyList(),
    val vehicles: List<Vehicle> = emptyList(),
    val skipVehiclePrompt: Boolean = false,
    val pendingClassification: PendingClassification? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MileOwlApp
    private val repo = app.container.tripRepository
    private val prefs = app.container.preferencesManager

    private val _pendingClassification = MutableStateFlow<PendingClassification?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        combine(
            repo.getTripsInRange(startOfMonth(), endOfMonth()),
            repo.getTripsInRange(startOfYear(), endOfYear()),
            prefs.irsRateFlow,
            TripTrackingService.isTrackingFlow,
            TripTrackingService.currentDistanceMilesFlow
        ) { monthTrips, yearTrips, irsRate, tracking, currentMiles ->
            val monthBusiness = monthTrips.filter { it.classification == TripClassification.BUSINESS }
            val monthPersonal = monthTrips.filter { it.classification == TripClassification.PERSONAL }
            val ytdBusiness = yearTrips.filter { it.classification == TripClassification.BUSINESS }

            HomeUiState(
                isTracking = tracking,
                currentTripMiles = currentMiles,
                monthBusinessMiles = monthBusiness.sumOf { it.distanceMiles },
                monthPersonalMiles = monthPersonal.sumOf { it.distanceMiles },
                monthTotalTrips = monthTrips.size,
                ytdBusinessMiles = ytdBusiness.sumOf { it.distanceMiles },
                ytdDeduction = ytdBusiness.sumOf { it.distanceMiles } * irsRate,
                irsRate = irsRate,
                recentTrips = monthTrips.take(5),
                unclassifiedTrips = monthTrips.filter { it.classification == TripClassification.UNCLASSIFIED }
            )
        },
        combine(
            repo.getAllVehicles(),
            prefs.skipVehiclePromptFlow,
            _pendingClassification
        ) { vehicles, skip, pending ->
            Triple(vehicles, skip, pending)
        }
    ) { base, vehicleState ->
        base.copy(
            vehicles = vehicleState.first,
            skipVehiclePrompt = vehicleState.second,
            pendingClassification = vehicleState.third
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    /**
     * Called when the user swipes to classify a trip. If the user has 2+ vehicles
     * and hasn't opted out of the vehicle prompt, this sets a pending classification
     * to trigger the vehicle selection dialog. Otherwise, it completes immediately.
     */
    fun classifyTrip(trip: Trip, classification: TripClassification) {
        viewModelScope.launch {
            val vehicles = uiState.value.vehicles
            val skipPrompt = uiState.value.skipVehiclePrompt

            if (vehicles.size >= 2 && !skipPrompt) {
                // Show vehicle selection dialog
                _pendingClassification.value = PendingClassification(trip, classification)
            } else {
                // Complete immediately — auto-assign default vehicle if available
                val defaultVehicle = repo.getDefaultVehicle()
                completeClassification(trip, classification, defaultVehicle?.id)
            }
        }
    }

    /**
     * Called from the vehicle selection dialog to complete a pending classification.
     */
    fun completePendingClassification(vehicleId: Long, alwaysUseThis: Boolean) {
        viewModelScope.launch {
            val pending = _pendingClassification.value ?: return@launch
            _pendingClassification.value = null

            // If "Always use this vehicle" was checked, save the preference
            if (alwaysUseThis) {
                prefs.setSkipVehiclePrompt(true)
                repo.setDefaultVehicle(vehicleId)
            }

            completeClassification(pending.trip, pending.classification, vehicleId)
        }
    }

    /**
     * Called when the user dismisses the vehicle dialog without selecting.
     * The trip is still classified, just without a vehicle assignment.
     */
    fun dismissVehiclePrompt() {
        viewModelScope.launch {
            val pending = _pendingClassification.value ?: return@launch
            _pendingClassification.value = null
            completeClassification(pending.trip, pending.classification, null)
        }
    }

    private suspend fun completeClassification(
        trip: Trip,
        classification: TripClassification,
        vehicleId: Long?
    ) {
        val defaultPurpose = when (classification) {
            TripClassification.BUSINESS -> TripPurpose.BUSINESS
            TripClassification.PERSONAL -> TripPurpose.PERSONAL
            TripClassification.UNCLASSIFIED -> null
        }
        repo.updateTrip(
            trip.copy(
                classification = classification,
                tripPurpose = trip.tripPurpose ?: defaultPurpose,
                vehicleId = vehicleId ?: trip.vehicleId
            )
        )
    }

    private fun startOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun endOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    private fun startOfYear(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun endOfYear(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, Calendar.DECEMBER)
        cal.set(Calendar.DAY_OF_MONTH, 31)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}
