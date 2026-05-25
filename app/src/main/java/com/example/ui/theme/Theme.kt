package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ProfessionalPrimaryDark,
    onPrimary = ProfessionalOnPrimaryDark,
    primaryContainer = ProfessionalPrimaryContainerDark,
    onPrimaryContainer = ProfessionalOnPrimaryContainerDark,
    secondary = ProfessionalSecondaryDark,
    onSecondary = ProfessionalOnSecondaryDark,
    secondaryContainer = ProfessionalSecondaryContainerDark,
    onSecondaryContainer = ProfessionalOnSecondaryContainerDark,
    tertiary = ProfessionalTertiaryDark,
    onTertiary = ProfessionalOnTertiaryDark,
    background = ProfessionalBackgroundDark,
    onBackground = ProfessionalOnBackgroundDark,
    surface = ProfessionalSurfaceDark,
    onSurface = ProfessionalOnSurfaceDark,
    surfaceVariant = ProfessionalSurfaceVariantDark,
    onSurfaceVariant = ProfessionalOnSurfaceVariantDark,
    outline = ProfessionalOutlineDark,
    outlineVariant = ProfessionalOutlineVariantDark,
    error = ProfessionalErrorDark
)

private val LightColorScheme = lightColorScheme(
    primary = ProfessionalPrimaryLight,
    onPrimary = ProfessionalOnPrimaryLight,
    primaryContainer = ProfessionalPrimaryContainerLight,
    onPrimaryContainer = ProfessionalOnPrimaryContainerLight,
    secondary = ProfessionalSecondaryLight,
    onSecondary = ProfessionalOnSecondaryLight,
    secondaryContainer = ProfessionalSecondaryContainerLight,
    onSecondaryContainer = ProfessionalOnSecondaryContainerLight,
    tertiary = ProfessionalTertiaryLight,
    onTertiary = ProfessionalOnTertiaryLight,
    background = ProfessionalBackgroundLight,
    onBackground = ProfessionalOnBackgroundLight,
    surface = ProfessionalSurfaceLight,
    onSurface = ProfessionalOnSurfaceLight,
    surfaceVariant = ProfessionalSurfaceVariantLight,
    onSurfaceVariant = ProfessionalOnSurfaceVariantLight,
    outline = ProfessionalOutlineLight,
    outlineVariant = ProfessionalOutlineVariantLight,
    error = ProfessionalErrorLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Make dynamicColor false by default to strictly enforce our premium design palette
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
