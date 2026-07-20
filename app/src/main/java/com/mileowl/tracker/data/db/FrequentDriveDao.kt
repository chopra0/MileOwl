package com.mileowl.tracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mileowl.tracker.data.model.FrequentDrive
import kotlinx.coroutines.flow.Flow

@Dao
interface FrequentDriveDao {
    @Query("SELECT * FROM frequent_drives ORDER BY name ASC")
    fun getAllFrequentDrives(): Flow<List<FrequentDrive>>

    @Query("SELECT * FROM frequent_drives WHERE id = :id")
    suspend fun getFrequentDriveById(id: Long): FrequentDrive?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFrequentDrive(drive: FrequentDrive): Long

    @Update
    suspend fun updateFrequentDrive(drive: FrequentDrive)

    @Delete
    suspend fun deleteFrequentDrive(drive: FrequentDrive)
}
