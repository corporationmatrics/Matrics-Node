package com.example.data.printer

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Paper width configuration for Thermal Receipt Printers.
 */
enum class ThermalPaperSize(val widthMm: Int, val charColumns: Int, val dotWidth: Int) {
    SIZE_58MM(58, 32, 384),  // 2-inch standard (32 characters per line)
    SIZE_80MM(80, 48, 576)   // 3-inch standard (48 characters per line)
}

/**
 * ESC/POS Command Generator for POS Thermal Receipt Printers.
 * Supports standard Bluetooth, USB, Network IP thermal printers (Epson, Star, Citizen, POS-58, POS-80, RETSOL, Everycom, TVS, NGX, etc.).
 */
object EscPosThermalPrinterEngine {

    // ESC/POS Control Codes
    val ESC: Byte = 0x1B
    val FS: Byte = 0x1C
    val GS: Byte = 0x1D
    val LF: Byte = 0x0A
    val CR: Byte = 0x0D

    // Initialize printer
    val CMD_INIT = byteArrayOf(ESC, 0x40)

    // Text Alignment
    val CMD_ALIGN_LEFT = byteArrayOf(ESC, 0x61, 0x00)
    val CMD_ALIGN_CENTER = byteArrayOf(ESC, 0x61, 0x01)
    val CMD_ALIGN_RIGHT = byteArrayOf(ESC, 0x61, 0x02)

    // Font Styles
    val CMD_BOLD_ON = byteArrayOf(ESC, 0x45, 0x01)
    val CMD_BOLD_OFF = byteArrayOf(ESC, 0x45, 0x00)
    val CMD_DOUBLE_HEIGHT_ON = byteArrayOf(GS, 0x21, 0x01)
    val CMD_DOUBLE_WIDTH_ON = byteArrayOf(GS, 0x21, 0x10)
    val CMD_DOUBLE_SIZE_ON = byteArrayOf(GS, 0x21, 0x11)
    val CMD_TEXT_NORMAL = byteArrayOf(GS, 0x21, 0x00)
    val CMD_UNDERLINE_ON = byteArrayOf(ESC, 0x2D, 0x01)
    val CMD_UNDERLINE_OFF = byteArrayOf(ESC, 0x2D, 0x00)
    val CMD_INVERSE_ON = byteArrayOf(GS, 0x42, 0x01)
    val CMD_INVERSE_OFF = byteArrayOf(GS, 0x42, 0x00)

    // Hardware actions
    val CMD_FEED_3 = byteArrayOf(ESC, 0x64, 0x03)
    val CMD_FEED_5 = byteArrayOf(ESC, 0x64, 0x05)
    val CMD_PAPER_CUT = byteArrayOf(GS, 0x56, 0x42, 0x00) // Full / Partial cut
    val CMD_DRAWER_KICK = byteArrayOf(ESC, 0x70, 0x00, 0x19, 0xFA.toByte()) // Open Cash Drawer pulse

    data class StoreReceiptHeader(
        val storeName: String = "MATRICS NODE STORE",
        val tagline: String = "Smart Cloud POS & Khata Terminal",
        val address: String = "Shop #12, Market Complex",
        val phone: String = "+91 98765 43210",
        val gstin: String = "GSTIN: 29AAAAA0000A1Z5",
        val fssai: String = "",
        val taxInvoiceTitle: String = "TAX INVOICE / CASH BILL"
    )

    data class ReceiptLineItem(
        val name: String,
        val qty: Double,
        val unit: String = "pcs",
        val unitPrice: Double,
        val total: Double
    )

    data class ReceiptData(
        val header: StoreReceiptHeader = StoreReceiptHeader(),
        val invoiceNo: String,
        val dateTimestamp: Long = System.currentTimeMillis(),
        val customerName: String = "",
        val customerPhone: String = "",
        val items: List<ReceiptLineItem>,
        val subtotal: Double,
        val discountAmount: Double = 0.0,
        val taxAmount: Double = 0.0,
        val netTotal: Double,
        val paymentMethod: String = "UPI / QR",
        val upiPaymentUri: String? = null,
        val barcodeValue: String = invoiceNo,
        val footerNotes: List<String> = listOf(
            "Thank you for your business!",
            "Goods once sold are subject to store policy.",
            "Powered by Matrics Cyphr POS"
        )
    )

