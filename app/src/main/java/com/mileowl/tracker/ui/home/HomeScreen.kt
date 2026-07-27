package com.mileowl.tracker.ui.home

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mileowl.tracker.data.model.Trip
import com.mileowl.tracker.data.model.TripClassification
import com.mileowl.tracker.service.TripTrackingService
import com.mileowl.tracker.ui.theme.Amber500
import com.mileowl.tracker.ui.theme.BusinessGreen
import com.mileowl.tracker.ui.theme.PersonalBlue
import com.mileowl.tracker.ui.theme.TrackingGreen
import com.mileowl.tracker.ui.theme.UnclassifiedGray
import com.mileowl.tracker.util.Constants
import com.mileowl.tracker.util.TrackingAlertHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToTrips: () -> Unit = {},
    onNavigateToTripDetail: (Long) -> Unit = {},
    onNavigateToFrequentDrives: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Track issues for banners and notifications
    var issues by remember {
        mutableStateOf(TrackingAlertHelper.checkIssues(context))
    }

    // Re-check on every resume and sync notifications
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                issues = TrackingAlertHelper.checkIssues(context)
                TrackingAlertHelper.syncNotifications(context, issues)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Header
                Text(
                    text = "\uD83E\uDD89 MileOwl",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // ─── Warning banners ────────────────────────────────────

            // Permission issues banner
            if (issues.hasPermissionIssue) {
                item {
                    val reasons = mutableListOf<String>()
                    if (issues.missingLocation) reasons.add("Location")
                    if (issues.missingActivityRecognition) reasons.add("Activity Recognition")
                    if (issues.missingBackgroundLocation) reasons.add("Background Location")

                    WarningBanner(
                        title = "Missing permissions",
                        message = "${reasons.joinToString(", ")} — trip tracking may not work.",
                        actionLabel = "Fix in Settings",
                        onAction = { TrackingAlertHelper.openAppSettings(context) }
                    )
                }
            }

            // GPS disabled banner
            if (issues.gpsDisabled) {
                item {
                    WarningBanner(
                        title = "Location services off",
                        message = "GPS is disabled. MileOwl cannot track trips without it.",
                        actionLabel = "Turn On",
                        onAction = { TrackingAlertHelper.openLocationSettings(context) }
                    )
                }
            }

            // Battery saver banner
            if (issues.batterySaverOn) {
                item {
                    WarningBanner(
                        title = "Battery Saver is on",
                        message = "Automatic trip detection may be delayed or missed.",
                        actionLabel = "Disable",
                        onAction = { TrackingAlertHelper.openBatterySaverSettings(context) }
                    )
                }
            }

            // Battery optimization restricted banner
            if (issues.batteryOptimizationRestricted && !issues.batterySaverOn) {
                item {
                    WarningBanner(
                        title = "Battery restricted",
                        message = "Android may kill MileOwl in the background. Allow unrestricted battery usage.",
                        actionLabel = "Fix",
                        onAction = { TrackingAlertHelper.requestBatteryExemption(context) },
                        isMinor = true
                    )
                }
            }

            // Play Services unavailable banner
            if (issues.playServicesUnavailable) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Amber500.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = Amber500,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Google Play Services required",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Google Play Services is required for automatic trip detection. Please install or update Google Play Services.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ─── Normal content ─────────────────────────────────────

            // Tracking Status Card
            item {
                TrackingStatusCard(isTracking = state.isTracking, currentMiles = state.currentTripMiles)
            }

            // Monthly Stats Card
            item {
                MonthlyStatsCard(
                    businessMiles = state.monthBusinessMiles,
                    personalMiles = state.monthPersonalMiles,
                    totalTrips = state.monthTotalTrips
                )
            }

            // YTD Card
            item {
                YtdCard(
                    businessMiles = state.ytdBusinessMiles,
                    deduction = state.ytdDeduction,
                    irsRate = state.irsRate
                )
            }

            // Frequent Drives Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToFrequentDrives() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Frequent Drives",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Quick-log your common routes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Recent Trips Header
            if (state.recentTrips.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Trips",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onNavigateToTrips() }
                        )
                    }
                }

                items(state.recentTrips) { trip ->
                    RecentTripItem(
                        trip = trip,
                        onClick = { onNavigateToTripDetail(trip.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }

        // FAB for manual trip start/stop
        FloatingActionButton(
            onClick = {
                if (state.isTracking) {
                    stopManualTrip(context)
                } else {
                    startManualTrip(context)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = if (state.isTracking) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }
        ) {
            Icon(
                imageVector = if (state.isTracking) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = if (state.isTracking) "Stop Trip" else "Start Trip",
                tint = Color.White
            )
        }
    }
}

// ─── Warning Banner ─────────────────────────────────────────────────

@Composable
private fun WarningBanner(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    isMinor: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isMinor) {
                Amber500.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = if (isMinor) Amber500 else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isMinor) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isMinor) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    }
                )
            }
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─── Existing composables (unchanged) ───────────────────────────────

@Composable
private fun TrackingStatusCard(isTracking: Boolean, currentMiles: Double = 0.0) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isTracking) {
                TrackingGreen.copy(alpha = 0.12f)
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
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isTracking) TrackingGreen else UnclassifiedGray)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isTracking) "Currently Tracking" else "Not Tracking",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isTracking) {
                        "Recording trip in progress…"
                    } else {
                        "Auto-detection active"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isTracking) {
                Text(
                    text = String.format("%.1f mi", currentMiles),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TrackingGreen
                )
            }
        }
    }
}

@Composable
private fun MonthlyStatsCard(
    businessMiles: Double,
    personalMiles: Double,
    totalTrips: Int
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "This Month",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = String.format("%.1f", businessMiles),
                    label = "Business mi",
                    color = BusinessGreen
                )
                StatItem(
                    value = String.format("%.1f", personalMiles),
                    label = "Personal mi",
                    color = PersonalBlue
                )
                StatItem(
                    value = totalTrips.toString(),
                    label = "Total trips",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun YtdCard(businessMiles: Double, deduction: Double, irsRate: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Year-to-Date",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = String.format("%.1f", businessMiles),
                    label = "Business mi",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                StatItem(
                    value = String.format("$%,.0f", deduction),
                    label = "IRS Deduction",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Rate: \$${String.format("%.2f", irsRate)}/mile",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun RecentTripItem(trip: Trip, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.US)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Classification badge
            val badgeColor = when (trip.classification) {
                TripClassification.BUSINESS -> BusinessGreen
                TripClassification.PERSONAL -> PersonalBlue
                TripClassification.UNCLASSIFIED -> UnclassifiedGray
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = trip.classification.name.first().toString(),
                    color = badgeColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${trip.startAddress ?: "Unknown"} \u2192 ${trip.endAddress ?: "Unknown"}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Text(
                    text = dateFormat.format(Date(trip.startTime)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format("%.1f mi", trip.distanceMiles),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${trip.durationMinutes} min",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun startManualTrip(context: Context) {
    val intent = Intent(context, TripTrackingService::class.java).apply {
        action = Constants.ACTION_START_TRACKING
        putExtra(Constants.EXTRA_IS_MANUAL, true)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun stopManualTrip(context: Context) {
    val intent = Intent(context, TripTrackingService::class.java).apply {
        action = Constants.ACTION_STOP_TRACKING
    }
    context.startService(intent)
}
