package com.mileowl.tracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mileowl.tracker.data.model.LocationPoint
import com.mileowl.tracker.data.model.Trip
import com.mileowl.tracker.data.model.TripClassification
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Query("SELECT * FROM trips WHERE isActive = 0 ORDER BY startTime DESC")
    fun getAllTrips(): Flow<List<Trip>>

    @Query(
        "SELECT * FROM trips WHERE isActive = 0 AND startTime >= :startDate AND startTime <= :endDate ORDER BY startTime DESC"
    )
    fun getTripsInRange(startDate: Long, endDate: Long): Flow<List<Trip>>

    @Query(
        "SELECT * FROM trips WHERE isActive = 0 AND classification = :classification ORDER BY startTime DESC"
    )
    fun getTripsByClassification(classification: TripClassification): Flow<List<Trip>>

    @Query(
        """SELECT * FROM trips 
        WHERE isActive = 0 
        AND classification = :classification 
        AND startTime >= :startDate AND startTime <= :endDate 
        ORDER BY startTime DESC"""
    )
    fun getTripsByClassificationInRange(
        classification: TripClassification,
        startDate: Long,
        endDate: Long
    ): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE id = :id")
    fun getTripById(id: Long): Flow<Trip?>

    @Query("SELECT id FROM trips WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTripId(): Long?

    @Query("SELECT * FROM trips WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTrip(): Trip?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip): Long

    @Update
    suspend fun updateTrip(trip: Trip)

    @Delete
    suspend fun deleteTrip(trip: Trip)

    @Query("SELECT * FROM location_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    suspend fun getLocationPointsForTrip(tripId: Long): List<LocationPoint>

    @Insert
    suspend fun insertLocationPoint(point: LocationPoint)

    @Query("DELETE FROM location_points WHERE tripId = :tripId")
    suspend fun deleteLocationPointsForTrip(tripId: Long)
}
