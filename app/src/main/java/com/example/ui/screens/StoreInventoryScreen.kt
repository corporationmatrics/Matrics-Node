package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BatchEntity
import com.example.data.model.CommodityEntity
import com.example.data.model.PurchaseOrderEntity
import com.example.data.model.PurchaseOrderItemEntity
import com.example.data.model.StockMovementEntity
import com.example.data.model.SupplierEntity
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

enum class InventoryTabMode {
    CATALOG,
    PURCHASE_ORDERS,
    SUPPLIERS,
    STOCK_AUDIT,
    SCM_ANALYTICS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreInventoryScreen(
    viewModel: CyphrViewModel
) {
    val context = LocalContext.current
    val colors = MaterialTheme.cyphrColors

    // Core flows
    val commodities by viewModel.allCommodities.collectAsStateWithLifecycle()
    val lowStockCommodities by viewModel.lowStockCommodities.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val purchaseOrders by viewModel.allPurchaseOrders.collectAsStateWithLifecycle()
    val stockMovements by viewModel.allStockMovements.collectAsStateWithLifecycle()
    val batches by viewModel.allBatches.collectAsStateWithLifecycle()
    val scmAnalytics by viewModel.supplyChainAnalytics.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(InventoryTabMode.CATALOG) }

    // Search and Catalog Filters
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    // Modals state
    var showAddSkuDialog by remember { mutableStateOf(false) }
    var showAddBatchDialog by remember { mutableStateOf(false) }
    var selectedBatchCommodity by remember { mutableStateOf<CommodityEntity?>(null) }
    var editingCommodity by remember { mutableStateOf<CommodityEntity?>(null) }
    var adjustmentCommodity by remember { mutableStateOf<CommodityEntity?>(null) }

    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var editingSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var paySupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var payPo by remember { mutableStateOf<PurchaseOrderEntity?>(null) }

    var promoCommodity by remember { mutableStateOf<CommodityEntity?>(null) }
    var promoBatchId by remember { mutableStateOf<Long?>(null) }
    var promoSuggestedDiscount by remember { mutableStateOf(25) }

    var showCreatePoDialog by remember { mutableStateOf(false) }
    var grnPurchaseOrder by remember { mutableStateOf<PurchaseOrderEntity?>(null) }
    var grnPoItems by remember { mutableStateOf<List<PurchaseOrderItemEntity>>(emptyList()) }

    val categories = remember(commodities) {
        listOf("ALL") + commodities.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    val filteredList = remember(commodities, searchQuery, selectedCategory) {
        commodities.filter { item ->
            val matchQuery = searchQuery.isBlank() ||
                    item.canonicalName.contains(searchQuery, ignoreCase = true) ||
                    item.rawKey.contains(searchQuery, ignoreCase = true) ||
                    item.brand.contains(searchQuery, ignoreCase = true) ||
                    item.sku.contains(searchQuery, ignoreCase = true)

            val matchCategory = selectedCategory == null || selectedCategory == "ALL" ||
                    item.category.equals(selectedCategory, ignoreCase = true)

            matchQuery && matchCategory
        }
    }

    val totalValuation = remember(commodities) {
        commodities.sumOf { (if (it.sellingPrice > 0) it.sellingPrice else it.lastKnownPrice) * it.stockQuantity }
    }

    GridBackgroundBox {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "INVENTORY & SUPPLY CHAIN",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.emberOrange,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = when (activeTab) {
                            InventoryTabMode.CATALOG -> "Master SKU Matrix"
                            InventoryTabMode.PURCHASE_ORDERS -> "Purchase Orders (PO)"
                            InventoryTabMode.SUPPLIERS -> "Distributor Directory"
                            InventoryTabMode.STOCK_AUDIT -> "Stock Movement Ledger"
                            InventoryTabMode.SCM_ANALYTICS -> "Demand & Expiry Radar"
                        },
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.ghostSilver
                    )
                }

