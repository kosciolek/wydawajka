# Android App Specs — Księgowy Krabuś

## Overview

**App Name:** Księgowy Krabuś (The Accountant Crab)
**Package:** `com.example.financetracker`
**Language:** Kotlin, Jetpack Compose
**Min SDK:** 33 · **Target SDK:** 36
**Architecture:** Single-activity (MainActivity) with composable UI

A Polish-language voice-first expense tracker. The user dictates a spending (e.g. "23 zł fryzjer"), which gets timestamped and uploaded to a Cloudflare Worker backed by D1.

## Features

1. **Voice recording** — Uses Android `SpeechRecognizer` with Polish (`pl-PL`). Configurable silence timeout (2–10 s, default 5).
2. **Manual text input** — Fallback text field on the idle screen.
3. **Cloudflare Worker sync** — Uploads expenses via `POST /create`, deletes via `DELETE /expense/:uuid`, uses bearer token auth.
4. **Spending summary** — Fetched from `GET /summary` on resume; shows totals for today, 7 days, 30 days.
5. **Dictate shortcut** — A second launcher icon ("Księgowy Krabuś (Dyktuj)") that opens straight into recording mode. Works from lock screen.

## Data Model

```kotlin
data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val datetime: Instant,
    val text: String       // e.g. "23 zł fryzjer"
)

data class SpendingSummary(
    val today: Double,
    val last7Days: Double,
    val last30Days: Double
)
```

## Cloudflare Worker Integration

- **Auth:** Bearer token stored in DataStore preferences.
- **Write:** `POST /create` with `{ uuid, text, timestamp }`. Upserts on conflict.
- **Delete:** `DELETE /expense/:uuid`.
- **Summary:** `GET /summary` returns `{ today, last7Days, last30Days }`.
- **HTTP client:** `java.net.HttpURLConnection` (no external dependencies).

## Local Storage

Jetpack DataStore Preferences, keys:

| Key | Type | Purpose |
|-----|------|---------|
| `worker_url` | String | Cloudflare Worker URL |
| `api_token` | String | Bearer token for worker auth |
| `speech_timeout_seconds` | Int | Silence timeout (default 5) |

## Permissions

| Permission | Purpose |
|------------|---------|
| `RECORD_AUDIO` | Voice input |
| `INTERNET` | Sheets API |
| `READ_EXTERNAL_STORAGE` (≤ SDK 32) | Legacy media access |
| `READ_MEDIA_AUDIO/IMAGES/VIDEO` | Granular media access (SDK 33+) |

## UI Screens & States

All UI is Jetpack Compose, Material 3, dark theme (`#121212` background, `#1E1E1E` surface, green accent `rgb(76,175,80)`).

### App State Machine

```
Idle → RequestingPermission → Recording → Processing → Displaying
                                  ↓                        ↓
                                Error                    Error
```

### Screens

1. **Idle** — Crab mascot greeting ("Cześć!"), "Nagraj wydatek" button, manual text field.
2. **Recording** — Animated pulsing crab, "Słucham!" message, tap-to-stop.
3. **Processing** — Crab with "Myślę..." spinner while uploading.
4. **Result (Displaying)** — Shows recorded text + timestamp, upload status. Actions: Usuń (delete), Powtórz (repeat), Nagraj kolejny (record next).
5. **Error** — Context-aware Polish messages (no audio, no permission, no internet, generic). Retry/close buttons.
6. **Settings** — Timeout slider, Worker URL field, API Token field, Save button with result feedback.

### Navigation

Two screens: `Main` and `Settings`. Settings accessible via top-right button.

## Manifest Highlights

- `showOnLockScreen=true`, `turnScreenOn=true` on MainActivity.
- `DictateShortcut` activity-alias with intent action `com.example.financetracker.DICTATE`.
- BroadcastReceiver stops recording if screen turns off.

## Build

- **Gradle Plugin:** 8.13.0
- **Kotlin:** 2.2.20
- **Compose BOM:** 2024.02.00
- **Java:** 17

Key dependencies: `activity-compose`, `lifecycle-viewmodel-compose`, `material3`, `datastore-preferences`.

## Source Layout

```
app/src/main/java/com/example/financetracker/
├── MainActivity.kt              # All UI composables + state management (~780 lines)
├── model/
│   ├── Expense.kt
│   └── SpendingSummary.kt
├── speech/
│   ├── SpeechToText.kt          # Interface
│   └── AndroidSpeechRecognizer.kt  # Android impl (Polish, configurable timeout)
├── api/
│   └── CloudflareWorkerService.kt  # Worker HTTP client
└── settings/
    ├── SettingsRepository.kt    # DataStore preferences
    └── SettingsScreen.kt        # Settings UI composable

res/
├── drawable/crab_assistant.png  # Mascot image
├── mipmap/                      # Launcher icons (all DPIs)
└── values/                      # strings, colors, themes
```
