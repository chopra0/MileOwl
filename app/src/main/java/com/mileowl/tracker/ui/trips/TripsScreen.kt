package com.mileowl.tracker.ui.trips

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mileowl.tracker.data.model.Trip
import com.mileowl.tracker.data.model.TripClassification
import com.mileowl.tracker.ui.theme.BusinessGreen
import com.mileowl.tracker.ui.theme.PersonalBlue
import com.mileowl.tracker.ui.theme.UnclassifiedGray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TripsScreen(
    onTripClick: (Long) -> Unit = {},
    viewModel: TripsViewModel = viewModel()
) {
    val trips by viewModel.trips.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedTrips by viewModel.selectedTrips.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${selectedTrips.size} trip${if (selectedTrips.size != 1) "s" else ""}?") },
            text = { Text("This will permanently delete the selected trips and their GPS data. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelected()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = if (isSelectionMode) "${selectedTrips.size} selected" else "Trips",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipItem(
                    label = "All",
                    selected = filter.classification == null,
                    onClick = { viewModel.setFilter(null) }
                )
                FilterChipItem(
                    label = "Business",
                    selected = filter.classification == TripClassification.BUSINESS,
                    onClick = { viewModel.setFilter(TripClassification.BUSINESS) },
                    color = BusinessGreen
                )
                FilterChipItem(
                    label = "Personal",
                    selected = filter.classification == TripClassification.PERSONAL,
                    onClick = { viewModel.setFilter(TripClassification.PERSONAL) },
                    color = PersonalBlue
                )
                FilterChipItem(
                    label = "Unclassified",
                    selected = filter.classification == TripClassification.UNCLASSIFIED,
                    onClick = { viewModel.setFilter(TripClassification.UNCLASSIFIED) },
                    color = UnclassifiedGray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (trips.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🦉",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No trips yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Start driving and MileOwl will track automatically",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    text = "${trips.size} trips",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(
                        items = trips,
                        key = { it.id }
                    ) { trip ->
                        if (isSelectionMode) {
                            SelectableTripItem(
                                trip = trip,
                                isSelected = selectedTrips.contains(trip.id),
                                onClick = { viewModel.toggleSelection(trip.id) }
                            )
                        } else {
                            SwipeableTripItem(
                                trip = trip,
                                onClick = { onTripClick(trip.id) },
                                onLongClick = { viewModel.enterSelectionMode(trip.id) },
                                onClassify = { classification ->
                                    viewModel.classifyTrip(trip, classification)
                                }
                            )
                        }
                    }
                }

                // Bottom action bar in selection mode
                if (isSelectionMode && selectedTrips.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Row 1: Classify buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.bulkClassify(TripClassification.BUSINESS) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BusinessGreen
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Business")
                                }
                                Button(
                                    onClick = { viewModel.bulkClassify(TripClassification.PERSONAL) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PersonalBlue
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Personal")
                                }
                            }
                            // Row 2: Delete button
                            Button(
                                onClick = { showDeleteDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Delete ${selectedTrips.size} Trip${if (selectedTrips.size != 1) "s" else ""}")
                            }
                            // Row 3: Select All / Select All Unclassified / Cancel
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.selectAll() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("All")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.selectAllUnclassified() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Unclassified")
                                }
                                TextButton(
                                    onClick = { viewModel.clearSelection() }
                                ) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SwipeableTripItem(
    trip: Trip,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClassify: (TripClassification) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onClassify(TripClassification.BUSINESS)
                    false // Don't actually dismiss, just classify
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onClassify(TripClassification.PERSONAL)
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> BusinessGreen.copy(alpha = 0.3f)
                    SwipeToDismissBoxValue.EndToStart -> PersonalBlue.copy(alpha = 0.3f)
                    else -> Color.Transparent
                },
                label = "swipeColor"
            )
            val label = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> "→ Business"
                SwipeToDismissBoxValue.EndToStart -> "Personal ←"
                else -> ""
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Text(
                    text = label,
                    fontWeight = FontWeight.Bold,
                    color = when (direction) {
                        SwipeToDismissBoxValue.StartToEnd -> BusinessGreen
                        SwipeToDismissBoxValue.EndToStart -> PersonalBlue
                        else -> Color.Transparent
                    }
                )
            }
        },
        content = {
            TripListItem(
                trip = trip,
                modifier = Modifier.combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
            )
        }
    )
}

@Composable
private fun SelectableTripItem(
    trip: Trip,
    isSelected: Boolean,
    onClick: () -> Unit
) {
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
            // Selection checkbox
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = if (isSelected) "Selected" else "Not selected",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            // Reuse trip info layout
            TripInfoContent(trip = trip, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TripListItem(trip: Trip, modifier: Modifier = Modifier) {
    val dateFormat = SimpleDateFormat("MMM d", Locale.US)
    val timeFormat = SimpleDateFormat("h:mm a", Locale.US)

    val badgeColor = when (trip.classification) {
        TripClassification.BUSINESS -> BusinessGreen
        TripClassification.PERSONAL -> PersonalBlue
        TripClassification.UNCLASSIFIED -> UnclassifiedGray
    }

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Classification badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = trip.classification.name.take(3),
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            TripInfoContent(trip = trip, modifier = Modifier.weight(1f))

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TripInfoContent(trip: Trip, modifier: Modifier = Modifier) {
    val dateFormat = SimpleDateFormat("MMM d", Locale.US)
    val timeFormat = SimpleDateFormat("h:mm a", Locale.US)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Trip info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildString {
                    append(trip.startAddress?.take(25) ?: "Unknown")
                    append(" → ")
                    append(trip.endAddress?.take(25) ?: "Unknown")
                },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = buildString {
                    append(dateFormat.format(Date(trip.startTime)))
                    append(" • ")
                    append(timeFormat.format(Date(trip.startTime)))
                    // Show tripPurpose label if available, else fall back to free-text purpose
                    val purposeLabel = trip.tripPurpose?.label ?: trip.purpose
                    if (purposeLabel != null) {
                        append(" • ")
                        append(purposeLabel)
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Distance and duration
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

@Composable
private fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    color: Color? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = if (color != null && selected) {
            FilterChipDefaults.filterChipColors(
                selectedContainerColor = color.copy(alpha = 0.2f),
                selectedLabelColor = color
            )
        } else {
            FilterChipDefaults.filterChipColors()
        }
    )
}
