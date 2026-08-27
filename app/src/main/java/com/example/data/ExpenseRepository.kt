package com.example.data

import com.example.data.dao.ExpenseDao
import com.example.data.dao.RecurringBillDao
import com.example.data.dao.SavingsAndKhataDao
import com.example.data.dao.SupplyChainDao
import com.example.data.model.BatchEntity
import com.example.data.model.CommodityEntity
import com.example.data.model.CommodityPricePoint
import com.example.data.model.CommoditySummary
import com.example.data.model.GrnReceiptItemInput
import com.example.data.model.GroceryItemEntity
import com.example.data.model.KhataEntryEntity
import com.example.data.model.LineItemEntity
import com.example.data.model.ParsedNlpItem
import com.example.data.model.PurchaseOrderEntity
import com.example.data.model.PurchaseOrderItemEntity
import com.example.data.model.RecurringBillEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.StockMovementEntity
import com.example.data.model.SupplierEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val recurringBillDao: RecurringBillDao? = null,
    private val savingsAndKhataDao: SavingsAndKhataDao? = null,
    private val supplyChainDao: SupplyChainDao? = null
) {

    val commodityEngine = CommodityEngine(expenseDao)

    val allTransactions: Flow<List<TransactionEntity>> = expenseDao.getAllTransactions()
    val allLineItems: Flow<List<LineItemEntity>> = expenseDao.getAllLineItems()
    val wishlistItems: Flow<List<GroceryItemEntity>> = expenseDao.getWishlistItems()
    val pantryItems: Flow<List<GroceryItemEntity>> = expenseDao.getPantryItems()
    val allCommodities: Flow<List<CommodityEntity>> = expenseDao.getAllCommodities()
    val lowStockCommodities: Flow<List<CommodityEntity>> = expenseDao.getLowStockCommodities()

    // Supply Chain Flows
    val allSuppliers: Flow<List<SupplierEntity>> = supplyChainDao?.getAllSuppliers() ?: emptyFlow()
    val allPurchaseOrders: Flow<List<PurchaseOrderEntity>> = supplyChainDao?.getAllPurchaseOrders() ?: emptyFlow()
    val allStockMovements: Flow<List<StockMovementEntity>> = supplyChainDao?.getStockMovements(150) ?: emptyFlow()
    val allBatches: Flow<List<BatchEntity>> = supplyChainDao?.getActiveBatches() ?: emptyFlow()

    // Storefront POS Sale Execution
    suspend fun executePosSale(
        items: List<com.example.data.model.PosCartItem>,
        customerName: String = "",
        customerPhone: String = "",
        paymentMethod: String = "UPI / QR",
        taxPercent: Double = 0.0,
        discountPercent: Double = 0.0,
        storeName: String = "Matrics Node Storefront"
    ): Long {
        if (items.isEmpty()) return -1L

        val subtotal = items.sumOf { it.lineTotal }
        val discountAmount = subtotal * (discountPercent / 100.0)
        val afterDiscount = (subtotal - discountAmount).coerceAtLeast(0.0)
        val taxAmount = afterDiscount * (taxPercent / 100.0)
        val netTotal = afterDiscount + taxAmount
        val now = System.currentTimeMillis()
        val invoiceNo = "INV-${(now % 1000000).toString().padStart(6, '0')}"

        val txEntity = TransactionEntity(
            title = if (customerName.isNotBlank()) "Sale: $customerName" else "Counter Retail Sale",
            vendor = storeName,
            category = items.firstOrNull()?.category ?: "Retail",
            totalAmount = netTotal,
            dateTimestamp = now,
            paymentMethod = paymentMethod,
            locationName = "POS Terminal 01",
            itemCount = items.size,
            rawVoicePrompt = "POS Sale: ${items.size} items, Total: ₹${netTotal.toInt()}",
            isVerified = true,
            transactionType = "SALE",
            customerName = customerName,
            customerPhone = customerPhone,
            subtotalAmount = subtotal,
            taxAmount = taxAmount,
            discountAmount = discountAmount,
            invoiceNumber = invoiceNo,
            orderStatus = if (paymentMethod.equals("KHATA", ignoreCase = true)) "PENDING_KHATA" else "SETTLED"
        )

        val txId = expenseDao.insertTransaction(txEntity)

        val lineItems = items.map { item ->
            LineItemEntity(
                transactionId = txId,
                name = item.name,
                category = item.category,
                quantity = item.quantity,
                unit = item.unit,
                unitPrice = item.unitPrice,
                totalPrice = item.lineTotal,
                vendor = storeName,
                dateTimestamp = now,
                canonicalName = item.canonicalName,
                brand = item.brand,
                storageType = "Storefront",
                shelfLifeDays = 30,
                costPrice = item.costPrice,
                sku = if (item.commodityId > 0) "SKU-${item.commodityId}" else ""
            )
        }

        expenseDao.insertLineItems(lineItems)

        // Decrement live inventory stock for each sold item and record audit movement
        for (item in items) {
            val targetId = if (item.commodityId > 0) item.commodityId else {
                val key = item.name.trim().lowercase()
                expenseDao.getCommodityByKey(key)?.id ?: 0L
            }
            if (targetId > 0) {
                val prevStock = item.availableStock
                val newStock = (prevStock - item.quantity).coerceAtLeast(0.0)
                expenseDao.decrementCommodityStock(targetId, item.quantity)
                supplyChainDao?.insertStockMovement(
                    StockMovementEntity(
                        commodityId = targetId,
                        commodityName = item.name,
                        changeQuantity = -item.quantity,
                        previousStock = prevStock,
                        newStock = newStock,
                        movementType = "POS_SALE",
                        referenceId = invoiceNo,
                        notes = "POS Checkout Counter: $paymentMethod",
                        timestamp = now,
                        unit = item.unit
                    )
                )
            }
        }

        // If Payment Method is Khata (Store Credit), automatically record under customer receivables
        if (paymentMethod.equals("KHATA", ignoreCase = true) && customerName.isNotBlank()) {
            savingsAndKhataDao?.insertKhataEntry(
                KhataEntryEntity(
                    personName = customerName,
                    personPhoneOrUpi = customerPhone.ifBlank { "Customer Store Credit" },
                    type = "YOU_WILL_GET",
                    amount = netTotal,
                    description = "POS Invoice #$invoiceNo (${items.size} items)",
                    dateTimestamp = now,
                    dueDateTimestamp = now + (15L * 86_400_000L),
                    isSettled = false
                )
            )
        }

        return txId
    }

    // Bidirectional Restocking Execution
    suspend fun executeRestock(
        items: List<ParsedNlpItem>,
        supplierName: String = "Wholesale Distributor",
        paymentMethod: String = "Bank Transfer"
    ): Long {
        if (items.isEmpty()) return -1L
        val now = System.currentTimeMillis()
        val totalCost = items.sumOf { (if (it.costPrice > 0) it.costPrice else it.price * 0.78) * it.quantity }

        val txId = expenseDao.insertTransaction(
            TransactionEntity(
                title = "Inward Restock: $supplierName",
                vendor = supplierName,
                category = "Inventory Restock",
                totalAmount = totalCost,
                dateTimestamp = now,
                paymentMethod = paymentMethod,
                locationName = "Store Warehouse",
                itemCount = items.size,
                rawVoicePrompt = "Restocked ${items.size} commodities",
                isVerified = true,
                transactionType = "RESTOCK",
                customerName = supplierName,
                orderStatus = "SETTLED"
            )
        )

        // Increment or Insert commodities in catalog
        for (item in items) {
            val key = item.name.trim().lowercase()
            val existing = expenseDao.getCommodityByKey(key)
            val effectiveCost = if (item.costPrice > 0) item.costPrice else item.price * 0.78
            val effectiveSell = if (item.price > 0) item.price else effectiveCost * 1.25

            if (existing != null) {
                expenseDao.incrementCommodityStock(existing.id, item.quantity)
                expenseDao.updateCommodityPricing(existing.id, effectiveCost, effectiveSell)
            } else {
                expenseDao.insertCommodity(
                    CommodityEntity(
                        rawKey = key,
                        canonicalName = item.canonicalName.ifBlank { item.name },
                        brand = item.brand,
                        category = item.category,
                        subcategory = item.subcategory,
                        defaultQuantity = item.quantity,
                        normalizedUnit = item.unit,
                        estimatedShelfLifeDays = item.shelfLifeDays,
                        storageType = item.storageType,
                        lastKnownPrice = effectiveSell,
                        useCount = 1,
                        isPreSeeded = false,
                        stockQuantity = item.quantity,
                        costPrice = effectiveCost,
                        sellingPrice = effectiveSell,
                        reorderThreshold = 8.0,
                        sku = "SKU-RESTOCK-${(now % 10000)}"
                    )
                )
            }
        }

        return txId
    }

    suspend fun updateCommodityStock(id: Long, newStock: Double) {
        expenseDao.updateCommodityStock(id, newStock.coerceAtLeast(0.0))
    }

    suspend fun quickRestockCommodity(id: Long, addQty: Double) {
        expenseDao.incrementCommodityStock(id, addQty)
    }

    suspend fun updateCommodityPricing(id: Long, costPrice: Double, sellingPrice: Double) {
        expenseDao.updateCommodityPricing(id, costPrice, sellingPrice)
    }

    suspend fun updateCommodityReorderThreshold(id: Long, threshold: Double) {
        expenseDao.updateCommodityReorderThreshold(id, threshold)
    }

    suspend fun insertCommodityDirect(commodity: CommodityEntity): Long {
        return expenseDao.insertCommodity(commodity)
    }

    suspend fun deleteCommodity(id: Long) {
        expenseDao.deleteCommodity(id)
    }

    // Recurring Bills & Subscriptions
    val allRecurringBills: Flow<List<RecurringBillEntity>> =
        recurringBillDao?.getAllRecurringBills() ?: emptyFlow()
    val activeRecurringBills: Flow<List<RecurringBillEntity>> =
        recurringBillDao?.getActiveRecurringBills() ?: emptyFlow()

    // Savings Goals
    val allSavingsGoals: Flow<List<SavingsGoalEntity>> =
        savingsAndKhataDao?.getAllSavingsGoals() ?: emptyFlow()

    // Khata (Ledger)
    val allKhataEntries: Flow<List<KhataEntryEntity>> =
        savingsAndKhataDao?.getAllKhataEntries() ?: emptyFlow()
    val pendingKhataEntries: Flow<List<KhataEntryEntity>> =
        savingsAndKhataDao?.getPendingKhataEntries() ?: emptyFlow()

    suspend fun resolveVoiceOrTextInput(
        rawText: String,
        apiKey: String
    ): Pair<String, List<ParsedNlpItem>> {
        return commodityEngine.resolveInput(rawText, apiKey)
    }

    suspend fun logExpenseWithItems(
        title: String,
        vendor: String,
        category: String,
        items: List<ParsedNlpItem>,
        paymentMethod: String = "UPI / Direct",
        locationName: String = "Auto-Pin",
        rawVoicePrompt: String? = null
    ): Long {
        val totalAmount = items.sumOf { it.price * it.quantity }
        val now = System.currentTimeMillis()

        val txEntity = TransactionEntity(
            title = title.ifBlank { "$vendor Purchase" },
            vendor = vendor,
            category = category.ifBlank { items.firstOrNull()?.category ?: "General" },
            totalAmount = if (totalAmount > 0) totalAmount else 100.0,
            dateTimestamp = now,
            paymentMethod = paymentMethod,
            locationName = locationName,
            itemCount = items.size.coerceAtLeast(1),
            rawVoicePrompt = rawVoicePrompt,
            isVerified = true
        )

        val txId = expenseDao.insertTransaction(txEntity)

        val lineItems = items.map { item ->
            LineItemEntity(
                transactionId = txId,
                name = item.name,
                category = item.category,
                quantity = item.quantity,
                unit = item.unit,
                unitPrice = item.price,
                totalPrice = item.price * item.quantity,
                vendor = vendor,
                dateTimestamp = now,
                canonicalName = item.canonicalName,
                brand = item.brand,
                storageType = item.storageType,
                shelfLifeDays = item.shelfLifeDays
            )
        }

        expenseDao.insertLineItems(lineItems)

        // Self-learning loop: persist newly resolved items into commodity_catalog table
        for (item in items) {
            learnCommodity(item)
        }

        return txId
    }

    suspend fun learnCommodity(item: ParsedNlpItem) {
        val key = item.name.trim().lowercase()
        if (key.isBlank()) return
        val existing = expenseDao.getCommodityByKey(key)
        if (existing != null) {
            expenseDao.incrementCommodityUsage(key, item.price)
        } else {
            expenseDao.insertCommodity(
                CommodityEntity(
                    rawKey = key,
                    canonicalName = item.canonicalName.ifBlank { item.name },
                    brand = item.brand,
                    category = item.category,
                    subcategory = item.subcategory,
                    defaultQuantity = item.quantity,
                    normalizedUnit = item.unit,
                    estimatedShelfLifeDays = item.shelfLifeDays,
                    storageType = item.storageType,
                    lastKnownPrice = item.price,
                    useCount = 1,
                    isPreSeeded = false
                )
            )
        }
    }

    suspend fun deleteTransaction(id: Long) {
        expenseDao.deleteTransaction(id)
        expenseDao.deleteLineItemsByTxId(id)
    }

    suspend fun addWishlistItem(
        name: String,
        quantity: String,
        estimatedPrice: Double,
        priceCap: Double,
        targetVendor: String,
        category: String,
        canonicalName: String = name,
        brand: String = "",
        storageType: String = "Pantry"
    ) {
        expenseDao.insertGroceryItem(
            GroceryItemEntity(
                name = name,
                quantity = quantity.ifBlank { "1 unit" },
                estimatedPrice = estimatedPrice,
                priceCap = if (priceCap > 0) priceCap else estimatedPrice,
                targetVendor = targetVendor.ifBlank { "FreshMart" },
                category = category,
                isChecked = false,
                isPantryItem = false,
                canonicalName = canonicalName,
                brand = brand,
                storageType = storageType
            )
        )
    }

    suspend fun toggleWishlistChecked(item: GroceryItemEntity) {
        expenseDao.updateGroceryItem(item.copy(isChecked = !item.isChecked))
    }

    suspend fun deleteGroceryItem(id: Long) {
        expenseDao.deleteGroceryItem(id)
    }

    suspend fun checkoutCheckedWishlistToExpense(
        vendorName: String = "Store Batch Checkout",
        paymentMethod: String = "UPI Instant"
    ): Long {
        val currentWishlist = wishlistItems.first()
        val checkedItems = currentWishlist.filter { it.isChecked }
        if (checkedItems.isEmpty()) return -1L

        val nlpItems = checkedItems.map { gItem ->
            ParsedNlpItem(
                name = gItem.name,
                category = gItem.category,
                quantity = 1.0,
                unit = gItem.quantity,
                price = if (gItem.estimatedPrice > 0) gItem.estimatedPrice else 100.0,
                vendor = gItem.targetVendor,
                canonicalName = gItem.canonicalName,
                brand = gItem.brand,
                storageType = gItem.storageType,
                shelfLifeDays = gItem.expiryDaysTotal
            )
        }

        val txId = logExpenseWithItems(
            title = "$vendorName Checkout",
            vendor = checkedItems.firstOrNull()?.targetVendor ?: vendorName,
            category = "Groceries",
            items = nlpItems,
            paymentMethod = paymentMethod,
            locationName = "Checkout Terminal",
            rawVoicePrompt = "Checklist checkout of ${checkedItems.size} items"
        )

        // Convert checked items into pantry inventory items with shelf-life tracking
        val pantryEntities = checkedItems.map { gItem ->
            GroceryItemEntity(
                name = gItem.name,
                quantity = gItem.quantity,
                estimatedPrice = gItem.estimatedPrice,
                priceCap = gItem.priceCap,
                targetVendor = gItem.targetVendor,
                category = gItem.category,
                isChecked = false,
                isPantryItem = true,
                purchaseDate = System.currentTimeMillis(),
                expiryDaysTotal = if (gItem.expiryDaysTotal > 0) gItem.expiryDaysTotal else 21,
                remainingDays = if (gItem.expiryDaysTotal > 0) gItem.expiryDaysTotal else 21,
                burnRateLevel = "NORMAL",
                lastBoughtDaysAgo = 0,
                canonicalName = gItem.canonicalName,
                brand = gItem.brand,
                storageType = gItem.storageType
            )
        }
        expenseDao.insertGroceryItems(pantryEntities)

        // Clear converted checklist items
        expenseDao.deleteCheckedWishlistItems()
        return txId
    }

    suspend fun addPantryItem(
        name: String,
        quantity: String,
        shelfLifeDays: Int,
        burnRate: String,
        canonicalName: String = name,
        brand: String = "",
        storageType: String = "Pantry"
    ) {
        expenseDao.insertGroceryItem(
            GroceryItemEntity(
                name = name,
                quantity = quantity,
                estimatedPrice = 0.0,
                priceCap = 0.0,
                targetVendor = "Home Pantry",
                category = NlpParsingEngine.inferCategory(name),
                isChecked = false,
                isPantryItem = true,
                purchaseDate = System.currentTimeMillis(),
                expiryDaysTotal = shelfLifeDays,
                remainingDays = shelfLifeDays,
                burnRateLevel = burnRate,
                lastBoughtDaysAgo = 0,
                canonicalName = canonicalName,
                brand = brand,
                storageType = storageType
            )
        )
    }

    fun getCommoditySummaries(query: String = ""): Flow<List<CommoditySummary>> {
        return allLineItems.map { items ->
            val grouped = items.groupBy { it.name.trim() }
            val summaries = mutableListOf<CommoditySummary>()

            for ((name, group) in grouped) {
                if (query.isNotBlank() && !name.contains(query, ignoreCase = true)) {
                    continue
                }

                val sortedByDate = group.sortedBy { it.dateTimestamp }
                val pricePoints = sortedByDate.map {
                    CommodityPricePoint(
                        dateTimestamp = it.dateTimestamp,
                        vendor = it.vendor,
                        unitPrice = it.unitPrice,
                        unit = it.unit
                    )
                }

                val prices = group.map { it.unitPrice }
                val lowest = prices.minOrNull() ?: 0.0
                val highest = prices.maxOrNull() ?: 0.0
                val currentAvg = prices.takeLast(3).average()

                val firstPrice = sortedByDate.firstOrNull()?.unitPrice ?: currentAvg
                val lastPrice = sortedByDate.lastOrNull()?.unitPrice ?: currentAvg
                val deltaPercent = if (firstPrice > 0) {
                    ((lastPrice - firstPrice) / firstPrice) * 100.0
                } else 0.0

                val firstItem = group.firstOrNull()
                summaries.add(
                    CommoditySummary(
                        name = name,
                        category = firstItem?.category ?: "General",
                        currentAvgPrice = currentAvg,
                        lowestPrice = lowest,
                        highestPrice = highest,
                        priceDeltaPercent = deltaPercent,
                        priceHistory = pricePoints,
                        canonicalName = firstItem?.canonicalName ?: name,
                        brand = firstItem?.brand ?: "",
                        storageType = firstItem?.storageType ?: "Pantry"
                    )
                )
            }
            summaries.sortedByDescending { it.priceHistory.size }
        }
    }

    // --- Recurring Bills & Subscription Reminders Management ---
    suspend fun addRecurringBill(bill: RecurringBillEntity): Long {
        return recurringBillDao?.insertBill(bill) ?: 0L
    }

    suspend fun updateRecurringBill(bill: RecurringBillEntity) {
        recurringBillDao?.updateBill(bill)
    }

    suspend fun deleteRecurringBill(id: Long) {
        recurringBillDao?.deleteBill(id)
    }

    suspend fun markBillPaid(id: Long, createExpenseTransaction: Boolean = true): Boolean {
        val bill = recurringBillDao?.getBillById(id) ?: return false
        val now = System.currentTimeMillis()
        val nextDue = calculateNextDueDate(bill.billingCycle, now)
        recurringBillDao.updatePaymentStatus(id, "PAID_THIS_CYCLE", now, nextDue)

        if (createExpenseTransaction) {
            logExpenseWithItems(
                title = "${bill.title} (Bill Paid)",
                vendor = bill.title,
                category = bill.category,
                items = listOf(
                    ParsedNlpItem(
                        name = bill.title,
                        category = bill.category,
                        quantity = 1.0,
                        unit = "cycle",
                        price = bill.amount,
                        vendor = bill.title,
                        brand = bill.serviceIcon
                    )
                ),
                paymentMethod = bill.paymentMethod,
                locationName = "Auto-Bill Pay",
                rawVoicePrompt = "Recurring Bill: ${bill.title}"
            )
        }
        return true
    }

    private fun calculateNextDueDate(cycle: String, fromDate: Long): Long {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = fromDate }
        when (cycle.uppercase()) {
            "WEEKLY" -> cal.add(java.util.Calendar.DAY_OF_YEAR, 7)
            "QUARTERLY" -> cal.add(java.util.Calendar.MONTH, 3)
            "ANNUAL" -> cal.add(java.util.Calendar.YEAR, 1)
            else -> cal.add(java.util.Calendar.MONTH, 1) // MONTHLY
        }
        return cal.timeInMillis
    }

    // --- Savings Goals Management ---
    suspend fun addSavingsGoal(goal: SavingsGoalEntity): Long {
        return savingsAndKhataDao?.insertSavingsGoal(goal) ?: 0L
    }

    suspend fun updateSavingsGoal(goal: SavingsGoalEntity) {
        savingsAndKhataDao?.updateSavingsGoal(goal)
    }

    suspend fun deleteSavingsGoal(id: Long) {
        savingsAndKhataDao?.deleteSavingsGoal(id)
    }

    suspend fun depositToSavingsGoal(id: Long, amount: Double) {
        val goal = savingsAndKhataDao?.getSavingsGoalById(id) ?: return
        val newAmount = (goal.currentAmount + amount).coerceAtLeast(0.0)
        savingsAndKhataDao.updateGoalAmount(id, newAmount)
    }

    suspend fun withdrawFromSavingsGoal(id: Long, amount: Double) {
        val goal = savingsAndKhataDao?.getSavingsGoalById(id) ?: return
        val newAmount = (goal.currentAmount - amount).coerceAtLeast(0.0)
        savingsAndKhataDao.updateGoalAmount(id, newAmount)
    }

    // --- Khata & Debt Ledger Management ---
    suspend fun getKhataEntryById(id: Long): KhataEntryEntity? {
        return savingsAndKhataDao?.getKhataEntryById(id)
    }

    suspend fun addKhataEntry(entry: KhataEntryEntity): Long {
        return savingsAndKhataDao?.insertKhataEntry(entry) ?: 0L
    }

    suspend fun updateKhataEntry(entry: KhataEntryEntity) {
        savingsAndKhataDao?.updateKhataEntry(entry)
    }

    suspend fun deleteKhataEntry(id: Long) {
        savingsAndKhataDao?.deleteKhataEntry(id)
    }

    suspend fun settleKhataEntry(id: Long, paymentMode: String = "CASH", note: String = "Full settlement") {
        val now = System.currentTimeMillis()
        val dao = savingsAndKhataDao ?: return
        val entry = dao.getKhataEntryById(id)
        if (entry != null) {
            val remaining = entry.remainingAmount
            if (remaining > 0) {
                recordPartialKhataPayment(
                    khataId = id,
                    installmentAmount = remaining,
                    paymentMode = paymentMode,
                    utrNumber = "",
                    note = note
                )
            } else {
                dao.settleKhataEntry(id, now)
            }
        } else {
            dao.settleKhataEntry(id, now)
        }
    }

    suspend fun recordPartialKhataPayment(
        khataId: Long,
        installmentAmount: Double,
        paymentMode: String = "CASH",
        utrNumber: String = "",
        note: String = ""
    ): Pair<KhataEntryEntity, Boolean>? {
        val dao = savingsAndKhataDao ?: return null
        val existing = dao.getKhataEntryById(khataId) ?: return null
        val now = System.currentTimeMillis()

        val currentHistory = parseInstallmentsJson(existing.paymentHistoryJson).toMutableList()
        val newInstallment = com.example.data.model.KhataInstallmentPayment(
            amount = installmentAmount,
            timestamp = now,
            paymentMode = paymentMode,
            utrNumber = utrNumber,
            note = note
        )
        currentHistory.add(newInstallment)
        val updatedHistoryJson = serializeInstallmentsJson(currentHistory)

        val newPaidAmount = existing.paidAmount + installmentAmount
        val isNowFullySettled = newPaidAmount >= existing.amount

        dao.updateKhataPartialPayment(
            id = khataId,
            newPaidAmount = newPaidAmount,
            paymentTime = now,
            historyJson = updatedHistoryJson
        )

        // If it's customer Udhaar receivable (YOU_WILL_GET), log received income transaction to sync daily ledger
        if (existing.type == "YOU_WILL_GET" && expenseDao != null) {
            try {
                val tx = TransactionEntity(
                    title = "Khata Settle: ${existing.personName}",
                    vendor = "Customer Khata",
                    category = "Customer Credit Settlement",
                    totalAmount = installmentAmount,
                    dateTimestamp = now,
                    paymentMethod = paymentMode,
                    locationName = "Storefront POS",
                    itemCount = 1,
                    customerName = existing.personName,
                    customerPhone = existing.personPhoneOrUpi,
                    rawVoicePrompt = if (note.isNotBlank()) note else "Installment for ${existing.description.ifEmpty { "Store Credit" }} (UTR: $utrNumber)",
                    invoiceNumber = existing.invoiceNumber,
                    transactionType = "KHATA_SETTLEMENT",
                    isVerified = true
                )
                expenseDao.insertTransaction(tx)
            } catch (_: Exception) {}
        }

        val updatedEntry = dao.getKhataEntryById(khataId) ?: existing
        return Pair(updatedEntry, isNowFullySettled)
    }

    private fun parseInstallmentsJson(json: String): List<com.example.data.model.KhataInstallmentPayment> {
        if (json.isBlank()) return emptyList()
        return try {
            val array = org.json.JSONArray(json)
            val list = mutableListOf<com.example.data.model.KhataInstallmentPayment>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    com.example.data.model.KhataInstallmentPayment(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        amount = obj.optDouble("amount", 0.0),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        paymentMode = obj.optString("paymentMode", "CASH"),
                        utrNumber = obj.optString("utrNumber", ""),
                        note = obj.optString("note", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeInstallmentsJson(list: List<com.example.data.model.KhataInstallmentPayment>): String {
        val array = org.json.JSONArray()
        for (item in list) {
            val obj = org.json.JSONObject().apply {
                put("id", item.id)
                put("amount", item.amount)
                put("timestamp", item.timestamp)
                put("paymentMode", item.paymentMode)
                put("utrNumber", item.utrNumber)
                put("note", item.note)
            }
            array.put(obj)
        }
        return array.toString()
    }

    // --- Supply Chain & Vendor Operations ---
    suspend fun insertSupplier(supplier: SupplierEntity): Long {
        return supplyChainDao?.insertSupplier(supplier) ?: 0L
    }

    suspend fun updateSupplier(supplier: SupplierEntity) {
        supplyChainDao?.updateSupplier(supplier)
    }

    suspend fun deleteSupplier(id: Long) {
        supplyChainDao?.deleteSupplier(id)
    }

    // --- Purchase Orders (PO) Workflow ---
    suspend fun createPurchaseOrder(
        supplierId: Long,
        supplierName: String,
        items: List<PurchaseOrderItemEntity>,
        expectedDeliveryDays: Int = 2,
        shippingNotes: String = ""
    ): Long {
        val scDao = supplyChainDao ?: return 0L
        val now = System.currentTimeMillis()
        val poNumber = "PO-${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)}-${(now % 10000).toString().padStart(4, '0')}"
        val total = items.sumOf { it.lineTotal }
        val tax = total * 0.05 // 5% standard estimated GST

        val poEntity = PurchaseOrderEntity(
            orderNumber = poNumber,
            supplierId = supplierId,
            supplierName = supplierName,
            status = "ORDERED",
            orderDateTimestamp = now,
            expectedDeliveryTimestamp = now + (expectedDeliveryDays.toLong() * 86_400_000L),
            totalAmount = total + tax,
            taxAmount = tax,
            paymentStatus = "UNPAID",
            shippingNotes = shippingNotes
        )

        val poId = scDao.insertPurchaseOrder(poEntity)
        val itemsWithPoId = items.map { it.copy(purchaseOrderId = poId) }
        scDao.insertPurchaseOrderItems(itemsWithPoId)
        return poId
    }

    fun getPurchaseOrderItems(poId: Long): Flow<List<PurchaseOrderItemEntity>> {
        return supplyChainDao?.getItemsForPurchaseOrder(poId) ?: emptyFlow()
    }

    suspend fun deletePurchaseOrder(poId: Long) {
        supplyChainDao?.deletePurchaseOrder(poId)
        supplyChainDao?.deletePurchaseOrderItems(poId)
    }

    suspend fun updatePurchaseOrderStatus(poId: Long, status: String) {
        supplyChainDao?.updatePurchaseOrderStatus(poId, status)
    }

    /**
     * Goods Received Note (GRN) Inward Intake:
     * 1. Marks PO as RECEIVED_GRN.
     * 2. Atomically increments master stock in commodity catalog & updates cost price.
     * 3. Creates batch records in inventory_batches with MFG and EXP timestamps.
     * 4. Logs complete inward audit entries in stock_movements.
     */
    suspend fun receiveStockGRN(
        poId: Long,
        supplierName: String,
        receivedItems: List<GrnReceiptItemInput>,
        grnNumber: String = ""
    ): Boolean {
        val scDao = supplyChainDao ?: return false
        val now = System.currentTimeMillis()
        val effectiveGrn = if (grnNumber.isNotBlank()) grnNumber else "GRN-${(now % 100000).toString().padStart(5, '0')}"

        // Update PO status
        scDao.updatePurchaseOrderStatus(
            poId = poId,
            status = "RECEIVED_GRN",
            receivedTime = now,
            grn = effectiveGrn
        )

        for (item in receivedItems) {
            if (item.receivedQuantity > 0) {
                val comm = if (item.commodityId > 0) {
                    expenseDao.getCommodityById(item.commodityId)
                } else {
                    expenseDao.getCommodityByKey(item.itemName.trim().lowercase())
                }

                val targetId = comm?.id ?: 0L
                val prevStock = comm?.stockQuantity ?: 0.0
                val newStock = prevStock + item.receivedQuantity

                if (targetId > 0) {
                    expenseDao.incrementCommodityStock(targetId, item.receivedQuantity)
                    if (item.unitCost > 0) {
                        val sellPrice = if (comm?.sellingPrice ?: 0.0 > 0) comm!!.sellingPrice else item.unitCost * 1.25
                        expenseDao.updateCommodityPricing(targetId, item.unitCost, sellPrice)
                    }
                }

                // Insert Batch
                val batchNo = if (item.batchNumber.isNotBlank()) item.batchNumber else "BAT-${System.currentTimeMillis() % 100000}"
                scDao.insertBatch(
                    BatchEntity(
                        commodityId = targetId,
                        commodityName = item.itemName,
                        batchNumber = batchNo,
                        quantity = item.receivedQuantity,
                        unit = item.unit,
                        costPrice = item.unitCost,
                        sellingPrice = if (comm?.sellingPrice ?: 0.0 > 0) comm!!.sellingPrice else item.unitCost * 1.25,
                        mfgDateTimestamp = now - (15L * 86_400_000L),
                        expiryDateTimestamp = if (item.expiryDateTimestamp > 0) item.expiryDateTimestamp else now + (180L * 86_400_000L),
                        supplierName = supplierName,
                        receivedDateTimestamp = now
                    )
                )

                // Log Stock Movement
                scDao.insertStockMovement(
                    StockMovementEntity(
                        commodityId = targetId,
                        commodityName = item.itemName,
                        changeQuantity = item.receivedQuantity,
                        previousStock = prevStock,
                        newStock = newStock,
                        movementType = "PURCHASE_GRN",
                        referenceId = "$effectiveGrn (PO-$poId)",
                        notes = "Inward Delivery Receipt from $supplierName [Batch: $batchNo]",
                        timestamp = now,
                        unit = item.unit
                    )
                )
            }
        }
        return true
    }

    /**
     * Manual Stock Correction / Shrinkage / Wastage Write-Off
     */
    suspend fun recordManualStockAdjustment(
        commodityId: Long,
        adjustmentQty: Double,
        reason: String, // MANUAL_CORRECTION, DAMAGED_WRITE_OFF, RETURN_INWARD
        notes: String
    ): Boolean {
        val comm = expenseDao.getCommodityById(commodityId) ?: return false
        val prevStock = comm.stockQuantity
        val newStock = (prevStock + adjustmentQty).coerceAtLeast(0.0)

        expenseDao.updateCommodityStock(commodityId, newStock)

        supplyChainDao?.insertStockMovement(
            StockMovementEntity(
                commodityId = commodityId,
                commodityName = comm.canonicalName,
                changeQuantity = adjustmentQty,
                previousStock = prevStock,
                newStock = newStock,
                movementType = reason,
                referenceId = "ADJ-${System.currentTimeMillis() % 10000}",
                notes = notes.ifBlank { "Stock Adjustment: $reason" },
                timestamp = System.currentTimeMillis(),
                unit = comm.normalizedUnit
            )
        )
        return true
    }

    /**
     * Auto-Generates a Purchase Order for all Low-Stock Items
     */
    suspend fun autoGenerateLowStockPurchaseOrder(
        supplier: SupplierEntity,
        lowStockItems: List<CommodityEntity>
    ): Long {
        if (lowStockItems.isEmpty()) return 0L

        val poItems = lowStockItems.map { comm ->
            val reorderQty = (comm.reorderThreshold * 3.0).coerceAtLeast(10.0)
            val costPrice = if (comm.costPrice > 0) comm.costPrice else comm.sellingPrice * 0.75
            PurchaseOrderItemEntity(
                purchaseOrderId = 0L,
                commodityId = comm.id,
                itemName = comm.canonicalName,
                brand = comm.brand,
                orderedQuantity = reorderQty,
                unit = comm.normalizedUnit,
                unitCostPrice = costPrice,
                lineTotal = reorderQty * costPrice
            )
        }

        return createPurchaseOrder(
            supplierId = supplier.id,
            supplierName = supplier.name,
            items = poItems,
            expectedDeliveryDays = supplier.leadTimeDays,
            shippingNotes = "Auto-Generated Replenishment PO for Low Stock items"
        )
    }

    /**
     * Records Accounts Payable Payment to Supplier / Vendor
     */
    suspend fun recordSupplierPayment(
        supplierId: Long,
        poId: Long?,
        amountPaid: Double
    ): Boolean {
        val scDao = supplyChainDao ?: return false
        if (amountPaid <= 0) return false

        scDao.deductOutstandingPayable(supplierId, amountPaid)
        if (poId != null && poId > 0) {
            val po = scDao.getPurchaseOrderById(poId)
            if (po != null) {
                val newStatus = if (amountPaid >= po.totalAmount) "PAID" else "PARTIALLY_PAID"
                scDao.updatePurchaseOrderPaymentStatus(poId, newStatus)
            }
        }
        return true
    }

    /**
     * Applies promotional markdown / clearance discount to a commodity or batch to avoid shrinkage
     */
    suspend fun applyCommodityMarkdown(
        commodityId: Long,
        markdownPercent: Double,
        batchId: Long? = null
    ): Boolean {
        val comm = expenseDao.getCommodityById(commodityId) ?: return false
        val currentPrice = comm.sellingPrice
        val discountedPrice = (currentPrice * (1.0 - (markdownPercent / 100.0))).coerceAtLeast(1.0)

        // Update commodity selling price
        expenseDao.updateCommodityPricing(commodityId, comm.costPrice, discountedPrice)

        // If batchId is specified, update batch selling price as well
        if (batchId != null && batchId > 0) {
            val activeBatches = supplyChainDao?.getActiveBatches()?.first() ?: emptyList()
            val targetBatch = activeBatches.find { it.id == batchId }
            if (targetBatch != null) {
                supplyChainDao?.updateBatch(targetBatch.copy(sellingPrice = discountedPrice))
            }
        }

        // Record audit movement for transparency
        supplyChainDao?.insertStockMovement(
            StockMovementEntity(
                commodityId = commodityId,
                commodityName = comm.canonicalName,
                changeQuantity = 0.0,
                previousStock = comm.stockQuantity,
                newStock = comm.stockQuantity,
                movementType = "PRICE_MARKDOWN",
                referenceId = "PROMO-${markdownPercent.toInt()}%",
                notes = "Promotional Markdown: Price reduced from ₹${currentPrice.toInt()} to ₹${discountedPrice.toInt()} (-${markdownPercent.toInt()}%)",
                timestamp = System.currentTimeMillis(),
                unit = comm.normalizedUnit
            )
        )
        return true
    }

    /**
     * Writes off an expired batch to prevent inventory shrinkage from affecting usable stock
     */
    suspend fun writeOffExpiredBatch(
        batchId: Long,
        commodityId: Long,
        quantity: Double,
        unit: String,
        commodityName: String
    ): Boolean {
        val scDao = supplyChainDao ?: return false

        // Mark batch as depleted
        scDao.markBatchDepleted(batchId)

        // Deduct usable stock from commodity catalog
        if (commodityId > 0 && quantity > 0) {
            val comm = expenseDao.getCommodityById(commodityId)
            val prevStock = comm?.stockQuantity ?: 0.0
            val newStock = (prevStock - quantity).coerceAtLeast(0.0)
            expenseDao.updateCommodityStock(commodityId, newStock)

            // Log damaged write-off
            scDao.insertStockMovement(
                StockMovementEntity(
                    commodityId = commodityId,
                    commodityName = commodityName,
                    changeQuantity = -quantity,
                    previousStock = prevStock,
                    newStock = newStock,
                    movementType = "DAMAGED_WRITE_OFF",
                    referenceId = "EXP-WRITEOFF-$batchId",
                    notes = "Expired Lot Discarded & Written-Off ($quantity $unit)",
                    timestamp = System.currentTimeMillis(),
                    unit = unit
                )
            )
        }
        return true
    }

    /**
     * Direct batch insertion for manual lot intake
     */
    suspend fun insertBatchDirect(batch: BatchEntity): Long {
        return supplyChainDao?.insertBatch(batch) ?: 0L
    }
}
