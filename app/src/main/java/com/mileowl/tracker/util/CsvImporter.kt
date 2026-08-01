package com.mileowl.tracker.util

import android.content.Context
import android.net.Uri
import com.mileowl.tracker.data.model.Trip
import com.mileowl.tracker.data.model.TripClassification
import com.mileowl.tracker.data.model.TripPurpose
import com.mileowl.tracker.data.repository.TripRepository
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val errors: List<String>
)

/**
 * Imports mileage trip data from a CSV file (MileIQ, QuickBooks, or generic format).
 *
 * Supports flexible column name matching so exports from MileIQ, QuickBooks Self-Employed,
 * Everlance, Hurdlr, or manual spreadsheets all work without modification.
 */
class CsvImporter(
    private val context: Context,
    private val repository: TripRepository
) {

    // Column name aliases — maps canonical field to possible CSV header names
    private val dateAliases = setOf(
        "date", "start_date", "start_date*", "trip date", "trip_date", "startdate"
    )
    private val endDateAliases = setOf(
        "end_date", "end_date*", "enddate", "end date"
    )
    private val startAddressAliases = setOf(
        "start", "start*", "from", "start location", "start_location",
        "starting location", "start address", "start_address", "origin",
        "starting_location", "departure"
    )
    private val endAddressAliases = setOf(
        "stop", "stop*", "end", "to", "end location", "end_location",
        "destination", "end address", "end_address", "ending location",
        "ending_location", "arrival"
    )
    private val milesAliases = setOf(
        "miles", "miles*", "distance", "total miles", "total_miles",
        "mileage", "dist", "trip distance", "trip_distance"
    )
    private val categoryAliases = setOf(
        "category", "category*", "type", "trip type", "trip_type",
        "classification"
    )
    private val purposeAliases = setOf(
        "purpose", "trip purpose", "trip_purpose", "description", "reason"
    )
    private val vehicleAliases = setOf(
        "vehicle", "car", "vehicle name", "vehicle_name"
    )
    private val parkingAliases = setOf(
        "parking", "parking cost", "parking_cost"
    )
    private val tollsAliases = setOf(
        "tolls", "toll", "tolls cost", "tolls_cost"
    )
    private val notesAliases = setOf(
        "notes", "note", "memo", "comments"
    )

    suspend fun importFromUri(uri: Uri): ImportResult {
        val errors = mutableListOf<String>()
        var imported = 0
        var skipped = 0

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return ImportResult(0, 0, listOf("Could not open file"))

            val reader = BufferedReader(InputStreamReader(inputStream))

            // Read header line
            val headerLine = reader.readLine()
                ?: return ImportResult(0, 0, listOf("File is empty"))

            val headers = parseCsvLine(headerLine).map { it.trim().lowercase() }

            // Map columns
            val dateCol = findColumn(headers, dateAliases)
            val endDateCol = findColumn(headers, endDateAliases)
            val startCol = findColumn(headers, startAddressAliases)
            val endCol = findColumn(headers, endAddressAliases)
            val milesCol = findColumn(headers, milesAliases)
            val categoryCol = findColumn(headers, categoryAliases)
            val purposeCol = findColumn(headers, purposeAliases)
            val vehicleCol = findColumn(headers, vehicleAliases)
            val parkingCol = findColumn(headers, parkingAliases)
            val tollsCol = findColumn(headers, tollsAliases)
            val notesCol = findColumn(headers, notesAliases)

            // Date and miles are required
            if (dateCol == -1) {
                return ImportResult(0, 0, listOf("Missing required column: Date. Found headers: ${headers.joinToString(", ")}"))
            }
            if (milesCol == -1) {
                return ImportResult(0, 0, listOf("Missing required column: Miles/Distance. Found headers: ${headers.joinToString(", ")}"))
            }

            // Parse each row
            var lineNum = 1
            var line = reader.readLine()
            while (line != null) {
                lineNum++
                if (line.isBlank()) {
                    line = reader.readLine()
                    continue
                }

                try {
                    val fields = parseCsvLine(line)

                    // Parse date
                    val dateStr = fields.getOrNull(dateCol)?.trim() ?: ""
                    if (dateStr.isBlank()) {
                        skipped++
                        line = reader.readLine()
                        continue
                    }
                    val startTime = parseDate(dateStr)
                    if (startTime == null) {
                        errors.add("Row $lineNum: Could not parse date '$dateStr'")
                        skipped++
                        line = reader.readLine()
                        continue
                    }

                    // Parse end date (or use start date + 30 min default)
                    val endDateStr = if (endDateCol >= 0) fields.getOrNull(endDateCol)?.trim() else null
                    val endTime = if (!endDateStr.isNullOrBlank()) {
                        parseDate(endDateStr) ?: (startTime + 30 * 60 * 1000L)
                    } else {
                        startTime + 30 * 60 * 1000L // default 30 min duration
                    }

                    // Parse miles
                    val milesStr = fields.getOrNull(milesCol)?.trim()?.replace(",", "") ?: "0"
                    val miles = milesStr.toDoubleOrNull() ?: 0.0
                    if (miles <= 0.0) {
                        skipped++
                        line = reader.readLine()
                        continue
                    }

                    // Parse optional fields
                    val startAddress = if (startCol >= 0) fields.getOrNull(startCol)?.trim() else null
                    val endAddress = if (endCol >= 0) fields.getOrNull(endCol)?.trim() else null
                    val categoryStr = if (categoryCol >= 0) fields.getOrNull(categoryCol)?.trim() else null
                    val purposeStr = if (purposeCol >= 0) fields.getOrNull(purposeCol)?.trim() else null
                    val parking = if (parkingCol >= 0) fields.getOrNull(parkingCol)?.trim()?.toDoubleOrNull() ?: 0.0 else 0.0
                    val tolls = if (tollsCol >= 0) fields.getOrNull(tollsCol)?.trim()?.toDoubleOrNull() ?: 0.0 else 0.0
                    val notes = if (notesCol >= 0) fields.getOrNull(notesCol)?.trim() else null

                    // Map category
                    val classification = mapClassification(categoryStr)

                    // Map purpose
                    val tripPurpose = mapPurpose(purposeStr, classification)

                    // Estimate duration from miles (assume avg 30 mph)
                    val durationMinutes = ((miles / 30.0) * 60.0).toInt().coerceAtLeast(1)
                    val calculatedEndTime = startTime + (durationMinutes * 60 * 1000L)

                    val trip = Trip(
                        startTime = startTime,
                        endTime = calculatedEndTime,
                        startAddress = startAddress?.takeIf { it.isNotBlank() },
                        endAddress = endAddress?.takeIf { it.isNotBlank() },
                        distanceMiles = miles,
                        durationMinutes = durationMinutes,
                        classification = classification,
                        purpose = notes?.takeIf { it.isNotBlank() } ?: purposeStr?.takeIf { it.isNotBlank() },
                        tripPurpose = tripPurpose,
                        parkingCost = parking,
                        tollsCost = tolls,
                        isActive = false
                    )

                    repository.insertTrip(trip)
                    imported++

                } catch (e: Exception) {
                    errors.add("Row $lineNum: ${e.message ?: "Unknown error"}")
                    skipped++
                }

                line = reader.readLine()
            }

            reader.close()
            inputStream.close()

        } catch (e: Exception) {
            errors.add("File error: ${e.message ?: "Unknown error"}")
        }

        return ImportResult(imported, skipped, errors)
    }

    private fun findColumn(headers: List<String>, aliases: Set<String>): Int {
        return headers.indexOfFirst { it in aliases }
    }

    /**
     * Parse a CSV line, handling quoted fields with commas inside them.
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        return result
    }

    /**
     * Try multiple date formats commonly found in MileIQ and other mileage exports.
     */
    private fun parseDate(dateStr: String): Long? {
        val formats = listOf(
            "MM/dd/yyyy HH:mm",
            "MM/dd/yyyy hh:mm a",
            "MM/dd/yyyy",
            "M/d/yyyy",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "MM-dd-yyyy",
            "M-d-yyyy",
            "MMM d, yyyy",
            "MMMM d, yyyy",
            "yyyy/MM/dd"
        )

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                sdf.isLenient = false
                sdf.timeZone = TimeZone.getDefault()
                val date = sdf.parse(dateStr)
                if (date != null) return date.time
            } catch (_: Exception) {
                // Try next format
            }
        }
        return null
    }

    private fun mapClassification(category: String?): TripClassification {
        if (category.isNullOrBlank()) return TripClassification.UNCLASSIFIED
        return when (category.lowercase().trim()) {
            "business", "work" -> TripClassification.BUSINESS
            "personal", "private" -> TripClassification.PERSONAL
            "medical", "medical/moving" -> TripClassification.BUSINESS  // IRS deductible
            "charity", "charitable" -> TripClassification.BUSINESS  // IRS deductible
            "moving" -> TripClassification.BUSINESS  // IRS deductible
            else -> TripClassification.UNCLASSIFIED
        }
    }

    private fun mapPurpose(purpose: String?, classification: TripClassification): TripPurpose? {
        if (purpose.isNullOrBlank()) return null
        val lower = purpose.lowercase().trim()
        return when {
            lower.contains("customer") || lower.contains("client") -> TripPurpose.CUSTOMER_VISIT
            lower.contains("meeting") -> TripPurpose.MEETING
            lower.contains("office") || lower.contains("between") -> TripPurpose.BETWEEN_OFFICES
            lower.contains("errand") || lower.contains("supplies") || lower.contains("shopping") -> TripPurpose.ERRAND_SUPPLIES
            lower.contains("meal") || lower.contains("entertain") || lower.contains("lunch") ||
                lower.contains("dinner") -> TripPurpose.MEAL_ENTERTAIN
            lower.contains("airport") || lower.contains("travel") || lower.contains("flight") -> TripPurpose.AIRPORT_TRAVEL
            lower.contains("delivery") || lower.contains("drop") || lower.contains("pickup") -> TripPurpose.DELIVERY
            lower.contains("site") || lower.contains("construction") || lower.contains("temporary") -> TripPurpose.TEMPORARY_SITE
            lower.contains("commute") -> TripPurpose.COMMUTE
            lower.contains("medical") || lower.contains("doctor") || lower.contains("hospital") -> TripPurpose.MEDICAL
            lower.contains("charity") || lower.contains("volunteer") -> TripPurpose.CHARITY
            lower.contains("moving") || lower.contains("move") -> TripPurpose.MOVING
            lower.contains("personal") || lower.contains("home") -> TripPurpose.PERSONAL
            classification == TripClassification.BUSINESS -> TripPurpose.BUSINESS
            classification == TripClassification.PERSONAL -> TripPurpose.PERSONAL
            else -> null
        }
    }
}
