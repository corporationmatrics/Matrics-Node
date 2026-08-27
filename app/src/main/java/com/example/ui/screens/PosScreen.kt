package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CommodityEntity
import com.example.data.model.PosCartItem
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberCard
import com.example.ui.components.GridBackgroundBox
import com.example.ui.components.PosQrCodeView
import com.example.ui.theme.AcidLime
import com.example.ui.theme.CyberRed
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.VoidBlack
import com.example.ui.theme.cyphrColors
import com.example.ui.viewmodel.CyphrViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: CyphrViewModel,
    onOpenVoiceHud: () -> Unit
) {
    val colors = MaterialTheme.cyphrColors
    val commodities by viewModel.allCommodities.collectAsStateWithLifecycle()
    val cartItems by viewModel.posCartItems.collectAsStateWithLifecycle()
    val taxPercent by viewModel.posTaxPercent.collectAsStateWithLifecycle()
    val discountPercent by viewModel.posDiscountPercent.collectAsStateWithLifecycle()
    val customerName by viewModel.posCustomerName.collectAsStateWithLifecycle()
    val customerPhone by viewModel.posCustomerPhone.collectAsStateWithLifecycle()
    val searchQuery by viewModel.posSearchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.posSelectedCategoryFilter.collectAsStateWithLifecycle()
    val checkoutState by viewModel.posCheckoutDialogState.collectAsStateWithLifecycle()

    var showCustomItemDialog by remember { mutableStateOf(false) }

    val categories = remember(commodities) {
        listOf("ALL") + commodities.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    val filteredCommodities = remember(commodities, searchQuery, selectedCategory) {
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

    val subtotal = remember(cartItems) { cartItems.sumOf { it.lineTotal } }
    val discountAmount = remember(subtotal, discountPercent) { subtotal * (discountPercent / 100.0) }
    val afterDiscount = (subtotal - discountAmount).coerceAtLeast(0.0)
    val taxAmount = remember(afterDiscount, taxPercent) { afterDiscount * (taxPercent / 100.0) }
    val netTotal = afterDiscount + taxAmount

    GridBackgroundBox {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // POS Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
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
                            text = "POS TERMINAL 01",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AcidLime,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "Storefront Checkout",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.ghostSilver
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Thermal Printer Settings Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.warmSurfaceElevated)
                            .border(1.dp, colors.warmBorder, RoundedCornerShape(10.dp))
                            .clickable { viewModel.openThermalPrinterSettings() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("pos_thermal_printer_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Thermal Printer",
                                tint = NeonCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PRINTER",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.ghostSilver
                            )
                        }
                    }

                    // Voice Sale Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.warmSurfaceElevated)
                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .clickable { onOpenVoiceHud() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("pos_voice_sale_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Sale",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "VOICE SALE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                    }

                    // Clear Cart Button
                    if (cartItems.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.warmSurfaceElevated)
                                .border(1.dp, CyberRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .clickable { viewModel.clearPosCart() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "CLEAR",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar & Add Custom Item Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setPosSearchQuery(it) },
                    placeholder = {
                        Text(
                            text = "Search catalog or SKU...",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 12.sp,
                            color = colors.steelGrey
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = colors.steelGrey,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setPosSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = colors.steelGrey,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("pos_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = colors.warmBorder,
                        focusedContainerColor = colors.warmCard,
                        unfocusedContainerColor = colors.warmCard,
                        focusedTextColor = colors.ghostSilver,
                        unfocusedTextColor = colors.ghostSilver
                    ),
                    singleLine = true
                )

                // Quick Add Custom Product Button
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.warmCard)
                        .border(1.dp, colors.warmBorder, RoundedCornerShape(12.dp))
                        .clickable { showCustomItemDialog = true }
                        .padding(horizontal = 10.dp)
                        .testTag("pos_add_custom_item_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add custom item",
                            tint = colors.emberOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "CUSTOM",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.emberOrange
                        )
                    }
                }

                // Camera Barcode Scanner Button
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                        .clickable { viewModel.openBarcodeScanner(com.example.ui.viewmodel.BarcodeScanMode.POS_BILLING) }
                        .padding(horizontal = 12.dp)
                        .testTag("pos_scan_barcode_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Barcode",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SCAN",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = (selectedCategory == null && cat == "ALL") || selectedCategory.equals(cat, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else colors.warmCard)
                            .border(
                                BorderStroke(1.dp, if (isSelected) NeonCyan else colors.warmBorder),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setPosCategoryFilter(if (cat == "ALL") null else cat) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = cat.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) NeonCyan else colors.ghostSilverMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Split Layout: Catalog Grid on Top (flexible weight), Active Bill on Bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Product Grid (Catalog selection)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(if (cartItems.isEmpty()) 1f else 0.55f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCommodities, key = { it.id }) { item ->
                        PosProductCard(
                            commodity = item,
                            onAddToCart = { viewModel.addItemToPosCart(item) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Active Cart & Bill Panel
                if (cartItems.isNotEmpty()) {
                    CyberCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.45f),
                        borderColor = NeonCyan.copy(alpha = 0.35f),
                        backgroundColor = colors.warmCard,
                        cornerCut = 14.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                        ) {
                            // Cart Header with Item Count
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = "Cart",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "CURRENT BILL (${cartItems.size} items)",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                }

                                Text(
                                    text = "Total: ₹${netTotal.toInt()}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AcidLime
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Cart Items Scrollable List
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                itemsIndexed(cartItems) { index, cartItem ->
                                    PosCartItemRow(
                                        item = cartItem,
                                        onIncrease = { viewModel.updatePosCartItemQuantity(index, cartItem.quantity + 1.0) },
                                        onDecrease = { viewModel.updatePosCartItemQuantity(index, cartItem.quantity - 1.0) },
                                        onRemove = { viewModel.removePosCartItem(index) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Customer Info Inputs & Bill Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = customerName,
                                    onValueChange = { viewModel.setPosCustomer(it, customerPhone) },
                                    placeholder = {
                                        Text(
                                            text = "Customer Name (Optional)",
                                            fontSize = 10.sp,
                                            color = colors.steelGrey
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = colors.warmBorder,
                                        focusedContainerColor = colors.warmSurfaceElevated,
                                        unfocusedContainerColor = colors.warmSurfaceElevated,
                                        focusedTextColor = colors.ghostSilver,
                                        unfocusedTextColor = colors.ghostSilver
                                    ),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = customerPhone,
                                    onValueChange = { viewModel.setPosCustomer(customerName, it) },
                                    placeholder = {
                                        Text(
                                            text = "Phone / UPI",
                                            fontSize = 10.sp,
                                            color = colors.steelGrey
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .height(40.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = colors.warmBorder,
                                        focusedContainerColor = colors.warmSurfaceElevated,
                                        unfocusedContainerColor = colors.warmSurfaceElevated,
                                        focusedTextColor = colors.ghostSilver,
                                        unfocusedTextColor = colors.ghostSilver
                                    ),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Checkout Action Buttons (Dynamic QR Pay, Cash, Khata)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Primary Dynamic UPI QR Checkout Button
                                Button(
                                    onClick = { viewModel.initiatePosCheckout("UPI / QR") },
                                    modifier = Modifier
                                        .weight(1.4f)
                                        .height(44.dp)
                                        .testTag("pos_checkout_upi_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonCyan,
                                        contentColor = VoidBlack
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = "UPI QR",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "UPI QR (₹${netTotal.toInt()})",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Cash Direct Sale Button
                                Button(
                                    onClick = { viewModel.confirmPosSaleCompleted("CASH") },
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .height(44.dp)
                                        .testTag("pos_checkout_cash_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AcidLime,
                                        contentColor = VoidBlack
                                    )
                                ) {
                                    Text(
                                        text = "CASH",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Khata Store Credit Button
                                Button(
                                    onClick = {
                                        if (customerName.isBlank()) {
                                            viewModel.setPosCustomer("Store Customer", "")
                                        }
                                        viewModel.confirmPosSaleCompleted("KHATA")
                                    },
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .height(44.dp)
                                        .testTag("pos_checkout_khata_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.warmSurfaceElevated,
                                        contentColor = colors.emberOrange
                                    ),
                                    border = BorderStroke(1.dp, colors.emberOrange)
                                ) {
                                    Text(
                                        text = "KHATA",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // POS Dynamic Merchant UPI QR Checkout Bottom Sheet
        if (checkoutState.isOpen) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissPosCheckout() },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = colors.warmCanvas
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CUSTOMER PAYMENT TERMINAL",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // QR Code View
                    PosQrCodeView(
                        upiUri = checkoutState.upiQrUri,
                        invoiceNo = checkoutState.invoiceNo,
                        amount = checkoutState.totalAmount
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Payment Method selector (UPI, Cash, Card, Khata)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("UPI / QR", "CASH", "CARD", "KHATA").forEach { mode ->
                            val isSelected = checkoutState.paymentMethod.equals(mode, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else colors.warmCard)
                                    .border(BorderStroke(1.dp, if (isSelected) NeonCyan else colors.warmBorder), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.updateCheckoutPaymentMethod(mode) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = mode,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NeonCyan else colors.ghostSilverMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm Payment Button
                    Button(
                        onClick = { viewModel.confirmPosSaleCompleted() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pos_confirm_payment_received_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AcidLime,
                            contentColor = VoidBlack
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Confirm",
                            tint = VoidBlack,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CONFIRM PAYMENT & PRINT BILL",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Custom Item Add Dialog Modal
        if (showCustomItemDialog) {
            var customName by remember { mutableStateOf("") }
            var customPrice by remember { mutableStateOf("") }
            var customQty by remember { mutableStateOf("1") }
            var customUnit by remember { mutableStateOf("pcs") }

            ModalBottomSheet(
                onDismissRequest = { showCustomItemDialog = false },
                containerColor = colors.warmCanvas
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "ADD CUSTOM / UNLISTED ITEM",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.emberOrange
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Item Name / Description") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customPrice,
                            onValueChange = { customPrice = it },
                            label = { Text("Unit Price (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = customQty,
                            onValueChange = { customQty = it },
                            label = { Text("Qty") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.6f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = customUnit,
                            onValueChange = { customUnit = it },
                            label = { Text("Unit") },
                            modifier = Modifier.weight(0.6f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val price = customPrice.toDoubleOrNull() ?: 0.0
                            val qty = customQty.toDoubleOrNull() ?: 1.0
                            if (customName.isNotBlank() && price > 0) {
                                viewModel.addCustomItemToPosCart(
                                    name = customName,
                                    price = price,
                                    quantity = qty,
                                    unit = customUnit
                                )
                                showCustomItemDialog = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.emberOrange,
                            contentColor = VoidBlack
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "ADD TO BILL",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun PosProductCard(
    commodity: CommodityEntity,
    onAddToCart: () -> Unit
) {
    val colors = MaterialTheme.cyphrColors
    val isLowStock = commodity.stockQuantity <= commodity.reorderThreshold
    val price = if (commodity.sellingPrice > 0) commodity.sellingPrice else if (commodity.lastKnownPrice > 0) commodity.lastKnownPrice else 50.0

    CyberCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onAddToCart() }
            .testTag("pos_product_card_${commodity.id}"),
        borderColor = if (isLowStock) CyberRed.copy(alpha = 0.5f) else colors.warmBorder,
        backgroundColor = colors.warmCard,
        cornerCut = 14.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = commodity.category.uppercase(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = colors.steelGrey
                )

                // Stock Indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isLowStock) CyberRed.copy(alpha = 0.15f) else AcidLime.copy(alpha = 0.12f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${commodity.stockQuantity.toInt()} ${commodity.normalizedUnit}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLowStock) CyberRed else AcidLime
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = commodity.canonicalName,
                fontFamily = FontFamily.SansSerif,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.ghostSilver,
                maxLines = 1
            )

            if (commodity.brand.isNotBlank()) {
                Text(
                    text = commodity.brand,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 10.sp,
                    color = colors.ghostSilverMuted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹${price.toInt()}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                        .clickable { onAddToCart() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = VoidBlack,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PosCartItemRow(
    item: PosCartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = MaterialTheme.cyphrColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.warmSurfaceElevated)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colors.ghostSilver,
                maxLines = 1
            )
            Text(
                text = "₹${item.unitPrice.toInt()} / ${item.unit}",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = colors.ghostSilverMuted
            )
        }

        // Qty controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(colors.warmCard)
                    .border(1.dp, colors.warmBorder, CircleShape)
                    .clickable { onDecrease() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease",
                    tint = colors.ghostSilver,
                    modifier = Modifier.size(12.dp)
                )
            }

            Text(
                text = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString(),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.ghostSilver,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(colors.warmCard)
                    .border(1.dp, colors.warmBorder, CircleShape)
                    .clickable { onIncrease() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    tint = colors.ghostSilver,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "₹${item.lineTotal.toInt()}",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NeonCyan
        )

        IconButton(
            onClick = { onRemove() },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = colors.steelGrey,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
