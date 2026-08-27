package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ReceiptOcrEngine
import com.example.data.model.HandwrittenBillItem
import com.example.data.model.HandwrittenBillResult
import com.example.data.model.KachaBillPresets
import com.example.ui.components.CameraCaptureView
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberCard
import com.example.util.ImageProcessingUtils
import com.example.ui.theme.AcidLime
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.EmberPeach
import com.example.ui.theme.GhostSilver
import com.example.ui.theme.GhostSilverMuted
import com.example.ui.theme.LaserLime
import com.example.ui.theme.LaserLimeGlow
import com.example.ui.theme.ScannerCyan
import com.example.ui.theme.ScannerCyanGlow
import com.example.ui.theme.ScribbleRed
import com.example.ui.theme.SteelGrey
import com.example.ui.theme.VoidBlack
import com.example.ui.theme.WarmBorder
import com.example.ui.theme.WarmBorderSubtle
import com.example.ui.theme.WarmCard
import com.example.ui.theme.WarmSurfaceElevated
import com.example.ui.theme.cyphrColors
import com.example.ui.viewmodel.CyphrViewModel
import java.util.Locale

@Composable
fun HandwrittenScannerModal(
    viewModel: CyphrViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.handwrittenScannerState.collectAsStateWithLifecycle()

    if (!state.isVisible) return

    Dialog(
        onDismissRequest = { viewModel.closeHandwrittenScanner() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = VoidBlack.copy(alpha = 0.95f)
        ) {
            if (state.showReconciliationSheet && state.reconciliationResult != null) {
                // SCREEN 6: The Reconciliation Sheet (Human-in-the-Loop Validation UI)
                ReconciliationSheetScreen(
                    viewModel = viewModel,
                    result = state.reconciliationResult!!,
                    selectedItemIndex = state.selectedItemIndex,
                    highContrastFilter = state.highContrastFilter,
                    capturedBitmap = if (state.highContrastFilter) state.binarizedBitmap ?: state.capturedBitmap else state.capturedBitmap
                )
            } else {
                // SCREEN 5: The Scanner HUD (Augmented Reality Capture)
                ScannerHudScreen(
                    viewModel = viewModel,
                    state = state
                )
            }
        }
    }
}

/**
 * SCREEN 5: The Scanner HUD (Augmented Reality Capture)
 */
