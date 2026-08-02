package com.mileowl.tracker.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mileowl.tracker.data.model.TripClassification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mileowl_prefs")

class PreferencesManager(private val context: Context) {

    /**
     * Synchronous read of auto-detection preference for use in Application.onCreate()
     * and BootReceiver where coroutines aren't available. Reads from the same
     * DataStore backing file via SharedPreferences.
     */
    fun isAutoDetectionEnabledSync(): Boolean {
        // DataStore prefs are stored in "mileowl_prefs.preferences_pb" but we
        // can't easily read protobuf synchronously. Use a parallel SharedPreferences
        // mirror that we keep in sync.
        val sharedPrefs = context.getSharedPreferences("mileowl_sync_prefs", Context.MODE_PRIVATE)
        return sharedPrefs.getBoolean("auto_detection_enabled", true)
    }

    private fun mirrorAutoDetection(enabled: Boolean) {
        context.getSharedPreferences("mileowl_sync_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("auto_detection_enabled", enabled)
            .apply()
    }

    private object Keys {
        val IRS_RATE = doublePreferencesKey("irs_rate")
        val VEHICLE_NAME = stringPreferencesKey("vehicle_name")
        val VEHICLE_YEAR = stringPreferencesKey("vehicle_year")
        val VEHICLE_MAKE = stringPreferencesKey("vehicle_make")
        val VEHICLE_MODEL = stringPreferencesKey("vehicle_model")
        val AUTO_DETECTION_ENABLED = booleanPreferencesKey("auto_detection_enabled")
        val HIGH_ACCURACY = booleanPreferencesKey("high_accuracy")
        val DEFAULT_CLASSIFICATION = stringPreferencesKey("default_classification")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val WORK_HOURS_ENABLED = booleanPreferencesKey("work_hours_enabled")
        val WORK_START_HOUR = stringPreferencesKey("work_start_hour")
        val WORK_END_HOUR = stringPreferencesKey("work_end_hour")
        val WORK_DAYS = stringPreferencesKey("work_days")
    }

    val irsRateFlow: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[Keys.IRS_RATE] ?: Constants.IRS_RATE_2026
    }

    val vehicleNameFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.VEHICLE_NAME] ?: ""
    }

    val vehicleYearFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.VEHICLE_YEAR] ?: ""
    }

    val vehicleMakeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.VEHICLE_MAKE] ?: ""
    }

    val vehicleModelFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.VEHICLE_MODEL] ?: ""
    }

    val autoDetectionEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_DETECTION_ENABLED] ?: true
    }

    val highAccuracyFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HIGH_ACCURACY] ?: true
    }

    val defaultClassificationFlow: Flow<TripClassification> = context.dataStore.data.map { prefs ->
        val name = prefs[Keys.DEFAULT_CLASSIFICATION] ?: TripClassification.UNCLASSIFIED.name
        TripClassification.valueOf(name)
    }

    suspend fun setIrsRate(rate: Double) {
        context.dataStore.edit { it[Keys.IRS_RATE] = rate }
    }

    suspend fun setVehicleName(name: String) {
        context.dataStore.edit { it[Keys.VEHICLE_NAME] = name }
    }

    suspend fun setVehicleYear(year: String) {
        context.dataStore.edit { it[Keys.VEHICLE_YEAR] = year }
    }

    suspend fun setVehicleMake(make: String) {
        context.dataStore.edit { it[Keys.VEHICLE_MAKE] = make }
    }

    suspend fun setVehicleModel(model: String) {
        context.dataStore.edit { it[Keys.VEHICLE_MODEL] = model }
    }

    suspend fun setAutoDetectionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_DETECTION_ENABLED] = enabled }
        mirrorAutoDetection(enabled)
    }

    suspend fun setHighAccuracy(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HIGH_ACCURACY] = enabled }
    }

    suspend fun setDefaultClassification(classification: TripClassification) {
        context.dataStore.edit { it[Keys.DEFAULT_CLASSIFICATION] = classification.name }
    }

    val onboardingCompleteFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETE] ?: false
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    // Work Hours preferences
    val workHoursEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.WORK_HOURS_ENABLED] ?: false
    }

    val workStartHourFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.WORK_START_HOUR] ?: "08:00"
    }

    val workEndHourFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.WORK_END_HOUR] ?: "18:00"
    }

    val workDaysFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.WORK_DAYS] ?: "Mon,Tue,Wed,Thu,Fri"
    }

    suspend fun setWorkHoursEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WORK_HOURS_ENABLED] = enabled }
    }

    suspend fun setWorkStartHour(hour: String) {
        context.dataStore.edit { it[Keys.WORK_START_HOUR] = hour }
    }

    suspend fun setWorkEndHour(hour: String) {
        context.dataStore.edit { it[Keys.WORK_END_HOUR] = hour }
    }

    suspend fun setWorkDays(days: String) {
        context.dataStore.edit { it[Keys.WORK_DAYS] = days }
    }
}
