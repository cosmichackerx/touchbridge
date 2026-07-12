package com.touchbridge.mobile.domain.repository

import com.touchbridge.mobile.domain.model.ConnectionInfo
import com.touchbridge.mobile.domain.model.DiscoveredDesktop
import com.touchbridge.mobile.domain.model.InputEvent
import com.touchbridge.mobile.domain.model.TouchSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface DiscoveryRepository {
    fun discover(): Flow<List<DiscoveredDesktop>>
    fun stopDiscovery()
}

interface ConnectionRepository {
    val connectionInfo: StateFlow<ConnectionInfo>
    suspend fun connect(host: String, port: Int, pin: String?, deviceName: String): Result<Unit>
    suspend fun disconnect()
    suspend fun sendEvent(event: InputEvent)
    suspend fun sendBinaryMove(dx: Int, dy: Int)
    suspend fun sendBinaryScroll(dx: Int, dy: Int)
}

interface SettingsRepository {
    val settings: Flow<TouchSettings>
    suspend fun updateSettings(settings: TouchSettings)
}
