package com.mileowl.tracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_locations")
data class SavedLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 200f,
    val defaultClassification: TripClassification = TripClassification.UNCLASSIFIED
)
