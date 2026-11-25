package com.wakeup.ui.theme

import android.provider.Settings
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle

private val LocalWakeColors = staticCompositionLocalOf { WakeColors(Palette.AURORA) }
private val LocalWakeFonts = staticCompositionLocalOf { WakeFonts() }

/**
 * True when the system "remove animations" setting is on. Every entrance, spring, ripple and
 * looping effect checks this and falls back to an instant state change.
 */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Theme accessor, mirroring `MaterialTheme.colorScheme`: `Wake.colors`, `Wake.fonts`. */
object Wake {
    val colors: WakeColors
        @Composable @ReadOnlyComposable get() = LocalWakeColors.current
    val fonts: WakeFonts
        @Composable @ReadOnlyComposable get() = LocalWakeFonts.current
}

/** The Liquid Glass theme: dark only, coloured by the chosen [palette]. */
@Composable
fun WakeUpTheme(palette: Palette, content: @Composable () -> Unit) {
    val colors = remember(palette) { WakeColors(palette) }
    val fonts = remember { WakeFonts() }
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }

    val scheme = darkColorScheme(
        primary = colors.orb2,
        background = colors.bg,
        onBackground = colors.text,
        surface = colors.bg,
        onSurface = colors.text,
        error = colors.error,
    )
    val typography = Typography(bodyLarge = TextStyle(fontFamily = fonts.display, color = colors.text))

    CompositionLocalProvider(
        LocalWakeColors provides colors,
        LocalWakeFonts provides fonts,
        LocalContentColor provides colors.text,
        LocalReduceMotion provides reduceMotion,
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
