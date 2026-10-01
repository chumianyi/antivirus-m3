package com.antivirus.m3.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antivirus.m3.AntivirusApp
import com.antivirus.m3.data.ProtectionMode
import com.antivirus.m3.service.ProtectionForegroundService
import com.antivirus.m3.util.PermissionManager
import com.antivirus.m3.util.PreferencesManager
import com.antivirus.m3.util.ShizukuHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private var preferences: PreferencesManager? = null
    private val permissionManager = try {
        PermissionManager(application)
    } catch (e: Exception) {
        Log.e("HomeViewModel", "PermissionManager init failed", e)
        null
    }

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
        try {
            preferences = AntivirusApp.instance.preferences
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Failed to get preferences", e)
        }

        viewModelScope.launch {
            try {
                preferences?.protectionMode?.collect { _protectionMode.value = it }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "protectionMode collect failed", e)
            }
        }
        viewModelScope.launch {
            try {
                preferences?.thanosActivated?.collect { _thanosActivated.value = it }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "thanosActivated collect failed", e)
            }
        }
        refreshPermissions()
    }

    fun refreshPermissions() {
        try {
            val pm = permissionManager ?: return
            _permissionStatus.value = PermissionStatus(
                overlay = safeCall { pm.hasOverlayPermission() } ?: false,
                accessibility = safeCall { pm.hasAccessibilityPermission() } ?: false,
                shizuku = safeCall { ShizukuHelper.isAvailable() && ShizukuHelper.hasPermission() } ?: false,
                notification = safeCall { pm.hasNotificationPermission() } ?: false,
                developerOptions = safeCall { pm.isDeveloperOptionsEnabled() } ?: false,
                wirelessDebugging = safeCall { pm.isWirelessDebuggingEnabled() } ?: false
            )
        } catch (e: Exception) {
            Log.e("HomeViewModel", "refreshPermissions failed", e)
        }
    }

    private inline fun <T> safeCall(block: () -> T): T? {
        return try {
            block()
        } catch (e: Exception) {
            Log.e("HomeViewModel", "safeCall failed", e)
            null
        }
    }

    fun setMode(mode: ProtectionMode) {
        viewModelScope.launch {
            try {
                preferences?.setProtectionMode(mode)
                _protectionMode.value = mode
                if (mode == ProtectionMode.OFF) {
                    ProtectionForegroundService.stop(getApplication())
                } else {
                    ProtectionForegroundService.start(getApplication(), mode)
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "setMode failed", e)
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
        try {
            if (ShizukuHelper.isAvailable() && !ShizukuHelper.hasPermission()) {
                ShizukuHelper.requestPermission()
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Shizuku request failed", e)
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
    }

    fun dismissThanosWarning() {
        _showThanosWarning.value = false
    }

    fun activateThanosMode() {
        viewModelScope.launch {
            try {
                preferences?.setThanosActivated(true)
                _thanosActivated.value = true
                setMode(ProtectionMode.THANOS)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "activateThanosMode failed", e)
            }
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
