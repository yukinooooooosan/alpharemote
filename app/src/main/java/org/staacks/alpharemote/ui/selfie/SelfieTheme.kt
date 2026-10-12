package org.staacks.alpharemote.ui.selfie

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.staacks.alpharemote.R
import org.staacks.alpharemote.ui.appearance.AppSkin

// Simple skin: one dark "viewfinder" palette for all four pages and the tab bar, like a camera's own display.
// It ignores the system light/dark setting on purpose. No per-device dynamic colors.
private val FinderColors = darkColorScheme(
    primary = Color(0xFFFFB020), onPrimary = Color(0xFF0D0E0F),
    primaryContainer = Color(0xFF3A2C0E), onPrimaryContainer = Color(0xFFFFD98A),
    secondary = Color(0xFFB9B9B0), onSecondary = Color(0xFF0D0E0F),
    secondaryContainer = Color(0xFF24262A), onSecondaryContainer = Color(0xFFEDEDE8),
    background = Color(0xFF0D0E0F), onBackground = Color(0xFFEDEDE8),
    surface = Color(0xFF151618), onSurface = Color(0xFFEDEDE8),
    surfaceContainerLowest = Color(0xFF0D0E0F), surfaceContainerLow = Color(0xFF151618),
    surfaceContainer = Color(0xFF1B1D20), surfaceContainerHigh = Color(0xFF202226),
    surfaceContainerHighest = Color(0xFF24262A), onSurfaceVariant = Color(0xFF9A9A92),
    outline = Color(0xFF55585C), outlineVariant = Color(0xFF2C2E30),
    error = Color(0xFFFF4D4D), onError = Color(0xFF0D0E0F),
)

/** Fonts of the Simple (viewfinder) look, shared by all pages. */
object FinderFonts {
    val Display = FontFamily(Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold), Font(R.font.barlow_condensed_bold, FontWeight.Bold))
    val Mono = FontFamily(Font(R.font.ibm_plex_mono_medium, FontWeight.Medium), Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold))
}

val LocalSelfieSkin = staticCompositionLocalOf { AppSkin.SIMPLE }

private val KawaiiLightColors = lightColorScheme(
    primary = Color(0xFFB82F75), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD8EB), onPrimaryContainer = Color(0xFF61123F),
    secondary = Color(0xFF7850A0), onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEDDFA), onSecondaryContainer = Color(0xFF482763),
    background = Color(0xFFFFF5FA), onBackground = Color(0xFF412535),
    surface = Color(0xFFFFFDFF), onSurface = Color(0xFF412535),
    surfaceContainerLow = Color(0xFFFFFDFF), surfaceContainer = Color(0xFFFFE9F4),
    surfaceContainerHighest = Color(0xFFF2E6FB), onSurfaceVariant = Color(0xFF795068),
    outline = Color(0xFFBC80A2), outlineVariant = Color(0xFFEAC2D9),
    error = Color(0xFFB3261E), onError = Color.White,
)
private val KawaiiDarkColors = darkColorScheme(
    primary = Color(0xFFF5A5D1), onPrimary = Color(0xFF591239),
    primaryContainer = Color(0xFF65324E), onPrimaryContainer = Color(0xFFFFD8EB),
    secondary = Color(0xFFDDB9F7), onSecondary = Color(0xFF422452),
    secondaryContainer = Color(0xFF4C345D), onSecondaryContainer = Color(0xFFF3DAFF),
    background = Color(0xFF261823), onBackground = Color(0xFFFFEAF5),
    surface = Color(0xFF4A293E), onSurface = Color(0xFFFFEAF5),
    surfaceContainerLow = Color(0xFF34222F), surfaceContainer = Color(0xFF3D2838),
    surfaceContainerHighest = Color(0xFF443047), onSurfaceVariant = Color(0xFFDEB7D0),
    outline = Color(0xFFBC83A8), outlineVariant = Color(0xFF78516D),
    error = Color(0xFFFFB4AB),
)
private val SimpleTypography = Typography()
private val KawaiiTypography = SimpleTypography.copy(
    headlineLarge = SimpleTypography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
    headlineSmall = SimpleTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = SimpleTypography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
    titleMedium = SimpleTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
    titleSmall = SimpleTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
    labelLarge = SimpleTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun SelfieTheme(darkTheme: Boolean = isSystemInDarkTheme(), skin: AppSkin = AppSkin.SIMPLE,
                content: @Composable () -> Unit) {
    val colors = when (skin) {
        AppSkin.SIMPLE -> FinderColors
        AppSkin.KAWAII -> if (darkTheme) KawaiiDarkColors else KawaiiLightColors
    }
    CompositionLocalProvider(LocalSelfieSkin provides skin) {
        MaterialTheme(colorScheme = colors,
            typography = if (skin == AppSkin.KAWAII) KawaiiTypography else SimpleTypography, content = content)
    }
}
