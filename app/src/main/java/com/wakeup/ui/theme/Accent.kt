package com.wakeup.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Converts an OKLCH color (as the design specifies its accents) to sRGB.
 * L in 0..1, C chroma, H hue in degrees. Out-of-gamut channels are clamped.
 */
fun oklch(l: Double, c: Double, hDeg: Double): Color {
    val h = Math.toRadians(hDeg)
    val a = c * cos(h)
    val b = c * sin(h)

    val l_ = l + 0.3963377774 * a + 0.2158037573 * b
    val m_ = l - 0.1055613458 * a - 0.0638541728 * b
    val s_ = l - 0.0894841775 * a - 1.2914855480 * b

    val lr = l_ * l_ * l_
    val mr = m_ * m_ * m_
    val sr = s_ * s_ * s_

    val r = 4.0767416621 * lr - 3.3077115913 * mr + 0.2309699292 * sr
    val g = -1.2684380046 * lr + 2.6097574011 * mr - 0.3413193965 * sr
    val bl = -0.0041960863 * lr - 0.7034186147 * mr + 1.7076147010 * sr

    return Color(gamma(r), gamma(g), gamma(bl))
}

private fun gamma(x: Double): Float {
    val v = if (x <= 0.0031308) 12.92 * x else 1.055 * x.pow(1.0 / 2.4) - 0.055
    return v.coerceIn(0.0, 1.0).toFloat()
}

/** A user-selectable accent. The five options from the design's `ACCENT_OPTIONS`. */
data class AccentOption(val name: String, val color: Color)

val ACCENT_OPTIONS: List<AccentOption> = listOf(
    AccentOption("Amber", oklch(0.70, 0.14, 55.0)),
    AccentOption("Green", oklch(0.66, 0.15, 150.0)),
    AccentOption("Ocean", oklch(0.64, 0.15, 248.0)),
    AccentOption("Violet", oklch(0.62, 0.16, 300.0)),
    AccentOption("Neon blue", oklch(0.70, 0.22, 250.0)),
)

const val DEFAULT_ACCENT_INDEX = 0 // Amber
