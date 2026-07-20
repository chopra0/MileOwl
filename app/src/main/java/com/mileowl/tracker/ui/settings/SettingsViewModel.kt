package com.mileowl.tracker.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mileowl.tracker.MileOwlApp
import com.mileowl.tracker.data.model.TripClassification
import com.mileowl.tracker.service.ActivityTransitionHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val irsRate: Double = 0.70,
    val vehicleName: String = "",
    val vehicleYear: String = "",
    val vehicleMake: String = "",
    val vehicleModel: String = "",
    val autoDetectionEnabled: Boolean = true,
    val highAccuracy: Boolean = true,
    val defaultClassification: TripClassification = TripClassification.UNCLASSIFIED
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MileOwlApp
    private val prefs = app.container.preferencesManager

    val uiState: StateFlow<SettingsUiState> = combine(
        prefs.irsRateFlow,
        prefs.vehicleNameFlow,
        prefs.vehicleYearFlow,
        prefs.vehicleMakeFlow,
        combine(
            prefs.vehicleModelFlow,
            prefs.autoDetectionEnabledFlow,
            prefs.highAccuracyFlow,
            prefs.defaultClassificationFlow
        ) { model, auto, accuracy, classification ->
            Triple(model, Triple(auto, accuracy, classification), Unit)
        }
    ) { irsRate, name, year, make, (model, extras, _) ->
        val (auto, accuracy, classification) = extras
        SettingsUiState(
            irsRate = irsRate,
            vehicleName = name,
            vehicleYear = year,
            vehicleMake = make,
            vehicleModel = model,
            autoDetectionEnabled = auto,
            highAccuracy = accuracy,
            defaultClassification = classification
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setIrsRate(rate: String) {
        val parsed = rate.toDoubleOrNull() ?: return
        viewModelScope.launch { prefs.setIrsRate(parsed) }
    }

    fun setVehicleName(name: String) {
        viewModelScope.launch { prefs.setVehicleName(name) }
    }

    fun setVehicleYear(year: String) {
        viewModelScope.launch { prefs.setVehicleYear(year) }
    }

    fun setVehicleMake(make: String) {
        viewModelScope.launch { prefs.setVehicleMake(make) }
    }

    fun setVehicleModel(model: String) {
        viewModelScope.launch { prefs.setVehicleModel(model) }
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
