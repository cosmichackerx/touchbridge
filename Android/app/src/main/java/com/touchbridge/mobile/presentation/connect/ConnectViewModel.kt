package com.touchbridge.mobile.presentation.connect

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touchbridge.mobile.domain.model.ConnectionState
import com.touchbridge.mobile.domain.model.DiscoveredDesktop
import com.touchbridge.mobile.domain.usecase.ConnectUseCase
import com.touchbridge.mobile.domain.usecase.DiscoverDesktopsUseCase
import com.touchbridge.mobile.util.AppLogger
import com.touchbridge.mobile.util.ConnectionEndpointParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectUiState(
    val desktops: List<DiscoveredDesktop> = emptyList(),
    val isDiscovering: Boolean = true,
    val isConnecting: Boolean = false,
    val manualHost: String = "",
    val pin: String = "",
    val selectedDesktop: DiscoveredDesktop? = null,
    val error: String? = null,
    val navigateToTouchpad: Boolean = false
)

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val discoverUseCase: DiscoverDesktopsUseCase,
    private val connectUseCase: ConnectUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState: StateFlow<ConnectUiState> = _uiState.asStateFlow()

    init {
        AppLogger.i("Connect", "Starting desktop discovery")
        viewModelScope.launch {
            discoverUseCase().collect { desktops ->
                val ordered = desktops.sortedByDescending { it.link.equals("usb", ignoreCase = true) }
                AppLogger.d("Connect", "Found ${ordered.size} desktop(s): ${ordered.map { it.name }}")
                _uiState.update { it.copy(desktops = ordered, isDiscovering = true) }
            }
        }
    }

    fun setManualHost(value: String) {
        _uiState.update { it.copy(manualHost = value, selectedDesktop = null) }
    }

    fun setPin(value: String) {
        _uiState.update { it.copy(pin = value) }
    }

    fun selectDesktop(desktop: DiscoveredDesktop) {
        AppLogger.i("Connect", "Selected desktop: ${desktop.name} @ ${desktop.host}:${desktop.port}")
        _uiState.update {
            it.copy(
                selectedDesktop = desktop,
                manualHost = "${desktop.host}:${desktop.port}"
            )
        }
    }

    fun connect() {
        val state = _uiState.value
        val endpoint = resolveEndpoint(state) ?: run {
            _uiState.update { it.copy(error = "Invalid address") }
            return
        }

        _uiState.update { it.copy(isConnecting = true, error = null) }
        AppLogger.i(
            "Connect",
            "Connecting to ${endpoint.webSocketUrl} (pin=${if (state.pin.isBlank()) "none" else "***"})"
        )

        viewModelScope.launch {
            val deviceName = Build.MODEL
            val result = connectUseCase(
                webSocketUrl = endpoint.webSocketUrl,
                pin = state.pin.ifBlank { null },
                deviceName = deviceName
            )

            _uiState.update {
                if (result.isSuccess) {
                    AppLogger.i("Connect", "Connected successfully")
                    it.copy(isConnecting = false, navigateToTouchpad = true)
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Connection failed"
                    AppLogger.e("Connect", "Connection failed: $err")
                    it.copy(
                        isConnecting = false,
                        error = err
                    )
                }
            }
        }
    }

    fun onNavigated() {
        _uiState.update { it.copy(navigateToTouchpad = false) }
    }

    override fun onCleared() {
        super.onCleared()
        discoverUseCase.stop()
    }

    private fun resolveEndpoint(state: ConnectUiState): ConnectionEndpointParser.Endpoint? {
        state.selectedDesktop?.let {
            return ConnectionEndpointParser.fromDiscoveredHost(it.host, it.port)
        }
        return ConnectionEndpointParser.parse(state.manualHost)
    }
}
