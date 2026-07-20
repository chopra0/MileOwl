package com.mileowl.tracker.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.mileowl.tracker.data.model.Trip
import com.mileowl.tracker.data.model.TripClassification
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

    /**
     * Generate a CSV file from a list of trips and return the File.
     *
     * @param context Android context for cache directory access
     * @param trips List of trips to include in the report
     * @param irsRate Current IRS standard mileage rate ($/mile)
     * @param vehicleInfo Vehicle description for the report header
     * @param startDate Report period start (epoch millis)
     * @param endDate Report period end (epoch millis)
     * @param vehicleNames Map of vehicle ID to display name for per-trip vehicle column
     */
    fun generateCsv(
        context: Context,
        trips: List<Trip>,
        irsRate: Double,
        vehicleInfo: String,
        startDate: Long,
        endDate: Long,
        vehicleNames: Map<Long, String> = emptyMap()
    ): File {
        val businessTrips = trips.filter { it.classification == TripClassification.BUSINESS }
        val totalBusinessMiles = businessTrips.sumOf { it.distanceMiles }
        val totalDeduction = totalBusinessMiles * irsRate
        val totalParking = trips.sumOf { it.parkingCost }
        val totalTolls = trips.sumOf { it.tollsCost }
        val totalExpenses = totalParking + totalTolls

        val fileName = "MileOwl_Report_${
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }.csv"
        val file = File(context.cacheDir, fileName)

        file.bufferedWriter().use { writer ->
            // Header section
            writer.write("MileOwl Mileage Report")
            writer.newLine()
            if (vehicleInfo.isNotBlank()) {
                writer.write("Vehicle: $vehicleInfo")
                writer.newLine()
            }
            writer.write("Period: ${dateFormat.format(Date(startDate))} - ${dateFormat.format(Date(endDate))}")
            writer.newLine()
            writer.write("IRS Standard Mileage Rate: \$${String.format("%.2f", irsRate)}/mile")
            writer.newLine()
            writer.newLine()

            // Summary
            writer.write("SUMMARY")
            writer.newLine()
            writer.write("Total Business Miles,${String.format("%.1f", totalBusinessMiles)}")
            writer.newLine()
            writer.write("Total IRS Deduction,\$${String.format("%.2f", totalDeduction)}")
            writer.newLine()
            writer.write("Total Trips,${trips.size}")
            writer.newLine()
            writer.write("Business Trips,${businessTrips.size}")
            writer.newLine()
            writer.write("Total Parking,\$${String.format("%.2f", totalParking)}")
            writer.newLine()
            writer.write("Total Tolls,\$${String.format("%.2f", totalTolls)}")
            writer.newLine()
            writer.write("Total Expenses (Parking + Tolls),\$${String.format("%.2f", totalExpenses)}")
            writer.newLine()
            writer.newLine()

            // Column headers
            writer.write("Date,Start Time,End Time,Start Address,End Address,Purpose Category,Business Notes,Client/Destination,Miles,Duration (min),Classification,IRS Rate,Deduction,Parking,Tolls,Vehicle")
            writer.newLine()

            // Data rows
            for (trip in trips) {
                val date = dateFormat.format(Date(trip.startTime))
                val startTime = timeFormat.format(Date(trip.startTime))
                val endTime = trip.endTime?.let { timeFormat.format(Date(it)) } ?: ""
                val startAddr = escapeCSV(trip.startAddress ?: "")
                val endAddr = escapeCSV(trip.endAddress ?: "")
                val purposeCategory = escapeCSV(trip.tripPurpose?.label ?: "")
                val businessNotes = escapeCSV(trip.purpose ?: "")
                val client = escapeCSV(trip.clientName ?: "")
                val miles = String.format("%.1f", trip.distanceMiles)
                val duration = trip.durationMinutes.toString()
                val classification = trip.classification.name
                val rate = if (trip.classification == TripClassification.BUSINESS) {
                    String.format("%.2f", irsRate)
                } else ""
                val deduction = if (trip.classification == TripClassification.BUSINESS) {
                    String.format("%.2f", trip.distanceMiles * irsRate)
                } else ""
                val parking = if (trip.parkingCost > 0.0) {
                    String.format("%.2f", trip.parkingCost)
                } else ""
                val tolls = if (trip.tollsCost > 0.0) {
                    String.format("%.2f", trip.tollsCost)
                } else ""
                val vehicle = escapeCSV(trip.vehicleId?.let { vehicleNames[it] } ?: "")

                writer.write("$date,$startTime,$endTime,$startAddr,$endAddr,$purposeCategory,$businessNotes,$client,$miles,$duration,$classification,$rate,$deduction,$parking,$tolls,$vehicle")
                writer.newLine()
            }
        }

        return file
    }

    /**
     * Create a share intent for the CSV file.
     */
    fun createShareIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "MileOwl Mileage Report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun escapeCSV(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
