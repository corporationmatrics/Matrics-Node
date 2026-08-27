package com.example.data

import com.example.data.model.ParsedNlpItem
import java.util.Locale

/**
 * CategorySuggester: A high-performance helper utility that automatically suggests and normalizes
 * transaction and item categories based on the 'canonical_name' extracted by Gemini AI or the local NLP parser.
 *
 * Utilizes a comprehensive curated mapping of common household commodities, groceries, utilities,
 * and lifestyle expenses to standard financial categories.
 */
object CategorySuggester {

    data class CategorySuggestion(
        val category: String,
        val subcategory: String = "",
        val confidence: Float = 1.0f,
        val storageType: String = "Pantry",
        val estimatedShelfLifeDays: Int = 30
    )

    // Primary Categories
    const val CAT_DAIRY = "Dairy"
    const val CAT_PRODUCE = "Produce"
    const val CAT_GRAINS = "Grains"
    const val CAT_PANTRY = "Pantry"
    const val CAT_BEVERAGES = "Beverages"
    const val CAT_SNACKS = "Snacks"
    const val CAT_HOUSEHOLD = "Household"
    const val CAT_PERSONAL_CARE = "Personal Care"
    const val CAT_UTILITIES = "Utilities"
    const val CAT_DINING = "Dining"
    const val CAT_TRANSPORT = "Transport"
    const val CAT_ENTERTAINMENT = "Entertainment"
    const val CAT_HEALTH = "Healthcare"
    const val CAT_GROCERIES = "Groceries"
    const val CAT_GENERAL = "General"

