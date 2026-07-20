package com.mileowl.tracker.ui.tripdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mileowl.tracker.MileOwlApp
import com.mileowl.tracker.data.model.Trip
import com.mileowl.tracker.data.model.TripClassification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TripDetailUiState(
    val trip: Trip? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false
)

class TripDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MileOwlApp
    private val repo = app.container.tripRepository

    private val _uiState = MutableStateFlow(TripDetailUiState())
    val uiState: StateFlow<TripDetailUiState> = _uiState

    fun loadTrip(tripId: Long) {
        viewModelScope.launch {
            repo.getTripById(tripId).collect { trip ->
                _uiState.value = TripDetailUiState(trip = trip, isLoading = false)
            }
        }
    }

    fun updateClassification(classification: TripClassification) {
        viewModelScope.launch {
            _uiState.value.trip?.let { trip ->
                repo.updateTrip(trip.copy(classification = classification))
            }
        }
    }

    fun updatePurpose(purpose: String) {
        viewModelScope.launch {
            _uiState.value.trip?.let { trip ->
                repo.updateTrip(trip.copy(purpose = purpose.ifBlank { null }))
            }
        }
    }

    fun updateClientName(clientName: String) {
        viewModelScope.launch {
            _uiState.value.trip?.let { trip ->
                repo.updateTrip(trip.copy(clientName = clientName.ifBlank { null }))
            }
        }
    }

    fun deleteTrip() {
        viewModelScope.launch {
            _uiState.value.trip?.let { trip ->
                repo.deleteTrip(trip)
                _uiState.value = _uiState.value.copy(isDeleted = true)
            }
        }
    }
}
