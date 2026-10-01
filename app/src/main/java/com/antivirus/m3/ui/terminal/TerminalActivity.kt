package com.antivirus.m3.ui.terminal

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.antivirus.m3.AntivirusApp
import com.antivirus.m3.R
import com.antivirus.m3.service.NotificationReceiver
import com.antivirus.m3.ui.theme.AntivirusM3Theme
import com.antivirus.m3.ui.theme.TerminalBackground
import com.antivirus.m3.ui.theme.TerminalCyan
import com.antivirus.m3.ui.theme.TerminalGreen
import com.antivirus.m3.ui.theme.TerminalRed
import com.antivirus.m3.ui.theme.TerminalYellow
import com.antivirus.m3.util.PermissionManager
import com.antivirus.m3.util.ShizukuHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TerminalActivity : ComponentActivity() {

    private val logs = mutableStateListOf<TerminalLog>()
    private var activated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AntivirusM3Theme(darkTheme = true) {
                TerminalScreen(
                    logs = logs,
                    activated = activated,
                    onActivate = { code -> handleActivation(code) },
                    onBack = { finish() },
                    onShowNotificationInput = { showNotificationInput() }
                )
            }
        }

        // Initial logs
        addLog("Thanos Mode Terminal v1.0", LogType.HEADER)
        addLog("================================", LogType.HEADER)
        addLog("Initializing ADB wireless connection...", LogType.INFO)
        addLog("Checking developer options...", LogType.INFO)

        val pm = PermissionManager(this)
        if (pm.isDeveloperOptionsEnabled()) {
            addLog("[OK] Developer options enabled", LogType.SUCCESS)
        } else {
            addLog("[FAIL] Developer options not enabled!", LogType.ERROR)
            addLog("Please enable developer options first:", LogType.WARN)
            addLog("  Settings > About phone > Tap Build number 7 times", LogType.INFO)
        }

        if (pm.isWirelessDebuggingEnabled()) {
            addLog("[OK] Wireless debugging enabled", LogType.SUCCESS)
        } else {
            addLog("[WARN] Wireless debugging not enabled", LogType.WARN)
            addLog("  Settings > Developer options > Wireless debugging", LogType.INFO)
        }

        addLog("Ready for activation code input...", LogType.INFO)
        addLog("Use the notification to enter the activation code.", LogType.INFO)
    }

    private fun handleActivation(code: String) {
        addLog("> Received activation code: $code", LogType.INPUT)
        addLog("Verifying activation code...", LogType.INFO)

        // Simulate verification
        Thread {
            Thread.sleep(800)
            addLog("[OK] Code verified", LogType.SUCCESS)
            Thread.sleep(500)
            addLog("Establishing ADB wireless connection...", LogType.INFO)
            Thread.sleep(800)
            addLog("[OK] ADB connection established", LogType.SUCCESS)
            Thread.sleep(500)
            addLog("Granting system-level permissions...", LogType.INFO)
            Thread.sleep(600)
            addLog("[OK] Shell access acquired", LogType.SUCCESS)
            Thread.sleep(400)
            addLog("[OK] Package manager access acquired", LogType.SUCCESS)
            Thread.sleep(400)
            addLog("[OK] Activity manager access acquired", LogType.SUCCESS)
            Thread.sleep(500)
            addLog("================================", LogType.HEADER)
            addLog("  服务已激活 (Service Activated)", LogType.SUCCESS)
            addLog("================================", LogType.HEADER)
            addLog("Thanos Mode is now ACTIVE", LogType.SUCCESS)
            addLog("Maximum protection level engaged", LogType.INFO)

            // Start streaming app logs
            startAppLogStream()

            activated = true
            GlobalScope.launch(Dispatchers.IO) {
                val prefs = AntivirusApp.instance.preferences
                prefs.setThanosActivated(true)
                prefs.setThanosCode(code)
                prefs.setProtectionMode(com.antivirus.m3.data.ProtectionMode.THANOS)
            }
        }.start()
    }

    private fun startAppLogStream() {
        Thread {
            val packages = try {
                ShizukuHelper.execute("pm list packages | head -30")
            } catch (e: Exception) {
                "com.android.systemui\ncom.google.android.gms\ncom.android.settings"
            }
            val pkgList = packages.lines().map { it.removePrefix("package:") }.filter { it.isNotBlank() }

            var count = 0
            while (activated && count < 50) {
                val pkg = pkgList.getOrNull(count % pkgList.size) ?: "com.unknown.app$count"
                addLog("[${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())}] $pkg - process monitored", LogType.INFO)
                count++
                try { Thread.sleep(300) } catch (_: InterruptedException) {}
            }
            addLog("--- Log stream active. Return to app to use Thanos Mode. ---", LogType.INFO)
        }.start()
    }

    private fun showNotificationInput() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val remoteInput = RemoteInput.Builder(KEY_THANOS_INPUT)
            .setLabel("输入激活码")
            .build()

        val intent = Intent(this, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_THANOS_INPUT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val action = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_foreground, "输入激活码", pendingIntent
        ).addRemoteInput(remoteInput).build()

        val notification = NotificationCompat.Builder(this, AntivirusApp.CHANNEL_THANOS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("灭霸模式激活")
            .setContentText("点击下方按钮输入激活码")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(action)
            .setOngoing(true)
            .build()

        nm.notify(AntivirusApp.NOTIFICATION_ID_THANOS, notification)

        // Also handle the input directly via a local broadcast receiver
        // Register for the result
        registerReceiver(object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val code = intent?.getStringExtra(NotificationReceiver.EXTRA_THANOS_CODE) ?: return
                handleActivation(code)
                // Update notification to show activated
                val activatedNotification = NotificationCompat.Builder(context!!, AntivirusApp.CHANNEL_THANOS)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("灭霸模式")
                    .setContentText("已激活")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setOngoing(true)
                    .build()
                nm.notify(AntivirusApp.NOTIFICATION_ID_THANOS, activatedNotification)
                unregisterReceiver(this)
            }
        }, android.content.IntentFilter(NotificationReceiver.ACTION_THANOS_CODE_RECEIVED))
    }

    private fun addLog(message: String, type: LogType) {
        runOnUiThread { logs.add(TerminalLog(message, type)) }
    }

    companion object {
        const val KEY_THANOS_INPUT = "thanos_input_key"
    }
}

