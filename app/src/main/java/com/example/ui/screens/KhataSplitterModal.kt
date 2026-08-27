package com.example.ui.screens

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.UpiPaymentManager
import com.example.data.UpiPaymentParams
import com.example.data.UpiPaymentResult
import com.example.data.model.CustomerLoyaltyTier
import com.example.data.model.CustomerProfileData
import com.example.data.model.KhataEntryEntity
import com.example.data.model.KhataInstallmentPayment
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberCard
import com.example.ui.theme.AcidLime
import com.example.ui.theme.CyberRed
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.EmberPeach
import com.example.ui.theme.GhostSilver
import com.example.ui.theme.GhostSilverMuted
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SteelGrey
import com.example.ui.theme.VoidBlack
import com.example.ui.theme.WarmBorder
import com.example.ui.theme.WarmBorderSubtle
import com.example.ui.theme.WarmCard
import com.example.ui.theme.WarmSurfaceElevated
import com.example.ui.theme.WarmTrackBackground
import com.example.ui.viewmodel.CyphrViewModel
import com.example.ui.viewmodel.WhatsAppReminderState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val WhatsAppGreen = Color(0xFF25D366)
private val GoldTierColor = Color(0xFFFFD700)
private val PlatinumTierColor = Color(0xFFE5E4E2)
private val BronzeTierColor = Color(0xFFCD7F32)