@Composable
fun ScannerHudScreen(
    viewModel: CyphrViewModel,
    state: com.example.ui.viewmodel.HandwrittenScannerState
) {
    val context = LocalContext.current
    val colors = MaterialTheme.cyphrColors

    // Real Camera Shutter Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        if (bmp != null) {
            viewModel.onHandwrittenRealBitmapCaptured(bmp)
        }
    }

    // Real Photo Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.onHandwrittenImageUriSelected(context, uri)
        }
    }

    // Laser Animation Transition
    val infiniteTransition = rememberInfiniteTransition(label = "laser_sweep")
    val laserYRatio by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "laser_sweep_pos"
    )

    val activeBitmap = if (state.highContrastFilter) state.binarizedBitmap ?: state.capturedBitmap else state.capturedBitmap

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("screen_scanner_hud")
    ) {
        // Top Header with HUD Telemetry
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (state.isDecrypting) LaserLime else ScannerCyan)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "PHYSICAL INVOICE OCR HUD",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ScannerCyan
                    )
                    Text(
                        text = if (state.imageSource != "PRESET") "LIVE AR SENSOR • PRINTED & HANDWRITTEN INVOICES" else "GEMINI 3.5 FLASH • UNIFIED INVOICE PARSING",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = GhostSilverMuted
                    )
                }
            }

            IconButton(
                onClick = { viewModel.closeHandwrittenScanner() },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(WarmSurfaceElevated)
                    .testTag("btn_close_scanner_hud")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close HUD", tint = GhostSilver)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Spacer(modifier = Modifier.height(8.dp))

        // Ingress Mode Switcher: Live Camera vs Benchmark Presets vs Gallery Import vs Raw OCR Paste
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(WarmSurfaceElevated)
                .border(1.dp, WarmBorderSubtle, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Live Camera Mode Tab
            val isCameraTab = state.imageSource == "CAMERA"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isCameraTab) ScannerCyan else Color.Transparent)
                    .clickable {
                        viewModel.switchToLiveCamera()
                    }
                    .padding(vertical = 8.dp)
                    .testTag("tab_live_camera"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = if (isCameraTab) VoidBlack else GhostSilver,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "CAMERA",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCameraTab) VoidBlack else GhostSilver
                    )
                }
            }

            // Benchmark Samples Tab
            val isPresetTab = state.imageSource == "PRESET"
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isPresetTab) ScannerCyan else Color.Transparent)
                    .clickable {
                        viewModel.selectHandwrittenPreset(0)
                    }
                    .padding(vertical = 8.dp)
                    .testTag("tab_sample_presets"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = if (isPresetTab) VoidBlack else GhostSilver,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "SAMPLES",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPresetTab) VoidBlack else GhostSilver
                    )
                }
            }

            // Gallery Import Tab
            val isGalleryTab = state.imageSource == "GALLERY"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isGalleryTab) ScannerCyan else Color.Transparent)
                    .clickable {
                        galleryLauncher.launch("image/*")
                    }
                    .padding(vertical = 8.dp)
                    .testTag("tab_gallery_import"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = if (isGalleryTab) VoidBlack else GhostSilver,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "GALLERY",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGalleryTab) VoidBlack else GhostSilver
                    )
                }
            }

            // Raw OCR Text Tab
            val isTextTab = state.imageSource == "TEXT_OCR"
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isTextTab) ScannerCyan else Color.Transparent)
                    .clickable {
                        viewModel.setImageSource("TEXT_OCR")
                    }
                    .padding(vertical = 8.dp)
                    .testTag("tab_text_ocr"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TextFields,
                        contentDescription = null,
                        tint = if (isTextTab) VoidBlack else GhostSilver,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "RAW OCR",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTextTab) VoidBlack else GhostSilver
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Document Filter Mode Selector Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FILTER:",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = GhostSilverMuted
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(ImageProcessingUtils.DocumentFilter.values()) { _, filter ->
                    val isFilterActive = state.activeFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isFilterActive) LaserLimeGlow else WarmSurfaceElevated)
                            .border(
                                1.dp,
                                if (isFilterActive) LaserLime else WarmBorderSubtle,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setDocumentFilter(filter) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("filter_${filter.name}")
                    ) {
                        Text(
                            text = filter.label,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                            fontWeight = if (isFilterActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isFilterActive) LaserLime else GhostSilver
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // When in Preset Mode: Show Benchmark Selector Chips
        if (state.imageSource == "PRESET") {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(KachaBillPresets.PRESETS) { idx, preset ->
                    val isSelected = state.selectedPresetIndex == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) ScannerCyanGlow else WarmSurfaceElevated)
                            .border(
                                1.dp,
                                if (isSelected) ScannerCyan else WarmBorderSubtle,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.selectHandwrittenPreset(idx) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("chip_preset_$idx")
                    ) {
                        Text(
                            text = preset.samplePresetName ?: preset.vendorName,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) ScannerCyan else GhostSilver
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Viewfinder AR Container (Live CameraX Viewfinder OR Document Preview OR Direct OCR Text Editor)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(if (state.highContrastFilter) Color(0xFF0F0E0D) else Color(0xFF181512))
                .border(1.dp, WarmBorder, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (state.imageSource == "TEXT_OCR") {
                // Direct Raw OCR Transcript Editor & Offline Parser Console
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TextFields, contentDescription = null, tint = LaserLime, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "OFFLINE OCR TRANSCRIPT CONSOLE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LaserLime
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Sample Quick-Fill Chips
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(WarmSurfaceElevated)
                                    .border(1.dp, ScannerCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        viewModel.setManualOcrText(
                                            "SHREE GANESH KIRANA\nDate: 20/08/2026\n1. Aata 5kg - 210\n2. Cheeni 2kg - 88\n3. Doodh 1/2L - 33\n4. 1 pav Amul Makkhan - 60\n5. Sarson Tel 1L - 145\nTOTAL: 536\nPichla baki: 450"
                                        )
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Kirana Kacha Slip", fontSize = 9.5.sp, color = ScannerCyan, fontFamily = FontFamily.Monospace)
                            }
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(WarmSurfaceElevated)
                                    .border(1.dp, EmberPeach.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        viewModel.setManualOcrText(
                                            "FRESHMART SUPERMARKET\nTax Invoice #4892\nFortune Sunflower Oil 1L x 1  165.00\nAmul Taaza Milk 500ml x 2  66.00\nTata Salt 1kg x 1  28.00\nKelloggs Corn Flakes 475g x 1  215.00\nSUBTOTAL: 474.00\nCGST 2.5%: 11.85\nSGST 2.5%: 11.85\nTOTAL: 497.70"
                                        )
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Thermal POS Receipt", fontSize = 9.5.sp, color = EmberPeach, fontFamily = FontFamily.Monospace)
                            }
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(WarmSurfaceElevated)
                                    .border(1.dp, LaserLime.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        viewModel.setManualOcrText(
                                            "BLINKIT QUICK COMMERCE\nOrder #BK-91823\nAmul Butter 100g x 1  58.00\nBrown Bread 400g x 1  45.00\nOrganic Eggs 6pcs x 1  72.00\nBananas 500g x 1  35.00\nDelivery Fee: 15.00\nTOTAL AMOUNT: 225.00"
                                        )
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Quick-Commerce Order", fontSize = 9.5.sp, color = LaserLime, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = state.manualOcrInputText,
                        onValueChange = { viewModel.setManualOcrText(it) },
                        placeholder = {
                            Text(
                                text = "Paste or type raw OCR receipt text here...\ne.g.\nAata 5kg 210\nCheeni 2kg 90\nDoodh 1L 66\nTotal: 366",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = GhostSilverMuted
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("input_manual_ocr_text"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = GhostSilver,
                            unfocusedTextColor = GhostSilver,
                            focusedContainerColor = Color(0xFF141210),
                            unfocusedContainerColor = Color(0xFF141210),
                            focusedBorderColor = ScannerCyan,
                            unfocusedBorderColor = WarmBorderSubtle
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            } else if (state.imageSource == "CAMERA" && activeBitmap == null) {
                // Live CameraX Streaming Viewfinder with Flash, Flip Lens & Shutter Snap
                CameraCaptureView(
                    onPhotoCaptured = { bmp ->
                        viewModel.onHandwrittenRealBitmapCaptured(bmp)
                    },
                    onGalleryPickRequested = {
                        galleryLauncher.launch("image/*")
                    },
                    isProcessing = state.isDecrypting,
                    hudTitle = "AR SENSOR • HANDWRITING OCR",
                    modifier = Modifier.fillMaxSize()
                )
            } else if (activeBitmap != null) {
                // Real Captured Photo or Gallery Uploaded Image
                Image(
                    bitmap = activeBitmap.asImageBitmap(),
                    contentDescription = "Captured Receipt Image",
                    modifier = Modifier
                        .fillMaxSize(0.92f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, ScannerCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                // Simulated Paper Receipt Document in Viewfinder
                val activePreset = KachaBillPresets.PRESETS[state.selectedPresetIndex.coerceIn(KachaBillPresets.PRESETS.indices)]

                Box(
                    modifier = Modifier
                        .fillMaxSize(0.85f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (state.highContrastFilter) Color(0xFFE8E5DF) else Color(0xFFF7F4EC))
                        .border(1.dp, Color(0xFFC0B8AC), RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    // Receipt Header & Ruled Lines (Authentic Indian Kirana Kacha Slip)
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "|| श्री ||",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B0000)
                            )
                            Text(
                                text = "ESTIMATE MEMO",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = activePreset.dateString,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color.DarkGray
                            )
                        }

                        Text(
                            text = activePreset.vendorName,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.highContrastFilter) Color.Black else Color(0xFF0D1B2A)
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.Gray)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Simulated Cursive Handwritten Line Items
                        activePreset.items.forEachIndexed { i, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${i + 1}. ${item.rawWrittenText}",
                                    fontFamily = FontFamily.Cursive,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.highContrastFilter) Color.Black else Color(0xFF0B192C)
                                )
                                Text(
                                    text = "₹${item.price.toInt()}",
                                    fontFamily = FontFamily.Cursive,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.highContrastFilter) Color.Black else Color(0xFF0B192C)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.Gray)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TOTAL:",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "₹${activePreset.shopkeeperTotal.toInt()}/-",
                                fontFamily = FontFamily.Cursive,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }

                        if (activePreset.khataOldBalance != null) {
                            Text(
                                text = "Pichla baki: ₹${activePreset.khataOldBalance.toInt()} | Kul baki: ₹${activePreset.khataNewBalance?.toInt() ?: 0}",
                                fontFamily = FontFamily.Cursive,
                                fontSize = 12.sp,
                                color = Color(0xFF8B0000)
                            )
                        }
                    }
                }
            }

            // AR Viewfinder Canvas: Neon Cyan Corner Brackets & Grid Crosshairs
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val pad = 24.dp.toPx()
                val cornerLen = 32.dp.toPx()
                val cyanColor = ScannerCyan

                // Top-Left
                drawLine(cyanColor, Offset(pad, pad), Offset(pad + cornerLen, pad), strokeWidth = 5f)
                drawLine(cyanColor, Offset(pad, pad), Offset(pad, pad + cornerLen), strokeWidth = 5f)

                // Top-Right
                drawLine(cyanColor, Offset(w - pad, pad), Offset(w - pad - cornerLen, pad), strokeWidth = 5f)
                drawLine(cyanColor, Offset(w - pad, pad), Offset(w - pad, pad + cornerLen), strokeWidth = 5f)

                // Bottom-Left
                drawLine(cyanColor, Offset(pad, h - pad), Offset(pad + cornerLen, h - pad), strokeWidth = 5f)
                drawLine(cyanColor, Offset(pad, h - pad), Offset(pad, h - pad - cornerLen), strokeWidth = 5f)

                // Bottom-Right
                drawLine(cyanColor, Offset(w - pad, h - pad), Offset(w - pad - cornerLen, h - pad), strokeWidth = 5f)
                drawLine(cyanColor, Offset(w - pad, h - pad), Offset(w - pad, h - pad - cornerLen), strokeWidth = 5f)

                // Center Reticle
                val cx = w / 2
                val cy = h / 2
                drawLine(cyanColor.copy(alpha = 0.5f), Offset(cx - 16.dp.toPx(), cy), Offset(cx + 16.dp.toPx(), cy), strokeWidth = 2f)
                drawLine(cyanColor.copy(alpha = 0.5f), Offset(cx, cy - 16.dp.toPx()), Offset(cx, cy + 16.dp.toPx()), strokeWidth = 2f)

                // Active Acid Lime Laser Scan Beam
                if (state.isDecrypting) {
                    val laserY = h * laserYRatio
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                LaserLime.copy(alpha = 0.35f),
                                LaserLime,
                                LaserLime.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            startY = laserY - 24.dp.toPx(),
                            endY = laserY + 24.dp.toPx()
                        ),
                        topLeft = Offset(pad, laserY - 18.dp.toPx()),
                        size = Size(w - 2 * pad, 36.dp.toPx())
                    )

                    drawLine(
                        color = LaserLime,
                        start = Offset(pad, laserY),
                        end = Offset(w - pad, laserY),
                        strokeWidth = 4f
                    )
                }
            }

            // High-Contrast / Binarization Filter Active Tag
            if (state.highContrastFilter) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .border(1.dp, LaserLime, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FilterBAndW, contentDescription = null, tint = LaserLime, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "BINARIZATION (INK BOOST)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = LaserLime
                        )
                    }
                }
            }

            // Terminal CRT Output Log Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .border(1.dp, if (state.isDecrypting) LaserLime else ScannerCyan, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.isDecrypting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                color = LaserLime,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (state.isDecrypting) "► FORENSIC EXTRACTION IN PROGRESS..." else "► READY TO CAPTURE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isDecrypting) LaserLime else ScannerCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = state.terminalLog,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = GhostSilver
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Controls: Filter Toggle + Auto-Capture / Decrypt Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash / Binarization Toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(WarmSurfaceElevated)
                    .border(1.dp, WarmBorderSubtle, RoundedCornerShape(12.dp))
                    .clickable { viewModel.toggleHighContrastFilter() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FilterBAndW,
                    contentDescription = "Contrast Filter",
                    tint = if (state.highContrastFilter) LaserLime else GhostSilverMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "INK ENHANCER",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.highContrastFilter) LaserLime else GhostSilver
                    )
                    Text(
                        text = if (state.highContrastFilter) "Active (Binarized)" else "Standard View",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 10.sp,
                        color = GhostSilverMuted
                    )
                }
            }

            // Capture / Trigger AI Decrypt Button
            val isTextMode = state.imageSource == "TEXT_OCR"
            Button(
                onClick = {
                    if (isTextMode) {
                        viewModel.parseDirectOcrText(state.manualOcrInputText)
                    } else {
                        viewModel.triggerHandwrittenCaptureAndDecrypt()
                    }
                },
                enabled = !state.isDecrypting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTextMode) LaserLime else ScannerCyan,
                    contentColor = VoidBlack
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("btn_capture_kacha_bill")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isTextMode) Icons.Default.ReceiptLong else Icons.Default.AutoAwesome,
                        contentDescription = "OCR Action",
                        tint = VoidBlack,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            state.isDecrypting -> "DECRYPTING..."
                            isTextMode -> "PARSE ON-DEVICE OCR"
                            else -> "AUTO-CAPTURE & PARSE"
                        },
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = VoidBlack
                    )
                }
            }
        }
    }
}

