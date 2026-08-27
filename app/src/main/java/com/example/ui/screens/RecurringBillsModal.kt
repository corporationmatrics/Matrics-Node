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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.RecurringBillEntity
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
fun RecurringBillsModal(viewModel: CyphrViewModel) {
    val recurringBills by viewModel.allRecurringBills.collectAsStateWithLifecycle()
    val monthlyCommitment by viewModel.totalMonthlyRecurringCommitment.collectAsStateWithLifecycle()
    val upcomingBills by viewModel.upcomingBillsNext7Days.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { viewModel.closeModal() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VoidBlack.copy(alpha = 0.94f))
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .testTag("recurring_bills_modal")
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
                                .background(EmberOrange.copy(alpha = 0.15f))
                                .border(1.dp, EmberOrange.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventRepeat,
                                contentDescription = "Recurring Bills",
                                tint = EmberOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "RECURRING BILLS & REMINDERS",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = GhostSilver
                            )
                            Text(
                                text = "Auto-Debits, Mandates & Subscriptions",
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
                            .testTag("close_recurring_bills_modal")
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
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "MONTHLY COMMITMENT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GhostSilverMuted,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "₹${monthlyCommitment.toInt()}",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmberOrange
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                CyberBadge(
                                    text = "${recurringBills.count { it.status == "ACTIVE" }} ACTIVE",
                                    color = AcidLime,
                                    backgroundColor = AcidLime.copy(alpha = 0.12f)
                                )
                                if (upcomingBills.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    CyberBadge(
                                        text = "${upcomingBills.size} DUE THIS WEEK",
                                        color = CyberRed,
                                        backgroundColor = CyberRed.copy(alpha = 0.15f)
                                    )
                                }
                            }
                        }
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
                        text = "SCHEDULED MANDATES (${recurringBills.size})",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GhostSilverMuted
                    )

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmberOrange,
                            contentColor = VoidBlack
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp).testTag("add_recurring_bill_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "NEW BILL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // List of Bills
                if (recurringBills.isEmpty()) {
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
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = GhostSilverMuted,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No recurring bills set yet",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GhostSilver
                            )
                            Text(
                                text = "Add Wi-Fi, Netflix, Rent, electricity, or gym subscriptions to stay ahead of auto-debits.",
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
                        items(recurringBills, key = { it.id }) { bill ->
                            RecurringBillCardItem(
                                bill = bill,
                                onPay = {
                                    viewModel.payRecurringBill(bill.id, bill.title, bill.amount)
                                },
                                onDelete = {
                                    viewModel.deleteRecurringBill(bill.id, bill.title)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRecurringBillDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, amount, category, cycle, dueDay, method, autoDebit, reminderDays, icon, notes ->
                viewModel.addRecurringBill(
                    title = title,
                    amount = amount,
                    category = category,
                    billingCycle = cycle,
                    dueDay = dueDay,
                    paymentMethod = method,
                    isAutoDebit = autoDebit,
                    reminderDaysBefore = reminderDays,
                    serviceIcon = icon,
                    notes = notes
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun RecurringBillCardItem(
    bill: RecurringBillEntity,
    onPay: () -> Unit,
    onDelete: () -> Unit
) {
    val now = System.currentTimeMillis()
    val diffDays = ((bill.nextDueDate - now) / (1000 * 60 * 60 * 24)).toInt()
    val dueText = when {
        diffDays < 0 -> "Overdue by ${-diffDays}d"
        diffDays == 0 -> "Due Today!"
        diffDays == 1 -> "Due Tomorrow"
        else -> "Due in $diffDays days"
    }

    val dueColor = when {
        diffDays <= 1 -> CyberRed
        diffDays <= 5 -> EmberOrange
        else -> AcidLime
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    val nextDueDateFormatted = remember(bill.nextDueDate) { dateFormat.format(Date(bill.nextDueDate)) }

    CyberCard(
        modifier = Modifier.fillMaxWidth().testTag("bill_item_${bill.id}"),
        borderColor = if (diffDays <= 3) dueColor.copy(alpha = 0.5f) else WarmBorderSubtle,
        backgroundColor = WarmSurfaceElevated,
        cornerCut = 12.dp
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmberOrange.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = bill.serviceIcon.take(3).uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmberOrange
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = bill.title,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GhostSilver
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${bill.category} • ${bill.billingCycle}",
                                fontSize = 11.sp,
                                color = GhostSilverMuted
                            )
                            if (bill.isAutoDebit) {
                                Spacer(modifier = Modifier.width(6.dp))
                                CyberBadge(
                                    text = "Auto-Debit",
                                    color = AcidLime,
                                    backgroundColor = AcidLime.copy(alpha = 0.15f)
                                )
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${bill.amount.toInt()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmberOrange
                    )
                    Text(
                        text = "/${bill.billingCycle.lowercase().take(2)}",
                        fontSize = 10.sp,
                        color = GhostSilverMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Due info & Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = dueColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$dueText ($nextDueDateFormatted)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = dueColor
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = GhostSilverMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = onPay,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AcidLime.copy(alpha = 0.18f),
                            contentColor = AcidLime
                        ),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, AcidLime.copy(alpha = 0.4f)),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PAY & LOG",
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
private fun AddRecurringBillDialog(
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Double,
        category: String,
        cycle: String,
        dueDay: Int,
        method: String,
        autoDebit: Boolean,
        reminderDays: Int,
        icon: String,
        notes: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Subscriptions") }
    var billingCycle by remember { mutableStateOf("MONTHLY") }
    var dueDay by remember { mutableIntStateOf(5) }
    var paymentMethod by remember { mutableStateOf("UPI AutoPay") }
    var isAutoDebit by remember { mutableStateOf(true) }
    var reminderDays by remember { mutableIntStateOf(3) }
    var serviceIcon by remember { mutableStateOf("DEFAULT") }
    var notes by remember { mutableStateOf("") }

    val presetServices = remember {
        listOf(
            Triple("Netflix 4K", 649.0, "NETFLIX"),
            Triple("Spotify Family", 179.0, "SPOTIFY"),
            Triple("Airtel Fiber", 999.0, "AIRTEL"),
            Triple("Bescom Power", 1800.0, "ELECTRICITY"),
            Triple("House Rent", 22000.0, "RENT"),
            Triple("Cult.fit Gym", 1299.0, "GYM"),
            Triple("Google One 2TB", 650.0, "GOOGLE_ONE"),
            Triple("Amazon Prime", 299.0, "AMAZON")
        )
    }

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
                        text = "ADD RECURRING BILL / MANDATE",
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
                    text = "QUICK POPULATE PRESETS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = GhostSilverMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetServices.forEach { (name, price, icon) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(WarmSurfaceElevated)
                                .border(1.dp, WarmBorderSubtle, RoundedCornerShape(6.dp))
                                .clickable {
                                    title = name
                                    amountText = price.toInt().toString()
                                    serviceIcon = icon
                                    if (name.contains("Rent")) category = "Housing"
                                    else if (name.contains("Power") || name.contains("Fiber")) category = "Utilities"
                                    else if (name.contains("Gym")) category = "Fitness"
                                    else category = "Subscriptions"
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$name (₹${price.toInt()})",
                                fontSize = 10.sp,
                                color = EmberOrange,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Service / Bill Title (e.g. Netflix, Wi-Fi)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = GhostSilver,
                        unfocusedTextColor = GhostSilver,
                        focusedBorderColor = EmberOrange,
                        unfocusedBorderColor = WarmBorderSubtle
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Amount (₹)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = GhostSilver,
                            unfocusedTextColor = GhostSilver,
                            focusedBorderColor = EmberOrange,
                            unfocusedBorderColor = WarmBorderSubtle
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = dueDay.toString(),
                        onValueChange = { dueDay = it.toIntOrNull()?.coerceIn(1, 31) ?: 1 },
                        label = { Text("Due Day (1-31)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = GhostSilver,
                            unfocusedTextColor = GhostSilver,
                            focusedBorderColor = EmberOrange,
                            unfocusedBorderColor = WarmBorderSubtle
                        ),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Auto-Debit / Mandate Enabled",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GhostSilver
                        )
                        Text(
                            text = "Automatically debited via UPI / Card mandate",
                            fontSize = 10.sp,
                            color = GhostSilverMuted
                        )
                    }

                    Switch(
                        checked = isAutoDebit,
                        onCheckedChange = { isAutoDebit = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VoidBlack,
                            checkedTrackColor = AcidLime,
                            uncheckedThumbColor = GhostSilverMuted,
                            uncheckedTrackColor = WarmSurfaceElevated
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && amount > 0) {
                            onSave(
                                title,
                                amount,
                                category,
                                billingCycle,
                                dueDay,
                                paymentMethod,
                                isAutoDebit,
                                reminderDays,
                                serviceIcon,
                                notes
                            )
                        }
                    },
                    enabled = title.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmberOrange,
                        contentColor = VoidBlack
                    ),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "SAVE RECURRING BILL",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