    /**
     * Curated canonical household items to financial category mapping.
     * Normalized lowercase keys for instant O(1) matching.
     */
    private val householdItemToCategoryMap: Map<String, CategorySuggestion> = buildMap {
        // --- DAIRY ---
        val dairyItems = listOf(
            "milk" to Pair("Fresh Milk", 7),
            "whole milk" to Pair("Fresh Milk", 7),
            "skimmed milk" to Pair("Fresh Milk", 7),
            "toned milk" to Pair("Fresh Milk", 7),
            "butter" to Pair("Spreads & Fats", 180),
            "salted butter" to Pair("Spreads & Fats", 180),
            "unsalted butter" to Pair("Spreads & Fats", 180),
            "white butter" to Pair("Spreads & Fats", 30),
            "cheese" to Pair("Cheese", 90),
            "cheddar cheese" to Pair("Cheese", 90),
            "mozzarella" to Pair("Cheese", 30),
            "parmesan" to Pair("Cheese", 180),
            "paneer" to Pair("Cottage Cheese", 14),
            "cottage cheese" to Pair("Cottage Cheese", 14),
            "yogurt" to Pair("Yogurt & Curd", 21),
            "greek yogurt" to Pair("Yogurt & Curd", 21),
            "curd" to Pair("Yogurt & Curd", 7),
            "dahi" to Pair("Yogurt & Curd", 7),
            "cream" to Pair("Fresh Cream", 14),
            "fresh cream" to Pair("Fresh Cream", 14),
            "heavy cream" to Pair("Fresh Cream", 14),
            "ghee" to Pair("Clarified Butter", 270),
            "condensed milk" to Pair("Dessert Mixes", 180),
            "buttermilk" to Pair("Probiotics", 5),
            "chaas" to Pair("Probiotics", 5),
            "lassi" to Pair("Sweet Probiotics", 5),
            "sour cream" to Pair("Cultured Dairy", 21),
            "tofu" to Pair("Plant Dairy", 14),
            "soy milk" to Pair("Plant Milk", 30),
            "almond milk" to Pair("Plant Milk", 30),
            "oat milk" to Pair("Plant Milk", 30)
        )
        for ((name, meta) in dairyItems) {
            put(name, CategorySuggestion(CAT_DAIRY, meta.first, 0.98f, "Refrigerated", meta.second))
        }

        // --- PRODUCE & FRESH ---
        val produceItems = listOf(
            "apple" to Pair("Fresh Fruits", 21),
            "apples" to Pair("Fresh Fruits", 21),
            "banana" to Pair("Fresh Fruits", 7),
            "bananas" to Pair("Fresh Fruits", 7),
            "orange" to Pair("Fresh Fruits", 14),
            "oranges" to Pair("Fresh Fruits", 14),
            "tomato" to Pair("Fresh Vegetables", 7),
            "tomatoes" to Pair("Fresh Vegetables", 7),
            "potato" to Pair("Root Vegetables", 30),
            "potatoes" to Pair("Root Vegetables", 30),
            "onion" to Pair("Root Vegetables", 30),
            "onions" to Pair("Root Vegetables", 30),
            "garlic" to Pair("Alliums & Aromatics", 60),
            "ginger" to Pair("Alliums & Aromatics", 30),
            "spinach" to Pair("Leafy Greens", 5),
            "coriander" to Pair("Herbs", 5),
            "mint" to Pair("Herbs", 5),
            "avocado" to Pair("Exotic Fruits", 7),
            "avocados" to Pair("Exotic Fruits", 7),
            "egg" to Pair("Eggs & Poultry", 21),
            "eggs" to Pair("Eggs & Poultry", 21),
            "organic eggs" to Pair("Eggs & Poultry", 21),
            "lemon" to Pair("Citrus", 14),
            "lime" to Pair("Citrus", 14),
            "green chili" to Pair("Chili & Peppers", 10),
            "capsicum" to Pair("Peppers", 10),
            "bell pepper" to Pair("Peppers", 10),
            "cucumber" to Pair("Salads", 7),
            "carrot" to Pair("Root Vegetables", 21),
            "carrots" to Pair("Root Vegetables", 21),
            "broccoli" to Pair("Cruciferous", 7),
            "cauliflower" to Pair("Cruciferous", 7),
            "cabbage" to Pair("Cruciferous", 14),
            "mushroom" to Pair("Fungi", 5),
            "strawberries" to Pair("Berries", 5),
            "blueberries" to Pair("Berries", 7),
            "watermelon" to Pair("Melons", 7),
            "papaya" to Pair("Tropical Fruits", 5),
            "mango" to Pair("Seasonal Fruits", 7),
            "grapes" to Pair("Table Fruits", 7)
        )
        for ((name, meta) in produceItems) {
            val storage = if (name in listOf("potato", "potatoes", "onion", "onions", "garlic", "banana", "bananas")) "Pantry" else "Refrigerated"
            put(name, CategorySuggestion(CAT_PRODUCE, meta.first, 0.98f, storage, meta.second))
        }

        // --- GRAINS & STAPLES ---
        val grainItems = listOf(
            "rice" to Pair("Rice & Grains", 365),
            "basmati rice" to Pair("Aromatic Rice", 365),
            "brown rice" to Pair("Whole Grains", 180),
            "wheat flour" to Pair("Flour & Atta", 90),
            "atta" to Pair("Whole Wheat Flour", 90),
            "maida" to Pair("Refined Flour", 120),
            "besan" to Pair("Gram Flour", 120),
            "oats" to Pair("Breakfast Grains", 180),
            "rolled oats" to Pair("Breakfast Grains", 180),
            "toor dal" to Pair("Pulses & Lentils", 365),
            "moong dal" to Pair("Pulses & Lentils", 365),
            "chana dal" to Pair("Pulses & Lentils", 365),
            "urad dal" to Pair("Pulses & Lentils", 365),
            "masoor dal" to Pair("Pulses & Lentils", 365),
            "rajma" to Pair("Kidney Beans", 365),
            "chickpeas" to Pair("Legumes", 365),
            "pasta" to Pair("Italian Grains", 365),
            "spaghetti" to Pair("Italian Grains", 365),
            "macaroni" to Pair("Italian Grains", 365),
            "noodles" to Pair("Instant Noodles", 180),
            "quinoa" to Pair("Ancient Grains", 365),
            "poha" to Pair("Flattened Rice", 180),
            "rava" to Pair("Semolina", 180),
            "sooji" to Pair("Semolina", 180),
            "bread" to Pair("Bakery Staples", 5),
            "whole wheat bread" to Pair("Bakery Staples", 5),
            "white bread" to Pair("Bakery Staples", 4),
            "sourdough bread" to Pair("Artisanal Bakery", 5)
        )
        for ((name, meta) in grainItems) {
            put(name, CategorySuggestion(CAT_GRAINS, meta.first, 0.98f, "Pantry", meta.second))
        }

        // --- PANTRY & CONDIMENTS ---
        val pantryItems = listOf(
            "cooking oil" to Pair("Edible Oils", 365),
            "sunflower oil" to Pair("Edible Oils", 365),
            "mustard oil" to Pair("Edible Oils", 365),
            "olive oil" to Pair("Specialty Oils", 365),
            "extra virgin olive oil" to Pair("Specialty Oils", 365),
            "coconut oil" to Pair("Specialty Oils", 365),
            "salt" to Pair("Seasonings", 730),
            "table salt" to Pair("Seasonings", 730),
            "rock salt" to Pair("Specialty Salt", 730),
            "pink salt" to Pair("Specialty Salt", 730),
            "sugar" to Pair("Sweeteners", 730),
            "brown sugar" to Pair("Sweeteners", 365),
            "jaggery" to Pair("Natural Sweeteners", 180),
            "honey" to Pair("Natural Sweeteners", 730),
            "turmeric" to Pair("Spices & Masala", 365),
            "chili powder" to Pair("Spices & Masala", 365),
            "coriander powder" to Pair("Spices & Masala", 365),
            "cumin seeds" to Pair("Whole Spices", 365),
            "mustard seeds" to Pair("Whole Spices", 365),
            "garam masala" to Pair("Spice Blends", 180),
            "black pepper" to Pair("Spices", 365),
            "soy sauce" to Pair("Sauces & Condiments", 365),
            "vinegar" to Pair("Condiments", 730),
            "apple cider vinegar" to Pair("Health Tonics", 730),
            "ketchup" to Pair("Table Sauces", 180),
            "tomato sauce" to Pair("Table Sauces", 180),
            "mayonnaise" to Pair("Emulsions", 90),
            "peanut butter" to Pair("Nut Butters", 180),
            "almond butter" to Pair("Nut Butters", 180),
            "jam" to Pair("Fruit Spreads", 180)
        )
        for ((name, meta) in pantryItems) {
            put(name, CategorySuggestion(CAT_PANTRY, meta.first, 0.98f, "Pantry", meta.second))
        }

        // --- BEVERAGES ---
        val beverageItems = listOf(
            "coffee" to Pair("Hot Beverages", 180),
            "coffee beans" to Pair("Artisanal Coffee", 90),
            "ground coffee" to Pair("Artisanal Coffee", 60),
            "instant coffee" to Pair("Coffee", 365),
            "cold brew" to Pair("Ready to Drink", 14),
            "tea" to Pair("Tea & Infusions", 365),
            "green tea" to Pair("Health Teas", 365),
            "black tea" to Pair("Tea", 365),
            "chai" to Pair("Tea Blends", 365),
            "matcha" to Pair("Specialty Tea", 180),
            "orange juice" to Pair("Fruit Juices", 14),
            "apple juice" to Pair("Fruit Juices", 30),
            "coconut water" to Pair("Hydration", 7),
            "sparkling water" to Pair("Carbonated Water", 180),
            "mineral water" to Pair("Drinking Water", 365),
            "energy drink" to Pair("Functional Drinks", 180),
            "kombucha" to Pair("Probiotic Beverages", 30),
            "lemonade" to Pair("Fruit Drinks", 14)
        )
        for ((name, meta) in beverageItems) {
            put(name, CategorySuggestion(CAT_BEVERAGES, meta.first, 0.98f, "Pantry", meta.second))
        }

        // --- SNACKS & CONFECTIONERY ---
        val snackItems = listOf(
            "chips" to Pair("Crisps & Wafers", 90),
            "potato chips" to Pair("Crisps & Wafers", 90),
            "nachos" to Pair("Tortilla Snacks", 120),
            "dark chocolate" to Pair("Confectionery", 180),
            "chocolate" to Pair("Confectionery", 180),
            "cookies" to Pair("Biscuits & Bakery", 120),
            "biscuits" to Pair("Biscuits & Bakery", 120),
            "almonds" to Pair("Dry Fruits & Nuts", 180),
            "cashews" to Pair("Dry Fruits & Nuts", 180),
            "walnuts" to Pair("Dry Fruits & Nuts", 180),
            "pistachios" to Pair("Dry Fruits & Nuts", 180),
            "raisins" to Pair("Dry Fruits", 180),
            "popcorn" to Pair("Kernel Snacks", 120),
            "granola bar" to Pair("Energy Snacks", 180),
            "protein bar" to Pair("Fitness Nutrition", 180)
        )
        for ((name, meta) in snackItems) {
            put(name, CategorySuggestion(CAT_SNACKS, meta.first, 0.98f, "Pantry", meta.second))
        }

        // --- HOUSEHOLD & CLEANING ---
        val householdItems = listOf(
            "laundry detergent" to Pair("Laundry Care", 730),
            "detergent" to Pair("Laundry Care", 730),
            "washing powder" to Pair("Laundry Care", 730),
            "fabric conditioner" to Pair("Laundry Care", 365),
            "dish soap" to Pair("Dishwashing", 730),
            "dishwashing liquid" to Pair("Dishwashing", 730),
            "toilet cleaner" to Pair("Bathroom Hygiene", 730),
            "surface cleaner" to Pair("Home Cleaning", 730),
            "floor cleaner" to Pair("Home Cleaning", 730),
            "disinfectant spray" to Pair("Hygiene", 730),
            "toilet paper" to Pair("Paper & Tissues", 1000),
            "paper towels" to Pair("Paper & Tissues", 1000),
            "facial tissues" to Pair("Paper & Tissues", 1000),
            "garbage bags" to Pair("Waste Management", 1000),
            "trash bags" to Pair("Waste Management", 1000),
            "aluminum foil" to Pair("Food Storage Wrap", 1000),
            "cling wrap" to Pair("Food Storage Wrap", 1000),
            "sponge" to Pair("Cleaning Tools", 90),
            "scrubber" to Pair("Cleaning Tools", 90),
            "broom" to Pair("Cleaning Tools", 365),
            "mop" to Pair("Cleaning Tools", 180),
            "mosquito repellent" to Pair("Pest Control", 365),
            "air freshener" to Pair("Home Fragrance", 180)
        )
        for ((name, meta) in householdItems) {
            put(name, CategorySuggestion(CAT_HOUSEHOLD, meta.first, 0.98f, "Pantry", meta.second))
        }

        // --- PERSONAL CARE & HEALTH ---
        val personalCareItems = listOf(
            "shampoo" to Pair("Hair Care", 730),
            "hair conditioner" to Pair("Hair Care", 730),
            "body wash" to Pair("Bath & Body", 730),
            "bath soap" to Pair("Bath & Body", 730),
            "soap" to Pair("Bath & Body", 730),
            "hand wash" to Pair("Hand Hygiene", 730),
            "toothpaste" to Pair("Oral Care", 730),
            "toothbrush" to Pair("Oral Care", 90),
            "mouthwash" to Pair("Oral Care", 365),
            "deodorant" to Pair("Personal Fragrance", 730),
            "sunscreen" to Pair("Skin Care", 365),
            "moisturizer" to Pair("Skin Care", 365),
            "face wash" to Pair("Skin Care", 365),
            "razor" to Pair("Shaving & Grooming", 180),
            "shaving cream" to Pair("Shaving & Grooming", 365),
            "multivitamins" to Pair("Supplements", 365),
            "vitamins" to Pair("Supplements", 365),
            "pain killer" to Pair("Pharmacy", 730),
            "paracetamol" to Pair("Pharmacy", 730),
            "bandages" to Pair("First Aid", 1000),
            "first aid" to Pair("First Aid", 1000)
        )
        for ((name, meta) in personalCareItems) {
            put(name, CategorySuggestion(CAT_PERSONAL_CARE, meta.first, 0.98f, "Pantry", meta.second))
        }

        // --- UTILITIES & BILLS ---
        val utilityItems = listOf(
            "electricity bill" to Pair("Home Utilities", 30),
            "electricity" to Pair("Home Utilities", 30),
            "power bill" to Pair("Home Utilities", 30),
            "water bill" to Pair("Municipal Utilities", 30),
            "water utility" to Pair("Municipal Utilities", 30),
            "wifi bill" to Pair("Internet & Connectivity", 30),
            "broadband" to Pair("Internet & Connectivity", 30),
            "mobile recharge" to Pair("Telecom Services", 30),
            "phone bill" to Pair("Telecom Services", 30),
            "gas cylinder" to Pair("Cooking Gas & Fuel", 60),
            "piped gas" to Pair("Cooking Gas", 30),
            "lpg" to Pair("Cooking Gas", 60),
            "petrol" to Pair("Automotive Fuel", 7),
            "diesel" to Pair("Automotive Fuel", 7),
            "ev charging" to Pair("Automotive Charging", 7)
        )
        for ((name, meta) in utilityItems) {
            put(name, CategorySuggestion(CAT_UTILITIES, meta.first, 0.99f, "N/A", meta.second))
        }

        // --- DINING & RESTAURANTS ---
        val diningItems = listOf(
            "pizza" to Pair("Casual Dining", 2),
            "burger" to Pair("Fast Food", 1),
            "sandwich" to Pair("Delicatessen", 1),
            "pasta" to Pair("Italian Dining", 2),
            "sushi" to Pair("Japanese Dining", 1),
            "croissant" to Pair("Bakery Cafe", 2),
            "salad bowl" to Pair("Healthy Dining", 1),
            "burrito" to Pair("Mexican Dining", 1),
            "thali" to Pair("Indian Dining", 1),
            "biryani" to Pair("Rice Dishes", 1),
            "lunch combo" to Pair("Restaurant Meals", 1),
            "dinner" to Pair("Restaurant Meals", 1),
            "coffee shop" to Pair("Cafe & Pastries", 1)
        )
        for ((name, meta) in diningItems) {
            put(name, CategorySuggestion(CAT_DINING, meta.first, 0.95f, "Refrigerated", meta.second))
        }

        // --- TRANSPORTATION ---
        val transportItems = listOf(
            "uber" to Pair("Ride Hailing", 1),
            "ola" to Pair("Ride Hailing", 1),
            "cab fare" to Pair("Taxi Services", 1),
            "auto fare" to Pair("Transit", 1),
            "metro pass" to Pair("Public Transit", 30),
            "bus ticket" to Pair("Public Transit", 1),
            "toll fee" to Pair("Highways & Tolls", 1),
            "parking fee" to Pair("Vehicle Parking", 1),
            "train ticket" to Pair("Rail Travel", 1),
            "flight ticket" to Pair("Airlines", 1)
        )
        for ((name, meta) in transportItems) {
            put(name, CategorySuggestion(CAT_TRANSPORT, meta.first, 0.99f, "N/A", meta.second))
        }
    }

