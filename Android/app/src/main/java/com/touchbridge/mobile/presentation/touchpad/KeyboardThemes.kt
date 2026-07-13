package com.touchbridge.mobile.presentation.touchpad

import androidx.compose.ui.graphics.Color
import com.touchbridge.mobile.domain.model.KeyboardThemeId

/**
 * Colour palette for a keyboard skin. [rowAccents] drives the per-row backlight glow: an empty
 * list means no glow, one colour means a uniform glow, several colours cycle down the rows for a
 * rainbow effect. When [tintLabels] is set, each key's label is drawn in its row accent.
 */
data class KbTheme(
    val panel: Color,
    val keyFill: Color,
    val keyFillActive: Color,
    val text: Color,
    val textActive: Color,
    val rowAccents: List<Color> = emptyList(),
    val tintLabels: Boolean = false
) {
    fun accentFor(rowIndex: Int): Color? =
        if (rowAccents.isEmpty()) null else rowAccents[rowIndex % rowAccents.size]
}

/** Maps the desktop-chosen theme id to its concrete colours. */
fun keyboardTheme(id: KeyboardThemeId): KbTheme = when (id) {
    KeyboardThemeId.Dark -> KbTheme(
        panel = Color.Black,
        keyFill = Color(0xFF2A2A38),
        keyFillActive = Color(0xFF4F8EF7),
        text = Color(0xFFE8E8F0),
        textActive = Color.White
    )

    KeyboardThemeId.NeonBlue -> KbTheme(
        panel = Color(0xFF05060A),
        keyFill = Color(0xFF0E1420),
        keyFillActive = Color(0xFF1E90FF),
        text = Color(0xFFA6DBFF),
        textActive = Color.White,
        rowAccents = listOf(Color(0xFF2FB8FF))
    )

    KeyboardThemeId.NeonPurple -> KbTheme(
        panel = Color(0xFF0A0410),
        keyFill = Color(0xFF17091F),
        keyFillActive = Color(0xFFB24BF3),
        text = Color(0xFFE9B8FF),
        textActive = Color.White,
        rowAccents = listOf(Color(0xFFC64DFF), Color(0xFFFF4DD2))
    )

    KeyboardThemeId.Rgb -> KbTheme(
        panel = Color.Black,
        keyFill = Color(0xFF141419),
        keyFillActive = Color(0xFFFFFFFF),
        text = Color.White,
        textActive = Color.Black,
        rowAccents = listOf(
            Color(0xFFFF3B3B), // red
            Color(0xFFFF9E3B), // orange
            Color(0xFFFFE23B), // yellow
            Color(0xFF48E06A), // green
            Color(0xFF3BC9FF), // cyan
            Color(0xFF6A5BFF), // blue
            Color(0xFFD46BFF)  // violet
        ),
        tintLabels = true
    )

    KeyboardThemeId.Ocean -> KbTheme(
        panel = Color(0xFF031316),
        keyFill = Color(0xFF07222A),
        keyFillActive = Color(0xFF16C7C7),
        text = Color(0xFF9FEDE9),
        textActive = Color(0xFF012A2A),
        rowAccents = listOf(Color(0xFF17D6C6), Color(0xFF2AA9FF))
    )

    KeyboardThemeId.Sunset -> KbTheme(
        panel = Color(0xFF160611),
        keyFill = Color(0xFF25101A),
        keyFillActive = Color(0xFFFF7A59),
        text = Color(0xFFFFC9A8),
        textActive = Color.White,
        rowAccents = listOf(Color(0xFFFF5E7E), Color(0xFFFF9E3B), Color(0xFFFFD23B))
    )

    KeyboardThemeId.Light -> KbTheme(
        panel = Color(0xFFE9ECF2),
        keyFill = Color(0xFFFFFFFF),
        keyFillActive = Color(0xFF4F8EF7),
        text = Color(0xFF1E2230),
        textActive = Color.White,
        rowAccents = listOf(Color(0xFFCED4E0))
    )
}
