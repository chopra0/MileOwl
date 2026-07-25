package com.mileowl.tracker.ui.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mileowl.tracker.service.ActivityTransitionHelper
import com.mileowl.tracker.ui.theme.Amber500
import com.mileowl.tracker.ui.theme.TrackingGreen

@Composable
fun PermissionScreen(
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permission states
    var hasFineLocation by remember {
        mutableStateOf(hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION))
    }
    var hasActivityRecognition by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                hasPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
            else true
        )
    }
    var hasBackgroundLocation by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                hasPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            else true
        )
    }
    var hasNotification by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            else true
        )
    }
    var hasBatteryExemption by remember {
        mutableStateOf(isBatteryExempt(context))
    }

    // Track whether each permission was requested at least once (to detect "permanently denied")
    var locationRequested by remember { mutableStateOf(false) }
    var activityRequested by remember { mutableStateOf(false) }
    var notificationRequested by remember { mutableStateOf(false) }

    // Dialog state for permanently denied permissions
    var showDeniedDialog by remember { mutableStateOf<PermissionDeniedInfo?>(null) }

    // Google Play Services
    val playServicesAvailable = remember {
        ActivityTransitionHelper.isPlayServicesAvailable(context)
    }

    // Re-check permissions when returning from Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasFineLocation = hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                hasActivityRecognition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    hasPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
                else true
                hasBackgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    hasPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                else true
                hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                else true
                hasBatteryExemption = isBatteryExempt(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Launchers
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        hasFineLocation = granted
        if (!granted) locationRequested = true
    }

    val activityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasActivityRecognition = granted
        if (!granted) activityRequested = true
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBackgroundLocation = granted
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotification = granted
        if (!granted) notificationRequested = true
    }

    val corePermissionsGranted = hasFineLocation && hasActivityRecognition

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Header
        Text(
            text = "\uD83E\uDD89 MileOwl",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Set up permissions",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "MileOwl needs a few permissions to automatically detect and track your trips.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Google Play Services warning
        if (!playServicesAvailable) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Amber500.copy(alpha = 0.15f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Google Play Services not found",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Automatic trip detection requires Google Play Services. You can still track trips manually using the start/stop button.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 1. Location
        PermissionCard(
            title = "Location",
            description = "Records your route to calculate mileage accurately.",
            granted = hasFineLocation,
            onGrant = {
                if (isPermanentlyDenied(activity, Manifest.permission.ACCESS_FINE_LOCATION, locationRequested)) {
                    showDeniedDialog = PermissionDeniedInfo(
                        title = "Location Permission Required",
                        reason = "Location permission is required to record your driving routes and calculate mileage for IRS deductions.",
                        steps = "1. Tap 'Open Settings' below\n2. Tap 'Permissions'\n3. Tap 'Location'\n4. Select 'Allow all the time'"
                    )
                } else {
                    locationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        )

        // 2. Activity Recognition (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PermissionCard(
                title = "Activity Recognition",
                description = "Detects when you start or stop driving to auto-track trips.",
                granted = hasActivityRecognition,
                enabled = hasFineLocation,
                onGrant = {
                    if (isPermanentlyDenied(activity, Manifest.permission.ACTIVITY_RECOGNITION, activityRequested)) {
                        showDeniedDialog = PermissionDeniedInfo(
                            title = "Activity Recognition Required",
                            reason = "Activity Recognition lets MileOwl automatically detect when you start driving, so trips are tracked without manual action.",
                            steps = "1. Tap 'Open Settings' below\n2. Tap 'Permissions'\n3. Tap 'Physical activity'\n4. Toggle it on"
                        )
                    } else {
                        activityLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                    }
                }
            )
        }

        // 3. Background Location (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PermissionCard(
                title = "Background Location",
                description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    "Tracks trips when the app is in the background. Tap Grant, then select \"Allow all the time\"."
                else
                    "Allows trip tracking to continue when the app is in the background.",
                granted = hasBackgroundLocation,
                enabled = hasFineLocation && hasActivityRecognition,
                onGrant = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        // Android 11+: always goes to Settings for background location
                        showDeniedDialog = PermissionDeniedInfo(
                            title = "Background Location Required",
                            reason = "Background location allows MileOwl to keep tracking your trip even when the screen is off or you switch apps.",
                            steps = "1. Tap 'Open Settings' below\n2. Tap 'Permissions'\n3. Tap 'Location'\n4. Select 'Allow all the time'"
                        )
                    } else {
                        backgroundLocationLauncher.launch(
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
                    }
                }
            )
        }

        // 4. Notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionCard(
                title = "Notifications",
                description = "Shows tracking status and trip summaries.",
                granted = hasNotification,
                onGrant = {
                    if (isPermanentlyDenied(activity, Manifest.permission.POST_NOTIFICATIONS, notificationRequested)) {
                        showDeniedDialog = PermissionDeniedInfo(
                            title = "Notifications Permission",
                            reason = "Notifications allow MileOwl to show tracking status and alert you about issues that could affect trip detection.",
                            steps = "1. Tap 'Open Settings' below\n2. Tap 'Notifications'\n3. Toggle 'Allow notifications' on"
                        )
                    } else {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
        }

        // 5. Battery Optimization Exemption
        PermissionCard(
            title = "Unrestricted Battery",
            description = "Prevents Android from killing MileOwl in the background during trips.",
            granted = hasBatteryExemption,
            onGrant = {
                requestBatteryExemption(context)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Get Started button
        Button(
            onClick = {
                // Register activity transitions if we have the permissions
                if (hasActivityRecognition && hasFineLocation && playServicesAvailable) {
                    ActivityTransitionHelper.registerTransitions(context)
                }
                // Request battery exemption if not already granted
                if (!hasBatteryExemption) {
                    requestBatteryExemption(context)
                }
                onSetupComplete()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = corePermissionsGranted,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (corePermissionsGranted) "Get Started" else "Grant permissions above",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (!corePermissionsGranted) {
            Text(
                text = "Location and Activity Recognition are required for trip tracking.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Permanently denied explanation dialog
    showDeniedDialog?.let { info ->
        PermissionDeniedDialog(
            info = info,
            onOpenSettings = {
                openAppSettings(context)
                showDeniedDialog = null
            },
            onDismiss = {
                showDeniedDialog = null
            }
        )
    }
}

// ─── Data class for denied dialog ───────────────────────────────────

private data class PermissionDeniedInfo(
    val title: String,
    val reason: String,
    val steps: String
)

// ─── Permanently denied explanation dialog ──────────────────────────

@Composable
private fun PermissionDeniedDialog(
    info: PermissionDeniedInfo,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = info.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = info.reason,
                    style = MaterialTheme.typography.bodyMedium
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "How to enable:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = info.steps,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}

// ─── Permission card ────────────────────────────────────────────────

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    enabled: Boolean = true,
    onGrant: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (granted) {
                TrackingGreen.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (granted) TrackingGreen
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (granted) {
                Text(
                    text = "\u2713",
                    style = MaterialTheme.typography.titleMedium,
                    color = TrackingGreen,
                    fontWeight = FontWeight.Bold
                )
            } else {
                FilledTonalButton(
                    onClick = onGrant,
                    enabled = enabled
                ) {
                    Text("Grant")
                }
            }
        }
    }
}

// ─── Helpers ────────────────────────────────────────────────────────

private fun hasPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(
        context, permission
    ) == PackageManager.PERMISSION_GRANTED
}

/**
 * Detects permanently denied state: permission not granted, was requested before,
 * and shouldShowRequestPermissionRationale returns false.
 */
private fun isPermanentlyDenied(
    activity: Activity?,
    permission: String,
    wasRequested: Boolean
): Boolean {
    if (activity == null) return false
    if (!wasRequested) return false
    if (hasPermission(activity, permission)) return false
    return !activity.shouldShowRequestPermissionRationale(permission)
}

private fun isBatteryExempt(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun requestBatteryExemption(context: Context) {
    try {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        ).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    } catch (_: Exception) { }
}

private fun openAppSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) { }
}
