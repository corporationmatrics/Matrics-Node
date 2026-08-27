package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * LEDGR / MATRICS Design Tokens supporting Charcoal Dark & Daylight Field Mode
 */
@Immutable
data class CyphrCustomColors(
    val isFieldMode: Boolean = false,
    val voidBlack: Color = VoidBlack,
    val warmCanvas: Color = WarmCanvas,
    val warmSurface: Color = WarmSurface,
    val warmSurfaceElevated: Color = WarmSurfaceElevated,
    val warmCard: Color = WarmCard,
    val warmBorder: Color = WarmBorder,
    val warmBorderSubtle: Color = WarmBorderSubtle,
    val warmTrackBackground: Color = WarmTrackBackground,
    val gridLineColor: Color = WarmGridLine,
    val emberOrange: Color = EmberOrange,
    val emberOrangeGlow: Color = EmberOrangeGlow,
    val emberPeach: Color = EmberPeach,
    val emberPeachSubtle: Color = EmberPeachSubtle,
    val neonCyan: Color = NeonCyan,
    val neonCyanGlow: Color = NeonCyanGlow,
    val acidLime: Color = AcidLime,
    val acidLimeGlow: Color = AcidLimeGlow,
    val cyberRed: Color = CyberRed,
    val cyberRedGlow: Color = CyberRedGlow,
    val neonAmber: Color = NeonAmber,
    val neonPurple: Color = NeonPurple,
    val ghostSilver: Color = GhostSilver,
    val ghostSilverMuted: Color = GhostSilverMuted,
    val steelGrey: Color = SteelGrey,
    val brandTagColor: Color = BrandTagColor,
    val glassCard: Color = GlassCard,
    val glassBorder: Color = GlassBorder,
    val glassElevated: Color = GlassElevated
)

fun getCyphrColors(isFieldMode: Boolean): CyphrCustomColors {
    return if (isFieldMode) {
        CyphrCustomColors(
            isFieldMode = true,
            voidBlack = FieldCanvas,
            warmCanvas = FieldCanvas,
            warmSurface = FieldSurface,
            warmSurfaceElevated = FieldSurfaceElevated,
            warmCard = FieldCard,
            warmBorder = FieldBorder,
            warmBorderSubtle = FieldBorderSubtle,
            warmTrackBackground = FieldTrackBackground,
            gridLineColor = FieldGridLine,
            emberOrange = EmberOrange,
            emberOrangeGlow = EmberOrangeGlow,
            emberPeach = EmberPeach,
            emberPeachSubtle = EmberPeachSubtle,
            neonCyan = NeonCyan,
            neonCyanGlow = NeonCyanGlow,
            acidLime = AcidLime,
            acidLimeGlow = AcidLimeGlow,
            cyberRed = CyberRed,
            cyberRedGlow = CyberRedGlow,
            neonAmber = NeonAmber,
            neonPurple = NeonPurple,
            ghostSilver = FieldTextPrimary,
            ghostSilverMuted = FieldTextSecondary,
            steelGrey = FieldTextMuted,
            brandTagColor = FieldBrandTag,
            glassCard = FieldCard,
            glassBorder = FieldBorder,
            glassElevated = FieldSurfaceElevated
        )
    } else {
        CyphrCustomColors(
            isFieldMode = false,
            voidBlack = VoidBlack,
            warmCanvas = WarmCanvas,
            warmSurface = WarmSurface,
            warmSurfaceElevated = WarmSurfaceElevated,
            warmCard = WarmCard,
            warmBorder = WarmBorder,
            warmBorderSubtle = WarmBorderSubtle,
            warmTrackBackground = WarmTrackBackground,
            gridLineColor = WarmGridLine,
            emberOrange = EmberOrange,
            emberOrangeGlow = EmberOrangeGlow,
            emberPeach = EmberPeach,
            emberPeachSubtle = EmberPeachSubtle,
            neonCyan = NeonCyan,
            neonCyanGlow = NeonCyanGlow,
            acidLime = AcidLime,
            acidLimeGlow = AcidLimeGlow,
            cyberRed = CyberRed,
            cyberRedGlow = CyberRedGlow,
            neonAmber = NeonAmber,
            neonPurple = NeonPurple,
            ghostSilver = GhostSilver,
            ghostSilverMuted = GhostSilverMuted,
            steelGrey = SteelGrey,
            brandTagColor = BrandTagColor,
            glassCard = GlassCard,
            glassBorder = GlassBorder,
            glassElevated = GlassElevated
        )
    }
}

val LocalCyphrColors = staticCompositionLocalOf { CyphrCustomColors() }

/**
 * Material 3 Dark ColorScheme
 */
val CyphrDarkColorScheme: ColorScheme = darkColorScheme(
    primary = EmberOrange,
    onPrimary = VoidBlack,
    primaryContainer = WarmSurfaceElevated,
    onPrimaryContainer = EmberOrange,
    secondary = EmberPeach,
    onSecondary = VoidBlack,
    secondaryContainer = WarmSurfaceElevated,
    onSecondaryContainer = EmberPeach,
    tertiary = AcidLime,
    onTertiary = VoidBlack,
    tertiaryContainer = WarmCard,
    onTertiaryContainer = AcidLime,
    background = VoidBlack,
    onBackground = GhostSilver,
    surface = WarmSurface,
    onSurface = GhostSilver,
    surfaceVariant = WarmCard,
    onSurfaceVariant = GhostSilverMuted,
    surfaceTint = EmberOrange,
    inverseSurface = GhostSilver,
    inverseOnSurface = VoidBlack,
    outline = WarmBorder,
    outlineVariant = SteelGreyDark,
    error = CyberRed,
    onError = GhostSilver,
    errorContainer = CyberRed.copy(alpha = 0.2f),
    onErrorContainer = CyberRed
)

/**
 * Material 3 Field (Daylight Light) ColorScheme
 */
val CyphrFieldColorScheme: ColorScheme = lightColorScheme(
    primary = EmberOrange,
    onPrimary = Color.White,
    primaryContainer = FieldSurfaceElevated,
    onPrimaryContainer = EmberOrangeDark,
    secondary = EmberPeach,
    onSecondary = Color.White,
    secondaryContainer = FieldSurfaceElevated,
    onSecondaryContainer = EmberOrangeDark,
    tertiary = AcidLime,
    onTertiary = Color.White,
    tertiaryContainer = FieldCard,
    onTertiaryContainer = AcidLime,
    background = FieldCanvas,
    onBackground = FieldTextPrimary,
    surface = FieldSurface,
    onSurface = FieldTextPrimary,
    surfaceVariant = FieldCard,
    onSurfaceVariant = FieldTextSecondary,
    surfaceTint = EmberOrange,
    inverseSurface = FieldTextPrimary,
    inverseOnSurface = FieldCanvas,
    outline = FieldBorder,
    outlineVariant = FieldBorderSubtle,
    error = CyberRed,
    onError = Color.White,
    errorContainer = CyberRed.copy(alpha = 0.15f),
    onErrorContainer = CyberRed
)

val CyphrShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Main Theme Composable for MATRICS with adaptive Field Mode
 */
@Composable
fun CyphrTheme(
    isFieldMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val customColors = getCyphrColors(isFieldMode)
    val colorScheme = if (isFieldMode) CyphrFieldColorScheme else CyphrDarkColorScheme

    CompositionLocalProvider(
        LocalCyphrColors provides customColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = CyphrShapes,
            content = content
        )
    }
}

val MaterialTheme.cyphrColors: CyphrCustomColors
    @Composable
    get() = LocalCyphrColors.current

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    CyphrTheme(isFieldMode = false, content = content)
}
