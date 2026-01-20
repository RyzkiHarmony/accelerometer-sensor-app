package com.pemalang.roaddamage.ui.screens

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.pemalang.roaddamage.data.local.TripDao
import com.pemalang.roaddamage.model.Trip
import com.pemalang.roaddamage.model.UploadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.concurrent.TimeUnit

@HiltViewModel
class TripDetailViewModel @Inject constructor(
    private val app: Application,
    private val tripDao: TripDao
) : ViewModel() {
    data class UiState(
        val trip: Trip? = null,
        val points: List<Pair<Double, Double>> = emptyList(),
        val magnitudes: List<Float> = emptyList()
    )
    sealed class Event {
        object Deleted : Event()
        data class Error(val message: String) : Event()
    }
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui
    val events =
        MutableSharedFlow<Event>(replay = 0, extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    suspend fun load(tripId: String) {
        val trip = tripDao.getById(tripId)
        if (trip == null) {
            events.tryEmit(Event.Error("Trip tidak ditemukan"))
            return
        }
        val pts = mutableListOf<Pair<Double, Double>>()
        val mags = mutableListOf<Float>()
        try {
            val file = File(trip.dataFilePath)
            if (file.exists()) {
                BufferedReader(FileReader(file)).use { br ->
                    var line = br.readLine()
                    while (line != null) {
                        if (!line.startsWith("timestamp")) {
                            val parts = line.split(",")
                            if (parts.size >= 7) {
                                val mag = parts[4].toFloatOrNull()
                                val lat = parts[5].toDoubleOrNull()
                                val lon = parts[6].toDoubleOrNull()
                                if (mag != null) mags.add(mag)
                                if (lat != null && lon != null) pts.add(lat to lon)
                            }
                        }
                        line = br.readLine()
                    }
                }
            }
        } catch (t: Throwable) {
            events.tryEmit(Event.Error("Gagal membaca file: ${t.message ?: ""}"))
        }
        _ui.value = UiState(trip = trip, points = pts, magnitudes = mags)
    }

    fun enqueueUpload() {
        val trip = _ui.value.trip ?: return
        if (trip.uploadStatus == UploadStatus.UPLOADED || trip.uploadStatus == UploadStatus.UPLOADING) return
        viewModelScope.launch { tripDao.upsert(trip.copy(uploadStatus = UploadStatus.UPLOADING)) }
        val input = Data.Builder().putString("tripId", trip.tripId).build()
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request =
            OneTimeWorkRequestBuilder<com.pemalang.roaddamage.work.TripUploadWorker>()
                .setInputData(input)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .addTag("upload_trip_${trip.tripId}")
                .build()
        WorkManager.getInstance(app)
            .enqueueUniqueWork("upload_trip_${trip.tripId}", androidx.work.ExistingWorkPolicy.KEEP, request)
    }

    fun deleteTrip() {
        val trip = _ui.value.trip ?: return
        viewModelScope.launch {
            try {
                tripDao.deleteById(trip.tripId)
                try {
                    File(trip.dataFilePath).delete()
                } catch (_: Throwable) {}
                events.tryEmit(Event.Deleted)
            } catch (t: Throwable) {
                events.tryEmit(Event.Error("Gagal menghapus: ${t.message ?: ""}"))
            }
        }
    }
}

