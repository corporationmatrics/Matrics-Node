package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// LEDGR / CYPHR DESIGN TOKENS & COLOR PALETTES
// High-Contrast, Accessible & Eye-Comfort
// ==========================================

// --- DARK CHARCOAL (DEFAULT LEDGR THEME) ---
val VoidBlack = Color(0xFF0F0E0D)
val WarmCanvas = Color(0xFF131210)
val WarmSurface = Color(0xFF1B1917)
val WarmSurfaceElevated = Color(0xFF262320)
val WarmCard = Color(0xFF1E1B18)
val WarmCardElevated = Color(0xFF282521)
val WarmBorder = Color(0xFF3E3832) // Crisp, distinct card & section boundary (~3.5:1 on dark)
val WarmBorderSubtle = Color(0xFF302B26)
val WarmBorderHighlight = Color(0xFFFF7A29)
val WarmTrackBackground = Color(0xFF2C2824)
val WarmGridLine = Color(0x22FFFFFF)

// --- DAYLIGHT "FIELD" MODE (OUTDOORS & IN-STORE HIGH CONTRAST) ---
val FieldCanvas = Color(0xFFF5F4F0)
val FieldSurface = Color(0xFFFFFFFF)
val FieldSurfaceElevated = Color(0xFFEBE8E1)
val FieldCard = Color(0xFFFFFFFF)
val FieldCardElevated = Color(0xFFF8F7F4)
val FieldBorder = Color(0xFFD3CCC0)
val FieldBorderSubtle = Color(0xFFE4DFD6)
val FieldBorderHighlight = Color(0xFFFF7A29)
val FieldTrackBackground = Color(0xFFE4DFD6)
val FieldGridLine = Color(0x15000000)

val FieldTextPrimary = Color(0xFF111827) // Deep high-contrast black (16:1)
val FieldTextSecondary = Color(0xFF374151) // Slate gray (9:1)
val FieldTextMuted = Color(0xFF6B7280) // Readable tertiary text (5:1)
val FieldBrandTag = Color(0xFF4B5563)

// --- SIGNATURE PRIMARY ACCENT: WARM EMBER ORANGE ---
val EmberOrange = Color(0xFFFF7A29)
val EmberOrangeMuted = Color(0xFFE8590C)
val EmberOrangeGlow = Color(0x33FF7A29)
val EmberPeach = Color(0xFFFF9E66)
val EmberPeachSubtle = Color(0x28FF7A29)
val EmberOrangeDark = Color(0xFFC04707)

// Backward compatibility references
val NeonCyan = Color(0xFF00E5FF)
val NeonCyanGlow = Color(0x3300E5FF)
val NeonCyanMuted = Color(0xFF00B4D8)

// AR & Vision Scanner HUD Accents
val ScannerCyan = Color(0xFF00F0FF) // Neon Cyan AR Bounding Box
val ScannerCyanGlow = Color(0x4400F0FF)
val LaserLime = Color(0xFFCCFF00) // Acid Lime Scanning Laser
val LaserLimeGlow = Color(0x66CCFF00)
val ScribbleRed = Color(0xFFFF3366) // Cyber Red Scribble Underline / Alert

val AcidLime = Color(0xFF22C55E) // High-contrast Emerald/Lime (passes AA contrast)
val AcidLimeGlow = Color(0x3322C55E)
val AcidLimeMuted = Color(0xFF16A34A)

val CyberRed = Color(0xFFEF4444) // Bright accessible alert red
val CyberRedGlow = Color(0x33EF4444)

val NeonAmber = Color(0xFFF59E0B)
val NeonAmberGlow = Color(0x33F59E0B)
val NeonPurple = Color(0xFFA855F7)
val NeonPurpleGlow = Color(0x33A855F7)

val Obsidian = Color(0xFF1E1B18)
val ObsidianElevated = Color(0xFF26221E)
val ObsidianCard = Color(0xFF1E1B18)
val ObsidianBorder = Color(0xFF3E3832)

val GlassBackground = Color(0xF21B1917)
val GlassElevated = Color(0xF8262320)
val GlassCard = Color(0xEB1E1B18)
val GlassBorder = Color(0xFF3E3832)
val GlassBorderSubtle = Color(0xFF302B26)
val GlassBorderActive = Color(0x99FF7A29)

// Dark Mode Typography Tokens (Crisp, High Contrast, Anti-Eye Strain)
val GhostSilver = Color(0xFFFAFAFA) // Pure crisp white (16.5:1 contrast against #131210)
val GhostSilverMuted = Color(0xFFD6D3D1) // High-contrast silver-gray (9.5:1 contrast)
val SteelGrey = Color(0xFFA8A29E) // Readable medium stone gray (6.2:1 contrast for timestamps/subtext)
val SteelGreyDark = Color(0xFF78716C) // Subtle borders/dividers (3.5:1)
val BrandTagColor = Color(0xFFE7E5E4) // Crisp subtitle / letterspaced brand headers

