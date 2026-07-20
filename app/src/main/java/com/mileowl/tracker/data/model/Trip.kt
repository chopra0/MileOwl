package com.mileowl.tracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val startLatitude: Double = 0.0,
    val startLongitude: Double = 0.0,
    val endLatitude: Double? = null,
    val endLongitude: Double? = null,
    val startAddress: String? = null,
    val endAddress: String? = null,
    val distanceMiles: Double = 0.0,
    val durationMinutes: Int = 0,
    val classification: TripClassification = TripClassification.UNCLASSIFIED,
    val purpose: String? = null,
    val clientName: String? = null,
    val isActive: Boolean = false
)
