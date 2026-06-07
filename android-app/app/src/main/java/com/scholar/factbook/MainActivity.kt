package com.scholar.factbook

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class MainActivity : ComponentActivity() {
    private val client = OkHttpClient()
    private val offlineFactStore by lazy { OfflineFactStore(this) }
    private val offlineFactsState = mutableStateOf<List<OfflineFact>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load saved Vercel URL from SharedPreferences
        val sharedPref = getSharedPreferences("factbook_prefs", Context.MODE_PRIVATE)
        val savedVercelUrl = sharedPref.getString("vercel_url", "") ?: ""

        offlineFactsState.value = offlineFactStore.getFacts()

        setContent {
            FactbookTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    var isSyncing by remember { mutableStateOf(false) }

                    MainScreen(
                        initialVercelUrl = savedVercelUrl,
                        offlineFacts = offlineFactsState.value,
                        isOnline = isNetworkAvailable(),
                        isSyncing = isSyncing,
                        onSaveVercelUrl = { url ->
                            sharedPref.edit().putString("vercel_url", url).apply()
                            Toast.makeText(this, "Vercel Workspace URL Saved", Toast.LENGTH_SHORT).show()
                        },
                        onSubmit = { vercelUrl, text, sourceUrl, sourceTitle, contextRemarks, onFinish ->
                            submitCapture(vercelUrl, text, sourceUrl, sourceTitle, contextRemarks,
                                onSuccess = {
                                    onFinish()
                                },
                                onFailure = { error ->
                                    onFinish()
                                }
                            )
                        },
                        onSyncClick = {
                            isSyncing = true
                            syncOfflineFacts(savedVercelUrl,
                                onSuccess = {
                                    isSyncing = false
                                    Toast.makeText(this, "Sync Complete: All facts uploaded!", Toast.LENGTH_LONG).show()
                                },
                                onFailure = { errorMsg ->
                                    isSyncing = false
                                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh offline queue facts list when app is focused/resumed
        offlineFactsState.value = offlineFactStore.getFacts()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private fun getErrorMessage(responseBody: String?): String {
        if (responseBody.isNullOrEmpty()) return "Server error (empty response)"
        return try {
            val json = com.google.gson.JsonParser.parseString(responseBody).asJsonObject
            if (json.has("error")) {
                json.get("error").asString
            } else {
                responseBody
            }
        } catch (e: Exception) {
            responseBody
        }
    }

    private fun submitCapture(
        vercelUrl: String,
        text: String,
        sourceUrl: String,
        sourceTitle: String,
        contextRemarks: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val cleanUrl = vercelUrl.trim().removeSuffix("/")
        if (cleanUrl.isEmpty() || !cleanUrl.startsWith("http")) {
            Toast.makeText(this, "Please configure a valid Vercel deployment URL", Toast.LENGTH_LONG).show()
            onFailure("Invalid URL")
            return
        }

        val factId = System.currentTimeMillis().toString()

        if (!isNetworkAvailable()) {
            val offlineFact = OfflineFact(
                id = factId,
                text = text,
                sourceUrl = sourceUrl,
                sourceTitle = sourceTitle,
                context = contextRemarks
            )
            offlineFactStore.addFact(offlineFact)
            offlineFactsState.value = offlineFactStore.getFacts()
            Toast.makeText(this, "Offline Mode: Saved to capture queue", Toast.LENGTH_LONG).show()
            onSuccess()
            return
        }

        val endpoint = "$cleanUrl/api/capture"

        val json = JsonObject().apply {
            addProperty("id", factId) // Add ID for deduplication on server side
            addProperty("text", text)
            if (sourceUrl.isNotEmpty()) addProperty("sourceUrl", sourceUrl)
            if (sourceTitle.isNotEmpty()) addProperty("sourceTitle", sourceTitle)
            if (contextRemarks.isNotEmpty()) addProperty("context", contextRemarks)
        }

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "Evidence Synchronized Successfully!", Toast.LENGTH_LONG).show()
                        onSuccess()
                    } else {
                        val serverError = getErrorMessage(responseBody)
                        Toast.makeText(this@MainActivity, "Sync Error: $serverError. Saved to offline queue.", Toast.LENGTH_LONG).show()
                        val offlineFact = OfflineFact(
                            id = factId,
                            text = text,
                            sourceUrl = sourceUrl,
                            sourceTitle = sourceTitle,
                            context = contextRemarks
                        )
                        offlineFactStore.addFact(offlineFact)
                        offlineFactsState.value = offlineFactStore.getFacts()
                        onSuccess()
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Connection failure. Saved to queue.", Toast.LENGTH_LONG).show()
                    val offlineFact = OfflineFact(
                        id = factId,
                        text = text,
                        sourceUrl = sourceUrl,
                        sourceTitle = sourceTitle,
                        context = contextRemarks
                    )
                    offlineFactStore.addFact(offlineFact)
                    offlineFactsState.value = offlineFactStore.getFacts()
                    onSuccess()
                }
            }
        }
    }

    private fun syncOfflineFacts(
        vercelUrl: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val cleanUrl = vercelUrl.trim().removeSuffix("/")
        if (cleanUrl.isEmpty() || !cleanUrl.startsWith("http")) {
            onFailure("Set Vercel URL in settings first")
            return
        }

        val facts = offlineFactStore.getFacts()
        if (facts.isEmpty()) {
            onSuccess()
            return
        }

        if (!isNetworkAvailable()) {
            onFailure("Internet connection unavailable")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            var successCount = 0
            var failCount = 0
            var lastError = ""

            for (fact in facts) {
                // Introduce a 2-second rate-limiting delay between requests
                kotlinx.coroutines.delay(2000)

                val endpoint = "$cleanUrl/api/capture"
                val json = JsonObject().apply {
                    addProperty("id", fact.id) // Send current fact ID for server-side deduplication check
                    addProperty("text", fact.text)
                    if (fact.sourceUrl.isNotEmpty()) addProperty("sourceUrl", fact.sourceUrl)
                    if (fact.sourceTitle.isNotEmpty()) addProperty("sourceTitle", fact.sourceTitle)
                    if (fact.context.isNotEmpty()) addProperty("context", fact.context)
                }

                val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(endpoint)
                    .post(requestBody)
                    .build()

                try {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()
                    if (response.isSuccessful) {
                        offlineFactStore.removeFact(fact.id)
                        successCount++
                    } else {
                        failCount++
                        lastError = getErrorMessage(responseBody)
                    }
                } catch (e: IOException) {
                    failCount++
                    lastError = e.message ?: "Connection error"
                }
            }

            withContext(Dispatchers.Main) {
                offlineFactsState.value = offlineFactStore.getFacts()
                if (failCount == 0) {
                    onSuccess()
                } else {
                    onFailure("Uploaded $successCount. Failed $failCount. Error: $lastError")
                }
            }
        }
    }
}

@Composable
fun FactbookTheme(content: @Composable () -> Unit) {
    val darkColors = darkColorScheme(
        background = Color(0xFF070814),
        surface = Color(0xFF111322),
        primary = Color(0xFF3B82F6),
        secondary = Color(0xFF06B6D4),
        onBackground = Color(0xFFE2E8F0),
        onSurface = Color(0xFFFAFAFA)
    )
    MaterialTheme(
        colorScheme = darkColors,
        content = content
    )
}

@Composable
fun PulsatingConnectionStatus(isOnline: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .background(
                color = if (isOnline) Color(0x1F10B981) else Color(0x24F59E0B),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = if (isOnline) Color(0x3310B981) else Color(0x40F59E0B),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .graphicsLayer(alpha = alpha)
                .background(
                    color = if (isOnline) Color(0xFF10B981) else Color(0xFFF59E0B),
                    shape = RoundedCornerShape(4.dp)
                )
        )
        Text(
            text = if (isOnline) "ONLINE" else "OFFLINE",
            color = if (isOnline) Color(0xFF34D399) else Color(0xFFFBBF24),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    initialVercelUrl: String,
    offlineFacts: List<OfflineFact>,
    isOnline: Boolean,
    isSyncing: Boolean,
    onSaveVercelUrl: (String) -> Unit,
    onSubmit: (String, String, String, String, String, () -> Unit) -> Unit,
    onSyncClick: () -> Unit
) {
    var vercelUrl by remember { mutableStateOf(initialVercelUrl) }
    var textInput by remember { mutableStateOf("") }
    var sourceUrlInput by remember { mutableStateOf("") }
    var sourceTitleInput by remember { mutableStateOf("") }
    var contextInput by remember { mutableStateOf("") }

    var showSettings by remember { mutableStateOf(initialVercelUrl.isEmpty() || initialVercelUrl.contains("your-app")) }
    var isSubmitting by remember { mutableStateOf(false) }

    val deepSpaceGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF070814), Color(0xFF13152A))
    )

    val cardBg = Color(0xF20F1123)
    val cardBorder = Color(0x26FFFFFF)
    val textInputFieldBg = Color(0xFF080914)
    val focusedBorderColor = Color(0xFF3B82F6)

    val actionButtonGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(deepSpaceGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Branding Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scholar's Ledger",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    letterSpacing = (-0.5).sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PulsatingConnectionStatus(isOnline = isOnline)

                    Button(
                        onClick = { showSettings = !showSettings },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                        border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (showSettings) "Back" else "Settings",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Center scrollable workstation forms vertically
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Offline Sync Alert Card (grows dynamically)
                    if (offlineFacts.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.2.dp, Color(0x66F59E0B), RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0x1AF59E0B)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color(0xFFF59E0B), RoundedCornerShape(3.dp))
                                        )
                                        Text(
                                            text = "OFFLINE CAPTURE QUEUE",
                                            color = Color(0xFFFBBF24),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    Text(
                                        text = "${offlineFacts.size} Pending",
                                        color = Color(0xFFFBBF24),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier
                                            .background(Color(0x33F59E0B), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Text(
                                    text = "Unsynchronized clippings are cached locally. Connect online to send these items to your ledger.",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )

                                Button(
                                    onClick = onSyncClick,
                                    enabled = !isSyncing,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        disabledContainerColor = Color(0x331E1E26)
                                    ),
                                    contentPadding = PaddingValues()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                brush = if (isOnline) {
                                                    Brush.horizontalGradient(
                                                        colors = listOf(Color(0xFFD97706), Color(0xFFF59E0B))
                                                    )
                                                } else {
                                                    Brush.horizontalGradient(
                                                        colors = listOf(Color(0x33D97706), Color(0x33F59E0B))
                                                    )
                                                },
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSyncing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                text = if (isOnline) "Sync ${offlineFacts.size} Items Now" else "Device Offline (Connect to Sync)",
                                                fontWeight = FontWeight.Bold,
                                                color = if (isOnline) Color.White else Color(0xFF94A3B8),
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showSettings) {
                        // Settings Section Container
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.2.dp, cardBorder, RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "WORKSPACE ENDPOINT",
                                    color = Color(0xFF06B6D4),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )

                                OutlinedTextField(
                                    value = vercelUrl,
                                    onValueChange = { vercelUrl = it },
                                    label = { Text("Vercel App Domain Link", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                    placeholder = { Text("https://your-deployment.vercel.app") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = textInputFieldBg,
                                        unfocusedContainerColor = textInputFieldBg,
                                        focusedBorderColor = focusedBorderColor,
                                        unfocusedBorderColor = Color(0x1AFFFFFF),
                                        cursorColor = focusedBorderColor
                                    )
                                )

                                Button(
                                    onClick = {
                                        onSaveVercelUrl(vercelUrl)
                                        showSettings = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(actionButtonGradient, RoundedCornerShape(14.dp))
                                            .padding(vertical = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Save Workspace Link", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        // Capture Workstation Form
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.2.dp, cardBorder, RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "CAPTURE WORKSTATION",
                                    color = Color(0xFF3B82F6),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )

                                // Text Input
                                OutlinedTextField(
                                    value = textInput,
                                    onValueChange = { textInput = it },
                                    label = { Text("Raw Citation Clipping Text", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 120.dp),
                                    maxLines = 8,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = textInputFieldBg,
                                        unfocusedContainerColor = textInputFieldBg,
                                        focusedBorderColor = focusedBorderColor,
                                        unfocusedBorderColor = Color(0x1AFFFFFF),
                                        cursorColor = focusedBorderColor
                                    )
                                )

                                // Source Title
                                OutlinedTextField(
                                    value = sourceTitleInput,
                                    onValueChange = { sourceTitleInput = it },
                                    label = { Text("Source Publisher / Title", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = textInputFieldBg,
                                        unfocusedContainerColor = textInputFieldBg,
                                        focusedBorderColor = focusedBorderColor,
                                        unfocusedBorderColor = Color(0x1AFFFFFF),
                                        cursorColor = focusedBorderColor
                                    )
                                )

                                // Source URL
                                OutlinedTextField(
                                    value = sourceUrlInput,
                                    onValueChange = { sourceUrlInput = it },
                                    label = { Text("Source Web URL", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = textInputFieldBg,
                                        unfocusedContainerColor = textInputFieldBg,
                                        focusedBorderColor = focusedBorderColor,
                                        unfocusedBorderColor = Color(0x1AFFFFFF),
                                        cursorColor = focusedBorderColor
                                    )
                                )

                                // Context Remarks
                                OutlinedTextField(
                                    value = contextInput,
                                    onValueChange = { contextInput = it },
                                    label = { Text("Study Remarks / Tags (Optional)", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = textInputFieldBg,
                                        unfocusedContainerColor = textInputFieldBg,
                                        focusedBorderColor = focusedBorderColor,
                                        unfocusedBorderColor = Color(0x1AFFFFFF),
                                        cursorColor = focusedBorderColor
                                    )
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = {
                                        isSubmitting = true
                                        onSubmit(vercelUrl, textInput, sourceUrlInput, sourceTitleInput, contextInput) {
                                            isSubmitting = false
                                            textInput = ""
                                            sourceUrlInput = ""
                                            sourceTitleInput = ""
                                            contextInput = ""
                                        }
                                    },
                                    enabled = textInput.isNotEmpty() && !isSubmitting,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        disabledContainerColor = Color(0xFF1E1E26)
                                    ),
                                    contentPadding = PaddingValues()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                brush = if (textInput.isNotEmpty()) actionButtonGradient else Brush.horizontalGradient(listOf(Color(0xFF1E1E26), Color(0xFF1E1E26))),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .padding(vertical = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSubmitting) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                text = if (isOnline) "Index Evidence Item" else "Save Offline & Queue",
                                                fontWeight = FontWeight.Bold,
                                                color = if (textInput.isNotEmpty()) Color.White else Color.Gray,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
