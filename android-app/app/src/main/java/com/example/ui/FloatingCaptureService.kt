package com.example.ui

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.database.CaptureDatabase
import com.example.data.database.CaptureEntity
import com.example.data.network.CapturePayload
import com.example.data.network.CaptureSettings
import com.example.data.network.VercelApiClient
import com.example.data.repository.SettingsRepository
import com.example.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    init {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
    }

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry

    fun onCreate() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun onStart() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onStop() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}

class FloatingCaptureService : Service() {
    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private val lifecycleOwner = FloatingLifecycleOwner()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        lifecycleOwner.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val text = intent?.getStringExtra("text") ?: ""
        val url = intent?.getStringExtra("url") ?: ""
        val title = intent?.getStringExtra("title") ?: ""

        lifecycleOwner.onStart()
        showOverlay(text, url, title)
        return START_NOT_STICKY
    }

    private fun showOverlay(sharedText: String, sharedUrl: String, sharedTitle: String) {
        if (composeView != null) {
            try {
                windowManager.removeView(composeView)
            } catch (e: Exception) {
                // View might not be attached
            }
        }

        // Configure Layout Parameters for System Overlay Dialog
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = 0
            y = 0
            // Constrain layout width to look like a floating dialog box
            width = (resources.displayMetrics.widthPixels * 0.9f).toInt()
        }

        val view = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                MyApplicationTheme {
                    FloatingCaptureCard(
                        initialText = sharedText,
                        initialUrl = sharedUrl,
                        initialTitle = sharedTitle,
                        onDrag = { dx, dy ->
                            layoutParams.x += dx.toInt()
                            layoutParams.y += dy.toInt()
                            try {
                                windowManager.updateViewLayout(this@apply, layoutParams)
                            } catch (e: Exception) {
                                // Ignore layout update issues
                            }
                        },
                        onDismiss = {
                            stopSelf()
                        },
                        context = this@FloatingCaptureService
                    )
                }
            }
        }
        composeView = view
        try {
            windowManager.addView(view, layoutParams)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to create overlay window: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    override fun onDestroy() {
        lifecycleOwner.onStop()
        lifecycleOwner.onDestroy()
        if (composeView != null) {
            try {
                windowManager.removeView(composeView)
            } catch (e: Exception) {
                // View might not be attached
            }
            composeView = null
        }
        super.onDestroy()
    }
}

