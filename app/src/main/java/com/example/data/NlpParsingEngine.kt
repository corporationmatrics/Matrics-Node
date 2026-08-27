package com.example.data

import com.example.data.model.ParsedNlpItem
import java.util.Locale

object NlpParsingEngine {

    private val categoryKeywords = mapOf(
        "Dairy" to listOf("milk", "butter", "cheese", "paneer", "yogurt", "curd", "cream", "amul", "ghee", "paal", "paalu", "haalu", "doi", "dahi", "loni", "toop", "venna", "vennai", "benne", "mosaru", "perugu", "tayir", "chaas", "lassi", "majjige"),
        "Grains" to listOf("rice", "wheat", "flour", "atta", "oats", "dal", "lentil", "pasta", "quinoa", "bread", "chawal", "arisi", "biyyam", "akki", "chal", "tandool", "ponni", "sonamasoori", "kolam", "miniket", "paruppu", "pappu", "bele", "daal", "poha", "rava", "sooji", "maida"),
        "Produce" to listOf("apple", "banana", "tomato", "potato", "onion", "avocado", "spinach", "egg", "eggs", "vegetable", "fruit", "berries", "aloo", "pyaz", "tamatar", "ande", "kaikari", "kuralu", "tarakari", "torkari", "bhaji", "thakkali", "vengayam", "urulaikizhangu", "muttai", "ullipayalu", "bangaladumpa", "gudlu", "eerulli", "alugadde", "motte", "peyaj", "alu", "deem", "kanda", "batata"),
        "Beverages" to listOf("coffee", "cold brew", "tea", "espresso", "latte", "juice", "soda", "water", "drink", "energy", "chai", "chaha", "kappi", "sarbat", "tiffin"),
        "Dining" to listOf("croissant", "pizza", "burger", "sandwich", "pasta", "sushi", "taco", "meal", "dinner", "lunch", "breakfast", "idli", "dosa", "dosai", "vadai", "vada", "samosa", "rasgulla", "mishti", "sweets", "luchi", "biryani", "thali"),
        "Pantry" to listOf("oil", "olive oil", "sugar", "salt", "spice", "sauce", "ketchup", "vinegar", "honey", "pepper", "cheeni", "saakhar", "sarkarai", "chekkara", "sakkare", "chini", "tel", "ennai", "noone", "yenne", "sarson", "mustard oil", "namak", "uppu", "nun", "meeth", "haldi", "mirchi", "jeera", "masala"),
        "Snacks" to listOf("chips", "chocolate", "cookie", "biscuit", "nuts", "popcorn", "candy", "wafer", "mixture", "murukku", "namkeen", "khakhra"),
        "Utilities" to listOf("bill", "electricity", "wifi", "internet", "recharge", "water bill", "gas", "fuel", "petrol", "bijli", "current bill", "diesel", "cylinder"),
        "Household" to listOf("soap", "detergent", "shampoo", "tissue", "cleaner", "paper", "sponge", "surf", "vim", "harpic")
    )

    // Vernacular Indian unit synonyms mapped to standard unit metrics
    private val vernacularUnitMap = mapOf(
        "kilo" to "kg", "kilos" to "kg", "kg" to "kg", "kgs" to "kg",
        "gram" to "g", "grams" to "g", "gm" to "g", "gms" to "g", "g" to "g",
        "litre" to "L", "litres" to "L", "liter" to "L", "liters" to "L", "l" to "L", "ltr" to "L",
        "milli" to "ml", "ml" to "ml",
        "packet" to "pack", "packets" to "pack", "pkt" to "pack", "pk" to "pack", "pack" to "pack", "packs" to "pack",
        "dabba" to "box", "packetu" to "pack", "dabbaalu" to "box", "potti" to "box",
        "bottle" to "bottle", "bottles" to "bottle",
        "piece" to "pcs", "pieces" to "pcs", "pcs" to "pcs", "unit" to "unit", "units" to "unit", "nos" to "pcs",
        "dozen" to "dozen", "dz" to "dozen",
        "pav" to "250g", "adha" to "0.5L", "adha kilo" to "0.5kg"
    )

