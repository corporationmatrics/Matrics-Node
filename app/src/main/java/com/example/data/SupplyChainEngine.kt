package com.example.data

import com.example.data.model.AbcClassificationItem
import com.example.data.model.BatchEntity
import com.example.data.model.CommodityEntity
import com.example.data.model.DeadStockItem
import com.example.data.model.HighVelocityItem
import com.example.data.model.LineItemEntity
import com.example.data.model.PurchaseOrderEntity
import com.example.data.model.PurchaseOrderItemEntity
import com.example.data.model.StockoutRiskItem
import com.example.data.model.SupplierEntity
import com.example.data.model.SupplyChainAnalytics
import com.example.data.model.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Intelligent Supply Chain & Inventory Calculation Engine.
 * Handles algorithmic demand forecasting, ABC Pareto analysis, FEFO batch expiry alerts,
 * dead-stock vs high-velocity analytics, and accounts payable tracking.
 */
object SupplyChainEngine {

    /**
     * Computes holistic Supply Chain Analytics & Forecasting KPIs.
     */
    fun computeSupplyChainAnalytics(
        commodities: List<CommodityEntity>,
        transactions: List<TransactionEntity>,
        lineItems: List<LineItemEntity>,
        batches: List<BatchEntity>,
        purchaseOrders: List<PurchaseOrderEntity>,
        suppliers: List<SupplierEntity>
    ): SupplyChainAnalytics {
        val totalValuation = commodities.sumOf { (if (it.sellingPrice > 0) it.sellingPrice else it.lastKnownPrice) * it.stockQuantity }
        val totalCostValuation = commodities.sumOf { (if (it.costPrice > 0) it.costPrice else (if (it.sellingPrice > 0) it.sellingPrice * 0.75 else it.lastKnownPrice * 0.75)) * it.stockQuantity }
        val potentialProfitValuation = (totalValuation - totalCostValuation).coerceAtLeast(0.0)

        val lowStockCount = commodities.count { it.stockQuantity <= it.reorderThreshold && it.stockQuantity > 0 }
        val outOfStockCount = commodities.count { it.stockQuantity <= 0 }

        val pendingPOs = purchaseOrders.filter { it.status != "RECEIVED_GRN" && it.status != "CANCELLED" }
        val pendingPoAmount = pendingPOs.sumOf { it.totalAmount }
        val totalAccountsPayable = suppliers.sumOf { it.outstandingPayable }

        // 1. Stockout Risk Assessment
        val stockoutRisks = calculateStockoutRisks(commodities, transactions, lineItems)

        // 2. ABC Classification (Pareto 80/15/5 revenue split)
        val abcItems = calculateAbcAnalysis(commodities, transactions, lineItems)

        // 3. Batch Expiry Classification (FEFO)
        val now = System.currentTimeMillis()
        val thirtyDaysFromNow = now + (30L * 86_400_000L)
        val expiredBatches = batches.filter { !it.isDepleted && it.expiryDateTimestamp < now }
        val nearExpiryBatches = batches.filter { !it.isDepleted && it.expiryDateTimestamp >= now && it.expiryDateTimestamp <= thirtyDaysFromNow }

        // 4. Dead-Stock vs High-Velocity Insights
        val deadStock = calculateDeadStock(commodities, transactions, lineItems)
        val highVelocity = calculateHighVelocity(commodities, transactions, lineItems)
        val totalDeadStockValuation = deadStock.sumOf { it.lockedCapital }

        return SupplyChainAnalytics(
            totalStockValuation = totalValuation,
            totalCostValuation = totalCostValuation,
            potentialProfitValuation = potentialProfitValuation,
            totalSkus = commodities.size,
            lowStockCount = lowStockCount,
            outOfStockCount = outOfStockCount,
            totalSuppliersCount = suppliers.size,
            pendingPurchaseOrdersCount = pendingPOs.size,
            pendingPoAmount = pendingPoAmount,
            totalAccountsPayable = totalAccountsPayable,
            stockoutRiskItems = stockoutRisks,
            nearExpiryBatches = nearExpiryBatches,
            expiredBatches = expiredBatches,
            abcAnalysisItems = abcItems,
            deadStockItems = deadStock,
            highVelocityItems = highVelocity,
            totalDeadStockValuation = totalDeadStockValuation
        )
    }

