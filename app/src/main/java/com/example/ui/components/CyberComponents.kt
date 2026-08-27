package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AcidLime
import com.example.ui.theme.CyberRed
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.EmberPeachSubtle
import com.example.ui.theme.GhostSilver
import com.example.ui.theme.GhostSilverMuted
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.SteelGrey
import com.example.ui.theme.VoidBlack
import com.example.ui.theme.cyphrColors
import com.example.ui.theme.getCategoryColor
import com.example.ui.viewmodel.CategorySpend
import java.util.Date
import java.util.Locale

/**
 * Geometric Grid Background Container supporting Charcoal & Daylight Field Mode
 */
@Composable
fun GridBackgroundBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.cyphrColors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.warmCanvas)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stepPx = 32.dp.toPx()
            val gridColor = colors.gridLineColor

            var x = 0f
            while (x <= size.width) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += stepPx
            }

            var y = 0f
            while (y <= size.height) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += stepPx
            }
        }
        content()
    }
}

/**
 * Top brand header with "B U D D Y", screen title, optional subtitle, and Field Mode switcher
 */
@Composable
fun LedgrHeader(
    title: String,
    subtitle: String? = null,
    onToggleFieldMode: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    isFieldMode: Boolean = false,
    isAiOnline: Boolean = false,
    modifier: Modifier = Modifier,
    brandLabel: String = "M A T R I C S"
) {
    val colors = MaterialTheme.cyphrColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_matrics_logo),
                    contentDescription = "Matrics Logo",
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = brandLabel,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 4.sp,
                    color = colors.brandTagColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontFamily = FontFamily.SansSerif,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                color = colors.ghostSilver
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.ghostSilverMuted
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onOpenSettings != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isAiOnline) colors.emberPeachSubtle else colors.warmSurfaceElevated)
                        .border(
                            BorderStroke(1.dp, if (isAiOnline) colors.emberOrange else colors.warmBorder),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { onOpenSettings() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("btn_open_settings_ai"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Settings",
                            tint = if (isAiOnline) colors.emberOrange else colors.steelGrey,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAiOnline) "AI: ON" else "AI API",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAiOnline) colors.emberOrange else colors.ghostSilverMuted
                        )
                    }
                }
            }

            if (onToggleFieldMode != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isFieldMode) colors.emberPeachSubtle else colors.warmSurfaceElevated)
                        .border(
                            BorderStroke(1.dp, if (isFieldMode) colors.emberOrange else colors.warmBorder),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { onToggleFieldMode() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("toggle_field_mode_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isFieldMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Theme Mode",
                            tint = colors.emberOrange,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isFieldMode) "FIELD" else "CHARCOAL",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFieldMode) colors.emberOrange else colors.ghostSilverMuted
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pill-shaped Segmented Period Filter (e.g. [ THIS WEEK ] vs [ THIS MONTH ])
 */
@Composable
fun PeriodPillFilter(
    options: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.cyphrColors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = selectedIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isSelected) colors.emberPeachSubtle else Color.Transparent)
                    .border(
                        BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) colors.emberOrange else colors.warmBorder
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable { onSelectIndex(index) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.uppercase(Locale.ROOT),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 1.2.sp,
                    color = if (isSelected) colors.emberOrange else colors.ghostSilverMuted
                )
            }
        }
    }
}

/**
 * Adaptive Rounded Card Container
 */
@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    backgroundColor: Color? = null,
    cornerCut: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.cyphrColors
    val finalBorder = borderColor ?: colors.warmBorder
    val finalBg = backgroundColor ?: colors.warmCard

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(cornerCut))
            .border(
                BorderStroke(1.dp, finalBorder),
                RoundedCornerShape(cornerCut)
            ),
        color = finalBg,
        shape = RoundedCornerShape(cornerCut)
    ) {
        content()
    }
}

/**
 * Budget Category Item with Progress Bar Track, Overspend Warning Badge & Alerts
 */
