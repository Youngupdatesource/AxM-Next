package frb.axeron.manager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import frb.axeron.manager.R

/**
 * Satu keluarga font: San Francisco.
 * - Mode normal (bold text OFF): sanfrancisco_medium
 * - Mode bold   (bold text ON) : sanfrancisco_heavy
 * Tiap family hanya berisi satu file untuk SEMUA weight. Di mode normal, teks yang
 * meminta weight >= W600 (SemiBold/Bold eksplisit) mendapat synthetic bold dari Compose
 * di atas Medium, sehingga hierarki judul vs isi tetap terlihat.
 */
val AppFontFamily = FontFamily(
    Font(R.font.sanfrancisco_medium, weight = FontWeight.W500)
)

val BoldFontFamily = FontFamily(
    Font(R.font.sanfrancisco_heavy, weight = FontWeight.W800)
)

private fun Typography.withFont(family: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family)
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
).withFont(AppFontFamily)

/** Mode bold: pakai font Heavy, letterSpacing mengikuti default (tidak dirapatkan). */
private fun boldStyle(base: TextStyle) = base.copy(
    fontFamily = BoldFontFamily,
    fontWeight = FontWeight.W800
)

fun boldTypography(base: Typography = Typography): Typography = base.copy(
    displayLarge = boldStyle(base.displayLarge),
    displayMedium = boldStyle(base.displayMedium),
    displaySmall = boldStyle(base.displaySmall),
    headlineLarge = boldStyle(base.headlineLarge),
    headlineMedium = boldStyle(base.headlineMedium),
    headlineSmall = boldStyle(base.headlineSmall),
    titleLarge = boldStyle(base.titleLarge),
    titleMedium = boldStyle(base.titleMedium),
    titleSmall = boldStyle(base.titleSmall),
    bodyLarge = boldStyle(base.bodyLarge),
    bodyMedium = boldStyle(base.bodyMedium),
    bodySmall = boldStyle(base.bodySmall),
    labelLarge = boldStyle(base.labelLarge),
    labelMedium = boldStyle(base.labelMedium),
    labelSmall = boldStyle(base.labelSmall)
)
