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
        FontWeight.W500,
        FontWeight.W600,
        FontWeight.W700,
        FontWeight.W800,
        FontWeight.W900
    ).map { Font(R.font.oneplus_sans_bold, weight = it) }
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

private fun boldStyle(sizeSp: Float, lineSp: Float, base: TextStyle) = base.copy(
    fontFamily = BoldFontFamily,
    fontSize = sizeSp.sp,
    lineHeight = lineSp.sp
)

fun boldTypography(base: Typography = Typography): Typography = base.copy(
    displayLarge = boldStyle(52f, 60f, base.displayLarge),
    displayMedium = boldStyle(42f, 50f, base.displayMedium),
    displaySmall = boldStyle(34f, 42f, base.displaySmall),
    headlineLarge = boldStyle(30f, 38f, base.headlineLarge),
    headlineMedium = boldStyle(26f, 34f, base.headlineMedium),
    headlineSmall = boldStyle(22f, 30f, base.headlineSmall),
    titleLarge = boldStyle(20f, 26f, base.titleLarge),
    titleMedium = boldStyle(15f, 20f, base.titleMedium),
    titleSmall = boldStyle(13f, 18f, base.titleSmall),
    bodyLarge = boldStyle(15f, 22f, base.bodyLarge),
    bodyMedium = boldStyle(13f, 19f, base.bodyMedium),
    bodySmall = boldStyle(12f, 17f, base.bodySmall),
    labelLarge = boldStyle(13f, 18f, base.labelLarge),
    labelMedium = boldStyle(11.5f, 16f, base.labelMedium),
    labelSmall = boldStyle(10.5f, 14f, base.labelSmall)
)

private val ScreenTitleBoldStyle = TextStyle(
    fontFamily = BoldFontFamily,
    fontSize = 28.sp,
    lineHeight = 34.sp
)

@Composable
fun screenTitleStyle(): TextStyle =
    if (AppearancePrefs.boldText) ScreenTitleBoldStyle else MaterialTheme.typography.titleLarge
