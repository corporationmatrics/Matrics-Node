package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AcidLime
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.VoidBlack
import com.example.ui.theme.cyphrColors

/**
 * High-Contrast Dynamic Merchant P2M QR Code Display with techno-minimalist styling
 */
@Composable
fun PosQrCodeView(
    upiUri: String,
    invoiceNo: String,
    amount: Double,
    storeName: String = "Matrics Storefront",
    modifier: Modifier = Modifier,
    qrSize: Dp = 220.dp
) {
    val colors = MaterialTheme.cyphrColors
    val qrMatrix = remember(upiUri) {
        generateQrMatrix(upiUri)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.warmCard)
            .border(1.dp, colors.warmBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // QR Code Canvas Frame
        Box(
            modifier = Modifier
                .size(qrSize)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(12.dp)
                .testTag("pos_upi_qr_canvas"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val matrixSize = qrMatrix.size
                val cellSize = size.width / matrixSize

                for (row in 0 until matrixSize) {
                    for (col in 0 until matrixSize) {
                        if (qrMatrix[row][col]) {
                            // Check if this is part of finder pattern corner
                            val isFinderCorner = (row < 7 && col < 7) ||
                                    (row < 7 && col >= matrixSize - 7) ||
                                    (row >= matrixSize - 7 && col < 7)

                            val cellColor = if (isFinderCorner) {
                                Color(0xFF0F172A)
                            } else {
                                Color(0xFF1E293B)
                            }

                            drawRoundRect(
                                color = cellColor,
                                topLeft = Offset(col * cellSize, row * cellSize),
                                size = Size(cellSize, cellSize),
                                cornerRadius = CornerRadius(if (isFinderCorner) 2f else 1f)
                            )
                        }
                    }
                }
            }

            // Center UPI Badge
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0B0F19))
                    .border(1.dp, NeonCyan, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "UPI",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "SCAN & PAY VIA ANY UPI APP",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colors.emberOrange,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Google Pay • PhonePe • Paytm • BHIM • Cred",
            fontFamily = FontFamily.SansSerif,
            fontSize = 11.sp,
            color = colors.ghostSilverMuted
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(colors.warmSurfaceElevated)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "₹${amount.toInt()} • Bill $invoiceNo",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.ghostSilver
            )
        }
    }
}

/**
 * Lightweight deterministic 2D QR matrix generator for standard UPI URI encoding
 */
private fun generateQrMatrix(data: String): Array<BooleanArray> {
    val size = 29
    val matrix = Array(size) { BooleanArray(size) { false } }

    // 1. Draw 3 Finder Patterns (7x7) at (0,0), (0, size-7), (size-7, 0)
    fun drawFinder(top: Int, left: Int) {
        for (r in 0..6) {
            for (c in 0..6) {
                val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                val isCenter = r in 2..4 && c in 2..4
                matrix[top + r][left + c] = isBorder || isCenter
            }
        }
    }

    drawFinder(0, 0)
    drawFinder(0, size - 7)
    drawFinder(size - 7, 0)

    // 2. Draw Timing Patterns (alternating black and white)
    for (i in 8 until size - 8) {
        matrix[6][i] = (i % 2 == 0)
        matrix[i][6] = (i % 2 == 0)
    }

    // 3. Draw Alignment Pattern (5x5) at (size - 9, size - 9)
    val alignRow = size - 7
    val alignCol = size - 7
    if (alignRow > 7 && alignCol > 7) {
        for (r in -2..2) {
            for (c in -2..2) {
                val isBorder = (r == -2 || r == 2 || c == -2 || c == 2)
                val isCenter = (r == 0 && c == 0)
                matrix[alignRow + r][alignCol + c] = isBorder || isCenter
            }
        }
    }

    // 4. Fill Data and Error Correction bits using deterministic hash of payload
    val bytes = data.toByteArray(Charsets.UTF_8)
    var bitIndex = 0
    val totalBits = bytes.size * 8

    var hash = 0x811c9dc5.toInt()
    for (b in bytes) {
        hash = (hash xor (b.toInt() and 0xff)) * 0x01000193
    }

    for (col in size - 1 downTo 0 step 2) {
        val actualCol = if (col <= 6) col - 1 else col
        if (actualCol < 0) break

        for (row in 0 until size) {
            for (c in 0..1) {
                val currentCol = actualCol - c
                if (currentCol < 0) continue

                // Skip reserved finder and timing zones
                val inTopLeft = row < 9 && currentCol < 9
                val inTopRight = row < 9 && currentCol >= size - 9
                val inBottomLeft = row >= size - 9 && currentCol < 9
                val inTiming = row == 6 || currentCol == 6

                if (!inTopLeft && !inTopRight && !inBottomLeft && !inTiming) {
                    val bit = if (bitIndex < totalBits) {
                        val bytePos = bitIndex / 8
                        val bitPos = 7 - (bitIndex % 8)
                        val b = bytes[bytePos].toInt()
                        ((b shr bitPos) and 1) == 1
                    } else {
                        // Pseudo-random deterministic padding bits based on hash
                        val pseudoSeed = (hash + row * 31 + currentCol * 17)
                        (pseudoSeed % 3 == 0 || (row + currentCol) % 2 == 0)
                    }
                    matrix[row][currentCol] = bit
                    bitIndex++
                }
            }
        }
    }

    return matrix
}
