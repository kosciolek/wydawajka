package com.example.financetracker.speech

interface SpeechToText {
    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        silenceTimeoutMs: Long = 5000
    )
    fun stopListening()
    fun destroy()
}
