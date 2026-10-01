package com.antivirus.m3

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antivirus.m3.data.VirusDatabase
import com.antivirus.m3.ui.navigation.AppNavigation
import com.antivirus.m3.ui.theme.AntivirusM3Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var startupError by mutableStateOf<String?>(null)
    private var isLoading by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            Log.e("MainActivity", "enableEdgeToEdge failed", e)
        }

        // Load virus database asynchronously before showing UI
        CoroutineScope(Dispatchers.IO).launch {
            try {
                VirusDatabase.load(this@MainActivity)
            } catch (e: Exception) {
                Log.e("MainActivity", "VirusDatabase load failed", e)
                withContext(Dispatchers.Main) {
                    startupError = "病毒库加载失败: ${e.message}\n\n${e.stackTraceToString().take(500)}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }

        try {
            setContent {
                val darkMode = try {
                    AntivirusApp.instance.preferences.darkMode
                } catch (e: Exception) {
                    null
                }
                val dynamicColor = try {
                    AntivirusApp.instance.preferences.dynamicColor
                } catch (e: Exception) {
                    null
                }

                var dark by remember { mutableStateOf(false) }
                var dynColor by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    darkMode?.collect { dark = it }
                    dynamicColor?.collect { dynColor = it }
                }

                AntivirusM3Theme(darkTheme = dark, dynamicColor = dynColor) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        when {
                            startupError != null -> ErrorScreen(
                                error = startupError!!,
                                onRetry = { recreate() }
                            )
                            isLoading -> LoadingScreen()
                            else -> AppNavigation()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "setContent failed", e)
            // Show a minimal error as fallback
            startupError = "应用启动失败: ${e.message}\n\n${e.stackTraceToString().take(800)}"
            isLoading = false
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("正在加载病毒库...", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun ErrorScreen(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1C1E))
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "应用启动出错",
            color = Color(0xFFFF5252),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            error,
            color = Color(0xFFB0BEC5),
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("重试")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "错误日志已保存到应用目录 crash_logs 文件夹",
            color = Color(0xFF78909C),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
