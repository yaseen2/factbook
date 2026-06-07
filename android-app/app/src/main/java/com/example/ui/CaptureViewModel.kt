package com.example.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.database.CaptureDatabase
import com.example.data.database.CaptureEntity
import com.example.data.network.CapturePayload
import com.example.data.network.CaptureSettings
import com.example.data.network.VercelApiClient
import com.example.data.repository.SettingsRepository
import com.example.data.sync.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppLog(
    val timestamp: String,
    val level: String, // "INFO", "SUCCESS", "ERROR"
    val message: String
)

class CaptureViewModel(private val context: Context) : ViewModel() {

    private val database = CaptureDatabase.getDatabase(context)
    private val captureDao = database.captureDao()
    private val settingsRepository = SettingsRepository(context)

    // Form inputs state
    val captureTextState = MutableStateFlow("")
    val captureUrlState = MutableStateFlow("")
    val captureTitleState = MutableStateFlow("")
    val captureRemarksState = MutableStateFlow("")

    // Sync state
    val isSyncing = MutableStateFlow(false)
    val syncStatusMessage = MutableStateFlow<String?>(null)

    // Log Console flow
    val appLogs = MutableStateFlow<List<AppLog>>(emptyList())

    // Secure Settings states
    val vercelApiUrl = settingsRepository.vercelApiUrlFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "https://your-app.vercel.app/api/capture"
    )

    val geminiApiKey = settingsRepository.geminiApiKeyFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val targetGoogleDocId = settingsRepository.targetGoogleDocIdFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val serviceAccountJson = settingsRepository.serviceAccountJsonFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val customGeminiPrompt = settingsRepository.customGeminiPromptFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    // Room DB History flow
    val captureHistory = captureDao.getAllCaptures().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Server health state
    val isVercelHealthy = MutableStateFlow<Boolean?>(null)

    init {
        log("INFO", "Factbook workstation initialized. Ready for clippings capture.")
        checkVercelHealth()
    }

    fun log(level: String, message: String) {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val timestamp = sdf.format(Date())
        val logEntry = AppLog(timestamp, level, message)
        val current = appLogs.value.toMutableList()
        current.add(logEntry)
        if (current.size > 150) {
            current.removeAt(0)
        }
        appLogs.value = current
    }

    fun clearLogs() {
        appLogs.value = emptyList()
        log("INFO", "Logs console cleared.")
    }

    fun updateFormFields(text: String, url: String, title: String, remarks: String) {
        captureTextState.value = text
        captureUrlState.value = url
        captureTitleState.value = title
        captureRemarksState.value = remarks
    }

    fun clearForm() {
        captureTextState.value = ""
        captureUrlState.value = ""
        captureTitleState.value = ""
        captureRemarksState.value = ""
        log("INFO", "Form input fields cleared.")
    }

    fun checkVercelHealth() {
        viewModelScope.launch {
            val isOnline = isNetworkAvailable()
            val targetUrl = VercelApiClient.sanitizeUrl(vercelApiUrl.value)
            log("INFO", "Performing connectivity probe. Target synchronized URL: $targetUrl")
            if (!isOnline) {
                isVercelHealthy.value = false
                log("ERROR", "No active internet hardware or network connection available.")
                return@launch
            }
            try {
                isVercelHealthy.value = true
                log("SUCCESS", "Network is up. Live sync target set to $targetUrl")
            } catch (e: Exception) {
                isVercelHealthy.value = false
                log("ERROR", "Connection validation error: ${e.localizedMessage}")
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Save settings
    fun saveSettings(vercelUrl: String, geminiKey: String, docId: String, serviceAccount: String, customPrompt: String) {
        viewModelScope.launch {
            log("INFO", "Saving workstation parameter changes. New target URL: $vercelUrl")
            settingsRepository.saveSettings(vercelUrl, geminiKey, docId, serviceAccount, customPrompt)
            checkVercelHealth()
        }
    }

    // Direct configuration load via settings hash
    fun importSettingsHash(hash: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            log("INFO", "Attempting settings parameters extraction from profile hash...")
            val success = settingsRepository.importFromSyncHash(hash)
            if (success) {
                log("SUCCESS", "Profile hash successfully verified and written.")
                checkVercelHealth()
                onSuccess()
            } else {
                log("ERROR", "Profile settings verification failed. Token hash contains syntax errors.")
                onError()
            }
        }
    }

    // Synchronize Card trigger
    fun requestCardSync(onComplete: (Boolean) -> Unit = {}) {
        val rawText = captureTextState.value
        val rawUrl = captureUrlState.value
        val rawTitle = captureTitleState.value
        val rawRemarks = captureRemarksState.value

        if (rawText.isEmpty()) {
            syncStatusMessage.value = "Error: Clipping source text cannot be empty."
            log("ERROR", "Refused Sync: Raw clipping body is completely empty.")
            onComplete(false)
            return
        }

        viewModelScope.launch {
            isSyncing.value = true
            syncStatusMessage.value = "Processing and preparing payload..."

            val activeVercelUrl = vercelApiUrl.value
            val sanitizedUrl = VercelApiClient.sanitizeUrl(activeVercelUrl)
            val activeGeminiKey = geminiApiKey.value
            val activeDocId = targetGoogleDocId.value
            val activeServiceAccount = serviceAccountJson.value
            val activePrompt = customGeminiPrompt.value

            val keysList = if (activeGeminiKey.isNotEmpty()) listOf(activeGeminiKey) else emptyList()

            log("INFO", "Initializing live sync submission. POST URL: $sanitizedUrl")
            log("INFO", "Doc Target ID: $activeDocId")
            log("INFO", "Gemini API Keys defined size: ${keysList.size}")

            val serviceAccountSafeMask = if (activeServiceAccount.length > 50) "${activeServiceAccount.take(50)}... [MASKED]" else "[EMPTY]"
            log("INFO", "Service Account JSON Status: $serviceAccountSafeMask")

            // Construct new capture entry
            val captureEntity = CaptureEntity(
                text = rawText,
                sourceUrl = rawUrl,
                sourceTitle = rawTitle,
                remarks = rawRemarks,
                isSynced = false
            )

            // Insert locally first to register in history database
            val localId = withContext(Dispatchers.IO) {
                captureDao.insertCapture(captureEntity)
            }
            log("INFO", "Clipping registered in local DB history under reference ID: $localId")

            val hasInternet = isNetworkAvailable()
            if (!hasInternet) {
                syncStatusMessage.value = "Device Offline. Captured Card saved to local backlog sync queue."
                log("WARN", "Device is completely offline. Added to background retry worker.")
                isWorkerSyncScheduled()
                isSyncing.value = false
                clearForm()
                onComplete(true)
                return@launch
            }

            try {
                val payload = CapturePayload(
                    text = rawText,
                    sourceUrl = rawUrl,
                    sourceTitle = rawTitle,
                    context = rawRemarks,
                    settings = CaptureSettings(
                        model = "gemini-3.5-flash",
                        geminiKeys = keysList,
                        googleDocId = activeDocId,
                        serviceAccount = activeServiceAccount,
                        customPrompt = activePrompt
                    )
                )

                log("INFO", "Sending JSON POST package to backend...")
                val response = VercelApiClient.service.syncCapture(sanitizedUrl, payload)
                val responseCode = response.code()
                log("INFO", "Response HTTP Status Code: $responseCode")

                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()
                    val docMsg = if (body?.docUrl != null) "Synced successfully: Google Doc updated!" else "Synced successfully!"
                    syncStatusMessage.value = docMsg
                    log("SUCCESS", "Vercel accepted the capture! Server Message: ${body?.message}. URL target Doc updated: ${body?.docUrl ?: "N/A"}")

                    // Update room record as synchronized
                    withContext(Dispatchers.IO) {
                        captureDao.updateCapture(
                            captureEntity.copy(id = localId.toInt(), isSynced = true, errorMessage = null)
                        )
                    }
                    clearForm()
                    onComplete(true)
                } else {
                    val genericErrorBodyString = response.errorBody()?.string() ?: ""
                    val serverErrMsg = response.body()?.message ?: genericErrorBodyString.take(800)
                    val fullVercelMessage = if (serverErrMsg.isNotEmpty()) serverErrMsg else "Unspecified reject"
                    
                    val failureMsg = "Vercel Sync failed: Status ${responseCode}. Card added to local sync queue."
                    syncStatusMessage.value = failureMsg
                    log("ERROR", "Vercel sync rejected! Code: $responseCode. Server Body output:\n$genericErrorBodyString")

                    withContext(Dispatchers.IO) {
                        captureDao.updateCapture(
                            captureEntity.copy(id = localId.toInt(), errorMessage = "Sync rejected (Status $responseCode): ${fullVercelMessage.take(200)}")
                        )
                    }
                    isWorkerSyncScheduled()
                    onComplete(true) // Saved locally
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Unknown server response"
                val failureMsg = "Connection/Network Error: $errorMsg. Card added to local sync queue."
                syncStatusMessage.value = failureMsg
                log("ERROR", "Network Exception occurred during transmission: $errorMsg")

                withContext(Dispatchers.IO) {
                    captureDao.updateCapture(
                        captureEntity.copy(id = localId.toInt(), errorMessage = "Network offline: $errorMsg")
                    )
                }
                isWorkerSyncScheduled()
                onComplete(true) // Saved locally
            } finally {
                isSyncing.value = false
                checkVercelHealth()
            }
        }
    }

    // Force retro-sync for a single pending queue item
    fun forceRetrySingleSync(capture: CaptureEntity) {
        viewModelScope.launch {
            isSyncing.value = true
            syncStatusMessage.value = "Retrying sync for card ID: ${capture.id}..."
            log("INFO", "Manual sync retry triggered for backlog ID: ${capture.id}")

            val activeVercelUrl = vercelApiUrl.value
            val sanitizedUrl = VercelApiClient.sanitizeUrl(activeVercelUrl)
            val activeGeminiKey = geminiApiKey.value
            val activeDocId = targetGoogleDocId.value
            val activeServiceAccount = serviceAccountJson.value
            val activePrompt = customGeminiPrompt.value
            val keysList = if (activeGeminiKey.isNotEmpty()) listOf(activeGeminiKey) else emptyList()

            try {
                val payload = CapturePayload(
                    text = capture.text,
                    sourceUrl = capture.sourceUrl,
                    sourceTitle = capture.sourceTitle,
                    context = capture.remarks,
                    settings = CaptureSettings(
                        model = "gemini-3.5-flash",
                        geminiKeys = keysList,
                        googleDocId = activeDocId,
                        serviceAccount = activeServiceAccount,
                        customPrompt = activePrompt
                    )
                )

                log("INFO", "Sending retry post to: $sanitizedUrl")
                val response = VercelApiClient.service.syncCapture(sanitizedUrl, payload)
                val responseCode = response.code()
                log("INFO", "Retry response status: $responseCode")

                if (response.isSuccessful && response.body()?.success == true) {
                    syncStatusMessage.value = "ID ${capture.id} Synced beautifully!"
                    log("SUCCESS", "Card ID ${capture.id} synced on retry! Server Msg: ${response.body()?.message}")
                    withContext(Dispatchers.IO) {
                        captureDao.updateCapture(
                            capture.copy(isSynced = true, errorMessage = null)
                        )
                    }
                } else {
                    val genericErrorBodyString = response.errorBody()?.string() ?: ""
                    val serverErrMsg = response.body()?.message ?: genericErrorBodyString.take(800)
                    syncStatusMessage.value = "ID ${capture.id} failed: Status $responseCode"
                    log("ERROR", "Retry sync rejected with code: $responseCode. Server Body:\n$genericErrorBodyString")
                    
                    withContext(Dispatchers.IO) {
                        captureDao.updateCapture(
                            capture.copy(errorMessage = "Rejected (Status $responseCode): ${serverErrMsg.take(200)}")
                        )
                    }
                }
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "Unknown error"
                syncStatusMessage.value = "ID ${capture.id} network error: $err"
                log("ERROR", "Retry sync met network/TCP exception for ID ${capture.id}: $err")
                withContext(Dispatchers.IO) {
                    captureDao.updateCapture(
                        capture.copy(errorMessage = "Network Exception: $err")
                    )
                }
            } finally {
                isSyncing.value = false
                checkVercelHealth()
            }
        }
    }

    // Delete single capture record
    fun deleteCapture(capture: CaptureEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            log("INFO", "Deleting clipping file record with local reference ID: ${capture.id}")
            captureDao.deleteCapture(capture)
        }
    }

    // Schedule background WorkManager to complete sync when online
    private fun isWorkerSyncScheduled() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(syncWorkRequest)
        log("INFO", "WorkManager background sync task scheduled to retry on connection recovery.")
    }
}

class CaptureViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CaptureViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CaptureViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

