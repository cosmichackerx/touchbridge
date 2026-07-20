package com.touchbridge.mobile.data.net

import com.touchbridge.mobile.data.codec.ProtocolCodec
import com.touchbridge.mobile.data.codec.ProtocolConstants
import com.touchbridge.mobile.domain.model.DiscoveredDesktop
import com.touchbridge.mobile.domain.repository.DiscoveryRepository
import com.touchbridge.mobile.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UdpDiscoveryClient @Inject constructor() : DiscoveryRepository {

    @Volatile
    private var running = false

    override fun discover(): Flow<List<DiscoveredDesktop>> = flow {
        running = true
        val found = LinkedHashMap<String, DiscoveredDesktop>()

        while (currentCoroutineContext().isActive && running) {
            try {
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    socket.soTimeout = 1500
                    val request = ProtocolCodec.discoverRequest().toByteArray()
                    val targets = broadcastTargets()
                    AppLogger.d(
                        "Discovery",
                        "Sending UDP discover on port ${ProtocolConstants.DISCOVERY_PORT} → $targets"
                    )
                    for (target in targets) {
                        try {
                            socket.send(
                                DatagramPacket(
                                    request, request.size,
                                    target,
                                    ProtocolConstants.DISCOVERY_PORT
                                )
                            )
                        } catch (e: Exception) {
                            AppLogger.w("Discovery", "Send to $target failed: ${e.message}")
                        }
                    }

                    val buf = ByteArray(1024)
                    val deadline = System.currentTimeMillis() + 1500
                    while (System.currentTimeMillis() < deadline && currentCoroutineContext().isActive) {
                        try {
                            val reply = DatagramPacket(buf, buf.size)
                            socket.receive(reply)
                            val desktop = parseAnnounce(String(reply.data, 0, reply.length))
                            if (desktop != null) {
                                AppLogger.i(
                                    "Discovery",
                                    "Found: ${desktop.name} @ ${desktop.host}:${desktop.port}"
                                )
                                found[desktop.host] = desktop
                            }
                        } catch (_: Exception) {
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.w("Discovery", "Broadcast failed: ${e.message}")
            }

            emit(found.values.toList())
            delay(2000)
        }
    }.flowOn(Dispatchers.IO)

    override fun stopDiscovery() {
        running = false
    }

    /**
     * Broadcast on every up IPv4 interface (Wi‑Fi, USB tether/RNDIS, etc.).
     * Global 255.255.255.255 often leaves via Wi‑Fi only and misses the USB tether PC.
     */
    private fun broadcastTargets(): List<InetAddress> {
        val targets = linkedSetOf<InetAddress>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return listOf(
                InetAddress.getByName("255.255.255.255")
            )
            for (nif in interfaces) {
                if (!nif.isUp || nif.isLoopback) continue
                for (addr in nif.interfaceAddresses) {
                    val bcast = addr.broadcast ?: continue
                    if (bcast is Inet4Address) targets.add(bcast)
                }
            }
        } catch (e: Exception) {
            AppLogger.w("Discovery", "Interface scan failed: ${e.message}")
        }
        targets.add(InetAddress.getByName("255.255.255.255"))
        return targets.toList()
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
                requiresPin = obj.optBoolean("requiresPin", true),
                link = obj.optString("link", "")
            )
        } catch (_: Exception) {
            null
        }
    }
}
