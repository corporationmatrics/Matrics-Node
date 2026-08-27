package com.example.data

import com.example.data.model.ParsedNlpItem
import java.util.Locale
import java.util.regex.Pattern

data class ParsedEmailInvoice(
    val merchant: String,
    val orderId: String,
    val orderDate: String,
    val totalAmount: Double,
    val paymentMethod: String,
    val items: List<ParsedNlpItem>,
    val deliveryFee: Double = 0.0,
    val taxes: Double = 0.0,
    val discount: Double = 0.0,
    val rawText: String = "",
    val isGrocery: Boolean = false
)

data class EmailInvoiceSample(
    val appName: String,
    val title: String,
    val sender: String,
    val subject: String,
    val sampleBody: String
)

object EmailInvoiceParser {

    fun parse(emailText: String, appHint: String? = null): ParsedEmailInvoice {
        val clean = emailText.trim()
        val lower = clean.lowercase(Locale.ROOT)

        val detectedMerchant = when {
            appHint != null && appHint.isNotBlank() -> appHint
            lower.contains("zepto") -> "Zepto"
            lower.contains("zomato") -> "Zomato"
            lower.contains("swiggy") || lower.contains("instamart") -> if (lower.contains("instamart")) "Swiggy Instamart" else "Swiggy"
            lower.contains("blinkit") -> "Blinkit"
            lower.contains("amazon") -> "Amazon"
            lower.contains("uber") -> "Uber"
            lower.contains("ola") -> "Ola Cabs"
            lower.contains("flipkart") -> "Flipkart"
            lower.contains("bigbasket") -> "BigBasket"
            else -> extractGenericMerchant(clean)
        }

        val orderId = extractOrderId(clean)
        val orderDate = extractDate(clean)
        val paymentMethod = extractPaymentMethod(clean)
        val totalAmount = extractTotal(clean)

        val items = extractLineItems(clean, detectedMerchant)
        val isGrocery = detectedMerchant in listOf("Zepto", "Blinkit", "Swiggy Instamart", "BigBasket", "Dmart") ||
                items.any { it.category in listOf("Groceries", "Dairy", "Produce", "Pantry") }

        val finalItems = if (items.isNotEmpty()) {
            items
        } else {
            listOf(
                ParsedNlpItem(
                    name = "$detectedMerchant Order",
                    category = if (isGrocery) "Groceries" else if (detectedMerchant in listOf("Zomato", "Swiggy")) "Dining" else "Shopping",
                    quantity = 1.0,
                    unit = "order",
                    price = if (totalAmount > 0) totalAmount else 250.0,
                    vendor = detectedMerchant
                )
            )
        }

        val calculatedTotal = if (totalAmount > 0) totalAmount else finalItems.sumOf { it.price * it.quantity }

        return ParsedEmailInvoice(
            merchant = detectedMerchant,
            orderId = orderId,
            orderDate = orderDate,
            totalAmount = calculatedTotal,
            paymentMethod = paymentMethod,
            items = finalItems,
            rawText = clean,
            isGrocery = isGrocery
        )
    }

