package com.touchbridge.mobile.presentation.touchpad

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.automirrored.filled.StopScreenShare
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.touchbridge.mobile.R
import com.touchbridge.mobile.domain.model.ConnectionState
import com.touchbridge.mobile.domain.model.ControlMode
import com.touchbridge.mobile.presentation.common.DebugLogPanel
import kotlinx.coroutines.delay

@Composable
fun TouchpadScreen(
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TouchpadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val screenBitmap by viewModel.screenBitmap.collectAsState()
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var showKeyboard by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }
    var captureText by remember { mutableStateOf("") }
    // In Keyboard mode the top bar + mode switcher auto-hide so the keyboard goes full-screen.
    var chromeVisible by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.connectionState) {
        if (uiState.connectionState == ConnectionState.Disconnected) {
            onDisconnect()
        }
    }

    DisposableEffect(Unit) {
        viewModel.startMoveFlush()
        onDispose { viewModel.stopMoveFlush() }
    }

    LaunchedEffect(showKeyboard) {
        if (showKeyboard) {
            captureText = ""
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
            captureText = ""
        }
    }

    // Leaving the trackpad hides the soft keyboard so it doesn't linger over other panels.
    LaunchedEffect(uiState.mode) {
        if (uiState.mode != ControlMode.Trackpad) showKeyboard = false
        // Chrome is always shown outside Keyboard mode; entering Keyboard shows it, then it hides.
        chromeVisible = true
    }

    // Auto-hide the chrome 3s after it becomes visible while in Keyboard mode.
    LaunchedEffect(uiState.mode, chromeVisible) {
        if (uiState.mode == ControlMode.Keyboard && chromeVisible) {
            delay(3000)
            chromeVisible = false
        }
    }

    // Every mode works in both portrait and landscape — let the phone rotate freely and each
    // panel adapts its layout to the current orientation.
    val context = LocalContext.current
    DisposableEffect(Unit) {
        (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        onDispose {
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val chromeShown = uiState.mode != ControlMode.Keyboard || chromeVisible

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .imePadding()
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = chromeShown,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                TopBar(
                    desktopName = uiState.desktopName ?: "…",
                    isConnected = uiState.connectionState == ConnectionState.Connected,
                    showLogs = showLogs,
                    onToggleLogs = { showLogs = !showLogs },
                    screenOn = uiState.screenOn,
                    onToggleScreen = viewModel::toggleScreen,
                    onBack = {
                        viewModel.disconnect()
                        onDisconnect()
                    }
                )

                ModeSwitcher(
                    current = uiState.mode,
                    onSelect = viewModel::setMode,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (uiState.mode) {
                ControlMode.Trackpad -> TrackpadSurface(
                    screenBitmap = if (uiState.screenOn) screenBitmap else null,
                    showKeyboard = showKeyboard,
                    captureText = captureText,
                    focusRequester = focusRequester,
                    onCaptureTextChange = { captureText = it },
                    onTextInput = viewModel::onTextInput,
                    onBackspace = { viewModel.onKey("backspace") },
                    onScroll = viewModel::onScroll,
                    onPointerMove = viewModel::onPointerMove,
                    onTap = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        viewModel.onTap()
                    }
                )
                ControlMode.Keyboard -> KeyboardPanel(
                    onChar = viewModel::onTextInput,
                    onKey = viewModel::onKey,
                    onChord = viewModel::onChord,
                    theme = keyboardTheme(uiState.keyboardTheme)
                )
                ControlMode.Mouse -> MousePanel(
                    onPointerMove = viewModel::onPointerMove,
                    onScroll = viewModel::onScroll,
                    onTap = viewModel::onTap,
                    onMouseDown = viewModel::onMouseDown,
                    onMouseUp = viewModel::onMouseUp
                )
                ControlMode.Scroll -> ScrollPanel(onScroll = viewModel::onScroll)
                ControlMode.Presentation -> PresentationPanel(onKey = viewModel::onKey)
                ControlMode.Media -> MediaPanel(onMedia = viewModel::onMediaKey)
                ControlMode.Gamepad -> GamepadPanel(
                    onKey = viewModel::onKey,
                    onKeyDown = viewModel::onKeyDown,
                    onKeyUp = viewModel::onKeyUp,
                    onPointerMove = viewModel::onPointerMove,
                    onMouseDown = viewModel::onMouseDown,
                    onMouseUp = viewModel::onMouseUp
                )
            }
        }

        if (showLogs) {
            DebugLogPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                onHide = { showLogs = false }
            )
        }

        if (uiState.mode == ControlMode.Trackpad) {
            BottomBar(
                onMouseDown = viewModel::onMouseDown,
                onMouseUp = viewModel::onMouseUp,
                onKeyboardToggle = {
                    showKeyboard = !showKeyboard
                }
            )
        }
      }

      // While the chrome is hidden in Keyboard mode, a thin top handle brings it back on swipe-down.
      if (uiState.mode == ControlMode.Keyboard && !chromeVisible) {
          RevealHandle(
              onReveal = { chromeVisible = true },
              modifier = Modifier.align(Alignment.TopCenter)
          )
      }
    }
}

