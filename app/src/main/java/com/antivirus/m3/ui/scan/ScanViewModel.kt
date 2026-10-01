package com.antivirus.m3.ui.scan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antivirus.m3.AntivirusApp
import com.antivirus.m3.data.AppInfo
import com.antivirus.m3.data.AppScanner
import com.antivirus.m3.data.ScanComplete
import com.antivirus.m3.data.ScanEvent
import com.antivirus.m3.data.ScanProgress
import com.antivirus.m3.data.ScanResult
import com.antivirus.m3.data.VirusDatabase
import com.antivirus.m3.util.AppUninstaller
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    private val scanner = AppScanner(application)

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _currentApp = MutableStateFlow("")
    val currentApp: StateFlow<String> = _currentApp.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _scannedCount = MutableStateFlow(0)
    val scannedCount: StateFlow<Int> = _scannedCount.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    private val _dangerousApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val dangerousApps: StateFlow<List<AppInfo>> = _dangerousApps.asStateFlow()

    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult: StateFlow<ScanResult?> = _scanResult.asStateFlow()

    private val _dbLoaded = MutableStateFlow(false)
    val dbLoaded: StateFlow<Boolean> = _dbLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            VirusDatabase.load(getApplication())
            _dbLoaded.value = true
        }
    }

    fun startScan(includeSystem: Boolean = false) {
        viewModelScope.launch {
            _scanState.value = ScanState.Scanning
            _dangerousApps.value = emptyList()
            _scanResult.value = null
            _progress.value = 0f
            _scannedCount.value = 0

            scanner.scanAllApps(includeSystem).collectLatest { event ->
                when (event) {
                    is ScanProgress -> {
                        _currentApp.value = event.currentApp
                        _scannedCount.value = event.current
                        _totalCount.value = event.total
                        _progress.value = if (event.total > 0) event.current.toFloat() / event.total else 0f
                        _dangerousApps.value = event.dangerousFound
                    }
                    is ScanComplete -> {
                        _scanState.value = ScanState.Complete
                        _scanResult.value = event.result
                        _progress.value = 1f
                        AntivirusApp.instance.preferences.setLastScanTime(System.currentTimeMillis())
                    }
                }
            }
        }
    }

    fun uninstallApp(packageName: String): Boolean {
        return AppUninstaller(getApplication()).uninstallApp(packageName)
    }

    fun resetScan() {
        _scanState.value = ScanState.Idle
        _dangerousApps.value = emptyList()
        _scanResult.value = null
        _progress.value = 0f
        _scannedCount.value = 0
        _currentApp.value = ""
    }
}

sealed class ScanState {
    data object Idle : ScanState()
    data object Scanning : ScanState()
    data object Complete : ScanState()
}
