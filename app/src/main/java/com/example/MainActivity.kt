package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.CyberBottomDock
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BarcodeScannerModal
import com.example.ui.screens.EcosystemModals
import com.example.ui.screens.PosScreen
import com.example.ui.screens.StoreInventoryScreen
import com.example.ui.screens.StorefrontDashboardScreen
import com.example.ui.screens.ThermalPrinterModals
import com.example.ui.screens.VoiceHudDialog
import com.example.ui.theme.CyphrTheme
import com.example.ui.theme.cyphrColors
import com.example.ui.viewmodel.CyphrViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: CyphrViewModel = viewModel()
            val isFieldMode by viewModel.isFieldMode.collectAsStateWithLifecycle()

            CyphrTheme(isFieldMode = isFieldMode) {
                CyphrApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun CyphrApp(
    viewModel: CyphrViewModel
) {
    var currentTab by remember { mutableIntStateOf(0) }
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val colors = MaterialTheme.cyphrColors

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.warmCanvas,
        bottomBar = {
            CyberBottomDock(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                onVoiceFabClick = { viewModel.openVoiceHud() }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> StorefrontDashboardScreen(
                    viewModel = viewModel,
                    onNavigateToPos = { currentTab = 1 },
                    onNavigateToInventory = { currentTab = 2 },
                    onNavigateToKhata = { viewModel.openModal("KHATA_SPLITTER") },
                    onOpenVoiceHud = { viewModel.openVoiceHud() }
                )
                1 -> PosScreen(
                    viewModel = viewModel,
                    onOpenVoiceHud = { viewModel.openVoiceHud() }
                )
                2 -> StoreInventoryScreen(
                    viewModel = viewModel
                )
                3 -> AnalyticsScreen(
                    viewModel = viewModel
                )
            }

            // Voice & Fast-Entry Overlay HUD with signature NLP readout
            VoiceHudDialog(viewModel = viewModel)

            // Barcode & QR Realtime Camera Scanner Modal
            BarcodeScannerModal(viewModel = viewModel)

            // Thermal Printer Receipt & Configuration Modals
            ThermalPrinterModals(viewModel = viewModel)

            // Ecosystem Integration Modals (OCR, Quick-Commerce, Instant Pay)
            EcosystemModals(viewModel = viewModel)

            // Toast Notification Banner
            AnimatedVisibility(
                visible = toastMessage != null,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                toastMessage?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.warmSurfaceElevated)
                            .border(1.dp, colors.warmBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "► $msg",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.emberOrange
                        )
                    }
                }
            }
        }
    }
}
