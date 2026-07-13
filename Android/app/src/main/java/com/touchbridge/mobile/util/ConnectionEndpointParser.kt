package com.touchbridge.mobile.util

import com.touchbridge.mobile.data.codec.ProtocolConstants

/**
 * Parses manual connection input into a WebSocket URL.
 *
 * Supported forms:
 * - wss://host.ngrok-free.app/tb
 * - ws://192.168.1.20:47831/tb
 * - https://host.ngrok-free.app  (converted to wss)
 * - host.ngrok-free.app
 * - 192.168.1.20:47831
 * - 192.168.1.20
 */
object ConnectionEndpointParser {

    data class Endpoint(val webSocketUrl: String)

    fun parse(input: String): Endpoint? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        when {
            trimmed.startsWith("wss://", ignoreCase = true) ||
                trimmed.startsWith("ws://", ignoreCase = true) -> {
                return Endpoint(ensurePath(trimmed))
            }

            trimmed.startsWith("https://", ignoreCase = true) -> {
                val withoutScheme = trimmed.removePrefix("https://").trimEnd('/')
                return Endpoint(ensurePath("wss://$withoutScheme"))
            }

            trimmed.startsWith("http://", ignoreCase = true) -> {
                val withoutScheme = trimmed.removePrefix("http://").trimEnd('/')
                return Endpoint(ensurePath("ws://$withoutScheme"))
            }
        }

        val hostPort = trimmed.split(":", limit = 2)
        if (hostPort.size == 2) {
            val host = hostPort[0]
            val port = hostPort[1].toIntOrNull() ?: return null
            if (host.isBlank() || port !in 1..65535) return null
            val scheme = if (isTunnelHost(host)) "wss" else "ws"
            return Endpoint("$scheme://$host:$port${ProtocolConstants.WS_PATH}")
        }

        val host = hostPort[0]
        if (host.isBlank()) return null

        return if (isTunnelHost(host)) {
            Endpoint("wss://$host${ProtocolConstants.WS_PATH}")
        } else {
            Endpoint("ws://$host:${ProtocolConstants.CONTROL_PORT}${ProtocolConstants.WS_PATH}")
        }
    }

    fun fromDiscoveredHost(host: String, port: Int): Endpoint =
        Endpoint("ws://$host:$port${ProtocolConstants.WS_PATH}")

    private fun ensurePath(url: String): String {
        val base = url.trimEnd('/')
        return if (base.endsWith(ProtocolConstants.WS_PATH)) base else "$base${ProtocolConstants.WS_PATH}"
    }

    private fun isTunnelHost(host: String): Boolean {
        val lower = host.lowercase()
        return lower.contains("ngrok") ||
            lower.endsWith(".ngrok-free.app") ||
            lower.endsWith(".ngrok-free.dev") ||
            lower.endsWith(".ngrok.io") ||
            lower.endsWith(".ngrok.app")
    }
}
