package com.touchbridge.mobile.presentation.touchpad

import android.content.res.Configuration
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Full desktop-style keyboard. Adapts to orientation: the numpad shows alongside the main
 * block in landscape and is hidden in portrait so the letter keys stay comfortably sized.
 * Modifier keys (Ctrl/Alt/Win/Shift) act as one-shot stickies: tap a modifier, then a key, and
 * it clears automatically — Caps Lock is a real toggle handled by the PC. Printable keys are
 * sent as text so symbols type reliably regardless of the PC layout; shortcuts (Ctrl/Alt/Win +
 * key) are sent as chords.
 */
@Composable
fun KeyboardPanel(
    onChar: (String) -> Unit,
    onKey: (String) -> Unit,
    onChord: (List<String>, String) -> Unit,
    theme: KbTheme,
    modifier: Modifier = Modifier
) {
    var activeMods by remember { mutableStateOf(emptySet<String>()) }

    fun clearOneShot() {
        if (activeMods.isNotEmpty()) activeMods = emptySet()
    }

    fun press(cap: Cap) {
        when (cap) {
            is Cap.Gap -> {}
            is Cap.Mod -> {
                activeMods = if (cap.mod in activeMods) activeMods - cap.mod else activeMods + cap.mod
            }
            is Cap.Char -> {
                val shortcutMods = activeMods - "shift"
                if (shortcutMods.isNotEmpty()) {
                    onChord(activeMods.toList(), cap.base.lowercase())
                    clearOneShot()
                } else if ("shift" in activeMods) {
                    onChar(cap.shifted)
                    clearOneShot()
                } else {
                    onChar(cap.base)
                }
            }
            is Cap.Named -> {
                if (activeMods.isNotEmpty()) {
                    onChord(activeMods.toList(), cap.code)
                    clearOneShot()
                } else {
                    onKey(cap.code)
                }
            }
        }
    }

    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(theme.panel)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Main block
        Column(
            modifier = Modifier.weight(15f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            mainRows.forEachIndexed { i, row ->
                KeyRow(row, activeMods, ::press, theme, theme.accentFor(i), Modifier.weight(1f))
            }
        }
        // Numpad — only when there's room (landscape).
        if (landscape) {
            Column(
                modifier = Modifier.weight(3.2f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                numpadRows.forEachIndexed { i, row ->
                    KeyRow(row, activeMods, ::press, theme, theme.accentFor(i), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun KeyRow(
    caps: List<Cap>,
    activeMods: Set<String>,
    onPress: (Cap) -> Unit,
    theme: KbTheme,
    accent: Color?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        caps.forEach { cap ->
            if (cap is Cap.Gap) {
                Spacer(Modifier.weight(cap.weight))
            } else {
                KeyButton(
                    cap = cap,
                    active = cap is Cap.Mod && cap.mod in activeMods,
                    theme = theme,
                    accent = accent,
                    onPress = { onPress(cap) },
                    modifier = Modifier.weight(cap.weight)
                )
            }
        }
    }
}

@Composable
private fun KeyButton(
    cap: Cap,
    active: Boolean,
    theme: KbTheme,
    accent: Color?,
    onPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val shape = RoundedCornerShape(6.dp)
    val bg = if (active) theme.keyFillActive else theme.keyFill
    val fg = when {
        active -> theme.textActive
        theme.tintLabels && accent != null -> accent
        else -> theme.text
    }
    val borderMod = if (accent != null) {
        Modifier.border(1.5.dp, accent.copy(alpha = if (active) 1f else 0.85f), shape)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(bg)
            .then(borderMod)
            .pointerInput(cap) {
                detectTapGestures(onTap = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onPress()
                })
            },
        contentAlignment = Alignment.Center
    ) {
        when (cap) {
            is Cap.Char -> {
                val topLabel = cap.topLabel
                if (topLabel != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.material3.Text(topLabel, color = fg.copy(alpha = 0.7f), fontSize = 8.sp, lineHeight = 9.sp)
                        androidx.compose.material3.Text(cap.mainLabel, color = fg, fontSize = 12.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium)
                    }
                } else {
                    androidx.compose.material3.Text(cap.mainLabel, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
            is Cap.Named -> androidx.compose.material3.Text(
                cap.label, color = fg, fontSize = 10.sp, lineHeight = 11.sp,
                fontWeight = FontWeight.Medium, textAlign = TextAlign.Center
            )
            is Cap.Mod -> androidx.compose.material3.Text(
                cap.label, color = fg, fontSize = 10.sp, lineHeight = 11.sp,
                fontWeight = FontWeight.Medium, textAlign = TextAlign.Center
            )
            is Cap.Gap -> {}
        }
    }
}

/** A key definition. Widths are relative weights within a row. */
private sealed interface Cap {
    val weight: Float

    /** Printable key. [base] is typed normally; [shifted] when Shift is held. */
    data class Char(val base: String, val shifted: String, override val weight: Float = 1f) : Cap {
        val isLetter get() = base.length == 1 && base[0].isLetter()
        val mainLabel get() = if (isLetter) base.uppercase() else base
        val topLabel: String? get() = if (isLetter) null else shifted.takeIf { it != base }
    }

    /** Non-printable key sent by code (enter, tab, arrows, F-keys…). */
    data class Named(val label: String, val code: String, override val weight: Float = 1f) : Cap

    /** Sticky modifier (ctrl/alt/shift/win). */
    data class Mod(val label: String, val mod: String, override val weight: Float = 1f) : Cap

    data class Gap(override val weight: Float = 1f) : Cap
}

private fun c(base: String, shifted: String, w: Float = 1f) = Cap.Char(base, shifted, w)
private fun n(label: String, code: String, w: Float = 1f) = Cap.Named(label, code, w)
private fun m(label: String, mod: String, w: Float = 1f) = Cap.Mod(label, mod, w)
private fun gap(w: Float) = Cap.Gap(w)

private val mainRows: List<List<Cap>> = listOf(
    listOf(
        n("Esc", "esc", 1.3f),
        n("F1", "f1"), n("F2", "f2"), n("F3", "f3"), n("F4", "f4"),
        n("F5", "f5"), n("F6", "f6"), n("F7", "f7"), n("F8", "f8"),
        n("F9", "f9"), n("F10", "f10"), n("F11", "f11"), n("F12", "f12"),
        n("Del", "delete", 1.2f)
    ),
    listOf(
        c("`", "~"), c("1", "!"), c("2", "@"), c("3", "#"), c("4", "$"),
        c("5", "%"), c("6", "^"), c("7", "&"), c("8", "*"), c("9", "("),
        c("0", ")"), c("-", "_"), c("=", "+"),
        n("Bksp", "backspace", 2f)
    ),
    listOf(
        n("Tab", "tab", 1.5f),
        c("q", "Q"), c("w", "W"), c("e", "E"), c("r", "R"), c("t", "T"),
        c("y", "Y"), c("u", "U"), c("i", "I"), c("o", "O"), c("p", "P"),
        c("[", "{"), c("]", "}"), c("\\", "|", 1.5f)
    ),
    listOf(
        n("Caps", "capslock", 1.8f),
        c("a", "A"), c("s", "S"), c("d", "D"), c("f", "F"), c("g", "G"),
        c("h", "H"), c("j", "J"), c("k", "K"), c("l", "L"),
        c(";", ":"), c("'", "\""),
        n("Enter", "enter", 2.2f)
    ),
    listOf(
        m("Shift", "shift", 2.3f),
        c("z", "Z"), c("x", "X"), c("c", "C"), c("v", "V"), c("b", "B"),
        c("n", "N"), c("m", "M"), c(",", "<"), c(".", ">"), c("/", "?"),
        m("Shift", "shift", 1.7f),
        n("↑", "up")
    ),
    listOf(
        m("Ctrl", "ctrl", 1.4f),
        m("Win", "win", 1.2f),
        m("Alt", "alt", 1.2f),
        c(" ", " ", 6f),
        m("Alt", "alt", 1.2f),
        m("Ctrl", "ctrl", 1.4f),
        n("←", "left"),
        n("↓", "down"),
        n("→", "right")
    )
)

private val numpadRows: List<List<Cap>> = listOf(
    listOf(n("PrtSc", "printscreen"), n("Home", "home"), n("PgUp", "pageup"), n("PgDn", "pagedown")),
    listOf(n("Num", "numlock"), c("/", "/"), c("*", "*"), c("-", "-")),
    listOf(c("7", "7"), c("8", "8"), c("9", "9"), c("+", "+")),
    listOf(c("4", "4"), c("5", "5"), c("6", "6"), n("End", "end")),
    listOf(c("1", "1"), c("2", "2"), c("3", "3"), n("Ins", "insert")),
    listOf(c("0", "0", 2f), c(".", "."), n("Ent", "enter"))
)
