package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.BatchEntity
import com.example.data.model.PurchaseOrderEntity
import com.example.data.model.PurchaseOrderItemEntity
import com.example.data.model.StockMovementEntity
import com.example.data.model.SupplierEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplyChainDao {

    // --- Suppliers ---
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE id = :id LIMIT 1")
    suspend fun getSupplierById(id: Long): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuppliers(suppliers: List<SupplierEntity>)

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Query("DELETE FROM suppliers WHERE id = :id")
    suspend fun deleteSupplier(id: Long)

    @Query("UPDATE suppliers SET outstandingPayable = outstandingPayable + :amount WHERE id = :id")
    suspend fun addOutstandingPayable(id: Long, amount: Double)

    @Query("UPDATE suppliers SET outstandingPayable = MAX(0.0, outstandingPayable - :amount) WHERE id = :id")
    suspend fun deductOutstandingPayable(id: Long, amount: Double)

    // --- Purchase Orders ---
    @Query("SELECT * FROM purchase_orders ORDER BY orderDateTimestamp DESC")
    fun getAllPurchaseOrders(): Flow<List<PurchaseOrderEntity>>

    @Query("SELECT * FROM purchase_orders WHERE id = :id LIMIT 1")
    suspend fun getPurchaseOrderById(id: Long): PurchaseOrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseOrder(po: PurchaseOrderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseOrderItems(items: List<PurchaseOrderItemEntity>)

    @Update
    suspend fun updatePurchaseOrder(po: PurchaseOrderEntity)

    @Query("UPDATE purchase_orders SET status = :status, receivedDateTimestamp = :receivedTime, grnNumber = :grn WHERE id = :poId")
    suspend fun updatePurchaseOrderStatus(poId: Long, status: String, receivedTime: Long? = null, grn: String = "")

    @Query("UPDATE purchase_orders SET paymentStatus = :paymentStatus WHERE id = :poId")
    suspend fun updatePurchaseOrderPaymentStatus(poId: Long, paymentStatus: String)

    @Query("SELECT * FROM purchase_order_items WHERE purchaseOrderId = :poId")
    fun getItemsForPurchaseOrder(poId: Long): Flow<List<PurchaseOrderItemEntity>>

    @Query("SELECT * FROM purchase_order_items WHERE purchaseOrderId = :poId")
    suspend fun getItemsForPurchaseOrderSync(poId: Long): List<PurchaseOrderItemEntity>

    @Query("DELETE FROM purchase_orders WHERE id = :id")
    suspend fun deletePurchaseOrder(id: Long)

    @Query("DELETE FROM purchase_order_items WHERE purchaseOrderId = :poId")
    suspend fun deletePurchaseOrderItems(poId: Long)

    // --- Stock Movement Ledger ---
    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC LIMIT :limit")
    fun getStockMovements(limit: Int = 150): Flow<List<StockMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovement(movement: StockMovementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovements(movements: List<StockMovementEntity>)

    // --- Batches & Expiry (FIFO/FEFO) ---
    @Query("SELECT * FROM inventory_batches WHERE isDepleted = 0 ORDER BY expiryDateTimestamp ASC")
    fun getActiveBatches(): Flow<List<BatchEntity>>

    @Query("SELECT * FROM inventory_batches WHERE commodityId = :commodityId AND isDepleted = 0 ORDER BY expiryDateTimestamp ASC")
    fun getBatchesForCommodity(commodityId: Long): Flow<List<BatchEntity>>

    @Query("SELECT * FROM inventory_batches WHERE expiryDateTimestamp <= :thresholdTimestamp AND isDepleted = 0 ORDER BY expiryDateTimestamp ASC")
    fun getNearExpiryBatches(thresholdTimestamp: Long): Flow<List<BatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: BatchEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatches(batches: List<BatchEntity>)

    @Update
    suspend fun updateBatch(batch: BatchEntity)

    @Query("UPDATE inventory_batches SET isDepleted = 1 WHERE id = :id")
    suspend fun markBatchDepleted(id: Long)
}