                // Action button based on active tab
                when (activeTab) {
                    InventoryTabMode.CATALOG -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { showAddBatchDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = VoidBlack
                                ),
                                modifier = Modifier.testTag("inventory_add_batch_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Batch",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "LOT",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { showAddSkuDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.emberOrange,
                                    contentColor = VoidBlack
                                ),
                                modifier = Modifier.testTag("inventory_add_sku_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add SKU",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "NEW SKU",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    InventoryTabMode.PURCHASE_ORDERS -> {
                        Button(
                            onClick = { showCreatePoDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = VoidBlack
                            ),
                            modifier = Modifier.testTag("create_po_header_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New PO",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "NEW PO",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    InventoryTabMode.SUPPLIERS -> {
                        Button(
                            onClick = {
                                editingSupplier = null
                                showAddSupplierDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.emberOrange,
                                contentColor = VoidBlack
                            ),
                            modifier = Modifier.testTag("add_supplier_header_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Vendor",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "VENDOR",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    else -> {}
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Segmented Tab Navigation Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Triple(InventoryTabMode.CATALOG, "CATALOG", Icons.Default.Inventory),
                    Triple(InventoryTabMode.PURCHASE_ORDERS, "PO ORDERS (${purchaseOrders.count { it.status != "RECEIVED_GRN" }})", Icons.Default.LocalShipping),
                    Triple(InventoryTabMode.SUPPLIERS, "DISTRIBUTORS (${suppliers.size})", Icons.Default.Business),
                    Triple(InventoryTabMode.STOCK_AUDIT, "AUDIT LEDGER", Icons.Default.History),
                    Triple(InventoryTabMode.SCM_ANALYTICS, "INTELLIGENCE", Icons.Default.Assessment)
                ).forEach { (mode, label, icon) ->
                    val isSelected = activeTab == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) colors.emberOrange.copy(alpha = 0.22f) else colors.warmCard)
                            .border(
                                BorderStroke(1.dp, if (isSelected) colors.emberOrange else colors.warmBorder),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { activeTab = mode }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) colors.emberOrange else colors.ghostSilverMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = label,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colors.emberOrange else colors.ghostSilverMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // TAB 1: MASTER SKU CATALOG
            if (activeTab == InventoryTabMode.CATALOG) {
                // Summary KPI Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.warmCard)
                            .border(1.dp, colors.warmBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = "TOTAL SKUS",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp,
                                color = colors.ghostSilverMuted
                            )
                            Text(
                                text = "${commodities.size}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.warmCard)
                            .border(1.dp, colors.warmBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = "LOW STOCK",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp,
                                color = colors.ghostSilverMuted
                            )
                            Text(
                                text = "${lowStockCommodities.size} Items",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (lowStockCommodities.isNotEmpty()) CyberRed else AcidLime
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.warmCard)
                            .border(1.dp, colors.warmBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = "STOCK VALUE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp,
                                color = colors.ghostSilverMuted
                            )
                            Text(
                                text = "₹${totalValuation.toInt()}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AcidLime
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar with Barcode Scanner trigger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("inventory_search_input"),
                        placeholder = {
                            Text(
                                text = "Search name, barcode, brand...",
                                fontSize = 12.sp,
                                color = colors.steelGrey
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = colors.steelGrey,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = colors.steelGrey,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.emberOrange,
                            unfocusedBorderColor = colors.warmBorder,
                            focusedContainerColor = colors.warmCard,
                            unfocusedContainerColor = colors.warmCard
                        )
                    )

                    IconButton(
                        onClick = { viewModel.openBarcodeScanner() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.warmCard)
                            .border(1.dp, colors.warmBorder, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Barcode",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Categories Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = (selectedCategory == null && cat == "ALL") || selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) colors.emberOrange.copy(alpha = 0.2f) else colors.warmCard)
                                .border(
                                    BorderStroke(1.dp, if (isSelected) colors.emberOrange else colors.warmBorder),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    selectedCategory = if (cat == "ALL") null else cat
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = cat,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) colors.emberOrange else colors.ghostSilverMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // SKU Cards List
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No commodities match the query.",
                            fontSize = 12.sp,
                            color = colors.steelGrey
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList, key = { it.id }) { comm ->
                            val isLowStock = comm.stockQuantity <= comm.reorderThreshold
                            val marginPercent = if (comm.sellingPrice > 0 && comm.costPrice > 0) {
                                ((comm.sellingPrice - comm.costPrice) / comm.sellingPrice) * 100.0
                            } else 0.0

                            CyberCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("commodity_card_${comm.id}"),
                                backgroundColor = colors.warmCard,
                                borderColor = if (isLowStock) CyberRed.copy(alpha = 0.6f) else colors.warmBorder,
                                cornerCut = 8.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = comm.canonicalName,
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.ghostSilver
                                                )

                                                if (comm.brand.isNotBlank()) {
                                                    CyberBadge(
                                                        text = comm.brand,
                                                        color = NeonCyan,
                                                        fontSize = 8.5.sp
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.padding(top = 2.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (comm.sku.isNotBlank()) {
                                                    Text(
                                                        text = "SKU: ${comm.sku}",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 9.5.sp,
                                                        color = colors.ghostSilverMuted
                                                    )
                                                }
                                                Text(
                                                    text = "• ${comm.category}",
                                                    fontSize = 10.sp,
                                                    color = colors.steelGrey
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { adjustmentCommodity = comm },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.History,
                                                    contentDescription = "Stock Adjustment",
                                                    tint = colors.emberOrange,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { editingCommodity = comm },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit",
                                                    tint = colors.steelGrey,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Pricing & Margins Matrix
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colors.warmSurfaceElevated)
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "MRP / SELL",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 8.5.sp,
                                                color = colors.ghostSilverMuted
                                            )
                                            Text(
                                                text = "₹${comm.sellingPrice.toInt()}",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AcidLime
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "INWARD COST",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 8.5.sp,
                                                color = colors.ghostSilverMuted
                                            )
                                            Text(
                                                text = if (comm.costPrice > 0) "₹${comm.costPrice.toInt()}" else "₹${comm.lastKnownPrice.toInt()}",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.ghostSilver
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "GROSS MARGIN",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 8.5.sp,
                                                color = colors.ghostSilverMuted
                                            )
                                            Text(
                                                text = "${marginPercent.toInt()}%",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (marginPercent >= 20.0) AcidLime else NeonAmber
                                            )
                                        }

                                        // Stock Quantity with Quick Stepper
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(colors.warmCard)
                                                    .border(1.dp, colors.warmBorder, RoundedCornerShape(4.dp))
                                                    .clickable {
                                                        viewModel.quickAdjustCommodityStock(comm.id, -1.0)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "-",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 13.sp,
                                                    color = CyberRed
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "${comm.stockQuantity.toInt()} ${comm.normalizedUnit}",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isLowStock) CyberRed else colors.ghostSilver
                                                )
                                                if (isLowStock) {
                                                    Text(
                                                        text = "LOW (<${comm.reorderThreshold.toInt()})",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 7.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CyberRed
                                                    )
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(colors.warmCard)
                                                    .border(1.dp, colors.warmBorder, RoundedCornerShape(4.dp))
                                                    .clickable {
                                                        viewModel.quickAdjustCommodityStock(comm.id, 1.0)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "+",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 13.sp,
                                                    color = AcidLime
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TAB 2: PURCHASE ORDERS & PROCUREMENT
            if (activeTab == InventoryTabMode.PURCHASE_ORDERS) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PURCHASE ORDERS PIPELINE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.ghostSilverMuted
                    )

                    // Auto-Replenish low stock button
                    if (lowStockCommodities.isNotEmpty() && suppliers.isNotEmpty()) {
                        Button(
                            onClick = {
                                suppliers.firstOrNull()?.let { sup ->
                                    viewModel.autoReplenishLowStockPO(sup)
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonAmber.copy(alpha = 0.2f),
                                contentColor = NeonAmber
                            ),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = "Auto Restock",
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "AUTO-REPLENISH LOW (${lowStockCommodities.size})",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (purchaseOrders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No purchase orders generated yet.",
                                fontSize = 12.sp,
                                color = colors.steelGrey
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showCreatePoDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = VoidBlack
                                )
                            ) {
                                Text("CREATE FIRST PURCHASE ORDER", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(purchaseOrders, key = { it.id }) { po ->
                            val isReceived = po.status == "RECEIVED_GRN"
                            val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(po.orderDateTimestamp))
                            val isPaid = po.paymentStatus == "PAID"
                            val deliveryStr = if (po.expectedDeliveryTimestamp > 0) {
                                SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(po.expectedDeliveryTimestamp))
                            } else "Standard"

                            CyberCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = colors.warmCard,
                                borderColor = if (isReceived) AcidLime.copy(alpha = 0.4f) else colors.warmBorder,
                                cornerCut = 8.dp
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
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = po.orderNumber,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.ghostSilver
                                                )

                                                CyberBadge(
                                                    text = po.status,
                                                    color = when (po.status) {
                                                        "RECEIVED_GRN" -> AcidLime
                                                        "IN_TRANSIT" -> NeonCyan
                                                        "ORDERED" -> NeonAmber
                                                        else -> colors.ghostSilverMuted
                                                    },
                                                    fontSize = 8.5.sp
                                                )

                                                CyberBadge(
                                                    text = if (isPaid) "PAID" else "UNPAID (AP)",
                                                    color = if (isPaid) AcidLime else CyberRed,
                                                    fontSize = 8.sp
                                                )
                                            }

                                            Text(
                                                text = "Distributor: ${po.supplierName} • Due: $deliveryStr",
                                                fontFamily = FontFamily.SansSerif,
                                                fontSize = 11.sp,
                                                color = colors.ghostSilverMuted
                                            )
                                        }

                                        Text(
                                            text = "₹${po.totalAmount.toInt()}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AcidLime
                                        )
                                    }

                                    if (po.shippingNotes.isNotBlank()) {
                                        Text(
                                            text = "Logistics Note: ${po.shippingNotes}",
                                            fontSize = 10.sp,
                                            color = colors.steelGrey,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Action Buttons: Share PO, Receive GRN, or Settle AP
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.sharePurchaseOrderViaWhatsApp(po, context)
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = colors.warmSurfaceElevated,
                                                contentColor = NeonCyan
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Share PO",
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "DISPATCH",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        if (!isPaid) {
                                            Button(
                                                onClick = { payPo = po },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = colors.emberOrange.copy(alpha = 0.2f),
                                                    contentColor = colors.emberOrange
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(34.dp)
                                            ) {
                                                Text(
                                                    text = "PAY PO",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        if (!isReceived) {
                                            Button(
                                                onClick = {
                                                    // Open GRN intake bottom sheet
                                                    grnPurchaseOrder = po
                                                    grnPoItems = commodities.take(3).map {
                                                        PurchaseOrderItemEntity(
                                                            purchaseOrderId = po.id,
                                                            commodityId = it.id,
                                                            itemName = it.canonicalName,
                                                            brand = it.brand,
                                                            orderedQuantity = 20.0,
                                                            unit = it.normalizedUnit,
                                                            unitCostPrice = if (it.costPrice > 0) it.costPrice else 50.0,
                                                            lineTotal = 1000.0
                                                        )
                                                    }
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = AcidLime,
                                                    contentColor = VoidBlack
                                                ),
                                                modifier = Modifier
                                                    .weight(1.2f)
                                                    .height(34.dp)
                                                    .testTag("receive_grn_button_${po.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Receive GRN",
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "GRN INTAKE",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(34.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(AcidLime.copy(alpha = 0.15f))
                                                    .border(1.dp, AcidLime.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "✓ STOCKED",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AcidLime
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TAB 3: SUPPLIERS & DISTRIBUTORS (ACCOUNTS PAYABLE DIRECTORY)
            if (activeTab == InventoryTabMode.SUPPLIERS) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Accounts Payable Summary Card
                    item {
                        CyberCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = colors.warmCard,
                            borderColor = if (scmAnalytics.totalAccountsPayable > 0) colors.emberOrange.copy(alpha = 0.5f) else colors.warmBorder,
                            cornerCut = 8.dp
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
                                        text = "TOTAL ACCOUNTS PAYABLE (AP)",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.ghostSilverMuted
                                    )
                                    Text(
                                        text = "₹${scmAnalytics.totalAccountsPayable.toInt()}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (scmAnalytics.totalAccountsPayable > 0) colors.emberOrange else AcidLime
                                    )
                                }
                                CyberBadge(
                                    text = "${suppliers.size} DISTRIBUTORS",
                                    color = NeonCyan,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }

                    if (suppliers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "No wholesale suppliers registered yet.",
                                        fontSize = 12.sp,
                                        color = colors.steelGrey
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            editingSupplier = null
                                            showAddSupplierDialog = true
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.emberOrange,
                                            contentColor = VoidBlack
                                        )
                                    ) {
                                        Text("REGISTER WHOLESALE DISTRIBUTOR", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        items(suppliers, key = { it.id }) { sup ->
                            CyberCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = colors.warmCard,
                                borderColor = colors.warmBorder,
                                cornerCut = 8.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = sup.name,
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.ghostSilver
                                                )

                                                CyberBadge(
                                                    text = "${sup.leadTimeDays}d Lead",
                                                    color = NeonCyan,
                                                    fontSize = 8.5.sp
                                                )

                                                if (sup.outstandingPayable > 0) {
                                                    CyberBadge(
                                                        text = "AP: ₹${sup.outstandingPayable.toInt()}",
                                                        color = colors.emberOrange,
                                                        fontSize = 8.5.sp
                                                    )
                                                }
                                            }

                                            if (sup.contactPerson.isNotBlank()) {
                                                Text(
                                                    text = "Contact: ${sup.contactPerson} • ${sup.paymentTerms}",
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontSize = 11.sp,
                                                    color = colors.ghostSilverMuted
                                                )
                                            }

                                            if (sup.gstin.isNotBlank()) {
                                                Text(
                                                    text = "GSTIN: ${sup.gstin}",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.5.sp,
                                                    color = colors.steelGrey
                                                )
                                            }
                                        }

                                        Row {
                                            IconButton(
                                                onClick = {
                                                    editingSupplier = sup
                                                    showAddSupplierDialog = true
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit",
                                                    tint = colors.steelGrey,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { viewModel.deleteSupplier(sup.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = CyberRed,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Quick Communication Action Bar
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (sup.phone.isNotBlank()) {
                                            Button(
                                                onClick = {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${sup.phone}"))
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {}
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = colors.warmSurfaceElevated,
                                                    contentColor = AcidLime
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Call,
                                                    contentDescription = "Call",
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "CALL",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        if (sup.outstandingPayable > 0) {
                                            Button(
                                                onClick = { paySupplier = sup },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = AcidLime.copy(alpha = 0.2f),
                                                    contentColor = AcidLime
                                                ),
                                                modifier = Modifier
                                                    .weight(1.1f)
                                                    .height(32.dp)
                                            ) {
                                                Text(
                                                    text = "PAY INVOICE",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.autoReplenishLowStockPO(sup)
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = colors.emberOrange.copy(alpha = 0.2f),
                                                contentColor = colors.emberOrange
                                            ),
                                            modifier = Modifier
                                                .weight(1.2f)
                                                .height(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocalShipping,
                                                contentDescription = "Reorder",
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "RAISE PO",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TAB 4: STOCK AUDIT LEDGER
            if (activeTab == InventoryTabMode.STOCK_AUDIT) {
                Text(
                    text = "IMMUTABLE STOCK MOVEMENT AUDIT LOG",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.ghostSilverMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (stockMovements.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No stock transactions recorded yet.",
                            fontSize = 12.sp,
                            color = colors.steelGrey
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(stockMovements, key = { it.id }) { mov ->
                            val isIncrement = mov.changeQuantity >= 0
                            val timeStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(mov.timestamp))

                            CyberCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = colors.warmCard,
                                borderColor = colors.warmBorder,
                                cornerCut = 6.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = mov.commodityName,
                                                fontFamily = FontFamily.SansSerif,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.ghostSilver
                                            )

                                            CyberBadge(
                                                text = mov.movementType,
                                                color = when (mov.movementType) {
                                                    "PURCHASE_GRN" -> AcidLime
                                                    "POS_SALE" -> CyberRed
                                                    "DAMAGED_WRITE_OFF" -> NeonAmber
                                                    else -> NeonCyan
                                                },
                                                fontSize = 7.5.sp
                                            )
                                        }

                                        Text(
                                            text = "$timeStr • Ref: ${mov.referenceId} ${if (mov.notes.isNotBlank()) "(${mov.notes})" else ""}",
                                            fontSize = 9.5.sp,
                                            color = colors.ghostSilverMuted,
                                            maxLines = 1
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${if (isIncrement) "+" else ""}${mov.changeQuantity.toInt()} ${mov.unit}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isIncrement) AcidLime else CyberRed
                                        )
                                        Text(
                                            text = "Bal: ${mov.newStock.toInt()}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            color = colors.steelGrey
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TAB 5: SCM INTELLIGENCE & DEMAND FORECASTING
            if (activeTab == InventoryTabMode.SCM_ANALYTICS) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // SCM KPI Radar
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.warmCard)
                                    .border(1.dp, colors.warmBorder, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("INVENTORY VALUE", fontFamily = FontFamily.Monospace, fontSize = 7.5.sp, color = colors.ghostSilverMuted)
                                    Text("₹${scmAnalytics.totalStockValuation.toInt()}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AcidLime)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.warmCard)
                                    .border(1.dp, colors.warmBorder, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("ACCOUNTS PAYABLE", fontFamily = FontFamily.Monospace, fontSize = 7.5.sp, color = colors.ghostSilverMuted)
                                    Text("₹${scmAnalytics.totalAccountsPayable.toInt()}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (scmAnalytics.totalAccountsPayable > 0) colors.emberOrange else AcidLime)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.warmCard)
                                    .border(1.dp, colors.warmBorder, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("DEAD CAPITAL", fontFamily = FontFamily.Monospace, fontSize = 7.5.sp, color = colors.ghostSilverMuted)
                                    Text("₹${scmAnalytics.totalDeadStockValuation.toInt()}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (scmAnalytics.totalDeadStockValuation > 0) CyberRed else AcidLime)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.warmCard)
                                    .border(1.dp, colors.warmBorder, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("HIGH VELOCITY", fontFamily = FontFamily.Monospace, fontSize = 7.5.sp, color = colors.ghostSilverMuted)
                                    Text("${scmAnalytics.highVelocityItems.size} SKUs", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                }
                            }
                        }
                    }

                    // 1. HIGH-VELOCITY INVENTORY HEROES
                    item {
                        CyberCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = colors.warmCard,
                            borderColor = if (scmAnalytics.highVelocityItems.isNotEmpty()) NeonCyan.copy(alpha = 0.5f) else colors.warmBorder,
                            cornerCut = 8.dp
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
                                    Text(
                                        text = "⚡ HIGH-VELOCITY INVENTORY HEROES",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                    Text(
                                        text = "Fast Movers (30d)",
                                        fontSize = 9.5.sp,
                                        color = colors.steelGrey
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (scmAnalytics.highVelocityItems.isEmpty()) {
                                    Text(
                                        text = "No high velocity items recorded in the last 30 days.",
                                        fontSize = 11.sp,
                                        color = colors.steelGrey
                                    )
                                } else {
                                    scmAnalytics.highVelocityItems.take(5).forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(colors.warmSurfaceElevated)
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.ghostSilver
                                                )
                                                Text(
                                                    text = "Sold: ${item.unitsSold30d.toInt()} ${item.unit} • Burn: ${String.format(Locale.getDefault(), "%.1f", item.dailyVelocity)}/day • Stock: ${item.currentStock.toInt()}",
                                                    fontSize = 10.sp,
                                                    color = colors.ghostSilverMuted
                                                )
                                            }

                                            Button(
                                                onClick = { showCreatePoDialog = true },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = NeonCyan.copy(alpha = 0.2f),
                                                    contentColor = NeonCyan
                                                ),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text(
                                                    text = "REORDER",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }

                    // 2. DEAD-STOCK & CAPITAL LOCKUP RADAR
                    item {
                        CyberCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = colors.warmCard,
                            borderColor = if (scmAnalytics.deadStockItems.isNotEmpty()) CyberRed.copy(alpha = 0.5f) else colors.warmBorder,
                            cornerCut = 8.dp
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
                                    Text(
                                        text = "⚠️ DEAD-STOCK & CAPITAL LOCKUP RADAR",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberRed
                                    )
                                    Text(
                                        text = "Zero Sales >30d",
                                        fontSize = 9.5.sp,
                                        color = CyberRed
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (scmAnalytics.deadStockItems.isEmpty()) {
                                    Text(
                                        text = "Healthy inventory turnover. No stagnant dead-stock detected.",
                                        fontSize = 11.sp,
                                        color = colors.steelGrey
                                    )
                                } else {
                                    scmAnalytics.deadStockItems.take(5).forEach { dead ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(colors.warmSurfaceElevated)
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = dead.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.ghostSilver
                                                )
                                                Text(
                                                    text = "Locked: ₹${dead.lockedCapital.toInt()} • In Stock: ${dead.currentStock.toInt()} ${dead.unit} • ${dead.daysSinceLastSale}d Inactive",
                                                    fontSize = 10.sp,
                                                    color = colors.ghostSilverMuted
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    val matchedComm = commodities.find { it.id == dead.commodityId }
                                                    if (matchedComm != null) {
                                                        promoCommodity = matchedComm
                                                        promoBatchId = null
                                                        promoSuggestedDiscount = 25
                                                    }
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = colors.emberOrange.copy(alpha = 0.2f),
                                                    contentColor = colors.emberOrange
                                                ),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text(
                                                    text = "CLEARANCE -25%",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }

                    // 3. FEFO PERISHABLE EXPIRY & SHELF-LIFE RADAR
                    item {
                        CyberCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = colors.warmCard,
                            borderColor = if (scmAnalytics.nearExpiryBatches.isNotEmpty()) NeonAmber.copy(alpha = 0.6f) else colors.warmBorder,
                            cornerCut = 8.dp
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
                                    Text(
                                        text = "🔴 FEFO EXPIRY & SHELF-LIFE RADAR",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonAmber
                                    )
                                    Text(
                                        text = "Batches <30d",
                                        fontSize = 9.5.sp,
                                        color = NeonAmber
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (scmAnalytics.nearExpiryBatches.isEmpty()) {
                                    Text(
                                        text = "No batch lots expiring within the next 30 days.",
                                        fontSize = 11.sp,
                                        color = colors.steelGrey
                                    )
                                } else {
                                    scmAnalytics.nearExpiryBatches.forEach { exp ->
                                        val daysUntilExpiry = ((exp.expiryDateTimestamp - System.currentTimeMillis()) / 86_400_000L)
                                        val isExpired = daysUntilExpiry <= 0

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(colors.warmSurfaceElevated)
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${exp.commodityName} (Lot: ${exp.batchNumber})",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.ghostSilver
                                                )
                                                Text(
                                                    text = "Qty: ${exp.quantity.toInt()} ${exp.unit} • ${if (isExpired) "EXPIRED" else "$daysUntilExpiry days left"}",
                                                    fontSize = 10.sp,
                                                    color = if (isExpired) CyberRed else NeonAmber
                                                )
                                            }

                                            if (isExpired) {
                                                Button(
                                                    onClick = {
                                                        viewModel.writeOffExpiredBatch(exp)
                                                    },
                                                    shape = RoundedCornerShape(6.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = CyberRed.copy(alpha = 0.2f),
                                                        contentColor = CyberRed
                                                    ),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text(
                                                        text = "DISCARD / WRITE-OFF",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            } else {
                                                Button(
                                                    onClick = {
                                                        val matchedComm = commodities.find { it.id == exp.commodityId }
                                                        if (matchedComm != null) {
                                                            promoCommodity = matchedComm
                                                            promoBatchId = exp.id
                                                            promoSuggestedDiscount = 30
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(6.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = NeonAmber.copy(alpha = 0.2f),
                                                        contentColor = NeonAmber
                                                    ),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text(
                                                        text = "MARKDOWN -30%",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }

                    // 4. ABC PARETO CLASSIFICATION
                    item {
                        CyberCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = colors.warmCard,
                            borderColor = colors.warmBorder,
                            cornerCut = 8.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "ABC PARETO CLASSIFICATION",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                    Text(
                                        text = "Revenue Contribution",
                                        fontSize = 10.sp,
                                        color = colors.steelGrey
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                scmAnalytics.abcAnalysisItems.take(5).forEach { abc ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            CyberBadge(
                                                text = "CLASS ${abc.classificationClass}",
                                                color = when (abc.classificationClass) {
                                                    "A" -> AcidLime
                                                    "B" -> NeonAmber
                                                    else -> colors.ghostSilverMuted
                                                },
                                                fontSize = 8.sp
                                            )
                                            Text(
                                                text = abc.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.ghostSilver
                                            )
                                        }

                                        Text(
                                            text = "₹${abc.totalRevenue.toInt()} (${abc.cumulativeSharePercent.toInt()}%)",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.ghostSilverMuted
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. STOCKOUT RISK RADAR & DAYS ON HAND (DOH)
                    item {
                        CyberCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = colors.warmCard,
                            borderColor = if (scmAnalytics.stockoutRiskItems.isNotEmpty()) CyberRed.copy(alpha = 0.5f) else colors.warmBorder,
                            cornerCut = 8.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "STOCKOUT RISK RADAR & DAYS ON HAND (DOH)",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (scmAnalytics.stockoutRiskItems.isNotEmpty()) CyberRed else AcidLime
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                if (scmAnalytics.stockoutRiskItems.isEmpty()) {
                                    Text(
                                        text = "All inventory items have healthy stock at current burn rates.",
                                        fontSize = 11.sp,
                                        color = colors.steelGrey
                                    )
                                } else {
                                    scmAnalytics.stockoutRiskItems.forEach { risk ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(colors.warmSurfaceElevated)
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = risk.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.ghostSilver
                                                )
                                                Text(
                                                    text = "Stock: ${risk.currentStock.toInt()} ${risk.unit} • Burn: ${String.format(Locale.getDefault(), "%.1f", risk.dailyBurnRate)}/day",
                                                    fontSize = 10.sp,
                                                    color = colors.ghostSilverMuted
                                                )
                                            }

                                            CyberBadge(
                                                text = "${risk.daysOfInventoryOnHand.toInt()} DAYS LEFT",
                                                color = CyberRed,
                                                fontSize = 9.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- BOTTOM SHEETS & DIALOGS ---

        // Add Manual Batch / Lot Modal
        if (showAddBatchDialog || selectedBatchCommodity != null) {
            AddBatchBottomSheet(
                commodities = commodities,
                suppliers = suppliers,
                initialCommodity = selectedBatchCommodity,
                onDismiss = {
                    showAddBatchDialog = false
                    selectedBatchCommodity = null
                },
                onSaveBatch = { commId, commName, bNo, qty, cost, sell, expDays, supName, unit ->
                    val expiryDateMs = System.currentTimeMillis() + (expDays * 86_400_000L)
                    viewModel.addCustomBatch(
                        commodityId = commId,
                        commodityName = commName,
                        batchNumber = bNo,
                        quantity = qty,
                        costPrice = cost,
                        sellingPrice = sell,
                        expiryDateMs = expiryDateMs,
                        supplierName = supName,
                        unit = unit
                    )
                }
            )
        }

        // Record Supplier Payment / Settle AP Modal
        if (paySupplier != null || payPo != null) {
            RecordSupplierPaymentBottomSheet(
                supplier = paySupplier,
                po = payPo,
                onDismiss = {
                    paySupplier = null
                    payPo = null
                },
                onConfirmPayment = { supId, poId, amount ->
                    viewModel.recordSupplierPayment(
                        supplierId = supId,
                        poId = poId,
                        amount = amount
                    )
                }
            )
        }

        // Promotional Markdown Clearance Modal
        promoCommodity?.let { comm ->
            PromotionalMarkdownBottomSheet(
                commodityName = comm.canonicalName,
                currentPrice = comm.sellingPrice,
                commodityId = comm.id,
                batchId = promoBatchId,
                suggestedDiscountPercent = promoSuggestedDiscount,
                onDismiss = {
                    promoCommodity = null
                    promoBatchId = null
                },
                onApplyMarkdown = { commId, discPercent, bId ->
                    viewModel.applyCommodityMarkdown(
                        commodityId = commId,
                        markdownPercent = discPercent,
                        batchId = bId
                    )
                }
            )
        }

        // Add / Edit Supplier Modal
        if (showAddSupplierDialog) {
            AddEditSupplierBottomSheet(
                supplier = editingSupplier,
                onDismiss = { showAddSupplierDialog = false },
                onSave = { name, contact, phone, email, address, gstin, paymentTerms, leadTime, notes ->
                    viewModel.saveSupplier(
                        id = editingSupplier?.id ?: 0L,
                        name = name,
                        contactPerson = contact,
                        phone = phone,
                        email = email,
                        address = address,
                        gstin = gstin,
                        paymentTerms = paymentTerms,
                        leadTimeDays = leadTime,
                        notes = notes
                    )
                }
            )
        }

        // Create Purchase Order Modal
        if (showCreatePoDialog) {
            CreatePurchaseOrderBottomSheet(
                suppliers = suppliers,
                commodities = commodities,
                onDismiss = { showCreatePoDialog = false },
                onCreatePO = { supplierId, supplierName, items, leadDays, notes ->
                    viewModel.createPurchaseOrder(
                        supplierId = supplierId,
                        supplierName = supplierName,
                        items = items,
                        expectedDeliveryDays = leadDays,
                        shippingNotes = notes
                    )
                }
            )
        }

        // Goods Received Note Intake Modal
        grnPurchaseOrder?.let { po ->
            ReceiveGrnBottomSheet(
                po = po,
                poItems = grnPoItems,
                onDismiss = { grnPurchaseOrder = null },
                onConfirmGRN = { poId, supplierName, items, grnNo ->
                    viewModel.receiveGoodsReceiptNote(poId, supplierName, items, grnNo)
                    grnPurchaseOrder = null
                }
            )
        }

        // Manual Stock Adjustment Modal
        adjustmentCommodity?.let { comm ->
            ManualStockAdjustmentBottomSheet(
                commodity = comm,
                onDismiss = { adjustmentCommodity = null },
                onConfirmAdjustment = { commodityId, changeQty, reason, notes ->
                    viewModel.performManualStockAdjustment(commodityId, changeQty, reason, notes)
                    adjustmentCommodity = null
                }
            )
        }

        // Add / Edit Master SKU Commodity Bottom Sheet
        if (showAddSkuDialog || editingCommodity != null) {
            AddEditCommodityBottomSheet(
                commodity = editingCommodity,
                onDismiss = {
                    showAddSkuDialog = false
                    editingCommodity = null
                },
                onSave = { rawKey, canonicalName, category, brand, unit, stock, reorder, cost, sell, sku ->
                    if (editingCommodity == null) {
                        viewModel.addCommoditySku(
                            rawKey = rawKey,
                            canonicalName = canonicalName,
                            category = category,
                            brand = brand,
                            unit = unit,
                            stock = stock,
                            reorder = reorder,
                            cost = cost,
                            sell = sell,
                            sku = sku
                        )
                    } else {
                        viewModel.updateCommodity(
                            editingCommodity!!.copy(
                                rawKey = rawKey.ifBlank { canonicalName.lowercase().trim() },
                                canonicalName = canonicalName,
                                category = category,
                                brand = brand,
                                normalizedUnit = unit,
                                stockQuantity = stock,
                                reorderThreshold = reorder,
                                costPrice = cost,
                                sellingPrice = sell,
                                lastKnownPrice = sell,
                                sku = sku
                            )
                        )
                    }
                    showAddSkuDialog = false
                    editingCommodity = null
                },
                onDelete = { commId ->
                    viewModel.deleteCommodity(commId)
                    editingCommodity = null
                }
            )
        }
    }
}

/**
 * Add / Edit Master Commodity SKU Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCommodityBottomSheet(
    commodity: CommodityEntity?,
    onDismiss: () -> Unit,
    onSave: (
        rawKey: String,
        canonicalName: String,
        category: String,
        brand: String,
        unit: String,
        stock: Double,
        reorder: Double,
        cost: Double,
        sell: Double,
        sku: String
    ) -> Unit,
    onDelete: (Long) -> Unit
) {
    val colors = MaterialTheme.cyphrColors

    var name by remember(commodity) { mutableStateOf(commodity?.canonicalName ?: "") }
    var category by remember(commodity) { mutableStateOf(commodity?.category ?: "Groceries") }
    var brand by remember(commodity) { mutableStateOf(commodity?.brand ?: "") }
    var unit by remember(commodity) { mutableStateOf(commodity?.normalizedUnit ?: "kg") }
    var stockStr by remember(commodity) { mutableStateOf(commodity?.stockQuantity?.toInt()?.toString() ?: "50") }
    var reorderStr by remember(commodity) { mutableStateOf(commodity?.reorderThreshold?.toInt()?.toString() ?: "10") }
    var costStr by remember(commodity) { mutableStateOf(commodity?.costPrice?.toInt()?.toString() ?: "40") }
    var sellStr by remember(commodity) { mutableStateOf(commodity?.sellingPrice?.toInt()?.toString() ?: "55") }
    var sku by remember(commodity) { mutableStateOf(commodity?.sku ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.warmCanvas
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
                    text = if (commodity == null) "ADD MASTER SKU" else "EDIT SKU DETAILS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.emberOrange
                )

                if (commodity != null) {
                    IconButton(onClick = { onDelete(commodity.id) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = CyberRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Commodity / Product Name *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("commodity_name_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand / Mfg") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("Barcode / SKU") },
                    modifier = Modifier.weight(1.2f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit (kg, pcs, ltr)") },
                    modifier = Modifier.weight(0.8f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = stockStr,
                    onValueChange = { stockStr = it },
                    label = { Text("Opening Stock") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = reorderStr,
                    onValueChange = { reorderStr = it },
                    label = { Text("Reorder Level") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it },
                    label = { Text("Inward Cost (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = sellStr,
                    onValueChange = { sellStr = it },
                    label = { Text("Selling Price / MRP (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val stock = stockStr.toDoubleOrNull() ?: 0.0
                        val reorder = reorderStr.toDoubleOrNull() ?: 10.0
                        val cost = costStr.toDoubleOrNull() ?: 0.0
                        val sell = sellStr.toDoubleOrNull() ?: 0.0
                        onSave(
                            name.lowercase().trim(),
                            name.trim(),
                            category.trim(),
                            brand.trim(),
                            unit.trim(),
                            stock,
                            reorder,
                            cost,
                            sell,
                            sku.trim()
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_commodity_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.emberOrange,
                    contentColor = VoidBlack
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (commodity == null) "SAVE SKU TO INVENTORY" else "UPDATE SKU",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