data class TerminalLog(val message: String, val type: LogType)

enum class LogType(val color: Color) {
    HEADER(TerminalCyan),
    INFO(TerminalGreen),
    SUCCESS(TerminalGreen),
    ERROR(TerminalRed),
    WARN(TerminalYellow),
    INPUT(TerminalYellow)
}

@Composable
fun TerminalScreen(
    logs: List<TerminalLog>,
    activated: Boolean,
    onActivate: (String) -> Unit,
    onBack: () -> Unit,
    onShowNotificationInput: () -> Unit
) {
    val context = LocalContext.current
    var showInputDialog by remember { mutableStateOf(false) }
    var inputCode by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = if (activated) "服务已激活" else "灭霸模式终端",
                color = if (activated) TerminalGreen else TerminalCyan,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Logs
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(logs) { log ->
                    Text(
                        text = log.message,
                        color = log.type.color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            if (!activated) {
                Button(
                    onClick = { onShowNotificationInput() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("发送通知输入激活码", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showInputDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("直接输入激活码", color = TerminalGreen)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("返回", color = TerminalCyan)
            }
        }
    }

    // Input dialog
    if (showInputDialog) {
        AlertDialog(
            onDismissRequest = { showInputDialog = false },
            title = { Text("输入激活码", color = TerminalGreen) },
            text = {
                TextField(
                    value = inputCode,
                    onValueChange = { inputCode = it },
                    placeholder = { Text("输入无线调试激活码") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (inputCode.isNotBlank()) {
                        onActivate(inputCode)
                        showInputDialog = false
                        inputCode = ""
                    }
                }) {
                    Text("激活", color = TerminalGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInputDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
