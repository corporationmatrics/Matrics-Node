package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.GhostSilver
import com.example.ui.theme.GhostSilverMuted
import com.example.ui.theme.LaserLime
import com.example.ui.theme.ScannerCyan
import com.example.ui.theme.ScannerCyanGlow
import com.example.ui.theme.SteelGrey
import com.example.ui.theme.VoidBlack
import com.example.ui.theme.WarmBorder
import com.example.ui.theme.WarmSurfaceElevated
import java.util.concurrent.Executors

/**
 * High-performance CameraX live viewfinder with AR HUD brackets, torch toggle,
 * front/rear camera flip, tap-to-focus, and instant high-res picture capture.
 */
@Composable
fun CameraCaptureView(
    onPhotoCaptured: (Bitmap) -> Unit,
    onGalleryPickRequested: () -> Unit,
    modifier: Modifier = Modifier,
    isProcessing: Boolean = false,
    hudTitle: String = "LIVE AR CAMERA FEED"
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    if (!hasCameraPermission) {
        // Permission Request State
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(VoidBlack)
                .border(1.dp, WarmBorder, RoundedCornerShape(16.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(WarmSurfaceElevated)
                        .border(1.dp, ScannerCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera Permission Required",
                        tint = ScannerCyan,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "CAMERA ACCESS REQUIRED",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = GhostSilver,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Enable live camera to scan handwritten kirana bills, printed receipts, and invoices with real-time AR HUD telemetry.",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.sp,
                    color = GhostSilverMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LaserLime,
                            contentColor = VoidBlack
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_grant_camera_permission")
                    ) {
                        Text(
                            text = "GRANT CAMERA ACCESS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = VoidBlack
                        )
                    }

                    Button(
                        onClick = onGalleryPickRequested,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarmSurfaceElevated,
                            contentColor = GhostSilver
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .border(1.dp, WarmBorder, RoundedCornerShape(12.dp))
                            .testTag("btn_camera_permission_choose_photo")
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = ScannerCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CHOOSE PHOTO",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = GhostSilver
                        )
                    }
                }
            }
        }
        return
    }

    // Live Camera Active Viewport
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(VoidBlack)
            .border(1.dp, ScannerCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        var previewViewInstance by remember { mutableStateOf<PreviewView?>(null) }

        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                previewViewInstance = previewView

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder()
                            .build()
                            .also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .setTargetRotation(previewView.display?.rotation ?: android.view.Surface.ROTATION_0)
                            .build()

                        imageCapture = capture

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()

                        cameraProvider.unbindAll()
                        val boundCamera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            capture
                        )
                        camera = boundCamera

                        // Synchronize torch
                        boundCamera.cameraControl.enableTorch(isTorchEnabled)
                    } catch (exc: Exception) {
                        Log.e("CameraCaptureView", "Use case binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            update = { previewView ->
                previewViewInstance = previewView
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(previewViewInstance, camera) {
                    detectTapGestures { offset ->
                        val pView = previewViewInstance ?: return@detectTapGestures
                        val cam = camera ?: return@detectTapGestures
                        val factory = SurfaceOrientedMeteringPointFactory(
                            pView.width.toFloat(),
                            pView.height.toFloat()
                        )
                        val point = factory.createPoint(offset.x, offset.y)
                        val action = FocusMeteringAction.Builder(point).build()
                        cam.cameraControl.startFocusAndMetering(action)
                    }
                }
        )

        // Live AR Reticle Overlay & Controls
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Top Controls Bar (Telemetry, Torch, Flip Lens)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Telemetry Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(VoidBlack.copy(alpha = 0.75f))
                        .border(1.dp, ScannerCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isCapturing || isProcessing) LaserLime else ScannerCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCapturing || isProcessing) "PROCESSING SENSOR..." else hudTitle,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = if (isCapturing || isProcessing) LaserLime else ScannerCyan
                    )
                }

                // Camera Action Badges (Torch & Lens Switch)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Flash / Torch Toggle
                    IconButton(
                        onClick = {
                            val newTorch = !isTorchEnabled
                            isTorchEnabled = newTorch
                            camera?.cameraControl?.enableTorch(newTorch)
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isTorchEnabled) LaserLime.copy(alpha = 0.25f) else VoidBlack.copy(alpha = 0.7f))
                            .border(1.dp, if (isTorchEnabled) LaserLime else WarmBorder, CircleShape)
                            .testTag("btn_camera_torch_toggle")
                    ) {
                        Icon(
                            imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Torch Toggle",
                            tint = if (isTorchEnabled) LaserLime else GhostSilver,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Flip Front/Back Lens
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                            isTorchEnabled = false
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(VoidBlack.copy(alpha = 0.7f))
                            .border(1.dp, WarmBorder, CircleShape)
                            .testTag("btn_camera_flip_lens")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera Lens",
                            tint = GhostSilver,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Bottom Shutter Controls & Gallery Import
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pick photo from gallery
                IconButton(
                    onClick = onGalleryPickRequested,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(VoidBlack.copy(alpha = 0.75f))
                        .border(1.dp, WarmBorder, CircleShape)
                        .testTag("btn_camera_choose_gallery")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Choose from Gallery",
                        tint = GhostSilver,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Primary Capture Shutter Button
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(VoidBlack.copy(alpha = 0.6f))
                        .border(2.dp, LaserLime, CircleShape)
                        .clickable(enabled = !isCapturing && !isProcessing) {
                            val cap = imageCapture ?: return@clickable
                            isCapturing = true
                            cap.takePicture(
                                cameraExecutor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val rotationDegrees = image.imageInfo.rotationDegrees
                                        val bmp = imageProxyToBitmap(image, rotationDegrees)
                                        image.close()
                                        ContextCompat.getMainExecutor(context).execute {
                                            isCapturing = false
                                            if (bmp != null) {
                                                onPhotoCaptured(bmp)
                                            }
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        Log.e("CameraCaptureView", "Photo capture failed: ${exception.message}", exception)
                                        ContextCompat.getMainExecutor(context).execute {
                                            isCapturing = false
                                        }
                                    }
                                }
                            )
                        }
                        .padding(5.dp)
                        .testTag("btn_camera_shutter"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing || isProcessing) {
                        CircularProgressIndicator(
                            color = LaserLime,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(LaserLime)
                        )
                    }
                }

                // Spacer or Placeholder to balance the row
                Box(modifier = Modifier.size(44.dp))
            }
        }
    }
}

/**
 * Helper to convert CameraX ImageProxy to an oriented Android Bitmap.
 */
private fun imageProxyToBitmap(image: ImageProxy, rotationDegrees: Int): Bitmap? {
    return try {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (bitmap != null && rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    } catch (e: Exception) {
        Log.e("CameraCaptureView", "Error converting imageProxy to bitmap", e)
        null
    }
}
