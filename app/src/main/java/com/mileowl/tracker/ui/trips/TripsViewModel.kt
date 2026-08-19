package com.mileowl.tracker.ui.trips

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mileowl.tracker.MileOwlApp
import com.mileowl.tracker.data.model.Trip
import com.mileowl.tracker.data.model.TripClassification
import com.mileowl.tracker.data.model.TripPurpose
import com.mileowl.tracker.data.model.Vehicle
import com.mileowl.tracker.ui.home.PendingClassification
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TripsFilter(
    val classification: TripClassification? = null // null = all
)

class TripsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MileOwlApp
    private val repo = app.container.tripRepository
    private val prefs = app.container.preferencesManager

    val filter = MutableStateFlow(TripsFilter())

    @OptIn(ExperimentalCoroutinesApi::class)
    val trips: StateFlow<List<Trip>> = filter.flatMapLatest { f ->
        if (f.classification != null) {
            repo.getTripsByClassification(f.classification)
        } else {
            repo.getAllTrips()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selection mode state
    val selectedTrips = MutableStateFlow<Set<Long>>(emptySet())
    val isSelectionMode = MutableStateFlow(false)

    // Vehicle selection state
    val vehicles: StateFlow<List<Vehicle>> = repo.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val skipVehiclePrompt: StateFlow<Boolean> = prefs.skipVehiclePromptFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _pendingClassification = MutableStateFlow<PendingClassification?>(null)
    val pendingClassification: StateFlow<PendingClassification?> = _pendingClassification

    fun setFilter(classification: TripClassification?) {
        filter.value = TripsFilter(classification)
    }

    /**
     * Called when the user swipes or taps to classify a trip. If the user has 2+
     * vehicles and hasn't opted out of the vehicle prompt, sets a pending state
     * to trigger the vehicle selection dialog. Otherwise, completes immediately.
     */
    fun classifyTrip(trip: Trip, classification: TripClassification) {
        viewModelScope.launch {
            val vehicleList = vehicles.value
            val skip = skipVehiclePrompt.value

            if (vehicleList.size >= 2 && !skip) {
                _pendingClassification.value = PendingClassification(trip, classification)
            } else {
                val defaultVehicle = repo.getDefaultVehicle()
                completeClassification(trip, classification, defaultVehicle?.id)
            }
        }
    }

    fun completePendingClassification(vehicleId: Long, alwaysUseThis: Boolean) {
        viewModelScope.launch {
            val pending = _pendingClassification.value ?: return@launch
            _pendingClassification.value = null

            if (alwaysUseThis) {
                prefs.setSkipVehiclePrompt(true)
                repo.setDefaultVehicle(vehicleId)
            }

            completeClassification(pending.trip, pending.classification, vehicleId)
        }
    }

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

    fun enterSelectionMode(tripId: Long) {
        isSelectionMode.value = true
        selectedTrips.value = setOf(tripId)
    }

    fun toggleSelection(tripId: Long) {
        val current = selectedTrips.value.toMutableSet()
        if (current.contains(tripId)) current.remove(tripId) else current.add(tripId)
        selectedTrips.value = current
        if (current.isEmpty()) isSelectionMode.value = false
    }

    fun selectAllUnclassified() {
        val unclassified = trips.value.filter { it.classification == TripClassification.UNCLASSIFIED }
        selectedTrips.value = unclassified.map { it.id }.toSet()
        if (selectedTrips.value.isNotEmpty()) isSelectionMode.value = true
    }

    fun selectAll() {
        selectedTrips.value = trips.value.map { it.id }.toSet()
        if (selectedTrips.value.isNotEmpty()) isSelectionMode.value = true
    }

    fun clearSelection() {
        selectedTrips.value = emptySet()
        isSelectionMode.value = false
    }

    fun bulkClassify(classification: TripClassification) {
        viewModelScope.launch {
            val selected = selectedTrips.value
            val defaultVehicle = repo.getDefaultVehicle()
            trips.value.filter { it.id in selected }.forEach { trip ->
                completeClassification(trip, classification, defaultVehicle?.id)
            }
            clearSelection()
        }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val selected = selectedTrips.value.toList()
            repo.deleteTrips(selected)
            clearSelection()
        }
    }
}
