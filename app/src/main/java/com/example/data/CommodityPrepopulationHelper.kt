package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.dao.ExpenseDao
import com.example.data.model.CommodityEntity
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Room DAO helper to pre-populate the Room SQLite database with lightweight
 * structured household commodities JSON asset, establishing Tier 2 offline matching.
 */
object CommodityPrepopulationHelper {

    private const val TAG = "CommodityPrepopulate"
    const val DEFAULT_ASSET_FILE = "household_commodities.json"

    /**
     * Reads the specified JSON asset file, parses the commodity records,
     * and inserts them into Room via [dao].
     *
     * @return The count of commodities successfully parsed and seeded.
     */
    suspend fun prePopulateFromAsset(
        context: Context,
        dao: ExpenseDao,
        assetFileName: String = DEFAULT_ASSET_FILE
    ): Int {
        return try {
            val jsonString = loadJsonFromAsset(context, assetFileName)
            if (jsonString.isNullOrBlank()) {
                Log.w(TAG, "Asset file '$assetFileName' was empty or could not be loaded.")
                return 0
            }

            val commodities = parseCommoditiesFromJson(jsonString)
            if (commodities.isNotEmpty()) {
                dao.insertCommodities(commodities)
                Log.d(TAG, "Successfully seeded ${commodities.size} master commodities from $assetFileName")
            }
            commodities.size
        } catch (e: Exception) {
            Log.e(TAG, "Error pre-populating commodities from asset: $assetFileName", e)
            0
        }
    }

    /**
     * Checks if the commodity catalog in the database is empty.
     * If empty, automatically seeds it from the JSON asset.
     */
    suspend fun checkAndSeedIfEmpty(
        context: Context,
        dao: ExpenseDao,
        assetFileName: String = DEFAULT_ASSET_FILE
    ): Int {
        val count = dao.getCommoditiesCount()
        return if (count == 0) {
            Log.d(TAG, "Commodity catalog is empty. Initializing Tier 2 pre-population...")
            prePopulateFromAsset(context, dao, assetFileName)
        } else {
            Log.d(TAG, "Commodity catalog already contains $count entries. Skipping initial seeding.")
            count
        }
    }

    /**
     * Reads a file from the Android assets folder as a string.
     */
    fun loadJsonFromAsset(context: Context, assetFileName: String): String? {
        return try {
            context.assets.open(assetFileName).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open asset file: $assetFileName", e)
            null
        }
    }

    /**
     * Parses a JSON Array string containing commodity specifications into a list of [CommodityEntity].
     */
    fun parseCommoditiesFromJson(jsonString: String): List<CommodityEntity> {
        val list = mutableListOf<CommodityEntity>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val rawKey = obj.optString("rawKey", "").trim().lowercase()
                if (rawKey.isEmpty()) continue

                val canonicalName = obj.optString("canonicalName", rawKey.replaceFirstChar { it.uppercase() })
                val brand = obj.optString("brand", "Standard")
                val category = obj.optString("category", "General")
                val subcategory = obj.optString("subcategory", "")
                val defaultQuantity = obj.optDouble("defaultQuantity", 1.0)
                val normalizedUnit = obj.optString("normalizedUnit", "unit")
                val estimatedShelfLifeDays = obj.optInt("estimatedShelfLifeDays", 30)
                val storageType = obj.optString("storageType", "Pantry")
                val lastKnownPrice = obj.optDouble("lastKnownPrice", 0.0)
                val useCount = obj.optInt("useCount", 1)
                val sellingPrice = obj.optDouble("sellingPrice", if (lastKnownPrice > 0) lastKnownPrice else 50.0)
                val costPrice = obj.optDouble("costPrice", (sellingPrice * 0.78).coerceAtLeast(0.0))
                val stockQuantity = obj.optDouble("stockQuantity", (20.0 + (i % 7) * 5.0))
                val reorderThreshold = obj.optDouble("reorderThreshold", 8.0)
                val sku = obj.optString("sku", "SKU-${1000 + i}")

                list.add(
                    CommodityEntity(
                        id = 0,
                        rawKey = rawKey,
                        canonicalName = canonicalName,
                        brand = brand,
                        category = category,
                        subcategory = subcategory,
                        defaultQuantity = defaultQuantity,
                        normalizedUnit = normalizedUnit,
                        estimatedShelfLifeDays = estimatedShelfLifeDays,
                        storageType = storageType,
                        lastKnownPrice = sellingPrice,
                        useCount = useCount,
                        isPreSeeded = true,
                        stockQuantity = stockQuantity,
                        costPrice = costPrice,
                        sellingPrice = sellingPrice,
                        reorderThreshold = reorderThreshold,
                        sku = sku
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing commodity JSON array", e)
        }
        return list
    }
}
