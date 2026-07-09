package com.example.diywallpaper.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.diywallpaper.ui.theme.AppTypography
import com.example.diywallpaper.ui.theme.Background
import com.example.diywallpaper.ui.theme.Border
import com.example.diywallpaper.ui.theme.Divider
import com.example.diywallpaper.ui.theme.Pink
import com.example.diywallpaper.ui.theme.PinkSoft
import com.example.diywallpaper.ui.theme.PinkStrong
import com.example.diywallpaper.ui.theme.Primary
import com.example.diywallpaper.ui.theme.PrimarySoft
import com.example.diywallpaper.ui.theme.SkyBlue
import com.example.diywallpaper.ui.theme.SkySoft
import com.example.diywallpaper.ui.theme.SoftCard
import com.example.diywallpaper.ui.theme.Surface
import com.example.diywallpaper.ui.theme.TextPrimary
import com.example.diywallpaper.ui.theme.TextSecondary

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,

    secondary = Pink,
    onSecondary = Color.White,

    tertiary = SkyBlue,
    onTertiary = Color.White,

    background = Background,
    onBackground = TextPrimary,

    surface = Surface,
    onSurface = TextPrimary,

    surfaceVariant = SoftCard,
    onSurfaceVariant = TextSecondary,

    outline = Border,
    outlineVariant = Divider,

    primaryContainer = PrimarySoft,
    onPrimaryContainer = Primary,

    secondaryContainer = PinkSoft,
    onSecondaryContainer = PinkStrong,

    tertiaryContainer = SkySoft,
    onTertiaryContainer = SkyBlue
)
private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,

    secondary = Pink,
    onSecondary = Color.White,

    tertiary = SkyBlue,
    onTertiary = Color.White,

    background = Background,
    onBackground = TextPrimary,

    surface = Surface,
    onSurface = TextPrimary,

    surfaceVariant = SoftCard,
    onSurfaceVariant = TextSecondary,

    outline = Border,
    outlineVariant = Divider,

    primaryContainer = PrimarySoft,
    onPrimaryContainer = Primary,

    secondaryContainer = PinkSoft,
    onSecondaryContainer = PinkStrong,

    tertiaryContainer = SkySoft,
    onTertiaryContainer = SkyBlue
)

@Composable
fun DIYWallpaperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}