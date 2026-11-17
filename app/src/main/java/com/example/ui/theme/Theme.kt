package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

enum class ThemeModePreference {
    SYSTEM, LIGHT, DARK
}

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = PureWhite,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = EmeraldOnContainerLight,
    secondary = EmeraldDark,
    onSecondary = PureWhite,
    secondaryContainer = Color(0xFFE2F5EC),
    onSecondaryContainer = EmeraldOnContainerLight,
    tertiary = InfoBlue,
    onTertiary = PureWhite,
    background = MintBackground,
    onBackground = SlateTextPrimary,
    surface = PureWhite,
    onSurface = SlateTextPrimary,
    surfaceVariant = Color(0xFFEEF5F1),
    onSurfaceVariant = SlateTextSecondary,
    outline = SlateBorderLight,
    error = DangerRed,
    onError = PureWhite,
    errorContainer = DangerContainer,
    onErrorContainer = Color(0xFF7F1D1D),
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldLight,
    onPrimary = Color(0xFF042F21),
    primaryContainer = Color(0xFF065F46),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = EmeraldPrimary,
    onSecondary = PureWhite,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF082F49),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
)

val TaliKhataShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun TaliKhataTheme(
    themePreference: ThemeModePreference = ThemeModePreference.SYSTEM,
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themePreference) {
        ThemeModePreference.LIGHT -> false
        ThemeModePreference.DARK -> true
        ThemeModePreference.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TaliKhataTypography,
        shapes = TaliKhataShapes,
        content = content,
    )
}
