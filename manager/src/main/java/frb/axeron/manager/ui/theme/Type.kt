package frb.axeron.manager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import frb.axeron.manager.R
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)

/** Font OnePlus Sans Bold (res/font/oneplus_sans_bold.ttf). Hanya ada varian Bold. */
val OnePlusSansFamily = FontFamily(Font(R.font.oneplus_sans_bold, FontWeight.Bold))

/**
 * Typography untuk opsi Appearance > Teks tebal: semua gaya memakai OnePlus Sans Bold.
 * Karena keluarga font hanya berisi varian Bold, Text yang meminta weight lain
 * (Normal/Medium/SemiBold) tetap dirender dengan Bold.
 */
val BoldTypography: Typography = Typography.let { t ->
    fun TextStyle.bold() = copy(fontFamily = OnePlusSansFamily, fontWeight = FontWeight.Bold)
    t.copy(
        displayLarge = t.displayLarge.bold(),
        displayMedium = t.displayMedium.bold(),
        displaySmall = t.displaySmall.bold(),
        headlineLarge = t.headlineLarge.bold(),
        headlineMedium = t.headlineMedium.bold(),
        headlineSmall = t.headlineSmall.bold(),
        titleLarge = t.titleLarge.bold(),
        titleMedium = t.titleMedium.bold(),
        titleSmall = t.titleSmall.bold(),
        bodyLarge = t.bodyLarge.bold(),
        bodyMedium = t.bodyMedium.bold(),
        bodySmall = t.bodySmall.bold(),
        labelLarge = t.labelLarge.bold(),
        labelMedium = t.labelMedium.bold(),
        labelSmall = t.labelSmall.bold()
    )
}
