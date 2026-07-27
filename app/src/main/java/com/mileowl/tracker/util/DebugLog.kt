package com.mileowl.tracker.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mileowl.tracker.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Simple debug logger — writes timestamped entries to a log file
 * and shows a notification so the user can see what's happening.
 */
object DebugLog {

    private const val LOG_FILE = "mileowl_debug.log"
    private const val CHANNEL_ID = "mileowl_debug"
    private const val CHANNEL_NAME = "MileOwl Debug"
    private const val MAX_LOG_LINES = 500
    private var notificationId = 5000

    /**
     * Log a debug event. Shows a notification and writes to the log file.
     */
    fun log(context: Context, tag: String, message: String, notify: Boolean = true) {
        val timestamp = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(Date())
        val entry = "[$timestamp] $tag: $message"

        // Write to file
        try {
            val file = File(context.filesDir, LOG_FILE)
            file.appendText(entry + "\n")

            // Trim if too long
            val lines = file.readLines()
            if (lines.size > MAX_LOG_LINES) {
                file.writeText(lines.takeLast(MAX_LOG_LINES).joinToString("\n") + "\n")
            }
        } catch (_: Exception) {}

        // Show notification
        if (notify) {
            showNotification(context, tag, message)
        }

        android.util.Log.d("MileOwl.$tag", message)
    }

    /**
     * Read the full debug log as a string.
     */
    fun readLog(context: Context): String {
        return try {
            val file = File(context.filesDir, LOG_FILE)
            if (file.exists()) file.readText() else "No logs yet."
        } catch (_: Exception) {
            "Could not read log."
        }
    }

    /**
     * Clear the debug log.
     */
    fun clearLog(context: Context) {
        try {
            File(context.filesDir, LOG_FILE).delete()
        } catch (_: Exception) {}
    }

    private fun showNotification(context: Context, title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Debug notifications for troubleshooting"
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_owl_notification)
            .setContentTitle("🦉 $title")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId++, notification)
    }
}
