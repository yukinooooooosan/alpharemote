package org.staacks.alpharemote.ui.selfie

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SelfieLightColors = lightColorScheme(
    primary = Color(0xFF456818), onPrimary = Color.White,
    primaryContainer = Color(0xFFD4EDAE), onPrimaryContainer = Color(0xFF253911),
    background = Color(0xFFF3F6ED), onBackground = Color(0xFF20271C),
    surface = Color(0xFFF3F6ED), onSurface = Color(0xFF20271C),
    surfaceVariant = Color(0xFFE4EBD9), onSurfaceVariant = Color(0xFF52604D),
    outline = Color(0xFF687660), outlineVariant = Color(0xFFBBC5AD),
)
private val SelfieDarkColors = darkColorScheme(
    primary = Color(0xFFDBF79B), onPrimary = Color(0xFF182018),
    primaryContainer = Color(0xFF35452B), onPrimaryContainer = Color(0xFFDBF79B),
    background = Color(0xFF182018), onBackground = Color(0xFFF4F5EC),
    surface = Color(0xFF182018), onSurface = Color(0xFFF4F5EC),
    surfaceVariant = Color(0xFF252B25), onSurfaceVariant = Color(0xFFADB7A5),
    outline = Color(0xFF94A386), outlineVariant = Color(0xFF465046),
)

@Composable
fun SelfieTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) SelfieDarkColors else SelfieLightColors, content = content)
}
