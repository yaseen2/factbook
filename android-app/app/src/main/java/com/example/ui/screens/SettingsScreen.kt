package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CaptureViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: CaptureViewModel,
    onNavigateBack: () -> Unit
) {
    // Collect secure attributes from DataStore Flow State
    val savedVercelUrl by viewModel.vercelApiUrl.collectAsState()
    val savedGeminiKey by viewModel.geminiApiKey.collectAsState()
    val savedDocId by viewModel.targetGoogleDocId.collectAsState()
    val savedServiceAccount by viewModel.serviceAccountJson.collectAsState()
    val savedCustomPrompt by viewModel.customGeminiPrompt.collectAsState()

    // Form states for manual updates
    var inputVercelUrl by remember(savedVercelUrl) { mutableStateOf(savedVercelUrl) }
    var inputGeminiKey by remember(savedGeminiKey) { mutableStateOf(savedGeminiKey) }
    var inputDocId by remember(savedDocId) { mutableStateOf(savedDocId) }
    var inputServiceAccount by remember(savedServiceAccount) { mutableStateOf(savedServiceAccount) }
    var inputCustomPrompt by remember(savedCustomPrompt) { mutableStateOf(savedCustomPrompt) }

    // Direct hash import input state
    var syncHashInput by remember { mutableStateOf("") }
    var hashImportSuccess by remember { mutableStateOf<Boolean?>(null) }

    val savingStatus = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Return to Dashboard",
                        tint = CosmicTextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "Workstation Params",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CosmicTextPrimary,
                        letterSpacing = (-0.3).sp,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "BRIDGE ENCRYPTION CONFIGS",
                        fontSize = 9.sp,
                        color = CosmicTextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Divider(color = CosmicBorder, thickness = 1.dp)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Sync instant profile loader
                item {
                    Text(
                        text = "INSTANT PROFILE DECODER",
                        color = CosmicSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                        border = BorderStroke(1.dp, CosmicPrimary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Load via Configuration Hash link (#sync=base64...)",
                                color = CosmicTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Paste the generated hash parameters exported from your Next.js Vercel dashboard to apply configs on one tap.",
                                color = CosmicTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = syncHashInput,
                                onValueChange = { syncHashInput = it },
                                placeholder = {
                                    Text(
                                        "Paste `#sync=ey...` hash link here",
                                        color = CosmicTextMuted,
                                        fontSize = 12.sp
                                    )
                                },
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp, max = 100.dp)
                                    .testTag("sync_hash_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CosmicTextPrimary,
                                    unfocusedTextColor = CosmicTextPrimary,
                                    focusedBorderColor = CosmicSecondary,
                                    unfocusedBorderColor = CosmicBorder,
                                    focusedContainerColor = CosmicSurfaceVariant,
                                    unfocusedContainerColor = CosmicSurfaceVariant
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    viewModel.importSettingsHash(
                                        hash = syncHashInput,
                                        onSuccess = {
                                            hashImportSuccess = true
                                            syncHashInput = ""
                                        },
                                        onError = {
                                            hashImportSuccess = false
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CosmicPrimary,
                                    contentColor = CosmicTextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("apply_hash_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Sync profile link button"
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import Configuration Profile", fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }

                            // Import Outcome Status Badge
                            AnimatedVisibility(
                                visible = hashImportSuccess != null,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (hashImportSuccess == true)
                                            CosmicSuccess.copy(alpha = 0.15f)
                                        else
                                            CosmicError.copy(alpha = 0.15f)
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (hashImportSuccess == true) CosmicSuccess else CosmicError
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (hashImportSuccess == true)
                                                Icons.Default.CheckCircle
                                            else
                                                Icons.Default.Error,
                                            contentDescription = "Decoder feedback icon",
                                            tint = if (hashImportSuccess == true) CosmicSuccess else CosmicError,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (hashImportSuccess == true)
                                                "Profile loaded successfully! Settings updated."
                                            else
                                                "Malformed Base64 payload sync configuration. Try copy again.",
                                            fontSize = 12.sp,
                                            color = CosmicTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { hashImportSuccess = null }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close feedback badge",
                                                tint = CosmicTextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Core parameters manual credentials Edit Screen
                item {
                    Text(
                        text = "MANUAL CREDENTIALS INTERACTION",
                        color = CosmicSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                        border = BorderStroke(1.dp, CosmicBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // 1. Vercel Endpoint Address
                            Text(
                                text = "Vercel Sync Endpoint URL",
                                color = CosmicTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = inputVercelUrl,
                                onValueChange = { inputVercelUrl = it },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("vercel_url_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CosmicTextPrimary,
                                    unfocusedTextColor = CosmicTextPrimary,
                                    focusedBorderColor = CosmicSecondary,
                                    unfocusedBorderColor = CosmicBorder,
                                    focusedContainerColor = CosmicSurfaceVariant,
                                    unfocusedContainerColor = CosmicSurfaceVariant
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 2. Gemini API Key Override
                            Text(
                                text = "Fallback Gemini API Key",
                                color = CosmicTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = inputGeminiKey,
                                onValueChange = { inputGeminiKey = it },
                                placeholder = {
                                    Text(
                                        "Saved in BuildConfig if left empty",
                                        color = CosmicTextMuted,
                                        fontSize = 12.sp
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("gemini_key_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CosmicTextPrimary,
                                    unfocusedTextColor = CosmicTextPrimary,
                                    focusedBorderColor = CosmicSecondary,
                                    unfocusedBorderColor = CosmicBorder,
                                    focusedContainerColor = CosmicSurfaceVariant,
                                    unfocusedContainerColor = CosmicSurfaceVariant
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 3. Target Doc ID
                            Text(
                                text = "Google Sheet / Doc Target ID",
                                color = CosmicTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = inputDocId,
                                onValueChange = { inputDocId = it },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("doc_id_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CosmicTextPrimary,
                                    unfocusedTextColor = CosmicTextPrimary,
                                    focusedBorderColor = CosmicSecondary,
                                    unfocusedBorderColor = CosmicBorder,
                                    focusedContainerColor = CosmicSurfaceVariant,
                                    unfocusedContainerColor = CosmicSurfaceVariant
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 4. Custom Gemini Prompt
                            Text(
                                text = "Custom Instruction System Prompt Override",
                                color = CosmicTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = inputCustomPrompt,
                                onValueChange = { inputCustomPrompt = it },
                                maxLines = 4,
                                placeholder = {
                                    Text(
                                        "Prompt sent during synthesis stage...",
                                        color = CosmicTextMuted,
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp, max = 100.dp)
                                    .testTag("custom_prompt_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CosmicTextPrimary,
                                    unfocusedTextColor = CosmicTextPrimary,
                                    focusedBorderColor = CosmicSecondary,
                                    unfocusedBorderColor = CosmicBorder,
                                    focusedContainerColor = CosmicSurfaceVariant,
                                    unfocusedContainerColor = CosmicSurfaceVariant
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 5. Service Account JSON file text
                            Text(
                                text = "Google Service Account Credentials JSON",
                                color = CosmicTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = inputServiceAccount,
                                onValueChange = { inputServiceAccount = it },
                                maxLines = 10,
                                placeholder = {
                                    Text(
                                        "{\n  \"type\": \"service_account\",\n  \"project_id\": \"...\"\n}",
                                        color = CosmicTextMuted,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 200.dp)
                                    .testTag("service_account_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CosmicTextPrimary,
                                    unfocusedTextColor = CosmicTextPrimary,
                                    focusedBorderColor = CosmicSecondary,
                                    unfocusedBorderColor = CosmicBorder,
                                    focusedContainerColor = CosmicSurfaceVariant,
                                    unfocusedContainerColor = CosmicSurfaceVariant
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Save Configuration Profile Button
                            Button(
                                onClick = {
                                    savingStatus.value = true
                                    viewModel.saveSettings(
                                        vercelUrl = inputVercelUrl,
                                        geminiKey = inputGeminiKey,
                                        docId = inputDocId,
                                        serviceAccount = inputServiceAccount,
                                        customPrompt = inputCustomPrompt
                                    )
                                    savingStatus.value = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CosmicSecondary,
                                    contentColor = CosmicBackground
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Save keys"
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (savingStatus.value) "Saving Profiles..." else "Save configurations locally",
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                // Section 3: Educational instructions about APK keys leaks 
                item {
                    Text(
                        text = "SECURITY PROTOCOL NOTICE",
                        color = CosmicSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicSurfaceVariant),
                        border = BorderStroke(1.dp, CosmicBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Shield Alert icon",
                                    tint = CosmicSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Bridge Security Warning",
                                    color = CosmicTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "I have included your API keys in the generated APK file for this prototype. Please be aware that Android APKs can be easily decompiled, and these keys can be extracted by anyone who has access to the file. Do not share this APK file publicly or with unauthorized individuals to prevent potential misuse.",
                                color = CosmicTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
