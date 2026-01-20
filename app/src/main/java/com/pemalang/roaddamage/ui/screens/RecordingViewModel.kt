package com.pemalang.roaddamage.ui.screens

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pemalang.roaddamage.data.local.TripDao
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
        private val tripDao: TripDao
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
    private val _gpsActive = MutableStateFlow(false)
    val gpsActive: StateFlow<Boolean> = _gpsActive
    val gpsAccuracy: StateFlow<Float?> = repo.gpsAccuracyFlow
    val totalTrips: StateFlow<Int> =
            tripDao.observeCount().stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val totalDistance: StateFlow<Float?> =
            tripDao.observeTotalDistance().stateIn(viewModelScope, SharingStarted.Lazily, 0f)

    init {
        viewModelScope.launch {
            repo.readingsFlow.collect { r ->
                val src = _magnitudes.value
                val dst =
                        if (src.size >= 200) src.drop(src.size - 199) + r.magnitude
                        else src + r.magnitude
                _magnitudes.value = dst
                val xsrc = _ax.value
                val ysrc = _ay.value
                val zsrc = _az.value
                _ax.value =
                        if (xsrc.size >= 200) xsrc.drop(xsrc.size - 199) + r.accelX
                        else xsrc + r.accelX
                _ay.value =
                        if (ysrc.size >= 200) ysrc.drop(ysrc.size - 199) + r.accelY
                        else ysrc + r.accelY
                _az.value =
                        if (zsrc.size >= 200) zsrc.drop(zsrc.size - 199) + r.accelZ
                        else zsrc + r.accelZ
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
