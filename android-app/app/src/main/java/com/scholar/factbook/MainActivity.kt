package com.scholar.factbook

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

class MainActivity : ComponentActivity() {

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load saved Vercel URL from SharedPreferences
        val sharedPref = getSharedPreferences("factbook_prefs", Context.MODE_PRIVATE)
        val savedVercelUrl = sharedPref.getString("vercel_url", "https://your-app.vercel.app") ?: ""

        // Extract raw shared text from incoming system intents
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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    MainScreen(
                        initialVercelUrl = savedVercelUrl,
                        initialText = sharedText,
                        initialUrl = sharedUrl,
                        initialTitle = sharedTitle,
                        onSaveVercelUrl = { url ->
                            sharedPref.edit().putString("vercel_url", url).apply()
                            Toast.makeText(this, "Vercel URL Saved", Toast.LENGTH_SHORT).show()
                        },
                        onSubmit = { vercelUrl, text, sourceUrl, sourceTitle, contextRemarks ->
                            submitCapture(vercelUrl, text, sourceUrl, sourceTitle, contextRemarks)
                        }
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
            // Clean url out of text body
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
        if (cleanUrl.isEmpty() || !cleanUrl.startsWith("http")) {
            Toast.makeText(this, "Please configure a valid Vercel deployment URL", Toast.LENGTH_LONG).show()
            return
        }

        val endpoint = "$cleanUrl/api/capture"
        
        // Build JSON body matching Next.js API payload
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
                        Toast.makeText(this@MainActivity, "Evidence Synchronized Successfully!", Toast.LENGTH_LONG).show()
                        finish() // Close app on success to return user to active reader tab
                    } else {
                        Toast.makeText(this@MainActivity, "Sync Error: ${response.code}\n$responseBody", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Network Failure: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

@Composable
fun FactbookTheme(content: @Composable () -> Unit) {
    // Custom premium dark theme colors matching our Next.js dashboard styling
    val darkColors = darkColorScheme(
        background = Color(0xFF050508),
        surface = Color(0xFF121216),
        primary = Color(0xFF2563EB),
        secondary = Color(0xFF06B6D4),
        onBackground = Color(0xFFE4E4E7),
        onSurface = Color(0xFFFAFAFA)
    )
    MaterialTheme(
        colorScheme = darkColors,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    initialVercelUrl: String,
    initialText: String,
    initialUrl: String,
    initialTitle: String,
    onSaveVercelUrl: (String) -> Unit,
    onSubmit: (String, String, String, String, String) -> Unit
) {
    var vercelUrl by remember { mutableStateOf(initialVercelUrl) }
    var textInput by remember { mutableStateOf(initialText) }
    var sourceUrlInput by remember { mutableStateOf(initialUrl) }
    var sourceTitleInput by remember { mutableStateOf(initialTitle) }
    var contextInput by remember { mutableStateOf("") }
    
    var showSettings by remember { mutableStateOf(initialVercelUrl.contains("your-app")) }
    var isSubmitting by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF050508), Color(0xFF0B0A12))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Premium App Branding
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
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Scholar's Ledger Mobile",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = (-0.5).sp
                    )
                }

                Button(
                    onClick = { showSettings = !showSettings },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E26)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (showSettings) "Back" else "Settings",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD4D4D8)
                    )
                }
            }

            if (showSettings) {
                // Settings Section Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF27272A), RoundedCornerShape(12.dp))
                        .background(Color(0xFF121216), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "WORKSPACE ENDPOINT",
                        color = Color(0xFF06B6D4),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    OutlinedTextField(
                        value = vercelUrl,
                        onValueChange = { vercelUrl = it },
                        label = { Text("Vercel App Domain Link", color = Color.Gray) },
                        placeholder = { Text("https://your-deployment.vercel.app") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFF27272A)
                        )
                    )

                    Button(
                        onClick = {
                            onSaveVercelUrl(vercelUrl)
                            showSettings = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Workspace Link", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Capture Workstation Form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF27272A), RoundedCornerShape(12.dp))
                        .background(Color(0xFF121216), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "CAPTURE WORKSTATION",
                        color = Color(0xFF3B82F6),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    // Text Input
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text("Raw Citation Clipping Text", color = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        maxLines = 8,
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

                    // Context Remarks
                    OutlinedTextField(
                        value = contextInput,
                        onValueChange = { contextInput = it },
                        label = { Text("Study Remarks / Tags (Optional)", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFF27272A)
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            isSubmitting = true
                            onSubmit(vercelUrl, textInput, sourceUrlInput, sourceTitleInput, contextInput)
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
    }
}
