package frb.axeron.manager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import frb.axeron.manager.ui.viewmodel.SettingsViewModel

fun hexToColor(hex: String): Color {
    val cleanHex = hex.removePrefix("#")
    val cleanDefault = basePrimaryDefault.toHexString().removePrefix("#")
    return when (cleanHex.length) {
        6 -> Color(("FF$cleanHex").toLong(16))
        8 -> Color(cleanHex.toLong(16))
        else -> Color(cleanDefault.toLong(16))
    }
}

@Composable
fun AxManagerTheme(
    // Dynamic color is available on Android 12+ ,
    contentCompose: @Composable (SettingsViewModel) -> Unit
) {
    val settingsViewModel: SettingsViewModel = viewModel<SettingsViewModel>()

    val darkTheme = when (settingsViewModel.getAppThemeId) {
        1 -> true
        2 -> false
        else -> isSystemInDarkTheme()
    }

    val dynamicColor = settingsViewModel.isDynamicColorEnabled
    val customPrimaryColor =
        hexToColor(settingsViewModel.customPrimaryColorHex)

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            when {
                darkTheme -> dynamicDarkColorScheme(context)
                else -> dynamicLightColorScheme(context)
            }
        }

        darkTheme -> getVortexDarkColorScheme(customPrimaryColor)
        else -> getVortexLightColorScheme(customPrimaryColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        contentCompose(settingsViewModel)
    }
}

/**
 * Warna kartu: sedikit lebih terang dari top bar (saat di-scroll) dan bottom bar, tapi tidak sama
 * dengan keduanya.
 *
 * Kedua bar memakai surfaceContainer. Di tema gelap, warna kartu dihitung relatif terhadap warna
 * bar itu sendiri (digeser sedikit ke arah onSurface), bukan memilih role surfaceContainer* lain,
 * karena pada palet dinamis/beberapa ROM role-role itu bisa hampir sama dan kartu jadi menyatu
 * dengan bar. Tema terang tetap memakai surfaceContainerHighest.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun cardContainerColor(
    fallback: Color = Color.Unspecified
): Color {
    val scheme = MaterialTheme.colorScheme
    return if (scheme.background.luminance() < 0.5f) {
        scheme.surfaceContainer.blend(scheme.onSurface, 0.06f)
    } else {
        scheme.surfaceContainerHighest
    }
}
