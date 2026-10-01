package com.goreecloud.clock.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val ClockLightColors = lightColorScheme(
    primary = Color(0xFF0F656A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2F0EF),
    onPrimaryContainer = Color(0xFF073D41),
    secondary = Color(0xFF536565),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE9E8),
    onSecondaryContainer = Color(0xFF263D3E),
    tertiary = Color(0xFF8A650E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE7A8),
    onTertiaryContainer = Color(0xFF493500),
    background = Color(0xFFF5F8F7),
    onBackground = Color(0xFF172021),
    surface = Color(0xFFFBFDFC),
    onSurface = Color(0xFF172021),
    surfaceVariant = Color(0xFFE6ECEB),
    onSurfaceVariant = Color(0xFF465252),
    surfaceContainerLow = Color(0xFFF0F5F4),
    surfaceContainer = Color(0xFFE9F0EF),
    surfaceContainerHigh = Color(0xFFE1E9E8),
    surfaceContainerHighest = Color(0xFFD9E3E1),
    outline = Color(0xFF748180),
    outlineVariant = Color(0xFFC3CFCD),
)

private val ClockDarkColors = darkColorScheme(
    primary = Color(0xFF92D6D5),
    onPrimary = Color(0xFF00373A),
    primaryContainer = Color(0xFF164F53),
    onPrimaryContainer = Color(0xFFC3EEEE),
    secondary = Color(0xFFB7CCCB),
    onSecondary = Color(0xFF213738),
    secondaryContainer = Color(0xFF374D4E),
    onSecondaryContainer = Color(0xFFD2E7E5),
    tertiary = Color(0xFFF0C66C),
    onTertiary = Color(0xFF432C00),
    tertiaryContainer = Color(0xFF624500),
    onTertiaryContainer = Color(0xFFFFE3A3),
    background = Color(0xFF0D1415),
    onBackground = Color(0xFFDDE5E3),
    surface = Color(0xFF11191A),
    onSurface = Color(0xFFDDE5E3),
    surfaceVariant = Color(0xFF293334),
    onSurfaceVariant = Color(0xFFC1CBC9),
    surfaceContainerLow = Color(0xFF151E1F),
    surfaceContainer = Color(0xFF1A2425),
    surfaceContainerHigh = Color(0xFF202B2C),
    surfaceContainerHighest = Color(0xFF283334),
    outline = Color(0xFF899493),
    outlineVariant = Color(0xFF3E4949),
)

private val ClockShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * GoreeCloud Clock visual theme aligned to the current official GLAZE UI 1.6.0 anchor.
 * Consumer acceptance remains repository-local; native Material 3 primitives preserve
 * Android accessibility, scaling, focus, and platform semantics.
 */
@Composable
fun ClockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) ClockDarkColors else ClockLightColors,
        shapes = ClockShapes,
    ) {
        ClockSystemBars(darkTheme)
        content()
    }
}

@Composable
private fun ClockSystemBars(darkTheme: Boolean) {
    val view = LocalView.current
    val background = MaterialTheme.colorScheme.background.toArgb()
    SideEffect {
        val activity = view.context.findActivity() ?: return@SideEffect
        activity.window.statusBarColor = background
        activity.window.navigationBarColor = background
        activity.window.isNavigationBarContrastEnforced = false
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
