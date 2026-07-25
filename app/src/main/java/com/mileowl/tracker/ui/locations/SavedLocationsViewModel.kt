package com.mileowl.tracker.ui.locations

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mileowl.tracker.MileOwlApp
import com.mileowl.tracker.data.model.SavedLocation
import com.mileowl.tracker.data.model.TripClassification
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavedLocationsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MileOwlApp
    private val repo = app.container.tripRepository

    val locations: StateFlow<List<SavedLocation>> =
        repo.getAllSavedLocations()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addLocation(
        name: String,
        address: String?,
        latitude: Double,
        longitude: Double,
        radius: Float,
        classification: TripClassification = TripClassification.UNCLASSIFIED
    ) {
        viewModelScope.launch {
            repo.insertSavedLocation(
                SavedLocation(
                    name = name,
                    address = address,
                    latitude = latitude,
                    longitude = longitude,
                    radiusMeters = radius,
                    defaultClassification = classification
                )
            )
        }
    }

    fun updateLocation(location: SavedLocation) {
        viewModelScope.launch {
            repo.updateSavedLocation(location)
        }
    }

    fun deleteLocation(location: SavedLocation) {
        viewModelScope.launch {
            repo.deleteSavedLocation(location)
        }
    }
}
