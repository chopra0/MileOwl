package com.mileowl.tracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mileowl.tracker.data.model.SavedLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedLocationDao {

    @Query("SELECT * FROM saved_locations ORDER BY name ASC")
    fun getAllSavedLocations(): Flow<List<SavedLocation>>

    @Query("SELECT * FROM saved_locations WHERE id = :id")
    suspend fun getSavedLocationById(id: Long): SavedLocation?

    @Query("SELECT * FROM saved_locations")
    suspend fun getAllSavedLocationsOnce(): List<SavedLocation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedLocation(location: SavedLocation): Long

    @Update
    suspend fun updateSavedLocation(location: SavedLocation)

    @Delete
    suspend fun deleteSavedLocation(location: SavedLocation)
}
