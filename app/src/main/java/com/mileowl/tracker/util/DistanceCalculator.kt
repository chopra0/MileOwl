package com.mileowl.tracker.util

import android.location.Location
import com.mileowl.tracker.data.model.LocationPoint

object DistanceCalculator {

    /**
     * Calculate total distance in miles from a list of location points.
     * Uses sequential pairs to sum up the actual path traveled.
     */
    fun calculateDistanceMiles(points: List<LocationPoint>): Double {
        if (points.size < 2) return 0.0

        var totalMeters = 0.0
        for (i in 0 until points.size - 1) {
            val results = FloatArray(1)
            Location.distanceBetween(
                points[i].latitude, points[i].longitude,
                points[i + 1].latitude, points[i + 1].longitude,
                results
            )
            totalMeters += results[0]
        }
        return totalMeters / Constants.METERS_PER_MILE
    }

    /**
     * Calculate distance between two coordinate pairs in miles.
     */
    fun distanceBetweenMiles(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble() / Constants.METERS_PER_MILE
    }

    /**
     * Calculate duration in minutes between two timestamps.
     */
    fun durationMinutes(startTime: Long, endTime: Long): Int {
        return ((endTime - startTime) / 60_000).toInt()
    }
}
