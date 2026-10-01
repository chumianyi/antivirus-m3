package com.antivirus.m3

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Environment
import com.antivirus.m3.util.PreferencesManager
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AntivirusApp : Application() {
    lateinit var preferences: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Global crash handler - write logs to file instead of crashing silently
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeCrashLog(throwable)
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            preferences = PreferencesManager(this)
        } catch (e: Exception) {
            // Fallback - preferences might fail on some devices
        }

        try {
            createNotificationChannels()
        } catch (e: Exception) {
            // Notification channels might fail
        }
    }

    private fun writeCrashLog(throwable: Throwable) {
        try {
            val dir = File(getExternalFilesDir(null), "crash_logs")
            if (!dir.exists()) dir.mkdirs()
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val file = File(dir, "crash_$timestamp.txt")
            PrintWriter(FileWriter(file)).use { pw ->
                pw.println("=== Antivirus M3 Crash Report ===")
                pw.println("Time: ${Date()}")
                pw.println("App Version: 1.0.0")
                pw.println("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                pw.println("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                pw.println()
                pw.println("Exception: ${throwable.javaClass.name}")
                pw.println("Message: ${throwable.message}")
                pw.println()
                pw.println("Stack Trace:")
                throwable.printStackTrace(pw)
                var cause = throwable.cause
                while (cause != null) {
                    pw.println()
                    pw.println("Caused by: ${cause.javaClass.name}: ${cause.message}")
                    cause.printStackTrace(pw)
                    cause = cause.cause
                }
            }
        } catch (_: Exception) {}
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val protectionChannel = NotificationChannel(
                CHANNEL_PROTECTION,
                "实时防护",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "安全护盾实时防护通知" }

            val scanChannel = NotificationChannel(
                CHANNEL_SCAN,
                "扫毒结果",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "病毒扫描结果通知" }

            val thanosChannel = NotificationChannel(
                CHANNEL_THANOS,
                "灭霸模式",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "灭霸模式激活与输入通知" }

            nm.createNotificationChannels(listOf(protectionChannel, scanChannel, thanosChannel))
        }
    }

    companion object {
        lateinit var instance: AntivirusApp
            private set
        const val CHANNEL_PROTECTION = "protection"
        const val CHANNEL_SCAN = "scan_result"
        const val CHANNEL_THANOS = "thanos_mode"
        const val NOTIFICATION_ID_PROTECTION = 1001
        const val NOTIFICATION_ID_SCAN = 1002
        const val NOTIFICATION_ID_THANOS = 1003
    }
}
