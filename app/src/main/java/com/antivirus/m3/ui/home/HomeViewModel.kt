package com.antivirus.m3.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antivirus.m3.AntivirusApp
import com.antivirus.m3.data.ProtectionMode
import com.antivirus.m3.data.VirusDatabase
import com.antivirus.m3.service.ProtectionForegroundService
import com.antivirus.m3.util.PermissionManager
import com.antivirus.m3.util.PreferencesManager
import com.antivirus.m3.util.ShizukuHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences: PreferencesManager = AntivirusApp.instance.preferences
    private val permissionManager = PermissionManager(application)

    private val _protectionMode = MutableStateFlow(ProtectionMode.OFF)
    val protectionMode: StateFlow<ProtectionMode> = _protectionMode.asStateFlow()

    private val _showBombardWarning = MutableStateFlow(false)
    val showBombardWarning: StateFlow<Boolean> = _showBombardWarning.asStateFlow()

    private val _showThanosWarning = MutableStateFlow(false)
    val showThanosWarning: StateFlow<Boolean> = _showThanosWarning.asStateFlow()

    private val _thanosActivated = MutableStateFlow(false)
    val thanosActivated: StateFlow<Boolean> = _thanosActivated.asStateFlow()

    private val _permissionStatus = MutableStateFlow(PermissionStatus())
    val permissionStatus: StateFlow<PermissionStatus> = _permissionStatus.asStateFlow()

    private val _showRootToast = MutableStateFlow(false)
    val showRootToast: StateFlow<Boolean> = _showRootToast.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.protectionMode.collect { _protectionMode.value = it }
        }
        viewModelScope.launch {
            preferences.thanosActivated.collect { _thanosActivated.value = it }
        }
        refreshPermissions()
        VirusDatabase.load(application)
    }

    fun refreshPermissions() {
        _permissionStatus.value = PermissionStatus(
            overlay = permissionManager.hasOverlayPermission(),
            accessibility = permissionManager.hasAccessibilityPermission(),
            shizuku = ShizukuHelper.isAvailable() && ShizukuHelper.hasPermission(),
            notification = permissionManager.hasNotificationPermission(),
            developerOptions = permissionManager.isDeveloperOptionsEnabled(),
            wirelessDebugging = permissionManager.isWirelessDebuggingEnabled()
        )
    }

    fun setMode(mode: ProtectionMode) {
        viewModelScope.launch {
            preferences.setProtectionMode(mode)
            _protectionMode.value = mode
            if (mode == ProtectionMode.OFF) {
                ProtectionForegroundService.stop(getApplication())
            } else {
                ProtectionForegroundService.start(getApplication(), mode)
            }
        }
    }

    fun enableDailyMode() {
        setMode(ProtectionMode.DAILY)
    }

    fun requestBombardMode() {
        _showBombardWarning.value = true
    }

    fun confirmBombardMode() {
        _showBombardWarning.value = false
        if (ShizukuHelper.isAvailable() && !ShizukuHelper.hasPermission()) {
            ShizukuHelper.requestPermission()
        }
        setMode(ProtectionMode.BOMBARD)
    }

    fun dismissBombardWarning() {
        _showBombardWarning.value = false
    }

    fun requestThanosMode() {
        _showThanosWarning.value = true
    }

    fun confirmThanosMode() {
        _showThanosWarning.value = false
        // Will guide user to wireless debugging and terminal activation
    }

    fun dismissThanosWarning() {
        _showThanosWarning.value = false
    }

    fun activateThanosMode() {
        viewModelScope.launch {
            preferences.setThanosActivated(true)
            _thanosActivated.value = true
            setMode(ProtectionMode.THANOS)
        }
    }

    fun showRootUnsupported() {
        _showRootToast.value = true
    }

    fun dismissRootToast() {
        _showRootToast.value = false
    }

    fun turnOffProtection() {
        setMode(ProtectionMode.OFF)
    }
}

data class PermissionStatus(
    val overlay: Boolean = false,
    val accessibility: Boolean = false,
    val shizuku: Boolean = false,
    val notification: Boolean = false,
    val developerOptions: Boolean = false,
    val wirelessDebugging: Boolean = false
)
