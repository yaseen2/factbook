package com.example.data.repository

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "factbook_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val VERCEL_API_URL = stringPreferencesKey("vercel_api_url")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val TARGET_GOOGLE_DOC_ID = stringPreferencesKey("target_google_doc_id")
        val SERVICE_ACCOUNT_JSON = stringPreferencesKey("google_service_account_json")
        val CUSTOM_GEMINI_PROMPT = stringPreferencesKey("custom_gemini_prompt")

        const val DEFAULT_VERCEL_URL = "https://factbook-orcin.vercel.app/api/capture"
        const val DEFAULT_DOC_ID = "1RzVdVffZT1OyLBanB1GkHUgQ_r687fAv5H5AYdUxvYo"
        val DEFAULT_SERVICE_ACCOUNT = """{
  "type": "service_account",
  "project_id": "liquid-alloy-384117",
  "private_key_id": "e70526d77ad3f171e25c103e3a6142424661334e",
  "private_key": "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDaOcSAdtI3+53N\ntKTAhb7xQpwt8wRXIkGvRZvXPxK0Xe1nME7Crhd6tl7HYQKHq4IXswAUUA9XEmN9\n+eGllw8ckkJoMqvdJBNOYXL5nmKL42YPl80b6ZxF0Reh31udy6hqZPDiYKhqjbKy\nO8rQ64QtedKV1tCTRg07+NPKlibqeUP7Cq1Xrp1tFqoYLEoin+DRIXHXVhYAIuEP\nYaXPI+CLFCPR6uMGJzD4SDqDDErxew1IDmliudjhrVnXnCivYLbADYdWOEKgzy3k\n608YKJKLCtOnzDgUcawjHi9bTb17CVWNzdU56acKLhYEFmiQgOD0+pQwo8uOrTzW\n/c4IJCEBAgMBAAECggEABWIYwiqBiQwAmosdkqTkpELxDgPJVEpDYIrRC1WXpTIO\nodWukQgxCr/mRU4pUntynBm4Th1OI5G6jpL+yVJD9yp3JObgbSDEsSb06nTJGDZQ\ngDX4bfssoF7wvEzW5Q+ZufYqKGDGBsretkgvT/QKBgJ+pV+aiZIters/+Oa3Uwqw\natCollXBTe/83WmBN5dyqTOR3Btq7WMjJycNSY1ke5NOJ8VKkw59y1KpRrKEsizo\naLnPyQoOs4SqntrSk1lqkvTFzQmnM/dmd08TZfUWSHlpe2PPoncdnyNp9lAELd0x\nOFAY4FHPn05u/Pm+BU5tIhM9kxpafOZK3GOIWxg4IQKBgQD5v0YVHY+Nx1oaLsgs\ntcx8A+UdtnlYVqkWfSjdF3RiX0euzyOBU1VExAV3l9f0Ks0WDnNPPo4Q2xv6KhBE\nR3FFOcT6J9Et2iyraa0ZDrGmdojg+IKD9Q77XoCYVVR0v46fbfqSYFsIYt/lHtMM\ngaq3By25Xcp2bmMKGGLXTyc+HQKBgQDfsHXUqkjsOb9/HbGbZ3EtK659WcWQPw3Q\n9wGAuw/DsSnSKWX+hBifl4b36M8XOHAd0KMmW55c1jZ1ZRwv2/Rq0Jm33ECd3LVW\ndAgMfbj4FCI0pIbFLkHYa8Z7Mmu/F4caLLT8eej+iuYpLVr0VKax0d8RWO4xqxer\nZ8RVbCZJNQKBgCLw6Mwxe5EZb/izzNu8f6RmIxr6GZYLYdK8pgfwrR7KN8w0PMJu\nN0LMrfsialtF1bWdRfKzTMr87LgeRHJZEHbf8zgQdAqXOKKuwUH9NUuVz+axvgja\nM0Nl4FXvskY3OqwBhgoOow3UT4DNxDc+uPSsc3A4uAfDv+V8re9zqojdAoGBAIH9\npOv6LTiB8qPnWKArw9xxJwyXT7Ucv2WxNUFe87FQUjJcdnYqw7hysS9+LmKbHKRm\n63jOA0CGB82+/VteojI0mS/8odxbN1JPSaIgqY4Y/SvEdME/uWoQb56GVZEOFigC\n1QZnvhu2EjzZq6KTUu/BO7V0Da/1cmLcgX72w2UFAoGAGNohus42tBA3MjGmPDcG\nIN81GXPpQcyuVBJ9H4PDVAkJYAw//t5KC3nQCROTP5X7p9xisJqGWHcTbCbCS45s\n0VHq30ebCqDFdZgSQva6JleYJAvL6InjfRgX9lqp1zyEUdjvKXooakprsAPsmnJN\n4ZyC7hjK4ht6dp44TvEC+qg=\n-----END PRIVATE KEY-----\n",
  "client_email": "yaseen@liquid-alloy-384117.iam.gserviceaccount.com",
  "client_id": "117636711267678853363",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/yaseen%40liquid-alloy-384117.iam.gserviceaccount.com",
  "universe_domain": "googleapis.com"
}"""
    }

    val vercelApiUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        val saved = preferences[VERCEL_API_URL]
        if (saved.isNullOrBlank() || saved == "https://your-app.vercel.app/api/capture" || saved == "https://your-app.vercel.app") {
            DEFAULT_VERCEL_URL
        } else {
            saved
        }
    }

    val geminiApiKeyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[GEMINI_API_KEY] ?: try {
            // Fallback to BuildConfig if present and not empty
            if (BuildConfig.GEMINI_API_KEY.isNotEmpty() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY") {
                BuildConfig.GEMINI_API_KEY
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    val targetGoogleDocIdFlow: Flow<String> = context.dataStore.data.map { preferences ->
        val saved = preferences[TARGET_GOOGLE_DOC_ID]
        if (saved.isNullOrBlank() || saved == "YOUR_GOOGLE_DOC_ID" || saved == "your-google-doc-id" || saved == "YOUR-DOC-ID") {
            DEFAULT_DOC_ID
        } else {
            saved
        }
    }

    val serviceAccountJsonFlow: Flow<String> = context.dataStore.data.map { preferences ->
        val saved = preferences[SERVICE_ACCOUNT_JSON]
        if (saved.isNullOrBlank() || saved.trim() == "YOUR_SERVICE_ACCOUNT_JSON" || saved.length < 50) {
            DEFAULT_SERVICE_ACCOUNT
        } else {
            saved
        }
    }

    val customGeminiPromptFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CUSTOM_GEMINI_PROMPT] ?: ""
    }

    suspend fun saveSettings(
        vercelUrl: String,
        geminiKey: String,
        docId: String,
        serviceAccount: String,
        customPrompt: String
    ) {
        context.dataStore.edit { preferences ->
            preferences[VERCEL_API_URL] = vercelUrl
            preferences[GEMINI_API_KEY] = geminiKey
            preferences[TARGET_GOOGLE_DOC_ID] = docId
            preferences[SERVICE_ACCOUNT_JSON] = serviceAccount
            preferences[CUSTOM_GEMINI_PROMPT] = customPrompt
        }
    }

    suspend fun importFromSyncHash(hash: String): Boolean {
        try {
            var rawHash = hash.trim()
            if (rawHash.contains("#sync=")) {
                rawHash = rawHash.substringAfter("#sync=")
            } else if (rawHash.contains("sync=")) {
                rawHash = rawHash.substringAfter("sync=")
            }

            // Remove any trailing parameters or hashes
            rawHash = rawHash.split("&", " ", "?").first()

            val decodedBytes = Base64.decode(rawHash, Base64.DEFAULT or Base64.NO_PADDING or Base64.NO_WRAP)
            val decodedString = String(decodedBytes, Charsets.UTF_8)

            var vercelUrl = ""
            var geminiKey = ""
            var docId = ""
            var serviceAccount = ""
            var customPrompt = ""

            if (decodedString.trim().startsWith("{")) {
                val json = JSONObject(decodedString)
                vercelUrl = json.optString("vercelUrl", json.optString("vercel_api_url", ""))
                geminiKey = json.optString("geminiKey", json.optString("gemini_api_key", ""))
                docId = json.optString("googleDocId", json.optString("target_google_doc_id", json.optString("docId", "")))
                serviceAccount = json.optString("serviceAccount", json.optString("google_service_account_json", json.optString("serviceAccountJson", "")))
                customPrompt = json.optString("customPrompt", json.optString("custom_gemini_prompt", ""))
            } else {
                val pairs = decodedString.split("&")
                for (pair in pairs) {
                    val parts = pair.split("=")
                    if (parts.size == 2) {
                        val key = parts[0]
                        val value = java.net.URLDecoder.decode(parts[1], "UTF-8")
                        when (key) {
                            "vercelUrl", "vercel_api_url" -> vercelUrl = value
                            "geminiKey", "gemini_api_key" -> geminiKey = value
                            "googleDocId", "target_google_doc_id", "docId" -> docId = value
                            "serviceAccount", "google_service_account_json", "serviceAccountJson" -> serviceAccount = value
                            "customPrompt", "custom_gemini_prompt" -> customPrompt = value
                        }
                    }
                }
            }

            // Let's retrieve existing to retain if decoded fields are empty
            context.dataStore.edit { preferences ->
                if (vercelUrl.isNotEmpty()) preferences[VERCEL_API_URL] = vercelUrl
                if (geminiKey.isNotEmpty()) preferences[GEMINI_API_KEY] = geminiKey
                if (docId.isNotEmpty()) preferences[TARGET_GOOGLE_DOC_ID] = docId
                if (serviceAccount.isNotEmpty()) preferences[SERVICE_ACCOUNT_JSON] = serviceAccount
                if (customPrompt.isNotEmpty()) preferences[CUSTOM_GEMINI_PROMPT] = customPrompt
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
