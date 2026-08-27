package com.example.data

import com.example.data.model.HandwrittenBillItem
import com.example.data.model.HandwrittenBillResult
import com.example.data.model.ParsedNlpItem
import java.util.Locale
import java.util.UUID

/**
 * Robust on-device deterministic Receipt & Invoice OCR Parsing Engine.
 * Capable of processing raw OCR transcriptions, thermal POS receipts, and informal handwritten bills
 * with zero internet connection or as a fallback when Gemini API quotas are exhausted.
 */
object ReceiptOcrEngine {

    data class OcrLineItem(
        val rawText: String,
        val canonicalName: String,
        val brand: String = "",
        val category: String = "Groceries",
        val subcategory: String = "",
        val quantity: Double = 1.0,
        val unit: String = "unit",
        val unitPrice: Double = 0.0,
        val totalPrice: Double = 0.0,
        val storageType: String = "Pantry",
        val shelfLifeDays: Int = 30,
        val confidence: Float = 0.95f,
        val isLowConfidence: Boolean = false
    )

    data class OcrParsedReceipt(
        val vendorName: String = "Retail Store",
        val dateString: String = "Today",
        val invoiceType: String = "PRINTED", // "PRINTED" or "HANDWRITTEN"
        val items: List<OcrLineItem> = emptyList(),
        val subtotal: Double = 0.0,
        val taxAmount: Double = 0.0,
        val discountAmount: Double = 0.0,
        val grandTotal: Double = 0.0,
        val calculatedSum: Double = 0.0,
        val mathError: Boolean = false,
        val mathErrorDelta: Double = 0.0,
        val paymentMethod: String = "Cash",
        val gstOrTaxId: String = "",
        val rawTranscript: String = ""
    )

    // Keywords that indicate summary / footer / metadata lines to ignore as product line items
    private val IGNORED_LINE_PATTERNS = listOf(
        Regex("""(?i)\b(subtotal|sub-total|sub\s*total)\b"""),
        Regex("""(?i)\b(grand\s*total|net\s*total|net\s*payable|total\s*amount|total\s*due|bal\s*due|amount\s*due|total\s*amt)\b"""),
        Regex("""(?i)\b(total\s*items|total\s*qty|item\s*count|items\s*count|qty\s*total)\b"""),
        Regex("""(?i)\b(cgst|sgst|igst|vat|sales\s*tax|tax\s*amount|tax\s*total|total\s*tax|gst\s*@)\b"""),
        Regex("""(?i)\b(discount|savings|you\s*saved|promo\s*disc|coupon|special\s*discount)\b"""),
        Regex("""(?i)\b(cash\s*tendered|cash\s*paid|cash\s*given|change\s*due|balance\s*returned|change\s*amt)\b"""),
        Regex("""(?i)\b(upi\s*ref|upi\s*trans|rrn|txn\s*id|transaction\s*id|auth\s*code|approval\s*code|terminal\s*id)\b"""),
        Regex("""(?i)\b(visa|mastercard|amex|rupay|debit\s*card|credit\s*card|card\s*no|card\s*xxxx|xxxx\s*xxxx)\b"""),
        Regex("""(?i)\b(gstin|gst\s*no|tax\s*invoice|retail\s*invoice|cash\s*memo|bill\s*no|receipt\s*no|invoice\s*no)\b"""),
        Regex("""(?i)\b(thank\s*you|visit\s*again|welcome|have\s*a\s*nice\s*day|terms\s*&\s*conditions|no\s*refund)\b"""),
        Regex("""(?i)\b(phone|tel|mobile|call|website|www\.|http|email|fssai|cin\b|reg\s*no)\b"""),
        Regex("""(?i)\b(date\s*:|time\s*:|cashier\s*:|pos\s*no|counter\s*:|shift\s*:|store\s*id)\b"""),
        Regex("""^[\s\-_=.*#:]{2,}$""") // Separator lines
    )

    private val KNOWN_STORE_PATTERNS = listOf(
        "reliance smart" to "Reliance Smart",
        "reliance fresh" to "Reliance Fresh",
        "dmart" to "DMart Supermarket",
        "star bazaar" to "Star Bazaar",
        "spencer" to "Spencer's Retail",
        "more retail" to "More Supermarket",
        "nature's basket" to "Nature's Basket",
        "trader joe" to "Trader Joe's",
        "whole foods" to "Whole Foods Market",
        "walmart" to "Walmart",
        "target" to "Target",
        "costco" to "Costco Wholesale",
        "safeway" to "Safeway",
        "kroger" to "Kroger",
        "aldi" to "ALDI",
        "blinkit" to "Blinkit",
        "zepto" to "Zepto",
        "instamart" to "Swiggy Instamart",
        "supermart" to "SuperMart Hypermarket",
        "kirana" to "Sharma Kirana Store",
        "provision" to "Gupta Provision Store",
        "general store" to "City General Store",
        "starbucks" to "Starbucks Coffee",
        "mcdonald" to "McDonald's",
        "wendy" to "Wendy's",
        "subway" to "Subway"
    )

