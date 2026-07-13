package com.touchbridge.mobile.domain.usecase

import com.touchbridge.mobile.domain.model.DiscoveredDesktop
import com.touchbridge.mobile.domain.model.InputEvent
import com.touchbridge.mobile.domain.repository.ConnectionRepository
import com.touchbridge.mobile.domain.repository.DiscoveryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DiscoverDesktopsUseCase @Inject constructor(
    private val discoveryRepository: DiscoveryRepository
) {
    operator fun invoke(): Flow<List<DiscoveredDesktop>> = discoveryRepository.discover()

    fun stop() = discoveryRepository.stopDiscovery()
}

class ConnectUseCase @Inject constructor(
    private val connectionRepository: ConnectionRepository
) {
    suspend operator fun invoke(
        webSocketUrl: String,
        pin: String?,
        deviceName: String
    ) = connectionRepository.connect(webSocketUrl, pin, deviceName)

    suspend fun disconnect() = connectionRepository.disconnect()
}

class SendInputUseCase @Inject constructor(
    private val connectionRepository: ConnectionRepository
) {
    suspend fun sendEvent(event: InputEvent) = connectionRepository.sendEvent(event)
    suspend fun sendMove(dx: Int, dy: Int) = connectionRepository.sendBinaryMove(dx, dy)
    suspend fun sendScroll(dx: Int, dy: Int) = connectionRepository.sendBinaryScroll(dx, dy)
}
