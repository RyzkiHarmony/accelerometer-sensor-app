package com.pemalang.roaddamage.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pemalang.roaddamage.R
import com.pemalang.roaddamage.data.local.AppDatabase
import com.pemalang.roaddamage.data.local.TripDao
import com.pemalang.roaddamage.data.remote.ApiService
import com.pemalang.roaddamage.model.UploadStatus
import java.io.File
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TripUploadWorker(appContext: Context, params: WorkerParameters) :
        CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val tripId = inputData.getString("tripId") ?: return Result.failure()
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "rdd.db").build()
        val dao: TripDao = db.tripDao()
        val trip = dao.getById(tripId) ?: return Result.failure()
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()
        val retrofit =
                Retrofit.Builder()
                        .baseUrl("https://example.com/")
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
        val api = retrofit.create(ApiService::class.java)
        return try {
            val userIdBody = trip.userId.toRequestBody("text/plain".toMediaType())
            val tripIdBody = trip.tripId.toRequestBody("text/plain".toMediaType())
            val metadataJson =
                    """{"duration":${trip.duration},"distance":${trip.distance},"startTime":${trip.startTime},"endTime":${trip.endTime}}"""
            val metadataBody = metadataJson.toRequestBody("application/json".toMediaType())
            val file = File(trip.dataFilePath)
            val fileBody = file.asRequestBody("text/csv".toMediaType())
            val filePart = MultipartBody.Part.createFormData("file", file.name, fileBody)
            val resp = api.uploadTrip(userIdBody, tripIdBody, metadataBody, filePart)
            val ok = resp.isSuccessful && (resp.body()?.success == true)
            if (ok) {
                dao.upsert(trip.copy(uploadStatus = UploadStatus.UPLOADED))
                notify("Upload Trip", "Berhasil")
                Result.success()
            } else {
                dao.upsert(trip.copy(uploadStatus = UploadStatus.FAILED))
                notify("Upload Trip", "Gagal")
                if (resp.code() in 500..599) Result.retry() else Result.failure()
            }
        } catch (t: Throwable) {
            dao.upsert(trip.copy(uploadStatus = UploadStatus.FAILED))
            notify("Upload Trip", "Gagal: jaringan")
            Result.retry()
        }
    }

    private fun notify(title: String, message: String) {
        val nm =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as
                        NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch =
                    NotificationChannel(
                            "upload_status",
                            "Upload Status",
                            NotificationManager.IMPORTANCE_DEFAULT
                    )
            nm.createNotificationChannel(ch)
        }
        val notif =
                NotificationCompat.Builder(applicationContext, "upload_status")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(message)
                        .build()
        nm.notify(System.currentTimeMillis().toInt(), notif)
    }
}