    /**
     * Calculates Stockout Risk and Days of Inventory on Hand (DOH) using past 30 days sales velocity.
     */
    fun calculateStockoutRisks(
        commodities: List<CommodityEntity>,
        transactions: List<TransactionEntity>,
        lineItems: List<LineItemEntity>
    ): List<StockoutRiskItem> {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 86_400_000L)

        val recentLineItems = lineItems.filter { item ->
            val parentTx = transactions.find { it.id == item.transactionId }
            parentTx != null && parentTx.dateTimestamp >= thirtyDaysAgo
        }

        val itemSalesMap = recentLineItems.groupBy { it.name.trim().lowercase() }
            .mapValues { (_, items) -> items.sumOf { it.quantity } }

        return commodities.mapNotNull { comm ->
            val soldIn30Days = itemSalesMap[comm.canonicalName.trim().lowercase()]
                ?: itemSalesMap[comm.rawKey.trim().lowercase()]
                ?: (if (comm.useCount > 5) comm.useCount.toDouble() else 1.5)

            val dailyBurnRate = (soldIn30Days / 30.0).coerceAtLeast(0.1)
            val daysOnHand = comm.stockQuantity / dailyBurnRate

            val annualDemand = dailyBurnRate * 365.0
            val orderCost = 100.0
            val unitPrice = if (comm.costPrice > 0) comm.costPrice else comm.sellingPrice * 0.75
            val holdingCost = (unitPrice * 0.20).coerceAtLeast(1.0)
            val rawEoq = sqrt((2 * annualDemand * orderCost) / holdingCost)
            val recommendedReorder = (rawEoq.coerceIn(10.0, 500.0)).roundToInt().toDouble()

            val urgency = when {
                comm.stockQuantity <= 0 -> "CRITICAL"
                daysOnHand <= 3.0 || comm.stockQuantity <= comm.reorderThreshold -> "CRITICAL"
                daysOnHand <= 7.0 -> "WARNING"
                else -> "SAFE"
            }

            if (urgency != "SAFE" || comm.stockQuantity <= comm.reorderThreshold) {
                StockoutRiskItem(
                    commodityId = comm.id,
                    name = comm.canonicalName,
                    currentStock = comm.stockQuantity,
                    unit = comm.normalizedUnit,
                    dailyBurnRate = (dailyBurnRate * 10).roundToInt() / 10.0,
                    daysOfInventoryOnHand = (daysOnHand * 10).roundToInt() / 10.0,
                    recommendedReorderQty = recommendedReorder,
                    urgencyLevel = urgency
                )
            } else {
                null
            }
        }.sortedBy { it.daysOfInventoryOnHand }
    }

    /**
     * Calculates Dead-Stock SKUs: Stagnant goods with zero or negligible sales in 30+ days that lock up working capital.
     */
    fun calculateDeadStock(
        commodities: List<CommodityEntity>,
        transactions: List<TransactionEntity>,
        lineItems: List<LineItemEntity>
    ): List<DeadStockItem> {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 86_400_000L)

        // Map recent transactions by item canonical name
        val itemRecentTxTimes = mutableMapOf<String, Long>()
        lineItems.forEach { item ->
            val tx = transactions.find { it.id == item.transactionId }
            if (tx != null) {
                val key = item.name.trim().lowercase()
                val prev = itemRecentTxTimes[key] ?: 0L
                if (tx.dateTimestamp > prev) {
                    itemRecentTxTimes[key] = tx.dateTimestamp
                }
            }
        }

        val now = System.currentTimeMillis()
        val deadStockList = mutableListOf<DeadStockItem>()

        for (comm in commodities) {
            if (comm.stockQuantity <= 0) continue // Out of stock is not dead stock

            val lastTxTime = itemRecentTxTimes[comm.canonicalName.trim().lowercase()]
                ?: itemRecentTxTimes[comm.rawKey.trim().lowercase()]
                ?: 0L

            val isIdle30d = lastTxTime < thirtyDaysAgo || lastTxTime == 0L
            val daysIdle = if (lastTxTime > 0) ((now - lastTxTime) / 86_400_000L).toInt() else 45

            if (isIdle30d && (daysIdle >= 25 || comm.useCount <= 2)) {
                val unitCost = if (comm.costPrice > 0) comm.costPrice else comm.sellingPrice * 0.70
                val lockedCapital = comm.stockQuantity * unitCost

                val action = when {
                    daysIdle >= 60 -> "Run 30% Flash Markdown Sale"
                    daysIdle >= 40 -> "Bundle Promo with Hero SKU"
                    else -> "Apply 15% Clearance Discount"
                }
                val discountPct = when {
                    daysIdle >= 60 -> 35
                    daysIdle >= 40 -> 25
                    else -> 15
                }

                deadStockList.add(
                    DeadStockItem(
                        commodityId = comm.id,
                        name = comm.canonicalName,
                        category = comm.category,
                        currentStock = comm.stockQuantity,
                        unit = comm.normalizedUnit,
                        unitCostPrice = unitCost,
                        lockedCapital = lockedCapital,
                        daysSinceLastSale = daysIdle,
                        recommendedAction = action,
                        discountRecommendationPercent = discountPct
                    )
                )
            }
        }

        return deadStockList.sortedByDescending { it.lockedCapital }
    }

    /**
     * Calculates High-Velocity / Fast-Moving Goods: SKUs with high turnover rate and daily sales velocity.
     */
    fun calculateHighVelocity(
        commodities: List<CommodityEntity>,
        transactions: List<TransactionEntity>,
        lineItems: List<LineItemEntity>
    ): List<HighVelocityItem> {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 86_400_000L)

        val recentLineItems = lineItems.filter { item ->
            val parentTx = transactions.find { it.id == item.transactionId }
            parentTx != null && parentTx.dateTimestamp >= thirtyDaysAgo
        }

        val itemSalesMap = recentLineItems.groupBy { it.name.trim().lowercase() }
        val highVelocityList = mutableListOf<HighVelocityItem>()

        for (comm in commodities) {
            val key = comm.canonicalName.trim().lowercase()
            val rawKey = comm.rawKey.trim().lowercase()
            val matchingItems = itemSalesMap[key] ?: itemSalesMap[rawKey] ?: emptyList()

            val unitsSold = matchingItems.sumOf { it.quantity }.let { if (it > 0) it else (comm.useCount * 1.5).coerceAtLeast(3.0) }
            val revenue = matchingItems.sumOf { it.totalPrice }.let { if (it > 0) it else unitsSold * comm.sellingPrice }
            val dailyVelocity = unitsSold / 30.0
            val daysUntilStockout = if (dailyVelocity > 0) comm.stockQuantity / dailyVelocity else 99.0
            val turnoverScore = if (comm.stockQuantity > 0) (unitsSold / (comm.stockQuantity + unitsSold)) * 100.0 else 90.0

            if (dailyVelocity >= 0.8 || comm.useCount >= 5 || unitsSold >= 15.0) {
                highVelocityList.add(
                    HighVelocityItem(
                        commodityId = comm.id,
                        name = comm.canonicalName,
                        category = comm.category,
                        currentStock = comm.stockQuantity,
                        unit = comm.normalizedUnit,
                        dailyVelocity = (dailyVelocity * 10).roundToInt() / 10.0,
                        daysUntilStockout = (daysUntilStockout * 10).roundToInt() / 10.0,
                        unitsSold30d = unitsSold,
                        revenue30d = revenue,
                        turnoverRateScore = (turnoverScore * 10).roundToInt() / 10.0
                    )
                )
            }
        }

        return highVelocityList.sortedByDescending { it.dailyVelocity }
    }

    /**
     * ABC Analysis (Pareto Classification):
     * Class A: Top 80% revenue
     * Class B: Next 15% revenue
     * Class C: Bottom 5% revenue
     */
    fun calculateAbcAnalysis(
        commodities: List<CommodityEntity>,
        transactions: List<TransactionEntity>,
        lineItems: List<LineItemEntity>
    ): List<AbcClassificationItem> {
        val totalRevenueByItem = lineItems.groupBy { it.name.trim().lowercase() }
            .mapValues { (_, items) -> items.sumOf { it.totalPrice } }

        val totalSystemRevenue = totalRevenueByItem.values.sum().coerceAtLeast(1.0)

        val rankedList = commodities.map { comm ->
            val itemRev = totalRevenueByItem[comm.canonicalName.trim().lowercase()]
                ?: totalRevenueByItem[comm.rawKey.trim().lowercase()]
                ?: (comm.sellingPrice * comm.useCount.coerceAtLeast(1))
            val share = (itemRev / totalSystemRevenue) * 100.0
            Triple(comm, itemRev, share)
        }.sortedByDescending { it.second }

        var cumulative = 0.0
        return rankedList.map { (comm, rev, share) ->
            cumulative += share
            val classification = when {
                cumulative <= 80.0 -> "A"
                cumulative <= 95.0 -> "B"
                else -> "C"
            }

            AbcClassificationItem(
                commodityId = comm.id,
                name = comm.canonicalName,
                category = comm.category,
                totalRevenue = rev,
                revenueSharePercent = (share * 10).roundToInt() / 10.0,
                cumulativeSharePercent = (cumulative * 10).roundToInt() / 10.0,
                classificationClass = classification
            )
        }
    }

    /**
     * Formats a Purchase Order into a clean text document suitable for WhatsApp, Email, or SMS to Suppliers.
     */
    fun formatPurchaseOrderShareText(
        po: PurchaseOrderEntity,
        items: List<PurchaseOrderItemEntity>,
        storeName: String = "CYPHR RETAIL & STORES",
        storePhone: String = "+91 98765 43210"
    ): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        val orderDateStr = dateFormat.format(Date(po.orderDateTimestamp))
        val expDateStr = dateFormat.format(Date(po.expectedDeliveryTimestamp))

        val sb = StringBuilder()
        sb.append("📋 *PURCHASE ORDER: ${po.orderNumber}*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🏬 *Buyer:* $storeName\n")
        sb.append("📞 *Contact:* $storePhone\n")
        sb.append("🚚 *Supplier:* ${po.supplierName}\n")
        sb.append("📅 *PO Date:* $orderDateStr\n")
        sb.append("⏰ *Req Delivery:* $expDateStr\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")
        sb.append("*ORDERED ITEMS:*\n")

        items.forEachIndexed { index, item ->
            val brandStr = if (item.brand.isNotBlank()) " (${item.brand})" else ""
            sb.append("${index + 1}. *${item.itemName}$brandStr*\n")
            sb.append("   • Qty: ${item.orderedQuantity.toInt()} ${item.unit} @ ₹${item.unitCostPrice.toInt()}/${item.unit}\n")
            sb.append("   • Subtotal: ₹${item.lineTotal.toInt()}\n")
        }

        sb.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        if (po.taxAmount > 0) {
            sb.append("GST / Tax: ₹${po.taxAmount.toInt()}\n")
        }
        sb.append("💰 *TOTAL ESTIMATED VALUE: ₹${po.totalAmount.toInt()}*\n")
        if (po.shippingNotes.isNotBlank()) {
            sb.append("📝 *Delivery Instructions:* ${po.shippingNotes}\n")
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("Please confirm receipt and dispatch schedule.")

        return sb.toString()
    }
}
