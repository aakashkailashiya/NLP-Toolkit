package com.example.exudesimulator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NavyBlue = Color(0xFF000080)
private val PaperYellow = Color(0xFFFDFD96)

private val DarkColorScheme = darkColorScheme(
    primary = PaperYellow,
    onPrimary = Color.Black,
    secondary = PaperYellow,
    tertiary = PaperYellow,
    background = Color.Black,
    surface = Color.Black,
    onSurface = Color.White,
    onSurfaceVariant = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = NavyBlue,
    onPrimary = Color.White,
    secondary = NavyBlue,
    tertiary = NavyBlue,
    background = Color.White,
    surface = Color.White,
    onSurface = Color.Black,
    onSurfaceVariant = Color.Black
)

@Composable
fun ExudeSimulatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
