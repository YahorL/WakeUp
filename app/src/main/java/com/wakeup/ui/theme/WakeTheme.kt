package com.wakeup.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

private val LocalWakeColors = staticCompositionLocalOf { darkColors(ACCENT_OPTIONS[DEFAULT_ACCENT_INDEX].color) }
private val LocalWakeFonts = staticCompositionLocalOf { FontPairing.GROTESK.resolve() }

/** Theme accessor, mirroring `MaterialTheme.colorScheme`: `Wake.colors`, `Wake.fonts`. */
object Wake {
    val colors: WakeColors
        @Composable @ReadOnlyComposable get() = LocalWakeColors.current
    val fonts: WakeFonts
        @Composable @ReadOnlyComposable get() = LocalWakeFonts.current
}

/**
 * The app theme. Builds the monochrome-plus-accent token bundle for the chosen [accent]/[dark]
 * and resolves the [fontPairing], exposing them via [Wake]. Also feeds a matching Material color
 * scheme so the few stock Material components (time picker dialog) inherit sane colors.
 */
@Composable
fun WakeUpTheme(
    accent: Color,
    dark: Boolean,
    fontPairing: FontPairing,
    content: @Composable () -> Unit,
) {
    val colors = if (dark) darkColors(accent) else lightColors(accent)
    val fonts = fontPairing.resolve()

    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = colors.accent,
        onPrimary = colors.onAccent,
        secondary = colors.accent,
        background = colors.bg,
        onBackground = colors.text,
        surface = colors.card,
        onSurface = colors.text,
        surfaceVariant = colors.bg2,
        onSurfaceVariant = colors.textDim,
        outline = colors.hairStrong,
        error = colors.error,
    )
    val typography = Typography(bodyLarge = TextStyle(fontFamily = fonts.ui, color = colors.text))

    CompositionLocalProvider(
        LocalWakeColors provides colors,
        LocalWakeFonts provides fonts,
        LocalContentColor provides colors.text,
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