@Composable
fun KhataSplitterModal(viewModel: CyphrViewModel) {
    val khataEntries by viewModel.allKhataEntries.collectAsStateWithLifecycle()
    val customerProfiles by viewModel.allCustomerProfiles.collectAsStateWithLifecycle()
    val totalYouWillGet by viewModel.totalYouWillGet.collectAsStateWithLifecycle()
    val totalYouWillPay by viewModel.totalYouWillPay.collectAsStateWithLifecycle()
    val netBalance by viewModel.netKhataBalance.collectAsStateWithLifecycle()

    val selectedCustomerProfile by viewModel.selectedCustomerProfile.collectAsStateWithLifecycle()
    val selectedKhataForInstallment by viewModel.selectedKhataForInstallment.collectAsStateWithLifecycle()
    val whatsappReminderState by viewModel.whatsappReminderPreview.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Customer Khata, 1 = Customer Profiles, 2 = Bill Splitter
    var showAddKhataDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { viewModel.closeModal() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VoidBlack.copy(alpha = 0.94f))
                .padding(horizontal = 12.dp, vertical = 20.dp)
                .testTag("khata_splitter_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(WarmCard)
                    .border(BorderStroke(1.dp, WarmBorder), RoundedCornerShape(24.dp))
                    .padding(16.dp)
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
                                imageVector = when (selectedTab) {
                                    0 -> Icons.Default.ReceiptLong
                                    1 -> Icons.Default.Person
                                    else -> Icons.Default.Group
                                },
                                contentDescription = "Khata Splitter",
                                tint = EmberOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "CUSTOMER KHATA & CREDIT",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = GhostSilver
                            )
                            Text(
                                text = "Store Credit, WhatsApp Reminders & Profiles",
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
                            .testTag("close_khata_modal")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = GhostSilverMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3-Way Navigation Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(WarmSurfaceElevated)
                        .padding(4.dp)
                ) {
                    listOf("KHATA LEDGER", "CUSTOMER PROFILES", "BILL SPLIT").forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) EmberOrange else WarmSurfaceElevated)
                                .clickable { selectedTab = index }
                                .padding(vertical = 8.dp)
                                .testTag("tab_khata_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) VoidBlack else GhostSilver
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> {
                        // TAB 0: CUSTOMER KHATA & CREDIT LEDGER
                        CustomerKhataLedgerView(
                            entries = khataEntries,
                            youWillGet = totalYouWillGet,
                            youWillPay = totalYouWillPay,
                            net = netBalance,
                            onAddClick = { showAddKhataDialog = true },
                            viewModel = viewModel
                        )
                    }
                    1 -> {
                        // TAB 1: CUSTOMER PROFILES & PURCHASE HISTORY
                        CustomerProfilesListView(
                            profiles = customerProfiles,
                            onSelectProfile = { viewModel.selectCustomerProfile(it) },
                            onSendReminder = { viewModel.openWhatsAppReminderForCustomer(it) },
                            onRedeemPoints = { name, pts -> viewModel.redeemCustomerLoyaltyPoints(name, pts) },
                            viewModel = viewModel
                        )
                    }
                    2 -> {
                        // TAB 2: BILL SPLITTER
                        BillSplitterView(
                            onConvertToKhata = { total, title, payerIsYou, shares, upi ->
                                viewModel.convertBillSplitToKhata(total, title, payerIsYou, shares, upi)
                                selectedTab = 0
                            },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    // Modal Dialog: Add New Customer Credit Entry
    if (showAddKhataDialog) {
        AddCustomerKhataDialog(
            onDismiss = { showAddKhataDialog = false },
            onSave = { name, phone, type, amt, desc, invoice, dueDays, tag ->
                viewModel.addKhataEntry(
                    personName = name,
                    personPhoneOrUpi = phone,
                    type = type,
                    amount = amt,
                    description = desc,
                    dueDaysFromNow = dueDays,
                    invoiceNumber = invoice,
                    customerTag = tag
                )
                showAddKhataDialog = false
            }
        )
    }

    // Modal Dialog: Partial Installment Payment
    selectedKhataForInstallment?.let { khataEntry ->
        PartialKhataPaymentDialog(
            entry = khataEntry,
            onDismiss = { viewModel.closeKhataInstallmentDialog() },
            onRecordPayment = { amt, mode, utr, note ->
                viewModel.recordPartialKhataPayment(
                    khataId = khataEntry.id,
                    installmentAmount = amt,
                    paymentMode = mode,
                    utrNumber = utr,
                    note = note
                )
            }
        )
    }

    // Modal Dialog: One-Tap WhatsApp Payment Reminder
    whatsappReminderState?.let { reminderState ->
        val context = LocalContext.current
        WhatsAppPaymentReminderDialog(
            state = reminderState,
            onDismiss = { viewModel.closeWhatsAppReminderDialog() },
            onSendWhatsApp = { viewModel.sendWhatsAppReminder(context, reminderState) },
            onCopyText = { viewModel.copyWhatsAppReminder(context, reminderState) }
        )
    }

    // Modal Dialog: Customer Profile & Invoices Detail Sheet
    selectedCustomerProfile?.let { profile ->
        CustomerProfileDetailDialog(
            profile = profile,
            onDismiss = { viewModel.selectCustomerProfile(null) },
            onSendReminder = { viewModel.openWhatsAppReminderForCustomer(profile) },
            onRedeemPoints = { pts -> viewModel.redeemCustomerLoyaltyPoints(profile.customerName, pts) },
            onOpenInstallment = { entry -> viewModel.openKhataInstallmentDialog(entry) }
        )
    }
}

// -------------------------------------------------------------
// TAB 0: CUSTOMER KHATA & CREDIT LEDGER VIEW
// -------------------------------------------------------------
@Composable
private fun ColumnScope.CustomerKhataLedgerView(
    entries: List<KhataEntryEntity>,
    youWillGet: Double,
    youWillPay: Double,
    net: Double,
    onAddClick: () -> Unit,
    viewModel: CyphrViewModel
) {
    val context = LocalContext.current
    var filterType by remember { mutableStateOf("ALL") } // ALL, GET, PAY, OVERDUE, SETTLED
    var searchQuery by remember { mutableStateOf("") }

    val now = remember { System.currentTimeMillis() }

    val filteredEntries = remember(entries, filterType, searchQuery) {
        entries.filter { entry ->
            val matchesSearch = searchQuery.isBlank() ||
                entry.personName.contains(searchQuery, ignoreCase = true) ||
                entry.personPhoneOrUpi.contains(searchQuery, ignoreCase = true) ||
                entry.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                entry.description.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (filterType) {
                "GET" -> entry.type == "YOU_WILL_GET" && !entry.isSettled
                "PAY" -> entry.type == "YOU_WILL_PAY" && !entry.isSettled
                "OVERDUE" -> entry.isOverdue(now)
                "SETTLED" -> entry.isSettled
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    // Summary Metric Cards
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CyberCard(
            modifier = Modifier.weight(1f),
            borderColor = AcidLime.copy(alpha = 0.35f),
            backgroundColor = WarmSurfaceElevated,
            cornerCut = 12.dp
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CallReceived,
                        contentDescription = null,
                        tint = AcidLime,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "STORE CREDIT (GET)",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GhostSilverMuted
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "₹${youWillGet.toInt()}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = AcidLime
                )
            }
        }

        CyberCard(
            modifier = Modifier.weight(1f),
            borderColor = CyberRed.copy(alpha = 0.35f),
            backgroundColor = WarmSurfaceElevated,
            cornerCut = 12.dp
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CallMade,
                        contentDescription = null,
                        tint = CyberRed,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PAYABLES (OWE)",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GhostSilverMuted
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "₹${youWillPay.toInt()}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberRed
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Search Bar & Filter Chips
    OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search customer, phone, invoice...", fontSize = 11.sp, color = GhostSilverMuted) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GhostSilverMuted, modifier = Modifier.size(16.dp)) },
        modifier = Modifier.fillMaxWidth().height(46.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = GhostSilver,
            unfocusedTextColor = GhostSilver,
            focusedBorderColor = EmberOrange,
            unfocusedBorderColor = WarmBorderSubtle
        ),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Filter Chips & Add Button Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filterOptions = listOf(
                "ALL" to "All (${entries.size})",
                "OVERDUE" to "Overdue (${entries.count { it.isOverdue(now) }})",
                "GET" to "Receivables",
                "PAY" to "Payables",
                "SETTLED" to "Settled"
            )
            items(filterOptions) { (key, label) ->
                val isSelected = filterType == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) EmberOrange.copy(alpha = 0.25f) else WarmSurfaceElevated)
                        .border(1.dp, if (isSelected) EmberOrange else WarmBorderSubtle, RoundedCornerShape(6.dp))
                        .clickable { filterType = key }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) EmberOrange else GhostSilver
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = EmberOrange, contentColor = VoidBlack),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(30.dp).testTag("btn_add_customer_credit"),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("NEW CREDIT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (filteredEntries.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(WarmSurfaceElevated)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = GhostSilverMuted, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No matching Khata records", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GhostSilver)
                Text("Log open store credits, partial payments, or send payment reminders.", fontSize = 11.sp, color = GhostSilverMuted)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredEntries, key = { it.id }) { entry ->
                CustomerKhataCard(
                    entry = entry,
                    now = now,
                    onOpenInstallment = { viewModel.openKhataInstallmentDialog(entry) },
                    onOpenWhatsAppReminder = { viewModel.openWhatsAppReminderForKhata(entry) },
                    onSettleFull = { viewModel.settleKhataEntry(entry.id, entry.personName, entry.remainingAmount) },
                    onDelete = { viewModel.deleteKhataEntry(entry.id) }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// CUSTOMER KHATA ITEM CARD WITH PARTIAL INSTALLMENT PROGRESS
// -------------------------------------------------------------
@Composable
private fun CustomerKhataCard(
    entry: KhataEntryEntity,
    now: Long,
    onOpenInstallment: () -> Unit,
    onOpenWhatsAppReminder: () -> Unit,
    onSettleFull: () -> Unit,
    onDelete: () -> Unit
) {
    val isGet = entry.type == "YOU_WILL_GET"
    val isOverdue = entry.isOverdue(now)
    val amountColor = if (isGet) AcidLime else CyberRed

    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    val dateStr = remember(entry.dateTimestamp) { dateFormat.format(Date(entry.dateTimestamp)) }
    val dueDateStr = remember(entry.dueDateTimestamp) {
        entry.dueDateTimestamp?.let { dateFormat.format(Date(it)) } ?: "No due date"
    }

    CyberCard(
        modifier = Modifier.fillMaxWidth().testTag("khata_item_${entry.id}"),
        borderColor = if (entry.isSettled) WarmBorderSubtle else if (isOverdue) CyberRed.copy(alpha = 0.6f) else amountColor.copy(alpha = 0.35f),
        backgroundColor = WarmSurfaceElevated,
        cornerCut = 12.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            // Top Row: Avatar, Name, Invoice Ref, Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(amountColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = entry.personName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = amountColor
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = entry.personName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GhostSilver
                            )
                            if (entry.customerTag.isNotBlank() && entry.customerTag != "REGULAR") {
                                Spacer(modifier = Modifier.width(6.dp))
                                CyberBadge(
                                    text = entry.customerTag,
                                    color = if (entry.customerTag == "VIP") GoldTierColor else EmberOrange,
                                    fontSize = 8.sp
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (entry.personPhoneOrUpi.isNotBlank()) {
                                Text(
                                    text = entry.personPhoneOrUpi,
                                    fontSize = 10.sp,
                                    color = GhostSilverMuted
                                )
                            }
                            if (entry.invoiceNumber.isNotBlank()) {
                                if (entry.personPhoneOrUpi.isNotBlank()) Text(" • ", fontSize = 10.sp, color = GhostSilverMuted)
                                Text(
                                    text = "#${entry.invoiceNumber}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = NeonCyan
                                )
                            }
                        }
                    }
                }

                // Balance amounts
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${entry.remainingAmount.toInt()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (entry.isSettled) AcidLime else amountColor
                    )
                    Text(
                        text = if (entry.isSettled) "SETTLED" else if (entry.paidAmount > 0) "Rem of ₹${entry.amount.toInt()}" else if (isGet) "You Will Get" else "You Owe",
                        fontSize = 9.sp,
                        color = if (entry.isSettled) AcidLime else GhostSilverMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Description / Note
            if (entry.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = entry.description,
                    fontSize = 11.sp,
                    color = GhostSilverMuted,
                    maxLines = 2
                )
            }

            // Running Balance Progress Bar (if partial payments exist)
            if (entry.amount > 0 && !entry.isSettled) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Paid: ₹${entry.paidAmount.toInt()} (${(entry.progressFraction * 100).toInt()}%)",
                            fontSize = 9.sp,
                            color = AcidLime
                        )
                        Text(
                            text = if (isOverdue) "⚠️ OVERDUE (Due $dueDateStr)" else "Due: $dueDateStr",
                            fontSize = 9.sp,
                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
                            color = if (isOverdue) CyberRed else GhostSilverMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { entry.progressFraction },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = AcidLime,
                        trackColor = WarmTrackBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = GhostSilverMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Right side: WhatsApp Reminder, Partial Payment, and Settle buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!entry.isSettled && isGet) {
                        // WhatsApp Reminder Button
                        Button(
                            onClick = onOpenWhatsAppReminder,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WhatsAppGreen.copy(alpha = 0.18f),
                                contentColor = WhatsAppGreen
                            ),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.4f)),
                            modifier = Modifier.height(28.dp).testTag("btn_whatsapp_reminder_${entry.id}"),
                            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WHATSAPP", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (!entry.isSettled) {
                        // Partial Installment Button
                        Button(
                            onClick = onOpenInstallment,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmberOrange.copy(alpha = 0.2f),
                                contentColor = EmberOrange
                            ),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, EmberOrange.copy(alpha = 0.5f)),
                            modifier = Modifier.height(28.dp).testTag("btn_partial_pay_${entry.id}"),
                            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ PARTIAL", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }

                        // Full Settle Button
                        Button(
                            onClick = onSettleFull,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AcidLime,
                                contentColor = VoidBlack
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp).testTag("btn_settle_full_${entry.id}"),
                            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp), tint = VoidBlack)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("SETTLE", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = VoidBlack)
                        }
                    } else {
                        CyberBadge(text = "✓ FULLY SETTLED", color = AcidLime, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: CUSTOMER PROFILES & PURCHASE HISTORY VIEW
// -------------------------------------------------------------
@Composable
private fun ColumnScope.CustomerProfilesListView(
    profiles: List<CustomerProfileData>,
    onSelectProfile: (CustomerProfileData) -> Unit,
    onSendReminder: (CustomerProfileData) -> Unit,
    onRedeemPoints: (String, Int) -> Unit,
    viewModel: CyphrViewModel
) {
    var searchCustomerQuery by remember { mutableStateOf("") }

    val filteredProfiles = remember(profiles, searchCustomerQuery) {
        if (searchCustomerQuery.isBlank()) profiles
        else profiles.filter {
            it.customerName.contains(searchCustomerQuery, ignoreCase = true) ||
            it.customerPhone.contains(searchCustomerQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
        // Search Customer
        OutlinedTextField(
            value = searchCustomerQuery,
            onValueChange = { searchCustomerQuery = it },
            placeholder = { Text("Search customer by name or phone...", fontSize = 11.sp, color = GhostSilverMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GhostSilverMuted, modifier = Modifier.size(16.dp)) },
            modifier = Modifier.fillMaxWidth().height(46.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = GhostSilver,
                unfocusedTextColor = GhostSilver,
                focusedBorderColor = EmberOrange,
                unfocusedBorderColor = WarmBorderSubtle
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Profiles Count Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "REGISTERED CUSTOMERS (${filteredProfiles.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GhostSilverMuted
            )
            Text(
                text = "Loyalty & Spend Metrics",
                fontSize = 10.sp,
                color = EmberOrange
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredProfiles.isEmpty()) {
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
                    Icon(Icons.Default.Person, contentDescription = null, tint = GhostSilverMuted, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No customer records found", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GhostSilver)
                    Text("Customer profiles are auto-generated from POS sales and Khata credits.", fontSize = 11.sp, color = GhostSilverMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredProfiles, key = { it.customerName + it.customerPhone }) { profile ->
                    CustomerProfileCard(
                        profile = profile,
                        onViewDetails = { onSelectProfile(profile) },
                        onSendReminder = { onSendReminder(profile) },
                        onRedeemPoints = { pts -> onRedeemPoints(profile.customerName, pts) }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// CUSTOMER PROFILE ITEM CARD
// -------------------------------------------------------------
@Composable
private fun CustomerProfileCard(
    profile: CustomerProfileData,
    onViewDetails: () -> Unit,
    onSendReminder: () -> Unit,
    onRedeemPoints: (Int) -> Unit
) {
    val tierColor = when (profile.loyaltyTier) {
        CustomerLoyaltyTier.PLATINUM -> PlatinumTierColor
        CustomerLoyaltyTier.GOLD -> GoldTierColor
        CustomerLoyaltyTier.SILVER -> GhostSilver
        CustomerLoyaltyTier.BRONZE -> BronzeTierColor
    }

    CyberCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() }
            .testTag("customer_card_${profile.customerName}"),
        borderColor = if (profile.outstandingKhataBalance > 0) CyberRed.copy(alpha = 0.4f) else WarmBorderSubtle,
        backgroundColor = WarmSurfaceElevated,
        cornerCut = 14.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            // Header: Name, Tier Badge, Points
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(tierColor.copy(alpha = 0.2f))
                            .border(1.dp, tierColor.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.customerName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = tierColor
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.customerName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GhostSilver
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            CyberBadge(
                                text = profile.loyaltyTier.tierName,
                                color = tierColor,
                                fontSize = 8.5.sp
                            )
                        }
                        if (profile.customerPhone.isNotBlank()) {
                            Text(
                                text = profile.customerPhone,
                                fontSize = 10.sp,
                                color = GhostSilverMuted
                            )
                        }
                    }
                }

                // Loyalty Points Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(GoldTierColor.copy(alpha = 0.15f))
                        .border(1.dp, GoldTierColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = GoldTierColor, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${profile.loyaltyPoints} pts",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldTierColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stats Grid (Total Spend, Avg Spend, Bills Count, Open Khata)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(WarmCard)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("LIFETIME SPEND", fontSize = 8.sp, color = GhostSilverMuted, fontWeight = FontWeight.SemiBold)
                    Text("₹${profile.totalLifetimeSpend.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AcidLime)
                }
                Column {
                    Text("AVG BILL", fontSize = 8.sp, color = GhostSilverMuted, fontWeight = FontWeight.SemiBold)
                    Text("₹${profile.averageBillAmount.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GhostSilver)
                }
                Column {
                    Text("BILLS", fontSize = 8.sp, color = GhostSilverMuted, fontWeight = FontWeight.SemiBold)
                    Text("${profile.totalBillsCount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("OPEN CREDIT", fontSize = 8.sp, color = GhostSilverMuted, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (profile.outstandingKhataBalance > 0) "₹${profile.outstandingKhataBalance.toInt()}" else "₹0",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (profile.outstandingKhataBalance > 0) CyberRed else AcidLime
                    )
                }
            }

            // Frequent Items Chips
            if (profile.frequentItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "FREQUENT PURCHASES:",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = GhostSilverMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(profile.frequentItems.take(4)) { item ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(WarmTrackBackground)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${item.itemName} (${item.quantity.toInt()} ${item.unit})",
                                fontSize = 9.5.sp,
                                color = GhostSilver
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Row: WhatsApp Reminder & View Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap to view full purchase history & invoices",
                    fontSize = 9.sp,
                    color = GhostSilverMuted
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (profile.outstandingKhataBalance > 0) {
                        Button(
                            onClick = onSendReminder,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WhatsAppGreen.copy(alpha = 0.2f),
                                contentColor = WhatsAppGreen
                            ),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.5f)),
                            modifier = Modifier.height(26.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("REMIND ₹${profile.outstandingKhataBalance.toInt()}", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onViewDetails,
                        colors = ButtonDefaults.buttonColors(containerColor = EmberOrange, contentColor = VoidBlack),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(26.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("VIEW PROFILE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOG: PARTIAL KHATA INSTALLMENT PAYMENT
// -------------------------------------------------------------
@Composable
private fun PartialKhataPaymentDialog(
    entry: KhataEntryEntity,
    onDismiss: () -> Unit,
    onRecordPayment: (amount: Double, mode: String, utr: String, note: String) -> Unit
) {
    val remaining = entry.remainingAmount
    var amountText by remember { mutableStateOf(remaining.toInt().toString()) }
    var selectedPaymentMode by remember { mutableStateOf("CASH") } // CASH, UPI_INSTANT, GPAY, PAYTM, PHONEPE, NEFT
    var utrNumber by remember { mutableStateOf("") }
    var paymentNote by remember { mutableStateOf("") }

    val paymentModes = listOf("CASH", "UPI_INSTANT", "GPAY", "PHONEPE", "PAYTM", "NEFT")

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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LOG PARTIAL PAYMENT",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GhostSilver
                        )
                        Text(
                            text = "Account: ${entry.personName} (${entry.description.take(25)})",
                            fontSize = 11.sp,
                            color = GhostSilverMuted
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = GhostSilverMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Balance summary card
                CyberCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = AcidLime.copy(alpha = 0.3f),
                    backgroundColor = WarmSurfaceElevated,
                    cornerCut = 10.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("TOTAL AMOUNT", fontSize = 8.5.sp, color = GhostSilverMuted)
                            Text("₹${entry.amount.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GhostSilver)
                        }
                        Column {
                            Text("PAID SO FAR", fontSize = 8.5.sp, color = GhostSilverMuted)
                            Text("₹${entry.paidAmount.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AcidLime)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("REMAINING DUE", fontSize = 8.5.sp, color = GhostSilverMuted)
                            Text("₹${remaining.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = EmberOrange)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick percentage presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        "25%" to (remaining * 0.25),
                        "50%" to (remaining * 0.50),
                        "75%" to (remaining * 0.75),
                        "Full Due" to remaining
                    )
                    presets.forEach { (label, amt) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(WarmSurfaceElevated)
                                .border(1.dp, WarmBorderSubtle, RoundedCornerShape(6.dp))
                                .clickable { amountText = amt.toInt().toString() }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = EmberOrange)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Amount Text Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Installment Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = GhostSilver,
                        unfocusedTextColor = GhostSilver,
                        focusedBorderColor = EmberOrange,
                        unfocusedBorderColor = WarmBorderSubtle
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Payment Mode Selector
                Text("PAYMENT MODE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GhostSilverMuted)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(paymentModes) { mode ->
                        val isSelected = selectedPaymentMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) EmberOrange else WarmSurfaceElevated)
                                .clickable { selectedPaymentMode = mode }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = mode.replace("_", " "),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) VoidBlack else GhostSilver
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // UTR / Note
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = utrNumber,
                        onValueChange = { utrNumber = it },
                        label = { Text("UTR / Ref # (Optional)") },
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
                        value = paymentNote,
                        onValueChange = { paymentNote = it },
                        label = { Text("Note (e.g. GPay part 1)") },
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

                Spacer(modifier = Modifier.height(16.dp))

                val payAmt = amountText.toDoubleOrNull() ?: 0.0
                Button(
                    onClick = {
                        if (payAmt > 0) {
                            onRecordPayment(payAmt, selectedPaymentMode, utrNumber, paymentNote)
                        }
                    },
                    enabled = payAmt > 0 && payAmt <= (remaining + 1.0),
                    colors = ButtonDefaults.buttonColors(containerColor = AcidLime, contentColor = VoidBlack),
                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("btn_confirm_partial_payment"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = VoidBlack)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RECORD ₹${payAmt.toInt()} PAYMENT",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VoidBlack
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOG: ONE-TAP WHATSAPP PAYMENT REMINDER
// -------------------------------------------------------------
@Composable
private fun WhatsAppPaymentReminderDialog(
    state: WhatsAppReminderState,
    onDismiss: () -> Unit,
    onSendWhatsApp: () -> Unit,
    onCopyText: () -> Unit
) {
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
                    .border(BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.5f)), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(WhatsAppGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "WHATSAPP REMINDER",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GhostSilver
                            )
                            Text(
                                text = "Auto-generated UPI Payment Intent Link",
                                fontSize = 10.sp,
                                color = GhostSilverMuted
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = GhostSilverMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Recipient & Outstanding Info
                CyberCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = WarmBorderSubtle,
                    backgroundColor = WarmSurfaceElevated,
                    cornerCut = 10.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(state.customerName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GhostSilver)
                            Text(state.customerPhone.ifBlank { "No phone saved" }, fontSize = 10.sp, color = GhostSilverMuted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("DUE AMOUNT", fontSize = 8.5.sp, color = GhostSilverMuted)
                            Text("₹${state.totalOutstanding.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AcidLime)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Formatted Message Preview
                Text("MESSAGE PREVIEW (WITH DYNAMIC UPI LINK):", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GhostSilverMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(WarmTrackBackground)
                        .border(1.dp, WarmBorderSubtle, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = state.formattedMessageText,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = GhostSilver,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCopyText,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarmSurfaceElevated,
                            contentColor = GhostSilver
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, WarmBorderSubtle),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("COPY TEXT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onSendWhatsApp,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhatsAppGreen,
                            contentColor = VoidBlack
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f).height(44.dp).testTag("btn_send_whatsapp_reminder")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = VoidBlack)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("OPEN WHATSAPP", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = VoidBlack)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOG: CUSTOMER PROFILE & INVOICES DETAIL SHEET
// -------------------------------------------------------------
@Composable
private fun CustomerProfileDetailDialog(
    profile: CustomerProfileData,
    onDismiss: () -> Unit,
    onSendReminder: () -> Unit,
    onRedeemPoints: (Int) -> Unit,
    onOpenInstallment: (KhataEntryEntity) -> Unit
) {
    val tierColor = when (profile.loyaltyTier) {
        CustomerLoyaltyTier.PLATINUM -> PlatinumTierColor
        CustomerLoyaltyTier.GOLD -> GoldTierColor
        CustomerLoyaltyTier.SILVER -> GhostSilver
        CustomerLoyaltyTier.BRONZE -> BronzeTierColor
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VoidBlack.copy(alpha = 0.94f))
                .padding(horizontal = 14.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(WarmCard)
                    .border(BorderStroke(1.dp, WarmBorder), RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(tierColor.copy(alpha = 0.2f))
                                .border(1.5.dp, tierColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = profile.customerName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = tierColor
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(profile.customerName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GhostSilver)
                                Spacer(modifier = Modifier.width(6.dp))
                                CyberBadge(text = profile.loyaltyTier.tierName, color = tierColor, fontSize = 9.sp)
                            }
                            Text(profile.customerPhone.ifBlank { "No phone recorded" }, fontSize = 11.sp, color = GhostSilverMuted)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = GhostSilverMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Loyalty Points Banner with Redeem Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(WarmSurfaceElevated)
                        .border(1.dp, GoldTierColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = GoldTierColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("${profile.loyaltyPoints} Store Loyalty Points", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldTierColor)
                            Text("1 Point = ₹0.50 instant discount on checkout", fontSize = 9.5.sp, color = GhostSilverMuted)
                        }
                    }

                    if (profile.loyaltyPoints >= 50) {
                        Button(
                            onClick = { onRedeemPoints(100.coerceAtMost(profile.loyaltyPoints)) },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldTierColor, contentColor = VoidBlack),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("REDEEM", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = VoidBlack)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Metric Summary Cards
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CyberCard(modifier = Modifier.weight(1f), borderColor = WarmBorderSubtle, backgroundColor = WarmSurfaceElevated, cornerCut = 8.dp) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("LIFETIME SPEND", fontSize = 8.sp, color = GhostSilverMuted)
                                    Text("₹${profile.totalLifetimeSpend.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AcidLime)
                                }
                            }
                            CyberCard(modifier = Modifier.weight(1f), borderColor = WarmBorderSubtle, backgroundColor = WarmSurfaceElevated, cornerCut = 8.dp) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("AVERAGE BILL", fontSize = 8.sp, color = GhostSilverMuted)
                                    Text("₹${profile.averageBillAmount.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GhostSilver)
                                }
                            }
                            CyberCard(modifier = Modifier.weight(1f), borderColor = WarmBorderSubtle, backgroundColor = WarmSurfaceElevated, cornerCut = 8.dp) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("OPEN KHATA", fontSize = 8.sp, color = GhostSilverMuted)
                                    Text("₹${profile.outstandingKhataBalance.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (profile.outstandingKhataBalance > 0) CyberRed else AcidLime)
                                }
                            }
                        }
                    }

                    // Frequent Items Section
                    if (profile.frequentItems.isNotEmpty()) {
                        item {
                            Text("TOP FREQUENT PURCHASES", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = GhostSilverMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                profile.frequentItems.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(WarmSurfaceElevated)
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(item.itemName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = GhostSilver)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("${item.quantity.toInt()} ${item.unit}", fontSize = 10.sp, color = GhostSilverMuted)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("₹${item.totalSpend.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AcidLime)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Khata Credit Ledgers Section
                    if (profile.khataEntries.isNotEmpty()) {
                        item {
                            Text("KHATA & STORE CREDIT INVOICES (${profile.khataEntries.size})", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = GhostSilverMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                profile.khataEntries.forEach { entry ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(WarmSurfaceElevated)
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (entry.invoiceNumber.isNotBlank()) "#${entry.invoiceNumber}" else "Credit Entry",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GhostSilver
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                CyberBadge(
                                                    text = if (entry.isSettled) "SETTLED" else "DUE",
                                                    color = if (entry.isSettled) AcidLime else CyberRed,
                                                    fontSize = 8.sp
                                                )
                                            }
                                            Text(entry.description, fontSize = 10.sp, color = GhostSilverMuted)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("₹${entry.remainingAmount.toInt()} rem", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (entry.isSettled) AcidLime else CyberRed)
                                            if (!entry.isSettled) {
                                                Text(
                                                    text = "Log Payment",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EmberOrange,
                                                    modifier = Modifier.clickable { onOpenInstallment(entry) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Past POS Bills Section
                    if (profile.pastTransactions.isNotEmpty()) {
                        item {
                            Text("PAST POS BILLS (${profile.pastTransactions.size})", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = GhostSilverMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                profile.pastTransactions.forEach { tx ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(WarmSurfaceElevated)
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(tx.title, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = GhostSilver)
                                            Text(dateFormat.format(Date(tx.dateTimestamp)), fontSize = 9.sp, color = GhostSilverMuted)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("₹${tx.totalAmount.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AcidLime)
                                            Text(tx.paymentMethod, fontSize = 9.sp, color = GhostSilverMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // WhatsApp Reminder Action Footer
                if (profile.outstandingKhataBalance > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onSendReminder,
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = VoidBlack),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = VoidBlack)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SEND WHATSAPP PAYMENT REMINDER (₹${profile.outstandingKhataBalance.toInt()})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VoidBlack)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: BILL SPLITTER VIEW
// -------------------------------------------------------------
@Composable
private fun ColumnScope.BillSplitterView(
    onConvertToKhata: (total: Double, title: String, payerIsYou: Boolean, shares: List<Pair<String, Double>>, upi: String) -> Unit,
    viewModel: CyphrViewModel
) {
    var billTitle by remember { mutableStateOf("Dinner at Social") }
    var totalAmountText by remember { mutableStateOf("2400") }
    var payerIsYou by remember { mutableStateOf(true) }
    var payerUpi by remember { mutableStateOf("matrics.store@okaxis") }

    val participants = remember {
        mutableStateListOf(
            "Rahul Verma",
            "Priya Sharma",
            "Amit Kumar",
            "You"
        )
    }

    var newParticipantName by remember { mutableStateOf("") }
    val totalAmount = totalAmountText.toDoubleOrNull() ?: 0.0
    val perPersonShare = if (participants.isNotEmpty() && totalAmount > 0) {
        totalAmount / participants.size
    } else 0.0

    Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
        CyberCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = WarmBorderSubtle,
            backgroundColor = WarmSurfaceElevated,
            cornerCut = 14.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = billTitle,
                    onValueChange = { billTitle = it },
                    label = { Text("Bill / Event Description") },
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
                        value = totalAmountText,
                        onValueChange = { totalAmountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Total Bill (₹)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = GhostSilver,
                            unfocusedTextColor = GhostSilver,
                            focusedBorderColor = EmberOrange,
                            unfocusedBorderColor = WarmBorderSubtle
                        ),
                        singleLine = true
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PER PERSON SHARE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = GhostSilverMuted
                        )
                        Text(
                            text = "₹${perPersonShare.toInt()}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AcidLime
                        )
                        Text(
                            text = "split across ${participants.size} people",
                            fontSize = 9.5.sp,
                            color = GhostSilverMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Participants Row
        Text(
            text = "SPLIT MEMBERS (${participants.size})",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = GhostSilverMuted
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newParticipantName,
                onValueChange = { newParticipantName = it },
                placeholder = { Text("Add friend / customer name...") },
                modifier = Modifier.weight(1f).height(46.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = GhostSilver,
                    unfocusedTextColor = GhostSilver,
                    focusedBorderColor = EmberOrange,
                    unfocusedBorderColor = WarmBorderSubtle
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newParticipantName.isNotBlank()) {
                        participants.add(newParticipantName.trim())
                        newParticipantName = ""
                    }
                },
                enabled = newParticipantName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmberOrange, contentColor = VoidBlack),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Text("ADD", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Member Chips
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(participants) { name ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(WarmSurfaceElevated)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GhostSilver
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "₹${perPersonShare.toInt()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AcidLime
                        )
                        if (name != "You" && participants.size > 2) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = GhostSilverMuted,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { participants.remove(name) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Convert to Khata action button
        Button(
            onClick = {
                val others = participants.filter { it != "You" }.map { Pair(it, perPersonShare) }
                if (others.isNotEmpty() && totalAmount > 0) {
                    onConvertToKhata(totalAmount, billTitle, payerIsYou, others, payerUpi)
                }
            },
            enabled = participants.size > 1 && totalAmount > 0,
            colors = ButtonDefaults.buttonColors(
                containerColor = EmberOrange,
                contentColor = VoidBlack
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("save_split_to_khata_button")
        ) {
            Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "CONVERT TO KHATA ENTRIES (${participants.size - 1} FRIENDS)",
                fontFamily = FontFamily.SansSerif,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// -------------------------------------------------------------
// DIALOG: ADD NEW CUSTOMER KHATA / CREDIT ENTRY
// -------------------------------------------------------------
@Composable
private fun AddCustomerKhataDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, type: String, amount: Double, desc: String, invoice: String, dueDays: Int, tag: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("YOU_WILL_GET") } // YOU_WILL_GET or YOU_WILL_PAY
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var invoiceNumber by remember { mutableStateOf("INV-${(1000..9999).random()}") }
    var dueDays by remember { mutableIntStateOf(7) }
    var customerTag by remember { mutableStateOf("REGULAR") } // REGULAR, VIP, WHOLESALE

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
                        text = "NEW CUSTOMER CREDIT RECORD",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GhostSilver
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = GhostSilverMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Type Toggle (You will get vs You will pay)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(WarmSurfaceElevated)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (type == "YOU_WILL_GET") AcidLime else WarmSurfaceElevated)
                            .clickable { type = "YOU_WILL_GET" }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "STORE CREDIT (+ RECEIVABLE)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (type == "YOU_WILL_GET") VoidBlack else GhostSilver
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (type == "YOU_WILL_PAY") CyberRed else WarmSurfaceElevated)
                            .clickable { type = "YOU_WILL_PAY" }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SUPPLIER PAYABLE (- OWE)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (type == "YOU_WILL_PAY") VoidBlack else GhostSilver
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer / Supplier Name (e.g. Ramesh Kumar)") },
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
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("WhatsApp / Phone") },
                        modifier = Modifier.weight(1.2f),
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

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = invoiceNumber,
                        onValueChange = { invoiceNumber = it },
                        label = { Text("Bill / Invoice Ref #") },
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
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Items / Purpose") },
                        modifier = Modifier.weight(1.2f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = GhostSilver,
                            unfocusedTextColor = GhostSilver,
                            focusedBorderColor = EmberOrange,
                            unfocusedBorderColor = WarmBorderSubtle
                        ),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Due date selector chips
                Text("CREDIT PERIOD / DUE DATE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GhostSilverMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val dueOptions = listOf(3 to "3 Days", 7 to "7 Days", 15 to "15 Days", 30 to "30 Days")
                    dueOptions.forEach { (days, label) ->
                        val isSel = dueDays == days
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) EmberOrange else WarmSurfaceElevated)
                                .clickable { dueDays = days }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = if (isSel) VoidBlack else GhostSilver)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val amt = amountText.toDoubleOrNull() ?: 0.0
                Button(
                    onClick = {
                        if (name.isNotBlank() && amt > 0) {
                            onSave(name, phone, type, amt, description, invoiceNumber, dueDays, customerTag)
                        }
                    },
                    enabled = name.isNotBlank() && amt > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == "YOU_WILL_GET") AcidLime else CyberRed,
                        contentColor = VoidBlack
                    ),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (type == "YOU_WILL_GET") "SAVE STORE CREDIT (₹${amt.toInt()})" else "SAVE PAYABLE (₹${amt.toInt()})",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VoidBlack
                    )
                }
            }
        }
    }
}
