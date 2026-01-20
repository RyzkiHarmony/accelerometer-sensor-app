package com.pemalang.roaddamage.sensors

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.tasks.await

class GPSHandler(private val app: Application, private val intervalSec: Long) {
    val locations =
            MutableSharedFlow<Location>(
                    replay = 0,
                    extraBufferCapacity = 16,
                    onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
    private val client = LocationServices.getFusedLocationProviderClient(app)
    private var callback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    suspend fun start() {
        val request =
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalSec * 1000)
                        .setWaitForAccurateLocation(false)
                        .setMinUpdateIntervalMillis(intervalSec * 1000)
                        .build()
        callback =
                object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        val loc = result.lastLocation
                        if (loc != null) locations.tryEmit(loc)
                    }
                }
        client.requestLocationUpdates(request, callback!!, app.mainLooper).await()
    }

    fun stop() {
        val c = callback ?: return
        client.removeLocationUpdates(c)
        callback = null
    }
}
