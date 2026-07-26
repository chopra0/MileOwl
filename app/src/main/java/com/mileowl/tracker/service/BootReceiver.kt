package com.mileowl.tracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device booted — starting drive monitor service")
            try {
                DriveMonitorService.start(context)
            } catch (e: Exception) {
                // Fallback: register transitions directly
                Log.w(TAG, "Could not start monitor service, registering transitions directly", e)
                try {
                    ActivityTransitionHelper.registerTransitions(context)
                } catch (e2: Exception) {
                    Log.e(TAG, "Could not register activity transitions", e2)
                }
            }
        }
    }
}
