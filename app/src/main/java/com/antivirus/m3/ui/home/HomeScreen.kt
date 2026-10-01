package com.antivirus.m3.ui.home

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.antivirus.m3.data.ProtectionMode
import com.antivirus.m3.ui.process.ProcessActivity
import com.antivirus.m3.ui.terminal.TerminalActivity
import com.antivirus.m3.util.PermissionManager
import com.antivirus.m3.util.ShizukuHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val viewModel: HomeViewModel = viewModel()
    val context = LocalContext.current
    val mode by viewModel.protectionMode.collectAsState()
    val permStatus by viewModel.permissionStatus.collectAsState()
    val showBombardWarning by viewModel.showBombardWarning.collectAsState()
    val showThanosWarning by viewModel.showThanosWarning.collectAsState()
    val thanosActivated by viewModel.thanosActivated.collectAsState()
    val showRootToast by viewModel.showRootToast.collectAsState()

    val scrollState = rememberScrollState()
    val permissionManager = remember { PermissionManager(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "安全护盾",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "全方位保护您的设备安全",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Security Shield Card
        SecurityShieldCard(
            mode = mode,
            permStatus = permStatus,
            onDailyClick = {
                if (!permStatus.overlay) permissionManager.requestOverlayPermission()
                else if (!permStatus.accessibility) permissionManager.requestAccessibilityPermission()
                else viewModel.enableDailyMode()
            },
            onBombardClick = { viewModel.requestBombardMode() },
            onThanosClick = { viewModel.requestThanosMode() },
            onTurnOff = { viewModel.turnOffProtection() }
        )

        // Permission Status
        PermissionStatusCard(permStatus = permStatus) {
            viewModel.refreshPermissions()
        }

        // Tools Section
        Text(
            text = "实用工具",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Process Management
        ToolCard(
            icon = Icons.Filled.Memory,
            title = "进程管理",
            description = "查看并管理所有运行中的进程（需要Shizuku权限）",
            onClick = {
                if (ShizukuHelper.isAvailable() && ShizukuHelper.hasPermission()) {
                    context.startActivity(Intent(context, ProcessActivity::class.java))
                } else {
                    ShizukuHelper.requestPermission()
                    viewModel.refreshPermissions()
                }
            }
        )

        // One-click Root
        ToolCard(
            icon = Icons.Filled.PowerSettingsNew,
            title = "一键 Root",
            description = "获取设备 Root 权限",
            onClick = { viewModel.showRootUnsupported() }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Bombard Warning Dialog
    if (showBombardWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBombardWarning() },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFFF9800)) },
            title = { Text("狂轰乱炸模式") },
            text = {
                Text("我们并不建议您在日常使用中开启此模式。\n\n该模式会进行更激进的检测和拦截，可能影响正常应用的使用体验。需要 Shizuku 权限以获得更强的防护能力。")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmBombardMode() }) {
                    Text("仍然开启", color = Color(0xFFFF9800))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissBombardWarning() }) {
                    Text("取消")
                }
            }
        )
    }

    // Thanos Warning Dialog
    if (showThanosWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissThanosWarning() },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFD32F2F)) },
            title = { Text("灭霸模式") },
            text = {
                Text("警告：灭霸模式是最高级别的防护模式，需要无线调试（ADB）权限。\n\n开启后将获得系统级别的控制权，可能对设备造成不可逆的影响。请确保您了解此模式的风险。\n\n需要先开启开发者选项和无线调试。")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.confirmThanosMode()
                    context.startActivity(Intent(context, TerminalActivity::class.java))
                }) {
                    Text("我已了解，继续", color = Color(0xFFD32F2F))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissThanosWarning() }) {
                    Text("取消")
                }
            }
        )
    }

    // Root unsupported toast-like dialog
    if (showRootToast) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRootToast() },
            icon = { Icon(Icons.Filled.Info, contentDescription = null) },
            title = { Text("一键 Root") },
            text = { Text("暂不支持一键 root 的功能") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissRootToast() }) {
                    Text("确定")
                }
            }
        )
    }
}

