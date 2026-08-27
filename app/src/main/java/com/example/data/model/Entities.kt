package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val vendor: String,
    val category: String,
    val totalAmount: Double,
    val dateTimestamp: Long,
    val paymentMethod: String = "UPI / Direct",
    val locationName: String = "Auto-Pin",
    val itemCount: Int = 1,
    val rawVoicePrompt: String? = null,
    val isVerified: Boolean = true,
    val transactionType: String = "SALE", // SALE, RESTOCK, EXPENSE, KHATA_SETTLEMENT
    val customerName: String = "",
    val customerPhone: String = "",
    val subtotalAmount: Double = totalAmount,
    val taxAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val invoiceNumber: String = "",
    val orderStatus: String = "SETTLED" // SETTLED, PENDING_KHATA, CANCELLED
)

@Entity(tableName = "line_items")
data class LineItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val name: String,
    val category: String,
    val quantity: Double = 1.0,
    val unit: String = "unit",
    val unitPrice: Double,
    val totalPrice: Double,
    val vendor: String,
    val dateTimestamp: Long,
    val canonicalName: String = name,
    val brand: String = "",
    val storageType: String = "Pantry",
    val shelfLifeDays: Int = 30,
    val costPrice: Double = 0.0,
    val sku: String = ""
)

@Entity(tableName = "grocery_items")
data class GroceryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: String = "1 unit",
    val estimatedPrice: Double = 0.0,
    val priceCap: Double = 0.0,
    val targetVendor: String = "FreshMart",
    val category: String = "Grocery",
    val isChecked: Boolean = false,
    val isPantryItem: Boolean = false,
    val purchaseDate: Long = System.currentTimeMillis(),
    val expiryDaysTotal: Int = 14,
    val remainingDays: Int = 14,
    val burnRateLevel: String = "NORMAL", // LOW, NORMAL, HIGH
    val lastBoughtDaysAgo: Int = 0,
    val canonicalName: String = name,
    val brand: String = "",
    val storageType: String = "Pantry",
    val stockLevel: Double = 25.0,
    val costPrice: Double = 0.0,
    val sellingPrice: Double = estimatedPrice,
    val reorderThreshold: Double = 5.0,
    val sku: String = ""
)

/**
 * Tier 1 & Tier 2 Commodity Master, Live Store Inventory Catalog
 */
@Entity(
    tableName = "commodity_catalog",
    indices = [Index(value = ["rawKey"], unique = true)]
)
data class CommodityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawKey: String, // Normalized search key, e.g. "amul butter"
    val canonicalName: String, // e.g. "Butter"
    val brand: String, // e.g. "Amul"
    val category: String, // e.g. "Dairy"
    val subcategory: String = "", // e.g. "Spreads & Fats"
    val defaultQuantity: Double = 1.0,
    val normalizedUnit: String = "g", // g, kg, L, ml, pcs, pack, unit
    val estimatedShelfLifeDays: Int = 180,
    val storageType: String = "Refrigerated", // Pantry, Refrigerated, Frozen
    val lastKnownPrice: Double = 0.0,
    val useCount: Int = 1,
    val isPreSeeded: Boolean = false,
    val stockQuantity: Double = 50.0,
    val costPrice: Double = 0.0, // Wholesale / Inward Cost
    val sellingPrice: Double = lastKnownPrice, // Retail Price
    val reorderThreshold: Double = 10.0, // Alert when stock <= threshold
    val sku: String = "" // Barcode / SKU code
)

data class CommodityPricePoint(
    val dateTimestamp: Long,
    val vendor: String,
    val unitPrice: Double,
    val unit: String
)

data class CommoditySummary(
    val name: String,
    val category: String,
    val currentAvgPrice: Double,
    val lowestPrice: Double,
    val highestPrice: Double,
    val priceDeltaPercent: Double,
    val priceHistory: List<CommodityPricePoint>,
    val canonicalName: String = name,
    val brand: String = "",
    val storageType: String = "Pantry",
    val currentStock: Double = 0.0,
    val costPrice: Double = 0.0,
    val sellingPrice: Double = currentAvgPrice,
    val reorderThreshold: Double = 10.0,
    val isLowStock: Boolean = false
)

data class ParsedNlpItem(
    val name: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val price: Double,
    val vendor: String,
    val canonicalName: String = name,
    val brand: String = "",
    val subcategory: String = "",
    val storageType: String = "Pantry", // Pantry, Refrigerated, Frozen
    val shelfLifeDays: Int = 30,
    val tierResolved: String = "TIER_3_GEMINI", // TIER_1_CACHE, TIER_2_SEEDED, TIER_3_GEMINI, TIER_FALLBACK
    val isRestockAction: Boolean = false,
    val costPrice: Double = 0.0
)

data class VoiceStructuredFinancialEntry(
    val vendor: String = "Store Customer",
    val primaryCategory: String = "Groceries",
    val paymentMethod: String = "UPI Instant",
    val notes: String = "",
    val items: List<ParsedNlpItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val confidence: Float = 0.95f,
    val isRestockCommand: Boolean = false,
    val customerName: String = ""
)

data class PosCartItem(
    val commodityId: Long = 0,
    val name: String,
    val canonicalName: String = name,
    val brand: String = "",
    val category: String = "General",
    val unitPrice: Double,
    val costPrice: Double = 0.0,
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val availableStock: Double = 99.0
) {
    val lineTotal: Double get() = unitPrice * quantity
    val lineProfit: Double get() = (unitPrice - costPrice) * quantity
    val marginPercent: Double get() = if (unitPrice > 0) ((unitPrice - costPrice) / unitPrice) * 100.0 else 0.0
}
