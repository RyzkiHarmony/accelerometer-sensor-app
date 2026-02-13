package com.pemalang.roaddamage.ui.screens

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pemalang.roaddamage.data.local.TripDao
import com.pemalang.roaddamage.data.prefs.UserPrefs
import com.pemalang.roaddamage.recording.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RecordingViewModel
@Inject
constructor(
        private val app: Application,
        private val repo: RecordingRepository,
        private val tripDao: TripDao,
        private val prefs: UserPrefs
) : ViewModel() {
    private val _magnitudes = MutableStateFlow<List<Float>>(emptyList())
    private val _ax = MutableStateFlow<List<Float>>(emptyList())
    private val _ay = MutableStateFlow<List<Float>>(emptyList())
    private val _az = MutableStateFlow<List<Float>>(emptyList())
    private val _points = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val magnitudes: StateFlow<List<Float>> = _magnitudes
    val ax: StateFlow<List<Float>> = _ax
    val ay: StateFlow<List<Float>> = _ay
    val az: StateFlow<List<Float>> = _az
    val points: StateFlow<List<Pair<Double, Double>>> = _points
    val distance: StateFlow<Float> = repo.distanceFlow
    val recording: StateFlow<Boolean> = repo.recordingFlow
    val startTime: StateFlow<Long> = repo.startTimeFlow
    val eventCount: StateFlow<Int> = repo.eventCountFlow
    val cameraTrigger: kotlinx.coroutines.flow.SharedFlow<Float> = repo.cameraTrigger
    private val _gpsActive = MutableStateFlow(false)
    val gpsActive: StateFlow<Boolean> = _gpsActive
    val gpsAccuracy: StateFlow<Float?> = repo.gpsAccuracyFlow
    val totalTrips: StateFlow<Int> =
            tripDao.observeCount().stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val totalDistance: StateFlow<Float?> =
            tripDao.observeTotalDistance().stateIn(viewModelScope, SharingStarted.Lazily, 0f)
    val samplingRate: StateFlow<Int> =
            prefs.samplingRateFlow.stateIn(viewModelScope, SharingStarted.Lazily, 50)
    val sensitivityThreshold: StateFlow<Float> =
            prefs.sensitivityFlow.stateIn(viewModelScope, SharingStarted.Lazily, 2.0f)

    fun saveCameraEvent(path: String, magnitude: Float) {
        viewModelScope.launch { repo.saveCameraEvent(path, magnitude) }
    }

    init {
        viewModelScope.launch {
            // UI Optimization: Batch updates to reduce recomposition frequency.
            // Instead of updating on every sensor event (50-100Hz), we update at ~10Hz.
            val buffer = mutableListOf<com.pemalang.roaddamage.model.SensorReading>()
            var lastUpdate = System.currentTimeMillis()

            repo.readingsFlow.collect { r ->
                buffer.add(r)
                val now = System.currentTimeMillis()
                if (now - lastUpdate >= 22) { // 22ms throttle (approx 45 FPS)
                    if (buffer.isNotEmpty()) {
                        // Update Magnitudes
                        val newMags = buffer.map { it.magnitude }
                        val currentMags = _magnitudes.value
                        _magnitudes.value = (currentMags + newMags).takeLast(200)

                        // Update Ax
                        val newAx = buffer.map { it.accelX }
                        val currentAx = _ax.value
                        _ax.value = (currentAx + newAx).takeLast(200)

                        // Update Ay
                        val newAy = buffer.map { it.accelY }
                        val currentAy = _ay.value
                        _ay.value = (currentAy + newAy).takeLast(200)

                        // Update Az
                        val newAz = buffer.map { it.accelZ }
                        val currentAz = _az.value
                        _az.value = (currentAz + newAz).takeLast(200)

                        buffer.clear()
                    }
                    lastUpdate = now
                }
            }
        }
        viewModelScope.launch {
            repo.pointsFlow.collect { p ->
                val src = _points.value
                val dst = if (src.size >= 300) src.drop(src.size - 299) + p else src + p
                _points.value = dst
            }
        }
        viewModelScope.launch {
            while (true) {
                val last = repo.gpsLastTs.value
                val active = (System.currentTimeMillis() - last) <= 5000
                _gpsActive.value = active
                delay(1000)
            }
        }
    }
}
