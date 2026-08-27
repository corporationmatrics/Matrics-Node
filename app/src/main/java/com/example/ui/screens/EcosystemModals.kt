package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ParsedNlpItem
import com.example.ui.components.CameraCaptureView
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
import com.example.ui.viewmodel.ApiKeyStatus
import com.example.ui.viewmodel.CyphrViewModel

@Composable
fun EcosystemModals(
    viewModel: CyphrViewModel,
    modifier: Modifier = Modifier
) {
    val activeModal by viewModel.activeModal.collectAsStateWithLifecycle()

    when (activeModal) {
        "RECEIPT", "HANDWRITTEN_SCANNER" -> HandwrittenScannerModal(viewModel)
        "AUTO_IMPORT", "QUICK_COMMERCE", "BANK_SMS_EMAIL_IMPORT" -> AutoImportHubModal(viewModel)
        "INSTANT_PAY" -> InstantPayModal(viewModel)
        "SETTINGS" -> SettingsAndAiModal(viewModel)
        "CATEGORY_BUDGETS" -> CategoryBudgetsModal(viewModel)
        "RECURRING_BILLS" -> RecurringBillsModal(viewModel)
        "SAVINGS_GOALS" -> SavingsGoalsModal(viewModel)
        "KHATA_SPLITTER" -> KhataSplitterModal(viewModel)
    }
}

