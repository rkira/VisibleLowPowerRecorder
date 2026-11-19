package com.example.visiblelowpowerrecorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.Recording
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor

class RecordingService : LifecycleService() {
    private var recording: Recording? = null
    private val channelId = "visible_recording"

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(7, notification("Preparing camera…"))
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HD))
                    .build()
                val capture = VideoCapture.withOutput(recorder)
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, capture)
                val name = "VisibleRecorder_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".mp4"
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, name)
                    put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= 29) put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, "Movies/VisibleRecorder")
                }
                val output = MediaStoreOutputOptions.Builder(contentResolver, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                    .setContentValues(values).build()
                var pending = capture.output.prepareRecording(this, output)
                if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    pending = pending.withAudioEnabled()
                }
                recording = pending.start(mainExecutor) { event ->
                    if (event is VideoRecordEvent.Start) {
                        getSystemService(NotificationManager::class.java).notify(7, notification("Recording is active"))
                    } else if (event is VideoRecordEvent.Finalize) {
                        recording = null
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            } catch (e: Exception) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }, mainExecutor)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(channelId, "Visible recording", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notification(message: String): Notification {
        val stopIntent = PendingIntent.getService(this, 9, Intent(this, StopRecordingService::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentTitle("Visible Recorder")
            .setContentText(message)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .build()
    }

    override fun onDestroy() {
        recording?.stop()
        recording = null
        super.onDestroy()
    }
}
