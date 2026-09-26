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
    primary = Color(0xFF0F766E),        // Modern athletic deep teal
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF115E59),
    secondary = Color(0xFF4F46E5),      // Athletic royal indigo
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF3730A3),
    tertiary = Color(0xFFD97706),       // Amber accent
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),     // Crisp, clean light background
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),        // Pure white card surfaces
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF134E48),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFF818CF8),
    onSecondary = Color(0xFF1E1B4B),
    tertiary = Color(0xFFFBBF24),
    background = Color(0xFF0B1117),
    surface = Color(0xFF101820),
    surfaceVariant = Color(0xFF253241),
    error = Color(0xFFFFB4AB),
)

@Composable
fun GymTrackerTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    dynamicColor: Boolean = false,
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
