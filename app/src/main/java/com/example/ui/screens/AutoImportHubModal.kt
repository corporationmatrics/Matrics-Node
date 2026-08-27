package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BankSmsParser
import com.example.data.EmailInvoiceParser
import com.example.data.ParsedBankSms
import com.example.data.ParsedEmailInvoice
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberCard
import com.example.ui.theme.AcidLime
import com.example.ui.theme.CyberRed
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.EmberPeach
import com.example.ui.theme.GhostSilver
import com.example.ui.theme.GhostSilverMuted
import com.example.ui.theme.ScannerCyan
import com.example.ui.theme.VoidBlack
import com.example.ui.theme.WarmBorder
import com.example.ui.theme.WarmBorderSubtle
import com.example.ui.theme.WarmCard
import com.example.ui.theme.WarmSurfaceElevated
import com.example.ui.theme.WarmTrackBackground
import com.example.ui.theme.cyphrColors
import com.example.ui.theme.getCategoryColor
import com.example.ui.viewmodel.CyphrViewModel

@Composable
fun AutoImportHubModal(
    viewModel: CyphrViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Bank SMS, 1: Email Invoices
    val colors = MaterialTheme.cyphrColors

    Dialog(
        onDismissRequest = { viewModel.closeModal() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VoidBlack.copy(alpha = 0.92f))
                .padding(horizontal = 14.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .testTag("modal_auto_import_hub"),
                borderColor = WarmBorder,
                backgroundColor = WarmCard,
                cornerCut = 28.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(EmberOrange.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Default.Message else Icons.Default.Email,
                                    contentDescription = null,
                                    tint = EmberOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AUTO-IMPORT HUB",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = GhostSilver
                                )
                                Text(
                                    text = "Bank SMS & Zepto/Zomato Email Invoices",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 11.sp,
                                    color = GhostSilverMuted
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.closeModal() },
                            modifier = Modifier.size(30.dp).testTag("btn_close_auto_import")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = GhostSilver)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Segmented Tabs: [ 📱 BANK SMS ] [ ✉️ EMAIL INVOICES ]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(WarmSurfaceElevated)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selectedTab == 0) EmberOrange else WarmTrackBackground)
                                .clickable { selectedTab = 0 }
                                .padding(vertical = 8.dp)
                                .testTag("tab_bank_sms"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = if (selectedTab == 0) VoidBlack else GhostSilver,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "BANK SMS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = if (selectedTab == 0) VoidBlack else GhostSilver
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selectedTab == 1) EmberOrange else WarmTrackBackground)
                                .clickable { selectedTab = 1 }
                                .padding(vertical = 8.dp)
                                .testTag("tab_email_invoices"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = if (selectedTab == 1) VoidBlack else GhostSilver,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "EMAIL INVOICES",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = if (selectedTab == 1) VoidBlack else GhostSilver
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (selectedTab == 0) {
                        BankSmsParserTab(viewModel = viewModel)
                    } else {
                        EmailInvoiceParserTab(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun BankSmsParserTab(viewModel: CyphrViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var smsInputText by remember { mutableStateOf("") }
    var parsedResult by remember { mutableStateOf<ParsedBankSms?>(null) }
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasSmsPermission = granted
        if (granted) {
            viewModel.showToast("✅ SMS Auto-Capture Activated!")
        } else {
            viewModel.showToast("⚠️ SMS Permission needed for automatic background capture")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // SMS Listener Status Card
        item {
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (hasSmsPermission) AcidLime.copy(alpha = 0.5f) else EmberPeach.copy(alpha = 0.5f),
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
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hasSmsPermission) Icons.Default.CheckCircle else Icons.Default.Security,
                            contentDescription = null,
                            tint = if (hasSmsPermission) AcidLime else EmberPeach,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (hasSmsPermission) "SMS Auto-Capture Active" else "SMS Permission Required",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GhostSilver
                            )
                            Text(
                                text = if (hasSmsPermission) 
                                    "Background receiver auto-detects HDFC, SBI, ICICI, Axis, Kotak debits"
                                else 
                                    "Grant permission to auto-capture bank debit SMS in real-time",
                                fontSize = 10.sp,
                                color = GhostSilverMuted
                            )
                        }
                    }

                    if (!hasSmsPermission) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.RECEIVE_SMS) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmberPeach,
                                contentColor = VoidBlack
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp).testTag("btn_grant_sms_permission"),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("ENABLE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = VoidBlack)
                        }
                    }
                }
            }
        }

        // Quick Bank SMS Samples Chips
        item {
            Column {
                Text(
                    text = "ONE-TAP TEST SAMPLES (INDIAN BANKS)",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = GhostSilverMuted
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BankSmsParser.SAMPLES.forEach { sample ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(WarmSurfaceElevated)
                                .border(1.dp, WarmBorderSubtle, RoundedCornerShape(10.dp))
                                .clickable {
                                    smsInputText = sample.body
                                    parsedResult = BankSmsParser.parse(sample.sender, sample.body)
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                                .testTag("btn_sample_sms_${sample.bankName.lowercase().take(4)}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FlashOn, contentDescription = null, tint = EmberOrange, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(sample.label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = GhostSilver)
                            }
                        }
                    }
                }
            }
        }

        // Manual SMS Input Box
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PASTE BANK TRANSACTION SMS",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = GhostSilverMuted
                    )

                    Row(
                        modifier = Modifier.clickable {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                smsInputText = clip
                                parsedResult = BankSmsParser.parse(null, clip)
                            }
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = EmberOrange, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PASTE CLIPBOARD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmberOrange)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = smsInputText,
                    onValueChange = {
                        smsInputText = it
                        parsedResult = if (it.isNotBlank()) BankSmsParser.parse(null, it) else null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(95.dp)
                        .testTag("input_bank_sms"),
                    placeholder = {
                        Text(
                            "e.g. Alert! You've spent Rs. 450.00 on your SBI card at STARBUCKS on 20-Aug. Ref 904812",
                            fontSize = 11.sp,
                            color = GhostSilverMuted.copy(alpha = 0.5f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = WarmSurfaceElevated,
                        unfocusedContainerColor = WarmSurfaceElevated,
                        focusedBorderColor = EmberOrange,
                        unfocusedBorderColor = WarmBorderSubtle,
                        focusedTextColor = GhostSilver,
                        unfocusedTextColor = GhostSilver
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Parsed Result Card
        parsedResult?.let { res ->
            item {
                CyberCard(
                    modifier = Modifier.fillMaxWidth().testTag("card_sms_parsed_result"),
                    borderColor = AcidLime.copy(alpha = 0.8f),
                    backgroundColor = WarmSurfaceElevated,
                    cornerCut = 16.dp
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
                                CyberBadge(
                                    text = res.bankName.uppercase(),
                                    color = AcidLime,
                                    backgroundColor = WarmCard
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                CyberBadge(
                                    text = res.txnType,
                                    color = EmberOrange,
                                    backgroundColor = WarmCard
                                )
                            }

                            CyberBadge(
                                text = "CONFIDENCE: ${res.confidence}",
                                color = ScannerCyan,
                                backgroundColor = WarmCard
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = res.merchant,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = GhostSilver
                                )
                                Text(
                                    text = "Account: ${res.accountLast4}  •  Ref: ${res.utrOrRef.take(12)}",
                                    fontSize = 11.sp,
                                    color = GhostSilverMuted
                                )
                            }

                            Text(
                                text = "₹${res.amount.toInt()}",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = EmberOrange
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(getCategoryColor(res.category).copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Category: ${res.category}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = getCategoryColor(res.category)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                viewModel.logParsedBankSms(res)
                                viewModel.closeModal()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AcidLime,
                                contentColor = VoidBlack
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_confirm_log_sms")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VoidBlack, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CONFIRM & LOG EXPENSE (₹${res.amount.toInt()})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = VoidBlack
                            )
                        }
                    }
                }
            }
        } ?: item {
            if (smsInputText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(WarmSurfaceElevated)
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚠️ Could not detect a valid debit amount or merchant in the pasted text. Please verify format.",
                        fontSize = 11.sp,
                        color = EmberPeach
                    )
                }
            }
        }
    }
}

