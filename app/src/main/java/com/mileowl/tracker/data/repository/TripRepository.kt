package com.mileowl.tracker.data.repository

import com.mileowl.tracker.data.db.FrequentDriveDao
import com.mileowl.tracker.data.db.SavedLocationDao
import com.mileowl.tracker.data.db.TripDao
import com.mileowl.tracker.data.db.VehicleDao
import com.mileowl.tracker.data.model.FrequentDrive
import com.mileowl.tracker.data.model.LocationPoint
import com.mileowl.tracker.data.model.SavedLocation
import com.mileowl.tracker.data.model.Trip
import com.mileowl.tracker.data.model.TripClassification
import com.mileowl.tracker.data.model.Vehicle
import kotlinx.coroutines.flow.Flow

class TripRepository(
    private val tripDao: TripDao,
    private val savedLocationDao: SavedLocationDao,
    private val vehicleDao: VehicleDao,
    private val frequentDriveDao: FrequentDriveDao
) {

    // ── Trips ──────────────────────────────────────────────────────────

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

    suspend fun getTripByIdOnce(id: Long): Trip? = tripDao.getTripByIdOnce(id)

    suspend fun getActiveTripId(): Long? = tripDao.getActiveTripId()

    suspend fun getActiveTrip(): Trip? = tripDao.getActiveTrip()

    suspend fun insertTrip(trip: Trip): Long = tripDao.insertTrip(trip)

    suspend fun updateTrip(trip: Trip) = tripDao.updateTrip(trip)

    suspend fun deleteTrip(trip: Trip) {
        tripDao.deleteLocationPointsForTrip(trip.id)
        tripDao.deleteTrip(trip)
    }

    suspend fun deleteTrip(tripId: Long) {
        tripDao.deleteLocationPointsForTrip(tripId)
        tripDao.deleteTripById(tripId)
    }

    suspend fun deleteTrips(tripIds: List<Long>) {
        if (tripIds.isEmpty()) return
        tripDao.deleteLocationPointsForTrips(tripIds)
        tripDao.deleteTripsById(tripIds)
    }

    suspend fun getLocationPointsForTrip(tripId: Long): List<LocationPoint> =
        tripDao.getLocationPointsForTrip(tripId)

    suspend fun insertLocationPoint(point: LocationPoint) =
        tripDao.insertLocationPoint(point)

    fun getTripsByVehicle(vehicleId: Long): Flow<List<Trip>> =
        tripDao.getTripsByVehicle(vehicleId)

    fun getTripsByVehicleInRange(vehicleId: Long, startDate: Long, endDate: Long): Flow<List<Trip>> =
        tripDao.getTripsByVehicleInRange(vehicleId, startDate, endDate)

    // ── Saved Locations ────────────────────────────────────────────────

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

    // ── Vehicles ───────────────────────────────────────────────────────

    fun getAllVehicles(): Flow<List<Vehicle>> = vehicleDao.getAllVehicles()

    suspend fun getVehicleById(id: Long): Vehicle? = vehicleDao.getVehicleById(id)

    suspend fun getDefaultVehicle(): Vehicle? = vehicleDao.getDefaultVehicle()

    suspend fun insertVehicle(vehicle: Vehicle): Long = vehicleDao.insertVehicle(vehicle)

    suspend fun updateVehicle(vehicle: Vehicle) = vehicleDao.updateVehicle(vehicle)

    suspend fun deleteVehicle(vehicle: Vehicle) = vehicleDao.deleteVehicle(vehicle)

    suspend fun clearDefaultVehicle() = vehicleDao.clearDefaultVehicle()

    suspend fun setDefaultVehicle(id: Long) {
        vehicleDao.clearDefaultVehicle()
        vehicleDao.setDefaultVehicle(id)
    }

    // ── Frequent Drives ────────────────────────────────────────────────

    fun getAllFrequentDrives(): Flow<List<FrequentDrive>> = frequentDriveDao.getAllFrequentDrives()

    suspend fun getFrequentDriveById(id: Long): FrequentDrive? =
        frequentDriveDao.getFrequentDriveById(id)

    suspend fun insertFrequentDrive(drive: FrequentDrive): Long =
        frequentDriveDao.insertFrequentDrive(drive)

    suspend fun updateFrequentDrive(drive: FrequentDrive) =
        frequentDriveDao.updateFrequentDrive(drive)

    suspend fun deleteFrequentDrive(drive: FrequentDrive) =
        frequentDriveDao.deleteFrequentDrive(drive)

    /**
     * Find a frequent drive matching both start and end coordinates within the given radius.
     */
    suspend fun findMatchingFrequentDrive(
        startLat: Double, startLon: Double,
        endLat: Double, endLon: Double,
        radiusMeters: Float = 300f
    ): FrequentDrive? {
        val drives = frequentDriveDao.getAllFrequentDrivesOnce()
        return drives.firstOrNull { drive ->
            val startResults = FloatArray(1)
            android.location.Location.distanceBetween(
                startLat, startLon,
                drive.startLatitude, drive.startLongitude,
                startResults
            )
            val endResults = FloatArray(1)
            android.location.Location.distanceBetween(
                endLat, endLon,
                drive.endLatitude, drive.endLongitude,
                endResults
            )
            startResults[0] <= radiusMeters && endResults[0] <= radiusMeters
        }
    }

    // ── Counts ─────────────────────────────────────────────────────────

    suspend fun getUnclassifiedTripCount(): Int = tripDao.getUnclassifiedTripCount()
}
