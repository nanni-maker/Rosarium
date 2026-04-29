package com.cambria.rosarium.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val RosariumDarkColorScheme = darkColorScheme(
    primary = RosariumGold,
    onPrimary = RosariumBlueNight,

    secondary = RosariumGoldSoft,
    onSecondary = RosariumBlueNight,

    tertiary = RosariumBlueSoft,
    onTertiary = RosariumTextPrimary,

    background = RosariumBackground,
    onBackground = RosariumTextPrimary,

    surface = RosariumSurface,
    onSurface = RosariumTextPrimary,

    surfaceVariant = RosariumSurfaceVariant,
    onSurfaceVariant = RosariumTextSecondary,

    error = RosariumError,
    onError = RosariumBlueNight,

    outline = RosariumGoldSoft,
    outlineVariant = RosariumBlueSoft
)

@Composable
fun RosariumTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme: ColorScheme =
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        } else {
            RosariumDarkColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}