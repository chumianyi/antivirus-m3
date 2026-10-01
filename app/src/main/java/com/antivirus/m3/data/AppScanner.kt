package com.antivirus.m3.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class AppScanner(private val context: Context) {

    fun getAllInstalledApps(): List<AppInfo> {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS or PackageManager.GET_META_DATA)
        return packages.map { pkg ->
            val appName = pkg.applicationInfo?.loadLabel(pm)?.toString() ?: pkg.packageName
            val isSystem = (pkg.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
            val permissions = pkg.requestedPermissions?.toList() ?: emptyList()
            AppInfo(
                packageName = pkg.packageName,
                appName = appName,
                versionName = pkg.versionName ?: "unknown",
                isSystemApp = isSystem,
                permissions = permissions
            )
        }
    }

    fun scanApp(app: AppInfo): AppInfo {
        val signature = VirusDatabase.getSignature(app.packageName)
        var riskScore = 0
        val riskReasons = mutableListOf<String>()
        val matchedSignatures = mutableListOf<VirusSignature>()

        if (signature != null) {
            matchedSignatures.add(signature)
            riskScore += signature.riskLevel * 25
            riskReasons.add("匹配病毒库特征: ${signature.name} (${signature.category})")
        }

        // Permission-based risk assessment
        for (perm in app.permissions) {
            val rule = VirusDatabase.getPermissionRisk(perm)
            if (rule != null && rule.risk > 0) {
                riskScore += rule.risk * 5
                if (rule.risk >= 3) {
                    riskReasons.add("高危权限: ${perm.substringAfterLast('.')} - ${rule.reason}")
                }
            }
        }

        // Heuristic: dangerous permission combinations
        val permSet = app.permissions.toSet()
        if (permSet.containsAll(listOf("android.permission.READ_SMS", "android.permission.SEND_SMS", "android.permission.INTERNET"))) {
            riskScore += 20
            riskReasons.add("危险权限组合: 短信读写+网络 (典型银行木马行为)")
        }
        if (permSet.containsAll(listOf("android.permission.RECORD_AUDIO", "android.permission.CAMERA", "android.permission.INTERNET"))) {
            riskScore += 15
            riskReasons.add("敏感权限组合: 录音+相机+网络 (可能的间谍行为)")
        }
        if (permSet.containsAll(listOf("android.permission.READ_CONTACTS", "android.permission.INTERNET"))) {
            riskScore += 10
            riskReasons.add("隐私权限组合: 通讯录+网络 (可能上传联系人)")
        }
        if (permSet.containsAll(listOf("android.permission.REQUEST_INSTALL_PACKAGES", "android.permission.INTERNET"))) {
            riskScore += 15
            riskReasons.add("危险权限组合: 静默安装+网络 (可能下载安装恶意软件)")
        }
        if (permSet.contains("android.permission.BIND_ACCESSIBILITY_SERVICE")) {
            riskScore += 30
            riskReasons.add("无障碍服务权限: 可完全控制设备操作")
        }
        if (permSet.contains("android.permission.SYSTEM_ALERT_WINDOW") && !app.isSystemApp) {
            riskScore += 10
            riskReasons.add("悬浮窗权限: 可覆盖其他应用界面进行钓鱼")
        }

        val riskLevel = when {
            riskScore >= 80 -> RiskLevel.CRITICAL
            riskScore >= 55 -> RiskLevel.HIGH
            riskScore >= 35 -> RiskLevel.MEDIUM
            riskScore >= 15 -> RiskLevel.LOW
            else -> RiskLevel.SAFE
        }

        return app.copy(
            riskScore = riskScore.coerceAtMost(100),
            riskLevel = riskLevel,
            matchedSignatures = matchedSignatures,
            riskReasons = riskReasons
        )
    }

    fun scanAllApps(
        includeSystem: Boolean = false,
        onProgress: ((Int, Int, String) -> Unit)? = null
    ): Flow<ScanEvent> = flow {
        val allApps = getAllInstalledApps().filter { includeSystem || !it.isSystemApp }
        val total = allApps.size
        val dangerous = mutableListOf<AppInfo>()
        var safeCount = 0
        val startTime = System.currentTimeMillis()

        for ((index, app) in allApps.withIndex()) {
            val scanned = scanApp(app)
            if (scanned.riskLevel >= RiskLevel.LOW) {
                dangerous.add(scanned)
            } else {
                safeCount++
            }
            emit(ScanProgress(index + 1, total, scanned.appName, scanned, dangerous.toList()))
            onProgress?.invoke(index + 1, total, scanned.appName)
        }

        val result = ScanResult(
            totalApps = total,
            scannedApps = total,
            dangerousApps = dangerous.sortedByDescending { it.riskScore },
            safeApps = safeCount,
            scanTime = System.currentTimeMillis() - startTime
        )
        emit(ScanComplete(result))
    }.flowOn(Dispatchers.IO)
}

sealed class ScanEvent
data class ScanProgress(
    val current: Int,
    val total: Int,
    val currentApp: String,
    val scannedApp: AppInfo,
    val dangerousFound: List<AppInfo>
) : ScanEvent()

data class ScanComplete(val result: ScanResult) : ScanEvent()
