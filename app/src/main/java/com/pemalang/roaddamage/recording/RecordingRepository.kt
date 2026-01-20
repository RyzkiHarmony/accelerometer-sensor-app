package com.pemalang.roaddamage.recording

import android.app.Application
import com.pemalang.roaddamage.data.local.TripDao
import com.pemalang.roaddamage.model.SensorReading
import com.pemalang.roaddamage.model.Trip
import com.pemalang.roaddamage.model.UploadStatus
import com.pemalang.roaddamage.util.Distance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepository @Inject constructor(
    private val app: Application,
    private val tripDao: TripDao
) {
    private var currentTrip: Trip? = null
    private var writer: BufferedWriter? = null
    private var lastLat: Double? = null
    private var lastLon: Double? = null
    private var totalDistance: Float = 0f
    private val _readings = MutableSharedFlow<SensorReading>(replay = 0, extraBufferCapacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _points = MutableSharedFlow<Pair<Double, Double>>(replay = 0, extraBufferCapacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _distance = MutableStateFlow(0f)
    private val _recording = MutableStateFlow(false)
    private val _startTime = MutableStateFlow(0L)
    private val _gpsLastTs = MutableStateFlow(0L)
    private val _gpsAccuracy = MutableStateFlow<Float?>(null)
    val readingsFlow: MutableSharedFlow<SensorReading> = _readings
    val pointsFlow: MutableSharedFlow<Pair<Double, Double>> = _points
    val distanceFlow: StateFlow<Float> = _distance
    val recordingFlow: StateFlow<Boolean> = _recording
    val startTimeFlow: StateFlow<Long> = _startTime
    val gpsLastTs: StateFlow<Long> = _gpsLastTs
    val gpsAccuracyFlow: StateFlow<Float?> = _gpsAccuracy

    suspend fun startTrip(userId: String): Trip {
        val tripId = UUID.randomUUID().toString()
        val file = File(app.getExternalFilesDir(null), "$tripId.csv")
        val now = System.currentTimeMillis()
        val trip = Trip(
            tripId = tripId,
            userId = userId,
            startTime = now,
            endTime = 0,
            duration = 0,
            distance = 0f,
            dataFilePath = file.absolutePath,
            uploadStatus = UploadStatus.PENDING,
            createdAt = now
        )
        withContext(Dispatchers.IO) {
            writer = BufferedWriter(FileWriter(file, true))
            writer?.write("timestamp,ax,ay,az,magnitude,lat,lon,alt,speed,accuracy,bearing\n")
            writer?.flush()
        }
        currentTrip = trip
        lastLat = null
        lastLon = null
        totalDistance = 0f
        _distance.value = 0f
        _recording.value = true
        _startTime.value = now
        tripDao.upsert(trip)
        return trip
    }

    suspend fun appendReading(reading: SensorReading) {
        withContext(Dispatchers.IO) {
            writer?.apply {
                write(
                    "${reading.timestamp},${reading.accelX},${reading.accelY},${reading.accelZ},${reading.magnitude}," +
                            "${reading.latitude ?: ""},${reading.longitude ?: ""},${reading.altitude ?: ""}," +
                            "${reading.speed ?: ""},${reading.accuracy ?: ""},${reading.bearing ?: ""}\n"
                )
            }
        }
        _readings.tryEmit(reading)
        val lat = reading.latitude
        val lon = reading.longitude
        if (lat != null && lon != null) {
            val prevLat = lastLat
            val prevLon = lastLon
            if (prevLat != null && prevLon != null) {
                totalDistance += Distance.haversine(prevLat, prevLon, lat, lon)
            }
            lastLat = lat
            lastLon = lon
            _points.tryEmit(lat to lon)
            _distance.value = totalDistance
            _gpsLastTs.value = System.currentTimeMillis()
            _gpsAccuracy.value = reading.accuracy
        }
    }

    suspend fun finishTrip() {
        withContext(Dispatchers.IO) {
            writer?.flush()
            writer?.close()
            writer = null
        }
        val now = System.currentTimeMillis()
        val trip = currentTrip ?: return
        val duration = ((now - trip.startTime) / 1000)
        val updated = trip.copy(endTime = now, duration = duration, distance = totalDistance)
        tripDao.upsert(updated)
        currentTrip = null
        _recording.value = false
    }
}
