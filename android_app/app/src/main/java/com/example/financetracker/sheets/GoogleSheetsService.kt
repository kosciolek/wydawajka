package com.example.financetracker.sheets

import android.util.Log
import com.example.financetracker.model.Expense
import com.example.financetracker.model.SpendingSummary
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest
import com.google.api.services.sheets.v4.model.DimensionRange
import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class GoogleSheetsService {

    companion object {
        private const val TAG = "GoogleSheetsService"
        private const val APPLICATION_NAME = "FinanceTracker"
    }

    private var sheetsService: Sheets? = null
    private var spreadsheetId: String? = null

    suspend fun configure(serviceAccountJson: String, spreadsheetId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val credentials = GoogleCredentials
                    .fromStream(ByteArrayInputStream(serviceAccountJson.toByteArray()))
                    .createScoped(listOf(SheetsScopes.SPREADSHEETS))

                val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
                val jsonFactory = GsonFactory.getDefaultInstance()

                sheetsService = Sheets.Builder(
                    httpTransport,
                    jsonFactory,
                    HttpCredentialsAdapter(credentials)
                )
                    .setApplicationName(APPLICATION_NAME)
                    .build()

                this@GoogleSheetsService.spreadsheetId = spreadsheetId
                Log.d(TAG, "Google Sheets service configured successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to configure Google Sheets service", e)
                false
            }
        }
    }

    suspend fun appendExpense(expense: Expense): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val service = sheetsService ?: return@withContext Result.failure(
                    IllegalStateException("Sheets service not configured")
                )
                val sheetId = spreadsheetId ?: return@withContext Result.failure(
                    IllegalStateException("Spreadsheet ID not set")
                )

                val formatter = DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault())

                // Columns: ID, datetime, text
                val row = listOf(
                    expense.id,
                    formatter.format(expense.datetime),
                    expense.text
                )

                val body = ValueRange().setValues(listOf(row))

                service.spreadsheets().values()
                    .append(sheetId, "A:C", body)
                    .setValueInputOption("USER_ENTERED")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute()

                Log.d(TAG, "Expense appended to spreadsheet with ID: ${expense.id}")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to append expense", e)
                Result.failure(e)
            }
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val service = sheetsService ?: return@withContext Result.failure(
                    IllegalStateException("Sheets service not configured")
                )
                val sheetId = spreadsheetId ?: return@withContext Result.failure(
                    IllegalStateException("Spreadsheet ID not set")
                )

                val response = service.spreadsheets().values()
                    .get(sheetId, "A:A")
                    .execute()

                val values = response.getValues() ?: return@withContext Result.failure(
                    IllegalStateException("No data in spreadsheet")
                )

                val rowIndex = values.indexOfFirst { row ->
                    row.isNotEmpty() && row[0].toString() == expenseId
                }

                if (rowIndex == -1) {
                    return@withContext Result.failure(
                        IllegalStateException("Expense not found: $expenseId")
                    )
                }

                val spreadsheet = service.spreadsheets().get(sheetId).execute()
                val numericSheetId = spreadsheet.sheets[0].properties.sheetId

                val deleteRequest = Request().setDeleteDimension(
                    DeleteDimensionRequest().setRange(
                        DimensionRange()
                            .setSheetId(numericSheetId)
                            .setDimension("ROWS")
                            .setStartIndex(rowIndex)
                            .setEndIndex(rowIndex + 1)
                    )
                )

                service.spreadsheets().batchUpdate(
                    sheetId,
                    BatchUpdateSpreadsheetRequest().setRequests(listOf(deleteRequest))
                ).execute()

                Log.d(TAG, "Expense deleted from spreadsheet: $expenseId")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete expense", e)
                Result.failure(e)
            }
        }
    }

    suspend fun fetchSpendingSummary(): Result<SpendingSummary> {
        return withContext(Dispatchers.IO) {
            try {
                val service = sheetsService ?: return@withContext Result.failure(
                    IllegalStateException("Sheets service not configured")
                )
                val sheetId = spreadsheetId ?: return@withContext Result.failure(
                    IllegalStateException("Spreadsheet ID not set")
                )

                // Fetch columns B (date) and D (price)
                val response = service.spreadsheets().values()
                    .get(sheetId, "B:D")
                    .execute()

                val values = response.getValues() ?: return@withContext Result.success(
                    SpendingSummary(0.0, 0.0, 0.0)
                )

                val today = LocalDate.now()
                val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

                var todayTotal = 0.0
                var last7DaysTotal = 0.0
                var last30DaysTotal = 0.0

                for (row in values) {
                    if (row.size < 3) continue

                    val dateStr = row[0]?.toString() ?: continue
                    val priceStr = row[2]?.toString() ?: continue

                    val price = priceStr.replace(",", ".").toDoubleOrNull() ?: continue

                    val rowDate = try {
                        LocalDateTime.parse(dateStr, dateFormatter).toLocalDate()
                    } catch (e: Exception) {
                        continue
                    }

                    val daysAgo = java.time.temporal.ChronoUnit.DAYS.between(rowDate, today)

                    if (daysAgo == 0L) {
                        todayTotal += price
                    }
                    if (daysAgo in 0..6) {
                        last7DaysTotal += price
                    }
                    if (daysAgo in 0..29) {
                        last30DaysTotal += price
                    }
                }

                Log.d(TAG, "Spending summary: today=$todayTotal, 7d=$last7DaysTotal, 30d=$last30DaysTotal")
                Result.success(SpendingSummary(todayTotal, last7DaysTotal, last30DaysTotal))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch spending summary", e)
                Result.failure(e)
            }
        }
    }

    fun isConfigured(): Boolean = sheetsService != null && spreadsheetId != null
}
