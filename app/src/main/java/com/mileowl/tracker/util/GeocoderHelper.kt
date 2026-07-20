package com.mileowl.tracker.util

import android.content.Context
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

object GeocoderHelper {

    /**
     * Reverse geocode latitude/longitude to a readable address string.
     * Returns null on failure.
     */
    suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String? = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val address = addresses.firstOrNull()
                        val result = address?.let { formatAddress(it) }
                        continuation.resume(result)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.firstOrNull()?.let { formatAddress(it) }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun formatAddress(address: android.location.Address): String {
        val parts = mutableListOf<String>()

        // Street number + street name
        address.thoroughfare?.let { street ->
            val number = address.subThoroughfare
            if (number != null) {
                parts.add("$number $street")
            } else {
                parts.add(street)
            }
        }

        // City
        address.locality?.let { parts.add(it) }

        // State abbreviation
        address.adminArea?.let { parts.add(it) }

        return if (parts.isNotEmpty()) parts.joinToString(", ") else {
            address.getAddressLine(0) ?: "Unknown"
        }
    }
}