    /**
     * Normalized keyword search entries for fuzzy token matching.
     */
    private val categoryPatternMatchers: List<Pair<Regex, CategorySuggestion>> = listOf(
        Regex("""(?i)\b(milk|butter|cheese|paneer|yogurt|curd|cream|dahi|ghee|tofu|lassi|chaas)\b""") to
                CategorySuggestion(CAT_DAIRY, "Dairy Products", 0.92f, "Refrigerated", 14),

        Regex("""(?i)\b(apple|banana|orange|tomato|potato|onion|spinach|coriander|avocado|egg|lemon|carrot|broccoli|mushroom|strawberry|berry|fruit|veggie|vegetable)\b""") to
                CategorySuggestion(CAT_PRODUCE, "Fresh Produce", 0.92f, "Refrigerated", 10),

        Regex("""(?i)\b(rice|wheat|flour|atta|oats|dal|lentil|pasta|noodle|quinoa|poha|sooji|rava|bread|toast|bagel)\b""") to
                CategorySuggestion(CAT_GRAINS, "Grains & Bakery", 0.92f, "Pantry", 180),

        Regex("""(?i)\b(oil|olive|salt|sugar|honey|jaggery|masala|turmeric|chili|cumin|pepper|sauce|ketchup|vinegar|spice)\b""") to
                CategorySuggestion(CAT_PANTRY, "Cooking Essentials", 0.92f, "Pantry", 365),

        Regex("""(?i)\b(coffee|cold brew|tea|matcha|espresso|juice|kombucha|soda|coke|pepsi|water|drink|beverage)\b""") to
                CategorySuggestion(CAT_BEVERAGES, "Beverages", 0.92f, "Pantry", 60),

        Regex("""(?i)\b(chip|chips|chocolate|cookie|biscuit|almond|cashew|walnut|nut|popcorn|wafer|snack|candy)\b""") to
                CategorySuggestion(CAT_SNACKS, "Snacks & Sweets", 0.92f, "Pantry", 120),

        Regex("""(?i)\b(detergent|soap|dishwash|cleaner|toilet paper|tissue|garbage bag|sponge|mop|broom|harpic|surf)\b""") to
                CategorySuggestion(CAT_HOUSEHOLD, "Household & Cleaning", 0.92f, "Pantry", 365),

        Regex("""(?i)\b(shampoo|toothpaste|body wash|deodorant|sunscreen|moisturizer|razor|vitamin|tablet|paracetamol|medicine)\b""") to
                CategorySuggestion(CAT_PERSONAL_CARE, "Personal Care & Health", 0.92f, "Pantry", 365),

        Regex("""(?i)\b(electricity|power bill|water bill|wifi|broadband|recharge|cylinder|lpg|gas bill|petrol|fuel|diesel)\b""") to
                CategorySuggestion(CAT_UTILITIES, "Bills & Utilities", 0.95f, "N/A", 30),

        Regex("""(?i)\b(pizza|burger|sandwich|sushi|pasta|croissant|salad|biryani|thali|lunch|dinner|meal|cafe|restaurant|takeout|zomato|swiggy)\b""") to
                CategorySuggestion(CAT_DINING, "Dining & Food", 0.92f, "Refrigerated", 2),

        Regex("""(?i)\b(uber|ola|cab|auto|metro|train|bus|flight|toll|parking|fuel|travel|fare)\b""") to
                CategorySuggestion(CAT_TRANSPORT, "Transport & Travel", 0.95f, "N/A", 1)
    )

