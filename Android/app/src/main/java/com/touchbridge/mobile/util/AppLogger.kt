package com.touchbridge.mobile.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private const val TAG = "TouchBridge"
    private const val MAX_LINES = 80
    private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    fun d(component: String, message: String) {
        Log.d(TAG, "[$component] $message")
        append("D", component, message)
    }

    fun i(component: String, message: String) {
        Log.i(TAG, "[$component] $message")
        append("I", component, message)
    }

    fun w(component: String, message: String) {
        Log.w(TAG, "[$component] $message")
        append("W", component, message)
    }

    fun e(component: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, "[$component] $message", throwable)
            append("E", component, "$message — ${throwable.message}")
        } else {
            Log.e(TAG, "[$component] $message")
            append("E", component, message)
        }
    }

    fun clear() {
        _logs.value = emptyList()
    }

    private fun append(level: String, component: String, message: String) {
        val line = "${timeFmt.format(Date())} $level/$component: $message"
        _logs.update { current ->
            (current + line).takeLast(MAX_LINES)
        }
    }
}
