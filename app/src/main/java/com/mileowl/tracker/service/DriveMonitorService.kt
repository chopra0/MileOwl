package com.mileowl.tracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mileowl.tracker.MainActivity
import com.mileowl.tracker.R
import com.mileowl.tracker.util.Constants

/**
 * Always-on foreground service that keeps MileOwl alive for drive detection.
 *
 * Two detection methods:
 * 1. Google's Activity Transition API (registered on start)
 * 2. Low-power speed monitoring — checks location every 30s and starts
 *    tracking if speed exceeds driving threshold (~15 mph)
 *
 * When either method detects driving, TripTrackingService takes over
 * with high-accuracy GPS tracking.
 */
class DriveMonitorService : Service() {

    companion object {
        private const val TAG = "DriveMonitorService"

        // ~15 mph = ~6.7 m/s — anything above this is likely driving
        private const val DRIVING_SPEED_THRESHOLD_MPS = 6.7f

        // Check location every 30 seconds for speed monitoring
        private const val MONITOR_INTERVAL_MS = 30_000L

        var isRunning = false
            private set

        fun start(context: Context) {
            if (isRunning) return
            val intent = Intent(context, DriveMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, DriveMonitorService::class.java)
            context.stopService(intent)
        }
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var speedCheckCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        startForeground(Constants.MONITOR_NOTIFICATION_ID, buildNotification())

        // Method 1: Register Google's Activity Transition detection
        try {
            ActivityTransitionHelper.registerTransitions(this)
            Log.d(TAG, "Activity transitions registered")
        } catch (e: Exception) {
            Log.w(TAG, "Could not register activity transitions", e)
        }

        // Method 2: Start low-power speed monitoring as backup
        startSpeedMonitoring()

        Log.d(TAG, "Drive monitor started — listening for drives")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        stopSpeedMonitoring()
        Log.d(TAG, "Drive monitor stopped")
    }

    /**
     * Low-power location checks every 30s. If speed > threshold and we're
     * not already tracking, start trip tracking.
     */
    private fun startSpeedMonitoring() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            MONITOR_INTERVAL_MS
        ).setMinUpdateIntervalMillis(MONITOR_INTERVAL_MS / 2)
            .build()

        speedCheckCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return

                // Skip if already tracking
                if (TripTrackingService.isTracking) return

                val speed = if (location.hasSpeed()) location.speed else 0f

                if (speed >= DRIVING_SPEED_THRESHOLD_MPS) {
                    Log.d(TAG, "Speed ${speed} m/s exceeds threshold — starting trip tracking")
                    startTrackingService()
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                speedCheckCallback,
                Looper.getMainLooper()
            )
            Log.d(TAG, "Speed monitoring started (every ${MONITOR_INTERVAL_MS / 1000}s)")
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing location permission for speed monitoring", e)
        }
    }

    private fun stopSpeedMonitoring() {
        if (::speedCheckCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(speedCheckCallback)
        }
    }

    private fun startTrackingService() {
        val serviceIntent = Intent(this, TripTrackingService::class.java).apply {
            action = Constants.ACTION_START_TRACKING
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.MONITOR_CHANNEL_ID,
                Constants.MONITOR_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps MileOwl ready to detect drives"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val contentIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, Constants.MONITOR_CHANNEL_ID)
            .setContentTitle("MileOwl")
            .setContentText("Watching for drives")
            .setSmallIcon(R.drawable.ic_owl_notification)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
