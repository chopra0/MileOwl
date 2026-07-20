package com.mileowl.tracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.mileowl.tracker.data.model.LocationPoint
import com.mileowl.tracker.data.model.SavedLocation
import com.mileowl.tracker.data.model.Trip
import com.mileowl.tracker.data.model.TripClassification

class Converters {
    @TypeConverter
    fun fromClassification(value: TripClassification): String = value.name

    @TypeConverter
    fun toClassification(value: String): TripClassification =
        TripClassification.valueOf(value)
}

@Database(
    entities = [Trip::class, LocationPoint::class, SavedLocation::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MileOwlDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao
    abstract fun savedLocationDao(): SavedLocationDao

    companion object {
        @Volatile
        private var INSTANCE: MileOwlDatabase? = null

        fun getInstance(context: Context): MileOwlDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MileOwlDatabase::class.java,
                    "mileowl.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
