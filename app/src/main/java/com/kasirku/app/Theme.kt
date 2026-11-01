package com.kasirku.app

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val Light = lightColorScheme(
    primary = Color(0xFF0F766E), onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1), onPrimaryContainer = Color(0xFF042F2E),
    secondary = Color(0xFF4F46E5), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E7FF), onSecondaryContainer = Color(0xFF1E1B4B),
    background = Color(0xFFF4F7F9), onBackground = Color(0xFF0F172A),
    surface = Color.White, onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0), onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
    error = Color(0xFFDC2626), errorContainer = Color(0xFFFEE2E2), onErrorContainer = Color(0xFF7F1D1D),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF1F5F9), surfaceContainerHigh = Color(0xFFE8EEF3),
    surfaceContainerHighest = Color(0xFFE2E8F0)
)

private val Dark = darkColorScheme(
    primary = Color(0xFF2DD4BF), onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF134E4A), onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFF818CF8), onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFF312E81), onSecondaryContainer = Color(0xFFE0E7FF),
    background = Color(0xFF0B1220), onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF111A2B), onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF1E293B), onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569),
    error = Color(0xFFF87171), errorContainer = Color(0xFF7F1D1D), onErrorContainer = Color(0xFFFEE2E2),
    surfaceContainerLowest = Color(0xFF0B1220), surfaceContainerLow = Color(0xFF142033),
    surfaceContainer = Color(0xFF18253B), surfaceContainerHigh = Color(0xFF1E2D47),
    surfaceContainerHighest = Color(0xFF253654)
)

@Composable
fun KasirTheme(dark: Boolean, content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = Typography(), content = content)
}
