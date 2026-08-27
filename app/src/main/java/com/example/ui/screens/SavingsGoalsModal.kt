package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SavingsGoalEntity
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberCard
import com.example.ui.theme.AcidLime
import com.example.ui.theme.CyberRed
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.EmberPeach
import com.example.ui.theme.GhostSilver
import com.example.ui.theme.GhostSilverMuted
import com.example.ui.theme.SteelGrey
import com.example.ui.theme.VoidBlack
import com.example.ui.theme.WarmBorder
import com.example.ui.theme.WarmBorderSubtle
import com.example.ui.theme.WarmCard
import com.example.ui.theme.WarmSurfaceElevated
import com.example.ui.theme.WarmTrackBackground
import com.example.ui.viewmodel.CyphrViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SavingsGoalsModal(viewModel: CyphrViewModel) {
    val savingsGoals by viewModel.allSavingsGoals.collectAsStateWithLifecycle()
    val totalTarget by viewModel.totalSavingsTarget.collectAsStateWithLifecycle()
    val totalSaved by viewModel.totalSavedSoFar.collectAsStateWithLifecycle()
    val overallProgress by viewModel.overallSavingsProgress.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDepositGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    Dialog(
        onDismissRequest = { viewModel.closeModal() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VoidBlack.copy(alpha = 0.94f))
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .testTag("savings_goals_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(WarmCard)
                    .border(BorderStroke(1.dp, WarmBorder), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AcidLime.copy(alpha = 0.15f))
                                .border(1.dp, AcidLime.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = "Savings Goals",
                                tint = AcidLime,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "SAVINGS TARGETS & GOALS",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = GhostSilver
                            )
                            Text(
                                text = "Milestones, Emergency Funds & Wealth Accumulation",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 11.sp,
                                color = GhostSilverMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.closeModal() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_savings_goals_modal")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = GhostSilverMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Summary Card
                CyberCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = WarmBorderSubtle,
                    backgroundColor = WarmSurfaceElevated,
                    cornerCut = 16.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL SAVED / TARGET",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GhostSilverMuted,
                                    letterSpacing = 1.sp
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "₹${totalSaved.toInt()}",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AcidLime
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "/ ₹${totalTarget.toInt()}",
                                        fontSize = 14.sp,
                                        color = GhostSilverMuted,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${(overallProgress * 100).toInt()}%",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AcidLime
                                )
                                Text(
                                    text = "Portfolio Progress",
                                    fontSize = 10.sp,
                                    color = GhostSilverMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { overallProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = AcidLime,
                            trackColor = WarmTrackBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE TARGETS (${savingsGoals.size})",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GhostSilverMuted
                    )

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AcidLime,
                            contentColor = VoidBlack
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp).testTag("add_savings_goal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "NEW GOAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // List of Goals
                if (savingsGoals.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(WarmSurfaceElevated)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = GhostSilverMuted,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No savings goals created yet",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GhostSilver
                            )
                            Text(
                                text = "Set targets for Emergency Fund, New Laptop, Japan Trip, or Gold investments.",
                                fontSize = 11.sp,
                                color = GhostSilverMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(savingsGoals, key = { it.id }) { goal ->
                            SavingsGoalCardItem(
                                goal = goal,
                                onQuickDeposit = { amount ->
                                    viewModel.depositToSavingsGoal(goal.id, amount, goal.title)
                                },
                                onCustomDeposit = {
                                    selectedDepositGoal = goal
                                },
                                onDelete = {
                                    viewModel.deleteSavingsGoal(goal.id, goal.title)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSavingsGoalDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, targetAmt, initialAmt, category, monthlyTarget, colorHex, months, notes ->
                viewModel.addSavingsGoal(
                    title = title,
                    targetAmount = targetAmt,
                    initialSaved = initialAmt,
                    category = category,
                    monthlyTarget = monthlyTarget,
                    colorHex = colorHex,
                    targetMonths = months,
                    notes = notes
                )
                showAddDialog = false
            }
        )
    }

    selectedDepositGoal?.let { goal ->
        DepositWithdrawDialog(
            goal = goal,
            onDismiss = { selectedDepositGoal = null },
            onDeposit = { amt ->
                viewModel.depositToSavingsGoal(goal.id, amt, goal.title)
                selectedDepositGoal = null
            },
            onWithdraw = { amt ->
                viewModel.withdrawFromSavingsGoal(goal.id, amt, goal.title)
                selectedDepositGoal = null
            }
        )
    }
}

@Composable
private fun SavingsGoalCardItem(
    goal: SavingsGoalEntity,
    onQuickDeposit: (Double) -> Unit,
    onCustomDeposit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (goal.targetAmount > 0) {
        (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
    } else 0f

    val isDone = goal.currentAmount >= goal.targetAmount
    val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)

    val dateFormat = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }
    val targetDateFormatted = remember(goal.targetDate) { dateFormat.format(Date(goal.targetDate)) }

    CyberCard(
        modifier = Modifier.fillMaxWidth().testTag("goal_item_${goal.id}"),
        borderColor = if (isDone) AcidLime.copy(alpha = 0.6f) else WarmBorderSubtle,
        backgroundColor = WarmSurfaceElevated,
        cornerCut = 14.dp
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
                            text = goal.title,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GhostSilver
                        )
                        if (isDone) {
                            Spacer(modifier = Modifier.width(6.dp))
                            CyberBadge(
                                text = "GOAL REACHED! 🎉",
                                color = AcidLime,
                                backgroundColor = AcidLime.copy(alpha = 0.15f)
                            )
                        }
                    }
                    Text(
                        text = "${goal.category} • Target by $targetDateFormatted",
                        fontSize = 11.sp,
                        color = GhostSilverMuted
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = GhostSilverMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "₹${goal.currentAmount.toInt()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDone) AcidLime else EmberOrange
                    )
                    Text(
                        text = " / ₹${goal.targetAmount.toInt()}",
                        fontSize = 12.sp,
                        color = GhostSilverMuted,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }

                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDone) AcidLime else EmberOrange
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isDone) AcidLime else EmberOrange,
                trackColor = WarmTrackBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Deposit buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (!isDone) "₹${remaining.toInt()} left to save" else "Target accomplished!",
                    fontSize = 11.sp,
                    color = if (isDone) AcidLime else GhostSilverMuted
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(WarmTrackBackground)
                            .border(1.dp, WarmBorderSubtle, RoundedCornerShape(6.dp))
                            .clickable { onQuickDeposit(1000.0) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "+₹1k",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AcidLime
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(WarmTrackBackground)
                            .border(1.dp, WarmBorderSubtle, RoundedCornerShape(6.dp))
                            .clickable { onQuickDeposit(5000.0) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "+₹5k",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AcidLime
                        )
                    }

                    Button(
                        onClick = onCustomDeposit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmberOrange.copy(alpha = 0.15f),
                            contentColor = EmberOrange
                        ),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, EmberOrange.copy(alpha = 0.4f)),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text(
                            text = "ADJUST",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddSavingsGoalDialog(
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        targetAmt: Double,
        initialAmt: Double,
        category: String,
        monthlyTarget: Double,
        colorHex: String,
        months: Int,
        notes: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var targetAmountText by remember { mutableStateOf("") }
    var initialSavedText by remember { mutableStateOf("0") }
    var category by remember { mutableStateOf("Emergency") }
    var targetMonths by remember { mutableIntStateOf(6) }
    var notes by remember { mutableStateOf("") }

    val presetCategories = remember {
        listOf(
            Pair("Emergency Fund", "Emergency"),
            Pair("MacBook / Tech", "Gadgets"),
            Pair("Japan / Vacation", "Travel"),
            Pair("Gold / Investment", "Investment"),
            Pair("Bike / Vehicle", "Vehicle"),
            Pair("Home Renovation", "Home")
        )
    }

    val targetAmount = targetAmountText.toDoubleOrNull() ?: 0.0
    val initialAmount = initialSavedText.toDoubleOrNull() ?: 0.0
    val requiredMonthly = if (targetMonths > 0 && targetAmount > initialAmount) {
        (targetAmount - initialAmount) / targetMonths
    } else 0.0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VoidBlack.copy(alpha = 0.9f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(WarmCard)
                    .border(BorderStroke(1.dp, WarmBorder), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NEW SAVINGS GOAL TARGET",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GhostSilver
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = GhostSilverMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "CATEGORY PRESET",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = GhostSilverMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetCategories.forEach { (label, cat) ->
                        val isSelected = category == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) AcidLime.copy(alpha = 0.2f) else WarmSurfaceElevated)
                                .border(1.dp, if (isSelected) AcidLime else WarmBorderSubtle, RoundedCornerShape(6.dp))
                                .clickable {
                                    category = cat
                                    if (title.isBlank()) title = label
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                color = if (isSelected) AcidLime else GhostSilver,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title (e.g. 6M Emergency Buffer)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = GhostSilver,
                        unfocusedTextColor = GhostSilver,
                        focusedBorderColor = AcidLime,
                        unfocusedBorderColor = WarmBorderSubtle
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetAmountText,
                        onValueChange = { targetAmountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Target Amount (₹)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = GhostSilver,
                            unfocusedTextColor = GhostSilver,
                            focusedBorderColor = AcidLime,
                            unfocusedBorderColor = WarmBorderSubtle
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = initialSavedText,
                        onValueChange = { initialSavedText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Current Saved (₹)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = GhostSilver,
                            unfocusedTextColor = GhostSilver,
                            focusedBorderColor = AcidLime,
                            unfocusedBorderColor = WarmBorderSubtle
                        ),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "TARGET TIMELINE: $targetMonths MONTHS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GhostSilverMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(3, 6, 12, 24).forEach { months ->
                        val isSelected = targetMonths == months
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) AcidLime.copy(alpha = 0.2f) else WarmSurfaceElevated)
                                .border(1.dp, if (isSelected) AcidLime else WarmBorderSubtle, RoundedCornerShape(6.dp))
                                .clickable { targetMonths = months }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${months}M",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) AcidLime else GhostSilver
                            )
                        }
                    }
                }

                if (requiredMonthly > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(WarmSurfaceElevated)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "💡 Plan: Save ₹${requiredMonthly.toInt()} / month to hit this goal in $targetMonths months",
                            fontSize = 11.sp,
                            color = AcidLime,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (title.isNotBlank() && targetAmount > 0) {
                            onSave(
                                title,
                                targetAmount,
                                initialAmount,
                                category,
                                requiredMonthly,
                                "#4ADE80",
                                targetMonths,
                                notes
                            )
                        }
                    },
                    enabled = title.isNotBlank() && targetAmount > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AcidLime,
                        contentColor = VoidBlack
                    ),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "CREATE SAVINGS GOAL",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DepositWithdrawDialog(
    goal: SavingsGoalEntity,
    onDismiss: () -> Unit,
    onDeposit: (Double) -> Unit,
    onWithdraw: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VoidBlack.copy(alpha = 0.85f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(WarmCard)
                    .border(BorderStroke(1.dp, WarmBorder), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = "ADJUST SAVINGS: ${goal.title}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GhostSilver
                )
                Text(
                    text = "Current Balance: ₹${goal.currentAmount.toInt()} / ₹${goal.targetAmount.toInt()}",
                    fontSize = 11.sp,
                    color = GhostSilverMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = GhostSilver,
                        unfocusedTextColor = GhostSilver,
                        focusedBorderColor = AcidLime,
                        unfocusedBorderColor = WarmBorderSubtle
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) onWithdraw(amt)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberRed.copy(alpha = 0.15f),
                            contentColor = CyberRed
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text("WITHDRAW", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) onDeposit(amt)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AcidLime,
                            contentColor = VoidBlack
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text("DEPOSIT", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
