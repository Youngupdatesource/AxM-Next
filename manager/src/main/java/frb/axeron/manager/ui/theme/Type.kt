package frb.axeron.manager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import frb.axeron.manager.R
import frb.axeron.manager.ui.util.AppearancePrefs

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

private fun boldStyle(sizeSp: Float, lineSp: Float, weight: FontWeight, base: TextStyle) = base.copy(
    fontFamily = BoldFontFamily,
    fontWeight = weight,
    fontSize = sizeSp.sp,
    lineHeight = lineSp.sp
)

fun boldTypography(base: Typography = Typography): Typography = base.copy(
    displayLarge = boldStyle(52f, 60f, FontWeight.W800, base.displayLarge),
    displayMedium = boldStyle(42f, 50f, FontWeight.W800, base.displayMedium),
    displaySmall = boldStyle(34f, 42f, FontWeight.W800, base.displaySmall),
    headlineLarge = boldStyle(30f, 38f, FontWeight.W800, base.headlineLarge),
    headlineMedium = boldStyle(26f, 34f, FontWeight.W800, base.headlineMedium),
    headlineSmall = boldStyle(22f, 30f, FontWeight.W800, base.headlineSmall),
    titleLarge = boldStyle(20f, 26f, FontWeight.W800, base.titleLarge),
    titleMedium = boldStyle(15f, 20f, FontWeight.W700, base.titleMedium),
    titleSmall = boldStyle(13f, 18f, FontWeight.W700, base.titleSmall),
    bodyLarge = boldStyle(15f, 22f, FontWeight.W500, base.bodyLarge),
    bodyMedium = boldStyle(13f, 19f, FontWeight.W500, base.bodyMedium),
    bodySmall = boldStyle(12f, 17f, FontWeight.W500, base.bodySmall),
    labelLarge = boldStyle(13f, 18f, FontWeight.W600, base.labelLarge),
    labelMedium = boldStyle(11.5f, 16f, FontWeight.W500, base.labelMedium),
    labelSmall = boldStyle(10.5f, 14f, FontWeight.W500, base.labelSmall)
)

private val ScreenTitleBoldStyle = TextStyle(
    fontFamily = BoldFontFamily,
    fontWeight = FontWeight.W800,
    fontSize = 28.sp,
    lineHeight = 34.sp
)

@Composable
fun screenTitleStyle(): TextStyle =
    if (AppearancePrefs.boldText) ScreenTitleBoldStyle else MaterialTheme.typography.titleLarge