@Composable
fun BudgetProgressRow(
    category: String,
    spent: Double,
    limit: Double,
    onEditBudget: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.cyphrColors
    val progress = if (limit > 0) (spent / limit).coerceIn(0.0, 1.0).toFloat() else 0f
    val isOver = spent > limit && limit > 0
    val isWarning = !isOver && limit > 0 && (spent / limit) >= 0.80
    val percentInt = if (limit > 0) ((spent / limit) * 100).toInt() else 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = category,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.ghostSilver
                )
                if (isOver) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberRed.copy(alpha = 0.15f))
                            .border(BorderStroke(0.6.dp, CyberRed), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "EXCEEDED",
                            color = CyberRed,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                } else if (isWarning) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmberOrange.copy(alpha = 0.15f))
                            .border(BorderStroke(0.6.dp, EmberOrange), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "80%+ USED",
                            color = EmberOrange,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "₹${spent.toInt()} / ${limit.toInt()}",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 13.sp,
                    fontWeight = if (isOver || isWarning) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isOver) colors.cyberRed else if (isWarning) colors.emberOrange else colors.ghostSilverMuted
                )
                if (onEditBudget != null) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Category Budget",
                        tint = colors.steelGrey,
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .clickable { onEditBudget() }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        // Progress Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.warmTrackBackground)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = if (progress > 0f) progress else 0.005f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (isOver) colors.cyberRed 
                        else if (isWarning) colors.emberOrange 
                        else colors.emberOrange.copy(alpha = 0.85f)
                    )
            )
        }

        // Subtext if overspent or near limit
        if (isOver) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "⚠️ Exceeded limit by ₹${(spent - limit).toInt()} ($percentInt% of budget)",
                fontFamily = FontFamily.SansSerif,
                fontSize = 10.5.sp,
                color = CyberRed
            )
        } else if (isWarning) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "⚡ Approaching limit: ₹${(limit - spent).toInt()} remaining ($percentInt% used)",
                fontFamily = FontFamily.SansSerif,
                fontSize = 10.5.sp,
                color = EmberOrange
            )
        }
    }
}

/**
 * 2-Column Action Cards (e.g. "Add by voice", "Order this list")
 */
@Composable
fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.cyphrColors
    CyberCard(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        borderColor = colors.warmBorder,
        backgroundColor = colors.warmCard,
        cornerCut = 18.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = colors.emberOrange,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.ghostSilver
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = FontFamily.SansSerif,
                fontSize = 11.sp,
                color = colors.ghostSilverMuted
            )
        }
    }
}

