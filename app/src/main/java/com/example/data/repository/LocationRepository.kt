package com.example.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.example.data.model.GeoLocationPoint
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class LocationRepository(context: Context) {

    private val appContext = context.applicationContext
    private val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentGpsLocation(): Result<GeoLocationPoint> {
        return try {
            val tokenSource = CancellationTokenSource()
            val location = fusedClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                tokenSource.token
            ).await() ?: fusedClient.lastLocation.await()

            if (location == null) {
                return Result.failure(IllegalStateException("GPS signal unavailable. Enter or search your city manually."))
            }

            val resolved = reverseGeocode(location.latitude, location.longitude)
            Result.success(
                GeoLocationPoint(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    city = resolved.first,
                    stateOrRegion = resolved.second,
                    addressHint = resolved.third,
                    isLiveGps = true
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): Triple<String, String, String> =
        withContext(Dispatchers.IO) {
            try {
                if (!Geocoder.isPresent()) {
                    return@withContext Triple("Local Area", "", formatCoords(lat, lon))
                }
                val geocoder = Geocoder(appContext, Locale.getDefault())
                val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(lat, lon, 1) { list ->
                            cont.resume(list.firstOrNull())
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                }

                val city = address?.locality
                    ?: address?.subAdminArea
                    ?: address?.adminArea
                    ?: "Local Area"
                val state = address?.adminArea ?: ""
                val street = listOfNotNull(address?.thoroughfare, address?.subLocality)
                    .joinToString(", ")
                    .ifBlank { formatCoords(lat, lon) }

                Triple(city, state, street)
            } catch (_: Exception) {
                Triple("Local Area", "", formatCoords(lat, lon))
            }
        }

    private fun formatCoords(lat: Double, lon: Double): String =
        String.format(Locale.US, "%.4f, %.4f", lat, lon)
}
