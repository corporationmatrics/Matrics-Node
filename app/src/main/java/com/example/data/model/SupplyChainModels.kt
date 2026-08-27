package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Supplier & Wholesale Distributor Entity
 */
@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val contactPerson: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val gstin: String = "",
    val paymentTerms: String = "Net 15 Days", // Net 15 Days, Net 30 Days, COD, Advance UPI
    val leadTimeDays: Int = 2,
    val rating: Float = 4.8f,
    val outstandingPayable: Double = 0.0,
    val notes: String = ""
)

/**
 * Purchase Order (PO) Header Entity for Inward Supply Chain Procurement
 */
@Entity(
    tableName = "purchase_orders",
    indices = [Index(value = ["orderNumber"], unique = true)]
)
data class PurchaseOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderNumber: String, // e.g. PO-2026-0042
    val supplierId: Long,
    val supplierName: String,
    val status: String = "ORDERED", // DRAFT, ORDERED, IN_TRANSIT, RECEIVED_GRN, CANCELLED
    val orderDateTimestamp: Long = System.currentTimeMillis(),
    val expectedDeliveryTimestamp: Long = System.currentTimeMillis() + (2 * 86_400_000L),
    val receivedDateTimestamp: Long? = null,
    val totalAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val paymentStatus: String = "UNPAID", // UNPAID, PARTIALLY_PAID, PAID
    val grnNumber: String = "", // Goods Received Note e.g. GRN-8821
    val shippingNotes: String = ""
)

/**
 * Line Item in a Purchase Order
 */
@Entity(
    tableName = "purchase_order_items",
    indices = [Index(value = ["purchaseOrderId"]), Index(value = ["commodityId"])]
)
data class PurchaseOrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseOrderId: Long,
    val commodityId: Long,
    val itemName: String,
    val brand: String = "",
    val orderedQuantity: Double = 1.0,
    val receivedQuantity: Double = 0.0,
    val unit: String = "pcs",
    val unitCostPrice: Double = 0.0,
    val lineTotal: Double = 0.0,
    val batchNumber: String = "",
    val expiryDateTimestamp: Long = 0L
)

/**
 * Stock Movement Ledger & Audit Trail
 */
@Entity(
    tableName = "stock_movements",
    indices = [Index(value = ["commodityId"]), Index(value = ["timestamp"])]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val commodityId: Long,
    val commodityName: String,
    val changeQuantity: Double, // + for inward, - for outward
    val previousStock: Double,
    val newStock: Double,
    val movementType: String, // PURCHASE_GRN, POS_SALE, MANUAL_CORRECTION, DAMAGED_WRITE_OFF, RETURN_INWARD, INITIAL_STOCK
    val referenceId: String = "", // PO number or invoice number
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val unit: String = "pcs"
)

/**
 * Batch & Expiry FEFO Tracking Entity
 */
@Entity(
    tableName = "inventory_batches",
    indices = [Index(value = ["commodityId"]), Index(value = ["expiryDateTimestamp"])]
)
data class BatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val commodityId: Long,
    val commodityName: String,
    val batchNumber: String,
    val quantity: Double,
    val unit: String = "pcs",
    val costPrice: Double,
    val sellingPrice: Double,
    val mfgDateTimestamp: Long = System.currentTimeMillis(),
    val expiryDateTimestamp: Long = System.currentTimeMillis() + (180 * 86_400_000L),
    val supplierName: String = "",
    val receivedDateTimestamp: Long = System.currentTimeMillis(),
    val isDepleted: Boolean = false
)

/**
 * Supply Chain Analytics Domain Model
 */
data class SupplyChainAnalytics(
    val totalStockValuation: Double = 0.0,
    val totalCostValuation: Double = 0.0,
    val potentialProfitValuation: Double = 0.0,
    val totalSkus: Int = 0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val totalSuppliersCount: Int = 0,
    val pendingPurchaseOrdersCount: Int = 0,
    val pendingPoAmount: Double = 0.0,
    val totalAccountsPayable: Double = 0.0,
    val stockoutRiskItems: List<StockoutRiskItem> = emptyList(),
    val nearExpiryBatches: List<BatchEntity> = emptyList(),
    val expiredBatches: List<BatchEntity> = emptyList(),
    val abcAnalysisItems: List<AbcClassificationItem> = emptyList(),
    val deadStockItems: List<DeadStockItem> = emptyList(),
    val highVelocityItems: List<HighVelocityItem> = emptyList(),
    val totalDeadStockValuation: Double = 0.0
)

data class DeadStockItem(
    val commodityId: Long,
    val name: String,
    val category: String,
    val currentStock: Double,
    val unit: String,
    val unitCostPrice: Double,
    val lockedCapital: Double,
    val daysSinceLastSale: Int,
    val recommendedAction: String,
    val discountRecommendationPercent: Int = 25
)

data class HighVelocityItem(
    val commodityId: Long,
    val name: String,
    val category: String,
    val currentStock: Double,
    val unit: String,
    val dailyVelocity: Double,
    val daysUntilStockout: Double,
    val unitsSold30d: Double,
    val revenue30d: Double,
    val turnoverRateScore: Double
)

data class StockoutRiskItem(
    val commodityId: Long,
    val name: String,
    val currentStock: Double,
    val unit: String,
    val dailyBurnRate: Double,
    val daysOfInventoryOnHand: Double,
    val recommendedReorderQty: Double,
    val urgencyLevel: String // CRITICAL, WARNING, SAFE
)

data class AbcClassificationItem(
    val commodityId: Long,
    val name: String,
    val category: String,
    val totalRevenue: Double,
    val revenueSharePercent: Double,
    val cumulativeSharePercent: Double,
    val classificationClass: String // "A" (Top 80%), "B" (Next 15%), "C" (Bottom 5%)
)

data class GrnReceiptItemInput(
    val commodityId: Long,
    val itemName: String,
    val receivedQuantity: Double,
    val unitCost: Double,
    val batchNumber: String,
    val expiryDateTimestamp: Long,
    val unit: String
)