@Composable
fun CyberBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = EmberOrange,
    backgroundColor: Color = color.copy(alpha = 0.12f),
    fontSize: androidx.compose.ui.unit.TextUnit = 10.sp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(BorderStroke(0.6.dp, color.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text.uppercase(Locale.ROOT),
            color = color,
            fontSize = fontSize,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

/**
 * Interactive High-Contrast Donut / Radial Category Breakdown Chart
 * Displays multi-colored distinct category slices, central spend summary, and tap-to-filter legend chips.
 */
@Composable
fun CategoryBreakdownDonutChart(
    categorySpends: List<CategorySpend>,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.cyphrColors
    if (categorySpends.isEmpty()) return

    val totalSpend = remember(categorySpends) {
        categorySpends.sumOf { it.amount }.coerceAtLeast(1.0)
    }

    val selectedSpend = remember(categorySpends, selectedCategory) {
        categorySpends.find { it.category == selectedCategory }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Donut Canvas with Central Stats
        Box(
            modifier = Modifier
                .size(190.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        // Toggle or clear on background click
                        if (selectedCategory != null) onSelectCategory(null)
                    }
            ) {
                val strokeWidth = 26.dp.toPx()
                val activeStrokeWidth = 32.dp.toPx()
                val diameter = size.minDimension - activeStrokeWidth
                val radius = diameter / 2f
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius)
                val arcSize = Size(diameter, diameter)

                var startAngle = -90f
                val gapDegrees = if (categorySpends.size > 1) 3.5f else 0f
                val availableDegrees = 360f - (gapDegrees * categorySpends.size)

                categorySpends.forEach { item ->
                    val sliceFraction = (item.amount / totalSpend).toFloat()
                    val sweepAngle = (sliceFraction * availableDegrees).coerceAtLeast(1.5f)
                    val isSelected = selectedCategory == null || selectedCategory == item.category
                    val isDirectMatch = selectedCategory == item.category

                    val catColor = getCategoryColor(item.category)
                    val drawColor = if (isSelected) catColor else catColor.copy(alpha = 0.28f)
                    val currentStroke = if (isDirectMatch) activeStrokeWidth else strokeWidth

                    drawArc(
                        color = drawColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(
                            width = currentStroke,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )

                    // Draw distinct inner highlight bead on active slice
                    if (isDirectMatch) {
                        drawArc(
                            color = Color.White.copy(alpha = 0.85f),
                            startAngle = startAngle + (sweepAngle / 2f) - 1.5f,
                            sweepAngle = 3f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = currentStroke + 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )
                    }

                    startAngle += sweepAngle + gapDegrees
                }
            }

            // Central stats display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                if (selectedSpend != null) {
                    val catColor = getCategoryColor(selectedSpend.category)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(catColor.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = selectedSpend.category.uppercase(Locale.ROOT),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = catColor,
                            letterSpacing = 0.6.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "₹${selectedSpend.amount.toInt()}",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.ghostSilver
                    )
                    Text(
                        text = "${((selectedSpend.amount / totalSpend) * 100).toInt()}% • tap to reset",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 10.sp,
                        color = colors.ghostSilverMuted
                    )
                } else {
                    Text(
                        text = "BREAKDOWN",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = colors.ghostSilverMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "₹${totalSpend.toInt()}",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        color = colors.emberOrange
                    )
                    Text(
                        text = "${categorySpends.size} categories",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 10.sp,
                        color = colors.steelGrey
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // High-Contrast Interactive Category Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "ALL" pill
            val isAllSelected = selectedCategory == null
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isAllSelected) colors.emberPeachSubtle else colors.warmSurfaceElevated)
                    .border(
                        BorderStroke(1.dp, if (isAllSelected) colors.emberOrange else colors.warmBorderSubtle),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelectCategory(null) }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "ALL",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isAllSelected) colors.emberOrange else colors.ghostSilverMuted
                )
            }

            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categorySpends.size) { index ->
                    val item = categorySpends[index]
                    val isSelected = selectedCategory == item.category
                    val catColor = getCategoryColor(item.category)
                    val percent = ((item.amount / totalSpend) * 100).toInt()

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) catColor.copy(alpha = 0.22f) else colors.warmSurfaceElevated)
                            .border(
                                BorderStroke(1.dp, if (isSelected) catColor else colors.warmBorderSubtle),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (isSelected) onSelectCategory(null) else onSelectCategory(item.category)
                            }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(catColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = item.category,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colors.ghostSilver else colors.ghostSilverMuted
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$percent%",
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = catColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Commodity Price Sparkline Chart
 */
@Composable
fun CommodityPriceChart(
    pricePoints: List<Pair<Date, Double>>,
    modifier: Modifier = Modifier,
    lineColor: Color = EmberOrange
) {
    val colors = MaterialTheme.cyphrColors
    if (pricePoints.size < 2) return

    val minPrice = pricePoints.minOf { it.second }
    val maxPrice = pricePoints.maxOf { it.second }
    val priceRange = if (maxPrice > minPrice) maxPrice - minPrice else 1.0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.warmSurfaceElevated)
            .border(BorderStroke(0.6.dp, colors.warmBorderSubtle), RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val stepX = width / (pricePoints.size - 1)

            val path = Path()
            val fillPath = Path()

            pricePoints.forEachIndexed { index, point ->
                val x = index * stepX
                val normalizedY = ((point.second - minPrice) / priceRange).toFloat()
                val y = height - (normalizedY * (height - 18f)) - 8f

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }

                if (index == pricePoints.size - 1) {
                    fillPath.lineTo(x, height)
                    fillPath.close()
                }

                drawCircle(
                    color = lineColor,
                    radius = 3.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.20f), Color.Transparent)
                )
            )

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

