package com.antivirus.m3.data

import kotlinx.serialization.Serializable

@Serializable
data class VirusSignature(
    val packageName: String,
    val name: String,
    val riskLevel: Int, // 0=safe, 1=low, 2=medium, 3=high, 4=critical
    val category: String,
    val description: String,
    val permissions: List<String> = emptyList(),
    val autoDelete: Boolean = false
)

data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val permissions: List<String>,
    val riskScore: Int = 0,
    val riskLevel: RiskLevel = RiskLevel.SAFE,
    val matchedSignatures: List<VirusSignature> = emptyList(),
    val riskReasons: List<String> = emptyList()
)

enum class RiskLevel(val displayName: String, val color: Long) {
    SAFE("安全", 0xFF4CAF50),
    LOW("低风险", 0xFFFFC107),
    MEDIUM("中风险", 0xFFFF9800),
    HIGH("高风险", 0xFFFF5722),
    CRITICAL("超风险", 0xFFD32F2F)
}

data class ScanResult(
    val totalApps: Int,
    val scannedApps: Int,
    val dangerousApps: List<AppInfo>,
    val safeApps: Int,
    val scanTime: Long
)

data class ProcessInfo(
    val pid: Int,
    val name: String,
    val user: String,
    val memoryKb: Long,
    val isSystem: Boolean
)

enum class ProtectionMode(val displayName: String, val description: String) {
    OFF("未开启", "防护未开启"),
    DAILY("日常守护", "基础防护模式，拦截已知病毒应用"),
    BOMBARD("狂轰乱炸", "高级防护模式，更强的检测与拦截"),
    THANOS("灭霸模式", "终极防护模式，ADB级别的系统控制权")
}
