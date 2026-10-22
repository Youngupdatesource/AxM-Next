package frb.axeron.manager.ui.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import frb.axeron.api.core.AxeronSettings

/** Opsi tampilan yang disimpan lokal dan bisa diamati Compose. */
object AppearancePrefs {
    private const val KEY_SWITCH_ICON = "appearance_switch_icon"
    private const val KEY_BOLD_TEXT = "appearance_bold_text"

    private val prefs get() = AxeronSettings.getPreferences()

    private var _switchIcon by mutableStateOf(prefs.getBoolean(KEY_SWITCH_ICON, false))

    private var _boldText by mutableStateOf(prefs.getBoolean(KEY_BOLD_TEXT, false))

    /** true = thumb Switch menampilkan ikon: ✓ saat aktif, ✕ saat mati. */
    var switchIcon: Boolean
        get() = _switchIcon
        set(value) {
            _switchIcon = value
            prefs.edit { putBoolean(KEY_SWITCH_ICON, value) }
        }

    var boldText: Boolean
        get() = _boldText
        set(value) {
            _boldText = value
            prefs.edit { putBoolean(KEY_BOLD_TEXT, value) }
        }
}
