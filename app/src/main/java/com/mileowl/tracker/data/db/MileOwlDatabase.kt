package com.mileowl.tracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mileowl.tracker.data.model.FrequentDrive
import com.mileowl.tracker.data.model.LocationPoint
import com.mileowl.tracker.data.model.SavedLocation
import com.mileowl.tracker.data.model.Trip
import com.mileowl.tracker.data.model.TripClassification
import com.mileowl.tracker.data.model.TripPurpose
import com.mileowl.tracker.data.model.Vehicle

class Converters {
    @TypeConverter
    fun fromClassification(value: TripClassification): String = value.name

    @TypeConverter
    fun toClassification(value: String): TripClassification =
        TripClassification.valueOf(value)

    @TypeConverter
    fun fromTripPurpose(value: TripPurpose?): String? = value?.name

    @TypeConverter
    fun toTripPurpose(value: String?): TripPurpose? =
        value?.let {
            try { TripPurpose.valueOf(it) } catch (_: Exception) { null }
        }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add new columns to trips
        db.execSQL("ALTER TABLE trips ADD COLUMN tripPurpose TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE trips ADD COLUMN parkingCost REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE trips ADD COLUMN tollsCost REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE trips ADD COLUMN vehicleId INTEGER DEFAULT NULL")

        // Create vehicles table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS vehicles (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                year TEXT NOT NULL DEFAULT '',
                make TEXT NOT NULL DEFAULT '',
                model TEXT NOT NULL DEFAULT '',
                isDefault INTEGER NOT NULL DEFAULT 0
            )
        """)

        // Create frequent_drives table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS frequent_drives (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                startAddress TEXT DEFAULT NULL,
                startLatitude REAL NOT NULL DEFAULT 0.0,
                startLongitude REAL NOT NULL DEFAULT 0.0,
                endAddress TEXT DEFAULT NULL,
                endLatitude REAL NOT NULL DEFAULT 0.0,
                endLongitude REAL NOT NULL DEFAULT 0.0,
                estimatedDistanceMiles REAL NOT NULL DEFAULT 0.0,
                defaultPurpose TEXT DEFAULT NULL,
                defaultVehicleId INTEGER DEFAULT NULL
            )
        """)
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE saved_locations ADD COLUMN defaultClassification TEXT NOT NULL DEFAULT 'UNCLASSIFIED'")
    }
}

@Database(
    entities = [Trip::class, LocationPoint::class, SavedLocation::class, Vehicle::class, FrequentDrive::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MileOwlDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao
    abstract fun savedLocationDao(): SavedLocationDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun frequentDriveDao(): FrequentDriveDao

    companion object {
        @Volatile
        private var INSTANCE: MileOwlDatabase? = null

        fun getInstance(context: Context): MileOwlDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MileOwlDatabase::class.java,
                    "mileowl.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build().also { INSTANCE = it }
            }
        }
    }
}
