package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.KhataEntryEntity
import com.example.data.model.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsAndKhataDao {
    // --- Savings Goals ---
    @Query("SELECT * FROM savings_goals ORDER BY isCompleted ASC, targetDate ASC")
    fun getAllSavingsGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getSavingsGoalById(id: Long): SavingsGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoals(goals: List<SavingsGoalEntity>)

    @Update
    suspend fun updateSavingsGoal(goal: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteSavingsGoal(id: Long)

    @Query("UPDATE savings_goals SET currentAmount = :newAmount, isCompleted = CASE WHEN :newAmount >= targetAmount THEN 1 ELSE 0 END WHERE id = :id")
    suspend fun updateGoalAmount(id: Long, newAmount: Double)

    @Query("SELECT COUNT(*) FROM savings_goals")
    suspend fun getSavingsGoalsCount(): Int

    // --- Khata (Ledger) Entries ---
    @Query("SELECT * FROM khata_entries ORDER BY isSettled ASC, dateTimestamp DESC")
    fun getAllKhataEntries(): Flow<List<KhataEntryEntity>>

    @Query("SELECT * FROM khata_entries WHERE isSettled = 0 ORDER BY dateTimestamp DESC")
    fun getPendingKhataEntries(): Flow<List<KhataEntryEntity>>

    @Query("SELECT * FROM khata_entries WHERE id = :id")
    suspend fun getKhataEntryById(id: Long): KhataEntryEntity?

    @Query("SELECT * FROM khata_entries WHERE personName = :name OR personPhoneOrUpi = :phone ORDER BY dateTimestamp DESC")
    fun getKhataEntriesByPerson(name: String, phone: String): Flow<List<KhataEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKhataEntry(entry: KhataEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKhataEntries(entries: List<KhataEntryEntity>)

    @Update
    suspend fun updateKhataEntry(entry: KhataEntryEntity)

    @Query("DELETE FROM khata_entries WHERE id = :id")
    suspend fun deleteKhataEntry(id: Long)

    @Query("UPDATE khata_entries SET isSettled = 1, settledDateTimestamp = :settledTime, paidAmount = amount WHERE id = :id")
    suspend fun settleKhataEntry(id: Long, settledTime: Long)

    @Query("UPDATE khata_entries SET paidAmount = :newPaidAmount, lastPaymentDateTimestamp = :paymentTime, paymentHistoryJson = :historyJson, isSettled = CASE WHEN :newPaidAmount >= amount THEN 1 ELSE 0 END, settledDateTimestamp = CASE WHEN :newPaidAmount >= amount THEN :paymentTime ELSE NULL END WHERE id = :id")
    suspend fun updateKhataPartialPayment(id: Long, newPaidAmount: Double, paymentTime: Long, historyJson: String)

    @Query("SELECT COUNT(*) FROM khata_entries")
    suspend fun getKhataEntriesCount(): Int
}
