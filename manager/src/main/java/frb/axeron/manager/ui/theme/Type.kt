package frb.axeron.manager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import frb.axeron.manager.R

val BoldFontFamily = FontFamily(
    listOf(
        FontWeight.W100,
        FontWeight.W200,
        FontWeight.W300,
        FontWeight.W400,
        FontWeight.W500
    ).map { Font(R.font.oneplus_sans_bold_plus, weight = it) } +
        listOf(
            FontWeight.W600,
            FontWeight.W700,
            FontWeight.W800,
            FontWeight.W900
        ).map { Font(R.font.oneplus_sans_heavy, weight = it) }
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

private fun boldStyle(weight: FontWeight, base: TextStyle) = base.copy(
    fontFamily = BoldFontFamily,
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