@Composable
fun SecurityShieldCard(
    mode: ProtectionMode,
    permStatus: PermissionStatus,
    onDailyClick: () -> Unit,
    onBombardClick: () -> Unit,
    onThanosClick: () -> Unit,
    onTurnOff: () -> Unit
) {
    val isProtected = mode != ProtectionMode.OFF

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isProtected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Shield Icon with status
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        if (isProtected)
                            Brush.verticalGradient(listOf(Color(0xFF4CAF50), Color(0xFF2E7D32)))
                        else
                            Brush.verticalGradient(listOf(Color(0xFF9E9E9E), Color(0xFF616161)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isProtected) "防护已开启" else "防护未开启",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isProtected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "当前模式: ${mode.displayName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mode selector
            ModeSelector(
                currentMode = mode,
                permStatus = permStatus,
                onDailyClick = onDailyClick,
                onBombardClick = onBombardClick,
                onThanosClick = onThanosClick
            )

            if (isProtected) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onTurnOff,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFD32F2F)
                    )
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("关闭防护")
                }
            }
        }
    }
}

@Composable
fun ModeSelector(
    currentMode: ProtectionMode,
    permStatus: PermissionStatus,
    onDailyClick: () -> Unit,
    onBombardClick: () -> Unit,
    onThanosClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ModeButton(
            title = "日常守护",
            description = "基础防护 · 拦截已知病毒",
            icon = Icons.Filled.GppGood,
            color = Color(0xFF4CAF50),
            selected = currentMode == ProtectionMode.DAILY,
            enabled = true,
            requiredPerms = "悬浮窗 + 无障碍",
            permsGranted = permStatus.overlay && permStatus.accessibility,
            onClick = onDailyClick
        )
        ModeButton(
            title = "狂轰乱炸",
            description = "高级防护 · 更强检测拦截",
            icon = Icons.Filled.Bolt,
            color = Color(0xFFFF9800),
            selected = currentMode == ProtectionMode.BOMBARD,
            enabled = true,
            requiredPerms = "Shizuku + 基础权限",
            permsGranted = permStatus.shizuku && permStatus.overlay && permStatus.accessibility,
            onClick = onBombardClick
        )
        ModeButton(
            title = "灭霸模式",
            description = "终极防护 · 系统级控制",
            icon = Icons.Filled.AdminPanelSettings,
            color = Color(0xFFD32F2F),
            selected = currentMode == ProtectionMode.THANOS,
            enabled = true,
            requiredPerms = "无线调试(ADB)",
            permsGranted = permStatus.wirelessDebugging,
            onClick = onThanosClick
        )
    }
}

@Composable
fun ModeButton(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    selected: Boolean,
    enabled: Boolean,
    requiredPerms: String,
    permsGranted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) color.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, color) else null
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (permsGranted) Icons.Filled.Check else Icons.Filled.Info,
                        contentDescription = null,
                        tint = if (permsGranted) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = requiredPerms,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = color)
            }
        }
    }
}

@Composable
fun PermissionStatusCard(permStatus: PermissionStatus, onRefresh: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("权限状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onRefresh) { Text("刷新") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            PermissionItem("悬浮窗权限", permStatus.overlay)
            PermissionItem("无障碍权限", permStatus.accessibility)
            PermissionItem("Shizuku 权限", permStatus.shizuku)
            PermissionItem("通知权限", permStatus.notification)
            PermissionItem("开发者选项", permStatus.developerOptions)
            PermissionItem("无线调试", permStatus.wirelessDebugging)
        }
    }
}

@Composable
fun PermissionItem(name: String, granted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (granted) Icons.Filled.Check else Icons.Filled.Close,
            contentDescription = null,
            tint = if (granted) Color(0xFF4CAF50) else Color(0xFFD32F2F),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = name, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = if (granted) "已授权" else "未授权",
            style = MaterialTheme.typography.bodySmall,
            color = if (granted) Color(0xFF4CAF50) else Color(0xFFD32F2F)
        )
    }
}

@Composable
fun ToolCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
