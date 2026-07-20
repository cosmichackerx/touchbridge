package com.touchbridge.mobile.presentation.touchpad

import android.content.res.Configuration
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
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

/**
 * Mouse-only view: trackpad surface + holdable L/M/R buttons (press = down, release = up).
 */
@Composable
fun MousePanel(
    onPointerMove: (Float, Float) -> Unit,
    onScroll: (Float, Float) -> Unit,
    onTap: () -> Unit,
    onMouseDown: (String) -> Unit,
    onMouseUp: (String) -> Unit,
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
                MouseKey(
                    stringResource(R.string.touchpad_left),
                    Modifier.weight(1.4f).fillMaxWidth(),
                    onPress = { onMouseDown("left") },
                    onRelease = { onMouseUp("left") }
                )
                MouseKey(
                    stringResource(R.string.touchpad_middle),
                    Modifier.weight(1f).fillMaxWidth(),
                    onPress = { onMouseDown("middle") },
                    onRelease = { onMouseUp("middle") }
                )
                MouseKey(
                    stringResource(R.string.touchpad_right),
                    Modifier.weight(1.4f).fillMaxWidth(),
                    onPress = { onMouseDown("right") },
                    onRelease = { onMouseUp("right") }
                )
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
                MouseKey(
                    stringResource(R.string.touchpad_left),
                    Modifier.weight(1.4f).fillMaxHeight(),
                    onPress = { onMouseDown("left") },
                    onRelease = { onMouseUp("left") }
                )
                MouseKey(
                    stringResource(R.string.touchpad_middle),
                    Modifier.weight(1f).fillMaxHeight(),
                    onPress = { onMouseDown("middle") },
                    onRelease = { onMouseUp("middle") }
                )
                MouseKey(
                    stringResource(R.string.touchpad_right),
                    Modifier.weight(1.4f).fillMaxHeight(),
                    onPress = { onMouseDown("right") },
                    onRelease = { onMouseUp("right") }
                )
            }
        }
    }
}

@Composable
private fun MouseSurface(
    onPointerMove: (Float, Float) -> Unit,
    onScroll: (Float, Float) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF14141C))
            .pointerInput(Unit) {
                detectLaptopTrackpadGestures(
                    onPointerMove = onPointerMove,
                    onScroll = onScroll,
                    onTap = onTap
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
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF2A2A38)),
        contentAlignment = Alignment.Center
    ) {
        HoldPressSurface(
            onPress = onPress,
            onRelease = onRelease,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(text = label, style = MaterialTheme.typography.titleMedium, color = Color(0xFFE8E8F0))
        }
    }
}
