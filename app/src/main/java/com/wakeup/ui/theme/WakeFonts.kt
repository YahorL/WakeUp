package com.wakeup.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.wakeup.R

/**
 * The four Google-Fonts families the design uses, bundled as TTF resources so they render
 * reliably on the lock screen without a network round-trip.
 */
private val HankenGrotesk = FontFamily(
    Font(R.font.hanken_grotesk_regular, FontWeight.Normal),
    Font(R.font.hanken_grotesk_medium, FontWeight.Medium),
    Font(R.font.hanken_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.hanken_grotesk_bold, FontWeight.Bold),
)

private val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_medium, FontWeight.SemiBold),
    Font(R.font.space_grotesk_medium, FontWeight.Bold),
)

private val InstrumentSerif = FontFamily(
    Font(R.font.instrument_serif_regular, FontWeight.Normal),
    Font(R.font.instrument_serif_regular, FontWeight.Medium),
)

private val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_medium, FontWeight.SemiBold),
)

/** Resolved fonts for the active pairing: [ui] for body/labels, [display] for headings, [num]
 *  for big tabular numerals (rendered at [numWeight]). */
data class WakeFonts(
    val ui: FontFamily,
    val display: FontFamily,
    val num: FontFamily,
    val numWeight: FontWeight,
)

/** The three selectable typeface pairings from the design. */
enum class FontPairing(val label: String) {
    GROTESK("Grotesk"),
    SERIF("Serif"),
    MONO("Mono");

    fun resolve(): WakeFonts = when (this) {
        GROTESK -> WakeFonts(ui = HankenGrotesk, display = SpaceGrotesk, num = SpaceGrotesk, numWeight = FontWeight.Medium)
        SERIF -> WakeFonts(ui = HankenGrotesk, display = InstrumentSerif, num = InstrumentSerif, numWeight = FontWeight.Normal)
        MONO -> WakeFonts(ui = SpaceGrotesk, display = JetBrainsMono, num = JetBrainsMono, numWeight = FontWeight.Medium)
    }
}
