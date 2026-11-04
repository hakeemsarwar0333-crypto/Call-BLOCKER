package com.example.callblocker

import android.app.role.RoleManager
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var logView: TextView

    private fun refreshLog() {
        val prefs = getSharedPreferences("blocker", Context.MODE_PRIVATE)
        val log = prefs.getString("log", "") ?: ""
        logView.text = if (log.isBlank()) "No blocked calls yet" else log
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("blocker", Context.MODE_PRIVATE)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        val label = TextView(this).apply { text = "Blocked ranges (one per line: start-end)" }
        val ranges = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 4
            setText(prefs.getString("ranges", CallBlockerService.DEFAULT_RANGES))
        }
        val save = Button(this).apply { text = "Save ranges" }
        val enable = Button(this).apply { text = "Enable call blocking" }
        val logLabel = TextView(this).apply {
            text = "\nBlocked calls (latest first)"
        }
        logView = TextView(this)
        val clear = Button(this).apply { text = "Clear log" }

        save.setOnClickListener {
            prefs.edit().putString("ranges", ranges.text.toString()).apply()
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        }
        enable.setOnClickListener {
            val rm = getSystemService(RoleManager::class.java)
            if (rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
                !rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), 1)
            } else {
                Toast.makeText(this, "Already enabled", Toast.LENGTH_SHORT).show()
            }
        }
        clear.setOnClickListener {
            prefs.edit().remove("log").apply()
            refreshLog()
        }

        listOf(label, ranges, save, enable, logLabel, logView, clear)
            .forEach { layout.addView(it) }
        setContentView(ScrollView(this).apply { addView(layout) })
        refreshLog()
    }

    override fun onResume() {
        super.onResume()
        refreshLog()
    }
}
