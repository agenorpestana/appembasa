package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = HydroBlueLight,
    onPrimary = HydroNavyDark,
    primaryContainer = HydroNavy,
    onPrimaryContainer = HydroBlueLight,
    secondary = HydroTealLight,
    onSecondary = HydroNavyDark,
    secondaryContainer = Color(0xFF0F3B37),
    onSecondaryContainer = HydroTealLight,
    tertiary = HydroCyan,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceContainerDark,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    error = StatusCritical,
    onError = Color.White
  )

private val LightColorScheme =
  lightColorScheme(
    primary = HydroBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = HydroNavy,
    secondary = HydroTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF003831),
    tertiary = HydroCyan,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceContainerLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    error = StatusCritical,
    onError = Color.White
  )

@Composable
fun HydroScanTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use our high-impact water brand theme by default
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

// Keep MyApplicationTheme alias for backward compatibility
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  HydroScanTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
