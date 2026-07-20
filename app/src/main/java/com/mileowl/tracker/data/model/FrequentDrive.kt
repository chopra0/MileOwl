package com.mileowl.tracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "frequent_drives")
data class FrequentDrive(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startAddress: String? = null,
    val startLatitude: Double = 0.0,
    val startLongitude: Double = 0.0,
    val endAddress: String? = null,
    val endLatitude: Double = 0.0,
    val endLongitude: Double = 0.0,
    val estimatedDistanceMiles: Double = 0.0,
    val defaultPurpose: TripPurpose? = null,
    val defaultVehicleId: Long? = null
)
