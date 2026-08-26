package com.clipper.android_clipper

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
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

    private var isInputMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        editText = findViewById(R.id.editText)
        textClipboard = findViewById(R.id.textClipboard)
        radioGroupMode = findViewById(R.id.radioGroupMode)
        radioClipboard = findViewById(R.id.radioClipboard)
        radioInput = findViewById(R.id.radioInput)

        // Enable text selection and scrolling
        editText.isVerticalScrollBarEnabled = true
        editText.setTextIsSelectable(true)

        // Auto save EditText content to file (for ContentProvider to read)
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Save EditText content to edit_text_data.txt
                saveEditTextToFile(s.toString())
            }
        })

        // Auto start clipboard service
        startClipboardService()

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
            // Also save to clipboard file
            saveClipboardToFile(text)
            Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveClipboardToFile(text: String) {
        try {
            val file = File(filesDir, "clipboard_data.txt")
            file.writeText(text)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveEditTextToFile(text: String) {
        try {
            val file = File(filesDir, "edit_text_data.txt")
            file.writeText(text)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun pasteFromClipboard() {
        if (isInputMode) {
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