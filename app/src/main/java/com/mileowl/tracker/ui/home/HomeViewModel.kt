package com.mileowl.tracker.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mileowl.tracker.MileOwlApp
import com.mileowl.tracker.data.model.Trip
import com.mileowl.tracker.data.model.TripClassification
import com.mileowl.tracker.service.TripTrackingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class HomeUiState(
    val isTracking: Boolean = false,
    val currentTripMiles: Double = 0.0,
    val monthBusinessMiles: Double = 0.0,
    val monthPersonalMiles: Double = 0.0,
    val monthTotalTrips: Int = 0,
    val ytdBusinessMiles: Double = 0.0,
    val ytdDeduction: Double = 0.0,
    val irsRate: Double = 0.70,
    val recentTrips: List<Trip> = emptyList()
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MileOwlApp
    private val repo = app.container.tripRepository
    private val prefs = app.container.preferencesManager

    val uiState: StateFlow<HomeUiState> = combine(
        repo.getTripsInRange(startOfMonth(), endOfMonth()),
        repo.getTripsInRange(startOfYear(), endOfYear()),
        prefs.irsRateFlow,
        TripTrackingService.isTrackingFlow,
        TripTrackingService.currentDistanceMilesFlow
    ) { monthTrips, yearTrips, irsRate, tracking, currentMiles ->
        val monthBusiness = monthTrips.filter { it.classification == TripClassification.BUSINESS }
        val monthPersonal = monthTrips.filter { it.classification == TripClassification.PERSONAL }
        val ytdBusiness = yearTrips.filter { it.classification == TripClassification.BUSINESS }

        HomeUiState(
            isTracking = tracking,
            currentTripMiles = currentMiles,
            monthBusinessMiles = monthBusiness.sumOf { it.distanceMiles },
            monthPersonalMiles = monthPersonal.sumOf { it.distanceMiles },
            monthTotalTrips = monthTrips.size,
            ytdBusinessMiles = ytdBusiness.sumOf { it.distanceMiles },
            ytdDeduction = ytdBusiness.sumOf { it.distanceMiles } * irsRate,
            irsRate = irsRate,
            recentTrips = monthTrips.take(5)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    private fun startOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun endOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

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

    private fun endOfYear(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, Calendar.DECEMBER)
        cal.set(Calendar.DAY_OF_MONTH, 31)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}
