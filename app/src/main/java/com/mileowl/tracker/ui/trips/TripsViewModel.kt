package com.mileowl.tracker.ui.trips

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mileowl.tracker.MileOwlApp
import com.mileowl.tracker.data.model.Trip
import com.mileowl.tracker.data.model.TripClassification
import com.mileowl.tracker.data.model.TripPurpose
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TripsFilter(
    val classification: TripClassification? = null // null = all
)

class TripsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MileOwlApp
    private val repo = app.container.tripRepository

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

    fun setFilter(classification: TripClassification?) {
        filter.value = TripsFilter(classification)
    }

    fun classifyTrip(trip: Trip, classification: TripClassification) {
        viewModelScope.launch {
            val defaultPurpose = when (classification) {
                TripClassification.BUSINESS -> TripPurpose.BUSINESS
                TripClassification.PERSONAL -> TripPurpose.PERSONAL
                TripClassification.UNCLASSIFIED -> null
            }
            repo.updateTrip(
                trip.copy(
                    classification = classification,
                    tripPurpose = trip.tripPurpose ?: defaultPurpose
                )
            )
        }
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

    fun clearSelection() {
        selectedTrips.value = emptySet()
        isSelectionMode.value = false
    }

    fun bulkClassify(classification: TripClassification) {
        viewModelScope.launch {
            val selected = selectedTrips.value
            trips.value.filter { it.id in selected }.forEach { trip ->
                classifyTrip(trip, classification)
            }
            clearSelection()
        }
    }
}
