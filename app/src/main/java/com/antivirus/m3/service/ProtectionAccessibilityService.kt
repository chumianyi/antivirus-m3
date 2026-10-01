package com.antivirus.m3.service

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.antivirus.m3.AntivirusApp
import com.antivirus.m3.R
import com.antivirus.m3.data.AppScanner
import com.antivirus.m3.data.ProtectionMode
import com.antivirus.m3.data.RiskLevel
import com.antivirus.m3.data.VirusDatabase
import com.antivirus.m3.util.AppUninstaller
import com.antivirus.m3.util.PreferencesManager
import com.antivirus.m3.util.ShizukuHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ProtectionAccessibilityService : AccessibilityService() {

    private var currentMode: ProtectionMode = ProtectionMode.OFF
    private lateinit var preferences: PreferencesManager
    private lateinit var scanner: AppScanner

    override fun onCreate() {
        super.onCreate()
        preferences = AntivirusApp.instance.preferences
        scanner = AppScanner(this)
        VirusDatabase.load(this)
        CoroutineScope(Dispatchers.IO).launch {
            preferences.protectionMode.collect { mode ->
                currentMode = mode
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (currentMode == ProtectionMode.OFF) return
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName) return

        checkAndProtect(packageName)
    }

    private fun checkAndProtect(packageName: String) {
        try {
            val appInfo = scanner.getAllInstalledApps().find { it.packageName == packageName } ?: return
            val scanned = scanner.scanApp(appInfo)

            when (scanned.riskLevel) {
                RiskLevel.CRITICAL -> {
                    // Auto-delete critical risk apps
                    Log.d("Protection", "Critical risk detected: $packageName, auto-deleting")
                    showProtectionNotification(scanned.appName, "超风险应用已自动删除", true)
                    CoroutineScope(Dispatchers.IO).launch {
                        AppUninstaller(this@ProtectionAccessibilityService).forceStopAndUninstall(packageName)
                    }
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
                RiskLevel.HIGH -> {
                    if (currentMode == ProtectionMode.BOMBARD || currentMode == ProtectionMode.THANOS) {
                        // In higher modes, also auto-delete high risk
                        Log.d("Protection", "High risk detected in $currentMode: $packageName, auto-deleting")
                        showProtectionNotification(scanned.appName, "高风险应用已自动删除", true)
                        CoroutineScope(Dispatchers.IO).launch {
                            AppUninstaller(this@ProtectionAccessibilityService).forceStopAndUninstall(packageName)
                        }
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    } else {
                        // Daily mode: ask user
                        showDeleteConfirmationNotification(packageName, scanned.appName)
                    }
                }
                RiskLevel.MEDIUM -> {
                    if (currentMode == ProtectionMode.THANOS) {
                        showDeleteConfirmationNotification(packageName, scanned.appName)
                    }
                }
                else -> { /* Safe or low risk, ignore */ }
            }
        } catch (e: Exception) {
            Log.e("Protection", "Error checking $packageName", e)
        }
    }

    private fun showProtectionNotification(appName: String, message: String, autoDeleted: Boolean) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, AntivirusApp.CHANNEL_PROTECTION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("安全护盾防护")
            .setContentText("$appName: $message")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        nm.notify(AntivirusApp.NOTIFICATION_ID_PROTECTION, notification)
    }

    private fun showDeleteConfirmationNotification(packageName: String, appName: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val confirmIntent = Intent(this, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_CONFIRM_DELETE
            putExtra(NotificationReceiver.EXTRA_PACKAGE_NAME, packageName)
            putExtra(NotificationReceiver.EXTRA_APP_NAME, appName)
        }
        val cancelIntent = Intent(this, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_CANCEL_DELETE
            putExtra(NotificationReceiver.EXTRA_PACKAGE_NAME, packageName)
        }

        val confirmPi = PendingIntent.getBroadcast(
            this, (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            confirmIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cancelPi = PendingIntent.getBroadcast(
            this, (System.currentTimeMillis() % Int.MAX_VALUE).toInt() + 1,
            cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, AntivirusApp.CHANNEL_PROTECTION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("发现危险应用")
            .setContentText("$appName 被检测为危险应用，是否删除？")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "确认删除", confirmPi)
            .addAction(0, "取消", cancelPi)
            .setAutoCancel(true)
            .build()
        nm.notify(packageName.hashCode(), notification)
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("Protection", "Accessibility service connected")
    }
}
