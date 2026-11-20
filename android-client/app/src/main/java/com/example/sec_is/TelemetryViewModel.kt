package com.example.sec_is

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

val CyberCyan = Color(0xFF38BDF8)
val NeonViolet = Color(0xFFA855F7)
val EmeraldMatrix = Color(0xFF34D399)
val CrimsonPulse = Color(0xFFF43F5E)

class TelemetryViewModel(application: Application) : AndroidViewModel(application) {

    val repository = TelemetryRepository(application)

    val latchedSnapshot: StateFlow<TelemetrySnapshot?> = repository.latchedSnapshot
    val connectionState: StateFlow<ConnectionState> = repository.connectionState
    val connectedIp: StateFlow<String> = repository.connectedIp
    val port: StateFlow<Int> = repository.port
    val accessPin: StateFlow<String> = repository.accessPin

    val cpuTempHistory: StateFlow<List<Float>> = repository.cpuTempHistory
    val gpuTempHistory: StateFlow<List<Float>> = repository.gpuTempHistory
    val iGpuLoadHistory: StateFlow<List<Float>> = repository.iGpuLoadHistory
    val netSpeedHistory: StateFlow<List<Float>> = repository.netSpeedHistory

    private val _accentColor = MutableStateFlow(EndfieldAcidChartreuse)
    val accentColor: StateFlow<Color> = _accentColor.asStateFlow()

    private val _showCpuCard = MutableStateFlow(true)
    val showCpuCard: StateFlow<Boolean> = _showCpuCard.asStateFlow()

    private val _showGpuCard = MutableStateFlow(true)
    val showGpuCard: StateFlow<Boolean> = _showGpuCard.asStateFlow()

    private val _showRamStorageCard = MutableStateFlow(true)
    val showRamStorageCard: StateFlow<Boolean> = _showRamStorageCard.asStateFlow()

    private val _showNetBatteryCard = MutableStateFlow(true)
    val showNetBatteryCard: StateFlow<Boolean> = _showNetBatteryCard.asStateFlow()

    private val _showTopAppsCard = MutableStateFlow(true)
    val showTopAppsCard: StateFlow<Boolean> = _showTopAppsCard.asStateFlow()

    private val _isCustomizationSheetOpen = MutableStateFlow(false)
    val isCustomizationSheetOpen: StateFlow<Boolean> = _isCustomizationSheetOpen.asStateFlow()

    private val _manualIpInput = MutableStateFlow("")
    val manualIpInput: StateFlow<String> = _manualIpInput.asStateFlow()

    private val _pinInput = MutableStateFlow(repository.accessPin.value)
    val pinInput: StateFlow<String> = _pinInput.asStateFlow()

    fun setAccentColor(color: Color) {
        _accentColor.value = color
    }

    fun setShowCpuCard(show: Boolean) {
        _showCpuCard.value = show
    }

    fun setShowGpuCard(show: Boolean) {
        _showGpuCard.value = show
    }

    fun setShowRamStorageCard(show: Boolean) {
        _showRamStorageCard.value = show
    }

    fun setShowNetBatteryCard(show: Boolean) {
        _showNetBatteryCard.value = show
    }

    fun setShowTopAppsCard(show: Boolean) {
        _showTopAppsCard.value = show
    }

    fun setCustomizationSheetOpen(open: Boolean) {
        _isCustomizationSheetOpen.value = open
    }

    fun updateManualIpInput(input: String) {
        _manualIpInput.value = input
    }

    fun updatePinInput(pin: String) {
        _pinInput.value = pin
    }

    fun connectManualIp() {
        val input = _manualIpInput.value.trim()
        val pin = _pinInput.value.trim()
        if (input.isNotEmpty()) {
            val parts = input.split(":")
            val ip = parts[0]
            val targetPort = if (parts.size > 1) parts[1].toIntOrNull() ?: 8765 else 8765
            repository.setManualConnection(ip, targetPort, pin)
        } else if (pin.isNotEmpty()) {
            repository.setAccessPin(pin)
            repository.reconnectWebSocket()
        }
    }

    fun enableAutoDiscovery() {
        repository.enableAutoDiscovery(_pinInput.value.trim())
    }

    override fun onCleared() {
        super.onCleared()
        repository.close()
    }
}
