package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CommodityPriceChart
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberCard
import com.example.ui.components.GridBackgroundBox
import com.example.ui.components.LedgrHeader
import com.example.ui.components.PeriodPillFilter
import com.example.ui.theme.VoidBlack
import com.example.ui.theme.cyphrColors
import com.example.ui.viewmodel.CyphrViewModel
import com.example.ui.viewmodel.PeriodMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(
    viewModel: CyphrViewModel,
    modifier: Modifier = Modifier
) {
    val comparisonData by viewModel.periodComparisonData.collectAsStateWithLifecycle()
    val commoditySummaries by viewModel.commoditySummaries.collectAsStateWithLifecycle()
    val isFieldMode by viewModel.isFieldMode.collectAsStateWithLifecycle()
    val userApiKey by viewModel.userGeminiApiKey.collectAsStateWithLifecycle()
    val aiInsights by viewModel.aiFinancialInsights.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val colors = MaterialTheme.cyphrColors
    var comparePeriodToggle by remember { mutableIntStateOf(1) } // 0: WEEKLY, 1: MONTHLY

    val currentPeriodLabel = remember {
        SimpleDateFormat("MMM yyyy", Locale.ROOT).format(Date()).uppercase(Locale.ROOT)
    }
    val priorPeriodLabel = remember {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.MONTH, -1)
        SimpleDateFormat("MMM yyyy", Locale.ROOT).format(cal.time).uppercase(Locale.ROOT)
    }

    GridBackgroundBox(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Matrics Header with Field Mode Switcher and Settings/AI trigger
            item {
                LedgrHeader(
                    title = "Compare",
                    subtitle = "Two periods, side by side.",
                    brandLabel = "M A T R I C S",
                    onToggleFieldMode = { viewModel.toggleFieldMode() },
                    onOpenSettings = { viewModel.openModal("SETTINGS") },
                    isFieldMode = isFieldMode,
                    isAiOnline = userApiKey.isNotBlank()
                )
            }

            // Period Segmented Filter: [ WEEKLY ] [ MONTHLY ]
            item {
                PeriodPillFilter(
                    options = listOf("Weekly", "Monthly"),
                    selectedIndex = comparePeriodToggle,
                    onSelectIndex = {
                        comparePeriodToggle = it
                        viewModel.setPeriodMode(if (it == 0) PeriodMode.W_O_W else PeriodMode.M_O_M)
                    },
                    modifier = Modifier.testTag("analytics_period_toggle")
                )
            }

            // Gemini AI Spend Variance Insights Card
            item {
                CyberCard(
                    modifier = Modifier.fillMaxWidth().testTag("ai_spend_advisor_card"),
                    borderColor = if (userApiKey.isNotBlank()) colors.emberOrange else colors.warmBorderSubtle,
                    backgroundColor = colors.warmCard,
                    cornerCut = 20.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Advisor",
                                    tint = colors.emberOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "GEMINI SPEND VARIANCE ADVISOR",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.emberOrange
                                )
                            }

                            if (userApiKey.isNotBlank()) {
                                CyberBadge(
                                    text = "GEMINI 3.5",
                                    color = colors.emberOrange,
                                    backgroundColor = colors.warmSurfaceElevated
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (aiInsights != null) {
                            Text(
                                text = aiInsights ?: "",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 12.sp,
                                color = colors.ghostSilver,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        } else {
                            Text(
                                text = if (userApiKey.isNotBlank())
                                    "Tap below to run Gemini deep analysis on recent price spikes and category variances."
                                else
                                    "Paste your Gemini API key in Settings to unlock deep variance diagnosis & budget forecasting.",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 11.sp,
                                color = colors.ghostSilverMuted
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Button(
                            onClick = {
                                if (userApiKey.isBlank()) {
                                    viewModel.openModal("SETTINGS")
                                } else {
                                    viewModel.requestAiFinancialInsights()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (userApiKey.isNotBlank()) colors.emberOrange else colors.warmSurfaceElevated,
                                contentColor = if (userApiKey.isNotBlank()) VoidBlack else colors.ghostSilver
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp).testTag("btn_run_ai_advisor")
                        ) {
                            if (isAiLoading) {
                                CircularProgressIndicator(
                                    color = VoidBlack,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (userApiKey.isBlank()) "CONFIG GEMINI API KEY" else "ANALYZE WITH GEMINI 3.5",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Dual Period Side-by-Side Comparison Card
            item {
                val currentSpend = comparisonData.primaryTotal
                val priorSpend = comparisonData.baselineTotal
                val deltaSpend = currentSpend - priorSpend

                CyberCard(
                    modifier = Modifier.fillMaxWidth().testTag("compare_dual_period_card"),
                    borderColor = colors.warmBorder,
                    backgroundColor = colors.warmCard,
                    cornerCut = 24.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Current Period Box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colors.warmSurfaceElevated)
                                    .border(1.dp, colors.warmBorderSubtle, RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = "Prev",
                                            tint = colors.steelGrey,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = currentPeriodLabel,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.ghostSilverMuted
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Next",
                                            tint = colors.steelGrey,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "₹${currentSpend.toInt()}",
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.emberOrange
                                    )
                                }
                            }

                            // Prior Period Box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colors.warmSurfaceElevated)
                                    .border(1.dp, colors.warmBorderSubtle, RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = "Prev",
                                            tint = colors.steelGrey,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = priorPeriodLabel,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.ghostSilverMuted
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Next",
                                            tint = colors.steelGrey,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "₹${priorSpend.toInt()}",
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.ghostSilver
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Subtitle Trend Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.warmSurfaceElevated)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = colors.emberOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "₹${deltaSpend.toInt().coerceAtLeast(0)} more than the compared period",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.ghostSilverMuted
                                )
                            }
                        }
                    }
                }
            }

            // "Last 6 months" Historical Timeline Card
            item {
                CyberCard(
                    modifier = Modifier.fillMaxWidth().testTag("last_6_months_card"),
                    borderColor = colors.warmBorder,
                    backgroundColor = colors.warmCard,
                    cornerCut = 24.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Last 6 months",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.ghostSilver
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        val sixMonthsSpend = listOf(14200.0, 18500.0, 16800.0, 22400.0, 19800.0, 24500.0)
                        val maxMonthSpend = sixMonthsSpend.maxOrNull() ?: 1.0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val months = listOf("Mar", "Apr", "May", "Jun", "Jul", "Aug")
                            sixMonthsSpend.forEachIndexed { idx, spend ->
                                val fraction = (spend / maxMonthSpend).coerceIn(0.1, 1.0).toFloat()
                                val isCurrent = idx == sixMonthsSpend.lastIndex

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(18.dp)
                                            .fillMaxHeight(fraction)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(if (isCurrent) colors.emberOrange else colors.warmTrackBackground)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = months[idx],
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        color = if (isCurrent) colors.ghostSilver else colors.steelGrey
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Apr 2026", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = colors.steelGrey)
                            Text("Jun 2026", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = colors.steelGrey)
                            Text("Aug 2026", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = colors.emberOrange)
                        }
                    }
                }
            }

            // Commodity Price Volatility & Indexation section
            item {
                CyberCard(
                    modifier = Modifier.fillMaxWidth().testTag("commodity_tracker_card"),
                    borderColor = colors.warmBorder,
                    backgroundColor = colors.warmCard,
                    cornerCut = 24.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Item Price Volatility",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.ghostSilver
                            )
                            CyberBadge(
                                text = "30-DAY INDEX",
                                color = colors.emberOrange
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val commodities = listOf("Milk (Whole 1L)", "Butter (500g)", "Atta (5kg)", "Olive Oil (500ml)")
                        var selectedCommodity by remember { mutableIntStateOf(0) }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(commodities.size) { idx ->
                                val isSel = selectedCommodity == idx
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) colors.emberOrange else colors.warmSurfaceElevated)
                                        .border(0.6.dp, if (isSel) colors.emberOrange else colors.warmBorderSubtle, RoundedCornerShape(8.dp))
                                        .clickable { selectedCommodity = idx }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = commodities[idx],
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) VoidBlack else colors.ghostSilverMuted
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val sampleHistory = listOf(
                            Pair(Date(System.currentTimeMillis() - 25 * 86400000L), 68.0),
                            Pair(Date(System.currentTimeMillis() - 20 * 86400000L), 70.0),
                            Pair(Date(System.currentTimeMillis() - 15 * 86400000L), 69.0),
                            Pair(Date(System.currentTimeMillis() - 10 * 86400000L), 72.0),
                            Pair(Date(System.currentTimeMillis() - 5 * 86400000L), 74.0),
                            Pair(Date(), 75.0)
                        )

                        CommodityPriceChart(
                            pricePoints = sampleHistory,
                            lineColor = colors.emberOrange
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Current avg: ₹75.00", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = colors.ghostSilver)
                            Text("+10.2% vs last month", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = colors.emberOrange)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
