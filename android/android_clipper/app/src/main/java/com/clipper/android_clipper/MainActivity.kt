package com.clipper.android_clipper

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var editText: EditText
    private lateinit var textClipboard: TextView
    private lateinit var radioGroupMode: RadioGroup
    private lateinit var radioClipboard: RadioButton
    private lateinit var radioInput: RadioButton
    private lateinit var resizeHandle: View

    private var isInputMode = false
    private var lastY = 0f
    private val MIN_HEIGHT = 100
    private var maxHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        editText = findViewById(R.id.editText)
        textClipboard = findViewById(R.id.textClipboard)
        radioGroupMode = findViewById(R.id.radioGroupMode)
        radioClipboard = findViewById(R.id.radioClipboard)
        radioInput = findViewById(R.id.radioInput)
        resizeHandle = findViewById(R.id.resizeHandle)

        // Get max height (half screen)
        maxHeight = resources.displayMetrics.heightPixels / 2

        // Enable text selection and scrolling
        editText.isVerticalScrollBarEnabled = true
        editText.setTextIsSelectable(true)

        // Auto start clipboard service
        startClipboardService()

        // Set up resize handle
        resizeHandle.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastY = event.rawY
                    v.setBackgroundColor(0xFF999999.toInt())
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.rawY - lastY
                    val newHeight = (editText.height + deltaY).toInt().coerceIn(MIN_HEIGHT, maxHeight)

                    val params = editText.layoutParams
                    params.height = newHeight
                    editText.layoutParams = params

                    lastY = event.rawY
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.setBackgroundColor(0xFFCCCCCC.toInt())
                    true
                }
                else -> false
            }
        }

        // Set up mode switching
        radioGroupMode.setOnCheckedChangeListener { _, checkedId ->
            isInputMode = checkedId == R.id.radioInput
            updateModeUI()
        }

        // Set up buttons
        findViewById<Button>(R.id.btnCopy).setOnClickListener { copyToClipboard() }
        findViewById<Button>(R.id.btnPaste).setOnClickListener { pasteFromClipboard() }
        findViewById<Button>(R.id.btnClear).setOnClickListener { clearClipboard() }
        findViewById<Button>(R.id.btnFloating).setOnClickListener { showFloatingWindow() }
        findViewById<Button>(R.id.btnSelectAll).setOnClickListener { selectAllText() }

        checkOverlayPermission()
        updateModeUI()
    }

    private fun updateModeUI() {
        if (isInputMode) {
            textClipboard.text = "当前模式: 输入内容"
            editText.hint = "在此输入内容"
        } else {
            textClipboard.text = "当前模式: 粘贴板"
            editText.hint = "输入内容（从剪贴板粘贴）"
            // Refresh clipboard display
            refreshClipboardDisplay()
        }
    }

    private fun refreshClipboardDisplay() {
        try {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).coerceToText(this).toString()
                if (text.isNotEmpty()) {
                    textClipboard.text = "剪贴板内容: ${text.take(50)}${if (text.length > 50) "..." else ""}"
                }
            }
        } catch (e: Exception) {
            // ignore
        }
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
        val text = editText.text.toString()
        if (text.isNotEmpty()) {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Clipper", text)
            clipboard.setPrimaryClip(clip)
            // Also save to file directly so Python can read it
            saveToFile(text)
            Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveToFile(text: String) {
        try {
            val file = File(filesDir, "clipboard_data.txt")
            file.writeText(text)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun pasteFromClipboard() {
        if (isInputMode) {
            // Input mode: just paste from clipboard
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).coerceToText(this).toString()
                editText.setText(text)
                textClipboard.text = "已粘贴到输入框"
            } else {
                Toast.makeText(this, "剪贴板为空", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Clipboard mode: read clipboard to input box
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).coerceToText(this).toString()
                editText.setText(text)
                textClipboard.text = "剪贴板内容: ${text.take(50)}${if (text.length > 50) "..." else ""}"
            } else {
                textClipboard.text = "剪贴板内容: (空)"
                Toast.makeText(this, "剪贴板为空", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun selectAllText() {
        editText.requestFocus()
        editText.selectAll()
        Toast.makeText(this, "已全选", Toast.LENGTH_SHORT).show()
    }

    private fun clearClipboard() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        } else {
            val clip = ClipData.newPlainText("", "")
            clipboard.setPrimaryClip(clip)
        }
        editText.text.clear()
        textClipboard.text = "剪贴板内容: (已清空)"
        Toast.makeText(this, "已清空", Toast.LENGTH_SHORT).show()
    }
}