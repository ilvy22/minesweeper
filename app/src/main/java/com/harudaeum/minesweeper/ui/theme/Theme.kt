package com.harudaeum.minesweeper.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Shared 하루 series tokens. Change only accent tokens for the next app.
val Coral = Color(0xFFFF6F5C)
val CoralInk = Color(0xFFB92D27)
val Sage = Color(0xFF4C8B77)
val Sun = Color(0xFFFFC857)
private val LightColors = lightColorScheme(
    primary = CoralInk, onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE7E1), onPrimaryContainer = Color(0xFF862A22),
    secondary = Sage, onSecondary = Color.White,
    secondaryContainer = Color(0xFFDFF3EB), onSecondaryContainer = Color(0xFF173E32),
    tertiary = Color(0xFF85611B), tertiaryContainer = Color(0xFFFFF2D0),
    onTertiaryContainer = Color(0xFF604309),
    background = Color(0xFFFFFDFC), onBackground = Color(0xFF24201E),
    surface = Color.White, onSurface = Color(0xFF24201E),
    surfaceVariant = Color(0xFFF5F1EE), onSurfaceVariant = Color(0xFF726A66),
    outline = Color(0xFFCABCB4), outlineVariant = Color(0xFFE9E0DB),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4A8), onPrimary = Color(0xFF68120D),
    primaryContainer = Color(0xFF632D28), onPrimaryContainer = Color(0xFFFFDAD4),
    secondary = Color(0xFF9CD5C0), onSecondary = Color(0xFF003829),
    secondaryContainer = Color(0xFF234A3D), onSecondaryContainer = Color(0xFFDFF3EB),
    tertiary = Sun, tertiaryContainer = Color(0xFF504019), onTertiaryContainer = Color(0xFFFFE4A0),
    background = Color(0xFF171615), onBackground = Color(0xFFF3F0EF),
    surface = Color(0xFF211F1D), onSurface = Color(0xFFF3F0EF),
    surfaceVariant = Color(0xFF2D2926), onSurfaceVariant = Color(0xFFC9C4C1),
    outline = Color(0xFF847A73), outlineVariant = Color(0xFF403934),
)
val SeriesTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 42.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 34.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

@Composable
fun HaruTheme(dark: Boolean, content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = SeriesTypography,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp)),
        content = content,
    )
}
