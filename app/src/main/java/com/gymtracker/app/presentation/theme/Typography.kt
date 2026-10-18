package com.gymtracker.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Tabular numbers feature setting for changing metrics and timers
const val TABULAR_NUMERALS_FEATURE = "tnum"

val ScreenTitleStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 34.sp,
)

val SectionTitleStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
)

val BodyStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
)

val LabelStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
)

val MetricStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 32.sp,
    lineHeight = 38.sp,
    fontFeatureSettings = TABULAR_NUMERALS_FEATURE,
)

val MetricTabularSmallStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    fontFeatureSettings = TABULAR_NUMERALS_FEATURE,
)

@Immutable
data class CustomTypography(
    val screenTitle: TextStyle = ScreenTitleStyle,
    val sectionTitle: TextStyle = SectionTitleStyle,
    val body: TextStyle = BodyStyle,
    val label: TextStyle = LabelStyle,
    val metric: TextStyle = MetricStyle,
    val metricTabularSmall: TextStyle = MetricTabularSmallStyle,
)

val AppTypography = Typography(
    headlineMedium = ScreenTitleStyle,
    titleLarge = ScreenTitleStyle,
    titleMedium = SectionTitleStyle,
    bodyLarge = BodyStyle,
    bodyMedium = BodyStyle,
    labelLarge = LabelStyle,
    labelMedium = LabelStyle,
    displayMedium = MetricStyle,
)

val LocalCustomTypography = staticCompositionLocalOf { CustomTypography() }
