package com.gymtracker.app.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Spacing(
    val xxxSmall: Dp = 2.dp,
    val xxSmall: Dp = 4.dp,
    val xSmall: Dp = 8.dp,
    val small: Dp = 12.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val xLarge: Dp = 32.dp,
    val xxLarge: Dp = 48.dp,

    // Semantic Layout Tokens
    val phoneGutter: Dp = 16.dp,
    val controlSpacing: Dp = 12.dp,
    val cardPadding: Dp = 16.dp,

    // Dense Row Exceptions
    val denseRowVerticalPadding: Dp = 6.dp,
    val denseRowHorizontalPadding: Dp = 8.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