/**
 * Neon/Ember Waveform Visualizer for Voice HUD
 * Supports dynamic state colors: Acid Lime (User speaking), Neon Cyan (TTS speaking), Steel Grey (Idle/Processing)
 */
@Composable
fun NeonWaveform(
    waveLevel: Float,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color? = null
) {
    val colors = MaterialTheme.cyphrColors
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val targetBarColor = activeColor ?: (if (isActive) colors.emberOrange else colors.steelGrey)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.warmSurfaceElevated)
            .border(BorderStroke(0.6.dp, colors.warmBorder), RoundedCornerShape(12.dp))
            .padding(vertical = 6.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val barsCount = 30
        val barWidth = 3.dp.toPx()
        val spacing = (width - (barsCount * barWidth)) / (barsCount - 1)

        for (i in 0 until barsCount) {
            val x = i * (barWidth + spacing)
            val factor = if (isActive) {
                (kotlin.math.sin(pulsePhase + (i * 0.35f)).toFloat() * 0.45f + 0.55f) * waveLevel
            } else {
                0.15f
            }
            val barHeight = (height * 0.8f * factor).coerceAtLeast(3.dp.toPx())
            val top = centerY - (barHeight / 2f)

            drawRoundRect(
                color = if (isActive) targetBarColor else colors.steelGrey.copy(alpha = 0.5f),
                topLeft = Offset(x, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
    }
}

/**
 * LEDGR 5-Slot Bottom Navigation Dock:
 * [Home] [Compare] ( [Mic FAB] ) [Prices] [List]
 */
@Composable
fun CyberBottomDock(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    onVoiceFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.cyphrColors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = if (colors.isFieldMode) 8.dp else 16.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = Color.Black.copy(alpha = if (colors.isFieldMode) 0.12f else 0.5f)
                )
                .clip(RoundedCornerShape(32.dp))
                .border(
                    BorderStroke(1.dp, colors.warmBorder),
                    shape = RoundedCornerShape(32.dp)
                ),
            color = colors.warmCard,
            shape = RoundedCornerShape(32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 0: Storefront
                DockItem(
                    icon = Icons.Default.Storefront,
                    label = "Store",
                    isSelected = currentTab == 0,
                    testTag = "tab_storefront",
                    onClick = { onTabSelected(0) }
                )

                // Tab 1: POS Hub
                DockItem(
                    icon = Icons.Default.PointOfSale,
                    label = "POS Hub",
                    isSelected = currentTab == 1,
                    testTag = "tab_pos",
                    onClick = { onTabSelected(1) }
                )

                // Center Raised Orange Mic Button
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            spotColor = colors.emberOrange,
                            ambientColor = colors.emberOrange
                        )
                        .clip(CircleShape)
                        .background(colors.emberOrange)
                        .clickable { onVoiceFabClick() }
                        .testTag("voice_hud_fab_trigger"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Record",
                        tint = VoidBlack,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Tab 2: Inventory
                DockItem(
                    icon = Icons.Default.Inventory,
                    label = "Inventory",
                    isSelected = currentTab == 2,
                    testTag = "tab_inventory",
                    onClick = { onTabSelected(2) }
                )

                // Tab 3: Khata / Ledger
                DockItem(
                    icon = Icons.Default.ReceiptLong,
                    label = "Khata",
                    isSelected = currentTab == 3,
                    testTag = "tab_khata",
                    onClick = { onTabSelected(3) }
                )
            }
        }
    }
}

@Composable
private fun DockItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.cyphrColors
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) colors.emberOrange else colors.steelGrey,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontFamily = FontFamily.SansSerif,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) colors.emberOrange else colors.steelGrey
        )
    }
}
