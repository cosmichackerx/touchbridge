package com.touchbridge.mobile.presentation.touchpad

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.touchbridge.mobile.R
import com.touchbridge.mobile.domain.model.ConnectionState

@Composable
fun TouchpadScreen(
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TouchpadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val view = LocalView.current
    val focusRequester = remember { FocusRequester() }
    var showKeyboard by remember { mutableStateOf(false) }
    var captureText by remember { mutableStateOf("") }

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
        if (showKeyboard) focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .imePadding()
    ) {
        TopBar(
            desktopName = uiState.desktopName ?: "…",
            isConnected = uiState.connectionState == ConnectionState.Connected,
            onBack = {
                viewModel.disconnect()
                onDisconnect()
            }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.onTap()
                        },
                        onDoubleTap = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.onRightClick()
                        }
                    )
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        var lastX = 0f
                        var lastY = 0f
                        var scrollLastX = 0f
                        var scrollLastY = 0f
                        var scrolling = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            val count = pressed.size

                            when {
                                count >= 2 -> {
                                    scrolling = true
                                    val c = pressed[0]
                                    if (scrollLastX == 0f) {
                                        scrollLastX = c.position.x
                                        scrollLastY = c.position.y
                                    } else {
                                        val sdx = c.position.x - scrollLastX
                                        val sdy = c.position.y - scrollLastY
                                        viewModel.onScroll(sdx, sdy)
                                        scrollLastX = c.position.x
                                        scrollLastY = c.position.y
                                    }
                                }
                                count == 1 && !scrolling -> {
                                    val c = pressed[0]
                                    if (lastX == 0f) {
                                        lastX = c.position.x
                                        lastY = c.position.y
                                    } else {
                                        val dx = c.position.x - lastX
                                        val dy = c.position.y - lastY
                                        viewModel.onPointerMove(dx, dy)
                                        lastX = c.position.x
                                        lastY = c.position.y
                                    }
                                }
                                count == 0 -> {
                                    lastX = 0f
                                    lastY = 0f
                                    scrollLastX = 0f
                                    scrollLastY = 0f
                                    scrolling = false
                                }
                            }
                        }
                    }
                }
        ) {
            if (showKeyboard) {
                BasicTextField(
                    value = captureText,
                    onValueChange = { newText ->
                        if (newText.length > captureText.length) {
                            val added = newText.substring(captureText.length)
                            viewModel.onTextInput(added)
                        } else if (newText.length < captureText.length) {
                            viewModel.onKey("backspace")
                        }
                        captureText = newText
                    },
                    modifier = Modifier
                        .size(1.dp)
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(color = Color.Transparent)
                )
            }
        }

        BottomBar(
            onLeftClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                viewModel.onLeftClick()
            },
            onMiddleClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                viewModel.onMiddleClick()
            },
            onRightClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                viewModel.onRightClick()
            },
            onKeyboardToggle = {
                showKeyboard = !showKeyboard
                if (showKeyboard) focusRequester.requestFocus()
            }
        )
    }
}

@Composable
private fun TopBar(
    desktopName: String,
    isConnected: Boolean,
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
            IconButton(onClick = { }) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun BottomBar(
    onLeftClick: () -> Unit,
    onMiddleClick: () -> Unit,
    onRightClick: () -> Unit,
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
            MouseButton(label = stringResource(R.string.touchpad_left), onClick = onLeftClick)
            MouseButton(label = stringResource(R.string.touchpad_middle), onClick = onMiddleClick)
            MouseButton(label = stringResource(R.string.touchpad_right), onClick = onRightClick)
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