    /**
     * Primary API: Suggests a category for a given commodity item based on its canonical name extracted by Gemini.
     *
     * @param canonicalName The normalized product name extracted by Gemini (e.g. "Butter", "Whole Milk", "Basmati Rice")
     * @param fallbackCategory Optional default category if no exact or fuzzy match is found
     * @return Suggested standard category string (e.g. "Dairy", "Produce", "Grains", "Pantry", etc.)
     */
    fun suggestCategory(canonicalName: String, fallbackCategory: String = CAT_GENERAL): String {
        return suggestCategoryWithDetails(canonicalName, fallbackCategory).category
    }

    /**
     * Detailed suggestion lookup providing category, subcategory, confidence, storage recommendation, and shelf-life.
     */
    fun suggestCategoryWithDetails(canonicalName: String, fallbackCategory: String = CAT_GENERAL): CategorySuggestion {
        if (canonicalName.isBlank()) {
            return CategorySuggestion(fallbackCategory, "General", 0.5f, "Pantry", 30)
        }

        val normalized = normalizeKey(canonicalName)

        // Tier 1: Exact Key Match in Household Dictionary
        householdItemToCategoryMap[normalized]?.let { return it }

        // Tier 2: Partial sub-key match in Dictionary
        for ((key, suggestion) in householdItemToCategoryMap) {
            if (normalized.contains(key) || key.contains(normalized)) {
                return suggestion.copy(confidence = 0.90f)
            }
        }

        // Tier 3: Regex Pattern Matching across Category Domains
        for ((pattern, suggestion) in categoryPatternMatchers) {
            if (pattern.containsMatchIn(canonicalName)) {
                return suggestion
            }
        }

        // Tier 4: Fallback with smart storage inference
        return CategorySuggestion(
            category = if (fallbackCategory.isNotBlank() && fallbackCategory != CAT_GENERAL) fallbackCategory else inferFallbackCategoryFromName(canonicalName),
            subcategory = "General",
            confidence = 0.60f,
            storageType = "Pantry",
            estimatedShelfLifeDays = 30
        )
    }

