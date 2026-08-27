package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ExpenseDao
import com.example.data.dao.RecurringBillDao
import com.example.data.dao.SavingsAndKhataDao
import com.example.data.dao.SupplyChainDao
import com.example.data.model.BatchEntity
import com.example.data.model.CommodityEntity
import com.example.data.model.GroceryItemEntity
import com.example.data.model.KhataEntryEntity
import com.example.data.model.LineItemEntity
import com.example.data.model.PurchaseOrderEntity
import com.example.data.model.PurchaseOrderItemEntity
import com.example.data.model.RecurringBillEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.StockMovementEntity
import com.example.data.model.SupplierEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        LineItemEntity::class,
        GroceryItemEntity::class,
        CommodityEntity::class,
        RecurringBillEntity::class,
        SavingsGoalEntity::class,
        KhataEntryEntity::class,
        SupplierEntity::class,
        PurchaseOrderEntity::class,
        PurchaseOrderItemEntity::class,
        StockMovementEntity::class,
        BatchEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun recurringBillDao(): RecurringBillDao
    abstract fun savingsAndKhataDao(): SavingsAndKhataDao
    abstract fun supplyChainDao(): SupplyChainDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    "cyphr_expense.db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(appContext, scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return getDatabase(context)
        }

        private class DatabaseCallback(
            private val context: Context,
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(
                            context = context,
                            dao = database.expenseDao(),
                            recurringBillDao = database.recurringBillDao(),
                            savingsAndKhataDao = database.savingsAndKhataDao(),
                            supplyChainDao = database.supplyChainDao()
                        )
                    }
                }
            }
        }

        private suspend fun populateInitialData(
            context: Context,
            dao: ExpenseDao,
            recurringBillDao: RecurringBillDao,
            savingsAndKhataDao: SavingsAndKhataDao,
            supplyChainDao: SupplyChainDao
        ) {
            val now = System.currentTimeMillis()
            val dayMillis = 86_400_000L

            // 1. Tier 2: Pre-Seeded Master Commodity Catalog from Lightweight JSON Asset
            val seededCount = CommodityPrepopulationHelper.prePopulateFromAsset(context, dao)
            if (seededCount == 0) {
                // Fallback direct seeding if asset reading is unavailable in test harness
                dao.insertCommodities(
                    listOf(
                        CommodityEntity(
                            rawKey = "amul butter",
                            canonicalName = "Butter",
                            brand = "Amul",
                            category = "Dairy",
                            subcategory = "Spreads & Fats",
                            defaultQuantity = 500.0,
                            normalizedUnit = "g",
                            estimatedShelfLifeDays = 180,
                            storageType = "Refrigerated",
                            lastKnownPrice = 300.0,
                            useCount = 5,
                            isPreSeeded = true,
                            stockQuantity = 24.0,
                            costPrice = 240.0,
                            sellingPrice = 300.0,
                            reorderThreshold = 6.0,
                            sku = "SKU-BUTTER-500"
                        ),
                        CommodityEntity(
                            rawKey = "whole milk",
                            canonicalName = "Milk",
                            brand = "Amul / Nandini",
                            category = "Dairy",
                            subcategory = "Fresh Milk",
                            defaultQuantity = 1.0,
                            normalizedUnit = "L",
                            estimatedShelfLifeDays = 7,
                            storageType = "Refrigerated",
                            lastKnownPrice = 70.0,
                            useCount = 8,
                            isPreSeeded = true,
                            stockQuantity = 4.0,
                            costPrice = 54.0,
                            sellingPrice = 70.0,
                            reorderThreshold = 10.0,
                            sku = "SKU-MILK-1L"
                        ),
                        CommodityEntity(
                            rawKey = "organic eggs",
                            canonicalName = "Organic Eggs",
                            brand = "Eggoz / Farm Fresh",
                            category = "Produce",
                            subcategory = "Poultry",
                            defaultQuantity = 12.0,
                            normalizedUnit = "pcs",
                            estimatedShelfLifeDays = 21,
                            storageType = "Refrigerated",
                            lastKnownPrice = 130.0,
                            useCount = 5,
                            isPreSeeded = true,
                            stockQuantity = 18.0,
                            costPrice = 95.0,
                            sellingPrice = 130.0,
                            reorderThreshold = 8.0,
                            sku = "SKU-EGGS-12PK"
                        )
                    )
                )
            }

            // 2. Populate sample historical transactions
            val tx1Id = dao.insertTransaction(
                TransactionEntity(
                    title = "FreshMart Superstore",
                    vendor = "FreshMart",
                    category = "Groceries",
                    totalAmount = 840.00,
                    dateTimestamp = now - (1 * dayMillis),
                    paymentMethod = "UPI Instant",
                    locationName = "Sector 4 Neo-Hub",
                    itemCount = 3,
                    rawVoicePrompt = "Rice at 50, Amul Butter 500 grams at 300 from FreshMart, Olive Oil at 490",
                    isVerified = true
                )
            )
            dao.insertLineItems(
                listOf(
                    LineItemEntity(0, tx1Id, "Amul Butter 500g", "Dairy", 1.0, "500g", 300.0, 300.0, "FreshMart", now - (1 * dayMillis), "Butter", "Amul", "Refrigerated", 180),
                    LineItemEntity(0, tx1Id, "Basmati Rice 1kg", "Grains", 1.0, "1kg", 50.0, 50.0, "FreshMart", now - (1 * dayMillis), "Basmati Rice", "Fortune", "Pantry", 365),
                    LineItemEntity(0, tx1Id, "Extra Virgin Olive Oil 500ml", "Pantry", 1.0, "500ml", 490.0, 490.0, "FreshMart", now - (1 * dayMillis), "Olive Oil", "Borges", "Pantry", 365)
                )
            )

            val tx2Id = dao.insertTransaction(
                TransactionEntity(
                    title = "Cyber Cafe & Roastery",
                    vendor = "Cyber Roast",
                    category = "Dining",
                    totalAmount = 380.00,
                    dateTimestamp = now - (2 * dayMillis),
                    paymentMethod = "Google Wallet",
                    locationName = "Downtown Cyber Arcade",
                    itemCount = 2,
                    rawVoicePrompt = "Cold Brew at 220, Croissant at 160 at Cyber Roast",
                    isVerified = true
                )
            )
            dao.insertLineItems(
                listOf(
                    LineItemEntity(0, tx2Id, "Cold Brew Coffee", "Beverages", 1.0, "unit", 220.0, 220.0, "Cyber Roast", now - (2 * dayMillis), "Cold Brew", "Artisan", "Refrigerated", 3),
                    LineItemEntity(0, tx2Id, "Almond Butter Croissant", "Dining", 1.0, "unit", 160.0, 160.0, "Cyber Roast", now - (2 * dayMillis), "Croissant", "Bakery", "Pantry", 2)
                )
            )

            val tx3Id = dao.insertTransaction(
                TransactionEntity(
                    title = "Blinkit Quick Delivery",
                    vendor = "Blinkit",
                    category = "Groceries",
                    totalAmount = 565.00,
                    dateTimestamp = now - (4 * dayMillis),
                    paymentMethod = "UPI Instant",
                    locationName = "Online Quick-Commerce",
                    itemCount = 3,
                    rawVoicePrompt = "Milk 2 packets at 70, Greek Yogurt at 95, Farm Fresh Eggs at 130",
                    isVerified = true
                )
            )
            dao.insertLineItems(
                listOf(
                    LineItemEntity(0, tx3Id, "Whole Milk 1L", "Dairy", 2.0, "1L", 70.0, 140.0, "Blinkit", now - (4 * dayMillis), "Milk", "Amul", "Refrigerated", 7),
                    LineItemEntity(0, tx3Id, "Greek Yogurt 400g", "Dairy", 1.0, "400g", 95.0, 95.0, "Blinkit", now - (4 * dayMillis), "Greek Yogurt", "Epigamia", "Refrigerated", 21),
                    LineItemEntity(0, tx3Id, "Farm Fresh Organic Eggs", "Produce", 1.0, "12pk", 130.0, 130.0, "Blinkit", now - (4 * dayMillis), "Eggs", "Eggoz", "Refrigerated", 21)
                )
            )

            // 3. Populate Wishlist & Pantry items
            dao.insertGroceryItems(
                listOf(
                    GroceryItemEntity(
                        name = "Extra Virgin Olive Oil",
                        quantity = "1 bottle (500ml)",
                        estimatedPrice = 480.0,
                        priceCap = 500.0,
                        targetVendor = "FreshMart",
                        category = "Pantry",
                        isChecked = false,
                        isPantryItem = false,
                        canonicalName = "Olive Oil",
                        brand = "Borges",
                        storageType = "Pantry"
                    ),
                    GroceryItemEntity(
                        name = "Almond Milk Unsweetened",
                        quantity = "2 packs (1L)",
                        estimatedPrice = 180.0,
                        priceCap = 200.0,
                        targetVendor = "Blinkit",
                        category = "Dairy",
                        isChecked = false,
                        isPantryItem = false,
                        canonicalName = "Almond Milk",
                        brand = "Raw Pressery",
                        storageType = "Refrigerated"
                    )
                )
            )

            dao.insertGroceryItems(
                listOf(
                    GroceryItemEntity(
                        name = "Amul Butter 500g",
                        quantity = "1 block",
                        estimatedPrice = 300.0,
                        priceCap = 300.0,
                        targetVendor = "FreshMart",
                        category = "Dairy",
                        isChecked = false,
                        isPantryItem = true,
                        purchaseDate = now - (1 * dayMillis),
                        expiryDaysTotal = 180,
                        remainingDays = 179,
                        burnRateLevel = "NORMAL",
                        lastBoughtDaysAgo = 1,
                        canonicalName = "Butter",
                        brand = "Amul",
                        storageType = "Refrigerated"
                    ),
                    GroceryItemEntity(
                        name = "Whole Milk 1L",
                        quantity = "1 carton",
                        estimatedPrice = 70.0,
                        priceCap = 75.0,
                        targetVendor = "Blinkit",
                        category = "Dairy",
                        isChecked = false,
                        isPantryItem = true,
                        purchaseDate = now - (3 * dayMillis),
                        expiryDaysTotal = 7,
                        remainingDays = 4,
                        burnRateLevel = "HIGH",
                        lastBoughtDaysAgo = 3,
                        canonicalName = "Milk",
                        brand = "Amul",
                        storageType = "Refrigerated"
                    )
                )
            )

            // 5. Seed Recurring Bills & Subscriptions
            recurringBillDao.insertBills(
                listOf(
                    RecurringBillEntity(
                        title = "Netflix 4K Ultra",
                        amount = 649.0,
                        category = "Entertainment",
                        billingCycle = "MONTHLY",
                        dueDay = 5,
                        nextDueDate = now + (4 * dayMillis),
                        paymentMethod = "UPI AutoPay",
                        isAutoDebit = true,
                        reminderDaysBefore = 3,
                        status = "ACTIVE",
                        serviceIcon = "NETFLIX",
                        notes = "Family plan shared across 4 screens"
                    ),
                    RecurringBillEntity(
                        title = "Airtel Xstream Fiber (200 Mbps)",
                        amount = 999.0,
                        category = "Utilities",
                        billingCycle = "MONTHLY",
                        dueDay = 12,
                        nextDueDate = now + (11 * dayMillis),
                        paymentMethod = "Credit Card",
                        isAutoDebit = true,
                        reminderDaysBefore = 2,
                        status = "ACTIVE",
                        serviceIcon = "AIRTEL",
                        notes = "Includes Disney+ Hotstar subscription"
                    ),
                    RecurringBillEntity(
                        title = "Spotify Duo",
                        amount = 149.0,
                        category = "Entertainment",
                        billingCycle = "MONTHLY",
                        dueDay = 18,
                        nextDueDate = now + (17 * dayMillis),
                        paymentMethod = "UPI AutoPay",
                        isAutoDebit = true,
                        reminderDaysBefore = 1,
                        status = "ACTIVE",
                        serviceIcon = "SPOTIFY",
                        notes = "Shared with partner"
                    ),
                    RecurringBillEntity(
                        title = "Bescom Electricity Bill",
                        amount = 1850.0,
                        category = "Utilities",
                        billingCycle = "MONTHLY",
                        dueDay = 24,
                        nextDueDate = now + (23 * dayMillis),
                        paymentMethod = "Manual / NetBanking",
                        isAutoDebit = false,
                        reminderDaysBefore = 5,
                        status = "ACTIVE",
                        serviceIcon = "ELECTRICITY",
                        notes = "Consumer # 849201844"
                    ),
                    RecurringBillEntity(
                        title = "Cult.fit Gym & Fitness",
                        amount = 12999.0,
                        category = "Fitness",
                        billingCycle = "ANNUAL",
                        dueDay = 15,
                        nextDueDate = now + (120 * dayMillis),
                        paymentMethod = "Credit Card EMI",
                        isAutoDebit = false,
                        reminderDaysBefore = 10,
                        status = "ACTIVE",
                        serviceIcon = "GYM",
                        notes = "Annual ELITE membership renews in Dec"
                    )
                )
            )

            // 6. Seed Savings Goals
            savingsAndKhataDao.insertSavingsGoals(
                listOf(
                    SavingsGoalEntity(
                        title = "Emergency Fund (6 Months)",
                        targetAmount = 150000.0,
                        currentAmount = 95000.0,
                        targetDate = now + (180 * dayMillis),
                        category = "Emergency",
                        monthlyContributionTarget = 10000.0,
                        colorHex = "#FF6B35",
                        notes = "Safety buffer in liquid mutual funds",
                        isCompleted = false
                    ),
                    SavingsGoalEntity(
                        title = "MacBook Pro M3 Pro",
                        targetAmount = 199900.0,
                        currentAmount = 135000.0,
                        targetDate = now + (75 * dayMillis),
                        category = "Gadgets",
                        monthlyContributionTarget = 20000.0,
                        colorHex = "#4ADE80",
                        notes = "Upgrading work machine for dev",
                        isCompleted = false
                    ),
                    SavingsGoalEntity(
                        title = "Japan Autumn Trip",
                        targetAmount = 220000.0,
                        currentAmount = 60000.0,
                        targetDate = now + (240 * dayMillis),
                        category = "Travel",
                        monthlyContributionTarget = 20000.0,
                        colorHex = "#FBBF24",
                        notes = "Flights, JR Pass & ryokan bookings",
                        isCompleted = false
                    ),
                    SavingsGoalEntity(
                        title = "Diwali Gold Coin 10g",
                        targetAmount = 75000.0,
                        currentAmount = 75000.0,
                        targetDate = now - (10 * dayMillis),
                        category = "Investment",
                        monthlyContributionTarget = 15000.0,
                        colorHex = "#E0E0E0",
                        notes = "Purchased on Dhanteras!",
                        isCompleted = true
                    )
                )
            )

            // 7. Seed Customer Khata (Udhaar / Store Credit & Supplier Ledger) Entries
            val rahulHistory = """[{"id":"inst-1","amount":500.0,"timestamp":${now - (1 * dayMillis)},"paymentMode":"UPI_INSTANT","utrNumber":"UPI/409281729102","note":"GPay partial installment"}]"""
            val priyaHistory = """[{"id":"inst-2","amount":300.0,"timestamp":${now - (2 * dayMillis)},"paymentMode":"CASH","utrNumber":"","note":"Cash counter deposit"}]"""
            
            savingsAndKhataDao.insertKhataEntries(
                listOf(
                    KhataEntryEntity(
                        personName = "Rahul Verma",
                        personPhoneOrUpi = "+91 98450 77123",
                        type = "YOU_WILL_GET",
                        amount = 1450.0,
                        paidAmount = 500.0,
                        description = "Monthly Grocery & Dry Fruits Store Credit",
                        invoiceNumber = "INV-2026-0812",
                        customerLoyaltyPoints = 280,
                        customerTag = "REGULAR",
                        dateTimestamp = now - (5 * dayMillis),
                        dueDateTimestamp = now + (2 * dayMillis),
                        lastPaymentDateTimestamp = now - (1 * dayMillis),
                        paymentHistoryJson = rahulHistory,
                        isSettled = false
                    ),
                    KhataEntryEntity(
                        personName = "Priya Sharma",
                        personPhoneOrUpi = "+91 98765 12340",
                        type = "YOU_WILL_GET",
                        amount = 820.0,
                        paidAmount = 300.0,
                        description = "Aashirvaad Atta & Amul Butter Store Credit",
                        invoiceNumber = "INV-2026-0819",
                        customerLoyaltyPoints = 460,
                        customerTag = "VIP",
                        dateTimestamp = now - (6 * dayMillis),
                        dueDateTimestamp = now + (1 * dayMillis),
                        lastPaymentDateTimestamp = now - (2 * dayMillis),
                        paymentHistoryJson = priyaHistory,
                        isSettled = false
                    ),
                    KhataEntryEntity(
                        personName = "Ananya Desai",
                        personPhoneOrUpi = "+91 99001 88452",
                        type = "YOU_WILL_GET",
                        amount = 2400.0,
                        paidAmount = 0.0,
                        description = "Festive Hamper & Gourmet Spices",
                        invoiceNumber = "INV-2026-0830",
                        customerLoyaltyPoints = 890,
                        customerTag = "VIP",
                        dateTimestamp = now - (3 * dayMillis),
                        dueDateTimestamp = now - (1 * dayMillis), // Overdue reminder test
                        isSettled = false
                    ),
                    KhataEntryEntity(
                        personName = "Vikram Malhotra",
                        personPhoneOrUpi = "+91 98110 55432",
                        type = "YOU_WILL_GET",
                        amount = 620.0,
                        paidAmount = 620.0,
                        description = "Dairy & Bread Quick Credit",
                        invoiceNumber = "INV-2026-0799",
                        customerLoyaltyPoints = 190,
                        customerTag = "REGULAR",
                        dateTimestamp = now - (12 * dayMillis),
                        dueDateTimestamp = now - (5 * dayMillis),
                        isSettled = true,
                        settledDateTimestamp = now - (4 * dayMillis)
                    ),
                    KhataEntryEntity(
                        personName = "Metro Wholesale & FMCG Depot",
                        personPhoneOrUpi = "+91 98112 34567",
                        type = "YOU_WILL_PAY",
                        amount = 4500.0,
                        paidAmount = 2000.0,
                        description = "Bulk Atta & Oil Batch Invoice Credit Terms",
                        invoiceNumber = "GRN-9912",
                        customerTag = "WHOLESALE",
                        dateTimestamp = now - (4 * dayMillis),
                        dueDateTimestamp = now + (10 * dayMillis),
                        isSettled = false
                    )
                )
            )

            // 6. Pre-Seeded Suppliers & Wholesale Distributors
            supplyChainDao.insertSuppliers(
                listOf(
                    SupplierEntity(
                        id = 1,
                        name = "Metro Wholesale & FMCG Depot",
                        contactPerson = "Rajesh Gupta",
                        phone = "+91 98112 34567",
                        email = "orders@metrowholesale.in",
                        address = "Plot 42, Sector 18 Industrial Area, Bengaluru",
                        gstin = "29AABCU9603R1ZM",
                        paymentTerms = "Net 15 Days",
                        leadTimeDays = 2,
                        rating = 4.9f,
                        outstandingPayable = 12400.0,
                        notes = "Primary bulk FMCG, oils, grains, and dry staples distributor."
                    ),
                    SupplierEntity(
                        id = 2,
                        name = "Amul Gujarat Co-op Dairy",
                        contactPerson = "Vikram Patel",
                        phone = "+91 98250 88990",
                        email = "supply@amuldairy.com",
                        address = "Cold Chain Hub 8, Dairy Circle, Bengaluru",
                        gstin = "24AAAAG1234F1ZX",
                        paymentTerms = "COD Cash On Delivery",
                        leadTimeDays = 1,
                        rating = 4.8f,
                        outstandingPayable = 0.0,
                        notes = "Daily morning delivery for Butter, Cheese, Fresh Paneer, Milk."
                    ),
                    SupplierEntity(
                        id = 3,
                        name = "ITC Direct Supply Hub",
                        contactPerson = "Sandeep Menon",
                        phone = "+91 98450 11223",
                        email = "distributor@itcsupplies.in",
                        address = "Logistics Park, Whitefield, Bengaluru",
                        gstin = "29AAACI1608G1ZB",
                        paymentTerms = "Net 30 Days",
                        leadTimeDays = 3,
                        rating = 4.7f,
                        outstandingPayable = 8500.0,
                        notes = "Aashirvaad Atta, Sunfeast biscuits, Bingo snacks, Classmate."
                    ),
                    SupplierEntity(
                        id = 4,
                        name = "Tata Consumer Products Depot",
                        contactPerson = "Manoj Joshi",
                        phone = "+91 97312 99887",
                        email = "wholesale@tataconsumer.com",
                        address = "Ring Road Warehouse 14, Bengaluru",
                        gstin = "29AAACT2001F1Z1",
                        paymentTerms = "Net 15 Days",
                        leadTimeDays = 2,
                        rating = 4.9f,
                        outstandingPayable = 4200.0,
                        notes = "Tata Salt, Tata Tea Premium, Sampann Pulses & Poha."
                    )
                )
            )

            // 7. Pre-Seeded Sample Purchase Orders
            val po1Id = supplyChainDao.insertPurchaseOrder(
                PurchaseOrderEntity(
                    id = 1,
                    orderNumber = "PO-2026-0041",
                    supplierId = 1,
                    supplierName = "Metro Wholesale & FMCG Depot",
                    status = "RECEIVED_GRN",
                    orderDateTimestamp = now - (6 * dayMillis),
                    expectedDeliveryTimestamp = now - (4 * dayMillis),
                    receivedDateTimestamp = now - (4 * dayMillis),
                    totalAmount = 18500.0,
                    taxAmount = 925.0,
                    paymentStatus = "PAID",
                    grnNumber = "GRN-9912",
                    shippingNotes = "Gate 2 Inward Loading Dock"
                )
            )

            supplyChainDao.insertPurchaseOrderItems(
                listOf(
                    PurchaseOrderItemEntity(
                        purchaseOrderId = po1Id,
                        commodityId = 1,
                        itemName = "Aashirvaad Atta 5kg",
                        brand = "Aashirvaad",
                        orderedQuantity = 40.0,
                        receivedQuantity = 40.0,
                        unit = "pack",
                        unitCostPrice = 245.0,
                        lineTotal = 9800.0,
                        batchNumber = "BAT-ASH-2026-A",
                        expiryDateTimestamp = now + (180 * dayMillis)
                    ),
                    PurchaseOrderItemEntity(
                        purchaseOrderId = po1Id,
                        commodityId = 2,
                        itemName = "Fortune Sunlite Oil 1L",
                        brand = "Fortune",
                        orderedQuantity = 60.0,
                        receivedQuantity = 60.0,
                        unit = "pack",
                        unitCostPrice = 145.0,
                        lineTotal = 8700.0,
                        batchNumber = "BAT-FOR-2026-C",
                        expiryDateTimestamp = now + (270 * dayMillis)
                    )
                )
            )

            val po2Id = supplyChainDao.insertPurchaseOrder(
                PurchaseOrderEntity(
                    id = 2,
                    orderNumber = "PO-2026-0042",
                    supplierId = 2,
                    supplierName = "Amul Gujarat Co-op Dairy",
                    status = "IN_TRANSIT",
                    orderDateTimestamp = now - (1 * dayMillis),
                    expectedDeliveryTimestamp = now + (1 * dayMillis),
                    totalAmount = 7600.0,
                    taxAmount = 380.0,
                    paymentStatus = "UNPAID",
                    shippingNotes = "Refrigerated reefer truck morning delivery"
                )
            )

            supplyChainDao.insertPurchaseOrderItems(
                listOf(
                    PurchaseOrderItemEntity(
                        purchaseOrderId = po2Id,
                        commodityId = 3,
                        itemName = "Amul Butter 500g",
                        brand = "Amul",
                        orderedQuantity = 25.0,
                        receivedQuantity = 0.0,
                        unit = "pack",
                        unitCostPrice = 240.0,
                        lineTotal = 6000.0
                    ),
                    PurchaseOrderItemEntity(
                        purchaseOrderId = po2Id,
                        commodityId = 4,
                        itemName = "Amul Cheese Slices 200g",
                        brand = "Amul",
                        orderedQuantity = 10.0,
                        receivedQuantity = 0.0,
                        unit = "pack",
                        unitCostPrice = 160.0,
                        lineTotal = 1600.0
                    )
                )
            )

            // 8. Pre-Seeded Batches & Expiry Dates
            supplyChainDao.insertBatches(
                listOf(
                    BatchEntity(
                        commodityId = 1,
                        commodityName = "Aashirvaad Atta 5kg",
                        batchNumber = "BAT-ASH-2026-A",
                        quantity = 35.0,
                        unit = "pack",
                        costPrice = 245.0,
                        sellingPrice = 295.0,
                        mfgDateTimestamp = now - (20 * dayMillis),
                        expiryDateTimestamp = now + (160 * dayMillis),
                        supplierName = "Metro Wholesale & FMCG Depot"
                    ),
                    BatchEntity(
                        commodityId = 2,
                        commodityName = "Fortune Sunlite Oil 1L",
                        batchNumber = "BAT-FOR-2026-C",
                        quantity = 52.0,
                        unit = "pack",
                        costPrice = 145.0,
                        sellingPrice = 175.0,
                        mfgDateTimestamp = now - (30 * dayMillis),
                        expiryDateTimestamp = now + (240 * dayMillis),
                        supplierName = "Metro Wholesale & FMCG Depot"
                    ),
                    BatchEntity(
                        commodityId = 3,
                        commodityName = "Amul Butter 500g",
                        batchNumber = "BAT-AML-2026-08",
                        quantity = 8.0,
                        unit = "pack",
                        costPrice = 235.0,
                        sellingPrice = 275.0,
                        mfgDateTimestamp = now - (60 * dayMillis),
                        expiryDateTimestamp = now + (18 * dayMillis), // Near expiry alert!
                        supplierName = "Amul Gujarat Co-op Dairy"
                    ),
                    BatchEntity(
                        commodityId = 4,
                        commodityName = "Tata Tea Gold 500g",
                        batchNumber = "BAT-TAT-2026-B",
                        quantity = 22.0,
                        unit = "pack",
                        costPrice = 270.0,
                        sellingPrice = 330.0,
                        mfgDateTimestamp = now - (40 * dayMillis),
                        expiryDateTimestamp = now + (320 * dayMillis),
                        supplierName = "Tata Consumer Products Depot"
                    )
                )
            )

            // 9. Pre-Seeded Stock Movement Audit Trail
            supplyChainDao.insertStockMovements(
                listOf(
                    StockMovementEntity(
                        commodityId = 1,
                        commodityName = "Aashirvaad Atta 5kg",
                        changeQuantity = 40.0,
                        previousStock = 0.0,
                        newStock = 40.0,
                        movementType = "PURCHASE_GRN",
                        referenceId = "PO-2026-0041 / GRN-9912",
                        notes = "Inward Shipment Received from Metro Wholesale",
                        timestamp = now - (4 * dayMillis)
                    ),
                    StockMovementEntity(
                        commodityId = 1,
                        commodityName = "Aashirvaad Atta 5kg",
                        changeQuantity = -5.0,
                        previousStock = 40.0,
                        newStock = 35.0,
                        movementType = "POS_SALE",
                        referenceId = "INV-00104",
                        notes = "Customer Checkout POS Counter 1",
                        timestamp = now - (2 * dayMillis)
                    ),
                    StockMovementEntity(
                        commodityId = 2,
                        commodityName = "Fortune Sunlite Oil 1L",
                        changeQuantity = 60.0,
                        previousStock = 0.0,
                        newStock = 60.0,
                        movementType = "PURCHASE_GRN",
                        referenceId = "PO-2026-0041 / GRN-9912",
                        notes = "Inward Shipment Received from Metro Wholesale",
                        timestamp = now - (4 * dayMillis)
                    ),
                    StockMovementEntity(
                        commodityId = 2,
                        commodityName = "Fortune Sunlite Oil 1L",
                        changeQuantity = -8.0,
                        previousStock = 60.0,
                        newStock = 52.0,
                        movementType = "POS_SALE",
                        referenceId = "INV-00108",
                        notes = "Quick Commerce Delivery Dispatch",
                        timestamp = now - (1 * dayMillis)
                    )
                )
            )
        }
    }
}
