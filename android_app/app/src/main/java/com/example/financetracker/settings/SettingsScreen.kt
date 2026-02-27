package com.example.financetracker.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    workerUrl: String,
    apiToken: String,
    speechTimeoutSeconds: Int,
    onWorkerUrlChange: (String) -> Unit,
    onApiTokenChange: (String) -> Unit,
    onSpeechTimeoutChange: (Int) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    isSaving: Boolean = false,
    saveResult: String? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Nagrywanie",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Czas ciszy przed zakończeniem: ${speechTimeoutSeconds}s",
                fontSize = 14.sp
            )

            Slider(
                value = speechTimeoutSeconds.toFloat(),
                onValueChange = { onSpeechTimeoutChange(it.roundToInt()) },
                valueRange = 2f..10f,
                steps = 7,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Dłuższy czas = więcej czekania po wypowiedzeniu",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Cloudflare Worker",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Połączenie z backendem:",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            OutlinedTextField(
                value = workerUrl,
                onValueChange = onWorkerUrlChange,
                label = { Text("Worker URL") },
                placeholder = { Text("https://your-worker.your-subdomain.workers.dev") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = apiToken,
                onValueChange = onApiTokenChange,
                label = { Text("API Token") },
                placeholder = { Text("Bearer token for authentication") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving && workerUrl.isNotBlank() && apiToken.isNotBlank()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isSaving) "Saving..." else "Save & Test Connection")
            }

            if (saveResult != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (saveResult.startsWith("Success") || saveResult.startsWith("Zapisano"))
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = saveResult,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