@Composable
private fun EmailInvoiceParserTab(viewModel: CyphrViewModel) {
    val clipboardManager = LocalClipboardManager.current
    var emailInputText by remember { mutableStateOf("") }
    var parsedInvoice by remember { mutableStateOf<ParsedEmailInvoice?>(null) }
    var autoRestockPantry by remember { mutableStateOf(true) }
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // App Templates Bar
        item {
            Column {
                Text(
                    text = "SELECT APP INVOICE TEMPLATE",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = GhostSilverMuted
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EmailInvoiceParser.SAMPLES.forEach { sample ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(WarmSurfaceElevated)
                                .border(1.dp, WarmBorderSubtle, RoundedCornerShape(10.dp))
                                .clickable {
                                    emailInputText = sample.sampleBody
                                    parsedInvoice = EmailInvoiceParser.parse(sample.sampleBody, sample.appName)
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                                .testTag("btn_sample_email_${sample.appName.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = EmberPeach, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(sample.title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = GhostSilver)
                            }
                        }
                    }
                }
            }
        }

        // Email Invoice Body Input
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PASTE INVOICE EMAIL CONTENT / FORWARD",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = GhostSilverMuted
                    )

                    Row(
                        modifier = Modifier.clickable {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                emailInputText = clip
                                parsedInvoice = EmailInvoiceParser.parse(clip)
                            }
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = EmberOrange, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PASTE CLIPBOARD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmberOrange)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = emailInputText,
                    onValueChange = {
                        emailInputText = it
                        if (it.isNotBlank()) {
                            parsedInvoice = EmailInvoiceParser.parse(it)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("input_email_invoice"),
                    placeholder = {
                        Text(
                            "Paste invoice email from Zepto, Zomato, Swiggy, Blinkit, Amazon...",
                            fontSize = 11.sp,
                            color = GhostSilverMuted.copy(alpha = 0.5f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = WarmSurfaceElevated,
                        unfocusedContainerColor = WarmSurfaceElevated,
                        focusedBorderColor = EmberOrange,
                        unfocusedBorderColor = WarmBorderSubtle,
                        focusedTextColor = GhostSilver,
                        unfocusedTextColor = GhostSilver
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            if (emailInputText.isNotBlank()) {
                                viewModel.parseEmailInvoiceWithGemini(
                                    emailText = emailInputText,
                                    onSuccess = { parsedInvoice = it },
                                    onError = { parsedInvoice = EmailInvoiceParser.parse(emailInputText) }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarmSurfaceElevated,
                            contentColor = ScannerCyan
                        ),
                        border = BorderStroke(1.dp, ScannerCyan.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp).testTag("btn_gemini_deep_parse"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        enabled = !isAiLoading
                    ) {
                        if (isAiLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = ScannerCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI Analyzing...", fontSize = 10.sp, color = ScannerCyan)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ScannerCyan, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("AI DEEP PARSE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ScannerCyan)
                        }
                    }
                }
            }
        }

        // Extracted Invoice Preview Table
        parsedInvoice?.let { inv ->
            item {
                CyberCard(
                    modifier = Modifier.fillMaxWidth().testTag("card_invoice_parsed_result"),
                    borderColor = EmberOrange.copy(alpha = 0.8f),
                    backgroundColor = WarmSurfaceElevated,
                    cornerCut = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Invoice Header Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CyberBadge(
                                    text = inv.merchant.uppercase(),
                                    color = EmberOrange,
                                    backgroundColor = WarmCard
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                CyberBadge(
                                    text = inv.paymentMethod,
                                    color = GhostSilver,
                                    backgroundColor = WarmCard
                                )
                            }

                            Text(
                                text = "₹${inv.totalAmount.toInt()}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = EmberOrange
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Order ID: ${inv.orderId}  •  ${inv.orderDate}",
                            fontSize = 10.sp,
                            color = GhostSilverMuted
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = WarmBorderSubtle, thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Itemized Breakdown List
                        Text(
                            text = "EXTRACTED LINE ITEMS (${inv.items.size})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = GhostSilverMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        inv.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = GhostSilver
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Qty: ${item.quantity.toInt()} (${item.unit})  •  ",
                                            fontSize = 10.sp,
                                            color = GhostSilverMuted
                                        )
                                        Text(
                                            text = item.category,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = getCategoryColor(item.category)
                                        )
                                    }
                                }

                                Text(
                                    text = "₹${(item.price * item.quantity).toInt()}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GhostSilver
                                )
                            }
                        }

                        if (inv.isGrocery) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = WarmBorderSubtle, thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Inventory,
                                        contentDescription = null,
                                        tint = AcidLime,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Auto-Restock Pantry",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GhostSilver
                                        )
                                        Text(
                                            text = "Track item shelf-life & expiry days",
                                            fontSize = 9.5.sp,
                                            color = GhostSilverMuted
                                        )
                                    }
                                }

                                Switch(
                                    checked = autoRestockPantry,
                                    onCheckedChange = { autoRestockPantry = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = VoidBlack,
                                        checkedTrackColor = AcidLime,
                                        uncheckedThumbColor = GhostSilverMuted,
                                        uncheckedTrackColor = WarmTrackBackground
                                    ),
                                    modifier = Modifier.testTag("switch_auto_restock_pantry")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                viewModel.logParsedEmailInvoice(inv, autoRestockPantry = autoRestockPantry)
                                viewModel.closeModal()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmberOrange,
                                contentColor = VoidBlack
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_confirm_log_email_invoice")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VoidBlack, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "IMPORT ${inv.items.size} ITEMS (₹${inv.totalAmount.toInt()})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = VoidBlack
                            )
                        }
                    }
                }
            }
        }
    }
}