    /**
     * Parses raw OCR text lines into a structured receipt object.
     */
    fun parseReceiptText(rawText: String): OcrParsedReceipt {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return OcrParsedReceipt(rawTranscript = rawText)
        }

        var detectedVendor = "Retail Store"
        var detectedDate = "Today"
        var detectedGst = ""
        var isHandwritten = false
        var grandTotal = 0.0
        var subtotal = 0.0
        var taxAmount = 0.0
        var discountAmount = 0.0
        var paymentMethod = "Cash"

        // 1. Detect Vendor from the first 4 non-empty lines
        for (i in 0 until minOf(5, lines.size)) {
            val line = lines[i]
            val lowerLine = line.lowercase(Locale.getDefault())
            
            // Match known store patterns
            val matchedStore = KNOWN_STORE_PATTERNS.find { lowerLine.contains(it.first) }
            if (matchedStore != null) {
                detectedVendor = matchedStore.second
                break
            } else if (i == 0 && !line.matches(Regex("""(?i).*(tax|invoice|bill|receipt|gst|date|tel|phone).*"""))) {
                // If the first line doesn't look like generic header text, take it as merchant name
                if (line.length in 3..35) {
                    detectedVendor = line.replace(Regex("""[^a-zA-Z0-9\s&'.-]"""), "").trim()
                }
            }
        }

        // 2. Scan lines for Date, GST, Totals, and Line Items
        val extractedItems = mutableListOf<OcrLineItem>()
        val dateRegex = Regex("""\b(\d{1,2}[./-]\d{1,2}[./-]\d{2,4}|\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+\d{2,4})\b""", RegexOption.IGNORE_CASE)
        val gstRegex = Regex("""\b(\d{2}[A-Z]{5}\d{4}[A-Z]{1}[A-Z0-9]{1}[Z]{1}[A-Z0-9]{1})\b""")

        for (line in lines) {
            // Check Date
            val dateMatch = dateRegex.find(line)
            if (dateMatch != null && detectedDate == "Today") {
                detectedDate = dateMatch.value
            }

            // Check GSTIN
            val gstMatch = gstRegex.find(line)
            if (gstMatch != null) {
                detectedGst = gstMatch.value
            }

            // Check Grand Total
            if (line.matches(Regex("""(?i).*\b(grand\s*total|net\s*total|net\s*payable|total\s*amount|total\s*due|bal\s*due|amount\s*due)\b.*"""))) {
                val amt = extractFirstMoneyAmount(line)
                if (amt > 0.0) grandTotal = amt
                continue
            }

            // Check Subtotal
            if (line.matches(Regex("""(?i).*\b(subtotal|sub-total|sub\s*total)\b.*"""))) {
                val amt = extractFirstMoneyAmount(line)
                if (amt > 0.0) subtotal = amt
                continue
            }

            // Check Tax / GST
            if (line.matches(Regex("""(?i).*\b(cgst|sgst|igst|vat|sales\s*tax|total\s*tax)\b.*"""))) {
                val amt = extractFirstMoneyAmount(line)
                if (amt > 0.0) taxAmount += amt
                continue
            }

            // Check Discount
            if (line.matches(Regex("""(?i).*\b(discount|savings|you\s*saved)\b.*"""))) {
                val amt = extractFirstMoneyAmount(line)
                if (amt > 0.0) discountAmount += amt
                continue
            }

            // Check Payment Method
            if (line.matches(Regex("""(?i).*\b(upi|gpay|phonepe|paytm)\b.*"""))) {
                paymentMethod = "UPI Instant"
            } else if (line.matches(Regex("""(?i).*\b(visa|mastercard|credit|debit|rupay|card)\b.*"""))) {
                paymentMethod = "Credit Card"
            }

            // Check if this line is an ignored summary/header line
            if (IGNORED_LINE_PATTERNS.any { it.containsMatchIn(line) }) {
                continue
            }

            // Check for colloquial handwritten Hinglish patterns
            if (line.matches(Regex("""(?i).*\b(aata|atta|makkhan|cheeni|shakar|doodh|sarson|tel|dal|masala|pav|adha|bora|peti)\b.*"""))) {
                isHandwritten = true
            }

            // Parse Line Item
            val parsedItem = parseReceiptLine(line, detectedVendor)
            if (parsedItem != null) {
                extractedItems.add(parsedItem)
            }
        }

        val calculatedSum = extractedItems.sumOf { it.totalPrice }
        val finalGrandTotal = if (grandTotal > 0.0) grandTotal else if (subtotal > 0.0) subtotal + taxAmount - discountAmount else calculatedSum
        val hasMathMismatch = grandTotal > 0.0 && Math.abs(grandTotal - calculatedSum) > 1.0

