package com.mileowl.tracker.data.repository

import com.mileowl.tracker.data.db.SavedLocationDao
import com.mileowl.tracker.data.db.TripDao
import com.mileowl.tracker.data.model.LocationPoint
import com.mileowl.tracker.data.model.SavedLocation
import com.mileowl.tracker.data.model.Trip
import com.mileowl.tracker.data.model.TripClassification
import kotlinx.coroutines.flow.Flow

class TripRepository(
    private val tripDao: TripDao,
    private val savedLocationDao: SavedLocationDao
) {

    fun getAllTrips(): Flow<List<Trip>> = tripDao.getAllTrips()

    fun getTripsInRange(startDate: Long, endDate: Long): Flow<List<Trip>> =
        tripDao.getTripsInRange(startDate, endDate)

    fun getTripsByClassification(classification: TripClassification): Flow<List<Trip>> =
        tripDao.getTripsByClassification(classification)

    fun getTripsByClassificationInRange(
        classification: TripClassification,
        startDate: Long,
        endDate: Long
    ): Flow<List<Trip>> =
        tripDao.getTripsByClassificationInRange(classification, startDate, endDate)

    fun getTripById(id: Long): Flow<Trip?> = tripDao.getTripById(id)

    suspend fun getActiveTripId(): Long? = tripDao.getActiveTripId()

    suspend fun getActiveTrip(): Trip? = tripDao.getActiveTrip()

    suspend fun insertTrip(trip: Trip): Long = tripDao.insertTrip(trip)

    suspend fun updateTrip(trip: Trip) = tripDao.updateTrip(trip)

    suspend fun deleteTrip(trip: Trip) {
        tripDao.deleteLocationPointsForTrip(trip.id)
        tripDao.deleteTrip(trip)
    }

    suspend fun getLocationPointsForTrip(tripId: Long): List<LocationPoint> =
        tripDao.getLocationPointsForTrip(tripId)

    suspend fun insertLocationPoint(point: LocationPoint) =
        tripDao.insertLocationPoint(point)

    // Saved Locations

    fun getAllSavedLocations(): Flow<List<SavedLocation>> =
        savedLocationDao.getAllSavedLocations()

    suspend fun getAllSavedLocationsOnce(): List<SavedLocation> =
        savedLocationDao.getAllSavedLocationsOnce()

    suspend fun getSavedLocationById(id: Long): SavedLocation? =
        savedLocationDao.getSavedLocationById(id)

    suspend fun insertSavedLocation(location: SavedLocation): Long =
        savedLocationDao.insertSavedLocation(location)

    suspend fun updateSavedLocation(location: SavedLocation) =
        savedLocationDao.updateSavedLocation(location)

    suspend fun deleteSavedLocation(location: SavedLocation) =
        savedLocationDao.deleteSavedLocation(location)

    /**
     * Find a saved location near the given coordinates.
     * Returns the closest match within its radius, or null.
     */
    suspend fun findNearbyLocation(latitude: Double, longitude: Double): SavedLocation? {
        val locations = savedLocationDao.getAllSavedLocationsOnce()
        return locations.firstOrNull { loc ->
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                latitude, longitude,
                loc.latitude, loc.longitude,
                results
            )
            results[0] <= loc.radiusMeters
        }
    }
}
