package com.gymtracker.app.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.gymtracker.app.data.local.entity.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF146C63),
    onPrimary = Color.White,
    secondary = Color(0xFF6B5B95),
    tertiary = Color(0xFFB45F06),
    background = Color(0xFFF7F9FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE2E7EF),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF42D6B5),
    onPrimary = Color(0xFF003731),
    secondary = Color(0xFFD2B8FF),
    tertiary = Color(0xFFFFB36B),
    background = Color(0xFF0B1117),
    surface = Color(0xFF101820),
    surfaceVariant = Color(0xFF253241),
    error = Color(0xFFFFB4AB),
)

@Composable
fun GymTrackerTheme(
    themeMode: ThemeMode,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) {
        DarkColors
    } else {
        LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
