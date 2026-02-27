package com.example.financetracker.model

data class SpendingSummary(
    val today: Double,
    val last7Days: Double,
    val last30Days: Double
)
