package com.example.vocabquiz.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2F6FED),
    onPrimary = Color.White,
    secondary = Color(0xFF7B61FF),
    tertiary = Color(0xFF00BFA5),
    surface = Color(0xFFF9FAFF),
    onSurface = Color(0xFF1B1B1F)
)

private val DarkColorScheme  = darkColorScheme(
    primary = Color(0xFFAEC6FF),
    onPrimary = Color(0xFF0A1B3A),
    secondary = Color(0xFFC7B8FF),
    tertiary = Color(0xFF66F2DD),
    surface = Color(0xFF0E1116),
    onSurface = Color(0xFFE7E9F0)
)

@Composable
fun VocabQuizTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
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
        shapes = Shapes(),
        content = content
    )
}