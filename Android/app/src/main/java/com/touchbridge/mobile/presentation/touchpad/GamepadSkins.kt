package com.touchbridge.mobile.presentation.touchpad

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class GamepadSkinId {
    Classic,
    Arctic,
    Neon,
    Midnight,
    Ember;

    companion object {
        val all = entries
    }
}

data class GamepadSkin(
    val id: GamepadSkinId,
    val labelRes: Int,
    val panel: Color,
    val panelGradient: List<Color>,
    val shell: Color,
    val shellEdge: Color,
    val padWell: Color,
    val stickKnob: Color,
    val stickKnobEdge: Color,
    val dpad: Color,
    val faceA: Color,
    val faceB: Color,
    val faceX: Color,
    val faceY: Color,
    val shoulder: Color,
    val meta: Color,
    val label: Color,
    val glow: Color?
)

fun gamepadSkin(id: GamepadSkinId): GamepadSkin = when (id) {
    GamepadSkinId.Classic -> GamepadSkin(
        id = id,
        labelRes = com.touchbridge.mobile.R.string.gamepad_skin_classic,
        panel = Color(0xFF0B0B10),
        panelGradient = listOf(Color(0xFF12121A), Color(0xFF07070C)),
        shell = Color(0xFF1C1C24),
        shellEdge = Color(0xFF2E2E3A),
        padWell = Color(0xFF101018),
        stickKnob = Color(0xFF2A2A34),
        stickKnobEdge = Color(0xFF4A4A58),
        dpad = Color(0xFF252530),
        faceA = Color(0xFF3D9B4A),
        faceB = Color(0xFFC23B3B),
        faceX = Color(0xFF2F6FBF),
        faceY = Color(0xFFD4A017),
        shoulder = Color(0xFF2A2A36),
        meta = Color(0xFF333344),
        label = Color(0xFFE8E8F0),
        glow = null
    )
    GamepadSkinId.Arctic -> GamepadSkin(
        id = id,
        labelRes = com.touchbridge.mobile.R.string.gamepad_skin_arctic,
        panel = Color(0xFFE8ECF2),
        panelGradient = listOf(Color(0xFFF5F7FA), Color(0xFFD9DEE8)),
        shell = Color(0xFFF7F9FC),
        shellEdge = Color(0xFFC5CCD8),
        padWell = Color(0xFFE2E6EE),
        stickKnob = Color(0xFFD0D5E0),
        stickKnobEdge = Color(0xFF9AA3B5),
        dpad = Color(0xFFCBD2DE),
        faceA = Color(0xFF3CB371),
        faceB = Color(0xFFE45757),
        faceX = Color(0xFF4C8DFF),
        faceY = Color(0xFFE6B422),
        shoulder = Color(0xFFDDE2EC),
        meta = Color(0xFFC8CFDB),
        label = Color(0xFF1A1F2A),
        glow = null
    )
    GamepadSkinId.Neon -> GamepadSkin(
        id = id,
        labelRes = com.touchbridge.mobile.R.string.gamepad_skin_neon,
        panel = Color(0xFF05060C),
        panelGradient = listOf(Color(0xFF0A0E1A), Color(0xFF03040A)),
        shell = Color(0xFF0E1320),
        shellEdge = Color(0xFF2FB8FF),
        padWell = Color(0xFF080C14),
        stickKnob = Color(0xFF152033),
        stickKnobEdge = Color(0xFF2FB8FF),
        dpad = Color(0xFF152033),
        faceA = Color(0xFF00E6A0),
        faceB = Color(0xFFFF3D7F),
        faceX = Color(0xFF3DB8FF),
        faceY = Color(0xFFFFE14A),
        shoulder = Color(0xFF152033),
        meta = Color(0xFF1A2438),
        label = Color(0xFFE8F6FF),
        glow = Color(0xFF2FB8FF)
    )
    GamepadSkinId.Midnight -> GamepadSkin(
        id = id,
        labelRes = com.touchbridge.mobile.R.string.gamepad_skin_midnight,
        panel = Color(0xFF0A0614),
        panelGradient = listOf(Color(0xFF160B28), Color(0xFF07040F)),
        shell = Color(0xFF1A0F2C),
        shellEdge = Color(0xFF7A4DFF),
        padWell = Color(0xFF11081E),
        stickKnob = Color(0xFF24163A),
        stickKnobEdge = Color(0xFFB24BFF),
        dpad = Color(0xFF24163A),
        faceA = Color(0xFF5CE5B0),
        faceB = Color(0xFFFF5CA8),
        faceX = Color(0xFF7AB8FF),
        faceY = Color(0xFFFFC14A),
        shoulder = Color(0xFF24163A),
        meta = Color(0xFF2A1A42),
        label = Color(0xFFF0E8FF),
        glow = Color(0xFFB24BFF)
    )
    GamepadSkinId.Ember -> GamepadSkin(
        id = id,
        labelRes = com.touchbridge.mobile.R.string.gamepad_skin_ember,
        panel = Color(0xFF120808),
        panelGradient = listOf(Color(0xFF2A1010), Color(0xFF0A0505)),
        shell = Color(0xFF1C1010),
        shellEdge = Color(0xFFFF5A3D),
        padWell = Color(0xFF140A0A),
        stickKnob = Color(0xFF2A1614),
        stickKnobEdge = Color(0xFFFF7A3D),
        dpad = Color(0xFF2A1614),
        faceA = Color(0xFF4AD47A),
        faceB = Color(0xFFFF4D4D),
        faceX = Color(0xFF4D9BFF),
        faceY = Color(0xFFFFB84D),
        shoulder = Color(0xFF2A1614),
        meta = Color(0xFF3A1E1A),
        label = Color(0xFFFFEDE6),
        glow = Color(0xFFFF5A3D)
    )
}

fun GamepadSkin.shellBrush(): Brush =
    Brush.verticalGradient(panelGradient)
