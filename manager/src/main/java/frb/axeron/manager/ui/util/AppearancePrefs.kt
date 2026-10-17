package frb.axeron.manager.ui.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import frb.axeron.api.core.AxeronSettings

/** Opsi tampilan yang disimpan lokal dan bisa diamati Compose. */
object AppearancePrefs {
    private const val KEY_SWITCH_ICON = "appearance_switch_icon"

    private val prefs get() = AxeronSettings.getPreferences()

    private var _switchIcon by mutableStateOf(prefs.getBoolean(KEY_SWITCH_ICON, false))

    /** true = thumb Switch menampilkan ikon: ✓ saat aktif, ✕ saat mati. */
    var switchIcon: Boolean
        get() = _switchIcon
        set(value) {
            _switchIcon = value
            prefs.edit { putBoolean(KEY_SWITCH_ICON, value) }
        }
}
