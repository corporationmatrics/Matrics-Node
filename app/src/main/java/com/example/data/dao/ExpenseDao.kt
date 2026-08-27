package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.CommodityEntity
import com.example.data.model.GroceryItemEntity
import com.example.data.model.LineItemEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM transactions ORDER BY dateTimestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM line_items ORDER BY dateTimestamp DESC")
    fun getAllLineItems(): Flow<List<LineItemEntity>>

    @Query("SELECT * FROM line_items WHERE transactionId = :transactionId")
    fun getLineItemsForTransaction(transactionId: Long): Flow<List<LineItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItems(items: List<LineItemEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("DELETE FROM line_items WHERE transactionId = :transactionId")
    suspend fun deleteLineItemsByTxId(transactionId: Long)

    @Query("SELECT * FROM line_items WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%' ORDER BY dateTimestamp ASC")
    fun searchLineItems(query: String): Flow<List<LineItemEntity>>

    @Query("SELECT DISTINCT name FROM line_items ORDER BY name ASC")
    fun getDistinctItemNames(): Flow<List<String>>

    // Commodity Master & Cache (Tier 1 & Tier 2)
    @Query("SELECT * FROM commodity_catalog WHERE id = :id LIMIT 1")
    suspend fun getCommodityById(id: Long): CommodityEntity?

    @Query("SELECT * FROM commodity_catalog WHERE rawKey = :key LIMIT 1")
    suspend fun getCommodityByKey(key: String): CommodityEntity?

    @Query("SELECT * FROM commodity_catalog WHERE LOWER(rawKey) LIKE '%' || LOWER(:query) || '%' OR LOWER(canonicalName) LIKE '%' || LOWER(:query) || '%' OR LOWER(brand) LIKE '%' || LOWER(:query) || '%' ORDER BY useCount DESC LIMIT 10")
    suspend fun searchCommoditiesFuzzy(query: String): List<CommodityEntity>

    @Query("SELECT * FROM commodity_catalog ORDER BY useCount DESC, id ASC")
    fun getAllCommodities(): Flow<List<CommodityEntity>>

    @Query("SELECT COUNT(*) FROM commodity_catalog")
    suspend fun getCommoditiesCount(): Int

    @Query("SELECT COUNT(*) FROM commodity_catalog WHERE isPreSeeded = 1")
    suspend fun getPreSeededCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommodity(commodity: CommodityEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCommodities(commodities: List<CommodityEntity>)

    @Query("UPDATE commodity_catalog SET useCount = useCount + 1, lastKnownPrice = :price WHERE rawKey = :key")
    suspend fun incrementCommodityUsage(key: String, price: Double)

    // Storefront Inventory & POS Extensions
    @Query("UPDATE commodity_catalog SET stockQuantity = :newStock WHERE id = :id")
    suspend fun updateCommodityStock(id: Long, newStock: Double)

    @Query("UPDATE commodity_catalog SET stockQuantity = MAX(0.0, stockQuantity - :qty) WHERE id = :id")
    suspend fun decrementCommodityStock(id: Long, qty: Double)

    @Query("UPDATE commodity_catalog SET stockQuantity = stockQuantity + :qty WHERE id = :id")
    suspend fun incrementCommodityStock(id: Long, qty: Double)

    @Query("UPDATE commodity_catalog SET costPrice = :costPrice, sellingPrice = :sellingPrice, lastKnownPrice = :sellingPrice WHERE id = :id")
    suspend fun updateCommodityPricing(id: Long, costPrice: Double, sellingPrice: Double)

    @Query("UPDATE commodity_catalog SET reorderThreshold = :threshold WHERE id = :id")
    suspend fun updateCommodityReorderThreshold(id: Long, threshold: Double)

    @Query("SELECT * FROM commodity_catalog WHERE stockQuantity <= reorderThreshold ORDER BY stockQuantity ASC")
    fun getLowStockCommodities(): Flow<List<CommodityEntity>>

    @Query("DELETE FROM commodity_catalog WHERE id = :id")
    suspend fun deleteCommodity(id: Long)

    @Query("SELECT * FROM transactions WHERE dateTimestamp >= :startOfDay ORDER BY dateTimestamp DESC")
    fun getTransactionsSince(startOfDay: Long): Flow<List<TransactionEntity>>

    // Grocery & Pantry
    @Query("SELECT * FROM grocery_items WHERE isPantryItem = 0 ORDER BY isChecked ASC, id DESC")
    fun getWishlistItems(): Flow<List<GroceryItemEntity>>

    @Query("SELECT * FROM grocery_items WHERE isPantryItem = 1 ORDER BY remainingDays ASC")
    fun getPantryItems(): Flow<List<GroceryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroceryItem(item: GroceryItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroceryItems(items: List<GroceryItemEntity>)

    @Update
    suspend fun updateGroceryItem(item: GroceryItemEntity)

    @Query("DELETE FROM grocery_items WHERE id = :id")
    suspend fun deleteGroceryItem(id: Long)

    @Query("DELETE FROM grocery_items WHERE isPantryItem = 0 AND isChecked = 1")
    suspend fun deleteCheckedWishlistItems()
}
