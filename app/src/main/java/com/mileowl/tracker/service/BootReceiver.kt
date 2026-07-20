package com.mileowl.tracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mileowl.tracker.MileOwlApp

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device booted — re-registering activity transitions")
            val app = context.applicationContext as? MileOwlApp
            if (app != null) {
                val autoDetectionEnabled = true // Default to true; async pref read not available here
                if (autoDetectionEnabled) {
                    ActivityTransitionHelper.registerTransitions(context)
                }
            }
        }
    }
}
