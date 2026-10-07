package org.staacks.alpharemote.ui.selfie

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// One restrained palette for all four pages and navigation. No per-device dynamic colors.
private val SelfieLightColors = lightColorScheme(
    primary = Color(0xFF506A47), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8D5), onPrimaryContainer = Color(0xFF253820),
    secondaryContainer = Color(0xFFE4E7DF), onSecondaryContainer = Color(0xFF282F25),
    background = Color(0xFFF7F7F2), onBackground = Color(0xFF22251F),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF22251F),
    surfaceContainerLow = Color(0xFFFFFFFF), surfaceContainer = Color(0xFFF0F1EA),
    surfaceContainerHighest = Color(0xFFEDEFE7), onSurfaceVariant = Color(0xFF5C6357),
    outline = Color(0xFF747D6E), outlineVariant = Color(0xFFD9DED2),
)
private val SelfieDarkColors = darkColorScheme(
    primary = Color(0xFFBDD5AD), onPrimary = Color(0xFF20351A),
    primaryContainer = Color(0xFF354B2C), onPrimaryContainer = Color(0xFFDBECD1),
    secondaryContainer = Color(0xFF30372D), onSecondaryContainer = Color(0xFFE4E9DF),
    background = Color(0xFF171A16), onBackground = Color(0xFFF0F2E9),
    surface = Color(0xFF22271F), onSurface = Color(0xFFF0F2E9),
    surfaceContainerLow = Color(0xFF22271F), surfaceContainer = Color(0xFF272D24),
    surfaceContainerHighest = Color(0xFF30372B), onSurfaceVariant = Color(0xFFB6BFAE),
    outline = Color(0xFF88937E), outlineVariant = Color(0xFF434C3C),
)

@Composable
fun SelfieTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) SelfieDarkColors else SelfieLightColors, content = content)
}