    /**
     * Determines the overall primary transaction category from a list of parsed items extracted by Gemini.
     * Evaluates item counts and spend weighting to return the most accurate primary category for the bill.
     */
    fun suggestTransactionCategory(items: List<ParsedNlpItem>, fallbackCategory: String = CAT_GROCERIES): String {
        if (items.isEmpty()) return fallbackCategory

        // If all items have the same category, use that directly
        val categoryFrequencies = mutableMapOf<String, Double>()
        for (item in items) {
            val cat = if (item.category.isNotBlank() && item.category != CAT_GENERAL) {
                item.category
            } else {
                suggestCategory(item.canonicalName.ifBlank { item.name })
            }
            val weight = (item.price * item.quantity).coerceAtLeast(1.0)
            categoryFrequencies[cat] = (categoryFrequencies[cat] ?: 0.0) + weight
        }

        val dominant = categoryFrequencies.maxByOrNull { it.value }?.key ?: fallbackCategory
        
        // If dominant category represents groceries subsets (Dairy, Produce, Grains, Pantry), and there are multiple categories,
        // group under Groceries for the whole transaction.
        val grocerySubsets = setOf(CAT_DAIRY, CAT_PRODUCE, CAT_GRAINS, CAT_PANTRY, CAT_SNACKS, CAT_BEVERAGES)
        val grocerySpend = categoryFrequencies.filter { it.key in grocerySubsets }.values.sum()
        val totalSpend = categoryFrequencies.values.sum()

        return if (grocerySpend / totalSpend >= 0.60 && categoryFrequencies.keys.count { it in grocerySubsets } > 1) {
            CAT_GROCERIES
        } else {
            dominant
        }
    }

