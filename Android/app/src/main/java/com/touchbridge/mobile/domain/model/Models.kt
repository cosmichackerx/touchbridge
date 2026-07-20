package com.touchbridge.mobile.domain.model

data class DiscoveredDesktop(
    val name: String,
    val host: String,
    val port: Int,
    val requiresPin: Boolean,
    /** "usb" | "lan" from PC announce; empty if unknown. */
    val link: String = ""
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

enum class ControlMode {
    Trackpad,
    Keyboard,
    Mouse,
    Scroll,
    Presentation,
    Media,
    Gamepad;

    /** Wire name used by the desktop protocol (lowercase). */
    val wireName: String get() = name.lowercase()

    companion object {
        fun fromWire(value: String?): ControlMode =
            entries.firstOrNull { it.wireName == value?.trim()?.lowercase() } ?: Trackpad
    }
}

/**
 * Visual skin for the on-screen keyboard. Chosen on the PC and pushed to the phone; the phone
 * never changes it locally.
 */
enum class KeyboardThemeId(val wireName: String) {
    Dark("dark"),
    NeonBlue("neon_blue"),
    NeonPurple("neon_purple"),
    Rgb("rgb"),
    Ocean("ocean"),
    Sunset("sunset"),
    Light("light");

    companion object {
        fun fromWire(value: String?): KeyboardThemeId =
            entries.firstOrNull { it.wireName == value?.trim()?.lowercase() } ?: Dark
    }
}

sealed class InputEvent {
    data class Move(val dx: Int, val dy: Int) : InputEvent()
    data class Scroll(val dx: Int, val dy: Int) : InputEvent()
    data class Click(val button: String, val action: String = "click") : InputEvent()
    data class Key(val code: String, val action: String = "tap") : InputEvent()
    data class Chord(val mods: List<String>, val code: String) : InputEvent()
    data class Text(val value: String) : InputEvent()
    data class Media(val key: String) : InputEvent()
    data class Mode(val name: String) : InputEvent()
}
