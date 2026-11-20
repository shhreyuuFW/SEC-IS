package com.example.sec_is

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    SYNCED,
    RECONNECTING,
    AUTH_REQUIRED
}

class TelemetryRepository(private val context: Context) {

    companion object {
        private const val SEC_PREFS = "telemetry_security_prefs"
        private const val KEY_SAVED_IP = "saved_ip"
        private const val KEY_SAVED_PORT = "saved_port"
        private const val KEY_ACCESS_PIN = "access_pin"
    }

    private val prefs = context.getSharedPreferences(SEC_PREFS, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _latchedSnapshot = MutableStateFlow<TelemetrySnapshot?>(null)
    val latchedSnapshot: StateFlow<TelemetrySnapshot?> = _latchedSnapshot.asStateFlow()

    private val _connectedIp = MutableStateFlow(prefs.getString(KEY_SAVED_IP, "127.0.0.1") ?: "127.0.0.1")
    val connectedIp: StateFlow<String> = _connectedIp.asStateFlow()

    private val _port = MutableStateFlow(prefs.getInt(KEY_SAVED_PORT, 8765))
    val port: StateFlow<Int> = _port.asStateFlow()

    private val _accessPin = MutableStateFlow(prefs.getString(KEY_ACCESS_PIN, "") ?: "")
    val accessPin: StateFlow<String> = _accessPin.asStateFlow()

    private val fallbackCandidates = listOf("127.0.0.1", "192.168.42.1", "192.168.137.1")
    private var fallbackIndex = 0
    private var hasUdpLock = false

    private val _cpuTempHistory = MutableStateFlow<List<Float>>(emptyList())
    val cpuTempHistory: StateFlow<List<Float>> = _cpuTempHistory.asStateFlow()

    private val _gpuTempHistory = MutableStateFlow<List<Float>>(emptyList())
    val gpuTempHistory: StateFlow<List<Float>> = _gpuTempHistory.asStateFlow()

    private val _iGpuLoadHistory = MutableStateFlow<List<Float>>(emptyList())
    val iGpuLoadHistory: StateFlow<List<Float>> = _iGpuLoadHistory.asStateFlow()

    private val _netSpeedHistory = MutableStateFlow<List<Float>>(emptyList())
    val netSpeedHistory: StateFlow<List<Float>> = _netSpeedHistory.asStateFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var isManualMode = false
    private var autoReconnectJob: Job? = null
    private var udpDiscoveryJob: Job? = null

    init {
        startUdpDiscovery()
        reconnectWebSocket()
        startAutoReconnectLoop()
    }

    fun setAccessPin(pin: String) {
        val clean = pin.trim()
        _accessPin.value = clean
        prefs.edit().putString(KEY_ACCESS_PIN, clean).apply()
    }

    fun setManualConnection(ip: String, targetPort: Int = 8765, pin: String = _accessPin.value) {
        isManualMode = true
        hasUdpLock = false
        val cleanIp = ip.trim()
        val cleanPin = pin.trim()
        if (cleanIp.isNotEmpty()) {
            _connectedIp.value = cleanIp
            _port.value = targetPort
            prefs.edit()
                .putString(KEY_SAVED_IP, cleanIp)
                .putInt(KEY_SAVED_PORT, targetPort)
                .apply()
        }
        if (cleanPin.isNotEmpty()) {
            setAccessPin(cleanPin)
        }
        reconnectWebSocket()
    }

    fun enableAutoDiscovery(pin: String = _accessPin.value) {
        if (pin.isNotBlank()) {
            setAccessPin(pin.trim())
        }
        isManualMode = false
        hasUdpLock = false
        reconnectWebSocket()
    }

    private fun startUdpDiscovery() {
        udpDiscoveryJob?.cancel()
        udpDiscoveryJob = scope.launch {
            UdpDiscoveryListener().listenForBeacons().collect { beacon ->
                if (!isManualMode) {
                    hasUdpLock = true
                    val changed = _connectedIp.value != beacon.ip || _port.value != beacon.port
                    _connectedIp.value = beacon.ip
                    _port.value = beacon.port
                    prefs.edit()
                        .putString(KEY_SAVED_IP, beacon.ip)
                        .putInt(KEY_SAVED_PORT, beacon.port)
                        .apply()

                    if (changed || (_connectionState.value != ConnectionState.SYNCED && _connectionState.value != ConnectionState.AUTH_REQUIRED)) {
                        reconnectWebSocket()
                    }
                }
            }
        }
    }

    private fun startAutoReconnectLoop() {
        autoReconnectJob?.cancel()
        autoReconnectJob = scope.launch {
            while (isActive) {
                delay(2200)
                // Do not spam reconnect if waiting on user to enter 6-digit PIN
                if (_connectionState.value == ConnectionState.RECONNECTING || _connectionState.value == ConnectionState.DISCONNECTED) {
                    if (!isManualMode && !hasUdpLock) {
                        val saved = prefs.getString(KEY_SAVED_IP, null)
                        val candidates = if (!saved.isNullOrBlank()) listOf(saved) + fallbackCandidates else fallbackCandidates
                        _connectedIp.value = candidates[fallbackIndex % candidates.size]
                        fallbackIndex++
                    }
                    reconnectWebSocket()
                }
            }
        }
    }

    @Synchronized
    fun reconnectWebSocket() {
        webSocket?.close(1000, "Reconnecting")
        webSocket = null

        val ip = _connectedIp.value.ifBlank { "127.0.0.1" }
        val p = _port.value
        val pin = _accessPin.value.trim()
        val encodedPin = try { URLEncoder.encode(pin, "UTF-8") } catch (_: Exception) { pin }
        val url = "ws://$ip:$p/?token=$encodedPin"

        if (_latchedSnapshot.value != null) {
            _connectionState.value = ConnectionState.RECONNECTING
        } else {
            _connectionState.value = ConnectionState.CONNECTING
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("X-Telemetry-Token", pin)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = ConnectionState.SYNCED
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val snapshot = TelemetrySnapshot.fromJson(text)
                    _latchedSnapshot.value = snapshot
                    _connectionState.value = ConnectionState.SYNCED

                    updateHistories(snapshot)
                    updateWidgetData(snapshot)
                } catch (_: Exception) {
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (response?.code == 401 || t.message?.contains("401") == true) {
                    _connectionState.value = ConnectionState.AUTH_REQUIRED
                    return
                }
                if (_latchedSnapshot.value != null) {
                    _connectionState.value = ConnectionState.RECONNECTING
                } else {
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (_latchedSnapshot.value != null) {
                    _connectionState.value = ConnectionState.RECONNECTING
                } else {
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            }
        })
    }

    private fun updateHistories(snapshot: TelemetrySnapshot) {
        val maxPoints = 30

        val newCpuTemp = snapshot.cpu.packageTempC.toFloat()
        val currentCpuList = _cpuTempHistory.value.toMutableList()
        currentCpuList.add(newCpuTemp)
        if (currentCpuList.size > maxPoints) currentCpuList.removeAt(0)
        _cpuTempHistory.value = currentCpuList

        val dGpu = snapshot.gpus.firstOrNull { it.type.lowercase(Locale.US) == "dedicated" }
            ?: snapshot.gpus.firstOrNull()
        val newGpuTemp = (dGpu?.coreTempC ?: 0.0).toFloat()
        val currentGpuList = _gpuTempHistory.value.toMutableList()
        currentGpuList.add(newGpuTemp)
        if (currentGpuList.size > maxPoints) currentGpuList.removeAt(0)
        _gpuTempHistory.value = currentGpuList

        val iGpu = snapshot.gpus.firstOrNull { it.type.lowercase(Locale.US) == "integrated" }
        val newIGpuLoad = (iGpu?.loadPercent ?: 0.0).toFloat()
        val currentIGpuList = _iGpuLoadHistory.value.toMutableList()
        currentIGpuList.add(newIGpuLoad)
        if (currentIGpuList.size > maxPoints) currentIGpuList.removeAt(0)
        _iGpuLoadHistory.value = currentIGpuList

        val newNetSpeed = snapshot.network.downloadKbS.toFloat()
        val currentNetList = _netSpeedHistory.value.toMutableList()
        currentNetList.add(newNetSpeed)
        if (currentNetList.size > maxPoints) currentNetList.removeAt(0)
        _netSpeedHistory.value = currentNetList
    }

    private fun updateWidgetData(snapshot: TelemetrySnapshot) {
        val wPrefs = context.getSharedPreferences(TelemetryWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
        val dGpu = snapshot.gpus.firstOrNull { it.type.lowercase(Locale.US) == "dedicated" }
            ?: snapshot.gpus.firstOrNull()

        val cpuLoadStr = String.format(Locale.US, "%.1f%%", snapshot.cpu.overallPercent)
        val cpuTempStr = String.format(Locale.US, "%.1f°C", snapshot.cpu.packageTempC)
        val gpuLoadStr = String.format(Locale.US, "%.1f%%", dGpu?.loadPercent ?: 0.0)
        val gpuTempStr = String.format(Locale.US, "%.1f°C", dGpu?.coreTempC ?: 0.0)
        val ramLoadStr = String.format(Locale.US, "%.1f%%", snapshot.ram.percent)
        val ramUsedStr = String.format(Locale.US, "%.1f GB", snapshot.ram.usedGb)

        wPrefs.edit()
            .putString(TelemetryWidgetProvider.KEY_CPU_LOAD, cpuLoadStr)
            .putString(TelemetryWidgetProvider.KEY_CPU_TEMP, cpuTempStr)
            .putString(TelemetryWidgetProvider.KEY_GPU_LOAD, gpuLoadStr)
            .putString(TelemetryWidgetProvider.KEY_GPU_TEMP, gpuTempStr)
            .putString(TelemetryWidgetProvider.KEY_RAM_LOAD, ramLoadStr)
            .putString(TelemetryWidgetProvider.KEY_RAM_USED, ramUsedStr)
            .apply()

        TelemetryWidgetProvider.updateAllWidgets(context)
    }

    fun close() {
        udpDiscoveryJob?.cancel()
        autoReconnectJob?.cancel()
        webSocket?.close(1000, "App closed")
    }
}
