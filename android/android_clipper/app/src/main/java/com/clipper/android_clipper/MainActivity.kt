package com.clipper.android_clipper

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 自动启动剪贴板监听服务
        startClipboardService()

        findViewById<Button>(R.id.btnCopy).setOnClickListener { copyToClipboard() }
        findViewById<Button>(R.id.btnPaste).setOnClickListener { pasteFromClipboard() }
        findViewById<Button>(R.id.btnClear).setOnClickListener { clearClipboard() }
        findViewById<Button>(R.id.btnFloating).setOnClickListener { showFloatingWindow() }

        checkOverlayPermission()
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "需要悬浮窗权限，请在设置中开启", Toast.LENGTH_LONG).show()
        }
    }

    private fun startClipboardService() {
        val intent = Intent(this, ClipboardService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun showFloatingWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请先开启悬浮窗权限", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
            return
        }

        val intent = Intent(this, FloatingService::class.java).apply { action = "SHOW" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "悬浮窗已显示", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun copyToClipboard() {
        val editText = findViewById<EditText>(R.id.editText)
        val text = editText.text.toString()
        if (text.isNotEmpty()) {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Clipper", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pasteFromClipboard() {
        val editText = findViewById<EditText>(R.id.editText)
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).coerceToText(this).toString()
            editText.setText(text)
        }
    }

    private fun clearClipboard() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        } else {
            val clip = android.content.ClipData.newPlainText("", "")
            clipboard.setPrimaryClip(clip)
        }
        Toast.makeText(this, "已清空", Toast.LENGTH_SHORT).show()
    }
}