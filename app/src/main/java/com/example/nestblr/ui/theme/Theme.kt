package com.example.nestblr.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Rose600,
    onPrimary = White,
    primaryContainer = Rose100,
    onPrimaryContainer = Rose900,

    secondary = Teal700,
    onSecondary = White,
    secondaryContainer = Teal100,
    onSecondaryContainer = Teal900,

    tertiary = Amber500,
    onTertiary = White,
    tertiaryContainer = Amber100,
    onTertiaryContainer = Amber900,

    background = Gray50,
    onBackground = Gray900,
    surface = White,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray500,

    outline = Gray200,
    outlineVariant = Gray100,

    error = Red600,
    onError = White,
)

private val DarkColors = darkColorScheme(
    primary = Rose400,
    onPrimary = White,
    primaryContainer = Rose900,
    onPrimaryContainer = Rose100,

    secondary = Teal400,
    onSecondary = Neutral950,
    secondaryContainer = Teal900,
    onSecondaryContainer = Teal100,

    tertiary = Amber300,
    onTertiary = Neutral950,
    tertiaryContainer = Amber900,
    onTertiaryContainer = Amber100,

    background = Neutral950,
    onBackground = Gray100,
    surface = Neutral900,
    onSurface = Gray100,
    surfaceVariant = Neutral800,
    onSurfaceVariant = Gray500,

    outline = Neutral700,
    outlineVariant = Neutral800,

    error = Red400,
    onError = Neutral950,
)

@Composable
fun NestBLRTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Brand colors win over Material You.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
