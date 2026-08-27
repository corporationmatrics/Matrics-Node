package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CommodityEntity
import com.example.data.model.TransactionEntity
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberCard
import com.example.ui.components.GridBackgroundBox
import com.example.ui.theme.AcidLime
import com.example.ui.theme.CyberRed
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.VoidBlack
import com.example.ui.theme.cyphrColors
import com.example.ui.viewmodel.CyphrViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StorefrontDashboardScreen(
    viewModel: CyphrViewModel,
    onNavigateToPos: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToKhata: () -> Unit,
    onOpenVoiceHud: () -> Unit
) {
    val colors = MaterialTheme.cyphrColors
    val storefrontState by viewModel.storefrontDashboardState.collectAsStateWithLifecycle()
    val lowStockList by viewModel.lowStockCommodities.collectAsStateWithLifecycle()
    val khataNetBalance by viewModel.netKhataBalance.collectAsStateWithLifecycle()
    val totalYouWillGet by viewModel.totalYouWillGet.collectAsStateWithLifecycle()
    val allKhataEntries by viewModel.allKhataEntries.collectAsStateWithLifecycle()
    val allCustomerProfiles by viewModel.allCustomerProfiles.collectAsStateWithLifecycle()

    GridBackgroundBox {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Storefront Status
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AcidLime)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "STOREFRONT ONLINE • SECTOR 4",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AcidLime,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "Matrics Retail Node",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.ghostSilver
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.warmSurfaceElevated)
                                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .clickable { viewModel.openBarcodeScanner(com.example.ui.viewmodel.BarcodeScanMode.PRICE_CHECKER) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("storefront_scan_price_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Price Check",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SCAN",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.warmSurfaceElevated)
                                .border(1.dp, AcidLime.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .clickable { onOpenVoiceHud() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("storefront_voice_assistant_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Assistant",
                                    tint = AcidLime,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "VOICE HUD",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AcidLime
                                )
                            }
                        }
                    }
                }
            }

            // Primary Metrics Hero Card: Revenue & Profit
            item {
                CyberCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = NeonCyan.copy(alpha = 0.4f),
                    backgroundColor = colors.warmCard,
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
                            Text(
                                text = "TODAY'S STORE REVENUE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.ghostSilverMuted,
                                letterSpacing = 1.sp
                            )
                            CyberBadge(
                                text = "${storefrontState.todayOrdersCount} ORDERS",
                                color = NeonCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "₹${storefrontState.todayRevenue.toInt()}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )

                            if (storefrontState.todayMarginPercent > 0) {
                                Box(
                                    modifier = Modifier
                                        .padding(bottom = 6.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AcidLime.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "+${storefrontState.todayMarginPercent.toInt()}% Margin",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AcidLime
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Sub-metrics 3 columns
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Gross Profit",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 11.sp,
                                    color = colors.ghostSilverMuted
                                )
                                Text(
                                    text = "₹${storefrontState.todayGrossProfit.toInt()}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AcidLime
                                )
                            }

                            Column {
                                Text(
                                    text = "Avg Order Value",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 11.sp,
                                    color = colors.ghostSilverMuted
                                )
                                Text(
                                    text = "₹${storefrontState.averageOrderValue.toInt()}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.ghostSilver
                                )
                            }

                            Column {
                                Text(
                                    text = "Inventory Value",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 11.sp,
                                    color = colors.ghostSilverMuted
                                )
                                Text(
                                    text = "₹${(storefrontState.totalInventoryValue / 1000).toInt()}k",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.emberOrange
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions Grid (POS Hub, Inventory, Khata Ledger)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // New Sale / POS Card
                    CyberCard(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onNavigateToPos() }
                            .testTag("storefront_quick_pos_card"),
                        borderColor = NeonCyan.copy(alpha = 0.5f),
                        backgroundColor = colors.warmCard,
                        cornerCut = 16.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PointOfSale,
                                contentDescription = "POS",
                                tint = NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "New Sale / POS",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.ghostSilver
                            )
                            Text(
                                text = "Quick barcode & billing",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 10.5.sp,
                                color = colors.ghostSilverMuted
                            )
                        }
                    }

                    // Store Inventory Card
                    CyberCard(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onNavigateToInventory() }
                            .testTag("storefront_quick_inventory_card"),
                        borderColor = colors.warmBorder,
                        backgroundColor = colors.warmCard,
                        cornerCut = 16.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = "Inventory",
                                tint = colors.emberOrange,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Inventory Hub",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.ghostSilver
                            )
                            Text(
                                text = "${storefrontState.totalSkusCount} Master SKUs",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 10.5.sp,
                                color = colors.ghostSilverMuted
                            )
                        }
                    }
                }
            }

            // Quick Barcode & QR Scanner Banner
            item {
                CyberCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewModel.openBarcodeScanner(com.example.ui.viewmodel.BarcodeScanMode.POS_BILLING) }
                        .testTag("storefront_quick_barcode_scanner_card"),
                    borderColor = NeonCyan.copy(alpha = 0.4f),
                    backgroundColor = colors.warmCard,
                    cornerCut = 14.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NeonCyan.copy(alpha = 0.15f))
                                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan Barcode",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Camera Barcode & QR Scanner",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.ghostSilver
                                )
                                Text(
                                    text = "Realtime ML Kit scanning for POS & Stock",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 11.sp,
                                    color = colors.ghostSilverMuted
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Open Scanner",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Quick Thermal Receipt Printer & Daily Z-Report Hub
            item {
                CyberCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewModel.openThermalPrinterSettings() }
                        .testTag("storefront_thermal_printer_hub_card"),
                    borderColor = AcidLime.copy(alpha = 0.4f),
                    backgroundColor = colors.warmCard,
                    cornerCut = 14.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AcidLime.copy(alpha = 0.15f))
                                    .border(1.dp, AcidLime.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = "Thermal Printer",
                                    tint = AcidLime,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Thermal Printer & Z-Report",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.ghostSilver
                                )
                                Text(
                                    text = "Bluetooth ESC/POS 58/80mm, Bills & End-of-Day",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 11.sp,
                                    color = colors.ghostSilverMuted
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Open Printer Hub",
                            tint = AcidLime,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Customer Khata & Store Credit Hub
            item {
                CyberCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewModel.openModal("KHATA_SPLITTER") }
                        .testTag("storefront_khata_credit_hub_card"),
                    borderColor = EmberOrange.copy(alpha = 0.5f),
                    backgroundColor = colors.warmCard,
                    cornerCut = 14.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(EmberOrange.copy(alpha = 0.15f))
                                    .border(1.dp, EmberOrange.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = "Khata Credit Hub",
                                    tint = EmberOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Customer Khata & Credit",
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.ghostSilver
                                    )
                                    if (totalYouWillGet > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        CyberBadge(
                                            text = "₹${totalYouWillGet.toInt()} DUE",
                                            color = AcidLime,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                                Text(
                                    text = "WhatsApp reminders, partial payments & loyalty",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 11.sp,
                                    color = colors.ghostSilverMuted
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Open Khata",
                            tint = EmberOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Low Stock Alert Banner & Urgent Restock
            if (lowStockList.isNotEmpty()) {
                item {
                    CyberCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = CyberRed.copy(alpha = 0.5f),
                        backgroundColor = CyberRed.copy(alpha = 0.08f),
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = CyberRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "LOW STOCK WARNING (${lowStockList.size} ITEMS)",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberRed
                                    )
                                }

                                Text(
                                    text = "RESTOCK",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.emberOrange,
                                    modifier = Modifier.clickable { onNavigateToInventory() }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            lowStockList.take(3).forEach { item ->
                                LowStockRow(
                                    commodity = item,
                                    onQuickRestock = { viewModel.quickRestockCommodity(item.id, 20.0) }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }

            // Today's Sales Activity Stream
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TODAY'S SALES STREAM",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.ghostSilverMuted,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "VIEW ALL",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        modifier = Modifier.clickable { onNavigateToKhata() }
                    )
                }
            }

            if (storefrontState.todaySalesTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.warmCard)
                            .border(1.dp, colors.warmBorder, RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PointOfSale,
                                contentDescription = "No Sales",
                                tint = colors.steelGrey,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No sales recorded yet today",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.sp,
                                color = colors.ghostSilverMuted
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onNavigateToPos,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = VoidBlack
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "START FIRST SALE",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                items(storefrontState.todaySalesTransactions, key = { it.id }) { tx ->
                    TodaySaleItemRow(transaction = tx)
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun LowStockRow(
    commodity: CommodityEntity,
    onQuickRestock: () -> Unit
) {
    val colors = MaterialTheme.cyphrColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.warmSurfaceElevated)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = commodity.canonicalName,
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colors.ghostSilver
            )
            Text(
                text = "Stock: ${commodity.stockQuantity.toInt()} (Min: ${commodity.reorderThreshold.toInt()})",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = CyberRed
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(AcidLime.copy(alpha = 0.15f))
                .border(1.dp, AcidLime.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .clickable { onQuickRestock() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "+20 RESTOCK",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = AcidLime
            )
        }
    }
}

@Composable
private fun TodaySaleItemRow(
    transaction: TransactionEntity
) {
    val colors = MaterialTheme.cyphrColors
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(transaction.dateTimestamp) { timeFormat.format(Date(transaction.dateTimestamp)) }

    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = colors.warmBorder,
        backgroundColor = colors.warmCard,
        cornerCut = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = "Receipt",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = transaction.title,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.ghostSilver,
                        maxLines = 1
                    )
                    Text(
                        text = "$formattedTime • ${transaction.paymentMethod}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = colors.ghostSilverMuted
                    )
                }
            }

            Text(
                text = "₹${transaction.totalAmount.toInt()}",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
        }
    }
}
