package com.example.financetracker.model

import java.time.Instant
import java.util.UUID

data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val datetime: Instant,
    val text: String
)
