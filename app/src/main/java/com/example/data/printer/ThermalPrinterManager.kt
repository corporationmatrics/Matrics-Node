package com.example.data.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

enum class PrinterConnectionType(val label: String) {
    BLUETOOTH("Bluetooth ESC/POS"),
    NETWORK_IP("Wi-Fi / Ethernet IP"),
    SYSTEM_PRINT("Android Print Spooler"),
    PREVIEW_ONLY("Virtual Thermal Preview")
}

data class DiscoveredPrinterDevice(
    val name: String,
    val address: String,
    val type: PrinterConnectionType,
    val isBonded: Boolean = true
)

data class ThermalPrinterConfig(
    val connectionType: PrinterConnectionType = PrinterConnectionType.BLUETOOTH,
    val paperSize: ThermalPaperSize = ThermalPaperSize.SIZE_58MM,
    val targetAddress: String = "", // Bluetooth MAC Address or Network IP Address
    val networkPort: Int = 9100,
    val autoPrintOnSale: Boolean = false,
    val includeQrCode: Boolean = true,
    val includeBarcode: Boolean = true,
    val autoCutPaper: Boolean = true,
    val kickCashDrawer: Boolean = false,
    val storeHeader: EscPosThermalPrinterEngine.StoreReceiptHeader = EscPosThermalPrinterEngine.StoreReceiptHeader()
)

data class ThermalPrinterStatus(
    val isConnected: Boolean = false,
    val isPrinting: Boolean = false,
    val connectedDeviceName: String = "No Printer Connected",
    val lastPrintTimestamp: Long = 0L,
    val lastError: String? = null,
    val totalReceiptsPrinted: Int = 0
)

class ThermalPrinterManager(private val context: Context) {

    private val TAG = "ThermalPrinterManager"
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val _config = MutableStateFlow(ThermalPrinterConfig())
    val config: StateFlow<ThermalPrinterConfig> = _config.asStateFlow()

    private val _status = MutableStateFlow(ThermalPrinterStatus())
    val status: StateFlow<ThermalPrinterStatus> = _status.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<DiscoveredPrinterDevice>>(emptyList())
    val pairedDevices: StateFlow<List<DiscoveredPrinterDevice>> = _pairedDevices.asStateFlow()

    private var activeBluetoothSocket: BluetoothSocket? = null

    init {
        refreshPairedDevices()
    }

    fun updateConfig(newConfig: ThermalPrinterConfig) {
        _config.value = newConfig
    }

    fun updatePaperSize(size: ThermalPaperSize) {
        _config.value = _config.value.copy(paperSize = size)
    }

    fun updateConnectionType(type: PrinterConnectionType) {
        _config.value = _config.value.copy(connectionType = type)
    }

    fun updateStoreHeader(header: EscPosThermalPrinterEngine.StoreReceiptHeader) {
        _config.value = _config.value.copy(storeHeader = header)
    }

    fun setAutoPrintOnSale(enabled: Boolean) {
        _config.value = _config.value.copy(autoPrintOnSale = enabled)
    }

    fun setIncludeQrCode(enabled: Boolean) {
        _config.value = _config.value.copy(includeQrCode = enabled)
    }

    fun setAutoCut(enabled: Boolean) {
        _config.value = _config.value.copy(autoCutPaper = enabled)
    }

    fun setTargetDevice(address: String, name: String) {
        _config.value = _config.value.copy(targetAddress = address)
        _status.value = _status.value.copy(connectedDeviceName = name)
    }

