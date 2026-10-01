package com.antivirus.m3.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.antivirus.m3.util.AppUninstaller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_CONFIRM_DELETE -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return
                val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: packageName
                Log.d("Notification", "User confirmed delete: $packageName")

                // Cancel the notification
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(packageName.hashCode())

                // Uninstall immediately
                CoroutineScope(Dispatchers.IO).launch {
                    AppUninstaller(context).forceStopAndUninstall(packageName)
                }
            }
            ACTION_CANCEL_DELETE -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(packageName.hashCode())
                Log.d("Notification", "User cancelled delete: $packageName")
            }
            ACTION_THANOS_INPUT -> {
                val code = intent.getStringExtra(EXTRA_THANOS_CODE) ?: return
                Log.d("Thanos", "Received code: $code")
                // Handle Thanos mode activation via notification input
                val resultIntent = Intent(ACTION_THANOS_CODE_RECEIVED).apply {
                    setPackage(context.packageName)
                    putExtra(EXTRA_THANOS_CODE, code)
                }
                context.sendBroadcast(resultIntent)
            }
        }
    }

    companion object {
        const val ACTION_CONFIRM_DELETE = "com.antivirus.m3.CONFIRM_DELETE"
        const val ACTION_CANCEL_DELETE = "com.antivirus.m3.CANCEL_DELETE"
        const val ACTION_THANOS_INPUT = "com.antivirus.m3.THANOS_INPUT"
        const val ACTION_THANOS_CODE_RECEIVED = "com.antivirus.m3.THANOS_CODE_RECEIVED"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_THANOS_CODE = "thanos_code"
    }
}
