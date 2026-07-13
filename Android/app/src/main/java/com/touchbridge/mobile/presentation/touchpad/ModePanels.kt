package com.touchbridge.mobile.presentation.touchpad

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import com.touchbridge.mobile.R
import com.touchbridge.mobile.domain.model.ControlMode

@Composable
fun ModeSwitcher(
    current: ControlMode,
    onSelect: (ControlMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlMode.entries.forEach { mode ->
            FilterChip(
                selected = mode == current,
                onClick = { onSelect(mode) },
                label = { Text(stringResource(mode.labelRes)) }
            )
        }
    }
}

@Composable
fun PresentationPanel(
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ModeScaffold(hint = stringResource(R.string.pres_hint), modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ControlButton(
                label = stringResource(R.string.pres_prev),
                icon = Icons.Filled.SkipPrevious,
                onClick = { onKey("left") },
                modifier = Modifier.weight(1f).height(96.dp)
            )
            ControlButton(
                label = stringResource(R.string.pres_next),
                icon = Icons.Filled.SkipNext,
                onClick = { onKey("right") },
                modifier = Modifier.weight(1f).height(96.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ControlButton(
                label = stringResource(R.string.pres_start),
                icon = Icons.Filled.PlayArrow,
                onClick = { onKey("f5") },
                modifier = Modifier.weight(1f).height(72.dp)
            )
            ControlButton(
                label = stringResource(R.string.pres_end),
                icon = Icons.Filled.Stop,
                onClick = { onKey("esc") },
                modifier = Modifier.weight(1f).height(72.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        ControlButton(
            label = stringResource(R.string.pres_black),
            icon = null,
            onClick = { onKey("b") },
            modifier = Modifier.fillMaxWidth().height(60.dp)
        )
    }
}

@Composable
fun MediaPanel(
    onMedia: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ModeScaffold(hint = stringResource(R.string.media_hint), modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ControlButton(
                label = stringResource(R.string.media_prev),
                icon = Icons.Filled.SkipPrevious,
                onClick = { onMedia("prev") },
                modifier = Modifier.weight(1f).height(96.dp)
            )
            ControlButton(
                label = stringResource(R.string.media_playpause),
                icon = Icons.Filled.PlayArrow,
                onClick = { onMedia("playpause") },
                modifier = Modifier.weight(1.4f).height(96.dp)
            )
            ControlButton(
                label = stringResource(R.string.media_next),
                icon = Icons.Filled.SkipNext,
                onClick = { onMedia("next") },
                modifier = Modifier.weight(1f).height(96.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ControlButton(
                label = stringResource(R.string.media_voldown),
                icon = Icons.Filled.VolumeDown,
                onClick = { onMedia("voldown") },
                modifier = Modifier.weight(1f).height(72.dp)
            )
            ControlButton(
                label = stringResource(R.string.media_mute),
                icon = Icons.Filled.VolumeOff,
                onClick = { onMedia("mute") },
                modifier = Modifier.weight(1f).height(72.dp)
            )
            ControlButton(
                label = stringResource(R.string.media_volup),
                icon = Icons.Filled.VolumeUp,
                onClick = { onMedia("volup") },
                modifier = Modifier.weight(1f).height(72.dp)
            )
        }
    }
}

@Composable
fun GamepadPanel(
    onKeyDown: (String) -> Unit,
    onKeyUp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ModeScaffold(hint = stringResource(R.string.gamepad_hint), modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DPad(onKeyDown = onKeyDown, onKeyUp = onKeyUp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HoldButton(
                    label = stringResource(R.string.gamepad_a),
                    onPress = { onKeyDown("space") },
                    onRelease = { onKeyUp("space") },
                    modifier = Modifier.size(84.dp)
                )
                HoldButton(
                    label = stringResource(R.string.gamepad_b),
                    onPress = { onKeyDown("enter") },
                    onRelease = { onKeyUp("enter") },
                    modifier = Modifier.size(84.dp)
                )
            }
        }
    }
}

@Composable
private fun DPad(
    onKeyDown: (String) -> Unit,
    onKeyUp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HoldIconButton(Icons.Filled.KeyboardArrowUp, { onKeyDown("up") }, { onKeyUp("up") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HoldIconButton(Icons.Filled.KeyboardArrowLeft, { onKeyDown("left") }, { onKeyUp("left") })
            Spacer(Modifier.size(64.dp))
            HoldIconButton(Icons.Filled.KeyboardArrowRight, { onKeyDown("right") }, { onKeyUp("right") })
        }
        HoldIconButton(Icons.Filled.KeyboardArrowDown, { onKeyDown("down") }, { onKeyUp("down") })
    }
}

@Composable
private fun ModeScaffold(
    hint: String,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        content()
        Spacer(Modifier.height(16.dp))
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ControlButton(
    label: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    FilledTonalButton(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onClick()
        },
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(4.dp))
            }
            Text(text = label, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun HoldButton(
    label: String,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    HoldSurface(onPress = onPress, onRelease = onRelease, modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun HoldIconButton(
    icon: ImageVector,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    HoldSurface(onPress = onPress, onRelease = onRelease, modifier = modifier.size(64.dp)) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun HoldSurface(
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown()
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onPress()
                waitForUpOrCancellation()
                onRelease()
            }
        }
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

private val ControlMode.labelRes: Int
    get() = when (this) {
        ControlMode.Trackpad -> R.string.mode_trackpad
        ControlMode.Keyboard -> R.string.mode_keyboard
        ControlMode.Mouse -> R.string.mode_mouse
        ControlMode.Presentation -> R.string.mode_presentation
        ControlMode.Media -> R.string.mode_media
        ControlMode.Gamepad -> R.string.mode_gamepad
    }
