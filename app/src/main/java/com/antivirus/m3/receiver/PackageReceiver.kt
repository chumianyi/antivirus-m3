package com.antivirus.m3.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.antivirus.m3.data.AppScanner
import com.antivirus.m3.data.ProtectionMode
import com.antivirus.m3.data.RiskLevel
import com.antivirus.m3.data.VirusDatabase
import com.antivirus.m3.util.AppUninstaller
import com.antivirus.m3.util.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class PackageReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                val packageName = intent.data?.schemeSpecificPart ?: return
                Log.d("PackageReceiver", "Package installed/replaced: $packageName")
                scanNewlyInstalledApp(context, packageName)
            }
        }
    }

    private fun scanNewlyInstalledApp(context: Context, packageName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                VirusDatabase.load(context)
                val scanner = AppScanner(context)
                val apps = scanner.getAllInstalledApps()
                val app = apps.find { it.packageName == packageName } ?: return@launch
                val scanned = scanner.scanApp(app)

                val prefs = PreferencesManager(context)
                val mode = prefs.protectionMode.first()

                if (mode != ProtectionMode.OFF && scanned.riskLevel >= RiskLevel.HIGH) {
                    if (scanned.riskLevel == RiskLevel.CRITICAL || mode == ProtectionMode.THANOS) {
                        AppUninstaller(context).forceStopAndUninstall(packageName)
                    }
                }
            } catch (e: Exception) {
                Log.e("PackageReceiver", "Error scanning $packageName", e)
            }
        }
    }
}
