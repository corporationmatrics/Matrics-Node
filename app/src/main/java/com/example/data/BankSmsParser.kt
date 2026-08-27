package com.example.data

import java.util.Locale
import java.util.regex.Pattern

data class ParsedBankSms(
    val bankName: String,
    val amount: Double,
    val txnType: String, // "DEBIT", "CREDIT", "UPI", "CARD", "ATM"
    val merchant: String,
    val category: String,
    val accountLast4: String,
    val utrOrRef: String,
    val rawSms: String,
    val timestamp: Long,
    val confidence: String = "HIGH"
)

data class BankSmsSample(
    val bankName: String,
    val label: String,
    val sender: String,
    val body: String
)

object BankSmsParser {

    private val BANK_IDENTIFIERS = mapOf(
        "HDFC" to "HDFC Bank",
        "SBI" to "State Bank of India",
        "SBIN" to "State Bank of India",
        "ICICI" to "ICICI Bank",
        "AXIS" to "Axis Bank",
        "KOTAK" to "Kotak Mahindra Bank",
        "PNB" to "Punjab National Bank",
        "CANARA" to "Canara Bank",
        "INDUS" to "IndusInd Bank",
        "IDFC" to "IDFC FIRST Bank",
        "BOB" to "Bank of Baroda",
        "BARODA" to "Bank of Baroda",
        "PAYTM" to "Paytm Payments Bank",
        "FEDERAL" to "Federal Bank",
        "YES" to "Yes Bank",
        "RBL" to "RBL Bank",
        "SCB" to "Standard Chartered",
        "CITI" to "Citi Bank",
        "UNION" to "Union Bank of India"
    )

    fun parse(sender: String?, body: String, timestamp: Long = System.currentTimeMillis()): ParsedBankSms? {
        val cleanBody = body.trim()
        if (cleanBody.length < 15) return null

        val isDebit = isDebitMessage(cleanBody)
        if (!isDebit && !isUpiDebit(cleanBody)) {
            // Only capture debits / spends for expense tracking
            return null
        }

        val amount = extractAmount(cleanBody) ?: return null
        val bankName = detectBankName(sender, cleanBody)
        val merchant = extractMerchant(cleanBody)
        val accountLast4 = extractAccountOrCard(cleanBody)
        val utr = extractUtr(cleanBody)
        val category = categorizeMerchant(merchant, cleanBody)
        val txnType = if (cleanBody.contains("UPI", ignoreCase = true)) "UPI" 
                      else if (cleanBody.contains("Card", ignoreCase = true)) "CARD" 
                      else "DEBIT"

        return ParsedBankSms(
            bankName = bankName,
            amount = amount,
            txnType = txnType,
            merchant = merchant,
            category = category,
            accountLast4 = accountLast4,
            utrOrRef = utr,
            rawSms = cleanBody,
            timestamp = timestamp,
            confidence = if (merchant != "Merchant" && utr.isNotBlank()) "HIGH" else "MEDIUM"
        )
    }

    private fun isDebitMessage(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        val debitKeywords = listOf(
            "debited", "spent", "paid", "sent", "withdrawn", "purchase", "dr.", "deducted", "transfer to"
        )
        val ignoreKeywords = listOf("will be debited", "requested", "failed", "reversed", "credited", "refund")
        
        if (ignoreKeywords.any { lower.contains(it) && !lower.contains("was debited") }) {
            return false
        }
        return debitKeywords.any { lower.contains(it) }
    }

    private fun isUpiDebit(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return lower.contains("upi") && (lower.contains("paid") || lower.contains("sent") || lower.contains("transferred") || lower.contains("debited"))
    }

