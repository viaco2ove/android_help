package com.clipper.android_clipper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat

class FloatingService : Service() {

    private var floatingView: View? = null
    private var windowManager: WindowManager? = null
    private var isFloating = false

    companion object {
        const val CHANNEL_ID = "clipper_floating"
        const val NOTIFICATION_ID = 2
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "SHOW" -> showFloatingWindow()
            "HIDE" -> hideFloatingWindow()
            else -> showFloatingWindow()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        hideFloatingWindow()
        super.onDestroy()
    }

    private fun showFloatingWindow() {
        if (isFloating) return

        // Start foreground service
        startForeground(NOTIFICATION_ID, createNotification())

        // Create floating view
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingView = inflater.inflate(R.layout.floating_layout, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }

        // Make it draggable
        floatingView?.let { view ->
            view.setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f
                private var isClick = false

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = params.x
                            initialY = params.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            isClick = true
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            params.x = initialX + (event.rawX - initialTouchX).toInt()
                            params.y = initialY + (event.rawY - initialTouchY).toInt()
                            windowManager?.updateViewLayout(view, params)
                            if (isClick) {
                                val dx = Math.abs(event.rawX - initialTouchX)
                                val dy = Math.abs(event.rawY - initialTouchY)
                                if (dx > 10 || dy > 10) isClick = false
                            }
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (isClick) {
                                Toast.makeText(this@FloatingService, "Android Clipper 运行中", Toast.LENGTH_SHORT).show()
                            }
                            return true
                        }
                    }
                    return false
                }
            })

            try {
                windowManager?.addView(view, params)
                isFloating = true
            } catch (e: Exception) {
                Toast.makeText(this, "无法显示悬浮窗", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hideFloatingWindow() {
        if (!isFloating) return
        try {
            floatingView?.let {
                windowManager?.removeView(it)
            }
        } catch (e: Exception) { }
        floatingView = null
        isFloating = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "悬浮窗服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持剪贴板服务在后台运行"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Android Clipper")
        .setContentText("悬浮窗已显示，可拖动")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()
}