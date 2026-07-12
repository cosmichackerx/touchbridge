package com.touchbridge.mobile.data.net

import com.touchbridge.mobile.data.codec.ProtocolCodec
import com.touchbridge.mobile.data.codec.ProtocolConstants
import com.touchbridge.mobile.domain.model.DiscoveredDesktop
import com.touchbridge.mobile.domain.repository.DiscoveryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UdpDiscoveryClient @Inject constructor() : DiscoveryRepository {

    private var running = false

    override fun discover(): Flow<List<DiscoveredDesktop>> = flow {
        running = true
        val found = LinkedHashMap<String, DiscoveredDesktop>()

        while (currentCoroutineContext().isActive && running) {
            try {
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    val request = ProtocolCodec.discoverRequest().toByteArray()
                    val packet = DatagramPacket(
                        request, request.size,
                        InetAddress.getByName("255.255.255.255"),
                        ProtocolConstants.DISCOVERY_PORT
                    )
                    socket.soTimeout = 1500
                    socket.send(packet)

                    val buf = ByteArray(1024)
                    val deadline = System.currentTimeMillis() + 1500
                    while (System.currentTimeMillis() < deadline && currentCoroutineContext().isActive) {
                        try {
                            val reply = DatagramPacket(buf, buf.size)
                            socket.receive(reply)
                            val desktop = parseAnnounce(String(reply.data, 0, reply.length))
                            if (desktop != null) {
                                found[desktop.host] = desktop
                            }
                        } catch (_: Exception) {
                            break
                        }
                    }
                }
            } catch (_: Exception) { /* network unavailable */ }

            emit(found.values.toList())
            delay(2000)
        }
    }

    override fun stopDiscovery() {
        running = false
    }

    private fun parseAnnounce(json: String): DiscoveredDesktop? {
        return try {
            val obj = JSONObject(json)
            if (obj.optString("magic") != ProtocolConstants.MAGIC) return null
            if (obj.optString("t") != "announce") return null
            DiscoveredDesktop(
                name = obj.getString("name"),
                host = obj.getString("host"),
                port = obj.getInt("port"),
                requiresPin = obj.optBoolean("requiresPin", true)
            )
        } catch (_: Exception) {
            null
        }
    }
}
