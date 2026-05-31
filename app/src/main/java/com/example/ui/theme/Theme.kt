package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

enum class AppTheme(val displayName: String, val icon: ImageVector) {
    EMERALD_SPIRIT("Emerald Spirit", Icons.Default.Favorite),
    GOLDEN_DESERT("Golden Desert", Icons.Default.Notifications),
    ROYAL_VELVET("Royal Velvet", Icons.Default.Star),
    CHARCOAL_MINIMALIST("Slate Cyber", Icons.Default.Settings),
    MIDNIGHT_KAABA("Sacred Kaaba", Icons.Default.Home)
}

// 1. EMERALD_SPIRIT
private val EmeraldDark = darkColorScheme(
    primary = SageMint,
    secondary = MysticEmerald,
    tertiary = WarmDuskAmber,
    background = MosqueMidnightDark,
    surface = MosqueSurfaceDark,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = OnMosqueDark,
    onSurface = OnMosqueDark,
    primaryContainer = SageMint.copy(alpha = 0.25f),
    secondaryContainer = MosqueSurfaceDark,
    tertiaryContainer = WarmDuskAmber.copy(alpha = 0.2f),
    error = Color(0xFFEF4444)
)

private val EmeraldLight = lightColorScheme(
    primary = MysticEmerald,
    secondary = SageMint,
    tertiary = WarmDuskAmber,
    background = PureIvoryLight,
    surface = SurfaceIvoryLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = OnSurfaceIvoryLight,
    onSurface = OnSurfaceIvoryLight,
    primaryContainer = MysticEmerald.copy(alpha = 0.12f),
    secondaryContainer = SurfaceIvoryLight,
    tertiaryContainer = WarmDuskAmber.copy(alpha = 0.15f),
    error = Color(0xFFDC2626)
)

// 2. GOLDEN_DESERT
private val DesertDark = darkColorScheme(
    primary = DesertGoldPrimary,
    secondary = SunburstSecondary,
    tertiary = OasisTealTertiary,
    background = DesertDarkBg,
    surface = DesertDarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = DesertOnDarkText,
    onSurface = DesertOnDarkText,
    primaryContainer = DesertGoldPrimary.copy(alpha = 0.25f),
    secondaryContainer = DesertDarkSurface,
    tertiaryContainer = OasisTealTertiary.copy(alpha = 0.2f),
    error = Color(0xFFEF4444)
)

private val DesertLight = lightColorScheme(
    primary = DesertGoldPrimary,
    secondary = SunburstSecondary,
    tertiary = OasisTealTertiary,
    background = DesertLightBg,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFF451A03),
    onSurface = Color(0xFF451A03),
    primaryContainer = DesertGoldPrimary.copy(alpha = 0.12f),
    secondaryContainer = Color.White,
    tertiaryContainer = OasisTealTertiary.copy(alpha = 0.15f),
    error = Color(0xFFDC2626)
)

// 3. ROYAL_VELVET
private val VelvetDark = darkColorScheme(
    primary = MidnightPlumPrimary,
    secondary = SoftLavenderSecondary,
    tertiary = SunsetRoseTertiary,
    background = MidnightPlumDarkBg,
    surface = MidnightPlumSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = VioletOnDarkText,
    onSurface = VioletOnDarkText,
    primaryContainer = MidnightPlumPrimary.copy(alpha = 0.25f),
    secondaryContainer = MidnightPlumSurface,
    tertiaryContainer = SunsetRoseTertiary.copy(alpha = 0.2f),
    error = Color(0xFFEF4444)
)

private val VelvetLight = lightColorScheme(
    primary = MidnightPlumPrimary,
    secondary = SoftLavenderSecondary,
    tertiary = SunsetRoseTertiary,
    background = VioletLightBg,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF3B0764),
    onSurface = Color(0xFF3B0764),
    primaryContainer = MidnightPlumPrimary.copy(alpha = 0.12f),
    secondaryContainer = Color.White,
    tertiaryContainer = SunsetRoseTertiary.copy(alpha = 0.15f),
    error = Color(0xFFDC2626)
)

// 4. CHARCOAL_MINIMALIST
private val CyberDark = darkColorScheme(
    primary = SlatePrimary,
    secondary = ElectricEmeraldSecondary,
    tertiary = CyberAmberTertiary,
    background = CyberDarkBg,
    surface = CyberSurfaceDark,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = CyberOnDarkText,
    onSurface = CyberOnDarkText,
    primaryContainer = SlatePrimary.copy(alpha = 0.25f),
    secondaryContainer = CyberSurfaceDark,
    tertiaryContainer = CyberAmberTertiary.copy(alpha = 0.2f),
    error = Color(0xFFEF4444)
)

private val CyberLight = lightColorScheme(
    primary = SlatePrimary,
    secondary = ElectricEmeraldSecondary,
    tertiary = CyberAmberTertiary,
    background = CyberLightBg,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF1E293B),
    primaryContainer = SlatePrimary.copy(alpha = 0.12f),
    secondaryContainer = Color.White,
    tertiaryContainer = CyberAmberTertiary.copy(alpha = 0.15f),
    error = Color(0xFFDC2626)
)

// 5. MIDNIGHT_KAABA
private val KaabaDark = darkColorScheme(
    primary = KiswahGoldPrimary,
    secondary = SilkCharcoalSecondary,
    tertiary = SilverTrimTertiary,
    background = SacredOnyxDarkBg,
    surface = SacredSurfaceDark,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = SacredOnDarkText,
    onSurface = SacredOnDarkText,
    primaryContainer = KiswahGoldPrimary.copy(alpha = 0.25f),
    secondaryContainer = SacredSurfaceDark,
    tertiaryContainer = SilverTrimTertiary.copy(alpha = 0.2f),
    error = Color(0xFFEF4444)
)

private val KaabaLight = lightColorScheme(
    primary = KiswahGoldPrimary,
    secondary = SilkCharcoalSecondary,
    tertiary = SilverTrimTertiary,
    background = AlabasterLightBg,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1917),
    onSurface = Color(0xFF1C1917),
    primaryContainer = KiswahGoldPrimary.copy(alpha = 0.12f),
    secondaryContainer = Color.White,
    tertiaryContainer = SilverTrimTertiary.copy(alpha = 0.15f),
    error = Color(0xFFDC2626)
)


@Composable
fun MyApplicationTheme(
  theme: AppTheme = AppTheme.EMERALD_SPIRIT,
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = when (theme) {
      AppTheme.EMERALD_SPIRIT -> if (darkTheme) EmeraldDark else EmeraldLight
      AppTheme.GOLDEN_DESERT -> if (darkTheme) DesertDark else DesertLight
      AppTheme.ROYAL_VELVET -> if (darkTheme) VelvetDark else VelvetLight
      AppTheme.CHARCOAL_MINIMALIST -> if (darkTheme) CyberDark else CyberLight
      AppTheme.MIDNIGHT_KAABA -> if (darkTheme) KaabaDark else KaabaLight
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

