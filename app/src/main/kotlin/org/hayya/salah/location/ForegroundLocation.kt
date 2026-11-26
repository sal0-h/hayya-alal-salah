// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.*
import android.os.*
import kotlinx.coroutines.*
import kotlin.coroutines.resume

object ForegroundLocation {
    fun allowed(context: Context) = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    @SuppressLint("MissingPermission") // Rechecked here, and revocation remains caught per provider.
    suspend fun get(context: Context): Location? {
        if (!allowed(context)) return null
        val manager = context.getSystemService(LocationManager::class.java)
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        val cached = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .filter { ageMillis(it) in 0..86_400_000L }.minByOrNull { ageMillis(it) }
        if (cached != null && ageMillis(cached) < 120_000) return cached
        val fresh = withTimeoutOrNull(12_000) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (continuation.isActive) continuation.resume(location)
                        manager.removeUpdates(this)
                    }
                    @Deprecated("Legacy provider callback") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) = Unit
                }
                continuation.invokeOnCancellation { manager.removeUpdates(listener) }
                var requested = false
                for (provider in providers) {
                    try { manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper()); requested = true }
                    catch (_: SecurityException) { /* Approximate-only grants may exclude GPS. */ }
                    catch (_: IllegalArgumentException) { /* Provider may disappear during the bounded request. */ }
                }
                if (!requested && continuation.isActive) continuation.resume(null)
            }
        }
        return fresh ?: cached
    }
    fun ageMillis(location: Location) = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000
}
