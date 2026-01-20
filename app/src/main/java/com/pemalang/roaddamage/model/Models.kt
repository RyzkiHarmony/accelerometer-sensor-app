package com.pemalang.roaddamage.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class SensorReading(
    val timestamp: Long,
    val accelX: Float,
    val accelY: Float,
    val accelZ: Float,
    val magnitude: Float,
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double?,
    val speed: Float?,
    val accuracy: Float?,
    val bearing: Float?
)

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey val tripId: String,
    val userId: String,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val distance: Float,
    val dataFilePath: String,
    val uploadStatus: UploadStatus,
    val createdAt: Long
)

enum class UploadStatus {
    PENDING,
    UPLOADING,
    UPLOADED,
    FAILED
}

