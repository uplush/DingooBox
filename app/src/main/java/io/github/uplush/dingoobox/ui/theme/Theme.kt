package io.github.uplush.dingoobox.ui.theme

import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.github.uplush.dingoobox.AppThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF456A98),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE9EDF3),
    onPrimaryContainer = Color(0xFF1C2028),
    secondary = Color(0xFF53647C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE9EDF3),
    onSecondaryContainer = Color(0xFF1C2028),
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF1C2028),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C2028),
    surfaceVariant = Color(0xFFE9EDF3),
    onSurfaceVariant = Color(0xFF68717D),
    outline = Color(0xFFC5CCD6),
    outlineVariant = Color(0xFFD9DEE7)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6A89B0),
    onPrimary = Color(0xFF101318),
    primaryContainer = Color(0xFF222730),
    onPrimaryContainer = Color(0xFFF1F3F5),
    secondary = Color(0xFFA9B6CB),
    onSecondary = Color(0xFF101318),
    secondaryContainer = Color(0xFF222730),
    onSecondaryContainer = Color(0xFFF1F3F5),
    background = Color(0xFF101318),
    onBackground = Color(0xFFF1F3F5),
    surface = Color(0xFF181C22),
    onSurface = Color(0xFFF1F3F5),
    surfaceVariant = Color(0xFF222730),
    onSurfaceVariant = Color(0xFF9DA5B0),
    outline = Color(0xFF3A424D),
    outlineVariant = Color(0xFF2C333D)
)

@Composable
fun DingooTheme(mode: AppThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val colors = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as ComponentActivity
            val window = activity.window
            val systemBarBackgroundColor = colors.background.toArgb()

            window.decorView.setBackgroundColor(systemBarBackgroundColor)

            val systemBarStyle =
                if (dark) {
                    SystemBarStyle.dark(systemBarBackgroundColor)
                } else {
                    SystemBarStyle.light(
                        systemBarBackgroundColor,
                        systemBarBackgroundColor
                    )
                }

            activity.enableEdgeToEdge(
                statusBarStyle = systemBarStyle,
                navigationBarStyle = systemBarStyle
            )

            // MiNiQ keeps the decor laid out from the physical screen origin;
            // this is required for Dialog and activity coordinates to agree.
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
