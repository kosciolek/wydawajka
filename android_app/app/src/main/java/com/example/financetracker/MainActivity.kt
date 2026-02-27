package com.example.financetracker

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.financetracker.model.Expense
import com.example.financetracker.model.SpendingSummary
import com.example.financetracker.settings.SettingsRepository
import com.example.financetracker.settings.SettingsScreen
import com.example.financetracker.api.CloudflareWorkerService
import com.example.financetracker.speech.AndroidSpeechRecognizer
import com.example.financetracker.speech.SpeechToText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {

    private lateinit var speechToText: SpeechToText
    private lateinit var settingsRepository: SettingsRepository
    private val workerService = CloudflareWorkerService()

    private var appState by mutableStateOf<AppState>(AppState.RequestingPermission)
    private var currentScreen by mutableStateOf<Screen>(Screen.Main)

    // Settings state
    private var workerUrl by mutableStateOf("")
    private var apiToken by mutableStateOf("")
    private var speechTimeoutSeconds by mutableStateOf(SettingsRepository.DEFAULT_SPEECH_TIMEOUT)
    private var isSavingSettings by mutableStateOf(false)
    private var settingsSaveResult by mutableStateOf<String?>(null)

    // Spending summary state
    private var spendingSummary by mutableStateOf<SpendingSummary?>(null)
    private var spendingSummaryError by mutableStateOf<String?>(null)

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                if (appState is AppState.Recording) {
                    stopRecording()
                }
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRecording()
        } else {
            appState = AppState.Error("Microphone permission required")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        speechToText = AndroidSpeechRecognizer(this)
        settingsRepository = SettingsRepository(this)

        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

        // Load saved settings
        lifecycleScope.launch {
            workerUrl = settingsRepository.workerUrl.first()
            apiToken = settingsRepository.apiToken.first()
            speechTimeoutSeconds = settingsRepository.speechTimeoutSeconds.first()

            if (workerUrl.isNotBlank() && apiToken.isNotBlank()) {
                workerService.configure(workerUrl, apiToken)
            }
        }

        setContent {
            FinanceTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen) {
                        Screen.Main -> {
                            MainScreen(
                                state = appState,
                                onScreenTap = {
                                    if (appState is AppState.Recording) {
                                        stopRecording()
                                    }
                                },
                                onDone = {
                                    appState = AppState.Idle
                                },
                                onRetry = { checkPermissionAndStart() },
                                onRepeat = { repeatExpense() },
                                onDelete = { deleteExpense() },
                                onSettings = { currentScreen = Screen.Settings },
                                onStartRecording = { checkPermissionAndStart() },
                                onManualSubmit = { text -> processText(text) },
                                isWorkerConfigured = workerService.isConfigured(),
                                spendingSummary = spendingSummary,
                                spendingSummaryError = spendingSummaryError
                            )
                        }
                        Screen.Settings -> {
                            SettingsScreen(
                                workerUrl = workerUrl,
                                apiToken = apiToken,
                                speechTimeoutSeconds = speechTimeoutSeconds,
                                onWorkerUrlChange = { workerUrl = it },
                                onApiTokenChange = { apiToken = it },
                                onSpeechTimeoutChange = { speechTimeoutSeconds = it },
                                onSave = { saveSettings() },
                                onBack = {
                                    currentScreen = Screen.Main
                                    settingsSaveResult = null
                                },
                                isSaving = isSavingSettings,
                                saveResult = settingsSaveResult
                            )
                        }
                    }
                }
            }
        }

        // Check if launched from dictation shortcut
        val isDictationMode = intent?.component?.shortClassName == ".DictateShortcut"
        if (isDictationMode) {
            checkPermissionAndStart()
        } else {
            appState = AppState.Idle
        }
    }

    override fun onResume() {
        super.onResume()
        fetchSpendingSummary()
    }

    private fun fetchSpendingSummary() {
        if (!workerService.isConfigured()) return

        lifecycleScope.launch {
            val result = workerService.fetchSpendingSummary()
            if (result.isSuccess) {
                spendingSummary = result.getOrNull()
                spendingSummaryError = null
            } else {
                spendingSummaryError = result.exceptionOrNull()?.message ?: "Unknown error"
            }
        }
    }

    private fun saveSettings() {
        isSavingSettings = true
        settingsSaveResult = null

        lifecycleScope.launch {
            try {
                settingsRepository.setWorkerUrl(workerUrl)
                settingsRepository.setApiToken(apiToken)
                settingsRepository.setSpeechTimeoutSeconds(speechTimeoutSeconds)

                if (workerUrl.isNotBlank() && apiToken.isNotBlank()) {
                    workerService.configure(workerUrl, apiToken)
                }

                settingsSaveResult = "Zapisano!"
            } catch (e: Exception) {
                settingsSaveResult = "Błąd: ${e.message}"
            } finally {
                isSavingSettings = false
            }
        }
    }

    private fun checkPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startRecording()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startRecording() {
        appState = AppState.Recording
        speechToText.startListening(
            onResult = { text ->
                processText(text)
            },
            onError = { error ->
                appState = AppState.Error(error)
            },
            silenceTimeoutMs = speechTimeoutSeconds * 1000L
        )
    }

    private fun stopRecording() {
        speechToText.stopListening()
    }

    private fun processText(text: String) {
        val expense = Expense(datetime = Instant.now(), text = text)

        if (workerService.isConfigured()) {
            appState = AppState.Processing("Uploading...")
            lifecycleScope.launch {
                val result = workerService.appendExpense(expense)
                if (result.isSuccess) {
                    appState = AppState.Displaying(expense, uploaded = true)
                } else {
                    appState = AppState.Displaying(expense, uploaded = false, uploadError = result.exceptionOrNull()?.message)
                }
            }
        } else {
            appState = AppState.Displaying(expense, uploaded = false)
        }
    }

    private fun repeatExpense() {
        val currentState = appState
        if (currentState !is AppState.Displaying) return

        val expenseToDelete = currentState.expense
        appState = currentState.copy(isDeleting = true)

        lifecycleScope.launch {
            val deleteResult = workerService.deleteExpense(expenseToDelete.id)
            if (deleteResult.isFailure) {
                appState = AppState.Error("Nie udało się usunąć: ${deleteResult.exceptionOrNull()?.message}")
                return@launch
            }

            appState = AppState.RequestingPermission
            checkPermissionAndStart()
        }
    }

    private fun deleteExpense() {
        val currentState = appState
        if (currentState !is AppState.Displaying) return

        val expenseToDelete = currentState.expense
        appState = currentState.copy(isDeleting = true)

        lifecycleScope.launch {
            val deleteResult = workerService.deleteExpense(expenseToDelete.id)
            if (deleteResult.isSuccess) {
                appState = AppState.RequestingPermission
                checkPermissionAndStart()
            } else {
                appState = AppState.Error("Nie udało się usunąć: ${deleteResult.exceptionOrNull()?.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
        speechToText.destroy()
    }
}

enum class Screen {
    Main, Settings
}

sealed class AppState {
    object Idle : AppState()
    object RequestingPermission : AppState()
    object Recording : AppState()
    data class Processing(val message: String = "Processing...") : AppState()
    data class Displaying(
        val expense: Expense,
        val uploaded: Boolean = false,
        val uploadError: String? = null,
        val isDeleting: Boolean = false
    ) : AppState()
    data class Error(val message: String) : AppState()
}

@Composable
fun MainScreen(
    state: AppState,
    onScreenTap: () -> Unit,
    onDone: () -> Unit,
    onRetry: () -> Unit,
    onRepeat: () -> Unit,
    onDelete: () -> Unit,
    onSettings: () -> Unit,
    onStartRecording: () -> Unit,
    onManualSubmit: (String) -> Unit,
    isWorkerConfigured: Boolean,
    spendingSummary: SpendingSummary?,
    spendingSummaryError: String?
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onScreenTap() },
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is AppState.Idle -> {
                    IdleScreen(
                        onStartRecording = onStartRecording,
                        onManualSubmit = onManualSubmit
                    )
                }
                is AppState.RequestingPermission -> {
                    Text("Requesting permission...")
                }
                is AppState.Recording -> {
                    RecordingScreen()
                }
                is AppState.Processing -> {
                    ProcessingScreen(message = state.message)
                }
                is AppState.Displaying -> {
                    ResultScreen(
                        expense = state.expense,
                        uploaded = state.uploaded,
                        uploadError = state.uploadError,
                        onDone = onDone,
                        onRepeat = onRepeat,
                        onDelete = onDelete,
                        isDeleting = state.isDeleting
                    )
                }
                is AppState.Error -> {
                    ErrorScreen(message = state.message, onRetry = onRetry, onDone = onDone)
                }
            }
        }

        // Spending summary in top left
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            if (spendingSummaryError != null) {
                Text(
                    text = "!",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (spendingSummary != null) {
                SpendingSummaryDisplay(spendingSummary)
            }
        }

        // Settings button in top right (drawn last to be on top)
        IconButton(
            onClick = onSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = if (isWorkerConfigured)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun SpendingSummaryDisplay(summary: SpendingSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SpendingRow("Dziś", summary.today)
        SpendingRow("7 dni", summary.last7Days)
        SpendingRow("30 dni", summary.last30Days)
    }
}

@Composable
fun SpendingRow(label: String, amount: Double) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            text = "${amount.toInt()} zł",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun IdleScreen(
    onStartRecording: () -> Unit,
    onManualSubmit: (String) -> Unit
) {
    var manualText by remember { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        CrabAssistant(message = "Cześć!")

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStartRecording,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Nagraj wydatek", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider(modifier = Modifier.fillMaxWidth(0.6f))

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = manualText,
            onValueChange = { manualText = it },
            label = { Text("Lub wpisz ręcznie") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (manualText.isNotBlank()) {
                    onManualSubmit(manualText)
                    manualText = ""
                }
            },
            enabled = manualText.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Dodaj")
        }
    }
}

