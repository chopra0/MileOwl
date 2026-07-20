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

class ActivityTransitionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ActivityTransReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) {
            Log.d(TAG, "No activity transition result in intent")
            return
        }

        val result = ActivityTransitionResult.extractResult(intent) ?: return

        for (event in result.transitionEvents) {
            val activityType = event.activityType
            val transitionType = event.transitionType

            Log.d(TAG, "Transition: activity=$activityType, transition=$transitionType")

            val isVehicle = activityType == DetectedActivity.IN_VEHICLE
                    || activityType == DetectedActivity.ON_BICYCLE

            if (isVehicle && transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                Log.d(TAG, "Entered vehicle — starting trip tracking")
                startTrackingService(context)
            } else if (isVehicle && transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                Log.d(TAG, "Exited vehicle — stopping trip tracking")
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
