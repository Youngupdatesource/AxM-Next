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

fun boldTypography(base: Typography = Typography): Typography {
    fun TextStyle.bold() = copy(fontFamily = BoldFontFamily)
    return base.copy(
        displayLarge = base.displayLarge.bold(),
        displayMedium = base.displayMedium.bold(),
        displaySmall = base.displaySmall.bold(),
        headlineLarge = base.headlineLarge.bold(),
        headlineMedium = base.headlineMedium.bold(),
        headlineSmall = base.headlineSmall.bold(),
        titleLarge = base.titleLarge.bold(),
        titleMedium = base.titleMedium.bold(),
        titleSmall = base.titleSmall.bold(),
        bodyLarge = base.bodyLarge.bold(),
        bodyMedium = base.bodyMedium.bold(),
        bodySmall = base.bodySmall.bold(),
        labelLarge = base.labelLarge.bold(),
        labelMedium = base.labelMedium.bold(),
        labelSmall = base.labelSmall.bold()
    )
}