    private fun extractAmount(text: String): Double? {
        // Regex patterns for Indian banking currency formats:
        // Rs. 450.00, Rs 1,250, INR 340.50, INR: 99.00, debited by Rs.500, paid Rs 320
        val patterns = listOf(
            Pattern.compile("(?i)(?:rs\\.?|inr|debited\\s+by|spent|paid|amount\\s*(?:of)?)\\s*[:\\s]?\\s*(?:rs\\.?|inr)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
            Pattern.compile("(?i)([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:rs\\.?|inr)\\s*(?:debited|spent|paid)"),
            Pattern.compile("(?i)(?:vpa|to)\\s+.*?\\s+(?:for|of)\\s+(?:rs\\.?|inr)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val amtStr = matcher.group(1)?.replace(",", "")?.trim()
                val parsed = amtStr?.toDoubleOrNull()
                if (parsed != null && parsed > 0 && parsed < 10_000_000) {
                    return parsed
                }
            }
        }
        return null
    }

    private fun detectBankName(sender: String?, text: String): String {
        val upperSender = (sender ?: "").uppercase(Locale.ROOT)
        val upperText = text.uppercase(Locale.ROOT)

        for ((key, name) in BANK_IDENTIFIERS) {
            if (upperSender.contains(key) || upperText.contains(key)) {
                return name
            }
        }
        return "Bank Account"
    }

    private fun extractMerchant(text: String): String {
        val patterns = listOf(
            // "at ZOMATO on", "to RAMESH on", "towards SWIGGY", "info: STARBUCKS"
            Pattern.compile("(?i)(?:at|to|info|towards|vpa|paid to|transferred to|spent on)\\s+([A-Za-z0-9\\.\\-\\_\'\\&\\s@]+?)(?:\\s+on|\\s+ref|\\s+upi|\\s+avl|\\s+bal|\\s+ending|\\s+thru|\\s+via|\\.|\\,|$)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)vpa\\s+([a-zA-Z0-9\\.\\_\\-]+@[a-zA-Z0-9]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)merchant[:\\s]+([A-Za-z0-9\\.\\-\\_\'\\s]+)", Pattern.CASE_INSENSITIVE)
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val raw = matcher.group(1)?.trim() ?: ""
                val clean = raw.replace(Regex("(?i)^(the|a)\\s+"), "")
                    .replace(Regex("(?i)\\s+(ltd|pvt|private|limited|inc|corp)"), "")
                    .trim()
                if (clean.length in 3..35 && !clean.contains("account", ignoreCase = true) && !clean.contains("bank", ignoreCase = true)) {
                    return clean
                }
            }
        }
        return "Merchant / Store"
    }

