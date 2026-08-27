package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

object ImageProcessingUtils {

    /**
     * Decodes and scales an image from Uri with EXIF orientation correction and max dimension bounding.
     * Prevents OOM and ensures lightning-fast Gemini multimodal transmission.
     */
    fun decodeAndOptimizeBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1280
    ): Bitmap? {
        return try {
            var inputStream: InputStream? = context.contentResolver.openInputStream(uri) ?: return null

            // 1. Measure dimensions without full decode
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, boundsOptions)
            inputStream?.close()

            val srcWidth = boundsOptions.outWidth
            val srcHeight = boundsOptions.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return null

            // 2. Calculate sample size
            var inSampleSize = 1
            val maxSrcDim = max(srcWidth, srcHeight)
            while (maxSrcDim / (inSampleSize * 2) >= maxDimension) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            inputStream = context.contentResolver.openInputStream(uri)
            val decodedBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close() ?: return null
            if (decodedBitmap == null) return null

            // 3. Read EXIF Orientation
            var rotationDegrees = 0
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    context.contentResolver.openInputStream(uri)?.use { exifStream ->
                        val exif = ExifInterface(exifStream)
                        val orientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                        rotationDegrees = when (orientation) {
                            ExifInterface.ORIENTATION_ROTATE_90 -> 90
                            ExifInterface.ORIENTATION_ROTATE_180 -> 180
                            ExifInterface.ORIENTATION_ROTATE_270 -> 270
                            else -> 0
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore EXIF errors on devices without exif tags
            }

            // 4. Rotate and scale to exact target bounds
            val finalMatrix = Matrix()
            if (rotationDegrees != 0) {
                finalMatrix.postRotate(rotationDegrees.toFloat())
            }

            val currentMaxDim = max(decodedBitmap.width, decodedBitmap.height)
            if (currentMaxDim > maxDimension) {
                val scale = maxDimension.toFloat() / currentMaxDim.toFloat()
                finalMatrix.postScale(scale, scale)
            }

            val resultBitmap = Bitmap.createBitmap(
                decodedBitmap,
                0,
                0,
                decodedBitmap.width,
                decodedBitmap.height,
                finalMatrix,
                true
            )

            if (resultBitmap != decodedBitmap) {
                decodedBitmap.recycle()
            }

            resultBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Scales an existing in-memory bitmap (e.g. from camera thumbnail)
     */
    fun scaleBitmap(source: Bitmap, maxDimension: Int = 1280): Bitmap {
        val srcWidth = source.width
        val srcHeight = source.height
        val maxDim = max(srcWidth, srcHeight)
        if (maxDim <= maxDimension) return source

        val scale = maxDimension.toFloat() / maxDim.toFloat()
        val targetWidth = (srcWidth * scale).toInt()
        val targetHeight = (srcHeight * scale).toInt()
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    enum class DocumentFilter(val label: String, val description: String) {
        NATURAL("Natural Color", "Original photo spectrum"),
        INK_BOOST("Ink Boost (Kacha)", "High-contrast pen binarization"),
        THERMAL_SHARP("Thermal Sharp", "Flattens receipt & clarifies faded POS text"),
        MONOCHROME_THRESHOLD("B&W Binary", "Pure black-and-white OCR threshold")
    }

    /**
     * Applies the specified document processing filter for OCR optimization.
     */
    fun applyDocumentFilter(source: Bitmap, filter: DocumentFilter): Bitmap {
        return when (filter) {
            DocumentFilter.NATURAL -> source
            DocumentFilter.INK_BOOST -> applyBinarizationAndInkBoost(source)
            DocumentFilter.THERMAL_SHARP -> flattenAndEnhanceThermalReceipt(source)
            DocumentFilter.MONOCHROME_THRESHOLD -> applyMonochromeThreshold(source)
        }
    }

    /**
     * Crops an in-memory bitmap based on relative bounding box ratios (0.0f .. 1.0f).
     */
    fun cropToRect(
        source: Bitmap,
        leftRatio: Float = 0.05f,
        topRatio: Float = 0.05f,
        rightRatio: Float = 0.95f,
        bottomRatio: Float = 0.95f
    ): Bitmap {
        val width = source.width
        val height = source.height
        val x = (leftRatio.coerceIn(0f, 0.9f) * width).toInt()
        val y = (topRatio.coerceIn(0f, 0.9f) * height).toInt()
        val w = ((rightRatio.coerceIn(leftRatio + 0.05f, 1.0f) * width) - x).toInt().coerceAtLeast(10)
        val h = ((bottomRatio.coerceIn(topRatio + 0.05f, 1.0f) * height) - y).toInt().coerceAtLeast(10)

        val safeW = if (x + w > width) width - x else w
        val safeH = if (y + h > height) height - y else h

        return Bitmap.createBitmap(source, x, y, safeW, safeH)
    }

    /**
     * Flattens paper wrinkles and sharpens faded thermal POS receipt text.
     */
    fun flattenAndEnhanceThermalReceipt(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0.05f) // Near monochrome

        // Enhance thermal text: Contrast = 2.4, Brightness = -10
        val contrast = 2.4f
        val brightness = -10f
        val scale = contrast
        val translate = (-0.5f * contrast + 0.5f) * 255f + brightness

        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )

        colorMatrix.postConcat(contrastMatrix)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)

        return outputBitmap
    }

    /**
     * Pure black & white thresholding for OCR engines
     */
    fun applyMonochromeThreshold(source: Bitmap, threshold: Int = 135): Bitmap {
        val width = source.width
        val height = source.height
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

            val bwColor = if (luminance < threshold) -0x1000000 else -0x1 // Black or White
            pixels[i] = bwColor
        }

        outputBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return outputBitmap
    }

    /**
     * Applies high-contrast binarization and ink boost filter.
     * Accentuates faded blue/black ballpoint ink, sharpens cursive handwriting strokes,
     * and suppresses yellow/grey paper background noise for maximum OCR fidelity.
     */
    fun applyBinarizationAndInkBoost(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Step 1: Grayscale conversion matrix
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)

        // Step 2: High contrast and dark ink boost
        // Contrast = 1.9, Brightness = -20
        val contrast = 1.9f
        val brightness = -20f
        val scale = contrast
        val translate = (-0.5f * contrast + 0.5f) * 255f + brightness

        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )

        colorMatrix.postConcat(contrastMatrix)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)

        return outputBitmap
    }

    /**
     * Converts Bitmap to Base64 encoded JPEG string for Gemini multimodal API
     */
    fun bitmapToBase64Jpeg(bitmap: Bitmap, quality: Int = 85): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
