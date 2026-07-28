package com.mileowl.tracker.ui.tripdetail

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mileowl.tracker.data.model.TripClassification
import com.mileowl.tracker.data.model.TripPurpose
import com.mileowl.tracker.data.model.LocationPoint
import com.mileowl.tracker.data.model.Vehicle
import com.mileowl.tracker.ui.theme.BusinessGreen
import com.mileowl.tracker.ui.theme.PersonalBlue
import com.mileowl.tracker.ui.theme.UnclassifiedGray
import com.mileowl.tracker.ui.common.AddVehicleDialog
import androidx.compose.ui.viewinterop.AndroidView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun purposeIcon(purpose: TripPurpose): ImageVector = when (purpose) {
    TripPurpose.BUSINESS -> Icons.Filled.BusinessCenter
    TripPurpose.BETWEEN_OFFICES -> Icons.Filled.Domain
    TripPurpose.CUSTOMER_VISIT -> Icons.Filled.LocationOn
    TripPurpose.MEETING -> Icons.Filled.Groups
    TripPurpose.ERRAND_SUPPLIES -> Icons.Filled.ShoppingCart
    TripPurpose.MEAL_ENTERTAIN -> Icons.Filled.Restaurant
    TripPurpose.TEMPORARY_SITE -> Icons.Filled.Construction
    TripPurpose.AIRPORT_TRAVEL -> Icons.Filled.Flight
    TripPurpose.DELIVERY -> Icons.Filled.LocalShipping
    TripPurpose.PERSONAL -> Icons.Filled.Home
    TripPurpose.COMMUTE -> Icons.Filled.DirectionsCar
    TripPurpose.MEDICAL -> Icons.Filled.LocalHospital
    TripPurpose.CHARITY -> Icons.Filled.VolunteerActivism
    TripPurpose.MOVING -> Icons.Filled.LocalShipping
    TripPurpose.OTHER -> Icons.Filled.MoreHoriz
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    tripId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TripDetailViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPurposeSheet by remember { mutableStateOf(false) }
    var showSavedSnackbar by remember { mutableStateOf(false) }

    LaunchedEffect(tripId) {
        viewModel.loadTrip(tripId)
    }

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onNavigateBack()
    }

    LaunchedEffect(state.savedAsFrequentDrive) {
        if (state.savedAsFrequentDrive) {
            showSavedSnackbar = true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Trip Details") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        )

        val trip = state.trip
        if (trip == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...")
            }
            return
        }

        val dateFormat = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.US)
        val timeFormat = SimpleDateFormat("h:mm a", Locale.US)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date header
            Text(
                text = dateFormat.format(Date(trip.startTime)),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Route summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        Icons.Filled.Route,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Route: ${String.format("%.1f mi", trip.distanceMiles)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${trip.startAddress ?: "Unknown"} → ${trip.endAddress ?: "Unknown"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Route map
            if (state.locationPoints.size >= 2) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Route Map",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        RouteMapView(
                            locationPoints = state.locationPoints,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    }
                }
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DetailStat(
                    icon = Icons.Filled.Straighten,
                    value = String.format("%.1f mi", trip.distanceMiles),
                    label = "Distance"
                )
                DetailStat(
                    icon = Icons.Filled.Schedule,
                    value = "${trip.durationMinutes} min",
                    label = "Duration"
                )
            }

            // Locations card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = BusinessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Start",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = trip.startAddress ?: "Unknown location",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = timeFormat.format(Date(trip.startTime)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "End",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = trip.endAddress ?: "Unknown location",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            trip.endTime?.let {
                                Text(
                                    text = timeFormat.format(Date(it)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Classification
            Text(
                text = "Classification",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            val classifications = TripClassification.entries.toList()
            val selectedIndex = classifications.indexOf(trip.classification)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                classifications.forEachIndexed { index, classification ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = classifications.size
                        ),
                        onClick = { viewModel.updateClassification(classification) },
                        selected = index == selectedIndex
                    ) {
                        Text(classification.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }

            // Purpose category picker
            Text(
                text = "Trip Purpose",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPurposeSheet = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (trip.tripPurpose != null) {
                        Icon(
                            imageVector = purposeIcon(trip.tripPurpose),
                            contentDescription = null,
                            tint = when (trip.tripPurpose.toClassification()) {
                                TripClassification.BUSINESS -> BusinessGreen
                                TripClassification.PERSONAL -> PersonalBlue
                                else -> UnclassifiedGray
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = trip.tripPurpose.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Text(
                            text = "Select a purpose...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
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

            // Notes field (formerly "Business Purpose")
            var purpose by remember(trip.purpose) { mutableStateOf(trip.purpose ?: "") }
            OutlinedTextField(
                value = purpose,
                onValueChange = {
                    purpose = it
                    viewModel.updatePurpose(it)
                },
                label = { Text("Notes") },
                placeholder = { Text("e.g., Client meeting, Delivery, Supply run") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Client name field
            var clientName by remember(trip.clientName) { mutableStateOf(trip.clientName ?: "") }
            OutlinedTextField(
                value = clientName,
                onValueChange = {
                    clientName = it
                    viewModel.updateClientName(it)
                },
                label = { Text("Client / Destination") },
                placeholder = { Text("e.g., Trinethra Newark, DVW Foods") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Parking & Tolls — side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                var parkingText by remember(trip.parkingCost) {
                    mutableStateOf(
                        if (trip.parkingCost > 0) String.format("%.2f", trip.parkingCost) else ""
                    )
                }
                OutlinedTextField(
                    value = parkingText,
                    onValueChange = {
                        parkingText = it
                        viewModel.updateParkingCost(it)
                    },
                    label = { Text("Parking (\$)") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                var tollsText by remember(trip.tollsCost) {
                    mutableStateOf(
                        if (trip.tollsCost > 0) String.format("%.2f", trip.tollsCost) else ""
                    )
                }
                OutlinedTextField(
                    value = tollsText,
                    onValueChange = {
                        tollsText = it
                        viewModel.updateTollsCost(it)
                    },
                    label = { Text("Tolls (\$)") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Vehicle selector
            Text(
                text = "Vehicle",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            var showAddVehicleDialog by remember { mutableStateOf(false) }

            VehicleDropdown(
                vehicles = state.vehicles,
                selectedVehicleId = trip.vehicleId,
                onVehicleSelected = { viewModel.updateVehicleId(it) },
                onAddVehicle = { showAddVehicleDialog = true }
            )

            if (showAddVehicleDialog) {
                AddVehicleDialog(
                    onDismiss = { showAddVehicleDialog = false },
                    onSave = { name, year, make, model ->
                        viewModel.addVehicle(name, year, make, model)
                        showAddVehicleDialog = false
                    }
                )
            }

            // Save as Frequent Drive
            OutlinedButton(
                onClick = { viewModel.saveAsFrequentDrive() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.savedAsFrequentDrive
            ) {
                Icon(
                    Icons.Filled.BookmarkAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (state.savedAsFrequentDrive) "Saved as Frequent Drive ✓"
                    else "Save as Frequent Drive"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Trip?") },
            text = { Text("This will permanently remove this trip and its location data.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTrip()
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

    // Purpose bottom sheet
    if (showPurposeSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showPurposeSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Select Purpose",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )

                HorizontalDivider()

                TripPurpose.entries.forEach { purposeItem ->
                    val isSelected = state.trip?.tripPurpose == purposeItem
                    val purposeColor = when (purposeItem.toClassification()) {
                        TripClassification.BUSINESS -> BusinessGreen
                        TripClassification.PERSONAL -> PersonalBlue
                        else -> UnclassifiedGray
                    }

                    ListItem(
                        headlineContent = {
                            Text(
                                text = purposeItem.label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = purposeIcon(purposeItem),
                                contentDescription = null,
                                tint = purposeColor,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        trailingContent = {
                            Text(
                                text = purposeItem.toClassification().name.lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = purposeColor
                            )
                        },
                        modifier = Modifier.clickable {
                            viewModel.updateTripPurpose(purposeItem)
                            showPurposeSheet = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehicleDropdown(
    vehicles: List<Vehicle>,
    selectedVehicleId: Long?,
    onVehicleSelected: (Long?) -> Unit,
    onAddVehicle: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedVehicle = vehicles.firstOrNull { it.id == selectedVehicleId }
    val displayText = selectedVehicle?.let {
        buildString {
            append(it.name)
            if (it.year.isNotBlank() || it.make.isNotBlank() || it.model.isNotBlank()) {
                append(" (")
                listOf(it.year, it.make, it.model)
                    .filter { s -> s.isNotBlank() }
                    .joinTo(this, " ")
                append(")")
            }
        }
    } ?: "No vehicle selected"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = displayText,
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
            DropdownMenuItem(
                text = { Text("None") },
                onClick = {
                    onVehicleSelected(null)
                    expanded = false
                }
            )
            vehicles.forEach { vehicle ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(vehicle.name)
                            if (vehicle.year.isNotBlank() || vehicle.make.isNotBlank() || vehicle.model.isNotBlank()) {
                                Text(
                                    text = listOf(vehicle.year, vehicle.make, vehicle.model)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    onClick = {
                        onVehicleSelected(vehicle.id)
                        expanded = false
                    }
                )
            }
            // Divider before Add New Vehicle
            if (vehicles.isNotEmpty()) {
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = {
                    Text(
                        text = "+ Add New Vehicle",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                },
                onClick = {
                    expanded = false
                    onAddVehicle()
                }
            )
        }
    }
}

@Composable
private fun DetailStat(
    icon: ImageVector,
    value: String,
    label: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RouteMapView(
    locationPoints: List<LocationPoint>,
    modifier: Modifier = Modifier
) {
    if (locationPoints.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No route data available",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val coordsJson = locationPoints.joinToString(",") { "[${it.latitude},${it.longitude}]" }
    val startLat = locationPoints.first().latitude
    val startLon = locationPoints.first().longitude
    val endLat = locationPoints.last().latitude
    val endLon = locationPoints.last().longitude

    val html = """
    <!DOCTYPE html>
    <html><head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
      html,body,#map{margin:0;padding:0;width:100%;height:100%}
      .leaflet-control-attribution{font-size:8px!important}
    </style>
    </head><body>
    <div id="map"></div>
    <script>
      var coords = [$coordsJson];
      var map = L.map('map',{zoomControl:false});
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{
        attribution:'© OSM',maxZoom:18
      }).addTo(map);
      var route = L.polyline(coords,{color:'#4285F4',weight:4,opacity:0.9}).addTo(map);
      map.fitBounds(route.getBounds().pad(0.15));
      L.circleMarker([$startLat,$startLon],{radius:6,color:'#34A853',fillColor:'#34A853',fillOpacity:1,weight:2}).addTo(map);
      L.circleMarker([$endLat,$endLon],{radius:6,color:'#EA4335',fillColor:'#EA4335',fillOpacity:1,weight:2}).addTo(map);
    </script>
    </body></html>
    """.trimIndent()

    AndroidView(
        factory = { context ->
            android.webkit.WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        },
        modifier = modifier
    )
}

@Composable
fun RouteMapCanvas(
    locationPoints: List<LocationPoint>,
    modifier: Modifier = Modifier
) {
    if (locationPoints.size < 2) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No route data available",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Calculate bounds
    val minLat = locationPoints.minOf { it.latitude }
    val maxLat = locationPoints.maxOf { it.latitude }
    val minLon = locationPoints.minOf { it.longitude }
    val maxLon = locationPoints.maxOf { it.longitude }

    // Add padding to bounds
    val latRange = (maxLat - minLat).coerceAtLeast(0.0005)
    val lonRange = (maxLon - minLon).coerceAtLeast(0.0005)
    val latPad = latRange * 0.15
    val lonPad = lonRange * 0.15

    val routeColor = Color(0xFF4285F4)
    val startColor = Color(0xFF34A853)
    val endColor = Color(0xFFEA4335)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        fun mapX(lon: Double): Float =
            ((lon - (minLon - lonPad)) / ((maxLon + lonPad) - (minLon - lonPad)) * width).toFloat()
        fun mapY(lat: Double): Float =
            (height - (lat - (minLat - latPad)) / ((maxLat + latPad) - (minLat - latPad)) * height).toFloat()

        // Draw route line
        val path = Path()
        locationPoints.forEachIndexed { index, point ->
            val x = mapX(point.longitude)
            val y = mapY(point.latitude)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path,
            color = routeColor,
            style = Stroke(
                width = 4.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Draw start marker (green circle)
        val startX = mapX(locationPoints.first().longitude)
        val startY = mapY(locationPoints.first().latitude)
        drawCircle(color = startColor, radius = 8.dp.toPx(), center = Offset(startX, startY))
        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(startX, startY))

        // Draw end marker (red circle)
        val endX = mapX(locationPoints.last().longitude)
        val endY = mapY(locationPoints.last().latitude)
        drawCircle(color = endColor, radius = 8.dp.toPx(), center = Offset(endX, endY))
        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(endX, endY))
    }
}
