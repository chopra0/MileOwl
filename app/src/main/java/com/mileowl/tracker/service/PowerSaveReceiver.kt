package com.mileowl.tracker.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.mileowl.tracker.R
import com.mileowl.tracker.util.Constants

class PowerSaveReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) return

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (pm.isPowerSaveMode) {
            // Tapping the notification opens Battery Saver settings
            val settingsIntent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, Constants.ALERTS_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Battery Saver is on")
                .setContentText("MileOwl may not detect trips automatically. Tap to disable Battery Saver.")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Battery Saver restricts background activity. MileOwl may not detect trips automatically. Tap to open Battery Saver settings.")
                )
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            nm.notify(Constants.POWER_SAVE_NOTIFICATION_ID, notification)
        } else {
            nm.cancel(Constants.POWER_SAVE_NOTIFICATION_ID)
        }
    }
}
