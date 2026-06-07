package com.example.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.CaptureDatabase
import com.example.data.network.CapturePayload
import com.example.data.network.CaptureSettings
import com.example.data.network.VercelApiClient
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.e("SyncWorker", "Iniciating backup sync workflow...")
        val database = CaptureDatabase.getDatabase(applicationContext)
        val captureDao = database.captureDao()
        val settingsRepository = SettingsRepository(applicationContext)

        val unsyncedList = captureDao.getUnsyncedCaptures()
        if (unsyncedList.isEmpty()) {
            return Result.success()
        }

        val url = settingsRepository.vercelApiUrlFlow.first()
        val geminiKey = settingsRepository.geminiApiKeyFlow.first()
        val docId = settingsRepository.targetGoogleDocIdFlow.first()
        val serviceAccount = settingsRepository.serviceAccountJsonFlow.first()
        val customPrompt = settingsRepository.customGeminiPromptFlow.first()

        val geminiKeysList = if (geminiKey.isNotEmpty()) listOf(geminiKey) else emptyList()

        var hasFailures = false

        for (capture in unsyncedList) {
            try {
                val payload = CapturePayload(
                    text = capture.text,
                    sourceUrl = capture.sourceUrl,
                    sourceTitle = capture.sourceTitle,
                    context = capture.remarks,
                    settings = CaptureSettings(
                        model = "gemini-3.5-flash",
                        geminiKeys = geminiKeysList,
                        googleDocId = docId,
                        serviceAccount = serviceAccount,
                        customPrompt = customPrompt
                    )
                )

                val response = VercelApiClient.service.syncCapture(VercelApiClient.sanitizeUrl(url), payload)
                if (response.isSuccessful && response.body()?.success == true) {
                    captureDao.updateCapture(
                        capture.copy(isSynced = true, errorMessage = null)
                    )
                } else {
                    hasFailures = true
                    val err = response.body()?.message ?: response.errorBody()?.string() ?: "API Refused integration"
                    captureDao.updateCapture(
                        capture.copy(errorMessage = "Refused: $err")
                    )
                }
            } catch (e: Exception) {
                hasFailures = true
                captureDao.updateCapture(
                    capture.copy(errorMessage = "Failed: ${e.localizedMessage}")
                )
            }
        }

        return if (hasFailures) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}
