package com.touchbridge.mobile.presentation.connect

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touchbridge.mobile.domain.model.ConnectionState
import com.touchbridge.mobile.domain.model.DiscoveredDesktop
import com.touchbridge.mobile.domain.usecase.ConnectUseCase
import com.touchbridge.mobile.domain.usecase.DiscoverDesktopsUseCase
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
        viewModelScope.launch {
            discoverUseCase().collect { desktops ->
                _uiState.update { it.copy(desktops = desktops, isDiscovering = true) }
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
        _uiState.update {
            it.copy(
                selectedDesktop = desktop,
                manualHost = "${desktop.host}:${desktop.port}"
            )
        }
    }

    fun connect() {
        val state = _uiState.value
        val (host, port) = parseHostPort(state) ?: run {
            _uiState.update { it.copy(error = "Invalid host:port") }
            return
        }

        _uiState.update { it.copy(isConnecting = true, error = null) }

        viewModelScope.launch {
            val deviceName = Build.MODEL
            val result = connectUseCase(
                host = host,
                port = port,
                pin = state.pin.ifBlank { null },
                deviceName = deviceName
            )

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(isConnecting = false, navigateToTouchpad = true)
                } else {
                    it.copy(
                        isConnecting = false,
                        error = result.exceptionOrNull()?.message ?: "Connection failed"
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

    private fun parseHostPort(state: ConnectUiState): Pair<String, Int>? {
        state.selectedDesktop?.let { return it.host to it.port }

        val parts = state.manualHost.split(":")
        if (parts.size == 2) {
            val port = parts[1].toIntOrNull() ?: return null
            return parts[0] to port
        }
        if (parts.size == 1 && parts[0].isNotBlank()) {
            return parts[0] to 47831
        }
        return null
    }
}
