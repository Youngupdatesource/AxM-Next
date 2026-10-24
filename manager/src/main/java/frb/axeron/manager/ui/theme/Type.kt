package frb.axeron.manager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import frb.axeron.manager.R

/**
 * Satu-satunya font aplikasi: San Francisco.
 * Dipakai ulang untuk semua weight & kedua mode (bold / non-bold), sehingga
 * pergantian mode tidak lagi berganti file font.
 */
val AppFontFamily = FontFamily(
    Font(R.font.sanfrancisco_bold, weight = FontWeight.W700)
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

private fun boldStyle(weight: FontWeight, base: TextStyle) = base.copy(
    fontFamily = AppFontFamily,
    fontWeight = weight
)

fun boldTypography(base: Typography = Typography): Typography = base.copy(
    displayLarge = boldStyle(FontWeight.W800, base.displayLarge),
    displayMedium = boldStyle(FontWeight.W800, base.displayMedium),
    displaySmall = boldStyle(FontWeight.W800, base.displaySmall),
    headlineLarge = boldStyle(FontWeight.W800, base.headlineLarge),
    headlineMedium = boldStyle(FontWeight.W800, base.headlineMedium),
    headlineSmall = boldStyle(FontWeight.W800, base.headlineSmall),
    titleLarge = boldStyle(FontWeight.W800, base.titleLarge),
    titleMedium = boldStyle(FontWeight.W700, base.titleMedium),
    titleSmall = boldStyle(FontWeight.W700, base.titleSmall),
    bodyLarge = boldStyle(FontWeight.W500, base.bodyLarge),
    bodyMedium = boldStyle(FontWeight.W500, base.bodyMedium),
    bodySmall = boldStyle(FontWeight.W500, base.bodySmall),
    labelLarge = boldStyle(FontWeight.W600, base.labelLarge),
    labelMedium = boldStyle(FontWeight.W500, base.labelMedium),
    labelSmall = boldStyle(FontWeight.W500, base.labelSmall)
)
