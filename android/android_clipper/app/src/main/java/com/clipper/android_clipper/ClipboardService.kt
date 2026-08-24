package com.clipper.android_clipper

import android.app.*
import android.content.*
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import java.io.File

class ClipboardService : Service() {

    private lateinit var clipboardManager: ClipboardManager
    private var listenerRegistered = false
    private val handler = Handler(Looper.getMainLooper())
    private val checkRunnable = Runnable { checkAndReregisterListener() }

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        saveToFile(getCurrentClipboardText())
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        registerListener()
        startForeground(NOTIFICATION_ID, createNotification())
        saveToFile(getCurrentClipboardText())

        // Periodically check if listener is still registered
        handler.postDelayed(checkRunnable, CHECK_INTERVAL_MS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Re-register listener if needed (service might have been recreated)
        if (!listenerRegistered) {
            registerListener()
        }
        // START_STICKY: tell system to recreate service if killed
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(checkRunnable)
        unregisterListener()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Service is being removed from task, restart it
        val restartIntent = Intent(this, ClipboardService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartIntent)
        } else {
            startService(restartIntent)
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun registerListener() {
        try {
            if (!listenerRegistered) {
                clipboardManager.addPrimaryClipChangedListener(clipboardListener)
                listenerRegistered = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun unregisterListener() {
        try {
            if (listenerRegistered) {
                clipboardManager.removePrimaryClipChangedListener(clipboardListener)
                listenerRegistered = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkAndReregisterListener() {
        // Periodically verify the listener is still working
        if (listenerRegistered) {
            try {
                // Force a save of current clipboard
                saveToFile(getCurrentClipboardText())
            } catch (e: Exception) {
                // Listener might have been invalidated, re-register
                unregisterListener()
                registerListener()
            }
        }
        // Schedule next check
        handler.postDelayed(checkRunnable, CHECK_INTERVAL_MS)
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
            val channel = NotificationChannel(
                CHANNEL_ID,
                "剪贴板服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于监听剪贴板变化"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Android Clipper")
            .setContentText("剪贴板监控中")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "clipper_channel"
        private const val NOTIFICATION_ID = 1
        private const val CHECK_INTERVAL_MS = 30_000L // Check every 30 seconds
    }
}