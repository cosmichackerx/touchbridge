package com.touchbridge.mobile.presentation.touchpad

import android.content.res.Configuration
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.touchbridge.mobile.R
import kotlin.math.abs

/**
 * Mouse-only view: a large trackpad surface with dedicated left/middle/right buttons.
 * Adapts to orientation — buttons sit beside the pad in landscape, below it in portrait.
 * Gestures match the trackpad (slide to move, two fingers to scroll, tap for left click,
 * double-tap / long-press for right click).
 */
@Composable
fun MousePanel(
    onPointerMove: (Float, Float) -> Unit,
    onScroll: (Float, Float) -> Unit,
    onTap: () -> Unit,
    onLeftClick: () -> Unit,
    onMiddleClick: () -> Unit,
    onRightClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val surface: @Composable (Modifier) -> Unit = { m ->
        MouseSurface(
            onPointerMove = onPointerMove,
            onScroll = onScroll,
            onTap = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onTap()
            },
            onRightClick = {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                onRightClick()
            },
            modifier = m
        )
    }

    if (landscape) {
        Row(
            modifier = modifier.fillMaxSize().background(Color.Black).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            surface(Modifier.weight(1f).fillMaxHeight())
            Column(
                modifier = Modifier.width(220.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MouseKey(stringResource(R.string.touchpad_left), Modifier.weight(1.4f).fillMaxWidth()) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onLeftClick()
                }
                MouseKey(stringResource(R.string.touchpad_middle), Modifier.weight(1f).fillMaxWidth()) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onMiddleClick()
                }
                MouseKey(stringResource(R.string.touchpad_right), Modifier.weight(1.4f).fillMaxWidth()) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onRightClick()
                }
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize().background(Color.Black).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            surface(Modifier.weight(1f).fillMaxWidth())
            Row(
                modifier = Modifier.fillMaxWidth().height(88.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MouseKey(stringResource(R.string.touchpad_left), Modifier.weight(1.4f).fillMaxHeight()) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onLeftClick()
                }
                MouseKey(stringResource(R.string.touchpad_middle), Modifier.weight(1f).fillMaxHeight()) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onMiddleClick()
                }
                MouseKey(stringResource(R.string.touchpad_right), Modifier.weight(1.4f).fillMaxHeight()) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onRightClick()
                }
            }
        }
    }
}

@Composable
private fun MouseSurface(
    onPointerMove: (Float, Float) -> Unit,
    onScroll: (Float, Float) -> Unit,
    onTap: () -> Unit,
    onRightClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF14141C))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var lastX = down.position.x
                    var lastY = down.position.y
                    var moved = false
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        when {
                            pressed.size >= 2 -> {
                                moved = true
                                val ptr = pressed[0]
                                onScroll(ptr.position.x - lastX, ptr.position.y - lastY)
                                lastX = ptr.position.x
                                lastY = ptr.position.y
                            }
                            pressed.size == 1 -> {
                                val ptr = pressed[0]
                                val dx = ptr.position.x - lastX
                                val dy = ptr.position.y - lastY
                                if (abs(dx) > 1f || abs(dy) > 1f) {
                                    moved = true
                                    onPointerMove(dx, dy)
                                }
                                lastX = ptr.position.x
                                lastY = ptr.position.y
                            }
                        }
                    } while (event.changes.any { it.pressed })
                    if (!moved) onTap()
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onRightClick() },
                    onLongPress = { onRightClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.mouse_hint),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6A6A7A),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp)
        )
    }
}

@Composable
private fun MouseKey(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF2A2A38))
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium, color = Color(0xFFE8E8F0))
    }
}
