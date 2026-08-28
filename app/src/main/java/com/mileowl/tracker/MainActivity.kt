package com.mileowl.tracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.mileowl.tracker.service.ActivityTransitionHelper
import com.mileowl.tracker.service.DriveMonitorService
import com.mileowl.tracker.ui.navigation.MileOwlNavGraph
import com.mileowl.tracker.ui.theme.MileOwlTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MileOwlTheme {
                // Register drive detection when the app is opened
                LaunchedEffect(Unit) {
                    launch {
                        registerTransitionsIfReady()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MileOwlNavGraph()
                }
            }
        }
    }

    private suspend fun registerTransitionsIfReady() {
        val app = applicationContext as MileOwlApp
        val prefs = app.container.preferencesManager
        
        // Check if auto-detection is enabled in preferences
        if (!prefs.autoDetectionEnabledFlow.first()) {
            return
        }

        val hasFineLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasActivityRecognition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (hasFineLocation && hasActivityRecognition) {
            try {
                // Start monitor service — safer to start from Activity context on 14+
                DriveMonitorService.start(this)
                Log.d("MainActivity", "Drive monitor service started")
            } catch (e: Exception) {
                Log.w("MainActivity", "Could not start monitor service", e)
                try {
                    ActivityTransitionHelper.registerTransitions(this)
                } catch (e2: Exception) {
                    Log.e("MainActivity", "Could not register transitions", e2)
                }
            }
        }
    }

    companion object {
        fun isIgnoringBatteryOptimizations(context: Context): Boolean {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return pm.isIgnoringBatteryOptimizations(context.packageName)
        }
    }
}
