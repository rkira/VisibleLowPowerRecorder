package com.example.visiblelowpowerrecorder

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val requestCode = 42
    private val permissions = buildList {
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 48, 36, 36)
        }
        layout.addView(TextView(this).apply {
            text = "VISIBLE LOW-POWER RECORDER\n\nRecording always shows an ongoing notification. Start only when you have permission to record everyone involved.\n\nLow-power defaults: 720p, 30 FPS, hardware encoding."
            textSize = 18f
        })
        layout.addView(Button(this).apply {
            text = "START RECORDING"
            setOnClickListener {
                if (permissions.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) startRecorder()
                else requestPermissions(permissions.toTypedArray(), requestCode)
            }
        })
        layout.addView(Button(this).apply {
            text = "STOP RECORDING"
            setOnClickListener { stopService(Intent(this@MainActivity, RecordingService::class.java)) }
        })
        setContentView(layout)
    }

    private fun startRecorder() {
        val intent = Intent(this, RecordingService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        Toast.makeText(this, "Recording started; see the persistent notification.", Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.requestCode && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) startRecorder()
        else Toast.makeText(this, "Camera, microphone and notification permissions are required.", Toast.LENGTH_LONG).show()
    }
}
