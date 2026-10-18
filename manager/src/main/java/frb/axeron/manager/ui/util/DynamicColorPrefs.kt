package frb.axeron.manager.ui.util

import android.os.Build
import androidx.core.content.edit
import frb.axeron.api.core.AxeronSettings

/**
 * Dynamic (Material You) color preference.
 *
 * The stored default in [AxeronSettings] is "off", so it was off for everyone on a fresh install
 * even where the system supports it. Until the user chooses, it now follows the platform: on
 * with Android 12+, off below. An explicit choice is remembered under its own key and also
 * written through to [AxeronSettings] so everything reading that keeps agreeing.
 */
object DynamicColorPrefs {
    private const val KEY = "dynamic_color_user_choice"

    val supported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun isEnabled(): Boolean {
        val prefs = AxeronSettings.getPreferences()
        return if (prefs.contains(KEY)) prefs.getBoolean(KEY, false) else supported
    }

    fun setEnabled(enabled: Boolean) {
        AxeronSettings.getPreferences().edit { putBoolean(KEY, enabled) }
        AxeronSettings.setEnableDynamicColor(enabled)
    }
}
