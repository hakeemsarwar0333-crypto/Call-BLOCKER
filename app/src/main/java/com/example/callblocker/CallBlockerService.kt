package com.example.callblocker

import android.content.Context
import android.content.SharedPreferences
import android.telecom.Call
import android.telecom.CallScreeningService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallBlockerService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val incoming = callDetails.handle?.schemeSpecificPart ?: ""
        val prefs = getSharedPreferences("blocker", Context.MODE_PRIVATE)
        val ranges = prefs.getString("ranges", DEFAULT_RANGES) ?: DEFAULT_RANGES

        val block = isBlocked(incoming, ranges)
        if (block) logBlocked(prefs, incoming)

        val response = CallResponse.Builder()
            .setDisallowCall(block)
            .setRejectCall(block)
            .setSkipCallLog(false)
            .setSkipNotification(block)
            .build()
        respondToCall(callDetails, response)
    }

    private fun logBlocked(prefs: SharedPreferences, number: String) {
        val time = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date())
        val shown = if (number.isBlank()) "Unknown" else number
        val old = prefs.getString("log", "") ?: ""
        val lines = (listOf("$time   $shown") + old.lines().filter { it.isNotBlank() }).take(100)
        prefs.edit().putString("log", lines.joinToString("\n")).apply()
    }

    companion object {
        const val DEFAULT_RANGES = "0518700000-0518799999\n0518080000-0518089999"

        fun normalize(raw: String): String {
            var d = raw.filter { it.isDigit() }
            if (d.startsWith("0092")) d = d.removePrefix("0092")
            else if (d.startsWith("92") && d.length >= 11) d = d.removePrefix("92")
            else if (d.startsWith("0")) d = d.removePrefix("0")
            return d
        }

        fun isBlocked(number: String, ranges: String): Boolean {
            val n = normalize(number).toLongOrNull() ?: return false
            return ranges.lines().any { line ->
                val parts = line.split("-")
                if (parts.size != 2) return@any false
                val lo = normalize(parts[0]).toLongOrNull() ?: return@any false
                val hi = normalize(parts[1]).toLongOrNull() ?: return@any false
                n in minOf(lo, hi)..maxOf(lo, hi)
            }
        }
    }
}
