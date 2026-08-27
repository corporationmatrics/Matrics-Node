package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.example.data.model.LineItemEntity
import com.example.data.model.TransactionEntity
import com.example.ui.components.BudgetProgressRow
import com.example.ui.components.CategoryBreakdownDonutChart
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberCard
import com.example.ui.components.GridBackgroundBox
import com.example.ui.components.LedgrHeader
import com.example.ui.components.PeriodPillFilter
import com.example.ui.theme.AcidLime
import com.example.ui.theme.CyberRed
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.EmberPeach
import com.example.ui.theme.GhostSilver
import com.example.ui.theme.GhostSilverMuted
import com.example.ui.theme.ScannerCyan
import com.example.ui.theme.SteelGrey
import com.example.ui.theme.VoidBlack
import com.example.ui.theme.cyphrColors
import com.example.ui.theme.getCategoryColor
import com.example.ui.viewmodel.CyphrViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: CyphrViewModel,
    onOpenVoiceHud: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val isFieldMode by viewModel.isFieldMode.collectAsStateWithLifecycle()
    val userApiKey by viewModel.userGeminiApiKey.collectAsStateWithLifecycle()
    val incomingBankSms by viewModel.incomingBankSmsAlert.collectAsStateWithLifecycle()
    val colors = MaterialTheme.cyphrColors
    var selectedPeriodIndex by remember { mutableIntStateOf(1) } // 0: THIS WEEK, 1: THIS MONTH
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    val currentMonthYear = remember {
        SimpleDateFormat("MMM yyyy", Locale.ROOT).format(Date())
    }

    GridBackgroundBox(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Matrics Brand Header with Field Mode Switcher and Settings/AI Modal
            item {
                LedgrHeader(
                    title = "Overview",
                    subtitle = currentMonthYear,
                    brandLabel = "M A T R I C S",
                    onToggleFieldMode = { viewModel.toggleFieldMode() },
                    onOpenSettings = { viewModel.openModal("SETTINGS") },
                    isFieldMode = isFieldMode,
                    isAiOnline = userApiKey.isNotBlank()
                )
            }

            // Period Segmented Pill Filter: [ THIS WEEK ] [ THIS MONTH ]
            item {
                PeriodPillFilter(
                    options = listOf("This Week", "This Month"),
                    selectedIndex = selectedPeriodIndex,
                    onSelectIndex = { selectedPeriodIndex = it },
                    modifier = Modifier.testTag("period_segmented_filter")
                )
            }

            // Hero Spend Container: GROSS SPEND + Single Clear Ember Orange Accent + "Record by voice"
            item {
                val displaySpend = if (selectedPeriodIndex == 0) {
                    dashboardState.totalSpendCurrentMonth * 0.28
                } else {
                    dashboardState.totalSpendCurrentMonth
                }

                CyberCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_spend_card"),
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
                            text = "GROSS SPEND",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.8.sp,
                            color = colors.ghostSilverMuted
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Large Ember Orange Amount
                        Text(
                            text = "₹${String.format(Locale.ROOT, "%,.0f", displaySpend)}",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            color = colors.emberOrange
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Comparative Trend Tag
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = colors.emberOrange,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "₹${String.format(Locale.ROOT, "%,.0f", displaySpend)} more than last month",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 12.sp,
                                color = colors.ghostSilverMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // "Record by voice" Button embedded inside Hero Card
                        Button(
                            onClick = onOpenVoiceHud,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_hero_record_voice"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.emberOrange,
                                contentColor = VoidBlack
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Mic",
                                    tint = VoidBlack,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Record by voice",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VoidBlack
                                )
                            }
                        }
                    }
                }
            }

            // Live Bank SMS Auto-Capture Alert Banner
            incomingBankSms?.let { sms ->
                item {
                    CyberCard(
                        modifier = Modifier.fillMaxWidth().testTag("banner_incoming_bank_sms"),
                        borderColor = AcidLime,
                        backgroundColor = colors.warmSurfaceElevated,
                        cornerCut = 14.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CyberBadge(
                                        text = "NEW BANK SMS DETECTED",
                                        color = AcidLime,
                                        backgroundColor = colors.warmCard
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = sms.bankName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.ghostSilver
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.dismissIncomingBankSmsAlert() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = colors.ghostSilverMuted, modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sms.merchant,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.ghostSilver
                                    )
                                    Text(
                                        text = "${sms.txnType} • Acct ${sms.accountLast4} • Category: ${sms.category}",
                                        fontSize = 10.sp,
                                        color = colors.ghostSilverMuted
                                    )
                                }

                                Text(
                                    text = "₹${sms.amount.toInt()}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmberOrange
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.logParsedBankSms(sms) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AcidLime,
                                        contentColor = VoidBlack
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .testTag("btn_auto_log_detected_sms")
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VoidBlack, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("AUTO-LOG ₹${sms.amount.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VoidBlack)
                                }

                                Button(
                                    onClick = { viewModel.openAutoImportModal() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.warmTrackBackground,
                                        contentColor = colors.ghostSilver
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("REVIEW", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = colors.ghostSilver)
                                }
                            }
                        }
                    }
                }
            }

            // Quick Ingress Tools: Unified Invoice OCR Scanner, Auto-Import Hub (SMS & Email), Instant Pay
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.warmSurfaceElevated)
                            .border(1.dp, ScannerCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.openHandwrittenScanner() }
                            .padding(vertical = 10.dp)
                            .testTag("btn_kacha_bill_scanner"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = ScannerCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan Invoice", fontSize = 11.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, color = ScannerCyan)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.warmSurfaceElevated)
                            .border(1.dp, EmberOrange.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.openAutoImportModal() }
                            .padding(vertical = 10.dp)
                            .testTag("btn_auto_import_hub"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Message, contentDescription = null, tint = EmberOrange, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("SMS & Mail", fontSize = 11.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, color = colors.ghostSilver)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(0.9f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.warmSurfaceElevated)
                            .border(1.dp, colors.warmBorder, RoundedCornerShape(12.dp))
                            .clickable { viewModel.openModal("INSTANT_PAY") }
                            .padding(vertical = 10.dp)
                            .testTag("btn_instant_pay"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payment, contentDescription = null, tint = colors.ghostSilverMuted, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Pay", fontSize = 11.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, color = colors.ghostSilver)
                        }
                    }
                }
            }

            // Ecosystem Fast Launchers: Recurring Bills, Savings Goals, Khata / Splitter
            item {
                val recurringBills by viewModel.allRecurringBills.collectAsStateWithLifecycle()
                val savingsGoals by viewModel.allSavingsGoals.collectAsStateWithLifecycle()
                val khataEntries by viewModel.allKhataEntries.collectAsStateWithLifecycle()
                val netKhata by viewModel.netKhataBalance.collectAsStateWithLifecycle()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Bills Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.warmSurfaceElevated)
                            .border(1.dp, EmberOrange.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.openRecurringBillsModal() }
                            .padding(vertical = 9.dp)
                            .testTag("btn_recurring_bills"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EventRepeat, contentDescription = null, tint = EmberOrange, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bills (${recurringBills.size})", fontSize = 11.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, color = GhostSilver)
                        }
                    }

                    // Savings Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.warmSurfaceElevated)
                            .border(1.dp, AcidLime.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.openSavingsGoalsModal() }
                            .padding(vertical = 9.dp)
                            .testTag("btn_savings_goals"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Savings, contentDescription = null, tint = AcidLime, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Goals (${savingsGoals.size})", fontSize = 11.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, color = GhostSilver)
                        }
                    }

                    // Khata Pill
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.warmSurfaceElevated)
                            .border(1.dp, colors.warmBorder, RoundedCornerShape(12.dp))
                            .clickable { viewModel.openKhataSplitterModal() }
                            .padding(vertical = 9.dp)
                            .testTag("btn_khata_splitter"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = EmberPeach, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Khata & Split", fontSize = 11.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, color = GhostSilver)
                        }
                    }
                }
            }

            // Budgets Card matching the reference design with category-level limits & overspend warnings
            item {
                val categoryBudgets by viewModel.categoryBudgets.collectAsStateWithLifecycle()
                val overspentCategories = remember(dashboardState.categorySpends, categoryBudgets) {
                    categoryBudgets.mapNotNull { (cat, limit) ->
                        val spent = dashboardState.categorySpends.find { it.category == cat }?.amount ?: 0.0
                        if (spent > limit && limit > 0) Triple(cat, spent, limit) else null
                    }
                }

                CyberCard(
                    modifier = Modifier.fillMaxWidth().testTag("budgets_card"),
                    borderColor = if (overspentCategories.isNotEmpty()) CyberRed.copy(alpha = 0.8f) else colors.warmBorder,
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
                            Column {
                                Text(
                                    text = "Budgets",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.ghostSilver
                                )
                                Text(
                                    text = "Category limits & real-time burn",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 11.sp,
                                    color = colors.ghostSilverMuted
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (overspentCategories.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CyberRed.copy(alpha = 0.15f))
                                            .border(BorderStroke(0.6.dp, CyberRed), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "⚠️ ${overspentCategories.size} OVER LIMIT",
                                            color = CyberRed,
                                            fontSize = 9.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.warmSurfaceElevated)
                                        .border(BorderStroke(0.6.dp, colors.warmBorder), RoundedCornerShape(8.dp))
                                        .clickable { viewModel.openCategoryBudgetModal() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("btn_configure_budgets"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "EDIT LIMITS",
                                        fontSize = 9.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.emberOrange
                                    )
                                }
                            }
                        }

                        if (overspentCategories.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CyberRed.copy(alpha = 0.12f))
                                    .border(BorderStroke(0.8.dp, CyberRed.copy(alpha = 0.5f)), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = "Overspend Alert",
                                        tint = CyberRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Overspend Alert: ${overspentCategories.joinToString { "${it.first} (+₹${(it.second - it.third).toInt()})" }}",
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = CyberRed
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Show dynamic category budgets with overspent priority
                        val categoryStatuses by viewModel.categoryBudgetStatuses.collectAsStateWithLifecycle()
                        val topStatuses = remember(categoryStatuses) {
                            categoryStatuses.take(6)
                        }
                        topStatuses.forEach { status ->
                            BudgetProgressRow(
                                category = status.category,
                                spent = status.spent,
                                limit = status.limit,
                                onEditBudget = { viewModel.openCategoryBudgetModal() }
                            )
                        }

                        if (categoryStatuses.size > 6) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.openCategoryBudgetModal() }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+ VIEW ALL ${categoryStatuses.size} CATEGORIES",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.emberOrange
                                )
                            }
                        }
                    }
                }
            }

            // Financial Ecosystem Command: Recurring Bills & Savings Targets & Khata Strip
            item {
                val recurringBills by viewModel.allRecurringBills.collectAsStateWithLifecycle()
                val monthlyCommitment by viewModel.totalMonthlyRecurringCommitment.collectAsStateWithLifecycle()
                val upcomingBills by viewModel.upcomingBillsNext7Days.collectAsStateWithLifecycle()

                val savingsGoals by viewModel.allSavingsGoals.collectAsStateWithLifecycle()
                val totalSaved by viewModel.totalSavedSoFar.collectAsStateWithLifecycle()
                val totalTarget by viewModel.totalSavingsTarget.collectAsStateWithLifecycle()
                val savingsProgress by viewModel.overallSavingsProgress.collectAsStateWithLifecycle()

                val khataEntries by viewModel.allKhataEntries.collectAsStateWithLifecycle()
                val totalYouWillGet by viewModel.totalYouWillGet.collectAsStateWithLifecycle()
                val totalYouWillPay by viewModel.totalYouWillPay.collectAsStateWithLifecycle()

                CyberCard(
                    modifier = Modifier.fillMaxWidth().testTag("ecosystem_summary_card"),
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
                            Column {
                                Text(
                                    text = "FINANCIAL COMMAND",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.ghostSilver
                                )
                                Text(
                                    text = "Mandates, Targets & Friend Khata",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 11.sp,
                                    color = colors.ghostSilverMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Recurring Mandate Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.warmSurfaceElevated)
                                .clickable { viewModel.openRecurringBillsModal() }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(EmberOrange.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.EventRepeat, contentDescription = null, tint = EmberOrange, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Recurring Bills & Mandates",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GhostSilver
                                    )
                                    val nextBill = upcomingBills.firstOrNull() ?: recurringBills.firstOrNull()
                                    Text(
                                        text = if (nextBill != null) "${nextBill.title} (₹${nextBill.amount.toInt()}) due soon" else "No upcoming bills",
                                        fontSize = 10.sp,
                                        color = GhostSilverMuted
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹${monthlyCommitment.toInt()}/mo",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmberOrange
                                )
                                Text(
                                    text = "${recurringBills.size} active",
                                    fontSize = 10.sp,
                                    color = GhostSilverMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Savings Targets Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.warmSurfaceElevated)
                                .clickable { viewModel.openSavingsGoalsModal() }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AcidLime.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Savings, contentDescription = null, tint = AcidLime, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Savings & Wealth Goals",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GhostSilver
                                    )
                                    val topGoal = savingsGoals.firstOrNull()
                                    Text(
                                        text = if (topGoal != null) "${topGoal.title} (${((topGoal.currentAmount / topGoal.targetAmount) * 100).toInt()}%)" else "Set savings goals",
                                        fontSize = 10.sp,
                                        color = GhostSilverMuted
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹${totalSaved.toInt()} / ₹${totalTarget.toInt()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AcidLime
                                )
                                Text(
                                    text = "${(savingsProgress * 100).toInt()}% saved",
                                    fontSize = 10.sp,
                                    color = AcidLime
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Khata & Split Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.warmSurfaceElevated)
                                .clickable { viewModel.openKhataSplitterModal() }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(EmberPeach.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Group, contentDescription = null, tint = EmberPeach, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Khata Ledger & Bill Split",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GhostSilver
                                    )
                                    Text(
                                        text = if (totalYouWillGet > 0) "Get ₹${totalYouWillGet.toInt()} from friends" else "All friend debts settled",
                                        fontSize = 10.sp,
                                        color = GhostSilverMuted
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (totalYouWillGet >= totalYouWillPay) "+₹${(totalYouWillGet - totalYouWillPay).toInt()}" else "-₹${(totalYouWillPay - totalYouWillGet).toInt()}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalYouWillGet >= totalYouWillPay) AcidLime else CyberRed
                                )
                                Text(
                                    text = "Net balance",
                                    fontSize = 10.sp,
                                    color = GhostSilverMuted
                                )
                            }
                        }
                    }
                }
            }

            // Where it went Card matching reference design with Interactive Radial/Donut chart
            item {
                CyberCard(
                    modifier = Modifier.fillMaxWidth().testTag("where_it_went_card"),
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
                                text = "Where it went",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.ghostSilver
                            )
                            if (selectedCategoryFilter != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.emberPeachSubtle)
                                        .clickable { selectedCategoryFilter = null }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "Clear Filter",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.emberOrange
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (dashboardState.categorySpends.isEmpty()) {
                            Text(
                                text = "No spend recorded for this period yet.",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.sp,
                                color = colors.ghostSilverMuted
                            )
                        } else {
                            CategoryBreakdownDonutChart(
                                categorySpends = dashboardState.categorySpends,
                                selectedCategory = selectedCategoryFilter,
                                onSelectCategory = { selectedCategoryFilter = it }
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = colors.warmBorderSubtle, thickness = 0.6.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            dashboardState.categorySpends.forEach { item ->
                                val catColor = getCategoryColor(item.category)
                                val isSelected = selectedCategoryFilter == null || selectedCategoryFilter == item.category
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedCategoryFilter = if (selectedCategoryFilter == item.category) null else item.category
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(catColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = item.category,
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) colors.ghostSilver else colors.ghostSilverMuted
                                        )
                                    }
                                    Text(
                                        text = "₹${item.amount.toInt()}",
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) colors.ghostSilver else colors.ghostSilverMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Card with dynamic category filter support
            item {
                val filteredTxns = remember(dashboardState.recentTransactions, selectedCategoryFilter) {
                    if (selectedCategoryFilter == null) {
                        dashboardState.recentTransactions
                    } else {
                        dashboardState.recentTransactions.filter { txn ->
                            txn.category.equals(selectedCategoryFilter, ignoreCase = true) ||
                            (dashboardState.lineItemsByTxId[txn.id]?.any { it.category.equals(selectedCategoryFilter, ignoreCase = true) } == true)
                        }
                    }
                }

                CyberCard(
                    modifier = Modifier.fillMaxWidth().testTag("recent_spend_card"),
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
                                text = if (selectedCategoryFilter != null) "Recent ($selectedCategoryFilter)" else "Recent",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.ghostSilver
                            )
                            if (selectedCategoryFilter != null) {
                                Text(
                                    text = "${filteredTxns.size} items",
                                    fontSize = 12.sp,
                                    color = colors.emberOrange,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (filteredTxns.isEmpty()) {
                            Text(
                                text = if (selectedCategoryFilter != null)
                                    "No recent transactions found in category “$selectedCategoryFilter”."
                                else
                                    "Nothing yet — tap the mic and say “rice at 50, amul butter 500 gm at 100 from dmart”.",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = colors.ghostSilverMuted
                            )
                        } else {
                            filteredTxns.take(5).forEach { txn ->
                                TransactionRowItem(
                                    transaction = txn,
                                    lineItems = dashboardState.lineItemsByTxId[txn.id] ?: emptyList(),
                                    onDelete = { viewModel.deleteTransaction(txn.id) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
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

@Composable
private fun TransactionRowItem(
    transaction: TransactionEntity,
    lineItems: List<LineItemEntity>,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val colors = MaterialTheme.cyphrColors
    val catColor = getCategoryColor(transaction.category)

    CyberCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        borderColor = colors.warmBorderSubtle,
        backgroundColor = colors.warmSurfaceElevated,
        cornerCut = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = transaction.vendor,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.ghostSilver
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(catColor.copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = transaction.category,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                color = catColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = SimpleDateFormat("dd MMM, hh:mm a", Locale.ROOT).format(Date(transaction.dateTimestamp)),
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 11.sp,
                        color = colors.steelGrey
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${String.format(Locale.ROOT, "%.2f", transaction.totalAmount)}",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.emberOrange
                    )

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = colors.steelGrey,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded && lineItems.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Divider(color = colors.warmBorder, thickness = 0.6.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    lineItems.forEach { item ->
                        val itemCatColor = getCategoryColor(item.category)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(itemCatColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${item.quantity} ${item.unit} ${item.name}",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 12.sp,
                                    color = colors.ghostSilver
                                )
                            }
                            Text(
                                text = "₹${String.format(Locale.ROOT, "%.2f", item.totalPrice)}",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.ghostSilver
                            )
                        }
                    }
                }
            }
        }
    }
}