    private fun extractAccountOrCard(text: String): String {
        val pattern = Pattern.compile("(?i)(?:a\\/c|acct|card|xx|ending|acc\\s*no)\\s*(?:no\\.?)?\\s*[:\\s]?\\s*([xX\\*]*\\d{3,4})")
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            val raw = matcher.group(1)?.trim() ?: ""
            return raw.replace(Regex("[^0-9]"), "").takeLast(4).let { if (it.isNotBlank()) "••$it" else "••XX" }
        }
        return "••Bank"
    }

    private fun extractUtr(text: String): String {
        val pattern = Pattern.compile("(?i)(?:ref|rrn|upi\\s*ref|txn|utr|id)[\"\\s\\:\\#\\-]*([A-Za-z0-9]{6,20})")
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim() ?: ""
        }
        return "UTR" + System.currentTimeMillis().toString().takeLast(6)
    }

    fun categorizeMerchant(merchant: String, body: String): String {
        val combined = "$merchant $body".lowercase(Locale.ROOT)

        return when {
            combined.contains("zomato") || combined.contains("swiggy") || combined.contains("mcdonald") ||
            combined.contains("starbucks") || combined.contains("kfc") || combined.contains("domino") ||
            combined.contains("burger") || combined.contains("cafe") || combined.contains("restaurant") ||
            combined.contains("pizza") || combined.contains("dine") || combined.contains("food") -> "Dining"

            combined.contains("zepto") || combined.contains("blinkit") || combined.contains("instamart") ||
            combined.contains("bigbasket") || combined.contains("dmart") || combined.contains("supermarket") ||
            combined.contains("kirana") || combined.contains("grocer") || combined.contains("nature's basket") ||
            combined.contains("milk") || combined.contains("vegetables") -> "Groceries"

            combined.contains("uber") || combined.contains("ola") || combined.contains("rapido") ||
            combined.contains("metro") || combined.contains("irctc") || combined.contains("makemytrip") ||
            combined.contains("indigo") || combined.contains("flight") || combined.contains("petrol") ||
            combined.contains("fuel") || combined.contains("hpcl") || combined.contains("bpcl") ||
            combined.contains("iocl") || combined.contains("shell") -> "Transport"

            combined.contains("amazon") || combined.contains("flipkart") || combined.contains("myntra") ||
            combined.contains("ajio") || combined.contains("nykaa") || combined.contains("zara") ||
            combined.contains("h&m") || combined.contains("retail") || combined.contains("mall") -> "Shopping"

            combined.contains("netflix") || combined.contains("spotify") || combined.contains("hotstar") ||
            combined.contains("bookmyshow") || combined.contains("pvr") || combined.contains("sonyliv") ||
            combined.contains("prime video") || combined.contains("cinema") -> "Entertainment"

            combined.contains("apollo") || combined.contains("pharmeasy") || combined.contains("1mg") ||
            combined.contains("hospital") || combined.contains("clinic") || combined.contains("medplus") ||
            combined.contains("pharmacy") || combined.contains("health") -> "Healthcare"

            combined.contains("airtel") || combined.contains("jio") || combined.contains("vi ") ||
            combined.contains("bescom") || combined.contains("electricity") || combined.contains("billdesk") ||
            combined.contains("recharge") || combined.contains("broadband") -> "Utilities"

            combined.contains("cult.fit") || combined.contains("gym") || combined.contains("decathlon") ||
            combined.contains("fitness") -> "Fitness"

            else -> "General"
        }
    }

    val SAMPLES: List<BankSmsSample> = listOf(
        BankSmsSample(
            bankName = "HDFC Bank",
            label = "HDFC Card: Zara ₹2,890",
            sender = "VM-HDFCBK",
            body = "Alert! You've spent Rs. 2,890.00 on your HDFC Bank Card ending 4092 at ZARA BANGALORE on 20-AUG-26. Avl Bal: Rs 48,210.00. Ref 904812."
        ),
        BankSmsSample(
            bankName = "SBI Bank",
            label = "SBI UPI: Starbucks ₹380",
            sender = "VK-SBIUPI",
            body = "Dear SBI User, your A/C 9842 has been debited by Rs 380.00 on 20-Aug-26 transfer to STARBUCKS COFFEE Ref No 628491024810. Download YONO SBI."
        ),
        BankSmsSample(
            bankName = "ICICI Bank",
            label = "ICICI UPI: Zepto ₹485",
            sender = "BW-ICICIB",
            body = "ICICI Bank Acct XX104 is debited for Rs 485.00 on 20-Aug-26. Info: UPI/ZEPTO QUICK COMM/zepto@hdfcbank. UPI Ref 382910481. Avl Bal: Rs 15,200."
        ),
        BankSmsSample(
            bankName = "Axis Bank",
            label = "Axis Bank: Swiggy ₹620",
            sender = "AX-AXISBK",
            body = "INR 620.00 debited from Axis Bank A/c no. XX8812 on 20-08-2026 13:45:10 towards SWIGGY BANGALORE. UPI Ref: 849201948123."
        ),
        BankSmsSample(
            bankName = "Kotak Bank",
            label = "Kotak: Shell Petrol ₹1,200",
            sender = "JM-KOTAKB",
            body = "Sent Rs. 1,200.00 from Kotak Bank AC *3920 to SHELL FUEL STATION on 20/08/2026. Ref: KTK9041829. Bal: Rs 32,490."
        )
    )
}
