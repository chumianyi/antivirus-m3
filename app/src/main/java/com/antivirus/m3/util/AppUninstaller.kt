package com.antivirus.m3.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import com.antivirus.m3.service.NotificationReceiver

class AppUninstaller(private val context: Context) {

    fun uninstallApp(packageName: String): Boolean {
        // Try Shizuku first (silent uninstall)
        if (ShizukuHelper.isAvailable() && ShizukuHelper.hasPermission()) {
            val result = ShizukuHelper.uninstallApp(packageName)
            if (result) return true
        }
        // Fall back to intent-based uninstall
        return requestUninstallViaIntent(packageName)
    }

    private fun requestUninstallViaIntent(packageName: String): Boolean {
        return try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(Intent.ACTION_DELETE)
                    .setData(Uri.parse("package:$packageName"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } else {
                Intent(Intent.ACTION_UNINSTALL_PACKAGE)
                    .setData(Uri.parse("package:$packageName"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun forceStopAndUninstall(packageName: String): Boolean {
        if (ShizukuHelper.isAvailable() && ShizukuHelper.hasPermission()) {
            ShizukuHelper.forceStopApp(packageName)
            ShizukuHelper.clearAppData(packageName)
        }
        return uninstallApp(packageName)
    }
}
