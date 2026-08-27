package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberCard
import com.example.ui.components.GridBackgroundBox
import com.example.ui.components.LedgrHeader
import com.example.ui.theme.AcidLime
import com.example.ui.theme.cyphrColors
import com.example.ui.theme.getCategoryColor
import com.example.ui.viewmodel.CyphrViewModel

data class PriceIndexItem(
    val name: String,
    val category: String,
    val avgPrice: Double,
    val avgVendor: String,
    val bestPrice: Double,
    val bestVendor: String,
    val discountPercent: Double
)

@Composable
fun PricesScreen(
    viewModel: CyphrViewModel,
    modifier: Modifier = Modifier
) {
    val isFieldMode by viewModel.isFieldMode.collectAsStateWithLifecycle()
    val userApiKey by viewModel.userGeminiApiKey.collectAsStateWithLifecycle()
    val colors = MaterialTheme.cyphrColors
    var searchQuery by remember { mutableStateOf("") }

    val samplePriceIndices = remember {
        listOf(
            PriceIndexItem("Amul Salted Butter 500g", "Dairy", 285.0, "FreshMart", 275.0, "DMart Ready", -3.5),
            PriceIndexItem("Fortune Sunlite Sunflower Oil 1L", "Pantry", 160.0, "Blinkit", 145.0, "DMart Ready", -9.3),
            PriceIndexItem("Aashirvaad Shudh Chakki Atta 5kg", "Grains", 270.0, "Zepto", 249.0, "Local Supermarket", -7.7),
            PriceIndexItem("Nandini Pasteurized Toned Milk 1L", "Dairy", 42.0, "FreshMart", 42.0, "Direct Store", 0.0),
            PriceIndexItem("Tata Tea Gold 500g", "Beverages", 340.0, "Instamart", 310.0, "DMart Ready", -8.8),
            PriceIndexItem("India Gate Basmati Rice 1kg", "Grains", 185.0, "Blinkit", 165.0, "SuperMart", -10.8)
        )
    }

    val filteredList = if (searchQuery.isBlank()) {
        samplePriceIndices
    } else {
        samplePriceIndices.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true)
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
                    title = "Prices",
                    subtitle = "Real-time item price benchmark across stores.",
                    brandLabel = "M A T R I C S",
                    onToggleFieldMode = { viewModel.toggleFieldMode() },
                    onOpenSettings = { viewModel.openModal("SETTINGS") },
                    isFieldMode = isFieldMode,
                    isAiOnline = userApiKey.isNotBlank()
                )
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search commodity or store...",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 13.sp,
                            color = colors.ghostSilverMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = colors.emberOrange,
                            modifier = Modifier.size(18.dp)
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
                        .fillMaxWidth()
                        .testTag("input_search_prices")
                )
            }

            // Lowest Price Deals Card
            item {
                CyberCard(
                    modifier = Modifier.fillMaxWidth().testTag("best_price_banner_card"),
                    borderColor = colors.warmBorder,
                    backgroundColor = colors.warmCard,
                    cornerCut = 24.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.emberOrange.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = colors.emberOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Best Basket Optimizer",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.ghostSilver
                                )
                                Text(
                                    text = "Save up to 12.4% on monthly groceries",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 11.sp,
                                    color = colors.ghostSilverMuted
                                )
                            }
                        }

                        CyberBadge(
                            text = "LIVE",
                            color = AcidLime
                        )
                    }
                }
            }

            // Price Index List
            items(filteredList) { item ->
                val catColor = getCategoryColor(item.category)
                CyberCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = colors.warmBorderSubtle,
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
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.ghostSilver
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(catColor.copy(alpha = 0.18f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = item.category,
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = catColor
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹${item.bestPrice.toInt()}",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.emberOrange
                                )
                                if (item.discountPercent < 0) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.TrendingDown,
                                            contentDescription = null,
                                            tint = AcidLime,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "${item.discountPercent}%",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AcidLime
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.warmSurfaceElevated)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lowest at: ${item.bestVendor}",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 11.sp,
                                color = colors.ghostSilverMuted
                            )
                            Text(
                                text = "Avg: ₹${item.avgPrice.toInt()}",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 11.sp,
                                color = colors.steelGrey
                            )
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
