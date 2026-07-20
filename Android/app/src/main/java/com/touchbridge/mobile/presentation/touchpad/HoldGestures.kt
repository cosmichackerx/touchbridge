package com.touchbridge.mobile.presentation.touchpad

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Press/hold: fire [onPress] once on down, [onRelease] on up.
 * Use for mouse buttons and keys that must stay physically down (e.g. game jump).
 */
@Composable
fun HoldPressSurface(
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onPress()
                try {
                    waitForUpOrCancellation()
                } finally {
                    onRelease()
                }
            }
        },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Fire [onTick] immediately, then keep repeating while the finger stays down.
 * Needed because Windows SendInput does not generate keyboard auto-repeat.
 */
fun Modifier.holdToRepeat(
    enabled: Boolean = true,
    initialDelayMs: Long = 280,
    intervalMs: Long = 55,
    onTick: () -> Unit
): Modifier = composed {
    if (!enabled) return@composed this
    val scope = rememberCoroutineScope()
    pointerInput(initialDelayMs, intervalMs, onTick) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()
            val job = scope.launch {
                onTick()
                delay(initialDelayMs)
                while (isActive) {
                    onTick()
                    delay(intervalMs)
                }
            }
            try {
                waitForUpOrCancellation()
            } finally {
                job.cancel()
            }
        }
    }
}