// --- SEMANTIC CATEGORY PALETTE (Vibrant, Distinct & Differentiable) ---
val CategoryGrocery = Color(0xFF10B981) // Emerald Green
val CategoryDairy = Color(0xFF06B6D4) // Sky Cyan
val CategoryDining = Color(0xFFF59E0B) // Amber Orange
val CategoryPantry = Color(0xFFEAB308) // Sun Gold
val CategoryBills = Color(0xFF6366F1) // Indigo Blue
val CategoryTech = Color(0xFFA855F7) // Electric Violet
val CategoryFuel = Color(0xFFFF6B35) // Coral Orange
val CategoryHealth = Color(0xFFEC4899) // Rose Pink
val CategoryEntertainment = Color(0xFF8B5CF6) // Royal Purple
val CategoryHousehold = Color(0xFF14B8A6) // Mint Teal
val CategoryOther = Color(0xFF94A3B8) // Slate Silver

fun getCategoryColor(category: String): Color {
    val normalized = category.lowercase().trim()
    return when {
        normalized.contains("grocer") || normalized.contains("veg") || normalized.contains("fruit") -> CategoryGrocery
        normalized.contains("dair") || normalized.contains("milk") || normalized.contains("butter") || normalized.contains("paneer") || normalized.contains("curd") || normalized.contains("cheese") -> CategoryDairy
        normalized.contains("dine") || normalized.contains("din") || normalized.contains("cafe") || normalized.contains("rest") || normalized.contains("swiggy") || normalized.contains("zomato") || normalized.contains("food") || normalized.contains("snack") -> CategoryDining
        normalized.contains("grain") || normalized.contains("atta") || normalized.contains("rice") || normalized.contains("dal") || normalized.contains("oil") || normalized.contains("pant") -> CategoryPantry
        normalized.contains("bill") || normalized.contains("util") || normalized.contains("power") || normalized.contains("bescom") || normalized.contains("airtel") || normalized.contains("wifi") || normalized.contains("rent") || normalized.contains("recharge") -> CategoryBills
        normalized.contains("tech") || normalized.contains("gadget") || normalized.contains("electr") || normalized.contains("app") -> CategoryTech
        normalized.contains("fuel") || normalized.contains("petrol") || normalized.contains("diesel") || normalized.contains("uber") || normalized.contains("ola") || normalized.contains("travel") || normalized.contains("commute") || normalized.contains("auto") -> CategoryFuel
        normalized.contains("health") || normalized.contains("med") || normalized.contains("pharm") || normalized.contains("care") || normalized.contains("doctor") -> CategoryHealth
        normalized.contains("entertain") || normalized.contains("netflix") || normalized.contains("spotify") || normalized.contains("movie") || normalized.contains("prime") || normalized.contains("subscri") -> CategoryEntertainment
        normalized.contains("house") || normalized.contains("clean") || normalized.contains("home") || normalized.contains("laundry") -> CategoryHousehold
        else -> {
            val hash = kotlin.math.abs(category.hashCode())
            val palette = listOf(CategoryGrocery, CategoryDairy, CategoryDining, CategoryPantry, CategoryBills, CategoryTech, CategoryFuel, CategoryHealth, CategoryEntertainment, CategoryHousehold)
            palette[hash % palette.size]
        }
    }
}

// Gradients
val EmberCardGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF26221E), Color(0xFF1B1917))
)

val EmberGlowGradient = Brush.horizontalGradient(
    colors = listOf(EmberOrange.copy(alpha = 0.25f), EmberPeach.copy(alpha = 0.15f))
)

val FrostedGlassGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF282420), Color(0xFF1B1917))
)

val FrostedGlowGradient = Brush.horizontalGradient(
    colors = listOf(EmberOrange.copy(alpha = 0.25f), EmberPeach.copy(alpha = 0.15f))
)

val CyberGridBrush = Brush.linearGradient(
    colors = listOf(EmberOrange.copy(alpha = 0.15f), EmberPeach.copy(alpha = 0.15f))
)

val HeroCardGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF28231F), Color(0xFF1B1917))
)

val NeonCyanGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF00E5FF), Color(0xFF38BDF8))
)

val AcidLimeGradient = Brush.horizontalGradient(
    colors = listOf(AcidLime, Color(0xFF4ADE80))
)

val CyberAlertGradient = Brush.horizontalGradient(
    colors = listOf(CyberRed, Color(0xFFF87171))
)
