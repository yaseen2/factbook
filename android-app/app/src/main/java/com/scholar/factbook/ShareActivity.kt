package com.scholar.factbook

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import java.util.regex.Pattern

class ShareActivity : ComponentActivity() {
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load saved Vercel URL from SharedPreferences
        val sharedPref = getSharedPreferences("factbook_prefs", Context.MODE_PRIVATE)
        val savedVercelUrl = sharedPref.getString("vercel_url", "") ?: ""

        if (savedVercelUrl.isEmpty()) {
            Toast.makeText(this, "Please open the main app first to configure your workspace endpoint", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Parse shared intent data
        var sharedText = ""
        var sharedUrl = ""
        var sharedTitle = ""

        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val incomingText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            if (incomingText.isNotEmpty()) {
                val parsed = extractUrlAndText(incomingText)
                sharedText = parsed.first
                sharedUrl = parsed.second
                sharedTitle = intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: ""
            }
        }

        val isOnline = isNetworkAvailable()

        setContent {
            FactbookTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // Clicking background dims the screen and cancels capture
                        .clickable { finish() }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ShareScreen(
                        isOnline = isOnline,
                        initialText = sharedText,
                        initialUrl = sharedUrl,
                        initialTitle = sharedTitle,
                        onSubmit = { text, sourceUrl, sourceTitle, contextRemarks ->
                            submitCapture(savedVercelUrl, text, sourceUrl, sourceTitle, contextRemarks)
                        },
                        onCancel = { finish() }
                    )
                }
            }
        }
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

    private fun saveOffline(
        text: String,
        sourceUrl: String,
        sourceTitle: String,
        contextRemarks: String
    ) {
        val store = OfflineFactStore(this)
        val offlineFact = OfflineFact(
            id = System.currentTimeMillis().toString(),
            text = text,
            sourceUrl = sourceUrl,
            sourceTitle = sourceTitle,
            context = contextRemarks
        )
        store.addFact(offlineFact)
        Toast.makeText(this, "Network Offline. Saved to local queue!", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun extractUrlAndText(text: String): Pair<String, String> {
        val urlRegex = "https?://[a-zA-Z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=]+"
        val pattern = Pattern.compile(urlRegex)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            val url = matcher.group()
            val cleanText = text.replace(url, "").trim()
            return Pair(cleanText, url)
        }
        return Pair(text, "")
    }

    private fun submitCapture(
        vercelUrl: String,
        text: String,
        sourceUrl: String,
        sourceTitle: String,
        contextRemarks: String
    ) {
        if (!isNetworkAvailable()) {
            saveOffline(text, sourceUrl, sourceTitle, contextRemarks)
            return
        }

        val cleanUrl = vercelUrl.trim().removeSuffix("/")
        val endpoint = "$cleanUrl/api/capture"

        val json = JsonObject().apply {
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
                val responseBody = response.body?.string() ?: ""

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ShareActivity, "Evidence Synchronized Successfully!", Toast.LENGTH_LONG).show()
                        finish() // Close overlay activity on success
                    } else {
                        Toast.makeText(this@ShareActivity, "Sync failed: Server error. Saved to queue.", Toast.LENGTH_LONG).show()
                        saveOffline(text, sourceUrl, sourceTitle, contextRemarks)
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ShareActivity, "Network error. Saved to local queue.", Toast.LENGTH_LONG).show()
                    saveOffline(text, sourceUrl, sourceTitle, contextRemarks)
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusBadge(isOnline: Boolean) {
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
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(
                    color = if (isOnline) Color(0xFF10B981) else Color(0xFFF59E0B),
                    shape = RoundedCornerShape(3.dp)
                )
        )
        Text(
            text = if (isOnline) "ONLINE" else "OFFLINE",
            color = if (isOnline) Color(0xFF34D399) else Color(0xFFFBBF24),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(
    isOnline: Boolean,
    initialText: String,
    initialUrl: String,
    initialTitle: String,
    onSubmit: (String, String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var textInput by remember { mutableStateOf(initialText) }
    var sourceUrlInput by remember { mutableStateOf(initialUrl) }
    var sourceTitleInput by remember { mutableStateOf(initialTitle) }
    var contextInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val glassBg = Color(0xF20B0C18)
    val cardBorder = Color(0x33FFFFFF)
    val focusedBorderColor = Color(0xFF3B82F6)
    val textInputFieldBg = Color(0xFF121424)

    val buttonGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = false, onClick = {}) // Clicking card does not dismiss
            .border(1.2.dp, cardBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = glassBg),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FACTBOOK.ACADEMICS",
                        color = Color(0xFF3B82F6),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Quick Evidence Capture",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = (-0.5).sp
                    )
                }

                ConnectionStatusBadge(isOnline = isOnline)
            }

            // Raw citation text
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = { Text("Raw Citation Clipping Text", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 180.dp),
                maxLines = 6,
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

            // Remarks / Tags
            OutlinedTextField(
                value = contextInput,
                onValueChange = { contextInput = it },
                label = { Text("Study Remarks / Tags (Optional)", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
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

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel Button
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // Submit Button with premium background gradient
                Button(
                    onClick = {
                        isSubmitting = true
                        onSubmit(textInput, sourceUrlInput, sourceTitleInput, contextInput)
                    },
                    enabled = textInput.isNotEmpty() && !isSubmitting,
                    modifier = Modifier
                        .weight(2f),
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
                                brush = if (textInput.isNotEmpty()) buttonGradient else Brush.horizontalGradient(listOf(Color(0xFF1E1E26), Color(0xFF1E1E26))),
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
                                text = if (isOnline) "Index Evidence" else "Queue Offline",
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
