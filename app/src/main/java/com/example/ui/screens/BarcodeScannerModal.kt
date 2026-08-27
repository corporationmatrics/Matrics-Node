package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.viewmodel.BarcodeScanMode
import com.example.ui.viewmodel.CyphrViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

private const val TAG = "BarcodeScannerModal"

@Composable
fun BarcodeScannerModal(
    viewModel: CyphrViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.barcodeScannerState.collectAsState()
    val isFieldMode by viewModel.isFieldMode.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    if (!state.isVisible) return

    // Camera permission check
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Audio Tone Generator for laser scan beeps
    val toneGen = remember {
        try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
        } catch (e: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                toneGen?.release()
            } catch (_: Exception) {}
        }
    }

    // Play feedback tone on scan
    val playScanFeedback: () -> Unit = {
        if (state.soundEnabled) {
            try {
                toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 110)
            } catch (_: Exception) {}
        }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    var showManualEntry by remember { mutableStateOf(false) }
    var manualBarcodeText by remember { mutableStateOf("") }
    var showDemoBarcodes by remember { mutableStateOf(false) }

    val bgColor = if (isFieldMode) Color(0xFF000000) else Color(0xFF070B14)
    val accentCyan = if (isFieldMode) Color(0xFF00FFFF) else Color(0xFF00E5FF)
    val accentLime = Color(0xFF10B981)

    // Main Camera Scanner Viewport
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .testTag("barcode_scanner_modal")
    ) {
        if (hasCameraPermission) {
            CameraPreviewWithAnalyzer(
                isTorchOn = state.isTorchOn,
                onBarcodeDetected = { code ->
                    playScanFeedback()
                    viewModel.handleBarcodeScanned(code)
                }
            )
        } else {
            // Camera Permission Needed State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = accentCyan,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Grant camera access to scan 1D retail barcodes (EAN-13, UPC, Code 128) and 2D QR codes in real-time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = accentCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Grant Camera Access", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Overlay: Reticle, Laser Beam & Guides
        ScannerOverlayView(
            accentColor = accentCyan,
            statusText = state.statusFeedback,
            lastScanned = state.lastScannedCode,
            scanCount = state.sessionScanCount
        )

        // Top Navigation & Control Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
                .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.closeBarcodeScanner() },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .testTag("close_scanner_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Scanner",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CYPHR SCANNER",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = accentCyan,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = when (state.mode) {
                                BarcodeScanMode.POS_BILLING -> "POS Quick Sale Scanner"
                                BarcodeScanMode.INVENTORY_SEARCH -> "Catalog SKU Finder"
                                BarcodeScanMode.INVENTORY_RESTOCK -> "Rapid Stock (+1) Scanner"
                                BarcodeScanMode.PRICE_CHECKER -> "Price & Margin Checker"
                                BarcodeScanMode.SKU_REGISTRATION -> "Link Barcode to Item"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Torch Toggle
                    IconButton(
                        onClick = { viewModel.toggleBarcodeScannerTorch() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (state.isTorchOn) accentCyan.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.15f),
                                CircleShape
                            )
                            .testTag("toggle_torch_button")
                    ) {
                        Icon(
                            imageVector = if (state.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Toggle Torch",
                            tint = if (state.isTorchOn) accentCyan else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Sound Toggle
                    IconButton(
                        onClick = { viewModel.toggleScannerSound() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .testTag("toggle_sound_button")
                    ) {
                        Icon(
                            imageVector = if (state.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Toggle Sound",
                            tint = if (state.soundEnabled) accentLime else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Manual Keypad Entry Toggle
                    IconButton(
                        onClick = { showManualEntry = !showManualEntry },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (showManualEntry) accentCyan.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.15f),
                                CircleShape
                            )
                            .testTag("manual_barcode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Type Barcode",
                            tint = if (showManualEntry) accentCyan else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mode Selector Tabs
            val tabs = listOf(
                BarcodeScanMode.POS_BILLING to "POS Bill",
                BarcodeScanMode.PRICE_CHECKER to "Price Check",
                BarcodeScanMode.INVENTORY_RESTOCK to "Restock (+1)",
                BarcodeScanMode.INVENTORY_SEARCH to "Find SKU"
            )

            val selectedTabIndex = tabs.indexOfFirst { it.first == state.mode }.coerceAtLeast(0)

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Black.copy(alpha = 0.5f),
                contentColor = accentCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = accentCyan,
                        height = 2.5.dp
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            ) {
                tabs.forEachIndexed { index, (mode, label) ->
                    Tab(
                        selected = state.mode == mode,
                        onClick = { viewModel.setBarcodeScanMode(mode) },
                        text = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (state.mode == mode) FontWeight.Bold else FontWeight.Medium,
                                color = if (state.mode == mode) accentCyan else Color(0xFF94A3B8)
                            )
                        }
                    )
                }
            }
        }

        // Bottom Dashboard / Recent Scans HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            // Live Matched Item Card (If scanned)
            AnimatedVisibility(
                visible = state.lastMatchedCommodity != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                state.lastMatchedCommodity?.let { commodity ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentCyan.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(accentLime, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = commodity.brand.ifBlank { commodity.category },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accentCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• ${commodity.sku.ifBlank { "EAN-13" }}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = commodity.canonicalName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Stock: ${commodity.stockQuantity.toInt()} ${commodity.normalizedUnit} • Cost: ₹${commodity.costPrice.toInt()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹${commodity.sellingPrice.toInt()}",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = accentLime
                                )
                                Text(
                                    text = when (state.mode) {
                                        BarcodeScanMode.POS_BILLING -> "Added to Cart ✓"
                                        BarcodeScanMode.INVENTORY_RESTOCK -> "Stock +1 ✓"
                                        BarcodeScanMode.PRICE_CHECKER -> "Verified ✓"
                                        else -> "Scanned"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentLime,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // Quick Demo Barcode Tester Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Demo Barcodes (Instant Click):",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
                TextButton(
                    onClick = { showDemoBarcodes = !showDemoBarcodes },
                    modifier = Modifier.height(26.dp)
                ) {
                    Text(
                        text = if (showDemoBarcodes) "Hide Demo Chips" else "Show Demo Chips ▾",
                        color = accentCyan,
                        fontSize = 10.sp
                    )
                }
            }

            AnimatedVisibility(visible = showDemoBarcodes) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val demoItems = listOf(
                        "8901262010053" to "Amul Butter",
                        "8901030383456" to "Tata Salt",
                        "8901058852378" to "Maggi Noodles",
                        "5449000000996" to "Coca-Cola 750ml",
                        "8901725181222" to "Aashirvaad Atta",
                        "SKU-1001" to "Milk 1L",
                        "SKU-1002" to "Basmati Rice"
                    )

                    items(demoItems) { (code, label) ->
                        Surface(
                            onClick = {
                                playScanFeedback()
                                viewModel.handleBarcodeScanned(code)
                            },
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.testTag("demo_barcode_$code")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = accentCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showManualEntry = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("type_code_button"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Type Code", fontSize = 12.sp)
                }

                Button(
                    onClick = { viewModel.closeBarcodeScanner() },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("done_scanning_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentCyan,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.mode == BarcodeScanMode.POS_BILLING) "View Bill (${state.sessionScanCount})" else "Done",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Manual Barcode Entry Sheet Dialog
        if (showManualEntry) {
            AlertDialog(
                onDismissRequest = { showManualEntry = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = null,
                            tint = accentCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enter Barcode / SKU Code", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "Type the 8, 12, or 13-digit barcode printed below the stripes, or alphanumeric SKU.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = manualBarcodeText,
                            onValueChange = { manualBarcodeText = it },
                            placeholder = { Text("e.g. 8901262010053 or SKU-1001") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (manualBarcodeText.isNotBlank()) {
                                        playScanFeedback()
                                        viewModel.handleBarcodeScanned(manualBarcodeText)
                                        showManualEntry = false
                                        manualBarcodeText = ""
                                    }
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_barcode_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentCyan,
                                unfocusedBorderColor = Color(0xFF475569)
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (manualBarcodeText.isNotBlank()) {
                                playScanFeedback()
                                viewModel.handleBarcodeScanned(manualBarcodeText)
                                showManualEntry = false
                                manualBarcodeText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentCyan, contentColor = Color.Black),
                        modifier = Modifier.testTag("submit_manual_barcode")
                    ) {
                        Text("Search / Add", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showManualEntry = false }) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                },
                containerColor = Color(0xFF0F172A),
                textContentColor = Color.White,
                titleContentColor = Color.White
            )
        }

        // Unrecognized Barcode -> Quick SKU Registration Dialog
        if (state.showUnrecognizedDialog && state.unrecognizedBarcode != null) {
            val unrecCode = state.unrecognizedBarcode ?: ""
            var newName by remember { mutableStateOf("") }
            var newCategory by remember { mutableStateOf("Groceries") }
            var newBrand by remember { mutableStateOf("Standard") }
            var newCostPrice by remember { mutableStateOf("40") }
            var newSellingPrice by remember { mutableStateOf("50") }
            var newStock by remember { mutableStateOf("25") }

            AlertDialog(
                onDismissRequest = { viewModel.dismissUnrecognizedBarcodeDialog() },
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "New Barcode Detected",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Code: $unrecCode",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = accentCyan
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "This item is not yet in your master catalog. Add it now to link this barcode forever:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Product Name *") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_name_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newBrand,
                                onValueChange = { newBrand = it },
                                label = { Text("Brand") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newCategory,
                                onValueChange = { newCategory = it },
                                label = { Text("Category") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newCostPrice,
                                onValueChange = { newCostPrice = it },
                                label = { Text("Cost (₹)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newSellingPrice,
                                onValueChange = { newSellingPrice = it },
                                label = { Text("MRP (₹)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newStock,
                                onValueChange = { newStock = it },
                                label = { Text("Stock") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                viewModel.registerNewSkuFromBarcode(
                                    barcode = unrecCode,
                                    name = newName,
                                    category = newCategory,
                                    brand = newBrand,
                                    costPrice = newCostPrice.toDoubleOrNull() ?: 0.0,
                                    sellingPrice = newSellingPrice.toDoubleOrNull() ?: 50.0,
                                    initialStock = newStock.toDoubleOrNull() ?: 20.0,
                                    addToPosBillImmediately = true
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentLime, contentColor = Color.Black),
                        modifier = Modifier.testTag("register_sku_save_button")
                    ) {
                        Text("Save & Add to Bill", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissUnrecognizedBarcodeDialog() }) {
                        Text("Skip / Ignore", color = Color(0xFF94A3B8))
                    }
                },
                containerColor = Color(0xFF0F172A),
                textContentColor = Color.White,
                titleContentColor = Color.White
            )
        }
    }
}

/**
 * CameraX Live Preview with real-time ML Kit Barcode Analyzer.
 */
@Composable
fun CameraPreviewWithAnalyzer(
    isTorchOn: Boolean,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var cameraControlRef by remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(isTorchOn) {
        try {
            cameraControlRef?.cameraControl?.enableTorch(isTorchOn)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling torch", e)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // Configure ML Kit Barcode Scanner for 1D and 2D Barcodes
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_ALL_FORMATS
                    )
                    .build()
                val barcodeScanner = BarcodeScanning.getClient(options)

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(barcodeScanner, imageProxy, onBarcodeDetected)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    cameraControlRef = camera
                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: androidx.camera.core.ImageProxy,
    onBarcodeDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    if (!rawValue.isNullOrBlank()) {
                        onBarcodeDetected(rawValue)
                        break
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Barcode scanning failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

/**
 * Animated Reticle with Sweeping Laser Line, Corner Guides, and Status HUD.
 */
@Composable
fun ScannerOverlayView(
    accentColor: Color,
    statusText: String,
    lastScanned: String,
    scanCount: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_sweep")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val boxWidth = width * 0.76f
        val boxHeight = width * 0.58f // Proportioned for retail 1D/2D barcodes

        val left = (width - boxWidth) / 2f
        val top = (height - boxHeight) / 2f - 40f
        val right = left + boxWidth
        val bottom = top + boxHeight

        // Dark dim background around reticle
        drawRect(
            color = Color.Black.copy(alpha = 0.55f),
            size = size
        )

        // Clear middle scanning window (cutout effect with transparent blend)
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(16f, 16f),
            blendMode = BlendMode.Clear
        )

        // Reticle Border
        drawRoundRect(
            color = accentColor.copy(alpha = 0.35f),
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(16f, 16f),
            style = Stroke(width = 2f)
        )

        // High-contrast Corner Accents
        val cornerLength = 36f
        val cornerStroke = 6f

        // Top-Left
        drawLine(accentColor, Offset(left, top + cornerLength), Offset(left, top), strokeWidth = cornerStroke)
        drawLine(accentColor, Offset(left, top), Offset(left + cornerLength, top), strokeWidth = cornerStroke)

        // Top-Right
        drawLine(accentColor, Offset(right - cornerLength, top), Offset(right, top), strokeWidth = cornerStroke)
        drawLine(accentColor, Offset(right, top), Offset(right, top + cornerLength), strokeWidth = cornerStroke)

        // Bottom-Left
        drawLine(accentColor, Offset(left, bottom - cornerLength), Offset(left, bottom), strokeWidth = cornerStroke)
        drawLine(accentColor, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth = cornerStroke)

        // Bottom-Right
        drawLine(accentColor, Offset(right - cornerLength, bottom), Offset(right, bottom), strokeWidth = cornerStroke)
        drawLine(accentColor, Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidth = cornerStroke)

        // Sweeping Laser Beam
        val laserY = top + (boxHeight * laserProgress)
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    accentColor.copy(alpha = 0.4f),
                    accentColor,
                    accentColor.copy(alpha = 0.4f),
                    Color.Transparent
                )
            ),
            start = Offset(left + 8f, laserY),
            end = Offset(right - 8f, laserY),
            strokeWidth = 4f
        )
    }
}
