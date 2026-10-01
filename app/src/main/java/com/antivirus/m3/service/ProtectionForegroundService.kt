package com.antivirus.m3.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.antivirus.m3.AntivirusApp
import com.antivirus.m3.MainActivity
import com.antivirus.m3.R
import com.antivirus.m3.data.ProtectionMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ProtectionForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mode = intent?.getStringExtra(EXTRA_MODE) ?: ProtectionMode.DAILY.name
        startForegroundWithMode(mode)
        return START_STICKY
    }

    private fun startForegroundWithMode(modeName: String) {
        val mode = ProtectionMode.valueOf(modeName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, AntivirusApp.CHANNEL_PROTECTION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("安全护盾运行中")
            .setContentText("当前模式: ${mode.displayName}")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(AntivirusApp.NOTIFICATION_ID_PROTECTION, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(AntivirusApp.NOTIFICATION_ID_PROTECTION, notification)
        }
    }

    companion object {
        const val EXTRA_MODE = "protection_mode"

        fun start(context: Context, mode: ProtectionMode) {
            val intent = Intent(context, ProtectionForegroundService::class.java).apply {
                putExtra(EXTRA_MODE, mode.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ProtectionForegroundService::class.java))
        }
    }
}
