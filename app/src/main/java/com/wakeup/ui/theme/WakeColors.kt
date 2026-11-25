package com.wakeup.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * A Liquid Glass palette. The orb colours are bright and only ever appear blurred (background
 * glows, glow shadows) or as tiny indicators; [fillFrom]→[fillTo] is the darker gradient used for
 * every solid accent surface so white text on it stays readable.
 */
enum class Palette(
    val label: String,
    val orb1: Color,
    val orb2: Color,
    val orb3: Color,
    val fillFrom: Color,
    val fillTo: Color,
) {
    AURORA("Aurora", Color(0xFF7B5CFF), Color(0xFF4C8DFF), Color(0xFFFF9F43), Color(0xFF6D28D9), Color(0xFF2563EB)),
    EMBER("Ember", Color(0xFFFF6A2B), Color(0xFFFFB020), Color(0xFF8A4DFF), Color(0xFFC2410C), Color(0xFFB45309)),
    LAGOON("Lagoon", Color(0xFF2EE6C5), Color(0xFF3D7BFF), Color(0xFF9B5CFF), Color(0xFF0F766E), Color(0xFF1D4ED8)),
}

/** The design's colour tokens (`tokens.json`). Dark only. */
data class WakeColors(val palette: Palette) {
    val bg = Color(0xFF08080C)
    val text = Color.White
    val textSecondary = Color.White.copy(alpha = 0.62f)
    val textTertiary = Color.White.copy(alpha = 0.50f)
    val textMuted = Color.White.copy(alpha = 0.72f)
    val onLight = Color(0xFF0B0B10)
    val error = Color(0xFFFFB4B4)
    val hairline = Color.White.copy(alpha = 0.10f)
    val whitePill = Color.White.copy(alpha = 0.94f)

    val orb1 get() = palette.orb1
    val orb2 get() = palette.orb2
    val orb3 get() = palette.orb3

    /** The 135° accent gradient for solid accent surfaces. */
    val fill: Brush = Brush.linearGradient(
        listOf(palette.fillFrom, palette.fillTo),
        start = Offset.Zero,
        end = Offset.Infinite,
    )
}
