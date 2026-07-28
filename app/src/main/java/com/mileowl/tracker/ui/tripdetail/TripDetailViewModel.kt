package com.mileowl.tracker.ui.tripdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mileowl.tracker.MileOwlApp
import com.mileowl.tracker.data.model.FrequentDrive
import com.mileowl.tracker.data.model.LocationPoint
import com.mileowl.tracker.data.model.Trip
import com.mileowl.tracker.data.model.TripClassification
import com.mileowl.tracker.data.model.TripPurpose
import com.mileowl.tracker.data.model.Vehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TripDetailUiState(
    val trip: Trip? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val vehicles: List<Vehicle> = emptyList(),
    val locationPoints: List<LocationPoint> = emptyList(),
    val savedAsFrequentDrive: Boolean = false
)

class TripDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MileOwlApp
    private val repo = app.container.tripRepository

    private val _uiState = MutableStateFlow(TripDetailUiState())
    val uiState: StateFlow<TripDetailUiState> = _uiState

    init {
        // Collect vehicles
        viewModelScope.launch {
            repo.getAllVehicles().collect { vehicles ->
                _uiState.value = _uiState.value.copy(vehicles = vehicles)
            }
        }
    }

    fun loadTrip(tripId: Long) {
        viewModelScope.launch {
            // Load location points once
            val points = repo.getLocationPointsForTrip(tripId)
            _uiState.value = _uiState.value.copy(locationPoints = points)

            // Collect trip changes
            repo.getTripById(tripId).collect { trip ->
                _uiState.value = _uiState.value.copy(trip = trip, isLoading = false)
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

    fun updateTripPurpose(purpose: TripPurpose) {
        viewModelScope.launch {
            _uiState.value.trip?.let { trip ->
                repo.updateTrip(
                    trip.copy(
                        tripPurpose = purpose,
                        classification = purpose.toClassification()
                    )
                )
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

    fun updateParkingCost(cost: String) {
        viewModelScope.launch {
            _uiState.value.trip?.let { trip ->
                val parsed = cost.toDoubleOrNull() ?: 0.0
                repo.updateTrip(trip.copy(parkingCost = parsed))
            }
        }
    }

    fun updateTollsCost(cost: String) {
        viewModelScope.launch {
            _uiState.value.trip?.let { trip ->
                val parsed = cost.toDoubleOrNull() ?: 0.0
                repo.updateTrip(trip.copy(tollsCost = parsed))
            }
        }
    }

    fun updateVehicleId(vehicleId: Long?) {
        viewModelScope.launch {
            _uiState.value.trip?.let { trip ->
                repo.updateTrip(trip.copy(vehicleId = vehicleId))
            }
        }
    }

    fun addVehicle(name: String, year: String, make: String, model: String) {
        viewModelScope.launch {
            val vehicle = Vehicle(name = name, year = year, make = make, model = model)
            val id = repo.insertVehicle(vehicle)
            // Auto-assign the new vehicle to this trip
            _uiState.value.trip?.let { trip ->
                repo.updateTrip(trip.copy(vehicleId = id))
            }
        }
    }

    fun saveAsFrequentDrive() {
        viewModelScope.launch {
            _uiState.value.trip?.let { trip ->
                val drive = FrequentDrive(
                    name = buildString {
                        append(trip.startAddress?.take(20) ?: "Start")
                        append(" → ")
                        append(trip.endAddress?.take(20) ?: "End")
                    },
                    startAddress = trip.startAddress,
                    startLatitude = trip.startLatitude,
                    startLongitude = trip.startLongitude,
                    endAddress = trip.endAddress,
                    endLatitude = trip.endLatitude ?: 0.0,
                    endLongitude = trip.endLongitude ?: 0.0,
                    estimatedDistanceMiles = trip.distanceMiles,
                    defaultPurpose = trip.tripPurpose,
                    defaultVehicleId = trip.vehicleId
                )
                repo.insertFrequentDrive(drive)
                _uiState.value = _uiState.value.copy(savedAsFrequentDrive = true)
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
