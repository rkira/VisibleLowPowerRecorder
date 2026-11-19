package com.example.visiblelowpowerrecorder

import android.app.Service
import android.content.Intent
import android.os.IBinder

class StopRecordingService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopService(Intent(this, RecordingService::class.java))
        stopSelf()
        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
