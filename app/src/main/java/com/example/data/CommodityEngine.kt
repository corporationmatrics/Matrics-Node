package com.example.data

import com.example.data.dao.ExpenseDao
import com.example.data.model.CommodityEntity
import com.example.data.model.ParsedNlpItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class CommodityEngine(private val expenseDao: ExpenseDao) {

    /**
     * Resolves raw voice or text transactions through the 3-Tier Commodity Architecture:
     * Tier 1: Local Device Cache (0 ms)
     * Tier 2: Pre-Seeded Master DB (10 ms)
     * Tier 3: Gemini 3.5 Flash Structured JSON Mode (Zero-shot normalization & enrichment)
     */
    suspend fun resolveInput(
        rawText: String,
        apiKey: String
    ): Pair<String, List<ParsedNlpItem>> = withContext(Dispatchers.IO) {
        if (rawText.isBlank()) {
            return@withContext Pair("Local Store", emptyList())
        }

        // Try Tier 3 Gemini API if API key is provided
        if (apiKey.isNotBlank()) {
            val geminiResult = GeminiService.parseVoiceTransactionStructured(rawText, apiKey)
            if (geminiResult.isSuccess) {
                val (vendor, items) = geminiResult.getOrThrow()
                // Self-Learning Cache Loop: Cache every newly parsed item into Tier 1 Local DB
                for (item in items) {
                    val key = normalizeKey(item.name)
                    expenseDao.insertCommodity(
                        CommodityEntity(
                            rawKey = key,
                            canonicalName = item.canonicalName,
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
                return@withContext Pair(vendor, items)
            }
        }

        // Local 2-Tier Pipeline (Tier 1 Cache + Tier 2 Pre-Seeded DB)
        val (vendor, parsedRawItems) = NlpParsingEngine.parseInput(rawText)
        val enrichedItems = mutableListOf<ParsedNlpItem>()

        for (item in parsedRawItems) {
            val key = normalizeKey(item.name)

            // Tier 1: Exact Key Match in Local SQLite Cache
            val cachedCommodity = expenseDao.getCommodityByKey(key)
            if (cachedCommodity != null) {
                expenseDao.incrementCommodityUsage(key, item.price)
                enrichedItems.add(
                    item.copy(
                        canonicalName = cachedCommodity.canonicalName,
                        brand = cachedCommodity.brand,
                        category = cachedCommodity.category,
                        subcategory = cachedCommodity.subcategory,
                        storageType = cachedCommodity.storageType,
                        shelfLifeDays = cachedCommodity.estimatedShelfLifeDays,
                        unit = if (item.unit == "unit") cachedCommodity.normalizedUnit else item.unit,
                        tierResolved = if (cachedCommodity.isPreSeeded) "TIER_2_SEEDED" else "TIER_1_CACHE"
                    )
                )
                continue
            }

            // Tier 2: Fuzzy Match in Pre-Seeded Master DB
            val fuzzyMatches = expenseDao.searchCommoditiesFuzzy(key)
            val bestMatch = fuzzyMatches.firstOrNull()
            if (bestMatch != null) {
                enrichedItems.add(
                    item.copy(
                        canonicalName = bestMatch.canonicalName,
                        brand = bestMatch.brand,
                        category = bestMatch.category,
                        subcategory = bestMatch.subcategory,
                        storageType = bestMatch.storageType,
                        shelfLifeDays = bestMatch.estimatedShelfLifeDays,
                        unit = if (item.unit == "unit") bestMatch.normalizedUnit else item.unit,
                        tierResolved = "TIER_2_SEEDED"
                    )
                )
            } else {
                // Tier Fallback: Household Category Suggester
                val targetName = item.canonicalName.ifBlank { item.name }
                val suggestion = CategorySuggester.suggestCategoryWithDetails(targetName)

                enrichedItems.add(
                    item.copy(
                        category = suggestion.category,
                        subcategory = suggestion.subcategory,
                        storageType = suggestion.storageType,
                        shelfLifeDays = suggestion.estimatedShelfLifeDays,
                        tierResolved = "TIER_FALLBACK"
                    )
                )
            }
        }

        Pair(vendor, enrichedItems)
    }

    private fun normalizeKey(name: String): String {
        return name.lowercase(Locale.ROOT)
            .replace(Regex("""\d+(?:\.\d+)?\s*(g|kg|ml|l|pcs|pack|pk|oz|gm)"""), "")
            .replace(Regex("""[^a-z0-9\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