    /**
     * Lists paired Bluetooth devices on the Android device.
     */
    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                val bonded = bluetoothAdapter.bondedDevices ?: emptySet()
                val list = bonded.map { device ->
                    DiscoveredPrinterDevice(
                        name = device.name ?: "Unknown Bluetooth Device",
                        address = device.address,
                        type = PrinterConnectionType.BLUETOOTH,
                        isBonded = true
                    )
                }
                _pairedDevices.value = list
                // If targetAddress is blank, pick first thermal printer if name matches POS/Printer/RP
                if (_config.value.targetAddress.isBlank() && list.isNotEmpty()) {
                    val candidate = list.firstOrNull {
                        it.name.contains("POS", ignoreCase = true) ||
                        it.name.contains("Print", ignoreCase = true) ||
                        it.name.contains("RP", ignoreCase = true) ||
                        it.name.contains("58", ignoreCase = true) ||
                        it.name.contains("80", ignoreCase = true) ||
                        it.name.contains("Thermal", ignoreCase = true)
                    } ?: list.first()
                    _config.value = _config.value.copy(targetAddress = candidate.address)
                    _status.value = _status.value.copy(connectedDeviceName = candidate.name)
                }
            } else {
                _pairedDevices.value = emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching paired Bluetooth devices: ${e.message}")
            _pairedDevices.value = emptyList()
        }
    }

    /**
     * Prints a customer receipt using the configured printer channel.
     */
    suspend fun printReceipt(
        receiptData: EscPosThermalPrinterEngine.ReceiptData
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        _status.value = _status.value.copy(isPrinting = true, lastError = null)
        val currentConfig = _config.value

        val mergedReceipt = receiptData.copy(
            header = if (receiptData.header.storeName == "MATRICS NODE STORE" && currentConfig.storeHeader.storeName.isNotBlank()) {
                currentConfig.storeHeader
            } else {
                receiptData.header
            }
        )

        try {
            when (currentConfig.connectionType) {
                PrinterConnectionType.BLUETOOTH -> {
                    if (currentConfig.targetAddress.isBlank()) {
                        throw IllegalStateException("No Bluetooth printer selected. Please select a paired printer in Settings.")
                    }
                    val bytes = EscPosThermalPrinterEngine.generateCustomerReceiptBytes(
                        receipt = mergedReceipt,
                        paperSize = currentConfig.paperSize,
                        includeQrCode = currentConfig.includeQrCode,
                        includeBarcode = currentConfig.includeBarcode,
                        autoCut = currentConfig.autoCutPaper,
                        kickDrawer = currentConfig.kickCashDrawer
                    )
                    sendBytesViaBluetooth(currentConfig.targetAddress, bytes)
                }

                PrinterConnectionType.NETWORK_IP -> {
                    if (currentConfig.targetAddress.isBlank()) {
                        throw IllegalStateException("No Network IP specified for ESC/POS printer (e.g. 192.168.1.100).")
                    }
                    val bytes = EscPosThermalPrinterEngine.generateCustomerReceiptBytes(
                        receipt = mergedReceipt,
                        paperSize = currentConfig.paperSize,
                        includeQrCode = currentConfig.includeQrCode,
                        includeBarcode = currentConfig.includeBarcode,
                        autoCut = currentConfig.autoCutPaper,
                        kickDrawer = currentConfig.kickCashDrawer
                    )
                    sendBytesViaNetwork(currentConfig.targetAddress, currentConfig.networkPort, bytes)
                }

                PrinterConnectionType.SYSTEM_PRINT -> {
                    printViaAndroidSystemSpooler(mergedReceipt, currentConfig.paperSize)
                }

                PrinterConnectionType.PREVIEW_ONLY -> {
                    // Simulates instantaneous preview print
                }
            }

            _status.value = _status.value.copy(
                isConnected = true,
                isPrinting = false,
                lastPrintTimestamp = System.currentTimeMillis(),
                totalReceiptsPrinted = _status.value.totalReceiptsPrinted + 1,
                lastError = null
            )
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to print receipt: ${e.message}", e)
            _status.value = _status.value.copy(
                isPrinting = false,
                lastError = e.message ?: "Print failure"
            )
            Result.failure(e)
        }
    }

    /**
     * Prints a daily Z-report / Shift Sales summary.
     */
    suspend fun printZReport(
        reportData: EscPosThermalPrinterEngine.ShiftZReportData
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        _status.value = _status.value.copy(isPrinting = true, lastError = null)
        val currentConfig = _config.value

        try {
            val bytes = EscPosThermalPrinterEngine.generateShiftZReportBytes(
                report = reportData,
                paperSize = currentConfig.paperSize,
                autoCut = currentConfig.autoCutPaper
            )

            when (currentConfig.connectionType) {
                PrinterConnectionType.BLUETOOTH -> {
                    if (currentConfig.targetAddress.isBlank()) throw IllegalStateException("No Bluetooth printer selected.")
                    sendBytesViaBluetooth(currentConfig.targetAddress, bytes)
                }
                PrinterConnectionType.NETWORK_IP -> {
                    if (currentConfig.targetAddress.isBlank()) throw IllegalStateException("No Network IP specified.")
                    sendBytesViaNetwork(currentConfig.targetAddress, currentConfig.networkPort, bytes)
                }
                PrinterConnectionType.SYSTEM_PRINT -> {
                    // Fallback to system spooler
                }
                PrinterConnectionType.PREVIEW_ONLY -> {}
            }

            _status.value = _status.value.copy(
                isPrinting = false,
                lastPrintTimestamp = System.currentTimeMillis(),
                totalReceiptsPrinted = _status.value.totalReceiptsPrinted + 1
            )
            Result.success(true)
        } catch (e: Exception) {
            _status.value = _status.value.copy(
                isPrinting = false,
                lastError = e.message ?: "Z-Report print error"
            )
            Result.failure(e)
        }
    }

    /**
     * Executes hardware test print.
     */
    suspend fun runTestPrint(): Result<Boolean> = withContext(Dispatchers.IO) {
        _status.value = _status.value.copy(isPrinting = true, lastError = null)
        val currentConfig = _config.value
        try {
            val testBytes = EscPosThermalPrinterEngine.generateTestPrintBytes(
                printerName = _status.value.connectedDeviceName,
                paperSize = currentConfig.paperSize
            )

            when (currentConfig.connectionType) {
                PrinterConnectionType.BLUETOOTH -> {
                    if (currentConfig.targetAddress.isBlank()) throw IllegalStateException("Select a paired printer to test print.")
                    sendBytesViaBluetooth(currentConfig.targetAddress, testBytes)
                }
                PrinterConnectionType.NETWORK_IP -> {
                    if (currentConfig.targetAddress.isBlank()) throw IllegalStateException("Enter printer IP address.")
                    sendBytesViaNetwork(currentConfig.targetAddress, currentConfig.networkPort, testBytes)
                }
                else -> {}
            }

            _status.value = _status.value.copy(
                isConnected = true,
                isPrinting = false,
                lastPrintTimestamp = System.currentTimeMillis(),
                totalReceiptsPrinted = _status.value.totalReceiptsPrinted + 1
            )
            Result.success(true)
        } catch (e: Exception) {
            _status.value = _status.value.copy(isPrinting = false, lastError = e.message)
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendBytesViaBluetooth(macAddress: String, bytes: ByteArray) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
            ?: throw IllegalStateException("Bluetooth adapter not found on device.")

        if (!adapter.isEnabled) {
            throw IllegalStateException("Bluetooth is disabled. Please turn on Bluetooth.")
        }

        val device: BluetoothDevice = adapter.getRemoteDevice(macAddress)
            ?: throw IllegalStateException("Could not find Bluetooth device at $macAddress")

        var socket: BluetoothSocket? = null
        try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            adapter.cancelDiscovery()
            socket.connect()

            val outputStream: OutputStream = socket.outputStream
            outputStream.write(bytes)
            outputStream.flush()
            Thread.sleep(250) // Allow printer buffer to complete
        } catch (e: Exception) {
            // Fallback for some non-standard SPP devices
            try {
                val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                socket = method.invoke(device, 1) as? BluetoothSocket
                socket?.connect()
                val outputStream = socket?.outputStream
                outputStream?.write(bytes)
                outputStream?.flush()
                Thread.sleep(250)
            } catch (fallbackEx: Exception) {
                throw IOException("Bluetooth connection failed: ${e.message}")
            }
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    private fun sendBytesViaNetwork(ipAddress: String, port: Int, bytes: ByteArray) {
        val socket = Socket()
        try {
            socket.connect(InetSocketAddress(ipAddress, port), 4000)
            val out = socket.getOutputStream()
            out.write(bytes)
            out.flush()
            Thread.sleep(200)
        } catch (e: Exception) {
            throw IOException("Network printer connection failed to $ipAddress:$port - ${e.message}")
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }

    private fun printViaAndroidSystemSpooler(
        receipt: EscPosThermalPrinterEngine.ReceiptData,
        paperSize: ThermalPaperSize
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "Invoice_${receipt.invoiceNo}"

        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val pdi = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback?.onLayoutFinished(pdi, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onWriteCancelled()
                    return
                }
                val pdfDocument = PdfDocument()
                val pageWidth = if (paperSize == ThermalPaperSize.SIZE_58MM) 200 else 280
                val pageHeight = 600
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
                val page = pdfDocument.startPage(pageInfo)

                val canvas = page.canvas
                val paint = Paint().apply {
                    color = Color.BLACK
                    textSize = 8.5f
                    isAntiAlias = true
                }

                val plainText = EscPosThermalPrinterEngine.formatReceiptAsPlainText(receipt, paperSize)
                var y = 20f
                for (line in plainText.lines()) {
                    canvas.drawText(line, 10f, y, paint)
                    y += 12f
                }

                pdfDocument.finishPage(page)

                try {
                    destination?.let {
                        FileOutputStream(it.fileDescriptor).use { outStream ->
                            pdfDocument.writeTo(outStream)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                } finally {
                    pdfDocument.close()
                }
            }
        }

        printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
    }

    /**
     * Shares receipt as text via Android intent (WhatsApp, SMS, Email).
     */
    fun shareReceiptAsText(receipt: EscPosThermalPrinterEngine.ReceiptData) {
        val plainText = EscPosThermalPrinterEngine.formatReceiptAsPlainText(receipt, _config.value.paperSize)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, plainText)
            type = "text/plain"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Receipt").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
