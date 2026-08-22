package com.clipper.android_clipper

import android.app.*
import android.content.*
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File

class ClipboardService : Service() {

    private lateinit var clipboardManager: ClipboardManager
    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        saveToFile(getCurrentClipboardText())
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener(clipboardListener)
        startForeground(1, createNotification())
        saveToFile(getCurrentClipboardText())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        clipboardManager.removePrimaryClipChangedListener(clipboardListener)
        super.onDestroy()
    }

    private fun getCurrentClipboardText(): String {
        return try {
            if (clipboardManager.hasPrimaryClip()) {
                clipboardManager.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString() ?: ""
            } else ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun saveToFile(text: String) {
        try {
            File(filesDir, "clipboard_data.txt").writeText(text)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "剪贴板服务", NotificationManager.IMPORTANCE_LOW)
            channel.description = "用于监听剪贴板变化"
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Android Clipper")
        .setContentText("服务运行中")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setPriority(NotificationCompat.PRIORITY_LOW).build()

    companion object {
        private const val CHANNEL_ID = "clipper_channel"
    }
}