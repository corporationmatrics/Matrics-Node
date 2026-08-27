package com.example.data.model

import java.util.UUID

/**
 * Data structures for Handwritten "Kacha Bill" & Informal Kirana Invoice Parsing
 */
data class HandwrittenBillItem(
    val id: String = UUID.randomUUID().toString(),
    val rawWrittenText: String,
    val canonicalName: String,
    val brand: String = "",
    val category: String = "Groceries",
    val subcategory: String = "",
    val quantity: Double = 1.0,
    val unit: String = "kg",
    val price: Double = 0.0,
    val confidenceScore: Float = 0.95f, // Score between 0.0 and 1.0
    val isLowConfidence: Boolean = false, // Highlight with Cyber Red if true
    val cropCoordinateX: Float = 0.5f, // Relative coordinate for zoom/pan
    val cropCoordinateY: Float = 0.5f,
    val storageType: String = "Pantry",
    val shelfLifeDays: Int = 30,
    val quickCommerceRefPrice: Double? = null // Price index comparison vs Blinkit/Zepto
)

data class HandwrittenBillResult(
    val vendorName: String = "Sharma Kirana Store",
    val dateString: String = "Today",
    val items: List<HandwrittenBillItem> = emptyList(),
    val shopkeeperTotal: Double = 0.0,
    val calculatedTrueTotal: Double = 0.0,
    val mathErrorFlag: Boolean = false,
    val mathErrorDelta: Double = 0.0,
    val khataOldBalance: Double? = null,
    val khataNewBalance: Double? = null,
    val rawTranscript: String = "",
    val isBinarized: Boolean = false,
    val samplePresetName: String? = null,
    val invoiceType: String = "HANDWRITTEN" // "PRINTED" or "HANDWRITTEN"
)

/**
 * Built-in Authentic Presets of Invoices for Instant Demo & Offline Verification
 */