    data class ShiftZReportData(
        val storeName: String = "MATRICS NODE STORE",
        val reportDate: String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()),
        val totalSales: Double,
        val totalBillsCount: Int,
        val cashCollected: Double,
        val upiCollected: Double,
        val khataOutstandingAdded: Double,
        val totalTaxCollected: Double,
        val totalDiscountsGiven: Double,
        val topSellingItems: List<Pair<String, Int>> = emptyList()
    )

    /**
     * Builds ESC/POS byte array for a complete customer sales receipt.
     */
    fun generateCustomerReceiptBytes(
        receipt: ReceiptData,
        paperSize: ThermalPaperSize = ThermalPaperSize.SIZE_58MM,
        includeQrCode: Boolean = true,
        includeBarcode: Boolean = true,
        autoCut: Boolean = true,
        kickDrawer: Boolean = false
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val cols = paperSize.charColumns
        val divider = "-".repeat(cols)
        val doubleDivider = "=".repeat(cols)

        // 1. Initialize
        out.write(CMD_INIT)
        if (kickDrawer) {
            out.write(CMD_DRAWER_KICK)
        }

        // 2. Header (Centered, Bold Store Name)
        out.write(CMD_ALIGN_CENTER)
        out.write(CMD_BOLD_ON)
        out.write(CMD_DOUBLE_SIZE_ON)
        out.write("${receipt.header.storeName}\n".toByteArray())
        out.write(CMD_TEXT_NORMAL)
        out.write(CMD_BOLD_OFF)

        if (receipt.header.tagline.isNotBlank()) {
            out.write("${receipt.header.tagline}\n".toByteArray())
        }
        if (receipt.header.address.isNotBlank()) {
            out.write("${receipt.header.address}\n".toByteArray())
        }
        if (receipt.header.phone.isNotBlank()) {
            out.write("Tel: ${receipt.header.phone}\n".toByteArray())
        }
        if (receipt.header.gstin.isNotBlank()) {
            out.write("${receipt.header.gstin}\n".toByteArray())
        }
        if (receipt.header.fssai.isNotBlank()) {
            out.write("FSSAI: ${receipt.header.fssai}\n".toByteArray())
        }

        out.write(CMD_BOLD_ON)
        out.write("${receipt.header.taxInvoiceTitle}\n".toByteArray())
        out.write(CMD_BOLD_OFF)
        out.write("$doubleDivider\n".toByteArray())

        // 3. Metadata (Invoice No, Date, Customer)
        out.write(CMD_ALIGN_LEFT)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy  hh:mm a", Locale.getDefault())
        val dateStr = dateFormat.format(Date(receipt.dateTimestamp))

        out.write(formatTwoColumns("Bill: #${receipt.invoiceNo}", dateStr, cols).toByteArray())
        out.write("\n".toByteArray())

        if (receipt.customerName.isNotBlank() || receipt.customerPhone.isNotBlank()) {
            val custText = buildString {
                append("Customer: ")
                if (receipt.customerName.isNotBlank()) append(receipt.customerName)
                if (receipt.customerPhone.isNotBlank()) append(" (${receipt.customerPhone})")
            }
            out.write("$custText\n".toByteArray())
        }
        out.write("$divider\n".toByteArray())

        // 4. Line Items Table
        out.write(CMD_BOLD_ON)
        if (paperSize == ThermalPaperSize.SIZE_58MM) {
            // 32 cols: ITEM (16) QTY(6) TOTAL(10)
            out.write(formatThreeColumns("ITEM", "QTY", "TOTAL", 14, 6, 12).toByteArray())
        } else {
            // 48 cols: ITEM (24) RATE(8) QTY(6) TOTAL(10)
            out.write(formatFourColumns("ITEM", "RATE", "QTY", "TOTAL", 22, 8, 6, 12).toByteArray())
        }
        out.write("\n".toByteArray())
        out.write(CMD_BOLD_OFF)
        out.write("$divider\n".toByteArray())

        // Print each item
        for (item in receipt.items) {
            val qtyStr = if (item.qty % 1.0 == 0.0) item.qty.toInt().toString() else item.qty.toString()
            val totalStr = "%.2f".format(item.total)
            val rateStr = "%.2f".format(item.unitPrice)

            if (paperSize == ThermalPaperSize.SIZE_58MM) {
                // First line: item name (can wrap)
                if (item.name.length > 14) {
                    out.write("${item.name}\n".toByteArray())
                    out.write(formatThreeColumns("", "${qtyStr}${item.unit}", "₹$totalStr", 14, 6, 12).toByteArray())
                } else {
                    out.write(formatThreeColumns(item.name, "${qtyStr}${item.unit}", "₹$totalStr", 14, 6, 12).toByteArray())
                }
            } else {
                if (item.name.length > 22) {
                    out.write("${item.name}\n".toByteArray())
                    out.write(formatFourColumns("", "₹$rateStr", "${qtyStr}${item.unit}", "₹$totalStr", 22, 8, 6, 12).toByteArray())
                } else {
                    out.write(formatFourColumns(item.name, "₹$rateStr", "${qtyStr}${item.unit}", "₹$totalStr", 22, 8, 6, 12).toByteArray())
                }
            }
            out.write("\n".toByteArray())
        }

        out.write("$divider\n".toByteArray())

        // 5. Totals & Tax Breakdown
        out.write(formatTwoColumns("Subtotal:", "₹%.2f".format(receipt.subtotal), cols).toByteArray())
        out.write("\n".toByteArray())

        if (receipt.discountAmount > 0.0) {
            out.write(formatTwoColumns("Discount Applied:", "-₹%.2f".format(receipt.discountAmount), cols).toByteArray())
            out.write("\n".toByteArray())
        }

        if (receipt.taxAmount > 0.0) {
            val halfTax = receipt.taxAmount / 2.0
            out.write(formatTwoColumns("CGST (Split):", "₹%.2f".format(halfTax), cols).toByteArray())
            out.write("\n".toByteArray())
            out.write(formatTwoColumns("SGST (Split):", "₹%.2f".format(halfTax), cols).toByteArray())
            out.write("\n".toByteArray())
        }

        out.write("$doubleDivider\n".toByteArray())

        // Grand Net Total (Bold & Large)
        out.write(CMD_BOLD_ON)
        out.write(CMD_DOUBLE_HEIGHT_ON)
        out.write(formatTwoColumns("NET TOTAL:", "₹%.2f".format(receipt.netTotal), cols).toByteArray())
        out.write("\n".toByteArray())
        out.write(CMD_TEXT_NORMAL)
        out.write(CMD_BOLD_OFF)
        out.write("$doubleDivider\n".toByteArray())

        // Payment Mode
        out.write(CMD_ALIGN_LEFT)
        out.write("Payment Mode: ${receipt.paymentMethod.uppercase()}\n".toByteArray())
        out.write("Status: PAID & VERIFIED\n".toByteArray())
        out.write("$divider\n".toByteArray())

        // 6. Dynamic UPI QR Code on Receipt (Scan to Pay or Digital Copy)
        if (includeQrCode && !receipt.upiPaymentUri.isNullOrBlank()) {
            out.write(CMD_ALIGN_CENTER)
            out.write("Scan to Pay / Verify UPI Bill:\n".toByteArray())
            val qrBytes = generateEscPosQrCodeBytes(receipt.upiPaymentUri, 6)
            out.write(qrBytes)
            out.write("\n".toByteArray())
        }

        // 7. 1D Barcode of Invoice ID
        if (includeBarcode && receipt.barcodeValue.isNotBlank()) {
            out.write(CMD_ALIGN_CENTER)
            val barcodeBytes = generateEscPosBarcode128(receipt.barcodeValue)
            out.write(barcodeBytes)
            out.write("${receipt.barcodeValue}\n".toByteArray())
        }

        // 8. Footer Notes
        out.write(CMD_ALIGN_CENTER)
        for (note in receipt.footerNotes) {
            out.write("$note\n".toByteArray())
        }

        // 9. Paper Feed and Cut
        out.write(CMD_FEED_5)
        if (autoCut) {
            out.write(CMD_PAPER_CUT)
        }

        return out.toByteArray()
    }

    /**
     * Builds ESC/POS byte array for Day-End Z-Report / Shift Sales Summary.
     */
    fun generateShiftZReportBytes(
        report: ShiftZReportData,
        paperSize: ThermalPaperSize = ThermalPaperSize.SIZE_58MM,
        autoCut: Boolean = true
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val cols = paperSize.charColumns
        val divider = "-".repeat(cols)
        val doubleDivider = "=".repeat(cols)

        out.write(CMD_INIT)
        out.write(CMD_ALIGN_CENTER)
        out.write(CMD_BOLD_ON)
        out.write(CMD_DOUBLE_SIZE_ON)
        out.write("${report.storeName}\n".toByteArray())
        out.write(CMD_TEXT_NORMAL)
        out.write("DAY-END SHIFT Z-REPORT\n".toByteArray())
        out.write("Date: ${report.reportDate}\n".toByteArray())
        out.write(CMD_BOLD_OFF)
        out.write("$doubleDivider\n".toByteArray())

        out.write(CMD_ALIGN_LEFT)
        out.write(CMD_BOLD_ON)
        out.write(formatTwoColumns("TOTAL GROSS SALES:", "₹%.2f".format(report.totalSales), cols).toByteArray())
        out.write("\n".toByteArray())
        out.write(CMD_BOLD_OFF)
        out.write(formatTwoColumns("Total Invoices Generated:", "${report.totalBillsCount}", cols).toByteArray())
        out.write("\n".toByteArray())
        out.write("$divider\n".toByteArray())

        out.write("PAYMENT COLLECTION SPLIT:\n".toByteArray())
        out.write(formatTwoColumns("  • UPI / Dynamic QR:", "₹%.2f".format(report.upiCollected), cols).toByteArray())
        out.write("\n".toByteArray())
        out.write(formatTwoColumns("  • Cash in Drawer:", "₹%.2f".format(report.cashCollected), cols).toByteArray())
        out.write("\n".toByteArray())
        out.write(formatTwoColumns("  • Khata / Credit Ledger:", "₹%.2f".format(report.khataOutstandingAdded), cols).toByteArray())
        out.write("\n".toByteArray())
        out.write("$divider\n".toByteArray())

        out.write("TAX & DISCOUNT BREAKDOWN:\n".toByteArray())
        out.write(formatTwoColumns("  • Total GST/Tax Collected:", "₹%.2f".format(report.totalTaxCollected), cols).toByteArray())
        out.write("\n".toByteArray())
        out.write(formatTwoColumns("  • Total Discounts Given:", "₹%.2f".format(report.totalDiscountsGiven), cols).toByteArray())
        out.write("\n".toByteArray())

        if (report.topSellingItems.isNotEmpty()) {
            out.write("$divider\n".toByteArray())
            out.write("TOP SELLING COMMODITIES:\n".toByteArray())
            report.topSellingItems.take(5).forEachIndexed { idx, item ->
                out.write(formatTwoColumns("${idx + 1}. ${item.first}", "${item.second} sold", cols).toByteArray())
                out.write("\n".toByteArray())
            }
        }

        out.write("$doubleDivider\n".toByteArray())
        out.write(CMD_ALIGN_CENTER)
        out.write("--- END OF DAY Z-REPORT ---\n".toByteArray())
        out.write("Generated at ${SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())}\n".toByteArray())

        out.write(CMD_FEED_5)
        if (autoCut) {
            out.write(CMD_PAPER_CUT)
        }

        return out.toByteArray()
    }

    /**
     * Builds ESC/POS byte array for hardware diagnostics test print.
     */
    fun generateTestPrintBytes(
        printerName: String = "Thermal POS Printer",
        paperSize: ThermalPaperSize = ThermalPaperSize.SIZE_58MM
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val cols = paperSize.charColumns
        val divider = "-".repeat(cols)

        out.write(CMD_INIT)
        out.write(CMD_ALIGN_CENTER)
        out.write(CMD_BOLD_ON)
        out.write(CMD_DOUBLE_SIZE_ON)
        out.write("PRINTER TEST OK\n".toByteArray())
        out.write(CMD_TEXT_NORMAL)
        out.write(CMD_BOLD_OFF)
        out.write("$printerName (${paperSize.widthMm}mm / ${paperSize.charColumns} cols)\n".toByteArray())
        out.write("$divider\n".toByteArray())

        out.write(CMD_ALIGN_LEFT)
        out.write("Left Aligned Text\n".toByteArray())
        out.write(CMD_ALIGN_CENTER)
        out.write("Center Aligned Text\n".toByteArray())
        out.write(CMD_ALIGN_RIGHT)
        out.write("Right Aligned Text\n".toByteArray())
        out.write(CMD_ALIGN_LEFT)

        out.write(CMD_BOLD_ON)
        out.write("Bold Font Sample\n".toByteArray())
        out.write(CMD_BOLD_OFF)

        out.write(CMD_UNDERLINE_ON)
        out.write("Underlined Text Sample\n".toByteArray())
        out.write(CMD_UNDERLINE_OFF)

        out.write(CMD_INVERSE_ON)
        out.write(" INVERSE HIGH CONTRAST \n".toByteArray())
        out.write(CMD_INVERSE_OFF)

        out.write("$divider\n".toByteArray())
        out.write(CMD_ALIGN_CENTER)
        out.write("QR Code Test:\n".toByteArray())
        out.write(generateEscPosQrCodeBytes("https://matrics.ai/pos-receipt-test", 5))
        out.write("\n".toByteArray())

        out.write("Barcode 128 Test:\n".toByteArray())
        out.write(generateEscPosBarcode128("TEST-88392"))
        out.write("TEST-88392\n".toByteArray())

        out.write(CMD_FEED_5)
        out.write(CMD_PAPER_CUT)
        return out.toByteArray()
    }

    /**
     * Converts a raw Bitmap into ESC/POS Raster bit-image command bytes (GS v 0).
     */
    fun bitmapToEscPosRaster(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8

        val out = ByteArrayOutputStream()
        // GS v 0 m xL xH yL yH
        out.write(byteArrayOf(GS, 0x76, 0x30, 0x00))
        out.write(byteArrayOf((widthBytes and 0xFF).toByte(), ((widthBytes shr 8) and 0xFF).toByte()))
        out.write(byteArrayOf((height and 0xFF).toByte(), ((height shr 8) and 0xFF).toByte()))

        val rawData = ByteArray(widthBytes * height)
        var byteIndex = 0

        for (y in 0 until height) {
            for (xByte in 0 until widthBytes) {
                var currentByte = 0
                for (bit in 0 until 8) {
                    val x = (xByte * 8) + bit
                    if (x < width) {
                        val pixel = bitmap.getPixel(x, y)
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val b = pixel and 0xFF
                        val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                        if (luminance < 128) {
                            currentByte = currentByte or (1 shl (7 - bit))
                        }
                    }
                }
                rawData[byteIndex++] = currentByte.toByte()
            }
        }

        out.write(rawData)
        return out.toByteArray()
    }

    /**
     * Native ESC/POS QR Code command sequence (Model 2, Error Correction Level M).
     */
    fun generateEscPosQrCodeBytes(data: String, moduleSize: Int = 6): ByteArray {
        val out = ByteArrayOutputStream()
        val dataBytes = data.toByteArray()
        val len = dataBytes.size + 3

        val pL = (len and 0xFF).toByte()
        val pH = ((len shr 8) and 0xFF).toByte()

        // 1. Set QR Code Model (Model 2) -> GS ( k 04 00 31 41 32 00
        out.write(byteArrayOf(GS, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00))

        // 2. Set Module Size (1 to 16) -> GS ( k 03 00 31 43 n
        val sizeByte = moduleSize.coerceIn(1, 16).toByte()
        out.write(byteArrayOf(GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, sizeByte))

        // 3. Set Error Correction Level (48=L, 49=M, 50=Q, 51=H) -> GS ( k 03 00 31 45 49
        out.write(byteArrayOf(GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x31))

        // 4. Store QR Code Data -> GS ( k pL pH 31 50 30 d1...dk
        out.write(byteArrayOf(GS, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30))
        out.write(dataBytes)

        // 5. Print Stored QR Code -> GS ( k 03 00 31 51 30
        out.write(byteArrayOf(GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30))

        return out.toByteArray()
    }

    /**
     * Native ESC/POS 1D Barcode (Code 128) sequence.
     */
    fun generateEscPosBarcode128(data: String): ByteArray {
        val out = ByteArrayOutputStream()
        // GS h n (Barcode height = 50 dots)
        out.write(byteArrayOf(GS, 0x68, 0x32))
        // GS w n (Barcode module width = 2)
        out.write(byteArrayOf(GS, 0x77, 0x02))
        // GS H n (HRI characters position = none)
        out.write(byteArrayOf(GS, 0x48, 0x00))

        // GS k 73 len data (Code 128)
        val dataBytes = data.toByteArray()
        out.write(byteArrayOf(GS, 0x6B, 0x49, dataBytes.size.toByte()))
        out.write(dataBytes)
        return out.toByteArray()
    }

    /**
     * Generates a QR Code as an Android Bitmap using ZXing.
     */
    fun generateQrBitmap(content: String, sizePx: Int = 250): Bitmap {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            sizePx,
            sizePx
        )
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    // Formatting Helper Functions for Monospace Thermal Layout
    private fun formatTwoColumns(left: String, right: String, totalCols: Int): String {
        val spacesNeeded = totalCols - left.length - right.length
        return if (spacesNeeded > 0) {
            left + " ".repeat(spacesNeeded) + right
        } else {
            val truncatedLeft = left.take((totalCols - right.length - 1).coerceAtLeast(0))
            val spaces = (totalCols - truncatedLeft.length - right.length).coerceAtLeast(1)
            truncatedLeft + " ".repeat(spaces) + right
        }
    }

    private fun formatThreeColumns(c1: String, c2: String, c3: String, w1: Int, w2: Int, w3: Int): String {
        val part1 = c1.padEnd(w1).take(w1)
        val part2 = c2.padStart(w2).take(w2)
        val part3 = c3.padStart(w3).take(w3)
        return "$part1$part2$part3"
    }

    private fun formatFourColumns(c1: String, c2: String, c3: String, c4: String, w1: Int, w2: Int, w3: Int, w4: Int): String {
        val part1 = c1.padEnd(w1).take(w1)
        val part2 = c2.padStart(w2).take(w2)
        val part3 = c3.padStart(w3).take(w3)
        val part4 = c4.padStart(w4).take(w4)
        return "$part1$part2$part3$part4"
    }

    /**
     * Formats receipt as plain ASCII monospace text for digital sharing or web/print preview.
     */
    fun formatReceiptAsPlainText(
        receipt: ReceiptData,
        paperSize: ThermalPaperSize = ThermalPaperSize.SIZE_58MM
    ): String {
        val cols = paperSize.charColumns
        val divider = "-".repeat(cols)
        val doubleDivider = "=".repeat(cols)
        val sb = StringBuilder()

        sb.appendLine(centerText(receipt.header.storeName, cols))
        if (receipt.header.tagline.isNotBlank()) sb.appendLine(centerText(receipt.header.tagline, cols))
        if (receipt.header.address.isNotBlank()) sb.appendLine(centerText(receipt.header.address, cols))
        if (receipt.header.phone.isNotBlank()) sb.appendLine(centerText("Tel: ${receipt.header.phone}", cols))
        if (receipt.header.gstin.isNotBlank()) sb.appendLine(centerText(receipt.header.gstin, cols))
        sb.appendLine(centerText(receipt.header.taxInvoiceTitle, cols))
        sb.appendLine(doubleDivider)

        val dateFormat = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault())
        sb.appendLine(formatTwoColumns("Bill: #${receipt.invoiceNo}", dateFormat.format(Date(receipt.dateTimestamp)), cols))
        if (receipt.customerName.isNotBlank() || receipt.customerPhone.isNotBlank()) {
            sb.appendLine("Customer: ${receipt.customerName} ${receipt.customerPhone}".trim())
        }
        sb.appendLine(divider)

        if (paperSize == ThermalPaperSize.SIZE_58MM) {
            sb.appendLine(formatThreeColumns("ITEM", "QTY", "TOTAL", 14, 6, 12))
        } else {
            sb.appendLine(formatFourColumns("ITEM", "RATE", "QTY", "TOTAL", 22, 8, 6, 12))
        }
        sb.appendLine(divider)

        for (item in receipt.items) {
            val qtyStr = if (item.qty % 1.0 == 0.0) item.qty.toInt().toString() else item.qty.toString()
            val totalStr = "₹%.2f".format(item.total)
            val rateStr = "₹%.2f".format(item.unitPrice)
            if (paperSize == ThermalPaperSize.SIZE_58MM) {
                sb.appendLine(formatThreeColumns(item.name, "$qtyStr${item.unit}", totalStr, 14, 6, 12))
            } else {
                sb.appendLine(formatFourColumns(item.name, rateStr, "$qtyStr${item.unit}", totalStr, 22, 8, 6, 12))
            }
        }

        sb.appendLine(divider)
        sb.appendLine(formatTwoColumns("Subtotal:", "₹%.2f".format(receipt.subtotal), cols))
        if (receipt.discountAmount > 0) sb.appendLine(formatTwoColumns("Discount:", "-₹%.2f".format(receipt.discountAmount), cols))
        if (receipt.taxAmount > 0) sb.appendLine(formatTwoColumns("GST/Tax:", "₹%.2f".format(receipt.taxAmount), cols))
        sb.appendLine(doubleDivider)
        sb.appendLine(formatTwoColumns("GRAND TOTAL:", "₹%.2f".format(receipt.netTotal), cols))
        sb.appendLine(doubleDivider)
        sb.appendLine("Payment: ${receipt.paymentMethod.uppercase()} (PAID)")
        sb.appendLine(divider)
        for (note in receipt.footerNotes) {
            sb.appendLine(centerText(note, cols))
        }
        return sb.toString()
    }

    private fun centerText(text: String, totalCols: Int): String {
        if (text.length >= totalCols) return text.take(totalCols)
        val pad = (totalCols - text.length) / 2
        return " ".repeat(pad) + text
    }
}
