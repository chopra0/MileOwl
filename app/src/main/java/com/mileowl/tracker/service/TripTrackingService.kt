package com.mileowl.tracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mileowl.tracker.MainActivity
import com.mileowl.tracker.MileOwlApp
import com.mileowl.tracker.R
import com.mileowl.tracker.data.model.LocationPoint
import com.mileowl.tracker.data.model.Trip
import com.mileowl.tracker.data.model.TripClassification
import com.mileowl.tracker.data.model.TripPurpose
import com.mileowl.tracker.util.Constants
import com.mileowl.tracker.util.DistanceCalculator
import com.mileowl.tracker.util.GeocoderHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TripTrackingService : Service() {

    companion object {
        private const val TAG = "TripTrackingService"

        private val _isTracking = kotlinx.coroutines.flow.MutableStateFlow(false)
        val isTrackingFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = _isTracking
        val isTracking: Boolean get() = _isTracking.value

        private val _currentDistanceMiles = kotlinx.coroutines.flow.MutableStateFlow(0.0)
        val currentDistanceMilesFlow: kotlinx.coroutines.flow.StateFlow<Double> = _currentDistanceMiles
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var currentTripId: Long = -1
    private var startLocation: Location? = null
    private var lastLocation: Location? = null
    private var totalDistanceMeters: Double = 0.0
    private var pointCount: Int = 0

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Constants.ACTION_START_TRACKING -> {
                if (!isTracking) {
                    startTracking()
                }
            }
            Constants.ACTION_STOP_TRACKING -> {
                stopTracking()
            }
            else -> {
                if (!isTracking) {
                    startTracking()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startTracking() {
        Log.d(TAG, "Starting trip tracking")

        // Prompt for battery optimization exemption if not already granted
        checkBatteryExemption()

        _isTracking.value = true
        _currentDistanceMiles.value = 0.0
        totalDistanceMeters = 0.0
        pointCount = 0
        startLocation = null
        lastLocation = null

        val notification = buildNotification(0.0)
        startForeground(Constants.TRACKING_NOTIFICATION_ID, notification)

        // Create a new trip record
        serviceScope.launch {
            val app = applicationContext as MileOwlApp
            val repo = app.container.tripRepository
            val prefs = app.container.preferencesManager

            var defaultClassification = com.mileowl.tracker.data.model.TripClassification.UNCLASSIFIED
            prefs.defaultClassificationFlow.collect { classification ->
                defaultClassification = classification
                // Only need the first emission
                val trip = Trip(
                    startTime = System.currentTimeMillis(),
                    classification = defaultClassification,
                    isActive = true
                )
                currentTripId = repo.insertTrip(trip)
                Log.d(TAG, "Created trip with id=$currentTripId")
                return@collect
            }
        }

        startLocationUpdates()
    }

    /**
     * Prompts the user to exempt MileOwl from battery optimization if not already granted.
     * Called reactively at the moment tracking starts, when it actually matters.
     */
    private fun checkBatteryExemption() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                ).apply {
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Could not request battery optimization exemption", e)
            }
        }
    }

    private fun stopTracking() {
        Log.d(TAG, "Stopping trip tracking")
        _isTracking.value = false
        stopLocationUpdates()

        // Finalize the trip
        serviceScope.launch {
            try {
                val app = applicationContext as MileOwlApp
                val repo = app.container.tripRepository
                val prefs = app.container.preferencesManager

                if (currentTripId > 0) {
                    val points = repo.getLocationPointsForTrip(currentTripId)
                    val distanceMiles = DistanceCalculator.calculateDistanceMiles(points)
                    val now = System.currentTimeMillis()

                    // Reverse geocode start and end
                    val startAddr = startLocation?.let {
                        GeocoderHelper.reverseGeocode(applicationContext, it.latitude, it.longitude)
                    }
                    val endAddr = lastLocation?.let {
                        GeocoderHelper.reverseGeocode(applicationContext, it.latitude, it.longitude)
                    }

                    // Check for nearby saved locations
                    val startSaved = startLocation?.let {
                        repo.findNearbyLocation(it.latitude, it.longitude)
                    }
                    val endSaved = lastLocation?.let {
                        repo.findNearbyLocation(it.latitude, it.longitude)
                    }

                    val tripFlow = repo.getTripById(currentTripId)
                    tripFlow.collect { trip ->
                        if (trip != null) {
                            // ── Auto-classification logic ──
                            val defaultClassification = prefs.defaultClassificationFlow.first()
                            var autoClassification = defaultClassification
                            var autoTripPurpose: TripPurpose? = null

                            // 1. Work hours check — outside work hours → Personal
                            val workHoursEnabled = prefs.workHoursEnabledFlow.first()
                            if (workHoursEnabled) {
                                val workStart = prefs.workStartHourFlow.first()
                                val workEnd = prefs.workEndHourFlow.first()
                                val workDays = prefs.workDaysFlow.first()

                                val cal = java.util.Calendar.getInstance().apply { timeInMillis = trip.startTime }
                                val dayOfWeek = cal.getDisplayName(
                                    java.util.Calendar.DAY_OF_WEEK,
                                    java.util.Calendar.SHORT,
                                    java.util.Locale.US
                                ) ?: ""
                                val hourMin = String.format(
                                    "%02d:%02d",
                                    cal.get(java.util.Calendar.HOUR_OF_DAY),
                                    cal.get(java.util.Calendar.MINUTE)
                                )

                                val isWorkDay = workDays.split(",").any {
                                    it.trim().equals(dayOfWeek, ignoreCase = true)
                                }
                                val isWorkHour = hourMin >= workStart && hourMin < workEnd

                                if (!isWorkDay || !isWorkHour) {
                                    autoClassification = TripClassification.PERSONAL
                                }
                            }

                            // 2. Saved location classification (overrides work hours)
                            if (endSaved != null && endSaved.defaultClassification != TripClassification.UNCLASSIFIED) {
                                autoClassification = endSaved.defaultClassification
                            } else if (startSaved != null && startSaved.defaultClassification != TripClassification.UNCLASSIFIED) {
                                autoClassification = startSaved.defaultClassification
                            }

                            // 3. FrequentDrive matching (start+end pair — most specific, wins)
                            val matchingDrive = repo.findMatchingFrequentDrive(
                                startLocation?.latitude ?: 0.0,
                                startLocation?.longitude ?: 0.0,
                                lastLocation?.latitude ?: 0.0,
                                lastLocation?.longitude ?: 0.0
                            )
                            if (matchingDrive != null && matchingDrive.defaultPurpose != null) {
                                autoTripPurpose = matchingDrive.defaultPurpose
                                autoClassification = matchingDrive.defaultPurpose.toClassification()
                            }

                            // Only auto-classify if trip is still UNCLASSIFIED (respect manual overrides)
                            val finalClassification = if (trip.classification == TripClassification.UNCLASSIFIED) {
                                autoClassification
                            } else {
                                trip.classification
                            }
                            val finalPurpose = if (trip.classification == TripClassification.UNCLASSIFIED) {
                                autoTripPurpose ?: trip.tripPurpose
                            } else {
                                trip.tripPurpose
                            }

                            val updatedTrip = trip.copy(
                                endTime = now,
                                startLatitude = startLocation?.latitude ?: 0.0,
                                startLongitude = startLocation?.longitude ?: 0.0,
                                endLatitude = lastLocation?.latitude,
                                endLongitude = lastLocation?.longitude,
                                startAddress = startSaved?.name ?: startAddr,
                                endAddress = endSaved?.name ?: endAddr,
                                distanceMiles = distanceMiles,
                                durationMinutes = DistanceCalculator.durationMinutes(
                                    trip.startTime, now
                                ),
                                isActive = false,
                                classification = finalClassification,
                                tripPurpose = finalPurpose
                            )
                            repo.updateTrip(updatedTrip)
                            Log.d(TAG, "Trip $currentTripId finalized: $distanceMiles mi, classification=$finalClassification")

                            // Check unclassified trip count and remind if threshold met
                            val unclassifiedCount = repo.getUnclassifiedTripCount()
                            if (unclassifiedCount >= Constants.UNCLASSIFIED_REMINDER_THRESHOLD) {
                                showUnclassifiedReminder(unclassifiedCount)
                            }
                        }
                        return@collect
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error finalizing trip", e)
            }
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Shows a notification reminding the user to classify pending trips.
     */
    private fun showUnclassifiedReminder(count: Int) {
        val contentIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 2, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, Constants.ALERTS_CHANNEL_ID)
            .setContentTitle("🦉 $count unclassified trips")
            .setContentText("Tap to classify your recent drives")
            .setSmallIcon(R.drawable.ic_owl_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(Constants.UNCLASSIFIED_REMINDER_NOTIFICATION_ID, notification)
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            Constants.LOCATION_UPDATE_INTERVAL_MS
        ).setMinUpdateIntervalMillis(Constants.LOCATION_FASTEST_INTERVAL_MS)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    handleNewLocation(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted", e)
            stopTracking()
        }
    }

    private fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun handleNewLocation(location: Location) {
        if (startLocation == null) {
            startLocation = location
        }

        // Calculate incremental distance
        lastLocation?.let { prev ->
            totalDistanceMeters += prev.distanceTo(location).toDouble()
        }
        lastLocation = location
        pointCount++

        // Save location point
        serviceScope.launch {
            if (currentTripId > 0) {
                val app = applicationContext as MileOwlApp
                val point = LocationPoint(
                    tripId = currentTripId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = location.time,
                    speed = if (location.hasSpeed()) location.speed else null,
                    accuracy = if (location.hasAccuracy()) location.accuracy else null
                )
                app.container.tripRepository.insertLocationPoint(point)
            }
        }

        // Update live distance for UI
        val distanceMiles = totalDistanceMeters / Constants.METERS_PER_MILE
        _currentDistanceMiles.value = distanceMiles

        // Update notification
        val notification = buildNotification(distanceMiles)
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(Constants.TRACKING_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when MileOwl is tracking a trip"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(distanceMiles: Double): Notification {
        val contentIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TripTrackingService::class.java).apply {
            action = Constants.ACTION_STOP_TRACKING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val milesText = String.format("%.1f mi", distanceMiles)

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("🦉 Tracking trip...")
            .setContentText("Distance: $milesText")
            .setSmallIcon(R.drawable.ic_owl_notification)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        _isTracking.value = false
        stopLocationUpdates()
        serviceScope.cancel()
    }
}
