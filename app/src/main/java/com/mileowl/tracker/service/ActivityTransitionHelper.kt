package com.mileowl.tracker.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.mileowl.tracker.util.Constants
import com.mileowl.tracker.util.DebugLog

object ActivityTransitionHelper {

    private const val TAG = "ActivityTransitionHelper"

    /**
     * Check whether Google Play Services is available on this device.
     */
    fun isPlayServicesAvailable(context: Context): Boolean {
        return try {
            val result = GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context)
            result == ConnectionResult.SUCCESS
        } catch (e: Exception) {
            Log.w(TAG, "Could not check Play Services availability", e)
            false
        }
    }

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
     * Returns true if registration was attempted, false if Play Services is unavailable.
     */
    fun registerTransitions(context: Context): Boolean {
        if (!isPlayServicesAvailable(context)) {
            Log.w(TAG, "Google Play Services not available — skipping activity transition registration")
            return false
        }

        val request = ActivityTransitionRequest(buildTransitionList())
        val intent = Intent(context, ActivityTransitionReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Constants.ACTIVITY_TRANSITION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        return try {
            val task = ActivityRecognition.getClient(context)
                .requestActivityTransitionUpdates(request, pendingIntent)
            task.addOnSuccessListener {
                DebugLog.log(context, "Registration", "✅ Signed up for drive detection")
            }
            task.addOnFailureListener { e ->
                DebugLog.log(context, "Registration", "❌ Failed: ${e.message}")
            }
            true
        } catch (e: SecurityException) {
            DebugLog.log(context, "Registration", "❌ Missing permission: ${e.message}")
            false
        } catch (e: Exception) {
            DebugLog.log(context, "Registration", "❌ Error: ${e.message}")
            false
        }
    }

    /**
     * Unregister from activity transition updates.
     */
    fun unregisterTransitions(context: Context) {
        if (!isPlayServicesAvailable(context)) return

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
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error unregistering activity transitions", e)
        }
    }
}