/**
 * SCREEN 6: The Reconciliation Sheet (Human-in-the-Loop Validation UI)
 */
@Composable
fun ReconciliationSheetScreen(
    viewModel: CyphrViewModel,
    result: HandwrittenBillResult,
    selectedItemIndex: Int?,
    highContrastFilter: Boolean,
    capturedBitmap: Bitmap? = null
) {
    val colors = MaterialTheme.cyphrColors
    var editingItem by remember { mutableStateOf<Pair<Int, HandwrittenBillItem>?>(null) }
    var zoomScale by remember { mutableFloatStateOf(1f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("screen_reconciliation_sheet")
    ) {
        // Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = LaserLime,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "INVOICE RECONCILIATION",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GhostSilver
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CyberBadge(
                            text = if (result.invoiceType == "PRINTED") "PRINTED POS" else "HANDWRITTEN",
                            color = if (result.invoiceType == "PRINTED") ScannerCyan else EmberOrange
                        )
                    }
                    Text(
                        text = "${result.vendorName} • ${result.items.size} ITEMS EXTRACTED",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = GhostSilverMuted
                    )
                }
            }

            IconButton(
                onClick = { viewModel.closeHandwrittenScanner() },
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(WarmSurfaceElevated)
                    .testTag("btn_close_reconciliation")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = GhostSilver)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // SPLIT VIEW TOP HALF: Receipt Photo Visualizer with Coordinate Crop Indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (highContrastFilter) Color(0xFF0F0E0D) else Color(0xFF1E1B18))
                .border(1.dp, WarmBorder, RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(1f, 3f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (capturedBitmap != null) {
                // Real Captured Receipt View with Zoom and Target Box Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = capturedBitmap.asImageBitmap(),
                        contentDescription = "Reconciliation Receipt",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )

                    // Overlay Active Item Bounding Indicator
                    selectedItemIndex?.let { idx ->
                        if (idx in result.items.indices) {
                            val activeItem = result.items[idx]
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val boxY = size.height * activeItem.cropCoordinateY
                                val boxX = size.width * activeItem.cropCoordinateX
                                val boxWidth = size.width * 0.45f
                                val boxHeight = 24.dp.toPx()

                                drawRoundRect(
                                    color = if (activeItem.isLowConfidence) ScribbleRed else ScannerCyan,
                                    topLeft = Offset((boxX - boxWidth / 2).coerceIn(4f, size.width - boxWidth - 4f), (boxY - boxHeight / 2).coerceIn(4f, size.height - boxHeight - 4f)),
                                    size = Size(boxWidth, boxHeight),
                                    cornerRadius = CornerRadius(6f, 6f),
                                    style = Stroke(width = 3f)
                                )
                            }
                        }
                    }
                }
            } else {
                // Simulated Receipt Canvas Display
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.92f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF7F4EC))
                        .padding(10.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = result.vendorName,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = result.dateString,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = Color.DarkGray
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.LightGray))
                        Spacer(modifier = Modifier.height(4.dp))

                        result.items.forEachIndexed { idx, item ->
                            val isSelected = selectedItemIndex == idx
                            val isUncertain = item.isLowConfidence

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when {
                                            isSelected && isUncertain -> ScribbleRed.copy(alpha = 0.25f)
                                            isSelected -> ScannerCyan.copy(alpha = 0.2f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = when {
                                            isSelected && isUncertain -> ScribbleRed
                                            isSelected -> ScannerCyan
                                            else -> Color.Transparent
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${idx + 1}. ${item.rawWrittenText}",
                                    fontFamily = FontFamily.Cursive,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUncertain) Color(0xFFB00020) else Color.Black
                                )
                                Text(
                                    text = "₹${item.price.toInt()}",
                                    fontFamily = FontFamily.Cursive,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }

            // Top-Right Crop / Zoom Indicator Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ZoomIn, contentDescription = null, tint = ScannerCyan, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PINCH TO ZOOM (${String.format(Locale.ROOT, "%.1fx", zoomScale)})", fontSize = 8.sp, color = ScannerCyan, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ARITHMETIC MISMATCH ALERT BANNER
        if (result.mathErrorFlag) {
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("banner_math_error"),
                borderColor = ScribbleRed,
                backgroundColor = WarmSurfaceElevated,
                cornerCut = 14.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Math Error",
                            tint = ScribbleRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ARITHMETIC MISMATCH DETECTED (BAD MATH)",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ScribbleRed
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Written by Shopkeeper:",
                                fontSize = 10.sp,
                                color = GhostSilverMuted
                            )
                            Text(
                                text = "₹${result.shopkeeperTotal.toInt()}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ScribbleRed,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "True Mathematical Sum:",
                                fontSize = 10.sp,
                                color = GhostSilverMuted
                            )
                            Text(
                                text = "₹${result.calculatedTrueTotal.toInt()} (Delta: ${if (result.mathErrorDelta > 0) "+₹${result.mathErrorDelta.toInt()}" else "-₹${(-result.mathErrorDelta).toInt()}"})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = LaserLime
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.applyTrueTotalCorrection() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LaserLime,
                                contentColor = VoidBlack
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.3f).height(36.dp).testTag("btn_correct_to_true_total")
                        ) {
                            Text("USE TRUE SUM (₹${result.calculatedTrueTotal.toInt()})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = VoidBlack)
                        }

                        OutlinedButton(
                            onClick = { viewModel.keepShopkeeperWrittenTotal() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(36.dp).testTag("btn_keep_written_total")
                        ) {
                            Text("KEEP WRITTEN", fontSize = 10.sp, color = GhostSilver)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // KHATA (VENDOR LEDGER) RUNNING BALANCE SYNC CARD
        if (result.khataOldBalance != null) {
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_khata_ledger_sync"),
                borderColor = EmberOrange,
                backgroundColor = WarmSurfaceElevated,
                cornerCut = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Khata Ledger",
                            tint = EmberOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "KHATA LEDGER SYNC (Pichla Baki)",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GhostSilver
                            )
                            Text(
                                text = "Previous Debt: ₹${result.khataOldBalance.toInt()} + Today: ₹${result.calculatedTrueTotal.toInt()}",
                                fontSize = 9.sp,
                                color = GhostSilverMuted
                            )
                        }
                    }

                    CyberBadge(
                        text = "NEW BAKI: ₹${result.khataNewBalance?.toInt() ?: (result.khataOldBalance + result.calculatedTrueTotal).toInt()}",
                        color = EmberOrange,
                        backgroundColor = WarmCard
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // SPLIT VIEW BOTTOM HALF: Editable Data Grid with Confidence Highlighting & Add Item action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STANDARDIZED COMMODITIES DATA GRID (${result.items.size})",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = GhostSilverMuted
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Add Item Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(WarmSurfaceElevated)
                        .border(1.dp, LaserLime.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .clickable { viewModel.addNewItemToReconciliation() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("btn_add_line_item")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = LaserLime, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "+ ADD ITEM",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = LaserLime
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(result.items) { idx, item ->
                val isSelected = selectedItemIndex == idx
                val isLowConfidence = item.isLowConfidence

                CyberCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.selectReconciliationItem(idx)
                            editingItem = Pair(idx, item)
                        }
                        .testTag("item_reconciliation_$idx"),
                    borderColor = when {
                        isLowConfidence -> ScribbleRed
                        isSelected -> ScannerCyan
                        else -> WarmBorderSubtle
                    },
                    backgroundColor = if (isSelected) WarmSurfaceElevated else WarmCard,
                    cornerCut = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.canonicalName,
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLowConfidence) ScribbleRed else GhostSilver
                                    )
                                    if (item.brand.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "[${item.brand}]",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            color = EmberPeach
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Written: \"${item.rawWrittenText}\" • ${item.quantity} ${item.unit} • ${item.category}",
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 10.sp,
                                        color = GhostSilverMuted
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₹${item.price.toInt()}",
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLowConfidence) ScribbleRed else EmberOrange
                                    )

                                    if (item.quickCommerceRefPrice != null && item.quickCommerceRefPrice > item.price) {
                                        val savings = (item.quickCommerceRefPrice - item.price).toInt()
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                                contentDescription = "Saved",
                                                tint = LaserLime,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "Save ₹$savings vs Quick-Comm",
                                                fontSize = 8.sp,
                                                color = LaserLime,
                                                fontFamily = FontFamily.SansSerif
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = { viewModel.deleteItemFromReconciliation(idx) },
                                    modifier = Modifier.size(24.dp).testTag("btn_delete_item_$idx")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Item",
                                        tint = GhostSilverMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        // Confidence Alert Banner for Scribbled / Illegible Items
                        if (isLowConfidence) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ScribbleRed.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = ScribbleRed, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SCRIBBLE UNCERTAIN (Confidence: ${(item.confidenceScore * 100).toInt()}%) • Tap to edit",
                                    fontSize = 9.sp,
                                    color = ScribbleRed,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Post-Scan Decision Action Panel
        val finalAmount = if (result.shopkeeperTotal > 0.0) result.shopkeeperTotal else result.calculatedTrueTotal
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(WarmSurfaceElevated)
                .border(1.dp, WarmBorderSubtle, RoundedCornerShape(14.dp))
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "POST-SCAN INVOICE DECISION",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScannerCyan
                )
                Text(
                    text = "${result.items.size} ITEMS • ₹${finalAmount.toInt()}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = LaserLime
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Decision 1: Record as Purchase
                Button(
                    onClick = { viewModel.commitHandwrittenBillToLedger(result) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmberOrange,
                        contentColor = VoidBlack
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_record_as_purchase")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = VoidBlack,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "RECORD PURCHASE",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = VoidBlack
                            )
                            Text(
                                text = "Log expense (₹${finalAmount.toInt()})",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 8.sp,
                                color = VoidBlack.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Decision 2: Add to Shopping List
                Button(
                    onClick = { viewModel.addReconciliationItemsToShoppingList(result) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ScannerCyan,
                        contentColor = VoidBlack
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_add_to_shopping_list")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = VoidBlack,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "ADD TO SHOPPING LIST",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = VoidBlack
                            )
                            Text(
                                text = "Save to buy later",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 8.sp,
                                color = VoidBlack.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog for Editing an Item
    editingItem?.let { (idx, item) ->
        EditLineItemDialog(
            item = item,
            onDismiss = { editingItem = null },
            onSave = { updated ->
                viewModel.updateReconciliationItem(idx, updated)
                editingItem = null
            }
        )
    }
}

/**
 * Tap-to-Edit Dialog for Human-in-the-Loop Corrections
 */
@Composable
fun EditLineItemDialog(
    item: HandwrittenBillItem,
    onDismiss: () -> Unit,
    onSave: (HandwrittenBillItem) -> Unit
) {
    var name by remember { mutableStateOf(item.canonicalName) }
    var brand by remember { mutableStateOf(item.brand) }
    var qty by remember { mutableStateOf(item.quantity.toString()) }
    var unit by remember { mutableStateOf(item.unit) }
    var price by remember { mutableStateOf(item.price.toString()) }
    var category by remember { mutableStateOf(item.category) }

    Dialog(onDismissRequest = onDismiss) {
        CyberCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dialog_edit_line_item"),
            borderColor = ScannerCyan,
            backgroundColor = WarmCard,
            cornerCut = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EDIT COMMODITY ITEM",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GhostSilver
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = GhostSilver)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Raw Written Text: \"${item.rawWrittenText}\"",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = EmberPeach
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Canonical Product Name", fontSize = 11.sp) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ScannerCyan,
                        unfocusedBorderColor = WarmBorder,
                        focusedTextColor = GhostSilver,
                        unfocusedTextColor = GhostSilver
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = qty,
                        onValueChange = { qty = it },
                        label = { Text("Qty", fontSize = 11.sp) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScannerCyan,
                            unfocusedBorderColor = WarmBorder,
                            focusedTextColor = GhostSilver,
                            unfocusedTextColor = GhostSilver
                        )
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (kg/g/L/ml)", fontSize = 11.sp) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScannerCyan,
                            unfocusedBorderColor = WarmBorder,
                            focusedTextColor = GhostSilver,
                            unfocusedTextColor = GhostSilver
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Price (₹)", fontSize = 11.sp) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScannerCyan,
                            unfocusedBorderColor = WarmBorder,
                            focusedTextColor = GhostSilver,
                            unfocusedTextColor = GhostSilver
                        )
                    )

                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Brand", fontSize = 11.sp) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScannerCyan,
                            unfocusedBorderColor = WarmBorder,
                            focusedTextColor = GhostSilver,
                            unfocusedTextColor = GhostSilver
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val parsedQty = qty.toDoubleOrNull() ?: item.quantity
                        val parsedPrice = price.toDoubleOrNull() ?: item.price
                        onSave(
                            item.copy(
                                canonicalName = name,
                                brand = brand,
                                quantity = parsedQty,
                                unit = unit,
                                price = parsedPrice,
                                isLowConfidence = false, // User verified
                                confidenceScore = 1.0f
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ScannerCyan,
                        contentColor = VoidBlack
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text("SAVE CORRECTION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VoidBlack)
                }
            }
        }
    }
}
