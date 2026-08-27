package com.example.ui.screens

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.UpiPaymentManager
import com.example.data.UpiPaymentParams
import com.example.data.UpiPaymentResult
import com.example.ui.components.CyberCard
import com.example.ui.components.GridBackgroundBox
import com.example.ui.components.LedgrHeader
import com.example.ui.components.QuickActionCard
import com.example.ui.theme.AcidLime
import com.example.ui.theme.VoidBlack
import com.example.ui.theme.cyphrColors
import com.example.ui.viewmodel.CyphrViewModel

@Composable
fun GroceryPantryScreen(
    viewModel: CyphrViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val wishlistItems by viewModel.wishlistItems.collectAsStateWithLifecycle()
    val isFieldMode by viewModel.isFieldMode.collectAsStateWithLifecycle()
    val userApiKey by viewModel.userGeminiApiKey.collectAsStateWithLifecycle()
    val colors = MaterialTheme.cyphrColors
    var newItemName by remember { mutableStateOf("") }

    var checkoutVendorName by remember { mutableStateOf("FreshMart Kirana") }
    var checkoutVendorUpi by remember { mutableStateOf("shopkeeper@okhdfcbank") }
    var showUpiCheckoutSheet by remember { mutableStateOf(false) }

    val checkedItems = wishlistItems.filter { it.isChecked }
    val checkedTotal = if (checkedItems.isNotEmpty()) checkedItems.sumOf { it.estimatedPrice } else 0.0

    // Activity Result Launcher for UPI Intent Return
    val upiCheckoutLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data?.getStringExtra("response")
            ?: result.data?.dataString
            ?: (result.data?.extras?.keySet()?.joinToString("&") { key -> "$key=${result.data?.extras?.get(key)}" } ?: "")

        val paymentResult = UpiPaymentManager.parseUpiResponse(data)
        when (paymentResult) {
            is UpiPaymentResult.Success -> {
                viewModel.checkoutGroceryWithUpi(
                    merchantName = checkoutVendorName,
                    payeeUpi = checkoutVendorUpi,
                    utrNumber = paymentResult.approvalRefNo
                )
                showUpiCheckoutSheet = false
            }
            is UpiPaymentResult.Failure -> {
                viewModel.showToast("❌ Payment failed: ${paymentResult.errorMessage}")
            }
            is UpiPaymentResult.Cancelled -> {
                viewModel.showToast("⚠️ Payment cancelled")
            }
        }
    }

    fun launchUpiPaymentForGrocery() {
        val total = if (checkedTotal > 0) checkedTotal else 150.0
        val params = UpiPaymentParams(
            payeeVpa = checkoutVendorUpi.trim(),
            payeeName = checkoutVendorName.ifBlank { "Kirana Merchant" },
            amount = total,
            transactionRefId = "GROCERY_${System.currentTimeMillis()}",
            transactionNote = "Grocery items: ${checkedItems.joinToString(", ") { it.name }.take(40)}"
        )
        try {
            val intent = UpiPaymentManager.createUpiIntent(params)
            upiCheckoutLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            viewModel.showToast("No UPI apps installed. Use simulation test.")
        } catch (e: Exception) {
            viewModel.showToast("Error launching UPI: ${e.message}")
        }
    }

    GridBackgroundBox(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Matrics Header with Field Mode Switcher and Settings/AI Modal
            item {
                LedgrHeader(
                    title = "Grocery list",
                    subtitle = "Things to buy, ticked off as you shop.",
                    brandLabel = "M A T R I C S",
                    onToggleFieldMode = { viewModel.toggleFieldMode() },
                    onOpenSettings = { viewModel.openModal("SETTINGS") },
                    isFieldMode = isFieldMode,
                    isAiOnline = userApiKey.isNotBlank()
                )
            }

            // Quick Add Input Bar with orange (+) button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        placeholder = {
                            Text(
                                text = "Add an item to buy...",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.sp,
                                color = colors.ghostSilverMuted
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.emberOrange,
                            unfocusedBorderColor = colors.warmBorder,
                            focusedTextColor = colors.ghostSilver,
                            unfocusedTextColor = colors.ghostSilver,
                            unfocusedContainerColor = colors.warmCard,
                            focusedContainerColor = colors.warmCard
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_add_grocery_item")
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            if (newItemName.isNotBlank()) {
                                viewModel.addWishlistItem(
                                    name = newItemName.trim(),
                                    qty = "1 unit",
                                    price = 50.0,
                                    cap = 60.0,
                                    vendor = "DMart",
                                    category = "Groceries"
                                )
                                newItemName = ""
                            }
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("btn_quick_add_grocery"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.emberOrange,
                            contentColor = VoidBlack
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Item",
                            tint = VoidBlack,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // "To buy (X)" Section Container
            item {
                val pendingItems = wishlistItems.filter { !it.isChecked }

                CyberCard(
                    modifier = Modifier.fillMaxWidth().testTag("to_buy_card"),
                    borderColor = colors.warmBorder,
                    backgroundColor = colors.warmCard,
                    cornerCut = 24.dp
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
                                text = "To buy (${pendingItems.size})",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.ghostSilver
                            )

                            Text(
                                text = "REMINDERS ON",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                                color = colors.ghostSilverMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (pendingItems.isEmpty()) {
                            Text(
                                text = "Nothing pending. Add items here or tap “To list” while recording an expense.",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = colors.ghostSilverMuted
                            )
                        } else {
                            pendingItems.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = item.isChecked,
                                            onCheckedChange = { viewModel.toggleWishlistChecked(item) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = colors.emberOrange,
                                                uncheckedColor = colors.steelGrey,
                                                checkmarkColor = VoidBlack
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = item.name,
                                                fontFamily = FontFamily.SansSerif,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.ghostSilver
                                            )
                                            Text(
                                                text = "${item.quantity} • ${item.category}",
                                                fontFamily = FontFamily.SansSerif,
                                                fontSize = 11.sp,
                                                color = colors.ghostSilverMuted
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteGroceryItem(item.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = colors.steelGrey,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2-Column Action Cards:
            // Left: [Mic] "Add by voice" ("add milk and atta")
            // Right: [Cart] "Order this list" ("Checkout & pay in-app")
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Default.Mic,
                        title = "Add by voice",
                        subtitle = "“add milk and atta”",
                        onClick = { viewModel.openVoiceHud() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("action_add_by_voice")
                    )

                    QuickActionCard(
                        icon = Icons.Default.ShoppingCart,
                        title = "Order this list",
                        subtitle = "Checkout & pay in-app",
                        onClick = { viewModel.openModal("QUICK_COMMERCE") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("action_order_this_list")
                    )
                }
            }

            // Purchased / Checked Section
            if (checkedItems.isNotEmpty()) {
                item {
                    CyberCard(
                        modifier = Modifier.fillMaxWidth().testTag("card_checked_groceries"),
                        borderColor = colors.emberOrange.copy(alpha = 0.5f),
                        backgroundColor = colors.warmSurfaceElevated,
                        cornerCut = 20.dp
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
                                Column {
                                    Text(
                                        text = "Checked for Purchase (${checkedItems.size})",
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.ghostSilver
                                    )
                                    Text(
                                        text = "Total: ₹${checkedTotal.toInt()}",
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.emberOrange
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { showUpiCheckoutSheet = !showUpiCheckoutSheet },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.emberOrange,
                                            contentColor = VoidBlack
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp).testTag("btn_toggle_upi_checkout"),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                    ) {
                                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp), tint = VoidBlack)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("UPI Pay & Restock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (showUpiCheckoutSheet) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.warmCard)
                                        .border(1.dp, colors.warmBorderSubtle, RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "DIRECT UPI CHECKOUT RAIL",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.emberOrange
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = checkoutVendorUpi,
                                        onValueChange = { checkoutVendorUpi = it },
                                        label = { Text("Vendor UPI ID (VPA)", fontSize = 11.sp, color = colors.ghostSilverMuted) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colors.emberOrange,
                                            unfocusedBorderColor = colors.warmBorderSubtle,
                                            focusedTextColor = colors.ghostSilver,
                                            unfocusedTextColor = colors.ghostSilver
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("input_grocery_upi_vpa")
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    OutlinedTextField(
                                        value = checkoutVendorName,
                                        onValueChange = { checkoutVendorName = it },
                                        label = { Text("Merchant / Store Name", fontSize = 11.sp, color = colors.ghostSilverMuted) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colors.emberOrange,
                                            unfocusedBorderColor = colors.warmBorderSubtle,
                                            focusedTextColor = colors.ghostSilver,
                                            unfocusedTextColor = colors.ghostSilver
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("input_grocery_merchant_name")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = { launchUpiPaymentForGrocery() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.emberOrange,
                                            contentColor = VoidBlack
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(38.dp).testTag("btn_trigger_grocery_upi")
                                    ) {
                                        Text("LAUNCH UPI INTENT (₹${checkedTotal.toInt()})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    OutlinedButton(
                                        onClick = {
                                            val fakeUtr = "UTR${System.currentTimeMillis().toString().takeLast(8)}"
                                            viewModel.checkoutGroceryWithUpi(
                                                merchantName = checkoutVendorName,
                                                payeeUpi = checkoutVendorUpi,
                                                utrNumber = fakeUtr
                                            )
                                            showUpiCheckoutSheet = false
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(32.dp).testTag("btn_simulate_grocery_upi")
                                    ) {
                                        Text("SIMULATE UPI SUCCESS (TEST)", fontSize = 10.sp, color = AcidLime, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            checkedItems.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { viewModel.toggleWishlistChecked(item) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Completed",
                                            tint = AcidLime,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${item.name} (~₹${item.estimatedPrice.toInt()})",
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 13.sp,
                                            color = colors.ghostSilverMuted
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteGroceryItem(item.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = colors.steelGrey, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
