package com.example.data.model

data class CategoryBudgetStatus(
    val category: String,
    val spent: Double,
    val limit: Double,
    val percentage: Float, // e.g. 0.85 = 85%, 1.2 = 120%
    val isOverspent: Boolean,
    val isCaution: Boolean, // >= 75% and <= 100%
    val remainingAmount: Double,
    val overspentAmount: Double,
    val iconName: String = "category"
)

data class OverspendAlertInfo(
    val category: String,
    val limit: Double,
    val currentSpend: Double,
    val overspentAmount: Double,
    val percentage: Float,
    val message: String
)
