package com.mileowl.tracker.ui.trips

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mileowl.tracker.MileOwlApp
import com.mileowl.tracker.data.model.Trip
import com.mileowl.tracker.data.model.TripClassification
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

    fun setFilter(classification: TripClassification?) {
        filter.value = TripsFilter(classification)
    }

    fun classifyTrip(trip: Trip, classification: TripClassification) {
        viewModelScope.launch {
            repo.updateTrip(trip.copy(classification = classification))
        }
    }
}
