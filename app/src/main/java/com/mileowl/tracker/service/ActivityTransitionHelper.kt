package com.mileowl.tracker.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.mileowl.tracker.util.Constants

object ActivityTransitionHelper {

    private const val TAG = "ActivityTransitionHelper"

    /**
     * Build the list of activity transitions we care about:
     * - ENTER vehicle (start tracking)
     * - EXIT vehicle (stop tracking)
     */
    private fun buildTransitionList(): List<ActivityTransition> {
        return listOf(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )
    }

    /**
     * Register for activity transition updates.
     */
    fun registerTransitions(context: Context) {
        val request = ActivityTransitionRequest(buildTransitionList())
        val intent = Intent(context, ActivityTransitionReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.ACTIVITY_TRANSITION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        try {
            val task = ActivityRecognition.getClient(context)
                .requestActivityTransitionUpdates(request, pendingIntent)
            task.addOnSuccessListener {
                Log.d(TAG, "Activity transition updates registered successfully")
            }
            task.addOnFailureListener { e ->
                Log.e(TAG, "Failed to register activity transitions", e)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing ACTIVITY_RECOGNITION permission", e)
        }
    }

    /**
     * Unregister from activity transition updates.
     */
    fun unregisterTransitions(context: Context) {
        val intent = Intent(context, ActivityTransitionReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.ACTIVITY_TRANSITION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        try {
            ActivityRecognition.getClient(context)
                .removeActivityTransitionUpdates(pendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Activity transition updates unregistered")
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing permission to unregister transitions", e)
        }
    }
}
