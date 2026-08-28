package com.mileowl.tracker.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mileowl.tracker.MainActivity
import com.mileowl.tracker.data.model.TripClassification
import com.mileowl.tracker.data.model.Vehicle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var vehicleToDelete by remember { mutableStateOf<Vehicle?>(null) }

    // File picker for CSV import
    val csvPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importCsv(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // IRS Rate Section
        SectionHeader("IRS Mileage Rate")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                var rateText by remember(state.irsRate) {
                    mutableStateOf(String.format("%.2f", state.irsRate))
                }
                OutlinedTextField(
                    value = rateText,
                    onValueChange = {
                        rateText = it
                        viewModel.setIrsRate(it)
                    },
                    label = { Text("Rate per mile (\$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    prefix = { Text("$") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "2026 IRS standard mileage rate: \$0.70/mile",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Import Data Section
        SectionHeader("Import Data")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Import trip history from MileIQ, QuickBooks, or any CSV file with Date and Miles columns.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (importState.isImporting) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Importing trips…",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            csvPicker.launch(arrayOf(
                                "text/csv",
                                "text/comma-separated-values",
                                "application/csv",
                                "application/vnd.ms-excel",
                                "*/*"
                            ))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import from CSV")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Manage Vehicles Section
        SectionHeader("Manage Vehicles")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (state.vehicles.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No vehicles added yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    state.vehicles.forEachIndexed { index, vehicle ->
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                        VehicleItem(
                            vehicle = vehicle,
                            onSetDefault = { viewModel.setDefaultVehicle(vehicle.id) },
                            onDelete = { vehicleToDelete = vehicle }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showAddVehicleDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Vehicle")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Vehicle Prompt Settings Section
        if (state.vehicles.size >= 2) {
            SectionHeader("Vehicle Selection")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Default vehicle for trips",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = if (state.skipVehiclePrompt && state.defaultVehicleName != null) {
                                    "Using \"${state.defaultVehicleName}\" for all trips"
                                } else {
                                    "Ask which vehicle on every trip"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (state.skipVehiclePrompt) {
                            TextButton(onClick = { viewModel.resetVehiclePrompt() }) {
                                Text("Reset")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Tracking Settings
        SectionHeader("Tracking")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Auto-detection toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-detection",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Automatically detect when you start driving",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.autoDetectionEnabled,
                        onCheckedChange = { viewModel.setAutoDetectionEnabled(it) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // High accuracy toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "High Accuracy GPS",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "More precise but uses more battery",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.highAccuracy,
                        onCheckedChange = { viewModel.setHighAccuracy(it) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Default classification
                Text(
                    text = "Default Trip Classification",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = state.defaultClassification.name.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        TripClassification.entries.forEach { classification ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        classification.name.lowercase()
                                            .replaceFirstChar { it.uppercase() }
                                    )
                                },
                                onClick = {
                                    viewModel.setDefaultClassification(classification)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Work Hours Section
        SectionHeader("Work Hours")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Work hours toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-classify by work hours",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Drives outside work hours → Personal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.workHoursEnabled,
                        onCheckedChange = { viewModel.setWorkHoursEnabled(it) }
                    )
                }

                if (state.workHoursEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Time pickers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Start time
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Start",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            WorkHourTimePicker(
                                value = state.workStartHour,
                                onValueChange = { viewModel.setWorkStartHour(it) }
                            )
                        }
                        // End time
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "End",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            WorkHourTimePicker(
                                value = state.workEndHour,
                                onValueChange = { viewModel.setWorkEndHour(it) }
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Work days chips
                    Text(
                        text = "Work Days",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val allDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    val selectedDays = state.workDays.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        allDays.forEach { day ->
                            val isSelected = selectedDays.any { it.equals(day, ignoreCase = true) }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newDays = if (isSelected) {
                                        selectedDays.filter { !it.equals(day, ignoreCase = true) }
                                    } else {
                                        selectedDays + day
                                    }
                                    viewModel.setWorkDays(newDays.joinToString(","))
                                },
                                label = {
                                    Text(
                                        text = day.take(2),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Battery Optimization Section
        SectionHeader("Battery")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val isExempt = remember {
                    mutableStateOf(MainActivity.isIgnoringBatteryOptimizations(context))
                }
                // Refresh on resume
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            isExempt.value = MainActivity.isIgnoringBatteryOptimizations(context)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Battery Optimization",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (isExempt.value) "Unrestricted — trips will be detected reliably"
                            else "Restricted — trips may be missed in low power mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isExempt.value) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                    if (!isExempt.value) {
                        TextButton(onClick = {
                            try {
                                val intent = android.content.Intent(
                                    android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                ).apply {
                                    data = android.net.Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) { }
                        }) {
                            Text("Fix")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // About section
        // Debug Log Section
        SectionHeader("Debug Log")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                val debugContext = androidx.compose.ui.platform.LocalContext.current
                var showLog by remember { mutableStateOf(false) }
                var logText by remember { mutableStateOf("") }

                OutlinedButton(
                    onClick = {
                        logText = com.mileowl.tracker.util.DebugLog.readLog(debugContext)
                        showLog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Debug Log")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        com.mileowl.tracker.util.DebugLog.clearLog(debugContext)
                        logText = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Log")
                }

                if (showLog && logText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Copy button
                            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                            TextButton(
                                onClick = {
                                    clipboardManager.setText(
                                        androidx.compose.ui.text.AnnotatedString(logText)
                                    )
                                }
                            ) {
                                Text("Copy to clipboard")
                            }
                            Text(
                                text = logText,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Feedback & Support Section
        SectionHeader("Feedback & Support")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                val feedbackContext = androidx.compose.ui.platform.LocalContext.current

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val deviceInfo = "App: MileOwl v2.4.0\n" +
                                "Device: ${Build.MANUFACTURER} ${Build.MODEL}\n" +
                                "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n\n" +
                                "Feedback:\n"
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("feedback@mileowl.app"))
                                putExtra(Intent.EXTRA_SUBJECT, "MileOwl Feedback")
                                putExtra(Intent.EXTRA_TEXT, deviceInfo)
                            }
                            try { feedbackContext.startActivity(intent) } catch (_: Exception) { }
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Send Feedback",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Report bugs or tell us what you think",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("feedback@mileowl.app"))
                                putExtra(Intent.EXTRA_SUBJECT, "MileOwl Feature Request")
                                putExtra(Intent.EXTRA_TEXT, "I'd like to see this feature in MileOwl:\n\n")
                            }
                            try { feedbackContext.startActivity(intent) } catch (_: Exception) { }
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Request a Feature",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Suggest improvements or new features",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader("About")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🦉 MileOwl v2.4.0",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Free, open-source mileage tracker for IRS-compliant business mileage logging.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "© 2026 PSA Imports LLC. All rights reserved.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Add Vehicle Dialog
    if (showAddVehicleDialog) {
        com.mileowl.tracker.ui.common.AddVehicleDialog(
            onDismiss = { showAddVehicleDialog = false },
            onSave = { name, year, make, model ->
                viewModel.addVehicle(name, year, make, model)
                showAddVehicleDialog = false
            }
        )
    }

    // Delete Vehicle Confirmation
    vehicleToDelete?.let { vehicle ->
        AlertDialog(
            onDismissRequest = { vehicleToDelete = null },
            title = { Text("Delete Vehicle?") },
            text = { Text("Remove \"${vehicle.name}\" from your vehicles? Trips using this vehicle will keep their data.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteVehicle(vehicle)
                    vehicleToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { vehicleToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Import Result Dialog
    importState.result?.let { result ->
        AlertDialog(
            onDismissRequest = { viewModel.clearImportResult() },
            title = {
                Text(
                    if (result.imported > 0) "Import Complete"
                    else "Import Failed"
                )
            },
            text = {
                Column {
                    if (result.imported > 0) {
                        Text(
                            text = "✅ ${result.imported} trip${if (result.imported != 1) "s" else ""} imported",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (result.skipped > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${result.skipped} row${if (result.skipped != 1) "s" else ""} skipped",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (result.errors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Issues:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        result.errors.take(5).forEach { error ->
                            Text(
                                text = "• $error",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (result.errors.size > 5) {
                            Text(
                                text = "…and ${result.errors.size - 5} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearImportResult() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun VehicleItem(
    vehicle: Vehicle,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.DirectionsCar,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = vehicle.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (vehicle.isDefault) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "Default",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            val details = listOf(vehicle.year, vehicle.make, vehicle.model)
                .filter { it.isNotBlank() }
                .joinToString(" ")
            if (details.isNotBlank()) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onSetDefault) {
            Icon(
                imageVector = if (vehicle.isDefault) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = if (vehicle.isDefault) "Default vehicle" else "Set as default",
                tint = if (vehicle.isDefault) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete vehicle",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkHourTimePicker(
    value: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val hours = (0..23).map { h -> String.format("%02d:00", h) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            textStyle = MaterialTheme.typography.bodyMedium
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            hours.forEach { hour ->
                DropdownMenuItem(
                    text = { Text(hour) },
                    onClick = {
                        onValueChange(hour)
                        expanded = false
                    }
                )
            }
        }
    }
}
