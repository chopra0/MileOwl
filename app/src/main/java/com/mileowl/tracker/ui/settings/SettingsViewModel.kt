package com.mileowl.tracker.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mileowl.tracker.MileOwlApp
import com.mileowl.tracker.data.model.TripClassification
import com.mileowl.tracker.data.model.Vehicle
import com.mileowl.tracker.service.ActivityTransitionHelper
import com.mileowl.tracker.util.CsvImporter
import com.mileowl.tracker.util.ImportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val irsRate: Double = 0.70,
    val vehicles: List<Vehicle> = emptyList(),
    val autoDetectionEnabled: Boolean = true,
    val highAccuracy: Boolean = true,
    val defaultClassification: TripClassification = TripClassification.UNCLASSIFIED,
    val workHoursEnabled: Boolean = false,
    val workStartHour: String = "08:00",
    val workEndHour: String = "18:00",
    val workDays: String = "Mon,Tue,Wed,Thu,Fri"
)

data class ImportState(
    val isImporting: Boolean = false,
    val result: ImportResult? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MileOwlApp
    private val repo = app.container.tripRepository
    private val prefs = app.container.preferencesManager

    private val _importState = MutableStateFlow(ImportState())
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
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
        },
        combine(
            prefs.workHoursEnabledFlow,
            prefs.workStartHourFlow,
            prefs.workEndHourFlow,
            prefs.workDaysFlow
        ) { enabled, start, end, days ->
            Triple(enabled, start, "$end|$days")
        }
    ) { base, workHours ->
        val parts = workHours.third.split("|")
        base.copy(
            workHoursEnabled = workHours.first,
            workStartHour = workHours.second,
            workEndHour = parts[0],
            workDays = parts[1]
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
                DriveMonitorService.start(app)
            } else {
                ActivityTransitionHelper.unregisterTransitions(app)
                DriveMonitorService.stop(app)
            }
        }
    }

    fun setHighAccuracy(enabled: Boolean) {
        viewModelScope.launch { prefs.setHighAccuracy(enabled) }
    }

    fun setDefaultClassification(classification: TripClassification) {
        viewModelScope.launch { prefs.setDefaultClassification(classification) }
    }

    fun setWorkHoursEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setWorkHoursEnabled(enabled) }
    }

    fun setWorkStartHour(hour: String) {
        viewModelScope.launch { prefs.setWorkStartHour(hour) }
    }

    fun setWorkEndHour(hour: String) {
        viewModelScope.launch { prefs.setWorkEndHour(hour) }
    }

    fun setWorkDays(days: String) {
        viewModelScope.launch { prefs.setWorkDays(days) }
    }

    fun importCsv(uri: Uri) {
        viewModelScope.launch {
            _importState.value = ImportState(isImporting = true)
            val importer = CsvImporter(app, repo)
            val result = importer.importFromUri(uri)
            _importState.value = ImportState(isImporting = false, result = result)
        }
    }

    fun clearImportResult() {
        _importState.value = ImportState()
    }
}