    fun inferCategory(itemName: String): String {
        val suggested = CategorySuggester.suggestCategory(itemName, "General")
        if (suggested != "General") return suggested

        val lower = itemName.lowercase(Locale.ROOT)
        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { lower.contains(it) }) {
                return category
            }
        }
        return "General"
    }

    /**
     * Parses continuous multi-item voice/text strings in English, Hinglish, Tanglish,
     * Tenglish, Kanglish, Banglish, and Marathi.
     */
    fun parseInput(
        rawInput: String,
        defaultVendor: String = "Local Store",
        language: RegionalLanguage = RegionalLanguage.HINGLISH
    ): Pair<String, List<ParsedNlpItem>> {
        if (rawInput.isBlank()) {
            return Pair(defaultVendor, emptyList())
        }

        var text = rawInput.trim()
        var extractedVendor = defaultVendor

        // Detect Vernacular & English merchant / store patterns:
        // "from [Vendor]", "se [Vendor]", "la [Vendor]", "lo [Vendor]", "nalli [Vendor]", "theke [Vendor]", "madhun [Vendor]"
        val vendorPatterns = listOf(
            Regex("""(?i)\b(?:from|at store|at merchant|vendor)\s+([A-Za-z0-9\s&'-]+)$"""),
            Regex("""(?i)^([A-Za-z0-9\s&'-]+)\s+(?:se|la|lo|nalli|theke|madhun|dokan theke|parlor nalli)\b"""),
            Regex("""(?i)\b(?:se|la|lo|nalli|theke|madhun)\s+([A-Za-z0-9\s&'-]+)$""")
        )

        for (vp in vendorPatterns) {
            val match = vp.find(text)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                if (candidate.isNotEmpty() && !candidate.contains("at ") && candidate.length <= 30) {
                    extractedVendor = candidate
                    text = text.removeRange(match.range).trim()
                    break
                }
            }
        }

        // Split by multi-lingual conjunctions:
        // English: "and", "plus", ",", ";"
        // Hindi/Hinglish: "aur", "tatha"
        // Tamil: "mathum", "apram"
        // Telugu: "mariyu", "inka"
        // Kannada: "mathu", "matte"
        // Bengali: "aar", "ebong"
        // Marathi: "aani", "va"
        val splitRegex = Regex(""",|;|\+|\band\b|\baur\b|\bmathum\b|\bmariyu\b|\bmathu\b|\baar\b|\baani\b|\bapram\b|\binka\b|\bmatte\b""", RegexOption.IGNORE_CASE)
        val itemSegments = text.split(splitRegex)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val parsedItems = mutableListOf<ParsedNlpItem>()

        for (segment in itemSegments) {
            val parsed = parseSingleSegment(segment, extractedVendor, language)
            if (parsed != null) {
                parsedItems.add(parsed)
            }
        }

        // Fallback if segment parsing was empty but text had content
        if (parsedItems.isEmpty() && text.isNotEmpty()) {
            val priceRegex = Regex("""(?:₹|\$|Rs\.?|INR|taka|roobai|rupayalu|roopayi|rupaye)?\s*(\d+(?:\.\d{1,2})?)""")
            val priceMatch = priceRegex.find(text)
            val price = priceMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 100.0
            val cleanName = text.replace(priceRegex, "")
                .replace(Regex("""(?i)\b(at|for|cost|price|mein|ku|lo|nalli|te|ne|cash|upi|gpay|phonepe)\b"""), "")
                .trim()
            val finalName = if (cleanName.isNotBlank()) cleanName else "Quick Expense"
            parsedItems.add(
                ParsedNlpItem(
                    name = finalName,
                    category = inferCategory(finalName),
                    quantity = 1.0,
                    unit = "unit",
                    price = price,
                    vendor = extractedVendor
                )
            )
        }

        return Pair(extractedVendor, parsedItems)
    }

    private fun parseSingleSegment(segment: String, vendor: String, language: RegionalLanguage): ParsedNlpItem? {
        var clean = segment.trim()
        if (clean.isBlank()) return null

        // Extract Price:
        // Multi-dialect currency identifiers:
        // Hindi: "mein", "ka", "rupay", "rupiya"
        // Tamil: "roobai", "ku", "vilai"
        // Telugu: "rupayalu", "ki", "dharaku"
        // Kannada: "roopayi", "ge", "bele"
        // Bengali: "taka", "te", "dam"
        // Marathi: "rupaye", "la", "kimmat"
        var price = 0.0
        val pricePatterns = listOf(
            Regex("""(?i)(?:spent|paid|cost|price|at|for|max|cap|mein|ku|ki|ge|te|la|₹|\$|rs\.?)\s*(\d+(?:\.\d{1,2})?)\s*(?:dollars?|bucks?|rupees?|rs|usd|eur|gbp|taka|roobai|rupayalu|roopayi|rupaye|rupay|rooba)?\b"""),
            Regex("""(\d+(?:\.\d{1,2})?)\s*(?:₹|\$|rs|rupees?|bucks?|dollars?|taka|roobai|rupayalu|roopayi|rupaye|rupay|rooba)\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(\d+(?:\.\d{1,2})?)\s*(?:mein|ku|ki|ge|te|ne|dilam|kotte|ichanu|vangunen|ghetla|liya|diyo)?\s*$"""),
            Regex("""\b(\d+(?:\.\d{1,2})?)\s*$""")
        )

        for (p in pricePatterns) {
            val match = p.find(clean)
            if (match != null) {
                val pVal = match.groupValues[1].toDoubleOrNull()
                if (pVal != null && pVal > 0) {
                    price = pVal
                    clean = clean.replace(match.value, "").trim()
                    break
                }
            }
        }

        // Extract Quantity & Vernacular Units (e.g. "2 kilo", "500g", "1 litre", "6 ande", "1 dabba", "2 packet")
        var quantity = 1.0
        var unit = "unit"

        val qtyPatterns = listOf(
            Regex("""(?i)\b(\d+(?:\.\d{1,2})?)\s*(g|grams|gm|gms|kg|kgs|kilo|kilos|l|liters|litre|litres|ltr|ml|packets|packet|packs|pack|pk|pkt|dabba|dabbaalu|bottles|bottle|units|unit|box|cans|lbs|oz|pcs|pieces|piece|dozen|nos)\b"""),
            Regex("""(?i)^(\d+)\s*(?:x|\*)\s+"""),
            Regex("""(?i)^(\d+)\s+([A-Za-z]+)""")
        )

        val unitMatch = qtyPatterns[0].find(clean)
        if (unitMatch != null) {
            quantity = unitMatch.groupValues[1].toDoubleOrNull() ?: 1.0
            val rawUnit = unitMatch.groupValues[2].lowercase(Locale.ROOT)
            unit = vernacularUnitMap[rawUnit] ?: rawUnit
            clean = clean.replace(unitMatch.value, "").trim()
        } else {
            val countMatch = qtyPatterns[1].find(clean)
            if (countMatch != null) {
                quantity = countMatch.groupValues[1].toDoubleOrNull() ?: 1.0
                clean = clean.substring(countMatch.range.last + 1).trim()
            }
        }

        // Clean up multi-lingual action verbs, payment markers, and dialect fillers
        val dialectFilter = Regex(
            """(?i)\b(spent|bought|paid|purchased|ordered|got|sold|sell|sale|restock|restocked|received|inward|on|at|for|from|in|of|with|the|a|an|dollars?|bucks?|rupees?|usd|""" +
            """liya|diya|de|becha|aaya|maal|udhaar|khata|mein|ka|ki|se|order|kiya|karo|""" +
            """vangunen|vangi|kuduthen|vithom|pannen|ku|la|roobai|kaasu|panam|""" +
            """ichanu|chesa|konnanu|ammamu|lo|ki|rupayalu|""" +
            """kotte|madide|thogonde|maratavada|nalli|ge|roopayi|""" +
            """nilam|dilam|korlam|bikri|theke|te|taka|""" +
            """ghetla|dile|vikla|kela|madhun|la|rupaye|""" +
            """upi|gpay|phonepe|paytm|cash|card|credit card|debit card|net banking)\b"""
        )

        clean = clean.replace(dialectFilter, " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (clean.isBlank()) {
            clean = "Item"
        }

        // Capitalize words nicely
        val formattedName = clean.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

        return ParsedNlpItem(
            name = formattedName,
            category = inferCategory(formattedName),
            quantity = quantity,
            unit = unit,
            price = if (price > 0) price else 50.0,
            vendor = vendor
        )
    }

    data class TokenChip(
        val text: String,
        val type: TokenType
    )

    enum class TokenType {
        ITEM_NAME,
        PRICE,
        QUANTITY,
        VENDOR,
        KEYWORD,
        VERNACULAR_ACTION
    }

    /**
     * Splits stream into live colored token chips for HUD display with vernacular dialect recognition
     */
    fun tokenizeForHud(rawText: String): List<TokenChip> {
        val tokens = mutableListOf<TokenChip>()
        val words = rawText.split(Regex("""\s+"""))

        var i = 0
        while (i < words.size) {
            val word = words[i]
            val lower = word.lowercase(Locale.ROOT)

            when {
                lower in listOf("at", "for", "from", "and", "plus", "in", "with", "max", "aur", "se", "la", "lo", "nalli", "theke", "madhun", "mein", "ku", "ki", "ge", "te", "mathu", "aar", "aani") -> {
                    tokens.add(TokenChip(word, TokenType.KEYWORD))
                }
                word.matches(Regex("""^(?:₹|\$|Rs\.?)?\d+(?:\.\d{1,2})?(?:taka|roobai|rupayalu|roopayi|rupaye|rupay)?$""")) ||
                lower in listOf("rupees", "rupay", "taka", "roobai", "rupayalu", "roopayi", "rupaye") -> {
                    tokens.add(TokenChip(word, TokenType.PRICE))
                }
                word.matches(Regex("""(?i)^\d+(g|kg|ml|l|pack|pk|oz|kilo|litre|dabba)$""")) ||
                lower in listOf("kg", "kilo", "g", "grams", "litre", "litres", "l", "ml", "packet", "pack", "dabba", "pav") -> {
                    tokens.add(TokenChip(word, TokenType.QUANTITY))
                }
                lower in listOf("freshmart", "blinkit", "zepto", "instamart", "amazon", "metro", "roast", "cafe", "store", "supermarket", "dmart", "nilgiris", "ratnadeep", "nandini", "more", "saravana", "kirana") -> {
                    tokens.add(TokenChip(word, TokenType.VENDOR))
                }
                lower in listOf("upi", "gpay", "phonepe", "paytm", "cash", "card", "vangunen", "ichanu", "kotte", "nilam", "ghetla", "liya", "diya") -> {
                    tokens.add(TokenChip(word, TokenType.VERNACULAR_ACTION))
                }
                else -> {
                    tokens.add(TokenChip(word, TokenType.ITEM_NAME))
                }
            }
            i++
        }
        return tokens
    }
}
