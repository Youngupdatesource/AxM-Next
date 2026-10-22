package frb.axeron.manager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import frb.axeron.manager.ui.util.AppearancePrefs
import frb.axeron.manager.ui.viewmodel.SettingsViewModel

/** Radius sudut kartu yang dipakai seragam di seluruh aplikasi. */
val CardCornerRadius = 28.dp

/** Shape kartu seragam. ElevatedCard/Card memakai Shapes.medium, jadi cukup diatur di sini. */
val AxShapes = Shapes(medium = RoundedCornerShape(CardCornerRadius))

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
                else -> dynamicLightColorScheme(context).tunedForLight()
            }
        }

        darkTheme -> getVortexDarkColorScheme(customPrimaryColor)
        else -> getVortexLightColorScheme(customPrimaryColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = if (AppearancePrefs.boldText) BoldTypography else Typography,
        shapes = AxShapes
    ) {
        contentCompose(settingsViewModel)
    }
}

/**
 * Penyetelan palet dinamis mode terang supaya tidak terlalu kontras (acuan: KernelSU-Next).
 *
 * - Teks utama tidak lagi hitam pekat: digeser 30% ke arah onSurfaceVariant.
 * - Garis outline dibuat lebih lembut.
 * - surfaceContainerLowest (putih murni di palet dinamis) dilunakkan ke arah surfaceContainerLow,
 *   supaya bottom sheet/dialog tidak terlihat "menyala" di atas latar yang sedikit keunguan.
 *
 * Warna latar, kartu, dan aksen tetap berasal dari wallpaper (Material You).
 */
private fun ColorScheme.tunedForLight(): ColorScheme = copy(
    onBackground = onBackground.blend(onSurfaceVariant, 0.30f),
    onSurface = onSurface.blend(onSurfaceVariant, 0.30f),
    outline = outline.blend(surface, 0.15f),
    outlineVariant = outlineVariant.blend(surface, 0.25f),
    surfaceContainerLowest = surfaceContainerLowest.blend(surfaceContainerLow, 0.5f)
)

/** True bila skema aktif adalah tema gelap. */
@Composable
fun isDarkScheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/**
 * Elevasi kartu. Tema gelap memakai 3dp (hampir tidak kelihatan), tema terang datar (0dp):
 * bayangan 3dp di latar terang membuat kartu terlihat mengambang dan terlalu kontras,
 * sedangkan KernelSU-Next memakai kartu datar bertinta.
 */
@Composable
fun cardElevation(): Dp = if (isDarkScheme()) 3.dp else 0.dp

/**
 * Warna top bar. Selalu sama dengan latar layar (background), baik saat konten di-scroll maupun
 * tidak. Default Material memakai surfaceContainer saat "scrolled"; pada palet dinamis gelap
 * warna itu kebiruan (#1C1E2A) dan membuat header Home berbeda dari header Pengaturan
 * (#0F131C, hitam sedikit biru).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun axTopAppBarColors(): TopAppBarColors {
    val bg = MaterialTheme.colorScheme.background
    return TopAppBarDefaults.topAppBarColors(
        containerColor = bg,
        scrolledContainerColor = bg
    )
}

/**
 * Warna kartu statistik (Privilege & Plugin di Home): secondaryContainer, sama dengan kartu
 * Superusers/Modules di KernelSU-Next. Pada palet dinamis gelap hasilnya keabu-biruan redup (~#373A4D),
 * pada terang lavender lembut (~#DFE1F8).
 */
@Composable
fun statTileContainerColor(): Color {
    val scheme = MaterialTheme.colorScheme
    // Gelap: diredupkan 25% ke arah latar supaya tidak terlalu kontras (~#373A4D seperti KernelSU-Next).
    return if (isDarkScheme()) scheme.secondaryContainer.blend(scheme.background, 0.25f)
    else scheme.secondaryContainer
}

@Composable
fun statTileContentColor(): Color = MaterialTheme.colorScheme.onSecondaryContainer

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
        // Ditambah sedikit biru (+2/255) supaya kartu terasa lebih biru: #24252C -> #24252E.
        val base = scheme.surfaceContainer.blend(scheme.onSurface, 0.06f)
        base.copy(blue = (base.blue + 2f / 255f).coerceAtMost(1f))
    } else {
        scheme.surfaceContainerHighest
    }
}

@Composable
fun insetContainerColor(): Color = MaterialTheme.colorScheme.surfaceContainer

@Composable
fun insetTextFieldColors(): TextFieldColors {
    val container = insetContainerColor()
    return TextFieldDefaults.colors(
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        focusedContainerColor = container,
        unfocusedContainerColor = container,
        disabledContainerColor = container,
        errorContainerColor = container
    )
}

/**
 * Warna badan kapsul nav. Di tema gelap dibuat lebih gelap dari kartu (kali 0,78 per kanal),
 * jadi kapsul tidak menyatu dengan kartu di belakangnya: kartu #24252E -> kapsul #1C1D24, setara
 * kapsul KernelSU, dan tetap mengikuti palet dinamis karena diturunkan dari [cardContainerColor].
 * Tema terang memakai campuran surfaceContainerLow/surfaceContainer.
 */
@Composable
fun capsuleContainerColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (scheme.background.luminance() < 0.5f) {
        val card = cardContainerColor()
        Color(red = card.red * 0.78f, green = card.green * 0.78f, blue = card.blue * 0.78f, alpha = 1f)
    } else {
        // Terang: kapsul lebih terang dari kartu (acuan KernelSU-Next #F2F0FB di atas kartu
        // #E3E1EF), bukan sama dengan kartu seperti sebelumnya.
        scheme.surfaceContainerLow.blend(scheme.surfaceContainer, 0.5f)
    }
}

/**
 * Warna latar bottom sheet. Di tema gelap diturunkan dari warna kartu (sedikit lebih gelap, sama
 * seperti kapsul nav), jadi tetap kebiruan seperti kartu dan latar lain. surfaceContainerLowest
 * dari palet dinamis hampir netral-hitam, sehingga sheet terlihat lebih hitam daripada layar
 * di belakangnya. Kartu di dalam sheet tetap terlihat karena lebih terang dari sheet.
 * Tema terang tetap memakai surfaceContainerLowest.
 */
@Composable
fun sheetContainerColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (scheme.background.luminance() < 0.5f) capsuleContainerColor()
    else scheme.surfaceContainerLow
}
