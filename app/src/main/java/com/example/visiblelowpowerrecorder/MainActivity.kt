package com.example.visiblelowpowerrecorder

import android.Manifest
import androidx.activity.ComponentActivity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var statusText: TextView
    private lateinit var timerText: TextView
    private lateinit var qualitySpinner: android.widget.Spinner
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: androidx.camera.video.Recording? = null

    private var recordingStartedAt = 0L

    private val timerHandler = Handler(Looper.getMainLooper())

    private val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private val requestCode = 42

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (recording != null) {
                val elapsed =
                    (System.currentTimeMillis() - recordingStartedAt) / 1000

                val minutes = elapsed / 60
                val seconds = elapsed % 60

                timerText.text = String.format(
                    Locale.US,
                    "REC  %02d:%02d",
                    minutes,
                    seconds
                )

                timerHandler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createUI()

        if (hasPermissions()) {
            startCamera()
        } else {
            requestPermissions(permissions, requestCode)
        }
    }

    private fun createUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        previewView = PreviewView(this).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }

        root.addView(
            previewView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        statusText = TextView(this).apply {
            text = "Camera ready"
            textSize = 18f
            setPadding(0, 12, 0, 8)
        }

        timerText = TextView(this).apply {
            text = "00:00"
            textSize = 24f
            setPadding(0, 8, 0, 8)
        }

        root.addView(statusText)
        root.addView(timerText)

        qualitySpinner = android.widget.Spinner(this)

        val qualityOptions = arrayOf(
            "720p HD",
            "1080p Full HD"
        )

        qualitySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            qualityOptions
        )

        root.addView(qualitySpinner)

        startButton = Button(this).apply {
            text = "START RECORDING"
            setOnClickListener {
                startRecording()
            }
        }

        stopButton = Button(this).apply {
            text = "STOP RECORDING"
            isEnabled = false
            setOnClickListener {
                stopRecording()
            }
        }

        root.addView(startButton)
        root.addView(stopButton)

        setContentView(root)
    }

    private fun hasPermissions(): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(
                this,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)

        providerFuture.addListener({
            try {
                cameraProvider = providerFuture.get()
                bindCamera()
            } catch (e: Exception) {
                statusText.text = "Camera initialization failed"

                Toast.makeText(
                    this,
                    e.message ?: "Camera error",
                    Toast.LENGTH_LONG
                ).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCamera() {
        val provider = cameraProvider ?: return

        val selectedQuality = getSelectedQuality()

        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(
                    selectedQuality,
                    QualitySelector.FallbackStrategy.lowerQualityOrHigherThan(
                        selectedQuality
                    )
                )
            )
            .build()

        videoCapture = VideoCapture.withOutput(recorder)

        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        val capture = videoCapture ?: return

        try {
            provider.unbindAll()

            provider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                capture
            )

            statusText.text = "Camera ready"

        } catch (e: Exception) {
            statusText.text = "Unable to open camera"

            Toast.makeText(
                this,
                e.message ?: "Camera binding failed",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun getSelectedQuality(): Quality {
        return when (qualitySpinner.selectedItemPosition) {
            1 -> Quality.FHD
            else -> Quality.HD
        }
    }

    private fun startRecording() {
        if (recording != null) return

        if (!hasPermissions()) {
            requestPermissions(permissions, requestCode)
            return
        }

        val capture = videoCapture

        if (capture == null) {
            Toast.makeText(
                this,
                "Camera is not ready",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val filename = "VisibleRecorder_" +
            SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.US
            ).format(Date()) +
            ".mp4"

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, filename)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(
                MediaStore.Video.Media.RELATIVE_PATH,
                "Movies/VisibleRecorder"
            )
        }

        val outputOptions = MediaStoreOutputOptions.Builder(
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
            .setContentValues(contentValues)
            .build()

        var pendingRecording = capture.output.prepareRecording(
            this,
            outputOptions
        )

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            pendingRecording = pendingRecording.withAudioEnabled()
        }

        recording = pendingRecording.start(
            ContextCompat.getMainExecutor(this)
        ) { event ->

            when (event) {
                is VideoRecordEvent.Start -> {
                    recordingStartedAt = System.currentTimeMillis()

                    statusText.text = "● RECORDING"
                    timerText.text = "REC 00:00"

                    startButton.isEnabled = false
                    stopButton.isEnabled = true
                    qualitySpinner.isEnabled = false

                    timerHandler.removeCallbacks(timerRunnable)
                    timerHandler.post(timerRunnable)
                }

                is VideoRecordEvent.Finalize -> {
                    recording = null

                    timerHandler.removeCallbacks(timerRunnable)

                    startButton.isEnabled = true
                    stopButton.isEnabled = false
                    qualitySpinner.isEnabled = true

                    timerText.text = "00:00"

                    if (event.hasError()) {
                        statusText.text = "Recording failed"

                        Toast.makeText(
                            this,
                            "Recording error: ${event.error}",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        statusText.text = "Video saved to Gallery"

                        Toast.makeText(
                            this,
                            "Saved to Movies/VisibleRecorder",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun stopRecording() {
        recording?.stop()

        statusText.text = "Stopping recording…"
        stopButton.isEnabled = false
    }

    override fun onPause() {
        recording?.stop()
        timerHandler.removeCallbacks(timerRunnable)

        super.onPause()
    }

    override fun onDestroy() {
        timerHandler.removeCallbacks(timerRunnable)

        cameraProvider?.unbindAll()

        recording?.stop()
        recording = null

        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == this.requestCode) {
            if (
                grantResults.isNotEmpty() &&
                grantResults.all {
                    it == PackageManager.PERMISSION_GRANTED
                }
            ) {
                startCamera()
            } else {
                statusText.text = "Permissions required"

                Toast.makeText(
                    this,
                    "Camera and microphone permissions are required",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
