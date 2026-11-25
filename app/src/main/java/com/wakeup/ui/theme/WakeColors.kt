package com.wakeup.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The full monochrome-plus-accent token bundle from the design. Almost everything is a neutral;
 * the accent (and its alpha-derived [accentSoft]/[glow]/[dot]) is the only chroma.
 */
data class WakeColors(
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val bg: Color,
    val bg2: Color,
    val card: Color,
    val text: Color,
    val textDim: Color,
    val textMute: Color,
    val hair: Color,
    val hairStrong: Color,
    /** Filled active day / selected-segment fill. */
    val selectedFill: Color,
    /** Switch off-track. */
    val switchOffTrack: Color,
    /** Numpad number-key background. */
    val keyBg: Color,
    /** Background accent glow. */
    val glow: Color,
    /** Background dot-grid color. */
    val dot: Color,
    val error: Color,
)

private val ERROR = Color(0xFFE5484D)

fun darkColors(accent: Color) = WakeColors(
    accent = accent,
    onAccent = Color.White,
    accentSoft = accent.copy(alpha = 0.22f),
    bg = Color(0xFF0E0F10),
    bg2 = Color(0xFF161718),
    card = Color(0xFF1A1B1C),
    text = Color(0xFFF2F2F0),
    textDim = Color(0xFF8D9194),
    textMute = Color(0xFF5E6265),
    hair = Color.White.copy(alpha = 0.10f),
    hairStrong = Color.White.copy(alpha = 0.18f),
    selectedFill = Color(0xFF2A2C2D),
    switchOffTrack = Color(0xFF2C2E30),
    keyBg = Color(0xFF1F2122),
    glow = accent.copy(alpha = 0.24f),
    dot = accent.copy(alpha = 0.52f),
    error = ERROR,
)

fun lightColors(accent: Color) = WakeColors(
    accent = accent,
    onAccent = Color.White,
    accentSoft = accent.copy(alpha = 0.14f),
    bg = Color(0xFFFAFAF9),
    bg2 = Color(0xFFF1F1EF),
    card = Color(0xFFFFFFFF),
    text = Color(0xFF181A1B),
    textDim = Color(0xFF74787B),
    textMute = Color(0xFFA3A7AA),
    hair = Color.Black.copy(alpha = 0.085f),
    hairStrong = Color.Black.copy(alpha = 0.16f),
    selectedFill = Color(0xFFECECEA),
    switchOffTrack = Color(0xFFD9D9D6),
    keyBg = Color(0xFFFFFFFF),
    glow = accent.copy(alpha = 0.14f),
    dot = accent.copy(alpha = 0.30f),
    error = ERROR,
)
