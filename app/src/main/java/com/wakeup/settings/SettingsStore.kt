package com.wakeup.settings

import android.content.Context
import com.wakeup.ui.theme.Palette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** App-wide appearance, persisted and observed live across every screen. */
data class AppSettings(val palette: Palette = Palette.AURORA)

/** SharedPreferences-backed settings exposed as a [StateFlow]. */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("wakeup_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    private fun load(): AppSettings {
        val palette = runCatching { Palette.valueOf(prefs.getString(KEY_PALETTE, null)!!) }
            .getOrDefault(Palette.AURORA)
        return AppSettings(palette)
    }

    fun setPalette(palette: Palette) {
        prefs.edit().putString(KEY_PALETTE, palette.name).apply()
        _state.value = AppSettings(palette)
    }

    private companion object {
        const val KEY_PALETTE = "palette"
    }
}
