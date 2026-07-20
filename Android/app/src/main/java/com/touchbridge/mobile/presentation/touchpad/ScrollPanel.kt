package com.touchbridge.mobile.presentation.touchpad

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.touchbridge.mobile.R

/** Dedicated scroll mode: drag anywhere to scroll, or hold arrow buttons to keep scrolling. */
@Composable
fun ScrollPanel(
    onScroll: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val notch = 150f

    fun tick(dx: Float, dy: Float) {
        onScroll(dx, dy)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF14141C))
                .pointerInput(Unit) {
                    detectScrollSurfaceGestures { dx, dy -> onScroll(dx, dy) }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.scroll_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6A6A7A),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScrollNotch(
                icon = Icons.Filled.KeyboardArrowLeft,
                label = stringResource(R.string.scroll_left),
                modifier = Modifier.weight(1f).height(64.dp),
                onTick = { tick(-notch, 0f) }
            )
            Column(
                modifier = Modifier.weight(1.2f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScrollNotch(
                    icon = Icons.Filled.KeyboardArrowUp,
                    label = stringResource(R.string.scroll_up),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    onTick = { tick(0f, -notch) }
                )
                ScrollNotch(
                    icon = Icons.Filled.KeyboardArrowDown,
                    label = stringResource(R.string.scroll_down),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    onTick = { tick(0f, notch) }
                )
            }
            ScrollNotch(
                icon = Icons.Filled.KeyboardArrowRight,
                label = stringResource(R.string.scroll_right),
                modifier = Modifier.weight(1f).height(64.dp),
                onTick = { tick(notch, 0f) }
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun ScrollNotch(
    icon: ImageVector,
    label: String,
    onTick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF2A2A38))
            .holdToRepeat(initialDelayMs = 220, intervalMs = 40) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onTick()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = Color(0xFFE8E8F0), modifier = Modifier.size(28.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFFB0B0C0))
        }
    }
}
