package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val BricolageGrotesqueFontFamily = FontFamily(
    Font(R.font.bricolage_grotesque, FontWeight.Normal),
    Font(R.font.bricolage_grotesque, FontWeight.Medium),
    Font(R.font.bricolage_grotesque, FontWeight.SemiBold),
    Font(R.font.bricolage_grotesque, FontWeight.Bold),
    Font(R.font.bricolage_grotesque, FontWeight.ExtraBold)
)

val DmSansFontFamily = FontFamily(
    Font(R.font.dm_sans, FontWeight.Normal),
    Font(R.font.dm_sans, FontWeight.Medium),
    Font(R.font.dm_sans, FontWeight.SemiBold),
    Font(R.font.dm_sans, FontWeight.Bold)
)

// Tipografía con números tabulares para evitar Layout Shift
val TabularBricolage = TextStyle(
    fontFamily = BricolageGrotesqueFontFamily,
    fontFeatureSettings = "tnum"
)

val TabularDmSans = TextStyle(
    fontFamily = DmSansFontFamily,
    fontFeatureSettings = "tnum"
)

val TabularSpeedBadge = TextStyle(
    fontFamily = DmSansFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    fontFeatureSettings = "tnum"
)

val TabularPercentBig = TextStyle(
    fontFamily = BricolageGrotesqueFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 28.sp,
    lineHeight = 32.sp,
    fontFeatureSettings = "tnum"
)

val TabularPercentSign = TextStyle(
    fontFamily = BricolageGrotesqueFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    fontFeatureSettings = "tnum"
)

val TabularEta = TextStyle(
    fontFamily = DmSansFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    fontFeatureSettings = "tnum"
)

val TabularParts = TextStyle(
    fontFamily = DmSansFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    fontFeatureSettings = "tnum"
)

val TabularBytes = TextStyle(
    fontFamily = DmSansFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    fontFeatureSettings = "tnum"
)

/**
 * Escala tipográfica estricta:
 * Máximo 4 tamaños principales de texto (12sp, 14sp, 16sp, 24sp)
 * y 2 pesos estándar (Normal y SemiBold), con soporte tabular para cifras numéricas.
 */
val Typography = Typography(
    // 24sp
    headlineLarge = TextStyle(
        fontFamily = BricolageGrotesqueFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontFeatureSettings = "tnum"
    ),
    headlineMedium = TextStyle(
        fontFamily = BricolageGrotesqueFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontFeatureSettings = "tnum"
    ),
    headlineSmall = TextStyle(
        fontFamily = BricolageGrotesqueFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    // 16sp
    titleLarge = TextStyle(
        fontFamily = BricolageGrotesqueFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = BricolageGrotesqueFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    // 14sp
    titleSmall = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // 12sp
    bodySmall = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum"
    ),
    labelMedium = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum"
    ),
    labelSmall = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum"
    )
)