    /**
     * Enriches a single [ParsedNlpItem] by checking its canonicalName against the household mapping,
     * ensuring consistent categorization, storage type, and shelf life.
     */
    fun enrichItem(item: ParsedNlpItem): ParsedNlpItem {
        val targetName = item.canonicalName.ifBlank { item.name }
        val suggestion = suggestCategoryWithDetails(targetName, item.category)

        val resolvedCategory = if (item.category.isBlank() || item.category == CAT_GENERAL || item.category == "Grocery") {
            suggestion.category
        } else {
            item.category
        }

        val resolvedSubcategory = if (item.subcategory.isBlank()) suggestion.subcategory else item.subcategory
        val resolvedStorage = if (item.storageType == "Pantry" && suggestion.storageType != "Pantry") suggestion.storageType else item.storageType
        val resolvedShelfLife = if (item.shelfLifeDays <= 0 || item.shelfLifeDays == 30) suggestion.estimatedShelfLifeDays else item.shelfLifeDays

        return item.copy(
            category = resolvedCategory,
            subcategory = resolvedSubcategory,
            storageType = resolvedStorage,
            shelfLifeDays = resolvedShelfLife
        )
    }

    /**
     * Enriches a collection of [ParsedNlpItem] items extracted by Gemini.
     */
    fun enrichItems(items: List<ParsedNlpItem>): List<ParsedNlpItem> {
        return items.map { enrichItem(it) }
    }

    /**
     * Returns a copy of the canonical household item dictionary for documentation or search auto-completion.
     */
    fun getHouseholdItemMapping(): Map<String, String> {
        return householdItemToCategoryMap.mapValues { it.value.category }
    }

    private fun normalizeKey(text: String): String {
        return text.lowercase(Locale.ROOT)
            .replace(Regex("""\d+(?:\.\d+)?\s*(g|kg|ml|l|pcs|pack|pk|oz|gm)"""), "")
            .replace(Regex("""[^a-z0-9\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun inferFallbackCategoryFromName(name: String): String {
        val lower = name.lowercase(Locale.ROOT)
        return when {
            lower.contains("bill") || lower.contains("recharge") || lower.contains("utility") -> CAT_UTILITIES
            lower.contains("fare") || lower.contains("ticket") || lower.contains("ride") -> CAT_TRANSPORT
            lower.contains("hotel") || lower.contains("meal") || lower.contains("dinner") || lower.contains("cafe") -> CAT_DINING
            else -> CAT_GENERAL
        }
    }
}
