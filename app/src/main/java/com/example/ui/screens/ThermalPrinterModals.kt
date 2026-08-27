package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.printer.DiscoveredPrinterDevice
import com.example.data.printer.EscPosThermalPrinterEngine
import com.example.data.printer.PrinterConnectionType
import com.example.data.printer.ThermalPaperSize
import com.example.ui.components.CyberCard
import com.example.ui.theme.AcidLime
import com.example.ui.theme.CyberRed
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.VoidBlack
import com.example.ui.theme.cyphrColors
import com.example.ui.viewmodel.CyphrViewModel

/**
 * Container Composable for Thermal Receipt Preview and Configuration Modals.
 */
@Composable
fun ThermalPrinterModals(viewModel: CyphrViewModel) {
    val receiptState by viewModel.thermalReceiptModalState.collectAsState()
    val isSettingsOpen by viewModel.isThermalPrinterSettingsOpen.collectAsState()

    if (receiptState.isVisible && receiptState.receiptData != null) {
        ThermalReceiptModal(viewModel = viewModel)
    }

    if (isSettingsOpen) {
        ThermalPrinterSettingsSheet(viewModel = viewModel)
    }
}

/**
 * High-fidelity Thermal Paper Receipt Preview Modal with ESC/POS print and share actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalReceiptModal(viewModel: CyphrViewModel) {
    val colors = MaterialTheme.cyphrColors
    val receiptState by viewModel.thermalReceiptModalState.collectAsState()
    val config by viewModel.thermalPrinterConfig.collectAsState()
    val status by viewModel.thermalPrinterStatus.collectAsState()
    val clipboard = LocalClipboardManager.current
    val receipt = receiptState.receiptData ?: return

    ModalBottomSheet(
        onDismissRequest = { viewModel.closeReceiptPreview() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.warmCanvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Thermal Printer",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "THERMAL RECEIPT",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.openThermalPrinterSettings() },
                        modifier = Modifier.size(32.dp).testTag("receipt_open_printer_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Printer Settings",
                            tint = colors.steelGrey,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.closeReceiptPreview() },
                        modifier = Modifier.size(32.dp).testTag("receipt_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.steelGrey,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Printer Connection Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.warmCard)
                    .border(1.dp, colors.warmBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (config.connectionType == PrinterConnectionType.BLUETOOTH) Icons.Default.Bluetooth else Icons.Default.Wifi,
                            contentDescription = "Device",
                            tint = if (status.isConnected) AcidLime else NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (config.targetAddress.isNotBlank()) status.connectedDeviceName else "Configure Printer (${config.connectionType.label})",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            color = colors.ghostSilver
                        )
                    }

                    // Paper Width Switcher
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val is58 = config.paperSize == ThermalPaperSize.SIZE_58MM
                        Text(
                            text = "58mm",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = if (is58) FontWeight.Bold else FontWeight.Normal,
                            color = if (is58) NeonCyan else colors.steelGrey,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (is58) NeonCyan.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { viewModel.updateThermalPaperSize(ThermalPaperSize.SIZE_58MM) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Text(
                            text = "80mm",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = if (!is58) FontWeight.Bold else FontWeight.Normal,
                            color = if (!is58) NeonCyan else colors.steelGrey,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (!is58) NeonCyan.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { viewModel.updateThermalPaperSize(ThermalPaperSize.SIZE_80MM) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Realistic Thermal Paper Card with Serrated Look
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF9F9F6)) // Warm thermal roll paper off-white
                    .border(1.dp, Color(0xFFD6D6CC), RoundedCornerShape(6.dp))
                    .padding(14.dp)
                    .testTag("thermal_paper_receipt_card")
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Raw Monospace Thermal Output
                    Text(
                        text = receiptState.rawAsciiPreview,
                        fontFamily = FontFamily.Monospace,
                        fontSize = if (config.paperSize == ThermalPaperSize.SIZE_58MM) 11.5.sp else 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E1E1E), // Ink black
                        lineHeight = 14.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Simulated UPI QR Code Box
                    if (config.includeQrCode && !receipt.upiPaymentUri.isNullOrBlank()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White)
                                .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "QR Code",
                                tint = Color.Black,
                                modifier = Modifier.size(54.dp)
                            )
                            Text(
                                text = "Scan & Pay: ₹${receipt.netTotal.toInt()}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tear off serration indicator
                    Text(
                        text = "- - - - - - - - - - [ TEAR HERE ] - - - - - - - - - -",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Primary Print Thermal Button
                Button(
                    onClick = { viewModel.printThermalReceiptDirect(receipt) },
                    enabled = !receiptState.isPrinting,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                        .testTag("thermal_print_execute_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = VoidBlack
                    )
                ) {
                    if (receiptState.isPrinting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = VoidBlack,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PRINTING...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PRINT RECEIPT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Share / WhatsApp Button
                OutlinedButton(
                    onClick = { viewModel.shareReceiptViaIntent(receipt) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("thermal_share_receipt_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AcidLime
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(AcidLime))
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SHARE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Copy Text Button
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(receiptState.rawAsciiPreview))
                        viewModel.showToast("Receipt copied to clipboard")
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("thermal_copy_text_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.ghostSilver
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Text",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Error alert if print failed
            if (receiptState.errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "⚠️ ${receiptState.errorMessage}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = CyberRed,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Thermal Printer Hardware Settings Sheet.
 * Configures Bluetooth devices, Network IP, Paper Width, Store Header, Auto-Cut, and Test Prints.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalPrinterSettingsSheet(viewModel: CyphrViewModel) {
    val colors = MaterialTheme.cyphrColors
    val config by viewModel.thermalPrinterConfig.collectAsState()
    val status by viewModel.thermalPrinterStatus.collectAsState()
    val pairedDevices by viewModel.pairedThermalPrinters.collectAsState()

    var storeName by remember(config.storeHeader.storeName) { mutableStateOf(config.storeHeader.storeName) }
    var tagline by remember(config.storeHeader.tagline) { mutableStateOf(config.storeHeader.tagline) }
    var address by remember(config.storeHeader.address) { mutableStateOf(config.storeHeader.address) }
    var phone by remember(config.storeHeader.phone) { mutableStateOf(config.storeHeader.phone) }
    var gstin by remember(config.storeHeader.gstin) { mutableStateOf(config.storeHeader.gstin) }
    var networkIp by remember(config.targetAddress) { mutableStateOf(config.targetAddress) }
    var showHeaderEditor by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = { viewModel.closeThermalPrinterSettings() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.warmCanvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Title Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "THERMAL PRINTER CONFIG",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(
                    onClick = { viewModel.closeThermalPrinterSettings() },
                    modifier = Modifier.size(32.dp).testTag("printer_settings_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.steelGrey,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Connection Interface Selector
            Text(
                text = "CONNECTION PROTOCOL",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.steelGrey
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PrinterConnectionType.values().forEach { type ->
                    val isSelected = config.connectionType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else colors.warmCard)
                            .border(1.dp, if (isSelected) NeonCyan else colors.warmBorder, RoundedCornerShape(8.dp))
                            .clickable { viewModel.updateThermalPrinterConfig(config.copy(connectionType = type)) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (type) {
                                PrinterConnectionType.BLUETOOTH -> "Bluetooth"
                                PrinterConnectionType.NETWORK_IP -> "Wi-Fi / IP"
                                PrinterConnectionType.SYSTEM_PRINT -> "Android Spool"
                                PrinterConnectionType.PREVIEW_ONLY -> "Virtual"
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) NeonCyan else colors.ghostSilverMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Bluetooth Paired Devices or IP Configuration
            if (config.connectionType == PrinterConnectionType.BLUETOOTH) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PAIRED BLUETOOTH PRINTERS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.steelGrey
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { viewModel.refreshPairedThermalPrinters() }
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "REFRESH",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = NeonCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (pairedDevices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.warmCard)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No paired Bluetooth printers found.\nPlease pair your POS printer (Everycom, RETSOL, TVS, Epson) in Android Bluetooth settings.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = colors.ghostSilverMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        pairedDevices.forEach { dev ->
                            val isChosen = config.targetAddress.equals(dev.address, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isChosen) NeonCyan.copy(alpha = 0.12f) else colors.warmCard)
                                    .border(1.dp, if (isChosen) NeonCyan else colors.warmBorder, RoundedCornerShape(10.dp))
                                    .clickable { viewModel.selectThermalPrinter(dev) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("printer_device_${dev.address}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Bluetooth,
                                            contentDescription = "Bluetooth",
                                            tint = if (isChosen) NeonCyan else colors.steelGrey,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = dev.name,
                                                fontFamily = FontFamily.SansSerif,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.ghostSilver
                                            )
                                            Text(
                                                text = dev.address,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                color = colors.ghostSilverMuted
                                            )
                                        }
                                    }

                                    if (isChosen) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (config.connectionType == PrinterConnectionType.NETWORK_IP) {
                Text(
                    text = "PRINTER NETWORK IP ADDRESS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.steelGrey
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = networkIp,
                    onValueChange = {
                        networkIp = it
                        viewModel.updateThermalPrinterConfig(config.copy(targetAddress = it))
                    },
                    placeholder = { Text("e.g. 192.168.1.100") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = colors.warmBorder,
                        focusedContainerColor = colors.warmCard,
                        unfocusedContainerColor = colors.warmCard,
                        focusedTextColor = colors.ghostSilver,
                        unfocusedTextColor = colors.ghostSilver
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Paper Width Selection (58mm vs 80mm)
            Text(
                text = "PAPER ROLL WIDTH",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.steelGrey
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    ThermalPaperSize.SIZE_58MM to "58mm (2-Inch / 32 Cols)",
                    ThermalPaperSize.SIZE_80MM to "80mm (3-Inch / 48 Cols)"
                ).forEach { (size, label) ->
                    val isSelected = config.paperSize == size
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else colors.warmCard)
                            .border(1.dp, if (isSelected) NeonCyan else colors.warmBorder, RoundedCornerShape(8.dp))
                            .clickable { viewModel.updateThermalPaperSize(size) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) NeonCyan else colors.ghostSilverMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Hardware Print Preferences (Auto-Print, UPI QR, Barcode, Auto-Cut)
            Text(
                text = "PRINTING PREFERENCES",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.steelGrey
            )
            Spacer(modifier = Modifier.height(6.dp))

            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = colors.warmBorder,
                backgroundColor = colors.warmCard,
                cornerCut = 8.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Auto-print on POS checkout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Auto-Print on POS Sale",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.ghostSilver
                            )
                            Text(
                                text = "Immediately dispatches receipt on checkout confirmation",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 10.5.sp,
                                color = colors.ghostSilverMuted
                            )
                        }

                        Switch(
                            checked = config.autoPrintOnSale,
                            onCheckedChange = { viewModel.setAutoPrintOnSale(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = VoidBlack,
                                checkedTrackColor = NeonCyan
                            )
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = colors.warmBorder)

                    // Dynamic UPI QR on Bill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Dynamic UPI QR on Receipt",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.ghostSilver
                            )
                            Text(
                                text = "Prints scan-to-pay QR code for customer on receipt",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 10.5.sp,
                                color = colors.ghostSilverMuted
                            )
                        }

                        Switch(
                            checked = config.includeQrCode,
                            onCheckedChange = { viewModel.updateThermalPrinterConfig(config.copy(includeQrCode = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = VoidBlack,
                                checkedTrackColor = NeonCyan
                            )
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = colors.warmBorder)

                    // Auto Paper Cut
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Auto Paper Cut (GS V)",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.ghostSilver
                            )
                            Text(
                                text = "Triggers motorized guillotine cutter after bill",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 10.5.sp,
                                color = colors.ghostSilverMuted
                            )
                        }

                        Switch(
                            checked = config.autoCutPaper,
                            onCheckedChange = { viewModel.updateThermalPrinterConfig(config.copy(autoCutPaper = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = VoidBlack,
                                checkedTrackColor = NeonCyan
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Store Receipt Header Customization (Store Name, GSTIN, Address, Phone)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STORE HEADER & TAX INFO",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.steelGrey
                )

                Text(
                    text = if (showHeaderEditor) "HIDE" else "CUSTOMIZE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.emberOrange,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { showHeaderEditor = !showHeaderEditor }
                        .padding(4.dp)
                )
            }

            if (showHeaderEditor) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Store Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tagline,
                        onValueChange = { tagline = it },
                        label = { Text("Tagline / Subheading") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Store Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Contact No") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = gstin,
                            onValueChange = { gstin = it },
                            label = { Text("GSTIN") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.updateThermalStoreHeader(
                                EscPosThermalPrinterEngine.StoreReceiptHeader(
                                    storeName = storeName,
                                    tagline = tagline,
                                    address = address,
                                    phone = phone,
                                    gstin = gstin
                                )
                            )
                            showHeaderEditor = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.emberOrange,
                            contentColor = VoidBlack
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "SAVE STORE HEADER",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 6. Diagnostics & Shift Z-Report Triggers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Test Print Button
                Button(
                    onClick = { viewModel.runThermalTestPrint() },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("printer_run_test_print_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = VoidBlack
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Test Print",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TEST PRINT",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Daily Shift Z-Report Button
                Button(
                    onClick = { viewModel.printDailyShiftZReport() },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(46.dp)
                        .testTag("printer_print_z_report_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AcidLime,
                        contentColor = VoidBlack
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = "Z-Report",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PRINT Z-REPORT",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