@Composable
private fun RevealHandle(
    onReveal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    if (dragAmount > 0f) {
                        change.consume()
                        onReveal()
                    }
                }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(width = 44.dp, height = 5.dp)
                .background(Color(0x66FFFFFF), RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun TrackpadSurface(
    showKeyboard: Boolean,
    captureText: String,
    focusRequester: FocusRequester,
    onCaptureTextChange: (String) -> Unit,
    onTextInput: (String) -> Unit,
    onBackspace: () -> Unit,
    onScroll: (Float, Float) -> Unit,
    onPointerMove: (Float, Float) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    screenBitmap: ImageBitmap? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                // Two fingers = scroll only. Right-click is via the R button — not gestures.
                detectLaptopTrackpadGestures(
                    onPointerMove = onPointerMove,
                    onScroll = onScroll,
                    onTap = onTap
                )
            }
    ) {
        screenBitmap?.let { frame ->
            Image(
                bitmap = frame,
                contentDescription = stringResource(R.string.touchpad_screen_view),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        if (showKeyboard) {
            BasicTextField(
                value = captureText,
                onValueChange = { newText ->
                    if (newText.length > captureText.length) {
                        onTextInput(newText.substring(captureText.length))
                    } else if (newText.length < captureText.length) {
                        onBackspace()
                    }
                    onCaptureTextChange(newText)
                },
                modifier = Modifier
                    .size(1.dp)
                    .focusRequester(focusRequester),
                textStyle = TextStyle(color = Color.Transparent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                keyboardActions = KeyboardActions()
            )
        }
    }
}

@Composable
private fun TopBar(
    desktopName: String,
    isConnected: Boolean,
    showLogs: Boolean,
    onToggleLogs: () -> Unit,
    screenOn: Boolean,
    onToggleScreen: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.touchpad_back),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(8.dp)
                        .background(
                            color = if (isConnected) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.error,
                            shape = CircleShape
                        )
                )
                Text(
                    text = desktopName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onToggleScreen, enabled = isConnected) {
                Icon(
                    if (screenOn) Icons.AutoMirrored.Filled.StopScreenShare else Icons.AutoMirrored.Filled.ScreenShare,
                    contentDescription = stringResource(
                        if (screenOn) R.string.touchpad_screen_hide else R.string.touchpad_screen_show
                    ),
                    tint = if (screenOn) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onToggleLogs) {
                Icon(
                    Icons.Default.BugReport,
                    contentDescription = stringResource(
                        if (showLogs) R.string.debug_hide_logs else R.string.debug_show_logs
                    ),
                    tint = if (showLogs) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun BottomBar(
    onMouseDown: (String) -> Unit,
    onMouseUp: (String) -> Unit,
    onKeyboardToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MouseButton(
                label = stringResource(R.string.touchpad_left),
                onPress = { onMouseDown("left") },
                onRelease = { onMouseUp("left") }
            )
            MouseButton(
                label = stringResource(R.string.touchpad_middle),
                onPress = { onMouseDown("middle") },
                onRelease = { onMouseUp("middle") }
            )
            MouseButton(
                label = stringResource(R.string.touchpad_right),
                onPress = { onMouseDown("right") },
                onRelease = { onMouseUp("right") }
            )
            IconButton(onClick = onKeyboardToggle) {
                Icon(
                    Icons.Default.Keyboard,
                    contentDescription = stringResource(R.string.touchpad_keyboard),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun MouseButton(
    label: String,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    HoldPressSurface(
        onPress = onPress,
        onRelease = onRelease,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
