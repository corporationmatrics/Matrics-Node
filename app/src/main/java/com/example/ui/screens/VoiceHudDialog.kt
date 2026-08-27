package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.RegionalLanguage
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberCard
import com.example.ui.components.NeonWaveform
import com.example.ui.theme.AcidLime
import com.example.ui.theme.CyberRed
import com.example.ui.theme.EmberPeach
import com.example.ui.theme.LaserLime
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ScannerCyan
import com.example.ui.theme.SteelGrey
import com.example.ui.theme.VoidBlack
import com.example.ui.theme.cyphrColors
import com.example.ui.viewmodel.CyphrViewModel
import com.example.ui.viewmodel.VoiceConversationState
import java.util.Locale

/**
 * UPGRADED VOICE HUD: Interactive Conversational Terminal
 * Bidirectional conversational flow:
 * 1. Listening (Live real-time stream + dynamic audio waveform)
 * 2. Parsed & Audio Confirmation (TTS verbal confirmation + item extraction cards)
 * 3. Action Listening (Two-phase keyword intent router: "Expense" vs "Shopping List")
 * 4. Commit & Feedback (Settlement into Room DB + auditory/haptic feedback)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceHudDialog(
    viewModel: CyphrViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = MaterialTheme.cyphrColors
    val hudState by viewModel.voiceHudState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current

    if (!hudState.isVisible) return

    // Runtime microphone permission launcher
    var hasRecordAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasRecordAudioPermission = isGranted
            if (isGranted) {
                viewModel.startListeningToRealAudio()
            }
        }
    )

    LaunchedEffect(Unit) {
        if (!hasRecordAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Pulsing animation for active mic & TTS rings
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (hudState.isListening || hudState.isTtsSpeaking) 1.2f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Waveform active color based on bidirectional conversational state
    val currentWaveColor = when (hudState.conversationState) {
        VoiceConversationState.TTS_SPEAKING_CONFIRM -> ScannerCyan
        VoiceConversationState.AWAITING_DISPATCH -> AcidLime
        VoiceConversationState.COMMITTED -> AcidLime
        VoiceConversationState.PARSING_ENTITIES,
        VoiceConversationState.PROCESSING_INTENT -> colors.emberOrange
        else -> if (hudState.isListening) AcidLime else colors.steelGrey
    }

    Dialog(
        onDismissRequest = { viewModel.closeVoiceHud() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE00A0B0E)) // Obsidian dark blur backdrop
                .padding(horizontal = 10.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.98f)
                    .testTag("voice_hud_modal"),
                borderColor = when (hudState.conversationState) {
                    VoiceConversationState.TTS_SPEAKING_CONFIRM -> ScannerCyan.copy(alpha = 0.8f)
                    VoiceConversationState.AWAITING_DISPATCH -> AcidLime.copy(alpha = 0.8f)
                    VoiceConversationState.COMMITTED -> AcidLime
                    else -> colors.warmBorder
                },
                backgroundColor = colors.warmCard,
                cornerCut = 20.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    // Header Bar with AI Mode, Conversational Badge & Close Button
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
                                    .background(currentWaveColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (hudState.conversationState) {
                                    VoiceConversationState.TTS_SPEAKING_CONFIRM -> "CONVERSATIONAL // TTS CONFIRM"
                                    VoiceConversationState.AWAITING_DISPATCH -> "CONVERSATIONAL // ACTION ROUTER"
                                    VoiceConversationState.COMMITTED -> "CONVERSATIONAL // COMMITTED"
                                    else -> "CONVERSATIONAL AGENT // ${hudState.selectedLanguage.name}"
                                },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = currentWaveColor
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Offline Whisper STT Toggle Chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (hudState.isOfflineMode) ScannerCyan.copy(alpha = 0.18f) else colors.warmSurfaceElevated)
                                    .border(0.6.dp, if (hudState.isOfflineMode) ScannerCyan else colors.warmBorderSubtle, RoundedCornerShape(6.dp))
                                    .clickable { viewModel.toggleOfflineWhisperMode() }
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                                    .testTag("toggle_whisper_offline_btn")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (hudState.isOfflineMode) Icons.Default.CloudOff else Icons.Default.Wifi,
                                        contentDescription = "Offline Mode",
                                        tint = if (hudState.isOfflineMode) ScannerCyan else colors.ghostSilverMuted,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (hudState.isOfflineMode) "WHISPER OFFLINE" else "HYBRID STT",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hudState.isOfflineMode) ScannerCyan else colors.ghostSilverMuted
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(5.dp))

                            IconButton(
                                onClick = { viewModel.closeVoiceHud() },
                                modifier = Modifier.size(28.dp).testTag("close_voice_hud")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close HUD",
                                    tint = colors.ghostSilver,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // REGIONAL LANGUAGE & DIALECT SELECTOR CAROUSEL
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RegionalLanguage.values().forEach { lang ->
                            val isSelected = hudState.selectedLanguage == lang
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) colors.emberOrange else colors.warmSurfaceElevated)
                                    .border(
                                        0.8.dp,
                                        if (isSelected) colors.emberOrange else colors.warmBorderSubtle,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.setVoiceLanguage(lang) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("lang_tab_${lang.code}")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = lang.nativeLabel,
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) VoidBlack else colors.ghostSilver
                                    )
                                    Text(
                                        text = lang.displayName.split("/").first().trim(),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 7.5.sp,
                                        color = if (isSelected) VoidBlack.copy(alpha = 0.8f) else colors.ghostSilverMuted
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Live Reactive Waveform (Neon Cyan / Acid Lime / Ember Orange / Steel Grey)
                    NeonWaveform(
                        waveLevel = hudState.audioWaveformLevel,
                        isActive = hudState.isListening || hudState.isTtsSpeaking,
                        activeColor = currentWaveColor
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Bidirectional Interactive Controller Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.warmSurfaceElevated)
                            .border(1.dp, currentWaveColor.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mic / Audio Pulsing Action Indicator
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    if (hudState.isTtsSpeaking) ScannerCyan
                                    else if (hudState.isListening) AcidLime
                                    else colors.warmCard
                                )
                                .border(1.5.dp, currentWaveColor, CircleShape)
                                .clickable {
                                    if (hudState.isListening) {
                                        viewModel.stopListeningToRealAudio()
                                    } else {
                                        if (hasRecordAudioPermission) {
                                            viewModel.startListeningToRealAudio()
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                }
                                .testTag("voice_hud_mic_toggle_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    hudState.isTtsSpeaking -> Icons.Default.VolumeUp
                                    hudState.isListening -> Icons.Default.Mic
                                    else -> Icons.Default.MicOff
                                },
                                contentDescription = "Conversational State",
                                tint = if (hudState.isListening || hudState.isTtsSpeaking) VoidBlack else colors.emberOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (hudState.conversationState) {
                                    VoiceConversationState.TTS_SPEAKING_CONFIRM -> "STEP 2: AUDIO CONFIRMATION"
                                    VoiceConversationState.AWAITING_DISPATCH -> "STEP 3: ACTION LISTENING // SAY INTENT"
                                    VoiceConversationState.COMMITTED -> "STEP 4: SETTLED // SUCCESS"
                                    VoiceConversationState.PARSING_ENTITIES -> "STEP 1: RECOGNIZING ENTITIES"
                                    else -> if (hudState.isListening) "STEP 1: LISTENING // ${hudState.selectedLanguage.displayName}" else "TAP MIC OR SAY ITEMS"
                                },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentWaveColor
                            )
                            Text(
                                text = hudState.statusMessage,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.ghostSilver,
                                maxLines = 2
                            )
                        }

                        if (hudState.isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = colors.emberOrange,
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    // TTS Spoken Prompt Highlight Card (Active Dialogue Display)
                    if (hudState.ttsPromptText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ScannerCyan.copy(alpha = 0.12f))
                                .border(0.8.dp, ScannerCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                .testTag("tts_spoken_dialogue_card")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Agent Prompt",
                                    tint = ScannerCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "\"${hudState.ttsPromptText}\"",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ScannerCyan
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Live Monospace Transcript Header / Text Field
                    OutlinedTextField(
                        value = hudState.rawTranscript,
                        onValueChange = { newText ->
                            viewModel.updateVoiceTranscript(newText, isFinal = true)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_transcript_input"),
                        placeholder = {
                            Text(
                                text = "e.g. \"5 kg rice at 250 and Amul butter 500g at 100\"",
                                fontSize = 10.5.sp,
                                color = colors.ghostSilverMuted
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (hudState.rawTranscript.isNotBlank()) {
                                    IconButton(
                                        onClick = { viewModel.updateVoiceTranscript("", isFinal = true) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear text",
                                            tint = colors.ghostSilverMuted,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            keyboardController?.hide()
                                            viewModel.updateVoiceTranscript(hudState.rawTranscript, isFinal = true)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Re-parse",
                                            tint = colors.emberOrange,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        keyboardController?.hide()
                                        viewModel.updateVoiceTranscript(hudState.rawTranscript, isFinal = true)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Parse prompt",
                                        tint = colors.emberOrange,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.emberOrange,
                            unfocusedBorderColor = colors.warmBorderSubtle,
                            focusedTextColor = colors.ghostSilver,
                            unfocusedTextColor = colors.ghostSilver,
                            focusedContainerColor = colors.warmSurfaceElevated,
                            unfocusedContainerColor = colors.warmSurfaceElevated
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()
                            viewModel.updateVoiceTranscript(hudState.rawTranscript, isFinal = true)
                        })
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Structured Summary Bar (Vendor, Location & Live Budget Warning)
                    if (hudState.parsedItems.isNotEmpty() || hudState.rawTranscript.isNotBlank()) {
                        CyberCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = colors.warmBorder,
                            backgroundColor = colors.warmSurfaceElevated,
                            cornerCut = 10.dp
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Store,
                                            contentDescription = "Vendor",
                                            tint = colors.emberOrange,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = hudState.detectedVendor,
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.emberOrange
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "GPS Pin",
                                            tint = colors.ghostSilverMuted,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = hudState.geoPin,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 8.5.sp,
                                            color = colors.ghostSilverMuted
                                        )
                                    }
                                }

                                val liveWarning = hudState.liveBudgetWarning
                                if (liveWarning != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (liveWarning.startsWith("🚨")) CyberRed.copy(alpha = 0.15f) else NeonAmber.copy(alpha = 0.15f))
                                            .border(0.6.dp, if (liveWarning.startsWith("🚨")) CyberRed else NeonAmber, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 7.dp, vertical = 4.dp)
                                            .testTag("voice_hud_live_budget_warning")
                                    ) {
                                        Text(
                                            text = liveWarning,
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (liveWarning.startsWith("🚨")) CyberRed else NeonAmber
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Parsed Item List Header & Subtotal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EXTRACTED ENTITIES (${hudState.parsedItems.size})",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                            letterSpacing = 0.8.sp,
                            color = colors.ghostSilverMuted,
                            fontWeight = FontWeight.Bold
                        )

                        if (hudState.parsedItems.isNotEmpty()) {
                            val subtotal = hudState.parsedItems.sumOf { it.price * it.quantity }
                            Text(
                                text = "TOTAL: ₹${String.format(Locale.ROOT, "%.0f", subtotal)}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.emberOrange
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Extracted Entity Cards List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (hudState.parsedItems.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.warmSurfaceElevated)
                                        .border(0.6.dp, colors.warmBorderSubtle, RoundedCornerShape(10.dp))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = null,
                                            tint = if (hudState.isListening) AcidLime else colors.ghostSilverMuted,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = if (hudState.isListening) "🎙️ Listening... Speak items, quantities, and prices" else "Tap microphone to speak or tap a regional sample below",
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (hudState.isListening) AcidLime else colors.ghostSilverMuted,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "e.g. \"Rice 5 kg at 250 and Amul Butter 500g at 100\"",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.5.sp,
                                            color = colors.steelGrey,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        itemsIndexed(hudState.parsedItems) { index, item ->
                            CyberCard(
                                modifier = Modifier.fillMaxWidth(),
                                borderColor = if (item.price <= 0.0) NeonAmber else colors.warmBorderSubtle,
                                backgroundColor = colors.warmSurfaceElevated,
                                cornerCut = 10.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.ghostSilver
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            if (item.brand.isNotBlank()) {
                                                CyberBadge(
                                                    text = item.brand,
                                                    color = colors.emberOrange,
                                                    backgroundColor = colors.warmCard
                                                )
                                            }
                                            CyberBadge(
                                                text = item.category,
                                                color = colors.ghostSilver,
                                                backgroundColor = colors.warmCard
                                            )
                                            CyberBadge(
                                                text = "${if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()} ${item.unit}",
                                                color = colors.ghostSilverMuted,
                                                backgroundColor = colors.warmCard
                                            )
                                            if (item.price <= 0.0) {
                                                CyberBadge(
                                                    text = "PRICE NEEDED",
                                                    color = NeonAmber,
                                                    backgroundColor = colors.warmCard
                                                )
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (item.price > 0) "₹${String.format(Locale.ROOT, "%.0f", item.price * item.quantity)}" else "₹0 (List)",
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.price > 0) colors.emberOrange else colors.steelGrey
                                        )
                                        if (item.price > 0) {
                                            Text(
                                                text = "@ ₹${item.price.toInt()}/${item.unit}",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 8.5.sp,
                                                color = colors.ghostSilverMuted
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteVoiceParsedItem(index) },
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete item",
                                                tint = colors.steelGrey,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Regional Dialect spoken presets header and chips
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${hudState.selectedLanguage.displayName.uppercase()} SPOKEN PROMPTS (TAP TO TEST):",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                letterSpacing = 0.5.sp,
                                color = colors.ghostSilverMuted
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                        }

                        itemsIndexed(hudState.selectedLanguage.samplePhrases) { _, prompt ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.warmSurfaceElevated)
                                    .border(0.6.dp, colors.warmBorderSubtle, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.simulateSpeechStream(prompt) }
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                                ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Simulate",
                                        tint = colors.emberOrange,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "\"$prompt\"",
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 10.5.sp,
                                        color = colors.ghostSilver
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Conversational Intent Hints (Step 3 Guidance)
                    if (hudState.conversationState == VoiceConversationState.AWAITING_DISPATCH || hudState.parsedItems.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.warmSurfaceElevated)
                                .border(0.6.dp, AcidLime.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🗣️ Voice Intents: Say \"Expense\" or \"Shopping List\"",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AcidLime
                                )
                                Text(
                                    text = "Touch Override Active",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp,
                                    color = colors.steelGrey
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                    }

                    // DUAL ACTION COMMIT BUTTONS / TOUCH OVERRIDES
                    val grandTotal = hudState.parsedItems.sumOf { it.price * it.quantity }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // Retail POS Action 1: Create POS Sale Bill
                        Button(
                            onClick = { viewModel.populatePosCartFromVoiceHud() },
                            enabled = hudState.parsedItems.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = VoidBlack,
                                disabledContainerColor = colors.warmSurfaceElevated,
                                disabledContentColor = colors.steelGrey
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("voice_create_pos_sale_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = "POS Sale",
                                    tint = if (hudState.parsedItems.isNotEmpty()) VoidBlack else colors.steelGrey,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (hudState.parsedItems.isNotEmpty()) {
                                        "CREATE POS SALE (₹${String.format(Locale.ROOT, "%.0f", grandTotal)})"
                                    } else {
                                        "CREATE POS SALE"
                                    },
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    letterSpacing = 0.5.sp,
                                    color = if (hudState.parsedItems.isNotEmpty()) VoidBlack else colors.steelGrey
                                )
                            }
                        }

                        // Retail POS Action 2: Inward Restock Inventory
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { viewModel.executeRestockFromVoiceHud() },
                                enabled = hudState.parsedItems.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AcidLime,
                                    contentColor = VoidBlack,
                                    disabledContainerColor = colors.warmSurfaceElevated,
                                    disabledContentColor = colors.steelGrey
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("voice_inward_restock_btn")
                            ) {
                                Text(
                                    text = "📦 RESTOCK INVENTORY",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    color = if (hudState.parsedItems.isNotEmpty()) VoidBlack else colors.steelGrey
                                )
                            }

                            // Action 3: Expense / Shopping List
                            OutlinedButton(
                                onClick = { viewModel.confirmVoiceHudTransaction() },
                                enabled = hudState.parsedItems.isNotEmpty(),
                                shape = RoundedCornerShape(10.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(
                                        if (hudState.parsedItems.isNotEmpty()) colors.emberOrange.copy(alpha = 0.8f) else colors.warmBorderSubtle
                                    )
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = colors.warmSurfaceElevated,
                                    contentColor = colors.ghostSilver,
                                    disabledContainerColor = colors.warmSurfaceElevated.copy(alpha = 0.5f),
                                    disabledContentColor = colors.steelGrey
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("one_tap_confirm_voice_btn")
                            ) {
                                Text(
                                    text = "LOG EXPENSE",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    color = if (hudState.parsedItems.isNotEmpty()) colors.emberOrange else colors.steelGrey
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
