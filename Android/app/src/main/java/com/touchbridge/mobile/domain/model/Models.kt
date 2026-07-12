package com.touchbridge.mobile.domain.model

data class DiscoveredDesktop(
    val name: String,
    val host: String,
    val port: Int,
    val requiresPin: Boolean
)

enum class ConnectionState {
    Disconnected,
    Discovering,
    Connecting,
    Connected,
    Error
}

data class ConnectionInfo(
    val state: ConnectionState = ConnectionState.Disconnected,
    val desktopName: String? = null,
    val errorMessage: String? = null
)

data class TouchSettings(
    val sensitivity: Float = 1.0f,
    val naturalScroll: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val keepAwake: Boolean = true
)

sealed class InputEvent {
    data class Move(val dx: Int, val dy: Int) : InputEvent()
    data class Scroll(val dx: Int, val dy: Int) : InputEvent()
    data class Click(val button: String, val action: String = "click") : InputEvent()
    data class Key(val code: String, val action: String = "tap") : InputEvent()
    data class Text(val value: String) : InputEvent()
    data class Mode(val name: String) : InputEvent()
}
