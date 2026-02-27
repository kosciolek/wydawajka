# Voice Expense Tracker Design

## Overview

Android app for quick voice-based expense tracking. Launch from Samsung lock screen, speak an expense in Polish (e.g., "20zł za tankowanie auta"), see extracted data.

## Architecture

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Speech    │────▶│     LLM     │────▶│   Display   │
│  Recognizer │     │  Extractor  │     │   Results   │
└─────────────┘     └─────────────┘     └─────────────┘
```

**Swappable interfaces:**
- `SpeechToText` - Android SpeechRecognizer now, Whisper later
- `ExpenseExtractor` - llama.cpp now, Gemini Nano later

**Single Activity app** - no fragments, no navigation.

## Recording Flow

**States:** `LAUNCH → RECORDING → PROCESSING → DISPLAYING → CLOSE`

**On launch:**
1. Request microphone permission (error and close if denied)
2. Immediately start speech recognition
3. Show pulsing mic icon + "Listening..."

**Stop triggers:**
- User taps screen
- Screen turns off (`ACTION_SCREEN_OFF`)

**On stop:**
1. Get transcribed text
2. Pass to LLM extractor (show spinner)
3. Display results

**Error handling:**
- No speech → "Nothing heard" + close button
- LLM fails → show original text, leave amount/reason blank

## Text Extraction

**Current implementation:** Regex-based parsing (LLM blocked due to library compatibility)

The kotlinllamacpp library requires Kotlin 2.2+ which isn't stable yet. Using regex fallback until a compatible library is available.

**Regex patterns:**
- Amount: `(\d+(?:[.,]\d+)?)\s*(?:zł|złotych|pln|zl|złote|złoty)`
- Reason: `(?:za|na)\s+(.+?)` or text before/after amount

**Data model:**
```kotlin
data class Expense(
    val datetime: Instant,
    val amount: Double?,
    val reason: String?,
    val originalText: String
)
```

## Future LLM Integration Options

When a compatible library becomes available:
1. Wait for kotlinllamacpp to support stable Kotlin versions
2. Use llama-cpp-kt with JNA (requires building native libs manually)
3. Use a cloud API (OpenAI, Anthropic) for extraction
4. Build custom JNI bindings for llama.cpp

## UI

Single screen with three states:

**Recording:**
- Pulsing mic icon
- "Listening..." text
- "Tap to stop" hint

**Processing:**
- Spinner
- "Processing..." text

**Results:**
- Datetime
- Amount (large)
- Reason
- Original text (smaller)
- Done button

Jetpack Compose, Material 3, minimal styling.

## Project Structure

```
app/src/main/
├── java/com/example/financetracker/
│   ├── MainActivity.kt
│   ├── speech/
│   │   ├── SpeechToText.kt
│   │   └── AndroidSpeechRecognizer.kt
│   ├── extraction/
│   │   ├── ExpenseExtractor.kt
│   │   └── LlamaCppExtractor.kt
│   └── model/
│       └── Expense.kt
├── assets/
│   └── phi-3-mini.gguf
└── res/
```

## Dependencies

```kotlin
dependencies {
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.material3:material3:1.2.0")
    // llama.cpp bindings
}
```

## Permissions

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

## Future Enhancements

- Swap SpeechRecognizer for Whisper (offline, better accuracy)
- Swap llama.cpp for Gemini Nano (when available on device)
- Persist expenses locally
- Google Sheets upload
