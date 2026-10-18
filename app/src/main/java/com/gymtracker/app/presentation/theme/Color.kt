package com.gymtracker.app.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Teal Tokens
val LightPrimary = Color(0xFF0F766E)         // Deep Athletic Teal
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFCCFBF1)
val LightOnPrimaryContainer = Color(0xFF115E59)

// Light Neutrals
val LightBackground = Color(0xFFF8FAFC)       // Slate 50
val LightOnBackground = Color(0xFF0F172A)     // Slate 900
val LightSurface = Color(0xFFFFFFFF)          // Pure White
val LightOnSurface = Color(0xFF0F172A)        // Slate 900
val LightSurfaceVariant = Color(0xFFF1F5F9)   // Slate 100
val LightOnSurfaceVariant = Color(0xFF475569) // Slate 600
val LightOutline = Color(0xFFCBD5E1)          // Slate 300
val LightOutlineVariant = Color(0xFFE2E8F0)   // Slate 200

// Dark Teal Tokens
val DarkPrimary = Color(0xFF2DD4BF)          // Vibrant Athletic Teal
val DarkOnPrimary = Color(0xFF003731)
val DarkPrimaryContainer = Color(0xFF134E48)
val DarkOnPrimaryContainer = Color(0xFFCCFBF1)

// Dark Neutrals
val DarkBackground = Color(0xFF0B1117)
val DarkOnBackground = Color(0xFFF1F5F9)
val DarkSurface = Color(0xFF101820)
val DarkOnSurface = Color(0xFFF1F5F9)
val DarkSurfaceVariant = Color(0xFF253241)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)  // Slate 400
val DarkOutline = Color(0xFF475569)           // Slate 600
val DarkOutlineVariant = Color(0xFF334155)    // Slate 700

// Semantic Colors - Light
val LightSuccess = Color(0xFF15803D)
val LightOnSuccess = Color(0xFFFFFFFF)
val LightSuccessContainer = Color(0xFFDCFCE7)
val LightOnSuccessContainer = Color(0xFF166534)

val LightWarning = Color(0xFFB45309)
val LightOnWarning = Color(0xFFFFFFFF)
val LightWarningContainer = Color(0xFFFEF3C7)
val LightOnWarningContainer = Color(0xFF92400E)

val LightError = Color(0xFFDC2626)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFEE2E2)
val LightOnErrorContainer = Color(0xFF991B1B)

val LightInfo = Color(0xFF0284C7)
val LightOnInfo = Color(0xFFFFFFFF)
val LightInfoContainer = Color(0xFFE0F2FE)
val LightOnInfoContainer = Color(0xFF075985)

// Semantic Colors - Dark
val DarkSuccess = Color(0xFF4ADE80)
val DarkOnSuccess = Color(0xFF052E16)
val DarkSuccessContainer = Color(0xFF14532D)
val DarkOnSuccessContainer = Color(0xFFDCFCE7)

val DarkWarning = Color(0xFFFBBF24)
val DarkOnWarning = Color(0xFF451A03)
val DarkWarningContainer = Color(0xFF78350F)
val DarkOnWarningContainer = Color(0xFFFEF3C7)

val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val DarkInfo = Color(0xFF38BDF8)
val DarkOnInfo = Color(0xFF082F49)
val DarkInfoContainer = Color(0xFF0C4A6E)
val DarkOnInfoContainer = Color(0xFFE0F2FE)

@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
)

val LightExtendedColors = ExtendedColors(
    success = LightSuccess,
    onSuccess = LightOnSuccess,
    successContainer = LightSuccessContainer,
    onSuccessContainer = LightOnSuccessContainer,
    warning = LightWarning,
    onWarning = LightOnWarning,
    warningContainer = LightWarningContainer,
    onWarningContainer = LightOnWarningContainer,
    info = LightInfo,
    onInfo = LightOnInfo,
    infoContainer = LightInfoContainer,
    onInfoContainer = LightOnInfoContainer,
)

val DarkExtendedColors = ExtendedColors(
    success = DarkSuccess,
    onSuccess = DarkOnSuccess,
    successContainer = DarkSuccessContainer,
    onSuccessContainer = DarkOnSuccessContainer,
    warning = DarkWarning,
    onWarning = DarkOnWarning,
    warningContainer = DarkWarningContainer,
    onWarningContainer = DarkOnWarningContainer,
    info = DarkInfo,
    onInfo = DarkOnInfo,
    infoContainer = DarkInfoContainer,
    onInfoContainer = DarkOnInfoContainer,
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
