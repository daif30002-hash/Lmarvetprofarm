package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MedicalGreen,
    secondary = EmeraldSecondary,
    tertiary = AccentYellow,
    background = DeepDarkBackground,
    surface = CardDarkBackground,
    onPrimary = DeepDarkBackground,
    onSecondary = ColorWhite,
    onBackground = ColorWhite,
    onSurface = ColorWhite,
    surfaceVariant = Color(0x0FFFFFFF), // white/5 or white/6 trans
    onSurfaceVariant = ColorWhite
)

private val LightColorScheme = darkColorScheme(
    primary = NavyPrimary,
    secondary = EmeraldSecondary,
    tertiary = MedicalGreen,
    background = LightBackground,
    surface = CardDarkBackground,
    onPrimary = DeepDarkBackground,
    onSecondary = ColorWhite,
    onBackground = ColorWhite,
    onSurface = ColorWhite,
    surfaceVariant = Color(0x0FFFFFFF), // white/5
    onSurfaceVariant = ColorWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamic color capability or disable to enforce our custom branding
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
