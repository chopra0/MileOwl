package com.mileowl.tracker.ui.report

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mileowl.tracker.MileOwlApp
import com.mileowl.tracker.data.model.Trip
import com.mileowl.tracker.data.model.TripClassification
import com.mileowl.tracker.util.CsvExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

data class ReportUiState(
    val trips: List<Trip> = emptyList(),
    val startDate: Long = startOfYear(),
    val endDate: Long = endOfToday(),
    val totalBusinessMiles: Double = 0.0,
    val totalPersonalMiles: Double = 0.0,
    val totalDeduction: Double = 0.0,
    val irsRate: Double = 0.70,
    val businessTripCount: Int = 0,
    val csvFile: File? = null
)

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

private fun endOfToday(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    return cal.timeInMillis
}

class ReportViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MileOwlApp
    private val repo = app.container.tripRepository
    private val prefs = app.container.preferencesManager

    private val _dateRange = MutableStateFlow(Pair(startOfYear(), endOfToday()))

    val uiState: StateFlow<ReportUiState> = combine(
        _dateRange.flatMapLatest { (start, end) ->
            repo.getTripsInRange(start, end)
        },
        prefs.irsRateFlow,
        _dateRange
    ) { trips, irsRate, (start, end) ->
        val businessTrips = trips.filter { it.classification == TripClassification.BUSINESS }
        val personalTrips = trips.filter { it.classification == TripClassification.PERSONAL }

        ReportUiState(
            trips = trips,
            startDate = start,
            endDate = end,
            totalBusinessMiles = businessTrips.sumOf { it.distanceMiles },
            totalPersonalMiles = personalTrips.sumOf { it.distanceMiles },
            totalDeduction = businessTrips.sumOf { it.distanceMiles } * irsRate,
            irsRate = irsRate,
            businessTripCount = businessTrips.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportUiState())

    fun setDateRange(start: Long, end: Long) {
        _dateRange.value = Pair(start, end)
    }

    fun generateCsv(context: Context): File {
        val state = uiState.value
        val vehicleInfo = "" // Could gather from prefs synchronously if needed
        return CsvExporter.generateCsv(
            context = context,
            trips = state.trips,
            irsRate = state.irsRate,
            vehicleInfo = vehicleInfo,
            startDate = state.startDate,
            endDate = state.endDate
        )
    }
}
