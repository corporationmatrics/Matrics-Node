package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for tracking recurring bills, utilities, and subscriptions with reminder schedules.
 */
@Entity(tableName = "recurring_bills")
data class RecurringBillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String = "Subscriptions",
    val billingCycle: String = "MONTHLY", // MONTHLY, QUARTERLY, ANNUAL, WEEKLY
    val dueDay: Int = 1, // Day of month or interval
    val nextDueDate: Long = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000),
    val paymentMethod: String = "UPI AutoPay",
    val isAutoDebit: Boolean = true,
    val reminderDaysBefore: Int = 3,
    val status: String = "ACTIVE", // ACTIVE, PAUSED, PAID_THIS_CYCLE
    val lastPaidDate: Long? = null,
    val serviceIcon: String = "DEFAULT", // NETFLIX, SPOTIFY, AIRTEL, ELECTRICITY, RENT, GYM, GOOGLE_ONE, CLOUD, DEFAULT
    val notes: String = ""
)

/**
 * Entity for tracking user savings targets, progress, and milestone projections.
 */
@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDate: Long = System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000),
    val category: String = "Emergency", // Emergency, Gadgets, Travel, Investment, Vehicle, Home
    val monthlyContributionTarget: Double = 5000.0,
    val colorHex: String = "#FF6B35",
    val notes: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Entity for customer and vendor credit ledger (Khata / Udhaar) tracking "You Will Get" vs "You Will Pay".
 * Supports running balance ledgers, partial installment payments, and WhatsApp reminders.
 */
@Entity(tableName = "khata_entries")
data class KhataEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personName: String,
    val personPhoneOrUpi: String = "",
    val type: String = "YOU_WILL_GET", // YOU_WILL_GET (Customer Udhaar / Store Credit receivable), YOU_WILL_PAY (Supplier payable)
    val amount: Double,
    val description: String = "",
    val dateTimestamp: Long = System.currentTimeMillis(),
    val dueDateTimestamp: Long? = null,
    val isSettled: Boolean = false,
    val settledDateTimestamp: Long? = null,
    val splitGroupId: String? = null,
    val paidAmount: Double = 0.0,
    val lastPaymentDateTimestamp: Long? = null,
    val paymentHistoryJson: String = "",
    val customerLoyaltyPoints: Int = 0,
    val customerTag: String = "REGULAR", // REGULAR, VIP, WHOLESALE, SLOW_PAYER
    val invoiceNumber: String = ""
) {
    val remainingAmount: Double
        get() = (amount - paidAmount).coerceAtLeast(0.0)

    val progressFraction: Float
        get() = if (amount > 0) (paidAmount / amount).coerceIn(0.0, 1.0).toFloat() else 0f

    fun isOverdue(currentTime: Long = System.currentTimeMillis()): Boolean {
        return !isSettled && dueDateTimestamp != null && dueDateTimestamp < currentTime
    }
}

/**
 * Model representing an individual partial installment payment made against a Khata credit entry.
 */
data class KhataInstallmentPayment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val paymentMode: String = "CASH", // CASH, UPI_INSTANT, BANK_TRANSFER, CHEQUE
    val utrNumber: String = "",
    val note: String = ""
)

/**
 * Aggregated Customer Profile summary containing lifetime metrics, purchase history,
 * loyalty tier status, and running Khata balances.
 */
data class CustomerProfileData(
    val customerName: String,
    val customerPhone: String,
    val totalLifetimeSpend: Double = 0.0,
    val totalBillsCount: Int = 0,
    val averageBillAmount: Double = 0.0,
    val frequentItems: List<FrequentCustomerItem> = emptyList(),
    val loyaltyPoints: Int = 0,
    val loyaltyTier: CustomerLoyaltyTier = CustomerLoyaltyTier.BRONZE,
    val outstandingKhataBalance: Double = 0.0,
    val totalKhataPaid: Double = 0.0,
    val firstVisitDate: Long = System.currentTimeMillis(),
    val lastVisitDate: Long = System.currentTimeMillis(),
    val openKhataEntriesCount: Int = 0,
    val khataEntries: List<KhataEntryEntity> = emptyList(),
    val pastTransactions: List<TransactionEntity> = emptyList()
)

data class FrequentCustomerItem(
    val itemName: String,
    val quantity: Double,
    val unit: String,
    val totalSpend: Double,
    val purchaseCount: Int
)

enum class CustomerLoyaltyTier(val displayName: String, val minPoints: Int, val perkDescription: String, val colorHex: Long) {
    BRONZE("Bronze Member", 0, "1% reward points on every ₹100", 0xFFCD7F32),
    SILVER("Silver Shopper", 150, "2% points + Zero-fee Khata credit", 0xFFC0C0C0),
    GOLD("Gold VIP", 400, "5% instant cashback & Priority billing", 0xFFFFD700),
    PLATINUM("Platinum Patron", 800, "10% VIP perks, Free Home Delivery & Custom credit line", 0xFFE5E4E2);

    val tierName: String
        get() = displayName
}

/**
 * In-memory model for bill splitting calculations and group expense division.
 */
data class BillSplitParticipant(
    val id: String,
    val name: String,
    val upiOrPhone: String = "",
    val customAmount: Double = 0.0,
    val isPaidByThisPerson: Boolean = false,
    val isIncludedInSplit: Boolean = true
)

data class BillSplitResult(
    val totalBillAmount: Double,
    val perPersonEqualAmount: Double,
    val title: String,
    val payerName: String,
    val payerUpi: String,
    val breakdown: List<ParticipantDebtSummary>
)

data class ParticipantDebtSummary(
    val participantName: String,
    val participantUpi: String,
    val owesAmount: Double,
    val sharePercent: Double
)