object KachaBillPresets {
    val PRESETS = listOf(
        HandwrittenBillResult(
            vendorName = "SuperMart Hypermarket",
            dateString = "19 Aug (Today)",
            invoiceType = "PRINTED",
            items = listOf(
                HandwrittenBillItem(
                    rawWrittenText = "ORGANIC BABY SPINACH 250G",
                    canonicalName = "Organic Baby Spinach",
                    brand = "FreshOrganics",
                    category = "Produce",
                    subcategory = "Leafy Greens",
                    quantity = 250.0,
                    unit = "g",
                    price = 85.0,
                    confidenceScore = 0.99f,
                    isLowConfidence = false,
                    cropCoordinateX = 0.28f,
                    cropCoordinateY = 0.20f,
                    storageType = "Refrigerated",
                    shelfLifeDays = 5,
                    quickCommerceRefPrice = 90.0
                ),
                HandwrittenBillItem(
                    rawWrittenText = "GREEK FETA CHEESE 200G",
                    canonicalName = "Greek Feta Cheese",
                    brand = "Epigamia",
                    category = "Dairy",
                    subcategory = "Cheese",
                    quantity = 200.0,
                    unit = "g",
                    price = 240.0,
                    confidenceScore = 0.99f,
                    isLowConfidence = false,
                    cropCoordinateX = 0.28f,
                    cropCoordinateY = 0.35f,
                    storageType = "Refrigerated",
                    shelfLifeDays = 30,
                    quickCommerceRefPrice = 260.0
                ),
                HandwrittenBillItem(
                    rawWrittenText = "COLD PRESSED OLIVE OIL 500ML",
                    canonicalName = "Extra Virgin Olive Oil",
                    brand = "Borges",
                    category = "Pantry",
                    subcategory = "Cooking Oils",
                    quantity = 500.0,
                    unit = "ml",
                    price = 490.0,
                    confidenceScore = 0.98f,
                    isLowConfidence = false,
                    cropCoordinateX = 0.28f,
                    cropCoordinateY = 0.50f,
                    storageType = "Pantry",
                    shelfLifeDays = 365,
                    quickCommerceRefPrice = 520.0
                ),
                HandwrittenBillItem(
                    rawWrittenText = "WHOLE WHEAT ARTISAN BREAD",
                    canonicalName = "Whole Wheat Artisan Bread",
                    brand = "The Baker's Dozen",
                    category = "Grains",
                    subcategory = "Bakery",
                    quantity = 1.0,
                    unit = "loaf",
                    price = 95.0,
                    confidenceScore = 0.99f,
                    isLowConfidence = false,
                    cropCoordinateX = 0.28f,
                    cropCoordinateY = 0.65f,
                    storageType = "Pantry",
                    shelfLifeDays = 4,
                    quickCommerceRefPrice = 100.0
                )
            ),
            shopkeeperTotal = 910.0,
            calculatedTrueTotal = 910.0,
            mathErrorFlag = false,
            mathErrorDelta = 0.0,
            rawTranscript = "SUPERMART HYPERMARKET\nGSTIN: 27AAAAA0000A1Z5\nSpinach 250g - 85.00\nFeta Cheese 200g - 240.00\nOlive Oil 500ml - 490.00\nArtisan Bread - 95.00\nTOTAL: 910.00",
            samplePresetName = "SuperMart (Printed Thermal POS)"
        ),
        HandwrittenBillResult(
            vendorName = "Sharma Kirana & General Store",
            dateString = "19 Aug (Today)",
            invoiceType = "HANDWRITTEN",
            items = listOf(
                HandwrittenBillItem(
                    rawWrittenText = "Aata 5kg",
                    canonicalName = "Whole Wheat Flour (Atta)",
                    brand = "Aashirvaad",
                    category = "Grains",
                    subcategory = "Flour",
                    quantity = 5.0,
                    unit = "kg",
                    price = 210.0,
                    confidenceScore = 0.96f,
                    isLowConfidence = false,
                    cropCoordinateX = 0.28f,
                    cropCoordinateY = 0.22f,
                    storageType = "Pantry",
                    shelfLifeDays = 90,
                    quickCommerceRefPrice = 250.0
                ),
                HandwrittenBillItem(
                    rawWrittenText = "1 pav Amul makkhan",
                    canonicalName = "Salted Table Butter",
                    brand = "Amul",
                    category = "Dairy",
                    subcategory = "Spreads",
                    quantity = 250.0,
                    unit = "g",
                    price = 80.0,
                    confidenceScore = 0.58f, // Bad cursive scribble! Low confidence
                    isLowConfidence = true,
                    cropCoordinateX = 0.32f,
                    cropCoordinateY = 0.36f,
                    storageType = "Refrigerated",
                    shelfLifeDays = 45,
                    quickCommerceRefPrice = 88.0
                ),
                HandwrittenBillItem(
                    rawWrittenText = "Shakar / Cheeni 2kg",
                    canonicalName = "Refined White Sugar",
                    brand = "Madhur",
                    category = "Pantry",
                    subcategory = "Sweeteners",
                    quantity = 2.0,
                    unit = "kg",
                    price = 90.0,
                    confidenceScore = 0.94f,
                    isLowConfidence = false,
                    cropCoordinateX = 0.29f,
                    cropCoordinateY = 0.50f,
                    storageType = "Pantry",
                    shelfLifeDays = 365,
                    quickCommerceRefPrice = 96.0
                ),
                HandwrittenBillItem(
                    rawWrittenText = "Doodh 1/2L",
                    canonicalName = "Fresh Cow Milk (Toned)",
                    brand = "Amul Taaza",
                    category = "Dairy",
                    subcategory = "Fresh Milk",
                    quantity = 500.0,
                    unit = "ml",
                    price = 35.0,
                    confidenceScore = 0.91f,
                    isLowConfidence = false,
                    cropCoordinateX = 0.30f,
                    cropCoordinateY = 0.64f,
                    storageType = "Refrigerated",
                    shelfLifeDays = 2,
                    quickCommerceRefPrice = 36.0
                ),
                HandwrittenBillItem(
                    rawWrittenText = "Sarson Tel 1L",
                    canonicalName = "Kachi Ghani Mustard Oil",
                    brand = "Fortune",
                    category = "Pantry",
                    subcategory = "Cooking Oils",
                    quantity = 1.0,
                    unit = "L",
                    price = 140.0,
                    confidenceScore = 0.93f,
                    isLowConfidence = false,
                    cropCoordinateX = 0.31f,
                    cropCoordinateY = 0.78f,
                    storageType = "Pantry",
                    shelfLifeDays = 180,
                    quickCommerceRefPrice = 165.0
                )
            ),
            shopkeeperTotal = 585.0, // Written total on receipt: 585
            calculatedTrueTotal = 555.0, // 210 + 80 + 90 + 35 + 140 = 555. (Math error of +30!)
            mathErrorFlag = true,
            mathErrorDelta = 30.0,
            khataOldBalance = null,
            khataNewBalance = null,
            rawTranscript = "Aata 5kg - 210\n1 pav Amul makkhan - 80\nCheeni 2kg - 90\nDoodh 1/2 - 35\nSarson Tel 1L - 140\nTotal = 585",
            samplePresetName = "Sharma Kirana (Hinglish & Math Error)"
        ),
        HandwrittenBillResult(
            vendorName = "Gupta Provision Store",
            dateString = "18 Aug",
            items = listOf(
                HandwrittenBillItem(
                    rawWrittenText = "Toor Dal 1kg",
                    canonicalName = "Unpolished Toor Dal",
                    brand = "Tata Sampann",
                    category = "Grains",
                    subcategory = "Pulses",
                    quantity = 1.0,
                    unit = "kg",
                    price = 165.0,
                    confidenceScore = 0.95f,
                    cropCoordinateX = 0.30f,
                    cropCoordinateY = 0.30f,
                    storageType = "Pantry",
                    shelfLifeDays = 120,
                    quickCommerceRefPrice = 185.0
                ),
                HandwrittenBillItem(
                    rawWrittenText = "Basmati Chawal 2kg",
                    canonicalName = "Rozana Basmati Rice",
                    brand = "India Gate",
                    category = "Grains",
                    subcategory = "Rice",
                    quantity = 2.0,
                    unit = "kg",
                    price = 180.0,
                    confidenceScore = 0.92f,
                    cropCoordinateX = 0.30f,
                    cropCoordinateY = 0.45f,
                    storageType = "Pantry",
                    shelfLifeDays = 365,
                    quickCommerceRefPrice = 210.0
                ),
                HandwrittenBillItem(
                    rawWrittenText = "Tata Namak 1pk",
                    canonicalName = "Iodized Table Salt",
                    brand = "Tata",
                    category = "Pantry",
                    subcategory = "Spices",
                    quantity = 1.0,
                    unit = "kg",
                    price = 25.0,
                    confidenceScore = 0.98f,
                    cropCoordinateX = 0.30f,
                    cropCoordinateY = 0.60f,
                    storageType = "Pantry",
                    shelfLifeDays = 720,
                    quickCommerceRefPrice = 28.0
                ),
                HandwrittenBillItem(
                    rawWrittenText = "Lifebuoy Sabun 2pc",
                    canonicalName = "Antibacterial Bath Soap (2-Pack)",
                    brand = "Lifebuoy",
                    category = "Household",
                    subcategory = "Personal Care",
                    quantity = 2.0,
                    unit = "pcs",
                    price = 50.0,
                    confidenceScore = 0.88f,
                    cropCoordinateX = 0.30f,
                    cropCoordinateY = 0.75f,
                    storageType = "Pantry",
                    shelfLifeDays = 365,
                    quickCommerceRefPrice = 56.0
                )
            ),
            shopkeeperTotal = 420.0,
            calculatedTrueTotal = 420.0,
            mathErrorFlag = false,
            mathErrorDelta = 0.0,
            khataOldBalance = 650.0, // Running credit ledger
            khataNewBalance = 1070.0, // 650 + 420
            rawTranscript = "Pichla baki = 650\nToor dal 1kg - 165\nChawal 2kg - 180\nNamak 1pk - 25\nSabun 2 - 50\nTotal = 420\nTotal Baaki = 1070",
            samplePresetName = "Gupta Store (Khata Ledger Sync)"
        ),
        HandwrittenBillResult(
            vendorName = "Verma Dairy & Farm Fresh",
            dateString = "19 Aug",
            items = listOf(
                HandwrittenBillItem(
                    rawWrittenText = "Paneer 1/2kg",
                    canonicalName = "Fresh Malai Paneer",
                    brand = "Local Dairy",
                    category = "Dairy",
                    subcategory = "Cottage Cheese",
                    quantity = 500.0,
                    unit = "g",
                    price = 180.0,
                    confidenceScore = 0.97f,
                    cropCoordinateX = 0.30f,
                    cropCoordinateY = 0.30f,
                    storageType = "Refrigerated",
                    shelfLifeDays = 4,
                    quickCommerceRefPrice = 210.0
                ),
                HandwrittenBillItem(
                    rawWrittenText = "Matka Dahi 2pc",
                    canonicalName = "Artisan Clay Pot Curd",
                    brand = "Local Dairy",
                    category = "Dairy",
                    subcategory = "Curd",
                    quantity = 400.0,
                    unit = "g",
                    price = 70.0,
                    confidenceScore = 0.90f,
                    cropCoordinateX = 0.30f,
                    cropCoordinateY = 0.50f,
                    storageType = "Refrigerated",
                    shelfLifeDays = 5,
                    quickCommerceRefPrice = 80.0
                ),
                HandwrittenBillItem(
                    rawWrittenText = "Desi Ghee 1/2L",
                    canonicalName = "Pure Cow Ghee (Bilona)",
                    brand = "Farm Fresh",
                    category = "Dairy",
                    subcategory = "Ghee",
                    quantity = 500.0,
                    unit = "ml",
                    price = 320.0,
                    confidenceScore = 0.94f,
                    cropCoordinateX = 0.30f,
                    cropCoordinateY = 0.70f,
                    storageType = "Pantry",
                    shelfLifeDays = 180,
                    quickCommerceRefPrice = 390.0
                )
            ),
            shopkeeperTotal = 570.0,
            calculatedTrueTotal = 570.0,
            mathErrorFlag = false,
            mathErrorDelta = 0.0,
            khataOldBalance = null,
            khataNewBalance = null,
            rawTranscript = "Paneer 1/2 - 180\nDahi 2 - 70\nGhee 1/2L - 320\nTotal = 570",
            samplePresetName = "Verma Dairy (Colloquial Units)"
        )
    )
}
