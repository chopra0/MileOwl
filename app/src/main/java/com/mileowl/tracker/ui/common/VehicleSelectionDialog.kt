package com.mileowl.tracker.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.mileowl.tracker.data.model.Vehicle

/**
 * Reusable dialog that appears after a trip is classified (business/personal)
 * when the user has 2+ vehicles. Lets the user pick which vehicle the trip
 * was taken in, with a "Don't ask again" checkbox that sets the chosen
 * vehicle as the permanent default.
 */
@Composable
fun VehicleSelectionDialog(
    vehicles: List<Vehicle>,
    onSelect: (vehicleId: Long, alwaysUseThis: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedVehicleId by remember {
        mutableStateOf(vehicles.firstOrNull { it.isDefault }?.id ?: vehicles.firstOrNull()?.id)
    }
    var alwaysUseThis by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Which vehicle?") },
        text = {
            Column {
                vehicles.forEachIndexed { index, vehicle ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedVehicleId = vehicle.id }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (selectedVehicleId == vehicle.id) {
                                Icons.Filled.CheckCircle
                            } else {
                                Icons.Outlined.Circle
                            },
                            contentDescription = if (selectedVehicleId == vehicle.id) "Selected" else "Not selected",
                            tint = if (selectedVehicleId == vehicle.id) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            Icons.Filled.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = vehicle.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
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
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // "Don't ask again" checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { alwaysUseThis = !alwaysUseThis },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = alwaysUseThis,
                        onCheckedChange = { alwaysUseThis = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Always use this vehicle",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedVehicleId?.let { onSelect(it, alwaysUseThis) }
                },
                enabled = selectedVehicleId != null
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Skip")
            }
        }
    )
}
