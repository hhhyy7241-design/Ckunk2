package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Brand 2026 Gradients & Accents
val ElectricBlue = Color(0xFF3A55FF)
val ElectricCyan = Color(0xFF00C2FF)
val SuccessGreen = Color(0xFF10B981)
val WarningAmber = Color(0xFFF59E0B)
val ErrorRose = Color(0xFFEF4444)

val BrandGradient = Brush.horizontalGradient(
    listOf(ElectricBlue, ElectricCyan)
)

val BrandRadialGlowDark = Brush.radialGradient(
    colors = listOf(
        Color(0x283A55FF),
        Color(0x0A00C2FF),
        Color.Transparent
    )
)

val BrandRadialGlowLight = Brush.radialGradient(
    colors = listOf(
        Color(0x183A55FF),
        Color(0x0800C2FF),
        Color.Transparent
    )
)

// Dark Theme (Deep, high contrast, clean)
val DarkBackground = Color(0xFF0B0E14)
val DarkSurface = Color(0xFF141923)
val DarkSurfaceElevated = Color(0xFF1D2330)
val DarkSurfaceVariant = Color(0xFF252D3D)
val DarkBorder = Color(0xFF2C3547)
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8) // High contrast secondary
val DarkTextTertiary = Color(0xFF64748B)

// Light Theme (Crisp, clean, soft shadows)
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFF1F5F9)
val LightSurfaceVariant = Color(0xFFE2E8F0)
val LightBorder = Color(0xFFCBD5E1)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569) // High contrast secondary
val LightTextTertiary = Color(0xFF64748B)
