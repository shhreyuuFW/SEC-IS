package com.example.sec_is

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket

data class DiscoveredBeacon(
    val ip: String,
    val port: Int,
    val hostname: String
)

class UdpDiscoveryListener {
    fun listenForBeacons(port: Int = 45454): Flow<DiscoveredBeacon> = flow {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket(port).apply {
                broadcast = true
                reuseAddress = true
                soTimeout = 3000 // 3 seconds timeout to check coroutine cancellation
            }
            val buffer = ByteArray(1024)

            while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    val json = JSONObject(text)
                    if (json.optString("service") == "laptop-telemetry") {
                        val payloadIp = json.optString("ip", "").takeIf { it.isNotBlank() }
                        val senderIp = payloadIp ?: packet.address?.hostAddress ?: continue
                        val hostname = json.optString("hostname", "LAPTOP")
                        val servicePort = json.optInt("port", 8765)
                        emit(DiscoveredBeacon(ip = senderIp, port = servicePort, hostname = hostname))
                    }
                } catch (e: Exception) {
                    // Timeout or socket error, continue loop if coroutine still active
                }
            }
        } catch (e: Exception) {
            // Socket creation error
        } finally {
            socket?.close()
        }
    }.flowOn(Dispatchers.IO)
}