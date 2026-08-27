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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommodityEntity
import com.example.data.model.GrnReceiptItemInput
import com.example.data.model.PurchaseOrderEntity
import com.example.data.model.PurchaseOrderItemEntity
import com.example.data.model.SupplierEntity
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberCard
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

/**
 * Add / Edit Supplier & Wholesale Distributor Modal
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSupplierBottomSheet(
    supplier: SupplierEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, contact: String, phone: String, email: String, address: String, gstin: String, paymentTerms: String, leadTime: Int, notes: String) -> Unit
) {
    val colors = MaterialTheme.cyphrColors

    var name by remember(supplier) { mutableStateOf(supplier?.name ?: "") }
    var contact by remember(supplier) { mutableStateOf(supplier?.contactPerson ?: "") }
    var phone by remember(supplier) { mutableStateOf(supplier?.phone ?: "") }
    var email by remember(supplier) { mutableStateOf(supplier?.email ?: "") }
    var address by remember(supplier) { mutableStateOf(supplier?.address ?: "") }
    var gstin by remember(supplier) { mutableStateOf(supplier?.gstin ?: "") }
    var paymentTerms by remember(supplier) { mutableStateOf(supplier?.paymentTerms ?: "Net 15 Days") }
    var leadTimeStr by remember(supplier) { mutableStateOf(supplier?.leadTimeDays?.toString() ?: "2") }
    var notes by remember(supplier) { mutableStateOf(supplier?.notes ?: "") }

    val termsOptions = listOf("Net 15 Days", "Net 30 Days", "COD Cash On Delivery", "Advance UPI")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.warmCanvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = if (supplier == null) "ADD WHOLESALE DISTRIBUTOR" else "EDIT SUPPLIER PROFILE",
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.emberOrange,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Distributor / Company Name *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("supplier_name_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contact Person") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone / WhatsApp") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("supplier_phone_input"),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = gstin,
                    onValueChange = { gstin = it },
                    label = { Text("GSTIN Tax ID") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Warehouse / Logistics Hub Address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Payment Terms & Credit Terms",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = colors.ghostSilverMuted
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                termsOptions.forEach { opt ->
                    val isSelected = paymentTerms == opt
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) colors.emberOrange.copy(alpha = 0.2f) else colors.warmCard)
                            .border(
                                BorderStroke(1.dp, if (isSelected) colors.emberOrange else colors.warmBorder),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { paymentTerms = opt }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = opt.replace(" Cash On Delivery", ""),
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) colors.emberOrange else colors.ghostSilverMuted,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = leadTimeStr,
                    onValueChange = { leadTimeStr = it },
                    label = { Text("Lead Time (Days)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.8f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Internal Supply Notes") },
                    modifier = Modifier.weight(1.2f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val lead = leadTimeStr.toIntOrNull() ?: 2
                        onSave(name, contact, phone, email, address, gstin, paymentTerms, lead, notes)
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_supplier_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.emberOrange,
                    contentColor = VoidBlack
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (supplier == null) "SAVE SUPPLIER TO DIRECTORY" else "UPDATE SUPPLIER",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Interactive Purchase Order (PO) Creator Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePurchaseOrderBottomSheet(
    suppliers: List<SupplierEntity>,
    commodities: List<CommodityEntity>,
    onDismiss: () -> Unit,
    onCreatePO: (supplierId: Long, supplierName: String, items: List<PurchaseOrderItemEntity>, leadDays: Int, notes: String) -> Unit
) {
    val colors = MaterialTheme.cyphrColors

    var selectedSupplier by remember { mutableStateOf(suppliers.firstOrNull()) }
    var shippingNotes by remember { mutableStateOf("Gate 2 Inward Loading Dock") }
    var selectedLeadDays by remember { mutableStateOf(selectedSupplier?.leadTimeDays ?: 2) }

    // Dynamic PO Line Items
    val poItems = remember {
        mutableStateListOf<PurchaseOrderItemEntity>()
    }

    var showAddItemDialog by remember { mutableStateOf(false) }

    val estimatedSubtotal = remember(poItems.toList()) {
        poItems.sumOf { it.lineTotal }
    }
    val estimatedTax = estimatedSubtotal * 0.05
    val estimatedGrandTotal = estimatedSubtotal + estimatedTax

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.warmCanvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NEW PURCHASE ORDER (PO)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.emberOrange
                    )
                    Text(
                        text = "Inward Procurement Requisition",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 11.sp,
                        color = colors.ghostSilverMuted
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.steelGrey
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Select Supplier Horizontal Selector
            Text(
                text = "SELECT DISTRIBUTOR / VENDOR",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = colors.ghostSilverMuted
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                suppliers.forEach { sup ->
                    val isSelected = selectedSupplier?.id == sup.id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) colors.emberOrange.copy(alpha = 0.2f) else colors.warmCard)
                            .border(
                                BorderStroke(1.dp, if (isSelected) colors.emberOrange else colors.warmBorder),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedSupplier = sup
                                selectedLeadDays = sup.leadTimeDays
                            }
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = sup.name.split(" ").take(2).joinToString(" "),
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) colors.emberOrange else colors.ghostSilverMuted,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Ordered Items List Header with Add Item button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ORDER ITEMS (${poItems.size})",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )

                Button(
                    onClick = { showAddItemDialog = true },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan.copy(alpha = 0.2f),
                        contentColor = NeonCyan
                    ),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Item",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ADD SKU",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (poItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.warmCard)
                        .border(1.dp, colors.warmBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No items added yet. Tap '+ ADD SKU' to add catalog commodities.",
                        fontSize = 11.sp,
                        color = colors.steelGrey
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(poItems) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.warmCard)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.itemName,
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.ghostSilver
                                )
                                Text(
                                    text = "${item.orderedQuantity.toInt()} ${item.unit} @ ₹${item.unitCostPrice.toInt()}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = colors.ghostSilverMuted
                                )
                            }

                            Text(
                                text = "₹${item.lineTotal.toInt()}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AcidLime
                            )

                            IconButton(
                                onClick = { poItems.remove(item) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = CyberRed,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Totals Summary Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.warmSurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Subtotal: ₹${estimatedSubtotal.toInt()} + GST (5%): ₹${estimatedTax.toInt()}",
                        fontSize = 10.sp,
                        color = colors.steelGrey
                    )
                    Text(
                        text = "ESTIMATED TOTAL",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.ghostSilverMuted
                    )
                }

                Text(
                    text = "₹${estimatedGrandTotal.toInt()}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AcidLime
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = shippingNotes,
                onValueChange = { shippingNotes = it },
                label = { Text("Logistics & Delivery Instructions") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    val sup = selectedSupplier ?: suppliers.firstOrNull()
                    if (sup != null && poItems.isNotEmpty()) {
                        onCreatePO(sup.id, sup.name, poItems.toList(), selectedLeadDays, shippingNotes)
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("confirm_create_po_button"),
                enabled = poItems.isNotEmpty() && selectedSupplier != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.emberOrange,
                    contentColor = VoidBlack
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "CONFIRM & DISPATCH PURCHASE ORDER",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Add SKU to PO Picker Sheet
        if (showAddItemDialog) {
            var selectedComm by remember { mutableStateOf(commodities.firstOrNull()) }
            var qtyStr by remember { mutableStateOf("25") }
            var costStr by remember(selectedComm) {
                val cost = if (selectedComm?.costPrice ?: 0.0 > 0) selectedComm!!.costPrice else (selectedComm?.sellingPrice ?: 50.0) * 0.75
                mutableStateOf(cost.toInt().toString())
            }

            ModalBottomSheet(
                onDismissRequest = { showAddItemDialog = false },
                containerColor = colors.warmCanvas
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "ADD SKU TO PURCHASE ORDER",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Select Master Commodity:",
                        fontSize = 11.sp,
                        color = colors.ghostSilverMuted
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(commodities) { comm ->
                            val isSel = selectedComm?.id == comm.id
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) NeonCyan.copy(alpha = 0.2f) else colors.warmCard)
                                    .border(BorderStroke(1.dp, if (isSel) NeonCyan else colors.warmBorder), RoundedCornerShape(6.dp))
                                    .clickable { selectedComm = comm }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = comm.canonicalName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) NeonCyan else colors.ghostSilver
                                    )
                                    Text(
                                        text = "Cur Stock: ${comm.stockQuantity.toInt()} ${comm.normalizedUnit}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = colors.ghostSilverMuted
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = qtyStr,
                            onValueChange = { qtyStr = it },
                            label = { Text("Order Qty (${selectedComm?.normalizedUnit ?: "pcs"})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = costStr,
                            onValueChange = { costStr = it },
                            label = { Text("Unit Inward Cost (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val comm = selectedComm
                            val qty = qtyStr.toDoubleOrNull() ?: 10.0
                            val cost = costStr.toDoubleOrNull() ?: 0.0
                            if (comm != null && qty > 0) {
                                poItems.add(
                                    PurchaseOrderItemEntity(
                                        purchaseOrderId = 0L,
                                        commodityId = comm.id,
                                        itemName = comm.canonicalName,
                                        brand = comm.brand,
                                        orderedQuantity = qty,
                                        unit = comm.normalizedUnit,
                                        unitCostPrice = cost,
                                        lineTotal = qty * cost
                                    )
                                )
                                showAddItemDialog = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = VoidBlack
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ADD ITEM TO PO",
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

/**
 * Goods Received Note (GRN) Inward Stock Intake Modal
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveGrnBottomSheet(
    po: PurchaseOrderEntity,
    poItems: List<PurchaseOrderItemEntity>,
    onDismiss: () -> Unit,
    onConfirmGRN: (poId: Long, supplierName: String, items: List<GrnReceiptItemInput>, grnNo: String) -> Unit
) {
    val colors = MaterialTheme.cyphrColors
    var grnNumber by remember { mutableStateOf("GRN-${System.currentTimeMillis() % 100000}") }

    // Maintain receipt parameters for each line item
    val receiptState = remember(poItems) {
        poItems.map { item ->
            mutableStateOf(
                GrnReceiptItemInput(
                    commodityId = item.commodityId,
                    itemName = item.itemName,
                    receivedQuantity = item.orderedQuantity,
                    unitCost = item.unitCostPrice,
                    batchNumber = "BAT-${item.itemName.take(3).uppercase()}-${System.currentTimeMillis() % 10000}",
                    expiryDateTimestamp = System.currentTimeMillis() + (180L * 86_400_000L),
                    unit = item.unit
                )
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.warmCanvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GOODS RECEIVED NOTE (GRN)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AcidLime
                    )
                    Text(
                        text = "Inward Stock Intake: ${po.orderNumber} • ${po.supplierName}",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 11.sp,
                        color = colors.ghostSilverMuted
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.steelGrey
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = grnNumber,
                onValueChange = { grnNumber = it },
                label = { Text("GRN Tracking Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "CONFIRM RECEIVED QUANTITY & BATCH CODES:",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = colors.ghostSilverMuted
            )

            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(receiptState.indices.toList()) { index ->
                    val itemState = receiptState[index]
                    val curr = itemState.value

                    CyberCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = colors.warmCard,
                        borderColor = colors.warmBorder,
                        cornerCut = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            Text(
                                text = curr.itemName,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.ghostSilver
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                var qtyStr by remember { mutableStateOf(curr.receivedQuantity.toInt().toString()) }
                                OutlinedTextField(
                                    value = qtyStr,
                                    onValueChange = {
                                        qtyStr = it
                                        val q = it.toDoubleOrNull() ?: 0.0
                                        itemState.value = curr.copy(receivedQuantity = q)
                                    },
                                    label = { Text("Recvd Qty (${curr.unit})") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )

                                var batchStr by remember { mutableStateOf(curr.batchNumber) }
                                OutlinedTextField(
                                    value = batchStr,
                                    onValueChange = {
                                        batchStr = it
                                        itemState.value = curr.copy(batchNumber = it)
                                    },
                                    label = { Text("Batch / Lot #") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    val inputs = receiptState.map { it.value }
                    onConfirmGRN(po.id, po.supplierName, inputs, grnNumber)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("confirm_grn_intake_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AcidLime,
                    contentColor = VoidBlack
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Confirm",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CONFIRM INWARD STOCK & UPDATE INVENTORY",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Manual Stock Adjustment & Waste Write-off Modal
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualStockAdjustmentBottomSheet(
    commodity: CommodityEntity,
    onDismiss: () -> Unit,
    onConfirmAdjustment: (commodityId: Long, changeQty: Double, reason: String, notes: String) -> Unit
) {
    val colors = MaterialTheme.cyphrColors
    var adjustmentType by remember { mutableStateOf("DAMAGED_WRITE_OFF") } // DAMAGED_WRITE_OFF, MANUAL_CORRECTION, RETURN_INWARD
    var qtyStr by remember { mutableStateOf("1") }
    var notes by remember { mutableStateOf("") }

    val reasonOptions = listOf(
        "DAMAGED_WRITE_OFF" to "Damaged / Expired Write-Off (-)",
        "MANUAL_CORRECTION" to "Audit Count Reconciliation (±)",
        "RETURN_INWARD" to "Customer Return Inward (+)"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.warmCanvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "MANUAL STOCK ADJUSTMENT / WRITE-OFF",
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.emberOrange
            )

            Text(
                text = "SKU: ${commodity.canonicalName} • Current Stock: ${commodity.stockQuantity.toInt()} ${commodity.normalizedUnit}",
                fontFamily = FontFamily.SansSerif,
                fontSize = 11.sp,
                color = colors.ghostSilverMuted
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Select Adjustment Reason:",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = colors.ghostSilverMuted
            )

            Spacer(modifier = Modifier.height(4.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                reasonOptions.forEach { (type, label) ->
                    val isSelected = adjustmentType == type
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) colors.emberOrange.copy(alpha = 0.15f) else colors.warmCard)
                            .border(BorderStroke(1.dp, if (isSelected) colors.emberOrange else colors.warmBorder), RoundedCornerShape(8.dp))
                            .clickable { adjustmentType = type }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) colors.emberOrange else colors.ghostSilver
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = qtyStr,
                onValueChange = { qtyStr = it },
                label = { Text("Quantity to Adjust (${commodity.normalizedUnit})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Reason / Auditor Notes") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val rawQty = qtyStr.toDoubleOrNull() ?: 0.0
                    val effectiveChange = when (adjustmentType) {
                        "DAMAGED_WRITE_OFF" -> -rawQty.coerceAtLeast(0.0)
                        "RETURN_INWARD" -> rawQty.coerceAtLeast(0.0)
                        else -> rawQty
                    }
                    onConfirmAdjustment(commodity.id, effectiveChange, adjustmentType, notes)
                    onDismiss()
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
                    text = "RECORD INVENTORY AUDIT ENTRY",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Record Accounts Payable Payment to Supplier Modal
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordSupplierPaymentBottomSheet(
    supplier: SupplierEntity?,
    po: PurchaseOrderEntity? = null,
    onDismiss: () -> Unit,
    onConfirmPayment: (supplierId: Long, poId: Long?, amount: Double) -> Unit
) {
    val colors = MaterialTheme.cyphrColors
    val initialAmount = po?.totalAmount ?: supplier?.outstandingPayable ?: 0.0
    var amountStr by remember { mutableStateOf(if (initialAmount > 0) initialAmount.toInt().toString() else "") }
    var paymentMethod by remember { mutableStateOf("UPI / Bank Transfer") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.warmCanvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "RECORD ACCOUNTS PAYABLE DISBURSEMENT",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AcidLime,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Vendor: ${supplier?.name ?: po?.supplierName ?: "Supplier"}",
                fontFamily = FontFamily.SansSerif,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.ghostSilver
            )

            if (supplier != null && supplier.outstandingPayable > 0) {
                Text(
                    text = "Total Outstanding Balance: ₹${supplier.outstandingPayable.toInt()}",
                    fontSize = 12.sp,
                    color = NeonAmber
                )
            }

            if (po != null) {
                Text(
                    text = "PO Reference: ${po.orderNumber} • Amount: ₹${po.totalAmount.toInt()}",
                    fontSize = 12.sp,
                    color = NeonCyan
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text("Payment Amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Full Balance", "50% Partial", "Custom").forEach { opt ->
                    val isSelected = when (opt) {
                        "Full Balance" -> amountStr == initialAmount.toInt().toString()
                        "50% Partial" -> amountStr == (initialAmount * 0.5).toInt().toString()
                        else -> false
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) AcidLime.copy(alpha = 0.2f) else colors.warmCard)
                            .border(1.dp, if (isSelected) AcidLime else colors.warmBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                when (opt) {
                                    "Full Balance" -> amountStr = initialAmount.toInt().toString()
                                    "50% Partial" -> amountStr = (initialAmount * 0.5).toInt().toString()
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(opt, fontSize = 11.sp, color = if (isSelected) AcidLime else colors.ghostSilverMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        val targetSupplierId = supplier?.id ?: po?.supplierId ?: 0L
                        onConfirmPayment(targetSupplierId, po?.id, amt)
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AcidLime,
                    contentColor = VoidBlack
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "CONFIRM VENDOR PAYMENT",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Add / Intake Custom Batch / Lot Modal (FIFO / FEFO)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBatchBottomSheet(
    commodities: List<CommodityEntity>,
    suppliers: List<SupplierEntity>,
    initialCommodity: CommodityEntity? = null,
    onDismiss: () -> Unit,
    onSaveBatch: (commodityId: Long, commodityName: String, batchNo: String, qty: Double, cost: Double, sell: Double, expDays: Int, supplierName: String, unit: String) -> Unit
) {
    val colors = MaterialTheme.cyphrColors

    var selectedCommodity by remember { mutableStateOf(initialCommodity ?: commodities.firstOrNull()) }
    var batchNo by remember { mutableStateOf("LOT-${System.currentTimeMillis() % 100000}") }
    var qtyStr by remember { mutableStateOf("50") }
    var costPriceStr by remember { mutableStateOf(selectedCommodity?.costPrice?.toInt()?.toString() ?: "100") }
    var sellPriceStr by remember { mutableStateOf(selectedCommodity?.sellingPrice?.toInt()?.toString() ?: "130") }
    var expiryDaysStr by remember { mutableStateOf("90") }
    var supplierName by remember { mutableStateOf(suppliers.firstOrNull()?.name ?: "Direct Wholesale") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.warmCanvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "REGISTER NEW BATCH / LOT (FEFO)",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Commodity Selector
            Text("Select SKU Commodity:", fontSize = 11.sp, color = colors.ghostSilverMuted)
            LazyColumn(modifier = Modifier.height(90.dp)) {
                items(commodities) { comm ->
                    val isSelected = selectedCommodity?.id == comm.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedCommodity = comm
                                costPriceStr = comm.costPrice.toInt().toString()
                                sellPriceStr = comm.sellingPrice.toInt().toString()
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(comm.canonicalName, fontSize = 12.sp, color = if (isSelected) NeonCyan else colors.ghostSilver)
                        if (isSelected) CyberBadge("SELECTED", color = NeonCyan)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = batchNo,
                    onValueChange = { batchNo = it },
                    label = { Text("Lot / Batch #") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it },
                    label = { Text("Quantity (${selectedCommodity?.normalizedUnit ?: "pcs"})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = costPriceStr,
                    onValueChange = { costPriceStr = it },
                    label = { Text("Unit Cost (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = expiryDaysStr,
                    onValueChange = { expiryDaysStr = it },
                    label = { Text("Shelf Life (Days)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = supplierName,
                onValueChange = { supplierName = it },
                label = { Text("Distributor / Source") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val comm = selectedCommodity ?: return@Button
                    val qty = qtyStr.toDoubleOrNull() ?: 1.0
                    val cost = costPriceStr.toDoubleOrNull() ?: comm.costPrice
                    val sell = sellPriceStr.toDoubleOrNull() ?: comm.sellingPrice
                    val days = expiryDaysStr.toIntOrNull() ?: 90
                    onSaveBatch(comm.id, comm.canonicalName, batchNo, qty, cost, sell, days, supplierName, comm.normalizedUnit)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = VoidBlack
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "REGISTER BATCH INVENTORY",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Promotional Markdown / Clearance Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionalMarkdownBottomSheet(
    commodityName: String,
    currentPrice: Double,
    commodityId: Long,
    batchId: Long? = null,
    suggestedDiscountPercent: Int = 25,
    onDismiss: () -> Unit,
    onApplyMarkdown: (commodityId: Long, discountPercent: Double, batchId: Long?) -> Unit
) {
    val colors = MaterialTheme.cyphrColors
    var selectedDiscount by remember { mutableStateOf(suggestedDiscountPercent) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.warmCanvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "APPLY PROMOTIONAL CLEARANCE MARKDOWN",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = EmberOrange,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = commodityName,
                fontFamily = FontFamily.SansSerif,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.ghostSilver
            )

            Text(
                text = "Current Retail Price: ₹${currentPrice.toInt()}",
                fontSize = 12.sp,
                color = colors.ghostSilverMuted
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text("Select Clearance Discount Rate:", fontSize = 11.sp, color = colors.ghostSilverMuted)

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15, 25, 35, 50).forEach { disc ->
                    val isSelected = selectedDiscount == disc
                    val discounted = (currentPrice * (1.0 - (disc / 100.0))).toInt()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) EmberOrange.copy(alpha = 0.2f) else colors.warmCard)
                            .border(1.dp, if (isSelected) EmberOrange else colors.warmBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedDiscount = disc }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("-$disc%", fontWeight = FontWeight.Bold, color = if (isSelected) EmberOrange else colors.ghostSilver)
                            Text("₹$discounted", fontSize = 10.sp, color = colors.ghostSilverMuted)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    onApplyMarkdown(commodityId, selectedDiscount.toDouble(), batchId)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmberOrange,
                    contentColor = VoidBlack
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "ACTIVATE -$selectedDiscount% MARKDOWN SALE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