@Composable
fun FloatingCaptureCard(
    initialText: String,
    initialUrl: String,
    initialTitle: String,
    onDrag: (Float, Float) -> Unit,
    onDismiss: () -> Unit,
    context: Context
) {
    var textInput by remember { mutableStateOf(initialText) }
    var urlInput by remember { mutableStateOf(initialUrl) }
    var titleInput by remember { mutableStateOf(initialTitle) }
    var remarksInput by remember { mutableStateOf("") }

    var isSyncing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // 85% opacity slate grey container background with indigo borders to look translucent
    Card(
        colors = CardDefaults.cardColors(containerColor = CosmicSurface.copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, CosmicPrimary.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Drag handle / Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x, dragAmount.y)
                        }
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag to reposition overlay",
                    tint = CosmicTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FACTBOOK FLOATING DECK",
                        color = CosmicSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Capture clipping directly",
                        color = CosmicTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss window",
                        tint = CosmicTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Raw Text Input Box
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = { Text("Highlighted citation body", color = CosmicTextSecondary, fontSize = 11.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp, max = 150.dp),
                maxLines = 6,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = CosmicTextPrimary,
                    unfocusedTextColor = CosmicTextPrimary,
                    focusedBorderColor = CosmicPrimary,
                    unfocusedBorderColor = CosmicBorder,
                    focusedContainerColor = CosmicSurfaceVariant.copy(alpha = 0.8f),
                    unfocusedContainerColor = CosmicSurfaceVariant.copy(alpha = 0.8f)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Title & URL inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    placeholder = { Text("Publisher/Dawn", color = CosmicTextMuted, fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CosmicTextPrimary,
                        unfocusedTextColor = CosmicTextPrimary,
                        focusedBorderColor = CosmicPrimary,
                        unfocusedBorderColor = CosmicBorder,
                        focusedContainerColor = CosmicSurfaceVariant.copy(alpha = 0.8f),
                        unfocusedContainerColor = CosmicSurfaceVariant.copy(alpha = 0.8f)
                    )
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = { Text("Source Link", color = CosmicTextMuted, fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CosmicTextPrimary,
                        unfocusedTextColor = CosmicTextPrimary,
                        focusedBorderColor = CosmicPrimary,
                        unfocusedBorderColor = CosmicBorder,
                        focusedContainerColor = CosmicSurfaceVariant.copy(alpha = 0.8f),
                        unfocusedContainerColor = CosmicSurfaceVariant.copy(alpha = 0.8f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Remarks field
            OutlinedTextField(
                value = remarksInput,
                onValueChange = { remarksInput = it },
                placeholder = { Text("Student study tags / remarks (Optional)", color = CosmicTextMuted, fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = CosmicTextPrimary,
                    unfocusedTextColor = CosmicTextPrimary,
                    focusedBorderColor = CosmicPrimary,
                    unfocusedBorderColor = CosmicBorder,
                    focusedContainerColor = CosmicSurfaceVariant.copy(alpha = 0.8f),
                    unfocusedContainerColor = CosmicSurfaceVariant.copy(alpha = 0.8f)
                )
            )

            // Status message
            statusMessage?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = msg,
                    color = if (msg.startsWith("Success")) CosmicSuccess else CosmicOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, CosmicBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicTextSecondary),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text("Cancel", fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        isSyncing = true
                        statusMessage = "Connecting..."
                        coroutineScope.launch {
                            try {
                                val database = CaptureDatabase.getDatabase(context)
                                val captureDao = database.captureDao()
                                val settingsRepository = SettingsRepository(context)

                                val vercelUrl = settingsRepository.vercelApiUrlFlow.first()
                                val geminiKey = settingsRepository.geminiApiKeyFlow.first()
                                val targetGoogleDocId = settingsRepository.targetGoogleDocIdFlow.first()
                                val serviceAccountJson = settingsRepository.serviceAccountJsonFlow.first()
                                val customGeminiPrompt = settingsRepository.customGeminiPromptFlow.first()

                                val sanitizedUrl = VercelApiClient.sanitizeUrl(vercelUrl)
                                val keysList = if (geminiKey.isNotEmpty()) listOf(geminiKey) else emptyList()

                                // Register clipping locally
                                val captureEntity = CaptureEntity(
                                    text = textInput,
                                    sourceUrl = urlInput,
                                    sourceTitle = titleInput,
                                    remarks = remarksInput,
                                    isSynced = false
                                )

                                val localId = withContext(Dispatchers.IO) {
                                    captureDao.insertCapture(captureEntity)
                                }

                                val payload = CapturePayload(
                                    text = textInput,
                                    sourceUrl = urlInput,
                                    sourceTitle = titleInput,
                                    context = remarksInput,
                                    settings = CaptureSettings(
                                        model = "gemini-3.5-flash",
                                        geminiKeys = keysList,
                                        googleDocId = targetGoogleDocId,
                                        serviceAccount = serviceAccountJson,
                                        customPrompt = customGeminiPrompt
                                    )
                                )

                                val response = withContext(Dispatchers.IO) {
                                    VercelApiClient.service.syncCapture(sanitizedUrl, payload)
                                }

                                withContext(Dispatchers.Main) {
                                    if (response.isSuccessful && response.body()?.success == true) {
                                        Toast.makeText(context, "Evidence clipping synchronized!", Toast.LENGTH_SHORT).show()
                                        // Update local DB status to synced
                                        withContext(Dispatchers.IO) {
                                            captureDao.updateCapture(
                                                captureEntity.copy(id = localId.toInt(), isSynced = true)
                                            )
                                        }
                                        onDismiss()
                                    } else {
                                        val errBody = response.errorBody()?.string() ?: ""
                                        val errMsg = response.body()?.message ?: "Sync rejected"
                                        statusMessage = "Sync failed (${response.code()}). Added to queue."
                                        Toast.makeText(context, "Sync failed: $errMsg", Toast.LENGTH_LONG).show()

                                        withContext(Dispatchers.IO) {
                                            captureDao.updateCapture(
                                                captureEntity.copy(
                                                    id = localId.toInt(),
                                                    errorMessage = "HTTP ${response.code()}: $errMsg"
                                                )
                                            )
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    val err = e.localizedMessage ?: "Network error"
                                    statusMessage = "Connection error. Added to local queue."
                                    Toast.makeText(context, "Offline: Saved to backlog.", Toast.LENGTH_LONG).show()
                                }
                            } finally {
                                isSyncing = false
                            }
                        }
                    },
                    enabled = !isSyncing && textInput.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary),
                    modifier = Modifier.weight(2f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = CosmicTextPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Sync",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Structure & Index", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