        return OcrParsedReceipt(
            vendorName = detectedVendor,
            dateString = detectedDate,
            invoiceType = if (isHandwritten) "HANDWRITTEN" else "PRINTED",
            items = extractedItems,
            subtotal = if (subtotal > 0.0) subtotal else calculatedSum,
            taxAmount = taxAmount,
            discountAmount = discountAmount,
            grandTotal = finalGrandTotal,
            calculatedSum = calculatedSum,
            mathError = hasMathMismatch,
            mathErrorDelta = if (hasMathMismatch) (grandTotal - calculatedSum) else 0.0,
            paymentMethod = paymentMethod,
            gstOrTaxId = detectedGst,
            rawTranscript = rawText
        )
    }

    /**
     * Parses a single text line into a structured commodity line item
     */
    private fun parseReceiptLine(line: String, vendor: String): OcrLineItem? {
        val trimmed = line.trim()
        if (trimmed.length < 3) return null

        // Format 1: Multiplier pattern (e.g. "2 x 45.00 Fresh Butter 90.00" or "Organic Milk 2 @ 35.00 = 70.00")
        val multiplierRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:x|@|\*)\s*(\d+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
        val multMatch = multiplierRegex.find(trimmed)
        var qty = 1.0
        var unitPrice = 0.0
        var totalPrice = 0.0
        var itemDesc = trimmed

        if (multMatch != null) {
            qty = multMatch.groupValues[1].toDoubleOrNull() ?: 1.0
            unitPrice = multMatch.groupValues[2].toDoubleOrNull() ?: 0.0
            totalPrice = qty * unitPrice

            // Remove multiplier from line
            itemDesc = trimmed.replace(multMatch.value, " ").trim()
        }

        // Extract Trailing Price if total price not calculated or if explicit price at end of line (e.g. "Apples 1.5kg 120.00")
        val trailingPriceRegex = Regex("""(?:₹|\$|rs\.?)?\s*(\d+(?:\.\d{1,2})?)\s*$""", RegexOption.IGNORE_CASE)
        val priceMatch = trailingPriceRegex.find(itemDesc)
        if (priceMatch != null) {
            val extractedPrice = priceMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            if (extractedPrice > 0.0) {
                if (totalPrice == 0.0) {
                    totalPrice = extractedPrice
                    if (qty > 0) unitPrice = totalPrice / qty
                } else if (Math.abs(totalPrice - extractedPrice) < 1.0) {
                    totalPrice = extractedPrice
                }
                itemDesc = itemDesc.substring(0, priceMatch.range.first).trim()
            }
        }

        // If no price was detected at all, skip line if it doesn't look like an item
        if (totalPrice <= 0.0) {
            // Attempt to find any numeric money figure in line
            val anyNumber = Regex("""\b(\d+(?:\.\d{1,2})?)\b""").findAll(trimmed).mapNotNull { it.groupValues[1].toDoubleOrNull() }.toList()
            if (anyNumber.isNotEmpty()) {
                totalPrice = anyNumber.last()
                unitPrice = totalPrice
            } else {
                return null
            }
        }

        // Extract Unit & Quantity from item description (e.g. "Butter 500g", "Milk 1L", "Rice 5kg", "Bread 1 loaf", "1 pav Amul")
        var detectedUnit = "unit"
        var isLowConfidence = false

        // Colloquial unit conversions:
        if (itemDesc.matches(Regex("""(?i).*\b(1\s*pav|ek\s*pav|pav)\b.*"""))) {
            qty = 250.0
            detectedUnit = "g"
            itemDesc = itemDesc.replace(Regex("""(?i)\b(1\s*pav|ek\s*pav|pav)\b"""), "").trim()
            isLowConfidence = true
        } else if (itemDesc.matches(Regex("""(?i).*\b(adha|aadha|1/2)\b.*"""))) {
            qty = 0.5
            itemDesc = itemDesc.replace(Regex("""(?i)\b(adha|aadha|1/2)\b"""), "").trim()
        }

        val unitRegex = Regex("""(\d+(?:\.\d+)?)\s*(kg|kgs|kilogram|g|gm|gms|gram|grams|l|ltr|liter|litres|ml|milli|pcs|pc|pack|packet|packs|can|bottle|loaf|bunch|units)\b""", RegexOption.IGNORE_CASE)
        val unitMatch = unitRegex.find(itemDesc)
        if (unitMatch != null) {
            qty = unitMatch.groupValues[1].toDoubleOrNull() ?: qty
            val rawUnit = unitMatch.groupValues[2].lowercase(Locale.getDefault())
            detectedUnit = when {
                rawUnit.startsWith("kg") || rawUnit.startsWith("kilo") -> "kg"
                rawUnit.startsWith("g") -> "g"
                rawUnit.startsWith("l") && !rawUnit.startsWith("loaf") -> "L"
                rawUnit.startsWith("ml") -> "ml"
                rawUnit.startsWith("loaf") -> "loaf"
                rawUnit.startsWith("can") -> "can"
                rawUnit.startsWith("bottle") -> "bottle"
                rawUnit.startsWith("pack") -> "pack"
                rawUnit.startsWith("pc") -> "pcs"
                else -> "unit"
            }
            itemDesc = itemDesc.replace(unitMatch.value, " ").trim()
        }

        // Clean up item name: remove barcodes, leading numbers, punctuation
        itemDesc = itemDesc.replace(Regex("""^\d+[\s\-.)]+"""), "") // Leading index e.g. "1. "
            .replace(Regex("""\b\d{6,}\b"""), "") // Barcode / SKU
            .replace(Regex("""(?i)\b(hsn|sku|item|desc|art|qty|rate|mrp|disc|amt)\b"""), "")
            .replace(Regex("""[#*:=_]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (itemDesc.isBlank() || itemDesc.length < 2) return null

        // Canonical normalization and category enrichment using CategorySuggester
        val canonicalName = itemDesc.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale.ROOT) else c.toString() }
        }
        val initialItem = ParsedNlpItem(
            name = canonicalName,
            category = CategorySuggester.suggestCategory(canonicalName),
            quantity = qty,
            unit = detectedUnit,
            price = totalPrice,
            vendor = vendor,
            canonicalName = canonicalName
        )
        val enriched = CategorySuggester.enrichItem(initialItem)

        return OcrLineItem(
            rawText = line.trim(),
            canonicalName = enriched.canonicalName,
            brand = enriched.brand,
            category = enriched.category,
            subcategory = enriched.subcategory,
            quantity = qty,
            unit = detectedUnit,
            unitPrice = if (unitPrice > 0.0) unitPrice else totalPrice,
            totalPrice = totalPrice,
            storageType = enriched.storageType,
            shelfLifeDays = enriched.shelfLifeDays,
            confidence = if (isLowConfidence) 0.65f else 0.95f,
            isLowConfidence = isLowConfidence
        )
    }

    /**
     * Converts OCR parsed receipt directly to the unified HandwrittenBillResult
     */
    fun toHandwrittenBillResult(parsed: OcrParsedReceipt): HandwrittenBillResult {
        val mappedItems = parsed.items.mapIndexed { index, item ->
            HandwrittenBillItem(
                id = UUID.randomUUID().toString(),
                rawWrittenText = item.rawText,
                canonicalName = item.canonicalName,
                brand = item.brand,
                category = item.category,
                subcategory = item.subcategory,
                quantity = item.quantity,
                unit = item.unit,
                price = item.totalPrice,
                confidenceScore = item.confidence,
                isLowConfidence = item.isLowConfidence,
                cropCoordinateX = 0.28f,
                cropCoordinateY = (0.18f + index * 0.14f).coerceAtMost(0.88f),
                storageType = item.storageType,
                shelfLifeDays = item.shelfLifeDays,
                quickCommerceRefPrice = item.totalPrice * 1.12
            )
        }

        return HandwrittenBillResult(
            vendorName = parsed.vendorName,
            dateString = parsed.dateString,
            items = mappedItems,
            shopkeeperTotal = parsed.grandTotal,
            calculatedTrueTotal = parsed.calculatedSum,
            mathErrorFlag = parsed.mathError,
            mathErrorDelta = parsed.mathErrorDelta,
            khataOldBalance = null,
            khataNewBalance = null,
            rawTranscript = parsed.rawTranscript,
            invoiceType = parsed.invoiceType
        )
    }

    /**
     * Converts OCR parsed receipt items to ParsedNlpItem list
     */
    fun toParsedNlpItems(parsed: OcrParsedReceipt): List<ParsedNlpItem> {
        return parsed.items.map { item ->
            ParsedNlpItem(
                name = if (item.brand.isNotBlank() && !item.canonicalName.contains(item.brand, ignoreCase = true)) "${item.brand} ${item.canonicalName}" else item.canonicalName,
                category = item.category,
                quantity = item.quantity,
                unit = item.unit,
                price = item.totalPrice,
                vendor = parsed.vendorName,
                canonicalName = item.canonicalName,
                brand = item.brand,
                subcategory = item.subcategory,
                storageType = item.storageType,
                shelfLifeDays = item.shelfLifeDays,
                tierResolved = "TIER_2_OFFLINE_OCR"
            )
        }
    }

    private fun extractFirstMoneyAmount(text: String): Double {
        val matches = Regex("""(\d+(?:\.\d{1,2})?)""").findAll(text).toList()
        return matches.lastOrNull()?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    }
}
