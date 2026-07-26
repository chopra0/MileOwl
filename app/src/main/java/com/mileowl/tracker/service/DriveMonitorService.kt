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
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mileowl.tracker.MainActivity
import com.mileowl.tracker.R
import com.mileowl.tracker.util.Constants

/**
 * Lightweight always-on foreground service that keeps MileOwl alive
 * so it can detect drives even when the app is closed.
 *
 * This service does NOT track location. It only keeps the process alive
 * and ensures the activity transition detector stays registered.
 * When driving is detected, TripTrackingService handles the actual tracking.
 */
class DriveMonitorService : Service() {

    companion object {
        private const val TAG = "DriveMonitorService"
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

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        startForeground(Constants.MONITOR_NOTIFICATION_ID, buildNotification())

        // Register drive detection
        try {
            ActivityTransitionHelper.registerTransitions(this)
            Log.d(TAG, "Drive monitor started — listening for drives")
        } catch (e: Exception) {
            Log.w(TAG, "Could not register activity transitions", e)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        Log.d(TAG, "Drive monitor stopped")
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