@Composable
fun CrabAssistant(
    message: String,
    modifier: Modifier = Modifier,
    animate: Boolean = false
) {
    val scale = if (animate) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        ).value
    } else 1f

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.crab_assistant),
            contentDescription = "Krabuś",
            modifier = Modifier
                .size(180.dp)
                .scale(scale)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = message,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RecordingScreen() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CrabAssistant(
            message = "Słucham!",
            animate = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Tap to stop",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ProcessingScreen(message: String = "Processing...") {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CrabAssistant(
            message = "Myślę...",
            animate = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        CircularProgressIndicator(
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun ResultScreen(
    expense: Expense,
    uploaded: Boolean,
    uploadError: String?,
    onDone: () -> Unit,
    onRepeat: () -> Unit,
    onDelete: () -> Unit,
    isDeleting: Boolean = false
) {
    val formatter = DateTimeFormatter
        .ofPattern("dd MMM yyyy HH:mm")
        .withZone(ZoneId.systemDefault())

    val crabMessage = when {
        isDeleting -> "Usuwam..."
        uploaded -> "Zapisane!"
        uploadError != null -> "Zapisałem, ale upload nie poszedł..."
        else -> "Mam!"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.crab_assistant),
            contentDescription = "Krabuś",
            modifier = Modifier.size(100.dp)
        )

        Text(
            text = crabMessage,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = formatter.format(expense.datetime),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "\"${expense.text}\"",
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uploaded) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Przesłano",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        } else if (uploadError != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Upload nie powiódł się: $uploadError",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onDelete,
                enabled = uploaded && !isDeleting,
                modifier = Modifier.weight(1f)
            ) {
                Text("Usuń")
            }

            OutlinedButton(
                onClick = onRepeat,
                enabled = uploaded && !isDeleting,
                modifier = Modifier.weight(1f)
            ) {
                Text("Powtórz")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onDone,
            enabled = !isDeleting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Nagraj kolejny", fontSize = 18.sp)
        }
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit, onDone: () -> Unit) {
    val crabMessage = when {
        message.contains("No speech", ignoreCase = true) ||
        message.contains("no match", ignoreCase = true) ||
        message.contains("didn't catch", ignoreCase = true) -> "Nic nie słyszę..."
        message.contains("permission", ignoreCase = true) -> "Potrzebuję dostępu do mikrofonu!"
        message.contains("network", ignoreCase = true) ||
        message.contains("internet", ignoreCase = true) -> "Nie mam połączenia..."
        else -> "Coś poszło nie tak..."
    }

    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CrabAssistant(message = crabMessage)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = onRetry) {
                Text("Spróbuj ponownie")
            }
            Button(onClick = onDone) {
                Text("Zamknij")
            }
        }
    }
}

@Composable
fun FinanceTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF4CAF50),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E)
        ),
        content = content
    )
}