@Composable
fun ReceiptOcrModal(viewModel: CyphrViewModel) {
    var isLiveCameraMode by remember { mutableStateOf(false) }

    val sampleReceiptItems = remember {
        listOf(
            ParsedNlpItem("Organic Baby Spinach 250g", "Produce", 1.0, "250g", 85.0, "SuperMart Express"),
            ParsedNlpItem("Greek Feta Cheese", "Dairy", 1.0, "200g", 240.0, "SuperMart Express"),
            ParsedNlpItem("Cold Pressed Olive Oil 500ml", "Pantry", 1.0, "500ml", 490.0, "SuperMart Express"),
            ParsedNlpItem("Whole Wheat Artisan Bread", "Grains", 1.0, "loaf", 95.0, "SuperMart Express")
        )
    }

    Dialog(
        onDismissRequest = { viewModel.closeModal() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VoidBlack.copy(alpha = 0.90f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .testTag("modal_receipt_ocr"),
                borderColor = WarmBorder,
                backgroundColor = WarmCard,
                cornerCut = 28.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = EmberOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RECEIPT OCR IMPORT",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GhostSilver
                            )
                        }
                        IconButton(
                            onClick = { viewModel.closeModal() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = GhostSilver)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isLiveCameraMode) {
                        // Live Camera Viewfinder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(WarmSurfaceElevated)
                                .border(1.dp, EmberOrange.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        ) {
                            CameraCaptureView(
                                onPhotoCaptured = { bmp ->
                                    isLiveCameraMode = false
                                    viewModel.onHandwrittenRealBitmapCaptured(bmp)
                                },
                                onGalleryPickRequested = {
                                    isLiveCameraMode = false
                                },
                                hudTitle = "PRINTED RECEIPT SENSOR",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { isLiveCameraMode = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Text("BACK TO ITEMS PREVIEW", color = GhostSilver, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        // Scanner Viewfinder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(115.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(WarmSurfaceElevated)
                                .border(1.dp, WarmBorder, RoundedCornerShape(16.dp))
                                .clickable { isLiveCameraMode = true }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Camera OCR",
                                    tint = EmberOrange,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "TAP TO OPEN LIVE CAMERA VIEWFINDER",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GhostSilver
                                )
                                Text(
                                    text = "Auto-extracts line-items, tax & store names via Gemini",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 10.sp,
                                    color = GhostSilverMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "EXTRACTED LINE-ITEMS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = GhostSilverMuted,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(sampleReceiptItems) { item ->
                                CyberCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    borderColor = WarmBorderSubtle,
                                    backgroundColor = WarmSurfaceElevated,
                                    cornerCut = 14.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = item.name,
                                                fontFamily = FontFamily.SansSerif,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GhostSilver
                                            )
                                            Text(
                                                text = "${item.category} • ${item.unit}",
                                                fontFamily = FontFamily.SansSerif,
                                                fontSize = 10.sp,
                                                color = GhostSilverMuted
                                            )
                                        }
                                        Text(
                                            text = "₹${item.price.toInt()}",
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmberOrange
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val total = sampleReceiptItems.sumOf { it.price * it.quantity }
                        Button(
                            onClick = { viewModel.processReceiptOcr("SuperMart Express", sampleReceiptItems) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmberOrange,
                                contentColor = VoidBlack
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp).testTag("btn_confirm_ocr_import")
                        ) {
                            Text(
                                text = "IMPORT ITEMIZED RECEIPT (₹${total.toInt()})",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = VoidBlack
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickCommerceModal(viewModel: CyphrViewModel) {
    Dialog(
        onDismissRequest = { viewModel.closeModal() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VoidBlack.copy(alpha = 0.90f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .testTag("modal_quick_commerce"),
                borderColor = WarmBorder,
                backgroundColor = WarmCard,
                cornerCut = 28.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = EmberOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "QUICK-COMMERCE SYNC",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GhostSilver
                            )
                        }
                        IconButton(
                            onClick = { viewModel.closeModal() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = GhostSilver)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Connects via email receipts and app webhooks to extract itemized digital invoices automatically.",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        color = GhostSilverMuted
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val platforms = listOf(
                        Triple("Blinkit", "10-min Groceries & Dairy", EmberOrange),
                        Triple("Zepto", "Instant Fresh & Pantry", EmberPeach),
                        Triple("Instamart", "Swiggy Quick Delivery", EmberOrange)
                    )

                    platforms.forEach { (name, desc, color) ->
                        CyberCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { viewModel.simulateQuickCommerceSync(name) }
                                .testTag("btn_sync_${name.lowercase()}"),
                            borderColor = WarmBorderSubtle,
                            backgroundColor = WarmSurfaceElevated,
                            cornerCut = 16.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = name,
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GhostSilver
                                    )
                                    Text(
                                        text = desc,
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 11.sp,
                                        color = GhostSilverMuted
                                    )
                                }

                                CyberBadge(
                                    text = "SYNC NOW",
                                    color = color,
                                    backgroundColor = WarmCard
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InstantPayModal(viewModel: CyphrViewModel) {
    val context = LocalContext.current
    var payAmount by remember { mutableStateOf("150") }
    var payMerchant by remember { mutableStateOf("Ramesh Kirana") }
    var payVpa by remember { mutableStateOf("ramesh@okhdfcbank") }
    var payCategory by remember { mutableStateOf("Groceries") }
    var payNote by remember { mutableStateOf("Daily Groceries & Essentials") }

    val installedUpiApps = remember {
        com.example.data.UpiPaymentManager.getInstalledUpiApps(context)
    }

    // Activity Result Launcher for UPI Intent Flow (Deep Link Return)
    val upiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data?.getStringExtra("response") 
            ?: result.data?.dataString 
            ?: (result.data?.extras?.keySet()?.joinToString("&") { key -> "$key=${result.data?.extras?.get(key)}" } ?: "")

        val paymentResult = com.example.data.UpiPaymentManager.parseUpiResponse(data)
        when (paymentResult) {
            is com.example.data.UpiPaymentResult.Success -> {
                val amt = payAmount.toDoubleOrNull() ?: 150.0
                viewModel.recordInstantUpiExpense(
                    merchantName = payMerchant,
                    amount = amt,
                    category = payCategory,
                    payeeUpi = payVpa,
                    utrNumber = paymentResult.approvalRefNo,
                    note = payNote
                )
                viewModel.closeModal()
            }
            is com.example.data.UpiPaymentResult.Failure -> {
                viewModel.showToast("❌ Payment failed: ${paymentResult.errorMessage}")
            }
            is com.example.data.UpiPaymentResult.Cancelled -> {
                viewModel.showToast("⚠️ Payment cancelled by user")
            }
        }
    }

    fun launchPayment(targetPackage: String? = null) {
        val amt = payAmount.toDoubleOrNull() ?: 150.0
        if (payVpa.isBlank()) {
            viewModel.showToast("Please enter a valid UPI ID (VPA)")
            return
        }
        val params = com.example.data.UpiPaymentParams(
            payeeVpa = payVpa.trim(),
            payeeName = payMerchant.ifBlank { "Merchant" },
            amount = amt,
            transactionRefId = "TXN${System.currentTimeMillis()}",
            transactionNote = payNote.ifBlank { "Direct UPI Transfer" }
        )

        try {
            val intent = com.example.data.UpiPaymentManager.createUpiIntent(params, targetPackage)
            upiLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            viewModel.showToast("No UPI apps found to handle payment. Use test simulation.")
        } catch (e: Exception) {
            viewModel.showToast("Could not launch UPI app: ${e.message}")
        }
    }

    Dialog(
        onDismissRequest = { viewModel.closeModal() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VoidBlack.copy(alpha = 0.92f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .testTag("modal_instant_pay"),
                borderColor = WarmBorder,
                backgroundColor = WarmCard,
                cornerCut = 24.dp
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Payment,
                                    contentDescription = null,
                                    tint = EmberOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "UPI INTENT INSTANT PAY",
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GhostSilver
                                    )
                                    Text(
                                        text = "Direct bank-to-bank deep link via GPay / PhonePe / Paytm",
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 10.sp,
                                        color = GhostSilverMuted
                                    )
                                }
                            }
                            IconButton(
                                onClick = { viewModel.closeModal() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = GhostSilver)
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = payVpa,
                            onValueChange = { payVpa = it },
                            label = { Text("Payee UPI ID (VPA)", color = GhostSilverMuted) },
                            placeholder = { Text("e.g. shopkeeper@okhdfcbank", color = GhostSilverMuted.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmberOrange,
                                unfocusedBorderColor = WarmBorder,
                                focusedTextColor = GhostSilver,
                                unfocusedTextColor = GhostSilver
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("input_payee_upi")
                        )
                    }

                    // UPI Suffix Chips
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val suffixes = listOf("@okhdfcbank", "@okaxis", "@paytm", "@ybl", "@upi")
                            suffixes.forEach { suffix ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(WarmSurfaceElevated)
                                        .border(1.dp, WarmBorderSubtle, RoundedCornerShape(8.dp))
                                        .clickable {
                                            val prefix = if (payVpa.contains("@")) payVpa.substringBefore("@") else payVpa
                                            payVpa = if (prefix.isNotBlank()) "$prefix$suffix" else "merchant$suffix"
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(suffix, fontSize = 10.sp, color = GhostSilverMuted, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = payMerchant,
                                onValueChange = { payMerchant = it },
                                label = { Text("Merchant / Store", color = GhostSilverMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmberOrange,
                                    unfocusedBorderColor = WarmBorder,
                                    focusedTextColor = GhostSilver,
                                    unfocusedTextColor = GhostSilver
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.3f).testTag("input_payee_merchant")
                            )

                            OutlinedTextField(
                                value = payAmount,
                                onValueChange = { payAmount = it },
                                label = { Text("Amount (₹)", color = GhostSilverMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmberOrange,
                                    unfocusedBorderColor = WarmBorder,
                                    focusedTextColor = GhostSilver,
                                    unfocusedTextColor = GhostSilver
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).testTag("input_pay_amount")
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = payNote,
                            onValueChange = { payNote = it },
                            label = { Text("Transaction Note / Bill Description", color = GhostSilverMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmberOrange,
                                unfocusedBorderColor = WarmBorder,
                                focusedTextColor = GhostSilver,
                                unfocusedTextColor = GhostSilver
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("input_pay_note")
                        )
                    }

                    // UPI Rail Status Card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(WarmSurfaceElevated)
                                .border(1.dp, WarmBorderSubtle, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AcidLime,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "CLIENT-SIDE ZERO COMPLIANCE NPCI DEEP LINK",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AcidLime
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Bypasses camera scanner. Directly opens banking app with pre-filled VPA & ₹$payAmount. UTR auto-captured upon completion.",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 11.sp,
                                    color = GhostSilverMuted
                                )
                            }
                        }
                    }

                    // Launch Buttons: Default OS Chooser vs Specific Apps
                    item {
                        Button(
                            onClick = { launchPayment(null) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmberOrange,
                                contentColor = VoidBlack
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_launch_upi_intent")
                        ) {
                            Text(
                                text = "PAY ₹$payAmount VIA UPI (ALL APPS)",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = VoidBlack
                            )
                        }
                    }

                    if (installedUpiApps.isNotEmpty()) {
                        item {
                            Text(
                                text = "OR LAUNCH SPECIFIC INSTALLED APP:",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = GhostSilverMuted
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                installedUpiApps.take(3).forEach { app ->
                                    Button(
                                        onClick = { launchPayment(app.packageName) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = WarmSurfaceElevated,
                                            contentColor = GhostSilver
                                        ),
                                        border = BorderStroke(1.dp, EmberOrange.copy(alpha = 0.6f)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Text(app.appName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Simulation / Offline test button for emulator without real bank accounts
                    item {
                        OutlinedButton(
                            onClick = {
                                val amt = payAmount.toDoubleOrNull() ?: 150.0
                                val fakeUtr = "UTR${System.currentTimeMillis().toString().takeLast(8)}"
                                viewModel.recordInstantUpiExpense(
                                    merchantName = payMerchant,
                                    amount = amt,
                                    category = payCategory,
                                    payeeUpi = payVpa,
                                    utrNumber = fakeUtr,
                                    note = payNote
                                )
                                viewModel.closeModal()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp).testTag("btn_simulate_upi_success")
                        ) {
                            Text("SIMULATE SUCCESSFUL UPI CALLBACK (EMULATOR TEST)", fontSize = 11.sp, color = AcidLime, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsAndAiModal(viewModel: CyphrViewModel) {
    val currentApiKey by viewModel.userGeminiApiKey.collectAsStateWithLifecycle()
    val apiStatus by viewModel.apiKeyTestStatus.collectAsStateWithLifecycle()
    val isFieldMode by viewModel.isFieldMode.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current

    var inputKey by remember(currentApiKey) { mutableStateOf(currentApiKey) }

    Dialog(
        onDismissRequest = { viewModel.closeModal() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VoidBlack.copy(alpha = 0.90f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .testTag("modal_settings_ai"),
                borderColor = WarmBorder,
                backgroundColor = WarmCard,
                cornerCut = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = EmberOrange,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "AI ENGINE & API KEYS",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GhostSilver
                                )
                                Text(
                                    text = "Gemini 3.5 Flash Multimodal Pipeline",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 10.sp,
                                    color = GhostSilverMuted
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.closeModal() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = GhostSilver)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // User Profile & System Status
                        item {
                            CyberCard(
                                modifier = Modifier.fillMaxWidth(),
                                borderColor = WarmBorderSubtle,
                                backgroundColor = WarmSurfaceElevated,
                                cornerCut = 16.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(EmberPeach),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Profile",
                                            tint = EmberOrange,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "OPERATOR SESSION",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            color = EmberOrange,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Buddy Pro • Offline / Cloud Hybrid",
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = GhostSilver
                                        )
                                    }
                                    CyberBadge(
                                        text = if (inputKey.isNotBlank()) "AI ACTIVE" else "OFFLINE NLP",
                                        color = if (inputKey.isNotBlank()) AcidLime else GhostSilverMuted,
                                        backgroundColor = WarmCard
                                    )
                                }
                            }
                        }

                        // Gemini API Key Input Card
                        item {
                            CyberCard(
                                modifier = Modifier.fillMaxWidth(),
                                borderColor = WarmBorder,
                                backgroundColor = WarmSurfaceElevated,
                                cornerCut = 18.dp
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
                                                imageVector = Icons.Default.Key,
                                                contentDescription = null,
                                                tint = EmberOrange,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "GEMINI API KEY",
                                                fontFamily = FontFamily.SansSerif,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GhostSilver
                                            )
                                        }

                                        Text(
                                            text = "gemini-3.5-flash",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = EmberOrange
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Paste your Google Gemini API key to enable live voice parsing, multimodal receipt OCR, and spending insights.",
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 11.sp,
                                        color = GhostSilverMuted
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = inputKey,
                                        onValueChange = { inputKey = it },
                                        placeholder = { Text("AIzaSy...", color = SteelGrey, fontSize = 12.sp) },
                                        label = { Text("Paste Gemini Key", color = GhostSilverMuted, fontSize = 11.sp) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EmberOrange,
                                            unfocusedBorderColor = WarmBorder,
                                            focusedTextColor = GhostSilver,
                                            unfocusedTextColor = GhostSilver
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("input_gemini_api_key"),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Action buttons for Key
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val clip = clipboardManager.getText()?.text
                                                if (!clip.isNullOrBlank()) {
                                                    inputKey = clip.trim()
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f).testTag("btn_paste_key")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentPaste,
                                                contentDescription = "Paste",
                                                tint = GhostSilver,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("PASTE", fontSize = 11.sp, color = GhostSilver)
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.saveGeminiApiKey(inputKey)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = EmberOrange,
                                                contentColor = VoidBlack
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1.2f).testTag("btn_save_key")
                                        ) {
                                            Text(
                                                text = "SAVE KEY",
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = VoidBlack
                                            )
                                        }

                                        if (currentApiKey.isNotBlank()) {
                                            IconButton(
                                                onClick = {
                                                    inputKey = ""
                                                    viewModel.clearGeminiApiKey()
                                                },
                                                modifier = Modifier.size(40.dp).testTag("btn_clear_key")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Clear",
                                                    tint = GhostSilverMuted
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Connection Test Section
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.testGeminiApiKey(inputKey) },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("btn_test_gemini_connection")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Test",
                                                tint = EmberOrange,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("TEST CONNECTION", fontSize = 11.sp, color = EmberOrange)
                                        }

                                        when (apiStatus) {
                                            is ApiKeyStatus.Testing -> {
                                                CircularProgressIndicator(
                                                    color = EmberOrange,
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            }
                                            is ApiKeyStatus.Success -> {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Success",
                                                        tint = AcidLime,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("CONNECTED", fontSize = 10.sp, color = AcidLime, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            is ApiKeyStatus.Error -> {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.ErrorOutline,
                                                        contentDescription = "Error",
                                                        tint = EmberOrange,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("FAILED", fontSize = 10.sp, color = EmberOrange, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            ApiKeyStatus.Idle -> {
                                                Text("IDLE", fontSize = 10.sp, color = GhostSilverMuted)
                                            }
                                        }
                                    }

                                    if (apiStatus is ApiKeyStatus.Error) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = (apiStatus as ApiKeyStatus.Error).error,
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 10.sp,
                                            color = EmberOrange
                                        )
                                    }
                                }
                            }
                        }

                        // Category-Level Budgets & Overspend Limits Setting Card
                        item {
                            val overspentCategories by viewModel.overspentCategories.collectAsStateWithLifecycle()
                            val categoryCount = viewModel.categoryBudgets.collectAsStateWithLifecycle().value.size

                            CyberCard(
                                modifier = Modifier.fillMaxWidth().testTag("settings_category_budgets_card"),
                                borderColor = if (overspentCategories.isNotEmpty()) CyberRed.copy(alpha = 0.7f) else WarmBorderSubtle,
                                backgroundColor = WarmSurfaceElevated,
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
                                        Text(
                                            text = "CATEGORY BUDGETS & LIMITS",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmberOrange
                                        )

                                        if (overspentCategories.isNotEmpty()) {
                                            Text(
                                                text = "${overspentCategories.size} EXCEEDED",
                                                color = CyberRed,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "$categoryCount categories configured with active thresholds & overspend triggers.",
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 11.sp,
                                        color = GhostSilverMuted
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = { viewModel.openModal("CATEGORY_BUDGETS") },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmberOrange,
                                            contentColor = VoidBlack
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().height(40.dp).testTag("btn_settings_manage_category_budgets")
                                    ) {
                                        Text("MANAGE CATEGORY LIMITS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VoidBlack)
                                    }
                                }
                            }
                        }

                        // AI Capabilities Unlocked
                        item {
                            CyberCard(
                                modifier = Modifier.fillMaxWidth(),
                                borderColor = WarmBorderSubtle,
                                backgroundColor = WarmSurfaceElevated,
                                cornerCut = 16.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = "ENABLED AI PIPELINES",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmberOrange
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    val features = listOf(
                                        Pair("🎙️ Voice Parsing", "Extracts complex multi-item baskets with prices & stores"),
                                        Pair("📷 Multimodal OCR", "Scans paper bills & digital invoices into line-items"),
                                        Pair("💡 Spend Advisor", "Real-time month-over-month inflation & variance analyst"),
                                        Pair("📈 Commodity Trends", "Predictive price forecasts for household essentials")
                                    )

                                    features.forEach { (title, desc) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column {
                                                Text(
                                                    text = title,
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GhostSilver
                                                )
                                                Text(
                                                    text = desc,
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontSize = 10.sp,
                                                    color = GhostSilverMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Privacy & Security Notice
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Secure",
                                    tint = SteelGrey,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Your API key is stored locally on this device and communicated directly to Google Gemini servers over TLS.",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 10.sp,
                                    color = GhostSilverMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.closeModal() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmberOrange,
                            contentColor = VoidBlack
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("btn_close_settings")
                    ) {
                        Text(
                            text = "DONE & RETURN",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = VoidBlack
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Category Budgets & Overspend Management Modal
 */
@Composable
fun CategoryBudgetsModal(viewModel: CyphrViewModel) {
    val categoryBudgets by viewModel.categoryBudgets.collectAsStateWithLifecycle()
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()

    var editingCategory by remember { mutableStateOf<String?>(null) }
    var editAmountText by remember { mutableStateOf("") }

    var isAddingNewCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryAmountText by remember { mutableStateOf("") }

    val totalAllocated = remember(categoryBudgets) {
        categoryBudgets.values.sum()
    }

    Dialog(
        onDismissRequest = { viewModel.closeModal() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VoidBlack.copy(alpha = 0.92f))
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .testTag("modal_category_budgets"),
                borderColor = WarmBorder,
                backgroundColor = WarmCard,
                cornerCut = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = EmberOrange,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "CATEGORY BUDGETS",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GhostSilver
                                )
                                Text(
                                    text = "Limits, Alerts & Overspend Thresholds",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 11.sp,
                                    color = GhostSilverMuted
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.closeModal() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = GhostSilver)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Summary Stats Card
                        item {
                            CyberCard(
                                modifier = Modifier.fillMaxWidth(),
                                borderColor = WarmBorderSubtle,
                                backgroundColor = WarmSurfaceElevated,
                                cornerCut = 14.dp
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("TOTAL ALLOCATED", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = EmberOrange, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("₹${totalAllocated.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GhostSilver)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("MONTHLY CEILING", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = GhostSilverMuted, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("₹${monthlyBudget.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EmberPeach)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    val allocProgress = if (monthlyBudget > 0) (totalAllocated / monthlyBudget).coerceIn(0.0, 1.0).toFloat() else 0f
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(WarmTrackBackground)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction = allocProgress)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(if (totalAllocated > monthlyBudget) CyberRed else EmberOrange)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    // Local WorkManager background monitor status
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.NotificationsActive,
                                                contentDescription = null,
                                                tint = AcidLime,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "BACKGROUND WORKER ALERTS",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AcidLime
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(WarmCard)
                                                .border(0.5.dp, AcidLime.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                                .clickable { viewModel.triggerBudgetAlertsCheckNow() }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("RUN CHECK", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = AcidLime)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "WorkManager checks 80% & 100% budget breaches and bills due in 24h.",
                                        fontSize = 9.5.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        color = GhostSilverMuted
                                    )
                                }
                            }
                        }

                        // Category List Header with Quick Add Button
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ACTIVE CATEGORIES (${categoryBudgets.size})",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GhostSilverMuted,
                                    letterSpacing = 1.sp
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(EmberOrange.copy(alpha = 0.15f))
                                        .border(BorderStroke(0.6.dp, EmberOrange.copy(alpha = 0.4f)), RoundedCornerShape(6.dp))
                                        .clickable {
                                            isAddingNewCategory = true
                                            newCategoryName = ""
                                            newCategoryAmountText = ""
                                        }
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = EmberOrange, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ ADD CATEGORY", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = EmberOrange)
                                    }
                                }
                            }
                        }

                        // Inline Form to Add New Category
                        if (isAddingNewCategory) {
                            item {
                                CyberCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    borderColor = EmberOrange,
                                    backgroundColor = WarmSurfaceElevated,
                                    cornerCut = 12.dp
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("NEW CATEGORY BUDGET", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = EmberOrange)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = newCategoryName,
                                                onValueChange = { newCategoryName = it },
                                                placeholder = { Text("Category name", fontSize = 11.sp, color = SteelGrey) },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = EmberOrange,
                                                    unfocusedBorderColor = WarmBorder,
                                                    focusedTextColor = GhostSilver,
                                                    unfocusedTextColor = GhostSilver
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1.2f),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = newCategoryAmountText,
                                                onValueChange = { newCategoryAmountText = it.filter { ch -> ch.isDigit() } },
                                                placeholder = { Text("Limit ₹", fontSize = 11.sp, color = SteelGrey) },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = EmberOrange,
                                                    unfocusedBorderColor = WarmBorder,
                                                    focusedTextColor = GhostSilver,
                                                    unfocusedTextColor = GhostSilver
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(0.9f),
                                                singleLine = true
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = { isAddingNewCategory = false },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Text("CANCEL", fontSize = 10.sp, color = GhostSilverMuted)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    val amount = newCategoryAmountText.toDoubleOrNull() ?: 0.0
                                                    if (newCategoryName.isNotBlank() && amount > 0) {
                                                        viewModel.updateCategoryBudget(newCategoryName.trim(), amount)
                                                        isAddingNewCategory = false
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = EmberOrange, contentColor = VoidBlack),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Text("ADD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = VoidBlack)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Category items with live spend & inline budget edit
                        items(categoryBudgets.toList()) { (category, limit) ->
                            val spent = dashboardState.categorySpends.find { it.category == category }?.amount ?: 0.0
                            val isOver = spent > limit && limit > 0
                            val isWarning = !isOver && limit > 0 && (spent / limit) >= 0.80
                            val progress = if (limit > 0) (spent / limit).coerceIn(0.0, 1.0).toFloat() else 0f
                            val percent = if (limit > 0) ((spent / limit) * 100).toInt() else 0

                            CyberCard(
                                modifier = Modifier.fillMaxWidth(),
                                borderColor = if (isOver) CyberRed.copy(alpha = 0.7f) else if (isWarning) EmberOrange.copy(alpha = 0.5f) else WarmBorderSubtle,
                                backgroundColor = WarmSurfaceElevated,
                                cornerCut = 12.dp
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
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
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GhostSilver
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            if (isOver) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(CyberRed.copy(alpha = 0.15f))
                                                        .border(BorderStroke(0.5.dp, CyberRed), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                                ) {
                                                    Text("OVER BY ₹${(spent - limit).toInt()}", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyberRed)
                                                }
                                            } else if (isWarning) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(EmberOrange.copy(alpha = 0.15f))
                                                        .border(BorderStroke(0.5.dp, EmberOrange), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                                ) {
                                                    Text("80%+ USED", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = EmberOrange)
                                                }
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "₹${spent.toInt()} / ₹${limit.toInt()}",
                                                fontFamily = FontFamily.SansSerif,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isOver) CyberRed else if (isWarning) EmberOrange else GhostSilver
                                            )

                                            IconButton(
                                                onClick = {
                                                    if (editingCategory == category) {
                                                        editingCategory = null
                                                    } else {
                                                        editingCategory = category
                                                        editAmountText = limit.toInt().toString()
                                                    }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit",
                                                    tint = EmberOrange,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Progress bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(WarmTrackBackground)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction = progress)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(if (isOver) CyberRed else if (isWarning) EmberOrange else AcidLime)
                                        )
                                    }

                                    // Progress and Remaining stats
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "$percent% utilized",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.SansSerif,
                                            color = GhostSilverMuted
                                        )
                                        Text(
                                            text = if (isOver) "₹${(spent - limit).toInt()} over limit" else "₹${(limit - spent).toInt()} left",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isOver) CyberRed else GhostSilverMuted
                                        )
                                    }

                                    // Inline editor if active
                                    if (editingCategory == category) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = editAmountText,
                                                onValueChange = { editAmountText = it.filter { ch -> ch.isDigit() } },
                                                label = { Text("New Limit ₹", fontSize = 10.sp, color = GhostSilverMuted) },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = EmberOrange,
                                                    unfocusedBorderColor = WarmBorder,
                                                    focusedTextColor = GhostSilver,
                                                    unfocusedTextColor = GhostSilver
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                            Button(
                                                onClick = {
                                                    val newAmt = editAmountText.toDoubleOrNull() ?: limit
                                                    viewModel.updateCategoryBudget(category, newAmt)
                                                    editingCategory = null
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = EmberOrange, contentColor = VoidBlack),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(40.dp)
                                            ) {
                                                Text("SAVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = VoidBlack)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Reset to defaults action
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = { viewModel.resetCategoryBudgetsToDefaults() },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = GhostSilverMuted, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("RESTORE DEFAULT ALLOCATIONS", fontSize = 11.sp, color = GhostSilverMuted)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.closeModal() },
                        colors = ButtonDefaults.buttonColors(containerColor = EmberOrange, contentColor = VoidBlack),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("btn_close_category_budgets")
                    ) {
                        Text(
                            text = "APPLY & RETURN",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = VoidBlack
                        )
                    }
                }
            }
        }
    }
}
