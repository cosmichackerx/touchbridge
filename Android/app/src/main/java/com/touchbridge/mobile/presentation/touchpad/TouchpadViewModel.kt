package com.touchbridge.mobile.presentation.touchpad

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touchbridge.mobile.domain.model.ConnectionState
import com.touchbridge.mobile.domain.model.ControlMode
import com.touchbridge.mobile.domain.model.InputEvent
import com.touchbridge.mobile.domain.model.KeyboardThemeId
import com.touchbridge.mobile.domain.repository.ConnectionRepository
import com.touchbridge.mobile.domain.usecase.ConnectUseCase
import com.touchbridge.mobile.domain.usecase.SendInputUseCase
import com.touchbridge.mobile.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TouchpadUiState(
    val desktopName: String? = null,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val sensitivity: Float = 1.0f,
    val mode: ControlMode = ControlMode.Trackpad,
    val screenOn: Boolean = false,
    val keyboardTheme: KeyboardThemeId = KeyboardThemeId.Dark
)

@HiltViewModel
class TouchpadViewModel @Inject constructor(
    private val sendInputUseCase: SendInputUseCase,
    private val connectUseCase: ConnectUseCase,
    private val connectionRepository: ConnectionRepository
) : ViewModel() {

    /** Live desktop frames decoded off the main thread for the trackpad background. */
    val screenBitmap: StateFlow<ImageBitmap?> = connectionRepository.screenFrame
        .map { bytes ->
            bytes?.let {
                runCatching { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }.getOrNull()
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow(TouchpadUiState())
    val uiState: StateFlow<TouchpadUiState> = _uiState.asStateFlow()

    private var pendingDx = 0f
    private var pendingDy = 0f
    private var flushJob: Job? = null

    init {
        AppLogger.i("Touchpad", "Screen opened")
        viewModelScope.launch {
            connectionRepository.connectionInfo.collect { info ->
                AppLogger.d("Touchpad", "Connection state: ${info.state}, desktop=${info.desktopName}")
                val stillConnected = info.state == ConnectionState.Connected
                _uiState.update {
                    it.copy(
                        desktopName = info.desktopName,
                        connectionState = info.state,
                        screenOn = it.screenOn && stillConnected
                    )
                }
            }
        }
        // PC bar → phone: reflect the mode chosen on the desktop without echoing it back.
        viewModelScope.launch {
            connectionRepository.incomingMode.collect { mode ->
                if (mode != _uiState.value.mode) {
                    AppLogger.i("Touchpad", "Applying desktop mode: $mode")
                    _uiState.update { it.copy(mode = mode) }
                }
            }
        }
        // PC → phone: apply the keyboard skin chosen on the desktop.
        viewModelScope.launch {
            connectionRepository.incomingKeyboardTheme.collect { theme ->
                if (theme != _uiState.value.keyboardTheme) {
                    AppLogger.i("Touchpad", "Applying keyboard theme: $theme")
                    _uiState.update { it.copy(keyboardTheme = theme) }
                }
            }
        }
    }

    fun setMode(mode: ControlMode) {
        if (mode == _uiState.value.mode) return
        AppLogger.i("Touchpad", "Mode selected on phone: $mode")
        _uiState.update { it.copy(mode = mode) }
        viewModelScope.launch {
            sendInputUseCase.sendEvent(InputEvent.Mode(mode.wireName))
        }
    }

    /** Toggle live mirroring of the desktop screen behind the trackpad. */
    fun toggleScreen() {
        val newOn = !_uiState.value.screenOn
        AppLogger.i("Touchpad", "Screen view toggled -> $newOn")
        _uiState.update { it.copy(screenOn = newOn) }
        viewModelScope.launch { connectionRepository.setScreenStream(newOn) }
    }

    /** Presentation controls send tap key events the desktop already understands. */
    fun onPresentationKey(code: String) {
        AppLogger.d("Input", "presentation key: $code")
        viewModelScope.launch {
            sendInputUseCase.sendEvent(InputEvent.Key(code))
        }
    }

    fun onMediaKey(key: String) {
        AppLogger.d("Input", "media key: $key")
        viewModelScope.launch {
            sendInputUseCase.sendEvent(InputEvent.Media(key))
        }
    }

    /** Gamepad buttons hold the key down until released for continuous input. */
    fun onKeyDown(code: String) {
        AppLogger.d("Input", "key down: $code")
        viewModelScope.launch {
            sendInputUseCase.sendEvent(InputEvent.Key(code, action = "down"))
        }
    }

    fun onKeyUp(code: String) {
        AppLogger.d("Input", "key up: $code")
        viewModelScope.launch {
            sendInputUseCase.sendEvent(InputEvent.Key(code, action = "up"))
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
        AppLogger.d("Input", "scroll dx=$dx dy=$dy")
        viewModelScope.launch {
            sendInputUseCase.sendScroll(
                (dx * 0.5f).toInt(),
                (dy * 0.5f).toInt()
            )
        }
    }

    fun onTap() {
        AppLogger.d("Input", "tap → left click")
        sendClick("left")
    }
    fun onLeftClick() {
        AppLogger.d("Input", "left click")
        sendClick("left")
    }
    fun onRightClick() {
        AppLogger.d("Input", "right click")
        sendClick("right")
    }
    fun onMiddleClick() {
        AppLogger.d("Input", "middle click")
        sendClick("middle")
    }

    fun onTextInput(text: String) {
        AppLogger.d("Input", "text: \"$text\"")
        viewModelScope.launch {
            sendInputUseCase.sendEvent(InputEvent.Text(text))
        }
    }

    fun onKey(code: String) {
        AppLogger.d("Input", "key: $code")
        viewModelScope.launch {
            sendInputUseCase.sendEvent(InputEvent.Key(code))
        }
    }

    /** A full-keyboard key press with active modifiers (e.g. Ctrl+C). */
    fun onChord(mods: List<String>, code: String) {
        AppLogger.d("Input", "chord: ${mods.joinToString("+")}+$code")
        viewModelScope.launch {
            sendInputUseCase.sendEvent(InputEvent.Chord(mods, code))
        }
    }

    fun disconnect() {
        AppLogger.i("Touchpad", "Disconnect requested")
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
