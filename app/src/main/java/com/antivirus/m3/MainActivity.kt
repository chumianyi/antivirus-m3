package com.antivirus.m3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.antivirus.m3.data.VirusDatabase
import com.antivirus.m3.ui.navigation.AppNavigation
import com.antivirus.m3.ui.theme.AntivirusM3Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Load virus database in background
        CoroutineScope(Dispatchers.IO).launch {
            VirusDatabase.load(this@MainActivity)
        }

        setContent {
            val darkMode by AntivirusApp.instance.preferences.darkMode.collectAsState(initial = false)
            val dynamicColor by AntivirusApp.instance.preferences.dynamicColor.collectAsState(initial = true)
            AntivirusM3Theme(darkTheme = darkMode, dynamicColor = dynamicColor) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}
