package com.wakeup.settings

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.wakeup.ui.theme.ACCENT_OPTIONS
import com.wakeup.ui.theme.DEFAULT_ACCENT_INDEX
import com.wakeup.ui.theme.FontPairing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** App-wide appearance, persisted and observed live across every screen. */
data class AppSettings(
    val accentIndex: Int = DEFAULT_ACCENT_INDEX,
    val dark: Boolean = true,
    val fontPairing: FontPairing = FontPairing.GROTESK,
) {
    val accent: Color get() = ACCENT_OPTIONS[accentIndex.coerceIn(ACCENT_OPTIONS.indices)].color
}

/** SharedPreferences-backed settings exposed as a [StateFlow]. */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("wakeup_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    private fun load(): AppSettings {
        val accent = prefs.getInt(KEY_ACCENT, DEFAULT_ACCENT_INDEX).coerceIn(ACCENT_OPTIONS.indices)
        val dark = prefs.getBoolean(KEY_DARK, true)
        val font = runCatching { FontPairing.valueOf(prefs.getString(KEY_FONT, FontPairing.GROTESK.name)!!) }
            .getOrDefault(FontPairing.GROTESK)
        return AppSettings(accent, dark, font)
    }

    fun setAccentIndex(index: Int) = update(_state.value.copy(accentIndex = index.coerceIn(ACCENT_OPTIONS.indices)))
    fun setDark(dark: Boolean) = update(_state.value.copy(dark = dark))
    fun setFontPairing(font: FontPairing) = update(_state.value.copy(fontPairing = font))

    private fun update(next: AppSettings) {
        prefs.edit()
            .putInt(KEY_ACCENT, next.accentIndex)
            .putBoolean(KEY_DARK, next.dark)
            .putString(KEY_FONT, next.fontPairing.name)
            .apply()
        _state.value = next
    }

    private companion object {
        const val KEY_ACCENT = "accent_index"
        const val KEY_DARK = "dark"
        const val KEY_FONT = "font_pairing"
    }
}
