package com.example.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager worker that periodically checks:
 * 1. Category and Monthly Budget thresholds (80% approaching, 100% overspend).
 * 2. Recurring bills & subscriptions due within the next 24 hours.
 * Dispatches local rich push notifications to alert the user.
 */
class BudgetAlertWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "cyphr_budget_and_bills_alert_worker"
        const val CHANNEL_ID_BUDGET = "cyphr_budget_alerts"
        const val CHANNEL_ID_BILLS = "cyphr_bill_reminders"

        fun schedulePeriodicCheck(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .build()

                // Run periodic check every 3 hours (WorkManager minimum is 15 minutes)
                val periodicWorkRequest = PeriodicWorkRequestBuilder<BudgetAlertWorker>(
                    3, TimeUnit.HOURS,
                    30, TimeUnit.MINUTES // Flex interval
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    periodicWorkRequest
                )
            } catch (e: Exception) {
                // WorkManager might not be initialized in headless test environments
            }
        }

        fun runOneTimeCheckNow(context: Context) {
            try {
                val oneTimeWork = androidx.work.OneTimeWorkRequestBuilder<BudgetAlertWorker>()
                    .build()
                WorkManager.getInstance(context).enqueue(oneTimeWork)
            } catch (e: Exception) {
                // WorkManager might not be initialized in headless test environments
            }
        }
    }

    override suspend fun doWork(): Result {
        try {
            createNotificationChannels()
            val db = AppDatabase.getInstance(context)
            val sharedPrefs = context.getSharedPreferences("cyphr_prefs", Context.MODE_PRIVATE)

            checkBudgetThresholds(db, sharedPrefs)
            checkUpcomingRecurringBills(db)

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    private suspend fun checkBudgetThresholds(
        db: AppDatabase,
        sharedPrefs: android.content.SharedPreferences
    ) {
        val transactions = db.expenseDao().getAllTransactions().first()
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        // Calculate this month's transactions
        val thisMonthTx = transactions.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.dateTimestamp }
            txCal.get(Calendar.MONTH) == currentMonth && txCal.get(Calendar.YEAR) == currentYear
        }

        val totalSpentThisMonth = thisMonthTx.sumOf { it.totalAmount }
        val monthlyBudget = sharedPrefs.getFloat("monthly_budget", 15000f).toDouble()

        // 1. Check Overall Monthly Budget
        if (monthlyBudget > 0) {
            val ratio = totalSpentThisMonth / monthlyBudget
            val lastNotifiedOverallLevel = sharedPrefs.getInt("last_notified_overall_budget_level", 0)

            if (ratio >= 1.0 && lastNotifiedOverallLevel < 100) {
                sendNotification(
                    id = 1001,
                    channelId = CHANNEL_ID_BUDGET,
                    title = "🚨 Monthly Budget Exceeded (100%)",
                    message = "You have spent ₹${totalSpentThisMonth.toInt()} of your ₹${monthlyBudget.toInt()} monthly budget.",
                    priority = NotificationCompat.PRIORITY_HIGH
                )
                sharedPrefs.edit().putInt("last_notified_overall_budget_level", 100).apply()
            } else if (ratio >= 0.8 && ratio < 1.0 && lastNotifiedOverallLevel < 80) {
                val remaining = (monthlyBudget - totalSpentThisMonth).coerceAtLeast(0.0)
                sendNotification(
                    id = 1002,
                    channelId = CHANNEL_ID_BUDGET,
                    title = "⚠️ Approaching Monthly Budget (80%)",
                    message = "Spent ${(ratio * 100).toInt()}% (₹${totalSpentThisMonth.toInt()}/₹${monthlyBudget.toInt()}). Only ₹${remaining.toInt()} left this month!",
                    priority = NotificationCompat.PRIORITY_DEFAULT
                )
                sharedPrefs.edit().putInt("last_notified_overall_budget_level", 80).apply()
            }
        }

        // 2. Check Category-Level Budgets
        val categorySpends = thisMonthTx.groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.totalAmount } }

        val defaultCategoryBudgets = mapOf(
            "Groceries" to 6000.0,
            "Dairy" to 1500.0,
            "Dining" to 2500.0,
            "Transport" to 2000.0,
            "Utilities" to 2500.0,
            "Shopping" to 3000.0,
            "Healthcare" to 1500.0,
            "Entertainment" to 1200.0,
            "Personal Care" to 1000.0,
            "Miscellaneous" to 1000.0
        )

        for ((cat, defaultLimit) in defaultCategoryBudgets) {
            val limit = sharedPrefs.getFloat("cat_budget_$cat", defaultLimit.toFloat()).toDouble()
            val spent = categorySpends[cat] ?: 0.0
            if (limit <= 0) continue

            val catRatio = spent / limit
            val prefKey = "last_notified_cat_level_$cat"
            val lastNotifiedCatLevel = sharedPrefs.getInt(prefKey, 0)
            val notifId = 2000 + cat.hashCode() % 1000

            if (catRatio >= 1.0 && lastNotifiedCatLevel < 100) {
                sendNotification(
                    id = notifId,
                    channelId = CHANNEL_ID_BUDGET,
                    title = "🚨 $cat Budget Breached!",
                    message = "You spent ₹${spent.toInt()} on $cat (Budget: ₹${limit.toInt()}). Over by ₹${(spent - limit).toInt()}.",
                    priority = NotificationCompat.PRIORITY_HIGH
                )
                sharedPrefs.edit().putInt(prefKey, 100).apply()
            } else if (catRatio >= 0.8 && catRatio < 1.0 && lastNotifiedCatLevel < 80) {
                val remaining = (limit - spent).coerceAtLeast(0.0)
                sendNotification(
                    id = notifId,
                    channelId = CHANNEL_ID_BUDGET,
                    title = "⚠️ $cat Budget at ${(catRatio * 100).toInt()}%",
                    message = "Spent ₹${spent.toInt()} of ₹${limit.toInt()} for $cat. ₹${remaining.toInt()} remaining.",
                    priority = NotificationCompat.PRIORITY_DEFAULT
                )
                sharedPrefs.edit().putInt(prefKey, 80).apply()
            }
        }
    }

    private suspend fun checkUpcomingRecurringBills(db: AppDatabase) {
        val now = System.currentTimeMillis()
        val next24Hours = now + TimeUnit.HOURS.toMillis(24)

        val activeBills = db.recurringBillDao().getActiveRecurringBills().first()

        for (bill in activeBills) {
            // Check if bill due date falls within next 24 hours (or is overdue today)
            if (bill.nextDueDate in (now - TimeUnit.HOURS.toMillis(6))..next24Hours) {
                val hoursRemaining = ((bill.nextDueDate - now) / (1000 * 60 * 60)).coerceAtLeast(0)
                val timeDesc = if (hoursRemaining <= 1) "due in less than an hour!" else "due in ~$hoursRemaining hours!"

                sendNotification(
                    id = 3000 + (bill.id.toInt() % 1000),
                    channelId = CHANNEL_ID_BILLS,
                    title = "🔔 Bill Due Soon: ${bill.title}",
                    message = "₹${bill.amount.toInt()} for ${bill.title} (${bill.category}) is $timeDesc (${bill.paymentMethod})",
                    priority = NotificationCompat.PRIORITY_HIGH
                )
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Budget Alerts Channel
            val budgetChannel = NotificationChannel(
                CHANNEL_ID_BUDGET,
                "Budget & Overspend Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when reaching 80% or exceeding monthly category budgets"
                enableVibration(true)
            }

            // Recurring Bills Channel
            val billsChannel = NotificationChannel(
                CHANNEL_ID_BILLS,
                "Recurring Bill Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for recurring subscriptions and utility bills due within 24 hours"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(budgetChannel)
            notificationManager.createNotificationChannel(billsChannel)
        }
    }

    private fun sendNotification(
        id: Int,
        channelId: String,
        title: String,
        message: String,
        priority: Int
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id, notification)
    }
}
