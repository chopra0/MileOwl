package com.mileowl.tracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.mileowl.tracker.util.Constants
import com.mileowl.tracker.util.DebugLog

class ActivityTransitionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ActivityTransReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        DebugLog.log(context, "Signal", "Received broadcast from Google", notify = false)

        if (!ActivityTransitionResult.hasResult(intent)) {
            DebugLog.log(context, "Signal", "Broadcast had no transition data")
            return
        }

        val result = ActivityTransitionResult.extractResult(intent) ?: return

        for (event in result.transitionEvents) {
            val activityType = event.activityType
            val transitionType = event.transitionType

            val activityName = when (activityType) {
                DetectedActivity.IN_VEHICLE -> "Driving"
                DetectedActivity.ON_BICYCLE -> "Biking"
                else -> "Activity($activityType)"
            }
            val transitionName = when (transitionType) {
                ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "Started"
                ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "Stopped"
                else -> "Unknown"
            }

            DebugLog.log(context, "Detection", "$transitionName $activityName")

            val isVehicle = activityType == DetectedActivity.IN_VEHICLE
                    || activityType == DetectedActivity.ON_BICYCLE

            if (isVehicle && transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                DebugLog.log(context, "Tracking", "Starting trip recording")
                startTrackingService(context)
            } else if (isVehicle && transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                DebugLog.log(context, "Tracking", "Stopping trip recording")
                stopTrackingService(context)
            }
        }
    }

    private fun startTrackingService(context: Context) {
        val serviceIntent = Intent(context, TripTrackingService::class.java).apply {
            action = Constants.ACTION_START_TRACKING
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    private fun stopTrackingService(context: Context) {
        val serviceIntent = Intent(context, TripTrackingService::class.java).apply {
            action = Constants.ACTION_STOP_TRACKING
        }
        context.startService(serviceIntent)
    }
}
