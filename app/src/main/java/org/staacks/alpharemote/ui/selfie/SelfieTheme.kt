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
import androidx.compose.ui.text.font.FontWeight
import org.staacks.alpharemote.ui.appearance.AppSkin

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
        AppSkin.SIMPLE -> if (darkTheme) SelfieDarkColors else SelfieLightColors
        AppSkin.KAWAII -> if (darkTheme) KawaiiDarkColors else KawaiiLightColors
    }
    CompositionLocalProvider(LocalSelfieSkin provides skin) {
        MaterialTheme(colorScheme = colors,
            typography = if (skin == AppSkin.KAWAII) KawaiiTypography else SimpleTypography, content = content)
    }
}
