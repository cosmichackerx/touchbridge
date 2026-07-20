package com.touchbridge.mobile.presentation.touchpad

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.HapticFeedbackConstants
import com.touchbridge.mobile.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Xbox-style virtual gamepad for portrait and landscape, with selectable skins.
 *
 * Mapping:
 * - Left stick / D-pad → WASD (held while active)
 * - Right stick → continuous mouse look
 * - A/B/X/Y → Space / Esc / Ctrl / Enter (held)
 * - L1/R1 → Q / E · L2/R2 → mouse left / right
 * - Select / Start → Tab / Enter tap
 */
@Composable
fun GamepadPanel(
    onKeyDown: (String) -> Unit,
    onKeyUp: (String) -> Unit,
    onKey: (String) -> Unit,
    onPointerMove: (Float, Float) -> Unit,
    onMouseDown: (String) -> Unit,
    onMouseUp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var skinId by remember { mutableStateOf(GamepadSkinId.Classic) }
    val skin = remember(skinId) { gamepadSkin(skinId) }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val heldKeys = remember { mutableSetOf<String>() }

    var lookX by remember { mutableFloatStateOf(0f) }
    var lookY by remember { mutableFloatStateOf(0f) }
    val latestMove = rememberUpdatedState(onPointerMove)

    LaunchedEffect(Unit) {
        while (isActive) {
            val x = lookX
            val y = lookY
            if (hypot(x, y) > 0.01f) {
                latestMove.value(x * 14f, y * 14f)
            }
            delay(16)
        }
    }

    fun pressKey(code: String) {
        if (heldKeys.add(code)) onKeyDown(code)
    }

    fun releaseKey(code: String) {
        if (heldKeys.remove(code)) onKeyUp(code)
    }

    fun setDirectionalKeys(active: Set<String>) {
        val all = setOf("w", "a", "s", "d")
        (all - active).forEach { releaseKey(it) }
        active.forEach { pressKey(it) }
    }

    DisposableEffect(Unit) {
        onDispose {
            heldKeys.toList().forEach { onKeyUp(it) }
            heldKeys.clear()
            onMouseUp("left")
            onMouseUp("right")
            lookX = 0f
            lookY = 0f
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(skin.shellBrush())
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        SkinPicker(
            current = skinId,
            skin = skin,
            onSelect = { skinId = it }
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(skin.shell)
                .border(
                    width = 1.5.dp,
                    color = skin.glow?.copy(alpha = 0.55f) ?: skin.shellEdge,
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(12.dp)
        ) {
            if (landscape) {
                LandscapePad(
                    skin = skin,
                    onDirKeys = ::setDirectionalKeys,
                    onLook = { x, y -> lookX = x; lookY = y },
                    onHoldKey = { code, down -> if (down) pressKey(code) else releaseKey(code) },
                    onMouse = { btn, down -> if (down) onMouseDown(btn) else onMouseUp(btn) },
                    onTapKey = onKey
                )
            } else {
                PortraitPad(
                    skin = skin,
                    onDirKeys = ::setDirectionalKeys,
                    onLook = { x, y -> lookX = x; lookY = y },
                    onHoldKey = { code, down -> if (down) pressKey(code) else releaseKey(code) },
                    onMouse = { btn, down -> if (down) onMouseDown(btn) else onMouseUp(btn) },
                    onTapKey = onKey
                )
            }
        }
    }
}

@Composable
private fun SkinPicker(
    current: GamepadSkinId,
    skin: GamepadSkin,
    onSelect: (GamepadSkinId) -> Unit
) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GamepadSkinId.all.forEach { id ->
            val option = gamepadSkin(id)
            FilterChip(
                selected = id == current,
                onClick = { onSelect(id) },
                label = {
                    Text(
                        text = stringResource(option.labelRes),
                        color = if (id == current) Color.White else skin.label.copy(alpha = 0.85f)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = skin.shoulder,
                    selectedContainerColor = skin.glow ?: skin.faceX,
                    labelColor = skin.label,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun LandscapePad(
    skin: GamepadSkin,
    onDirKeys: (Set<String>) -> Unit,
    onLook: (Float, Float) -> Unit,
    onHoldKey: (String, Boolean) -> Unit,
    onMouse: (String, Boolean) -> Unit,
    onTapKey: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ShoulderRow(skin = skin, onHoldKey = onHoldKey, onMouse = onMouse)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AnalogStick(
                    skin = skin,
                    size = 128.dp,
                    onVector = { x, y -> onDirKeys(vectorToWasd(x, y)) },
                    onRelease = { onDirKeys(emptySet()) }
                )
                DPad(skin = skin, button = 42.dp, onDirKeys = onDirKeys)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                MetaButton(skin = skin, label = stringResource(R.string.gamepad_select)) {
                    onTapKey("tab")
                }
                MetaButton(skin = skin, label = stringResource(R.string.gamepad_start)) {
                    onTapKey("enter")
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FaceCluster(skin = skin, button = 52.dp, onHoldKey = onHoldKey)
                AnalogStick(
                    skin = skin,
                    size = 110.dp,
                    lookStick = true,
                    onVector = onLook,
                    onRelease = { onLook(0f, 0f) }
                )
            }
        }
    }
}

@Composable
private fun PortraitPad(
    skin: GamepadSkin,
    onDirKeys: (Set<String>) -> Unit,
    onLook: (Float, Float) -> Unit,
    onHoldKey: (String, Boolean) -> Unit,
    onMouse: (String, Boolean) -> Unit,
    onTapKey: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ShoulderRow(skin = skin, onHoldKey = onHoldKey, onMouse = onMouse)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                AnalogStick(
                    skin = skin,
                    size = 120.dp,
                    onVector = { x, y -> onDirKeys(vectorToWasd(x, y)) },
                    onRelease = { onDirKeys(emptySet()) }
                )
                DPad(skin = skin, button = 38.dp, onDirKeys = onDirKeys)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                FaceCluster(skin = skin, button = 48.dp, onHoldKey = onHoldKey)
                AnalogStick(
                    skin = skin,
                    size = 104.dp,
                    lookStick = true,
                    onVector = onLook,
                    onRelease = { onLook(0f, 0f) }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetaButton(skin = skin, label = stringResource(R.string.gamepad_select)) {
                onTapKey("tab")
            }
            Spacer(Modifier.width(16.dp))
            MetaButton(skin = skin, label = stringResource(R.string.gamepad_start)) {
                onTapKey("enter")
            }
        }
    }
}

@Composable
private fun ShoulderRow(
    skin: GamepadSkin,
    onHoldKey: (String, Boolean) -> Unit,
    onMouse: (String, Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ShoulderButton(
            label = stringResource(R.string.gamepad_l2),
            skin = skin,
            modifier = Modifier.weight(1f),
            onPress = { onMouse("left", true) },
            onRelease = { onMouse("left", false) }
        )
        ShoulderButton(
            label = stringResource(R.string.gamepad_l1),
            skin = skin,
            modifier = Modifier.weight(1f),
            onPress = { onHoldKey("q", true) },
            onRelease = { onHoldKey("q", false) }
        )
        ShoulderButton(
            label = stringResource(R.string.gamepad_r1),
            skin = skin,
            modifier = Modifier.weight(1f),
            onPress = { onHoldKey("e", true) },
            onRelease = { onHoldKey("e", false) }
        )
        ShoulderButton(
            label = stringResource(R.string.gamepad_r2),
            skin = skin,
            modifier = Modifier.weight(1f),
            onPress = { onMouse("right", true) },
            onRelease = { onMouse("right", false) }
        )
    }
}

@Composable
private fun ShoulderButton(
    label: String,
    skin: GamepadSkin,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    HoldPressSurface(
        onPress = onPress,
        onRelease = onRelease,
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(skin.shoulder)
            .border(1.dp, skin.shellEdge.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
    ) {
        Text(
            text = label,
            color = skin.label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun FaceCluster(
    skin: GamepadSkin,
    button: Dp,
    onHoldKey: (String, Boolean) -> Unit
) {
    val gap = 8.dp
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FaceButton("Y", skin.faceY, button, { onHoldKey("enter", true) }, { onHoldKey("enter", false) })
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            FaceButton("X", skin.faceX, button, { onHoldKey("ctrl", true) }, { onHoldKey("ctrl", false) })
            Spacer(Modifier.width(button))
            FaceButton("B", skin.faceB, button, { onHoldKey("esc", true) }, { onHoldKey("esc", false) })
        }
        FaceButton("A", skin.faceA, button, { onHoldKey("space", true) }, { onHoldKey("space", false) })
    }
}

@Composable
private fun FaceButton(
    label: String,
    color: Color,
    size: Dp,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    HoldPressSurface(
        onPress = onPress,
        onRelease = onRelease,
        modifier = Modifier
            .size(size)
            .shadow(6.dp, CircleShape, ambientColor = color.copy(alpha = 0.35f))
            .clip(CircleShape)
            .background(color)
            .border(2.dp, color.copy(alpha = 0.9f), CircleShape)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun DPad(
    skin: GamepadSkin,
    button: Dp,
    onDirKeys: (Set<String>) -> Unit
) {
    fun hold(keys: Set<String>, down: Boolean) {
        onDirKeys(if (down) keys else emptySet())
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DPadKey(skin, button, "▲") { down -> hold(setOf("w"), down) }
        Row {
            DPadKey(skin, button, "◀") { down -> hold(setOf("a"), down) }
            Spacer(Modifier.size(button))
            DPadKey(skin, button, "▶") { down -> hold(setOf("d"), down) }
        }
        DPadKey(skin, button, "▼") { down -> hold(setOf("s"), down) }
    }
}

@Composable
private fun DPadKey(
    skin: GamepadSkin,
    size: Dp,
    label: String,
    onHold: (Boolean) -> Unit
) {
    HoldPressSurface(
        onPress = { onHold(true) },
        onRelease = { onHold(false) },
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(skin.dpad)
            .border(1.dp, skin.shellEdge, RoundedCornerShape(10.dp))
    ) {
        Text(text = label, color = skin.label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetaButton(
    skin: GamepadSkin,
    label: String,
    onClick: () -> Unit
) {
    val view = LocalView.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(skin.meta)
            .border(1.dp, skin.shellEdge, RoundedCornerShape(20.dp))
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = skin.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AnalogStick(
    skin: GamepadSkin,
    size: Dp,
    onVector: (Float, Float) -> Unit,
    onRelease: () -> Unit,
    lookStick: Boolean = false
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val latestVector = rememberUpdatedState(onVector)
    val latestRelease = rememberUpdatedState(onRelease)

    Box(
        modifier = Modifier
            .size(size)
            .shadow(
                elevation = if (skin.glow != null) 10.dp else 4.dp,
                shape = CircleShape,
                ambientColor = (skin.glow ?: Color.Black).copy(alpha = 0.35f)
            )
            .clip(CircleShape)
            .background(skin.padWell)
            .border(
                width = if (lookStick) 2.dp else 1.5.dp,
                color = skin.glow?.copy(alpha = 0.65f) ?: skin.shellEdge,
                shape = CircleShape
            )
            .pointerInput(size) {
                val radiusPx = size.toPx() / 2f
                val knobTravel = radiusPx * 0.55f
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
                    fun apply(pos: Offset) {
                        val delta = pos - center
                        val dist = hypot(delta.x, delta.y)
                        val clamped = if (dist > knobTravel && dist > 0f) {
                            delta * (knobTravel / dist)
                        } else {
                            delta
                        }
                        knobOffset = clamped
                        val nx = (clamped.x / knobTravel).coerceIn(-1f, 1f)
                        val ny = (clamped.y / knobTravel).coerceIn(-1f, 1f)
                        if (hypot(nx, ny) < 0.22f) {
                            latestVector.value(0f, 0f)
                        } else {
                            latestVector.value(nx, ny)
                        }
                    }
                    apply(down.position)
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.pressed }
                        if (change != null) {
                            apply(change.position)
                            change.consume()
                        }
                    } while (event.changes.any { it.pressed })
                    knobOffset = Offset.Zero
                    latestRelease.value()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(knobOffset.x.toInt(), knobOffset.y.toInt()) }
                .size(size * 0.42f)
                .clip(CircleShape)
                .background(skin.stickKnob)
                .border(2.dp, skin.stickKnobEdge, CircleShape)
        )
    }
}

/** Map stick vector (−1..1) to WASD set. */
private fun vectorToWasd(x: Float, y: Float): Set<String> {
    val mag = hypot(x, y)
    if (mag < 0.28f) return emptySet()
    val angle = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
    return when {
        angle in -22.5f..22.5f -> setOf("d")
        angle in 22.5f..67.5f -> setOf("d", "s")
        angle in 67.5f..112.5f -> setOf("s")
        angle in 112.5f..157.5f -> setOf("a", "s")
        angle >= 157.5f || angle <= -157.5f -> setOf("a")
        angle in -157.5f..-112.5f -> setOf("a", "w")
        angle in -112.5f..-67.5f -> setOf("w")
        else -> setOf("d", "w")
    }
}
