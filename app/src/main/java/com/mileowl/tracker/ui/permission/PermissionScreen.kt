package com.mileowl.tracker.ui.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.mileowl.tracker.ui.theme.TrackingGreen

@Composable
fun PermissionScreen(
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    // ─── Permission states ──────────────────────────────────────────
    var hasFineLocation by remember {
        mutableStateOf(hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION))
    }
    var hasBackgroundLocation by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                hasPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            else true
        )
    }
    var hasActivityRecognition by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                hasPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
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

    // ─── Auto-fire state ────────────────────────────────────────────
    // Steps: 0 = Location (fg + bg combined), 1 = Activity Recognition, 2 = Notifications, 3 = done
    var currentStep by remember { mutableStateOf(0) }
    var retryTrigger by remember { mutableStateOf(0) }

    // Track denials for permanently-denied detection
    var locationRequested by remember { mutableStateOf(false) }
    var activityRequested by remember { mutableStateOf(false) }
    var notificationRequested by remember { mutableStateOf(false) }

    // Dialog state
    var showDeniedDialog by remember { mutableStateOf<PermissionDeniedInfo?>(null) }
    var showBackgroundLocationGuide by remember { mutableStateOf(false) }

    // Play Services
    val playServicesAvailable = remember {
        ActivityTransitionHelper.isPlayServicesAvailable(context)
    }

    // Location is fully granted only when BOTH foreground AND background are granted
    val locationFullyGranted = hasFineLocation && hasBackgroundLocation
    val corePermissionsGranted = locationFullyGranted && hasActivityRecognition

    // ─── Re-check on resume (returning from Settings) ───────────────
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasFineLocation = hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                hasBackgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    hasPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                else true
                hasActivityRecognition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    hasPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
                else true
                hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                else true
                retryTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ─── Launchers ──────────────────────────────────────────────────

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBackgroundLocation = granted
        if (granted) {
            currentStep = 1
        }
        // If denied on Android 10, user stays on step 0. ON_RESUME will retryTrigger.
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        hasFineLocation = fineGranted
        if (fineGranted) {
            // Foreground granted — now request background ("Allow all the time")
            if (hasBackgroundLocation || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // Both already granted (pre-Q treats fg as sufficient)
                currentStep = 1
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+: show guide dialog before opening location settings
                showBackgroundLocationGuide = true
            } else {
                // Android 10: shows a system dialog directly
                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        } else {
            locationRequested = true
        }
    }

    val activityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasActivityRecognition = granted
        if (granted) {
            currentStep = 2
        } else {
            activityRequested = true
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotification = granted
        // Notifications are optional — move on regardless
        currentStep = 3
    }

    // ─── Auto-fire permissions in sequence ───────────────────────────
    LaunchedEffect(currentStep, retryTrigger) {
        when (currentStep) {
            0 -> {
                if (!hasFineLocation) {
                    // Need foreground location first
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
                } else if (!hasBackgroundLocation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Foreground granted but background not — request it.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        // Android 11+: show guide dialog before opening location settings
                        showBackgroundLocationGuide = true
                    } else {
                        // Android 10: shows a system dialog directly
                        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                } else {
                    // Both foreground and background granted
                    currentStep = 1
                }
            }
            1 -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || hasActivityRecognition) {
                    currentStep = 2
                } else if (isPermanentlyDenied(activity, Manifest.permission.ACTIVITY_RECOGNITION, activityRequested)) {
                    showDeniedDialog = PermissionDeniedInfo(
                        title = "Activity Recognition Required",
                        reason = "Activity Recognition lets MileOwl automatically detect when you start driving, so trips are tracked without manual action.",
                        steps = "1. Tap 'Open Settings' below\n2. Tap 'Permissions'\n3. Tap 'Physical activity'\n4. Toggle it on"
                    )
                } else {
                    activityLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                }
            }
            2 -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || hasNotification) {
                    currentStep = 3
                } else if (isPermanentlyDenied(activity, Manifest.permission.POST_NOTIFICATIONS, notificationRequested)) {
                    // Notifications are optional — skip silently if permanently denied
                    currentStep = 3
                } else {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            // Step 3: all done, nothing to auto-fire
        }
    }

    // ─── UI ─────────────────────────────────────────────────────────
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
            text = "Free mileage tracking. No catch.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "MileOwl needs a few permissions to automatically detect and track your trips.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Play Services error (hard requirement, not a soft warning)
        if (!playServicesAvailable) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Google Play Services required",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Google Play Services is required for automatic trip detection. Please install or update Google Play Services.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // 1. Location (combined foreground + background — only green when both granted)
        PermissionStatusCard(
            title = "Location",
            description = "Allows MileOwl to record routes and track trips at all times.",
            granted = locationFullyGranted,
            isActive = currentStep == 0 && !locationFullyGranted,
            onRetry = { retryTrigger++ }
        )

        // 2. Activity Recognition (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PermissionStatusCard(
                title = "Activity Recognition",
                description = "Detects when you start or stop driving to auto-track trips.",
                granted = hasActivityRecognition,
                isActive = currentStep == 1 && !hasActivityRecognition,
                onRetry = { retryTrigger++ }
            )
        }

        // 3. Notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionStatusCard(
                title = "Notifications",
                description = "Shows tracking status and trip summaries.",
                granted = hasNotification,
                isActive = currentStep == 2 && !hasNotification,
                onRetry = { retryTrigger++ }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Get Started button
        Button(
            onClick = {
                if (hasActivityRecognition && hasFineLocation && playServicesAvailable) {
                    ActivityTransitionHelper.registerTransitions(context)
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
                text = if (corePermissionsGranted) "Get Started" else "Setting up permissions...",
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

    // Guide dialog before opening location settings (Android 11+)
    if (showBackgroundLocationGuide) {
        AlertDialog(
            onDismissRequest = { /* don't dismiss on outside tap */ },
            title = {
                Text(
                    text = "One more step",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "MileOwl needs to track your drives even when the app is closed.\n\n" +
                    "On the next screen, tap \"Allow all the time\" to turn on background tracking."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showBackgroundLocationGuide = false
                    backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }) {
                    Text("Continue")
                }
            }
        )
    }

    // Explanation / Settings redirect dialog
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

// ─── Permission status card (display-only, no Grant button) ─────────

@Composable
private fun PermissionStatusCard(
    title: String,
    description: String,
    granted: Boolean,
    isActive: Boolean,
    onRetry: () -> Unit
) {
    val cardModifier = if (isActive) {
        Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onRetry() }
    } else {
        Modifier.fillMaxWidth()
    }

    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(
            containerColor = when {
                granted -> TrackingGreen.copy(alpha = 0.08f)
                isActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp)
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
                        when {
                            granted -> TrackingGreen
                            isActive -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        }
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
            } else if (isActive) {
                Text(
                    text = "...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
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

private fun openAppSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) { }
}
