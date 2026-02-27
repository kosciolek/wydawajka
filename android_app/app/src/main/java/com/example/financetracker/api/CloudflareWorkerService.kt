package com.example.financetracker.api

import android.util.Log
import com.example.financetracker.model.Expense
import com.example.financetracker.model.SpendingSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CloudflareWorkerService {

    companion object {
        private const val TAG = "CloudflareWorkerService"
    }

    private var workerUrl: String? = null
    private var apiToken: String? = null

    fun configure(workerUrl: String, apiToken: String) {
        this.workerUrl = workerUrl.trimEnd('/')
        this.apiToken = apiToken
    }

    fun isConfigured(): Boolean = !workerUrl.isNullOrBlank() && !apiToken.isNullOrBlank()

    suspend fun appendExpense(expense: Expense): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "${workerUrl!!}/create"
            val formatter = DateTimeFormatter.ISO_INSTANT

            val json = JSONObject().apply {
                put("uuid", expense.id)
                put("text", expense.text)
                put("timestamp", formatter.format(expense.datetime))
            }

            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer ${apiToken!!}")
                doOutput = true
            }

            OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }

            val code = conn.responseCode
            conn.disconnect()

            if (code in 200..299) {
                Log.d(TAG, "Expense uploaded: ${expense.id}")
                Result.success(Unit)
            } else {
                Result.failure(RuntimeException("HTTP $code"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload expense", e)
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "${workerUrl!!}/expense/$expenseId"

            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("Authorization", "Bearer ${apiToken!!}")
            }

            val code = conn.responseCode
            conn.disconnect()

            if (code in 200..299) {
                Log.d(TAG, "Expense deleted: $expenseId")
                Result.success(Unit)
            } else {
                Result.failure(RuntimeException("HTTP $code"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete expense", e)
            Result.failure(e)
        }
    }

    suspend fun fetchSpendingSummary(): Result<SpendingSummary> = withContext(Dispatchers.IO) {
        try {
            val url = "${workerUrl!!}/summary"

            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer ${apiToken!!}")
            }

            val code = conn.responseCode
            if (code !in 200..299) {
                conn.disconnect()
                return@withContext Result.failure(RuntimeException("HTTP $code"))
            }

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(body)
            val summary = SpendingSummary(
                today = json.getDouble("today"),
                last7Days = json.getDouble("last7Days"),
                last30Days = json.getDouble("last30Days")
            )

            Log.d(TAG, "Summary: today=${summary.today}, 7d=${summary.last7Days}, 30d=${summary.last30Days}")
            Result.success(summary)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch spending summary", e)
            Result.failure(e)
        }
    }
}
