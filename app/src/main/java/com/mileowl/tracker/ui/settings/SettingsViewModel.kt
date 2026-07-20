package com.mileowl.tracker.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mileowl.tracker.MileOwlApp
import com.mileowl.tracker.data.model.TripClassification
import com.mileowl.tracker.data.model.Vehicle
import com.mileowl.tracker.service.ActivityTransitionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val irsRate: Double = 0.70,
    val vehicles: List<Vehicle> = emptyList(),
    val autoDetectionEnabled: Boolean = true,
    val highAccuracy: Boolean = true,
    val defaultClassification: TripClassification = TripClassification.UNCLASSIFIED
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MileOwlApp
    private val repo = app.container.tripRepository
    private val prefs = app.container.preferencesManager

    val uiState: StateFlow<SettingsUiState> = combine(
        prefs.irsRateFlow,
        repo.getAllVehicles(),
        prefs.autoDetectionEnabledFlow,
        prefs.highAccuracyFlow,
        prefs.defaultClassificationFlow
    ) { irsRate, vehicles, auto, accuracy, classification ->
        SettingsUiState(
            irsRate = irsRate,
            vehicles = vehicles,
            autoDetectionEnabled = auto,
            highAccuracy = accuracy,
            defaultClassification = classification
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setIrsRate(rate: String) {
        val parsed = rate.toDoubleOrNull() ?: return
        viewModelScope.launch { prefs.setIrsRate(parsed) }
    }

    fun addVehicle(name: String, year: String, make: String, model: String) {
        viewModelScope.launch {
            val vehicle = Vehicle(name = name, year = year, make = make, model = model)
            repo.insertVehicle(vehicle)
        }
    }

    fun updateVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repo.updateVehicle(vehicle)
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repo.deleteVehicle(vehicle)
        }
    }

    fun setDefaultVehicle(vehicleId: Long) {
        viewModelScope.launch {
            repo.clearDefaultVehicle()
            repo.setDefaultVehicle(vehicleId)
        }
    }

    fun setAutoDetectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setAutoDetectionEnabled(enabled)
            if (enabled) {
                ActivityTransitionHelper.registerTransitions(app)
            } else {
                ActivityTransitionHelper.unregisterTransitions(app)
            }
        }
    }

    fun setHighAccuracy(enabled: Boolean) {
        viewModelScope.launch { prefs.setHighAccuracy(enabled) }
    }

    fun setDefaultClassification(classification: TripClassification) {
        viewModelScope.launch { prefs.setDefaultClassification(classification) }
    }
}
