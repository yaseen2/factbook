package com.scholar.factbook

import android.content.Context
import android.content.Intent
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
                        vercelUrl = savedVercelUrl,
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
                        Toast.makeText(this@ShareActivity, "Sync Error: ${response.code}\n$responseBody", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ShareActivity, "Network Failure: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(
    vercelUrl: String,
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = false, onClick = {}) // Clicking card does not dismiss
            .border(1.dp, Color(0xFF27272A), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xE60F0F1D)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Quick Evidence Capture",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = Color.Gray, fontSize = 12.sp)
                }
            }

            // Raw citation text
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = { Text("Raw Citation Clipping Text", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 180.dp),
                maxLines = 6,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFF27272A)
                )
            )

            // Source Title
            OutlinedTextField(
                value = sourceTitleInput,
                onValueChange = { sourceTitleInput = it },
                label = { Text("Source Publisher / Title", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFF27272A)
                )
            )

            // Source URL
            OutlinedTextField(
                value = sourceUrlInput,
                onValueChange = { sourceUrlInput = it },
                label = { Text("Source Web URL", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFF27272A)
                )
            )

            // Tags
            OutlinedTextField(
                value = contextInput,
                onValueChange = { contextInput = it },
                label = { Text("Study Remarks / Tags (Optional)", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFF27272A)
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            Button(
                onClick = {
                    isSubmitting = true
                    onSubmit(textInput, sourceUrlInput, sourceTitleInput, contextInput)
                },
                enabled = textInput.isNotEmpty() && !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    disabledContainerColor = Color(0xFF1E1E26)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Structure & Index Evidence Item",
                        fontWeight = FontWeight.Bold,
                        color = if (textInput.isNotEmpty()) Color.White else Color.Gray
                    )
                }
            }
        }
    }
}
