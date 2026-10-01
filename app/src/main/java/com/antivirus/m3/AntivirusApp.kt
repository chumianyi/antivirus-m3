package com.antivirus.m3

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.antivirus.m3.util.PreferencesManager

class AntivirusApp : Application() {
    lateinit var preferences: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferences = PreferencesManager(this)
        createNotificationChannels()
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
