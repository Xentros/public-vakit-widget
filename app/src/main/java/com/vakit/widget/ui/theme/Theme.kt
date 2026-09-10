package com.vakit.widget.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7EFB5),
    onPrimaryContainer = Color(0xFF002106),
    secondary = Color(0xFF52634F),
    background = Color(0xFFFDFDF6),
    surface = Color(0xFFFDFDF6),
    surfaceVariant = Color(0xFFE1E4DC),
    onSurfaceVariant = Color(0xFF414941),
    outline = Color(0xFF71796F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CD59A),
    onPrimary = Color(0xFF00390F),
    primaryContainer = Color(0xFF175323),
    onPrimaryContainer = Color(0xFFB7EFB5),
    secondary = Color(0xFFB9CCB4),
    background = Color(0xFF111411),
    surface = Color(0xFF111411),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC2C9BF),
    outline = Color(0xFF8C9389),
)

@Composable
fun VakitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
