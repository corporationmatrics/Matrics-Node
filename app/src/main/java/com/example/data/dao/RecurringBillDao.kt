package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.RecurringBillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringBillDao {
    @Query("SELECT * FROM recurring_bills ORDER BY nextDueDate ASC")
    fun getAllRecurringBills(): Flow<List<RecurringBillEntity>>

    @Query("SELECT * FROM recurring_bills WHERE status = 'ACTIVE' ORDER BY nextDueDate ASC")
    fun getActiveRecurringBills(): Flow<List<RecurringBillEntity>>

    @Query("SELECT * FROM recurring_bills WHERE id = :id")
    suspend fun getBillById(id: Long): RecurringBillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: RecurringBillEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBills(bills: List<RecurringBillEntity>)

    @Update
    suspend fun updateBill(bill: RecurringBillEntity)

    @Query("DELETE FROM recurring_bills WHERE id = :id")
    suspend fun deleteBill(id: Long)

    @Query("UPDATE recurring_bills SET status = :status, lastPaidDate = :lastPaidDate, nextDueDate = :nextDueDate WHERE id = :id")
    suspend fun updatePaymentStatus(id: Long, status: String, lastPaidDate: Long, nextDueDate: Long)

    @Query("SELECT COUNT(*) FROM recurring_bills")
    suspend fun getBillsCount(): Int
}
