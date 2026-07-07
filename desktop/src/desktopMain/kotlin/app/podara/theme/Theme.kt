package app.podara.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class PodaraColors(
    val background: androidx.compose.ui.graphics.Color,
    val surface: androidx.compose.ui.graphics.Color,
    val elevated: androidx.compose.ui.graphics.Color,
    val border: androidx.compose.ui.graphics.Color,
    val divider: androidx.compose.ui.graphics.Color,
    val textPrimary: androidx.compose.ui.graphics.Color,
    val textSecondary: androidx.compose.ui.graphics.Color,
    val textMuted: androidx.compose.ui.graphics.Color,
    val textDisabled: androidx.compose.ui.graphics.Color,
    val accent: androidx.compose.ui.graphics.Color,
    val accentHover: androidx.compose.ui.graphics.Color,
    val accentPressed: androidx.compose.ui.graphics.Color,
    val success: androidx.compose.ui.graphics.Color,
    val warning: androidx.compose.ui.graphics.Color,
    val danger: androidx.compose.ui.graphics.Color,
    val info: androidx.compose.ui.graphics.Color
)

private val DarkPodaraColors = PodaraColors(
    background = PodiumBackground,
    surface = PodiumSurface,
    elevated = PodiumElevated,
    border = PodiumBorder,
    divider = PodiumDivider,
    textPrimary = PodiumTextPrimary,
    textSecondary = PodiumTextSecondary,
    textMuted = PodiumTextMuted,
    textDisabled = PodiumTextDisabled,
    accent = PodiumAccent,
    accentHover = PodiumAccentHover,
    accentPressed = PodiumAccentPressed,
    success = PodiumSuccess,
    warning = PodiumWarning,
    danger = PodiumDanger,
    info = PodiumInfo
)

private val LightPodaraColors = PodaraColors(
    background = BackgroundLight,
    surface = SurfaceLight,
    elevated = SurfaceLight,
    border = PodiumBorder,
    divider = PodiumDivider,
    textPrimary = OnBackgroundLight,
    textSecondary = SecondaryLight,
    textMuted = PodiumTextMuted,
    textDisabled = PodiumTextDisabled,
    accent = PrimaryLight,
    accentHover = PodiumAccentHover,
    accentPressed = PodiumAccentPressed,
    success = PodiumSuccess,
    warning = PodiumWarning,
    danger = PodiumDanger,
    info = PodiumInfo
)

val LocalPodaraColors = staticCompositionLocalOf { DarkPodaraColors }

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

@Composable
fun PodaraTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val podiumColors = if (darkTheme) DarkPodaraColors else LightPodaraColors

    CompositionLocalProvider(LocalPodaraColors provides podiumColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

object PodaraTheme {
    val colors: PodaraColors
        @Composable
        get() = LocalPodaraColors.current
}