    private fun extractGenericMerchant(text: String): String {
        val pattern = Pattern.compile("(?i)(?:invoice from|order at|receipt from|welcome to)\\s+([A-Za-z0-9\\s&]+?)(?:\\n|\\.|\\,|#)")
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim()?.take(25) ?: "Online Store"
        }
        return "Digital Merchant"
    }

    private fun extractOrderId(text: String): String {
        val patterns = listOf(
            Pattern.compile("(?i)(?:order\\s*id|order\\s*#|invoice\\s*#|bill\\s*no|txn\\s*id)[:\\s#]*([A-Za-z0-9\\-_]{4,25})"),
            Pattern.compile("(?i)#([A-Za-z0-9\\-_]{6,16})")
        )
        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group(1)?.trim() ?: ""
            }
        }
        return "ORD" + System.currentTimeMillis().toString().takeLast(6)
    }

    private fun extractDate(text: String): String {
        val pattern = Pattern.compile("(?i)(?:date|ordered on|placed on)[:\\s]*([0-9]{1,2}[\\/\\-\\s][A-Za-z0-9]{3,9}[\\/\\-\\s][0-9]{2,4})")
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim() ?: "Today"
        }
        return "Today"
    }

    private fun extractPaymentMethod(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("upi") || lower.contains("gpay") || lower.contains("phonepe") -> "UPI Instant"
            lower.contains("credit card") -> "Credit Card"
            lower.contains("debit card") -> "Debit Card"
            lower.contains("netbanking") -> "Net Banking"
            lower.contains("cash on delivery") || lower.contains("cod") -> "Cash on Delivery"
            else -> "Digital Payment"
        }
    }

    private fun extractTotal(text: String): Double {
        val patterns = listOf(
            Pattern.compile("(?i)(?:total\\s*(?:amount|payable|paid|bill)?|grand\\s*total|amount\\s*paid|net\\s*amount)[:\\s]*₹?\\s*(?:rs\\.?|inr)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
            Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:paid|debited)")
        )
        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val numStr = matcher.group(1)?.replace(",", "")?.trim()
                val parsed = numStr?.toDoubleOrNull()
                if (parsed != null && parsed > 0) {
                    return parsed
                }
            }
        }
        return 0.0
    }

    private fun extractLineItems(text: String, vendor: String): List<ParsedNlpItem> {
        val items = mutableListOf<ParsedNlpItem>()
        val lines = text.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue
            if (trimmed.startsWith("Total", ignoreCase = true) || 
                trimmed.startsWith("Subtotal", ignoreCase = true) ||
                trimmed.startsWith("Delivery Fee", ignoreCase = true) ||
                trimmed.startsWith("Taxes", ignoreCase = true) ||
                trimmed.startsWith("Discount", ignoreCase = true) ||
                trimmed.startsWith("GST", ignoreCase = true)) {
                continue
            }

            // Patterns like:
            // "1x Amul Taaza Milk 1L - Rs. 54.00"
            // "Amul Butter 500g (Qty: 1) ₹285"
            // "Chicken Dum Biryani x 2 : Rs 520"
            // "Whole Wheat Bread 400g - 1 - Rs. 45"
            val itemPattern = Pattern.compile("(?i)(?:([0-9]{1,2})\\s*[xX]\\s+)?([A-Za-z0-9\\s\\(\\)\\.\\-_\\/]+?)(?:\\s*[xX]\\s*([0-9]{1,2}))?\\s*(?:[-:–|₹]|Rs\\.?|INR)\\s*(?:Rs\\.?|INR|₹)?\\s*([0-9]+(?:\\.[0-9]{1,2})?)")
            val matcher = itemPattern.matcher(trimmed)
            if (matcher.find()) {
                val qtyStr1 = matcher.group(1)
                val rawName = matcher.group(2)?.trim() ?: ""
                val qtyStr2 = matcher.group(3)
                val priceStr = matcher.group(4)

                val quantity = (qtyStr1 ?: qtyStr2)?.toDoubleOrNull() ?: 1.0
                val price = priceStr?.toDoubleOrNull() ?: 0.0

                if (rawName.length in 3..50 && price > 0 && 
                    !rawName.contains("order", ignoreCase = true) && 
                    !rawName.contains("invoice", ignoreCase = true) &&
                    !rawName.contains("delivery", ignoreCase = true)) {

                    val (category, canonical, shelfLife) = guessItemAttributes(rawName, vendor)

                    items.add(
                        ParsedNlpItem(
                            name = rawName,
                            category = category,
                            quantity = quantity,
                            unit = extractUnitFromName(rawName),
                            price = price / (if (quantity > 1) quantity else 1.0),
                            vendor = vendor,
                            canonicalName = canonical,
                            shelfLifeDays = shelfLife,
                            storageType = if (category == "Dairy" || category == "Produce") "Refrigerated" else "Pantry"
                        )
                    )
                }
            }
        }
        return items
    }

    private fun extractUnitFromName(name: String): String {
        val lower = name.lowercase(Locale.ROOT)
        return when {
            lower.contains("500g") -> "500g"
            lower.contains("1kg") || lower.contains("1 kg") -> "1kg"
            lower.contains("2kg") || lower.contains("2 kg") -> "2kg"
            lower.contains("1l") || lower.contains("1 l") || lower.contains("1 litre") -> "1L"
            lower.contains("500ml") -> "500ml"
            lower.contains("12pk") || lower.contains("12 pk") || lower.contains("12 eggs") -> "12pk"
            lower.contains("6pk") || lower.contains("6 pk") -> "6pk"
            lower.contains("loaf") -> "loaf"
            lower.contains("packet") || lower.contains("pack") -> "pack"
            else -> "unit"
        }
    }

    private fun guessItemAttributes(name: String, vendor: String): Triple<String, String, Int> {
        val lower = name.lowercase(Locale.ROOT)
        return when {
            lower.contains("milk") -> Triple("Dairy", "Milk", 7)
            lower.contains("butter") -> Triple("Dairy", "Butter", 180)
            lower.contains("paneer") || lower.contains("cheese") -> Triple("Dairy", "Paneer / Cheese", 14)
            lower.contains("curd") || lower.contains("yogurt") -> Triple("Dairy", "Yogurt", 14)
            lower.contains("egg") -> Triple("Produce", "Eggs", 21)
            lower.contains("bread") || lower.contains("bun") || lower.contains("pav") -> Triple("Grains", "Bread", 4)
            lower.contains("rice") || lower.contains("atta") || lower.contains("flour") || lower.contains("dal") -> Triple("Pantry", "Staples", 365)
            lower.contains("oil") || lower.contains("ghee") -> Triple("Pantry", "Cooking Oil", 365)
            lower.contains("biryani") || lower.contains("pizza") || lower.contains("burger") || lower.contains("pasta") || lower.contains("roll") -> Triple("Dining", "Prepared Meal", 1)
            vendor in listOf("Zomato", "Swiggy") -> Triple("Dining", "Food Item", 1)
            vendor in listOf("Zepto", "Blinkit", "Swiggy Instamart") -> Triple("Groceries", name.take(15), 30)
            vendor in listOf("Uber", "Ola Cabs") -> Triple("Transport", "Ride Fare", 0)
            else -> Triple("Shopping", name.take(15), 365)
        }
    }

    val SAMPLES: List<EmailInvoiceSample> = listOf(
        EmailInvoiceSample(
            appName = "Zepto",
            title = "Zepto: 10-Min Grocery (₹485)",
            sender = "orders@zeptonow.com",
            subject = "Your Zepto order #ZP-98421 has been delivered! ⚡",
            sampleBody = """
                Hi Upendra,
                Thank you for ordering with Zepto! Here is your invoice:

                Order ID: #ZP-98421
                Delivery Date: 20 Aug 2026, 14:15 PM
                Payment Mode: Paid via UPI (GPay)

                ITEMS ORDERED:
                1x Amul Taaza Toned Milk 1L - Rs. 54.00
                1x Amul Salted Butter 500g - Rs. 285.00
                1x Eggoz Farm Fresh Eggs 12pk - Rs. 128.00
                1x Fresh Coriander Bunch 100g - Rs. 18.00

                Item Total: Rs. 485.00
                Handling Fee: Rs. 0.00
                Delivery Fee: FREE
                Grand Total: Rs. 485.00

                GSTIN: 27AABCV8941N1Z0
                Delivered in 8 minutes!
            """.trimIndent()
        ),
        EmailInvoiceSample(
            appName = "Zomato",
            title = "Zomato: Meghana Biryani (₹680)",
            sender = "noreply@zomato.com",
            subject = "Order Delivered: Meghana Foods, Koramangala (Order #ZOM-49102)",
            sampleBody = """
                Here is the invoice for your order from Meghana Foods:

                Order #ZOM-49102
                Date: 20-Aug-2026 19:30
                Payment: Paid Online (HDFC Card Ending 4092)

                Order Summary:
                Special Chicken Dum Biryani x 1 - Rs. 380.00
                Butter Naan x 3 - Rs. 180.00
                Gulab Jamun (2 pcs) x 1 - Rs. 80.00

                Subtotal: Rs. 640.00
                Restaurant Taxes (GST 5%): Rs. 32.00
                Delivery Partner Fee: Rs. 40.00
                Zomato Gold Discount: -Rs. 32.00
                Total Amount Paid: Rs. 680.00
            """.trimIndent()
        ),
        EmailInvoiceSample(
            appName = "Blinkit",
            title = "Blinkit: Pantry Restock (₹735)",
            sender = "invoices@blinkit.com",
            subject = "Invoice for Blinkit Order #BLK-77192",
            sampleBody = """
                Blinkit Commerce Pvt Ltd
                Order ID: BLK-77192
                Placed on: 20 Aug 2026
                Paid via: PhonePe UPI

                Line Items:
                Fortune Sunlite Sunflower Oil 1L x 1 - Rs. 145.00
                Aashirvaad Shudh Chakki Atta 5kg x 1 - Rs. 240.00
                Epigamia Greek Yogurt Natural 400g x 2 - Rs. 190.00
                Harvest Gold Hearty Brown Bread 400g x 1 - Rs. 50.00
                Haldiram's Bhujia 400g x 1 - Rs. 110.00

                Grand Total: Rs. 735.00
            """.trimIndent()
        ),
        EmailInvoiceSample(
            appName = "Amazon",
            title = "Amazon: Anker USB-C Hub (₹1,499)",
            sender = "auto-confirm@amazon.in",
            subject = "Your Amazon.in order #402-894102-1928491",
            sampleBody = """
                Amazon.in Tax Invoice
                Order Placed: 20 August 2026
                Amazon.in order number: #402-894102-1928491
                Payment method: Amazon Pay ICICI Credit Card

                Items Ordered:
                Anker 5-in-1 USB-C Data Hub with HDMI x 1 - Rs. 1,499.00

                Item Subtotal: Rs. 1,499.00
                Shipping & Handling: Rs. 0.00
                Total for this Order: Rs. 1,499.00
            """.trimIndent()
        ),
        EmailInvoiceSample(
            appName = "Uber",
            title = "Uber: Ride to Airport (₹540)",
            sender = "uber.india@uber.com",
            subject = "Your Wednesday evening trip with Uber",
            sampleBody = """
                Trip Invoice from Uber B.V.
                Trip ID: UB-984218
                Date: 20 Aug 2026, 06:15 PM
                Payment: Paytm Wallet

                Trip Breakdown:
                Uber Premier Airport Fare x 1 - Rs. 490.00
                Toll & Parking charges - Rs. 50.00

                Total Paid: Rs. 540.00
            """.trimIndent()
        )
    )
}
