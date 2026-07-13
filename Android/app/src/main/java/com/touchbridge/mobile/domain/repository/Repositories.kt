package com.touchbridge.mobile.domain.repository

import com.touchbridge.mobile.domain.model.ConnectionInfo
import com.touchbridge.mobile.domain.model.ControlMode
import com.touchbridge.mobile.domain.model.DiscoveredDesktop
import com.touchbridge.mobile.domain.model.InputEvent
import com.touchbridge.mobile.domain.model.KeyboardThemeId
import com.touchbridge.mobile.domain.model.TouchSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface DiscoveryRepository {
    fun discover(): Flow<List<DiscoveredDesktop>>
    fun stopDiscovery()
}

interface ConnectionRepository {
    val connectionInfo: StateFlow<ConnectionInfo>

    /** Mode pushed by the desktop (PC bar) so the phone layout stays in sync. */
    val incomingMode: StateFlow<ControlMode>

    /** Keyboard skin pushed by the desktop; the phone only applies it. */
    val incomingKeyboardTheme: StateFlow<KeyboardThemeId>

    /** Latest live screen frame from the desktop as raw JPEG bytes, or null when off. */
    val screenFrame: StateFlow<ByteArray?>
    suspend fun connect(webSocketUrl: String, pin: String?, deviceName: String): Result<Unit>
    suspend fun disconnect()
    suspend fun sendEvent(event: InputEvent)
    suspend fun sendBinaryMove(dx: Int, dy: Int)
    suspend fun sendBinaryScroll(dx: Int, dy: Int)

    /** Ask the desktop to start/stop mirroring its screen. */
    suspend fun setScreenStream(on: Boolean)
}

interface SettingsRepository {
    val settings: Flow<TouchSettings>
    suspend fun updateSettings(settings: TouchSettings)
}
