package com.touchbridge.mobile.presentation.touchpad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touchbridge.mobile.domain.model.ConnectionState
import com.touchbridge.mobile.domain.model.InputEvent
import com.touchbridge.mobile.domain.repository.ConnectionRepository
import com.touchbridge.mobile.domain.usecase.ConnectUseCase
import com.touchbridge.mobile.domain.usecase.SendInputUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TouchpadUiState(
    val desktopName: String? = null,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val sensitivity: Float = 1.0f
)

@HiltViewModel
class TouchpadViewModel @Inject constructor(
    private val sendInputUseCase: SendInputUseCase,
    private val connectUseCase: ConnectUseCase,
    connectionRepository: ConnectionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TouchpadUiState())
    val uiState: StateFlow<TouchpadUiState> = _uiState.asStateFlow()

    private var pendingDx = 0f
    private var pendingDy = 0f
    private var flushJob: Job? = null

    init {
        viewModelScope.launch {
            connectionRepository.connectionInfo.collect { info ->
                _uiState.update {
                    it.copy(
                        desktopName = info.desktopName,
                        connectionState = info.state
                    )
                }
            }
        }
    }

    fun startMoveFlush() {
        flushJob?.cancel()
        flushJob = viewModelScope.launch {
            while (isActive) {
                delay(10)
                flushMove()
            }
        }
    }

    fun stopMoveFlush() {
        flushJob?.cancel()
        flushJob = null
    }

    fun onPointerMove(dx: Float, dy: Float) {
        pendingDx += dx * _uiState.value.sensitivity
        pendingDy += dy * _uiState.value.sensitivity
    }

    fun onScroll(dx: Float, dy: Float) {
        viewModelScope.launch {
            sendInputUseCase.sendScroll(
                (dx * 0.5f).toInt(),
                (dy * 0.5f).toInt()
            )
        }
    }

    fun onTap() = sendClick("left")
    fun onLeftClick() = sendClick("left")
    fun onRightClick() = sendClick("right")
    fun onMiddleClick() = sendClick("middle")

    fun onTextInput(text: String) {
        viewModelScope.launch {
            sendInputUseCase.sendEvent(InputEvent.Text(text))
        }
    }

    fun onKey(code: String) {
        viewModelScope.launch {
            sendInputUseCase.sendEvent(InputEvent.Key(code))
        }
    }

    fun disconnect() {
        viewModelScope.launch { connectUseCase.disconnect() }
    }

    private fun sendClick(button: String) {
        viewModelScope.launch {
            sendInputUseCase.sendEvent(InputEvent.Click(button))
        }
    }

    private suspend fun flushMove() {
        val dx = pendingDx.toInt()
        val dy = pendingDy.toInt()
        if (dx == 0 && dy == 0) return
        pendingDx -= dx
        pendingDy -= dy
        sendInputUseCase.sendMove(dx, dy)
    }
}
