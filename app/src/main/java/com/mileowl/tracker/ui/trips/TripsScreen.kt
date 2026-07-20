package com.mileowl.tracker.ui.trips

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@Composable
fun TripsScreen(
    onTripClick: (Long) -> Unit = {},
    viewModel: TripsViewModel = viewModel()
) {
    val trips by viewModel.trips.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Trips",
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = trips,
                    key = { it.id }
                ) { trip ->
                    SwipeableTripItem(
                        trip = trip,
                        onClick = { onTripClick(trip.id) },
                        onClassify = { classification ->
                            viewModel.classifyTrip(trip, classification)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTripItem(
    trip: Trip,
    onClick: () -> Unit,
    onClassify: (TripClassification) -> Unit
) {
    val dismissState = rememberDismissState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                DismissValue.DismissedToEnd -> {
                    onClassify(TripClassification.BUSINESS)
                    false // Don't actually dismiss, just classify
                }
                DismissValue.DismissedToStart -> {
                    onClassify(TripClassification.PERSONAL)
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismiss(
        state = dismissState,
        background = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                when (direction) {
                    DismissDirection.StartToEnd -> BusinessGreen.copy(alpha = 0.3f)
                    DismissDirection.EndToStart -> PersonalBlue.copy(alpha = 0.3f)
                    else -> Color.Transparent
                },
                label = "swipeColor"
            )
            val label = when (direction) {
                DismissDirection.StartToEnd -> "→ Business"
                DismissDirection.EndToStart -> "Personal ←"
                else -> ""
            }
            val alignment = when (direction) {
                DismissDirection.StartToEnd -> Alignment.CenterStart
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
                        DismissDirection.StartToEnd -> BusinessGreen
                        DismissDirection.EndToStart -> PersonalBlue
                        else -> Color.Transparent
                    }
                )
            }
        },
        dismissContent = {
            TripListItem(trip = trip, onClick = onClick)
        }
    )
}

@Composable
private fun TripListItem(trip: Trip, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM d", Locale.US)
    val timeFormat = SimpleDateFormat("h:mm a", Locale.US)

    val badgeColor = when (trip.classification) {
        TripClassification.BUSINESS -> BusinessGreen
        TripClassification.PERSONAL -> PersonalBlue
        TripClassification.UNCLASSIFIED -> UnclassifiedGray
    }

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
                        if (trip.purpose != null) {
                            append(" • ")
                            append(trip.purpose)
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
