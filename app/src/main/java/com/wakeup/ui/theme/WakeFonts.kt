package com.wakeup.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.wakeup.R

/** Outfit ships as one variable font; each weight the design uses is an instance of it. */
@OptIn(ExperimentalTextApi::class)
private fun outfit(weight: Int) = Font(
    R.font.outfit,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val Outfit = FontFamily(outfit(200), outfit(300), outfit(400), outfit(500), outfit(600))

private val IbmPlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
)

/**
 * The two bundled families: [display] (Outfit) for everything, [mono] (IBM Plex Mono) for the
 * small uppercase section labels. Bundled so they render on the lock screen without a network.
 */
data class WakeFonts(val display: FontFamily = Outfit, val mono: FontFamily = IbmPlexMono) {
    @Deprecated("old design system") val ui get() = display
    @Deprecated("old design system") val num get() = display
    @Deprecated("old design system") val numWeight get() = FontWeight.ExtraLight
}
