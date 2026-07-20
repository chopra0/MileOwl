package com.mileowl.tracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val year: String = "",
    val make: String = "",
    val model: String = "",
    val isDefault: Boolean = false
)
