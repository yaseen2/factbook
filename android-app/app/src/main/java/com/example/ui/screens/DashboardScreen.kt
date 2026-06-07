package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.CaptureEntity
import com.example.ui.CaptureViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: CaptureViewModel,
    presetText: String,
    presetUrl: String,
    onNavigateToSettings: () -> Unit,
    onClearPreset: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val currentText by viewModel.captureTextState.collectAsState()
    val currentUrl by viewModel.captureUrlState.collectAsState()
    val currentTitle by viewModel.captureTitleState.collectAsState()
    val currentRemarks by viewModel.captureRemarksState.collectAsState()

    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMsg by viewModel.syncStatusMessage.collectAsState()
    val isVercelHealthy by viewModel.isVercelHealthy.collectAsState()
    val historyList by viewModel.captureHistory.collectAsState()

    // Load shared intent presets if present
    LaunchedEffect(presetText, presetUrl) {
        if (presetText.isNotEmpty() || presetUrl.isNotEmpty()) {
            viewModel.updateFormFields(
                text = if (presetText.isNotEmpty()) presetText else currentText,
                url = if (presetUrl.isNotEmpty()) presetUrl else currentUrl,
                title = currentTitle,
                remarks = currentRemarks
            )
            onClearPreset() // Clear on activity state so it doesn't re-trigger on config change
        }
    }

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
            // Elegantly Styled Title Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Factbook Capturer",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CosmicTextPrimary,
                        letterSpacing = (-0.5).sp,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "CSS WORKSTATION BRIDGE",
                        fontSize = 10.sp,
                        color = CosmicTextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Connection Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                when (isVercelHealthy) {
                                    true -> CosmicSuccess.copy(alpha = 0.15f)
                                    false -> CosmicError.copy(alpha = 0.15f)
                                    null -> CosmicTextMuted.copy(alpha = 0.15f)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = when (isVercelHealthy) {
                                    true -> CosmicSuccess.copy(alpha = 0.4f)
                                    false -> CosmicError.copy(alpha = 0.4f)
                                    null -> CosmicTextMuted.copy(alpha = 0.4f)
                                },
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.checkVercelHealth() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    when (isVercelHealthy) {
                                        true -> CosmicSuccess
                                        false -> CosmicError
                                        null -> CosmicTextMuted
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (isVercelHealthy) {
                                true -> "VERCEL CONNECTED"
                                false -> "OFFLINE / ERROR"
                                null -> "CHECKING PROBE"
                            },
                            color = when (isVercelHealthy) {
                                true -> CosmicSuccess
                                false -> CosmicError
                                null -> CosmicTextSecondary
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_nav_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open Settings Configuration",
                            tint = CosmicTextPrimary
                        )
                    }
                }
            }

            Divider(color = CosmicBorder, thickness = 1.dp)

            // Split Layout Scroll Area
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Section 1: Capture Deck
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CLIP CARD CAPTURER",
                            color = CosmicSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                            border = BorderStroke(1.dp, CosmicBorder),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Raw text container with paste action
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Clipped Raw News/PDF Text",
                                        color = CosmicTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    TextButton(
                                        onClick = {
                                            val clipboardText = clipboardManager.getText()?.text ?: ""
                                            if (clipboardText.isNotEmpty()) {
                                                viewModel.updateFormFields(
                                                    text = clipboardText,
                                                    url = currentUrl,
                                                    title = currentTitle,
                                                    remarks = currentRemarks
                                                )
                                            }
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = CosmicSecondary)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentPaste,
                                            contentDescription = "Paste Clipping text",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Paste", fontSize = 12.sp)
                                    }
                                }

                                OutlinedTextField(
                                    value = currentText,
                                    onValueChange = {
                                        viewModel.updateFormFields(
                                            text = it,
                                            url = currentUrl,
                                            title = currentTitle,
                                            remarks = currentRemarks
                                        )
                                    },
                                    placeholder = {
                                        Text(
                                            "Paste CSS target notes, highlighted facts, ratios, or historic figures...",
                                            color = CosmicTextMuted
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 120.dp, max = 200.dp)
                                        .testTag("clipper_text_input"),
                                    maxLines = 10,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = CosmicTextPrimary,
                                        unfocusedTextColor = CosmicTextPrimary,
                                        focusedBorderColor = CosmicPrimary,
                                        unfocusedBorderColor = CosmicBorder,
                                        focusedContainerColor = CosmicSurfaceVariant,
                                        unfocusedContainerColor = CosmicSurfaceVariant
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // URL and Title Input Cards
                                Text(
                                    text = "Source Reference",
                                    color = CosmicTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = currentTitle,
                                        onValueChange = {
                                            viewModel.updateFormFields(
                                                text = currentText,
                                                url = currentUrl,
                                                title = it,
                                                remarks = currentRemarks
                                            )
                                        },
                                        placeholder = { Text("Publisher/e.g., Dawn", color = CosmicTextMuted, fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("source_title_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = CosmicTextPrimary,
                                            unfocusedTextColor = CosmicTextPrimary,
                                            focusedBorderColor = CosmicPrimary,
                                            unfocusedBorderColor = CosmicBorder,
                                            focusedContainerColor = CosmicSurfaceVariant,
                                            unfocusedContainerColor = CosmicSurfaceVariant
                                        )
                                    )

                                    OutlinedTextField(
                                        value = currentUrl,
                                        onValueChange = {
                                            viewModel.updateFormFields(
                                                text = currentText,
                                                url = it,
                                                title = currentTitle,
                                                remarks = currentRemarks
                                            )
                                        },
                                        placeholder = { Text("Web URL", color = CosmicTextMuted, fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .testTag("source_url_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = CosmicTextPrimary,
                                            unfocusedTextColor = CosmicTextPrimary,
                                            focusedBorderColor = CosmicPrimary,
                                            unfocusedBorderColor = CosmicBorder,
                                            focusedContainerColor = CosmicSurfaceVariant,
                                            unfocusedContainerColor = CosmicSurfaceVariant
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Study remarks
                                Text(
                                    text = "Student Remarks & Category Tags",
                                    color = CosmicTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                OutlinedTextField(
                                    value = currentRemarks,
                                    onValueChange = {
                                        viewModel.updateFormFields(
                                            text = currentText,
                                            url = currentUrl,
                                            title = currentTitle,
                                            remarks = it
                                        )
                                    },
                                    placeholder = {
                                        Text(
                                            "e.g., Pakistan Economy - CPEC loans, Page 4. Use in Paper II",
                                            color = CosmicTextMuted,
                                            fontSize = 13.sp
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("student_remarks_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = CosmicTextPrimary,
                                        unfocusedTextColor = CosmicTextPrimary,
                                        focusedBorderColor = CosmicPrimary,
                                        unfocusedBorderColor = CosmicBorder,
                                        focusedContainerColor = CosmicSurfaceVariant,
                                        unfocusedContainerColor = CosmicSurfaceVariant
                                    )
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Clear & Sync buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.clearForm() },
                                        border = BorderStroke(1.dp, CosmicBorder),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicTextSecondary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Clear", fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.requestCardSync() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CosmicPrimary,
                                            contentColor = CosmicTextPrimary
                                        ),
                                        enabled = !isSyncing && currentText.isNotEmpty(),
                                        modifier = Modifier
                                            .weight(2f)
                                            .testTag("sync_button")
                                    ) {
                                        if (isSyncing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = CosmicTextPrimary,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.CloudUpload,
                                                contentDescription = "Sync to work Cloud"
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Sync Workstation", fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }

                        // Sync message alert
                        AnimatedVisibility(
                            visible = syncMsg != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            syncMsg?.let { msg ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (msg.contains("Error") || msg.contains("failed") || msg.contains("Connection"))
                                            CosmicError.copy(alpha = 0.1f)
                                        else
                                            CosmicSuccess.copy(alpha = 0.1f)
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (msg.contains("Error") || msg.contains("failed") || msg.contains("Connection"))
                                            CosmicError.copy(alpha = 0.3f)
                                        else
                                            CosmicSuccess.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (msg.contains("Error") || msg.contains("failed") || msg.contains("Connection"))
                                                Icons.Default.ErrorOutline
                                            else
                                                Icons.Default.CheckCircle,
                                            contentDescription = "Status Response",
                                            tint = if (msg.contains("Error") || msg.contains("failed") || msg.contains("Connection"))
                                                CosmicError
                                            else
                                                CosmicSuccess,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = msg,
                                            fontSize = 12.sp,
                                            color = CosmicTextPrimary,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.syncStatusMessage.value = null },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close status badge",
                                                tint = CosmicTextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Cards Backlog history
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BACKLOG INDEX (${historyList.size})",
                            color = CosmicSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "${historyList.count { !it.isSynced }} Pending Syncs",
                            color = if (historyList.any { !it.isSynced }) CosmicOrange else CosmicTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (historyList.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LayersClear,
                                contentDescription = "No local backlog",
                                tint = CosmicTextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Backlog index is empty",
                                color = CosmicTextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Shared clippings or quick-captured cards will register here.",
                                color = CosmicTextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    items(historyList) { capture ->
                        HistoryCard(
                            capture = capture,
                            onRetrySync = { viewModel.forceRetrySingleSync(capture) },
                            onDelete = { viewModel.deleteCapture(capture) }
                        )
                    }
                }

                // Section 3: Professional Diagnostics Console System
                item {
                    Divider(
                        color = CosmicBorder,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)
                    )
                }

                item {
                    val logs by viewModel.appLogs.collectAsState()
                    var isConsoleExpanded by remember { mutableStateOf(false) }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                        border = BorderStroke(1.dp, CosmicBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("diagnostic_terminal_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Header row with toggle, clear, and copy action
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { isConsoleExpanded = !isConsoleExpanded }
                                ) {
                                    Icon(
                                        imageVector = if (isConsoleExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Toggle diagnostic details",
                                        tint = CosmicSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "DIAGNOSTIC CONSOLE",
                                        color = CosmicSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CosmicBorder)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${logs.size} EVENTS",
                                            color = CosmicTextSecondary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (logs.isNotEmpty()) {
                                        TextButton(
                                            onClick = {
                                                val logText = logs.joinToString("\n") { "[${it.timestamp}] [${it.level}] ${it.message}" }
                                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(logText))
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.textButtonColors(contentColor = CosmicTextSecondary)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ContentPaste,
                                                contentDescription = "Copy all terminal events",
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copy", fontSize = 11.sp)
                                        }

                                        TextButton(
                                            onClick = { viewModel.clearLogs() },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.textButtonColors(contentColor = CosmicError)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = "Clear console",
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Clear", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            if (isConsoleExpanded) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF030308)),
                                    border = BorderStroke(1.dp, CosmicBorder),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 240.dp)
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        if (logs.isEmpty()) {
                                            Text(
                                                text = "No recorded workstation telemetry events.\nType or paste a clipping and hit 'Sync' to test connection status.",
                                                color = CosmicTextMuted,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                modifier = Modifier
                                                    .align(Alignment.CenterHorizontally)
                                                    .padding(vertical = 16.dp)
                                            )
                                        } else {
                                            logs.forEach { logItem ->
                                                val tokenColor = when (logItem.level) {
                                                    "SUCCESS" -> CosmicSuccess
                                                    "ERROR" -> CosmicError
                                                    "WARN" -> CosmicOrange
                                                    else -> CosmicTextSecondary
                                                }
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        text = "[${logItem.timestamp}]",
                                                        color = CosmicTextMuted,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 11.sp,
                                                        modifier = Modifier.padding(end = 6.dp)
                                                    )
                                                    Text(
                                                        text = "[${logItem.level}]",
                                                        color = tokenColor,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(end = 8.dp)
                                                    )
                                                    Text(
                                                        text = logItem.message,
                                                        color = CosmicTextPrimary,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Inspect raw HTML page reports, JSON failures, and query parameters directly inside the app. Tap 'Copy' to copy all diagnostic info.",
                                    color = CosmicTextMuted,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            } else {
                                Text(
                                    text = if (logs.isNotEmpty()) "Tap to expand and see recent workstation telemetry logs (" + logs.size + " logged events)" else "Tap to expand and monitor sync details",
                                    color = CosmicTextMuted,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .clickable { isConsoleExpanded = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    capture: CaptureEntity,
    onRetrySync: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val dateString = remember(capture.timestamp) { formatter.format(Date(capture.timestamp)) }

    Card(
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(
            width = 1.dp,
            color = if (capture.isSynced) CosmicBorder else CosmicOrange.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (capture.isSynced) Icons.Default.CheckCircle else Icons.Default.Schedule,
                        contentDescription = "Sync State Indicator",
                        tint = if (capture.isSynced) CosmicSuccess else CosmicOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (capture.isSynced) "SYNCED TO VERCEL" else "BACKLOG / OFFLINE",
                        color = if (capture.isSynced) CosmicSuccess else CosmicOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = dateString,
                    color = CosmicTextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body Clipping
            Text(
                text = capture.text,
                color = CosmicTextPrimary,
                fontSize = 13.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Meta reference values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (capture.sourceTitle.isNotEmpty() || capture.sourceUrl.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Source",
                                tint = CosmicSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (capture.sourceTitle.isNotEmpty()) {
                                    "${capture.sourceTitle} (${capture.sourceUrl})"
                                } else {
                                    capture.sourceUrl
                                },
                                color = CosmicSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (capture.remarks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notes,
                                contentDescription = "Remarks notes label",
                                tint = CosmicTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Study: ${capture.remarks}",
                                color = CosmicTextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row {
                    // Force Retry if not synced
                    if (!capture.isSynced) {
                        IconButton(
                            onClick = onRetrySync,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Retry synchronization sync for this card",
                                tint = CosmicSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Delete single entry
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Remove card from backlog",
                            tint = CosmicError,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Sync rejected details
            if (!capture.isSynced && capture.errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = capture.errorMessage,
                    color = CosmicError,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(CosmicError.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
