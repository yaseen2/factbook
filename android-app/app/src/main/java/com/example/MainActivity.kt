package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.CaptureViewModel
import com.example.ui.CaptureViewModelFactory
import com.example.ui.FloatingCaptureService
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicError
import com.example.ui.theme.CosmicPrimary
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.CosmicTextPrimary
import com.example.ui.theme.CosmicTextSecondary
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val sharedTextState = mutableStateOf("")
    private val sharedUrlState = mutableStateOf("")
    private val hasOverlayPermissionState = mutableStateOf(false)
    private lateinit var viewModel: CaptureViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Setup custom ViewModel 
        viewModel = ViewModelProvider(
            this, 
            CaptureViewModelFactory(applicationContext)
        )[CaptureViewModel::class.java]

        hasOverlayPermissionState.value = Settings.canDrawOverlays(this)
        handleShareIntent(intent)

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (!hasOverlayPermissionState.value) {
                            OverlayPermissionBanner(
                                onRequestPermission = {
                                    val overlayIntent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:$packageName")
                                    )
                                    startActivity(overlayIntent)
                                }
                            )
                        }

                        NavHost(
                            navController = navController,
                            startDestination = "dashboard",
                            modifier = Modifier.weight(1f)
                        ) {
                            composable("dashboard") {
                                DashboardScreen(
                                    viewModel = viewModel,
                                    presetText = sharedTextState.value,
                                    presetUrl = sharedUrlState.value,
                                    onNavigateToSettings = { navController.navigate("settings") },
                                    onClearPreset = {
                                        sharedTextState.value = ""
                                        sharedUrlState.value = ""
                                    }
                                )
                            }

                            composable("settings") {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasOverlayPermissionState.value = Settings.canDrawOverlays(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            if (sharedText.isNotEmpty()) {
                var url = ""
                var text = sharedText

                // Parse out URLs if they are shared alone or inside text
                if (sharedText.startsWith("http://") || sharedText.startsWith("https://")) {
                    url = sharedText
                    text = ""
                } else {
                    val urlRegex = "(https?://[^\\s]+)".toRegex()
                    val matchedUrl = urlRegex.find(sharedText)?.value
                    if (matchedUrl != null) {
                        url = matchedUrl
                        text = sharedText.replace(matchedUrl, "").trim()
                    }
                }

                val title = intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: ""

                if (Settings.canDrawOverlays(this)) {
                    // Trigger overlay capturing service
                    val serviceIntent = Intent(this, FloatingCaptureService::class.java).apply {
                        putExtra("text", text)
                        putExtra("url", url)
                        putExtra("title", title)
                    }
                    startService(serviceIntent)
                    finish()
                } else {
                    // Fall back to standard Compose UI flow
                    sharedTextState.value = text
                    sharedUrlState.value = url
                }
            }
        }
    }
}

@Composable
fun OverlayPermissionBanner(onRequestPermission: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, CosmicError.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "FLOATING CAPTURE INACTIVE",
                    color = CosmicError,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Grant overlay permission to capture clippings in a floating pop-up without leaving Chrome.",
                    color = CosmicTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Enable",
                    color = CosmicTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
