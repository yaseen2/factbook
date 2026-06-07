package com.example.data.network

data class CapturePayload(
    val text: String,
    val sourceUrl: String,
    val sourceTitle: String,
    val context: String,
    val settings: CaptureSettings
)

data class CaptureSettings(
    val model: String = "gemini-3.5-flash",
    val geminiKeys: List<String>,
    val googleDocId: String,
    val serviceAccount: String,
    val customPrompt: String
)

data class CaptureResponse(
    val success: Boolean,
    val message: String?,
    val docUrl: String? = null
)
