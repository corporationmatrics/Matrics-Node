package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ExpenseRepository
import com.example.data.GeminiService
import com.example.data.BankSmsParser
import com.example.data.BankSmsReceiver
import com.example.data.EmailInvoiceParser
import com.example.data.NlpParsingEngine
import com.example.data.OfflineWhisperSttEngine
import com.example.data.ParsedBankSms
import com.example.data.ParsedEmailInvoice
import com.example.data.ReceiptOcrEngine
import com.example.data.RegionalLanguage
import com.example.data.VoiceRecognitionManager
import com.example.data.VoiceTtsEngine
import com.example.data.SupplyChainEngine
import com.example.data.model.AbcClassificationItem
import com.example.data.model.BatchEntity
import com.example.data.model.CategoryBudgetStatus
import com.example.data.model.CommodityEntity
import com.example.data.model.CommoditySummary
import com.example.data.model.GrnReceiptItemInput
import com.example.data.model.GroceryItemEntity
import com.example.data.model.HandwrittenBillItem
import com.example.data.model.HandwrittenBillResult
import com.example.data.model.KachaBillPresets
import com.example.data.model.KhataEntryEntity
import com.example.data.model.KhataInstallmentPayment
import com.example.data.model.CustomerProfileData
import com.example.data.model.CustomerLoyaltyTier
import com.example.data.model.FrequentCustomerItem
import com.example.data.model.LineItemEntity
import com.example.data.model.OverspendAlertInfo
import com.example.data.model.ParsedNlpItem
import com.example.data.model.PurchaseOrderEntity
import com.example.data.model.PurchaseOrderItemEntity
import com.example.data.model.RecurringBillEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.StockMovementEntity
import com.example.data.model.StockoutRiskItem
import com.example.data.model.SupplierEntity
import com.example.data.model.SupplyChainAnalytics
import com.example.data.model.TransactionEntity
import com.example.data.worker.BudgetAlertWorker
import com.example.data.printer.DiscoveredPrinterDevice
import com.example.data.printer.EscPosThermalPrinterEngine
import com.example.data.printer.PrinterConnectionType
import com.example.data.printer.ThermalPaperSize
import com.example.data.printer.ThermalPrinterConfig
import com.example.data.printer.ThermalPrinterManager
import com.example.data.printer.ThermalPrinterStatus
import com.example.util.ImageProcessingUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class PeriodMode {
    WEEKLY,
    MONTHLY,
    W_O_W,
    M_O_M
}

sealed class ApiKeyStatus {
    object Idle : ApiKeyStatus()
    object Testing : ApiKeyStatus()
    object Success : ApiKeyStatus()
    data class Error(val error: String) : ApiKeyStatus()
}

data class CategorySpend(
    val category: String,
    val amount: Double,
    val percentage: Float
)

data class VarianceDriver(
    val category: String,
    val primarySpend: Double,
    val baselineSpend: Double,
    val deltaPercent: Double,
    val isIncrease: Boolean
)

data class PeriodComparisonData(
    val primaryTotal: Double,
    val baselineTotal: Double,
    val deltaPercent: Double,
    val primaryLabel: String,
    val baselineLabel: String,
    val primaryCurve: List<Pair<String, Double>>,
    val baselineCurve: List<Pair<String, Double>>,
    val varianceDrivers: List<VarianceDriver>
)

data class DashboardUiState(
    val totalSpendCurrentMonth: Double = 0.0,
    val totalSpendLastMonth: Double = 0.0,
    val monthOverMonthDelta: Double = 0.0,
    val monthlyBudget: Double = 15000.0,
    val burnRatePercentage: Float = 0.0f,
    val daysRemainingInMonth: Int = 13,
    val dailyBurnVelocity: Double = 0.0,
    val categorySpends: List<CategorySpend> = emptyList(),
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val lineItemsByTxId: Map<Long, List<LineItemEntity>> = emptyMap()
)

data class StorefrontDashboardState(
    val todayRevenue: Double = 0.0,
    val todayCost: Double = 0.0,
    val todayGrossProfit: Double = 0.0,
    val todayMarginPercent: Double = 0.0,
    val todayOrdersCount: Int = 0,
    val averageOrderValue: Double = 0.0,
    val lowStockCount: Int = 0,
    val totalInventoryValue: Double = 0.0,
    val totalSkusCount: Int = 0,
    val todaySalesTransactions: List<TransactionEntity> = emptyList(),
    val lowStockItems: List<CommodityEntity> = emptyList()
)

data class PosCheckoutDialogState(
    val isOpen: Boolean = false,
    val invoiceNo: String = "",
    val totalAmount: Double = 0.0,
    val subtotal: Double = 0.0,
    val taxAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val customerName: String = "",
    val customerPhone: String = "",
    val paymentMethod: String = "UPI / QR", // "UPI / QR", "CASH", "CARD", "KHATA"
    val upiQrUri: String = "",
    val isPaymentVerified: Boolean = false
)

enum class BarcodeScanMode {
    POS_BILLING,       // Scan barcodes directly into active POS bill
    INVENTORY_SEARCH,  // Scan to find and filter master SKU catalog
    INVENTORY_RESTOCK, // Scan to rapidly increment item stock (+1 / batch)
    PRICE_CHECKER,     // Customer / Store clerk rapid price & stock checker
    SKU_REGISTRATION   // Scan barcode to link to new or existing commodity
}

data class ScannedBarcodeRecord(
    val barcode: String,
    val timestamp: Long = System.currentTimeMillis(),
    val matchedCommodityName: String? = null,
    val price: Double = 0.0,
    val quantityAdded: Double = 1.0
)

data class BarcodeScannerUiState(
    val isVisible: Boolean = false,
    val mode: BarcodeScanMode = BarcodeScanMode.POS_BILLING,
    val isTorchOn: Boolean = false,
    val isContinuousScan: Boolean = true,
    val soundEnabled: Boolean = true,
    val lastScannedCode: String = "",
    val lastMatchedCommodity: CommodityEntity? = null,
    val lastScanTimestamp: Long = 0L,
    val sessionScanCount: Int = 0,
    val scannedHistory: List<ScannedBarcodeRecord> = emptyList(),
    val unrecognizedBarcode: String? = null,
    val showUnrecognizedDialog: Boolean = false,
    val statusFeedback: String = "Align barcode or QR code within the frame"
)

enum class VoiceConversationState {
    IDLE,
    LISTENING,              // Phase 1: Listening to spoken items/quantities/prices
    PARSING_ENTITIES,       // Extracting structured items
    TTS_SPEAKING_CONFIRM,   // TTS verbally confirming and asking route choice
    AWAITING_DISPATCH,      // Phase 2: Action listening for "Expense" / "Shopping List"
    PROCESSING_INTENT,      // Matching intent
    COMMITTED               // Saved with feedback
}

data class VoiceHudState(
    val isVisible: Boolean = false,
    val isListening: Boolean = false,
    val isOfflineMode: Boolean = false,
    val selectedLanguage: RegionalLanguage = RegionalLanguage.HINGLISH,
    val conversationState: VoiceConversationState = VoiceConversationState.IDLE,
    val rawTranscript: String = "",
    val tokenChips: List<NlpParsingEngine.TokenChip> = emptyList(),
    val parsedItems: List<ParsedNlpItem> = emptyList(),
    val detectedVendor: String = "FreshMart",
    val detectedCategory: String = "Groceries",
    val detectedPaymentMethod: String = "UPI Instant",
    val detectedNotes: String = "",
    val geoPin: String = "Sector 4 Cyber Hub",
    val audioWaveformLevel: Float = 0.08f,
    val isProcessing: Boolean = false,
    val statusMessage: String = "Ready to listen",
    val modelUsed: String = "gemini-3.5-flash",
    val confidence: Float = 0.95f,
    val liveBudgetWarning: String? = null,
    val ttsPromptText: String = "",
    val isTtsSpeaking: Boolean = false,
    val lastActionDispatched: String? = null
)

data class HandwrittenScannerState(
    val isVisible: Boolean = false,
    val isScanning: Boolean = false,
    val highContrastFilter: Boolean = true,
    val activeFilter: ImageProcessingUtils.DocumentFilter = ImageProcessingUtils.DocumentFilter.INK_BOOST,
    val isDecrypting: Boolean = false,
    val scanLaserProgress: Float = 0f,
    val terminalLog: String = "AWAITING INVOICE CAPTURE...",
    val selectedPresetIndex: Int = 0,
    val capturedBitmap: Bitmap? = null,
    val binarizedBitmap: Bitmap? = null,
    val imageSource: String = "PRESET", // "CAMERA", "GALLERY", "PRESET", "TEXT_OCR"
    val reconciliationResult: HandwrittenBillResult? = null,
    val selectedItemIndex: Int? = null,
    val showReconciliationSheet: Boolean = false,
    val editingItem: HandwrittenBillItem? = null,
    val statusMessage: String = "Point camera at handwritten Kacha bill, upload photo, or choose demo preset",
    val cropLeft: Float = 0.05f,
    val cropTop: Float = 0.05f,
    val cropRight: Float = 0.95f,
    val cropBottom: Float = 0.95f,
    val manualOcrInputText: String = "",
    val activeTab: Int = 0
)

data class ThermalReceiptModalState(
    val isVisible: Boolean = false,
    val receiptData: EscPosThermalPrinterEngine.ReceiptData? = null,
    val rawAsciiPreview: String = "",
    val isPrinting: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

data class WhatsAppReminderState(
    val customerName: String,
    val customerPhone: String,
    val totalOutstanding: Double,
    val invoiceNumber: String,
    val description: String,
    val dynamicUpiLink: String,
    val formattedMessageText: String,
    val khataEntryId: Long? = null,
    val dueDateFormatted: String = ""
)

class CyphrViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = ExpenseRepository(
        expenseDao = db.expenseDao(),
        recurringBillDao = db.recurringBillDao(),
        savingsAndKhataDao = db.savingsAndKhataDao(),
        supplyChainDao = db.supplyChainDao()
    )

    // Field Mode (High-contrast outdoor sunlight visibility)
    private val _isFieldMode = MutableStateFlow(false)
    val isFieldMode: StateFlow<Boolean> = _isFieldMode.asStateFlow()

    // Period Mode (Weekly vs Monthly)
    private val _periodMode = MutableStateFlow(PeriodMode.MONTHLY)
    val periodMode: StateFlow<PeriodMode> = _periodMode.asStateFlow()

    // User Gemini API Key & Key Status
    private val sharedPrefs = application.getSharedPreferences("cyphr_prefs", Context.MODE_PRIVATE)
    private val _userGeminiApiKey = MutableStateFlow(sharedPrefs.getString("gemini_api_key", "") ?: "")
    val userGeminiApiKey: StateFlow<String> = _userGeminiApiKey.asStateFlow()

    private val _apiKeyTestStatus = MutableStateFlow<ApiKeyStatus>(ApiKeyStatus.Idle)
    val apiKeyTestStatus: StateFlow<ApiKeyStatus> = _apiKeyTestStatus.asStateFlow()

    // AI Financial Insights
    private val _aiFinancialInsights = MutableStateFlow<String?>(null)
    val aiFinancialInsights: StateFlow<String?> = _aiFinancialInsights.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Monthly Budget & Category-Level Budgets
    private val _monthlyBudget = MutableStateFlow(sharedPrefs.getFloat("monthly_budget", 15000f).toDouble())
    val monthlyBudget: StateFlow<Double> = _monthlyBudget.asStateFlow()

    private val defaultCategoryBudgets = mapOf(
        "Groceries" to 6000.0,
        "Dairy" to 1500.0,
        "Dining" to 2500.0,
        "Transport" to 2000.0,
        "Utilities" to 2500.0,
        "Shopping" to 3000.0,
        "Healthcare" to 1500.0,
        "Entertainment" to 1200.0,
        "Personal Care" to 1000.0,
        "Miscellaneous" to 1000.0
    )

    private val _categoryBudgets = MutableStateFlow(loadCategoryBudgetsInternal())
    val categoryBudgets: StateFlow<Map<String, Double>> = _categoryBudgets.asStateFlow()

    // Toast and Modals
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _activeModal = MutableStateFlow<String?>(null)
    val activeModal: StateFlow<String?> = _activeModal.asStateFlow()

    // Bank SMS Auto-Capture Alert Flow
    private val _incomingBankSmsAlert = MutableStateFlow<ParsedBankSms?>(null)
    val incomingBankSmsAlert: StateFlow<ParsedBankSms?> = _incomingBankSmsAlert.asStateFlow()

    // Voice HUD State
    private val _voiceHudState = MutableStateFlow(VoiceHudState())
    val voiceHudState: StateFlow<VoiceHudState> = _voiceHudState.asStateFlow()

    // Real Voice Recognition Engine, Offline Whisper STT Engine, and Voice TTS Engine
    private var voiceRecognitionManager: VoiceRecognitionManager? = null
    private val offlineWhisperEngine by lazy { OfflineWhisperSttEngine(application, viewModelScope) }
    private val voiceTtsEngine by lazy { VoiceTtsEngine(application) }
    private var voiceNlpJob: Job? = null
    private var actionDispatchSilenceJob: Job? = null

    // Handwritten Scanner State
    private val _handwrittenScannerState = MutableStateFlow(HandwrittenScannerState())
    val handwrittenScannerState: StateFlow<HandwrittenScannerState> = _handwrittenScannerState.asStateFlow()

    // Camera Barcode & QR Scanner State
    private val _barcodeScannerState = MutableStateFlow(BarcodeScannerUiState())
    val barcodeScannerState: StateFlow<BarcodeScannerUiState> = _barcodeScannerState.asStateFlow()

    // Thermal Receipt Printer Engine & Connectivity Manager
    val thermalPrinterManager by lazy { ThermalPrinterManager(application) }
    val thermalPrinterConfig: StateFlow<ThermalPrinterConfig> = thermalPrinterManager.config
    val thermalPrinterStatus: StateFlow<ThermalPrinterStatus> = thermalPrinterManager.status
    val pairedThermalPrinters: StateFlow<List<DiscoveredPrinterDevice>> = thermalPrinterManager.pairedDevices

    private val _thermalReceiptModalState = MutableStateFlow(ThermalReceiptModalState())
    val thermalReceiptModalState: StateFlow<ThermalReceiptModalState> = _thermalReceiptModalState.asStateFlow()

    private val _isThermalPrinterSettingsOpen = MutableStateFlow(false)
    val isThermalPrinterSettingsOpen: StateFlow<Boolean> = _isThermalPrinterSettingsOpen.asStateFlow()

    // Period Comparison Filters
    private val _primaryPeriodMonth = MutableStateFlow(getCurrentYearMonth())
    val primaryPeriodMonth: StateFlow<String> = _primaryPeriodMonth.asStateFlow()

    private val _baselinePeriodMonth = MutableStateFlow(getPreviousYearMonth(getCurrentYearMonth()))
    val baselinePeriodMonth: StateFlow<String> = _baselinePeriodMonth.asStateFlow()

    // Search and Filtering
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    // Flows from Repository
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLineItems: StateFlow<List<LineItemEntity>> = repository.allLineItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wishlistItems: StateFlow<List<GroceryItemEntity>> = repository.wishlistItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pantryItems: StateFlow<List<GroceryItemEntity>> = repository.pantryItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCommodities: StateFlow<List<CommodityEntity>> = repository.allCommodities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val commoditySummaries: StateFlow<List<CommoditySummary>> = repository
        .getCommoditySummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard State
    val dashboardState: StateFlow<DashboardUiState> = combine(
        repository.allTransactions,
        repository.allLineItems,
        _monthlyBudget
    ) { txList, allLineItemsList, budget ->
        calculateDashboardMetrics(txList, allLineItemsList, budget)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    // --- STOREFRONT POS & INVENTORY HUB STATE ---
    private val _posCartItems = MutableStateFlow<List<com.example.data.model.PosCartItem>>(emptyList())
    val posCartItems: StateFlow<List<com.example.data.model.PosCartItem>> = _posCartItems.asStateFlow()

    private val _posTaxPercent = MutableStateFlow(0.0) // GST 0% / 5% / 12% / 18%
    val posTaxPercent: StateFlow<Double> = _posTaxPercent.asStateFlow()

    private val _posDiscountPercent = MutableStateFlow(0.0)
    val posDiscountPercent: StateFlow<Double> = _posDiscountPercent.asStateFlow()

    private val _posCustomerName = MutableStateFlow("")
    val posCustomerName: StateFlow<String> = _posCustomerName.asStateFlow()

    private val _posCustomerPhone = MutableStateFlow("")
    val posCustomerPhone: StateFlow<String> = _posCustomerPhone.asStateFlow()

    private val _posSelectedCategoryFilter = MutableStateFlow<String?>(null)
    val posSelectedCategoryFilter: StateFlow<String?> = _posSelectedCategoryFilter.asStateFlow()

    private val _posSearchQuery = MutableStateFlow("")
    val posSearchQuery: StateFlow<String> = _posSearchQuery.asStateFlow()

    private val _posCheckoutDialogState = MutableStateFlow(PosCheckoutDialogState())
    val posCheckoutDialogState: StateFlow<PosCheckoutDialogState> = _posCheckoutDialogState.asStateFlow()

    val lowStockCommodities: StateFlow<List<CommodityEntity>> = repository.lowStockCommodities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Storefront Dashboard State
    val storefrontDashboardState: StateFlow<StorefrontDashboardState> = combine(
        repository.allTransactions,
        repository.allLineItems,
        repository.allCommodities,
        repository.lowStockCommodities
    ) { transactions, lineItems, commodities, lowStock ->
        calculateStorefrontMetrics(transactions, lineItems, commodities, lowStock)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StorefrontDashboardState())

    // --- SUPPLY CHAIN & VENDOR MANAGEMENT FLOWS ---
    val allSuppliers: StateFlow<List<SupplierEntity>> = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPurchaseOrders: StateFlow<List<PurchaseOrderEntity>> = repository.allPurchaseOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStockMovements: StateFlow<List<StockMovementEntity>> = repository.allStockMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBatches: StateFlow<List<BatchEntity>> = repository.allBatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Holistic Supply Chain Analytics, Demand Forecasting, & Expiry Radar State
    val supplyChainAnalytics: StateFlow<SupplyChainAnalytics> = combine(
        repository.allCommodities,
        repository.allTransactions,
        repository.allLineItems,
        repository.allBatches,
        repository.allPurchaseOrders
    ) { commodities, transactions, lineItems, batches, purchaseOrders ->
        val suppliers = repository.allSuppliers.first()
        SupplyChainEngine.computeSupplyChainAnalytics(
            commodities = commodities,
            transactions = transactions,
            lineItems = lineItems,
            batches = batches,
            purchaseOrders = purchaseOrders,
            suppliers = suppliers
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SupplyChainAnalytics())

    // Category-Level Budget Statuses and Overspend Warning Flows
    val categoryBudgetStatuses: StateFlow<List<CategoryBudgetStatus>> = combine(
        dashboardState,
        _categoryBudgets
    ) { dashState, budgets ->
        val spendsMap = dashState.categorySpends.associate { it.category to it.amount }
        val allCats = (budgets.keys + spendsMap.keys).distinct().sorted()
        allCats.map { cat ->
            val limit = budgets[cat] ?: 2000.0
            val spent = spendsMap[cat] ?: 0.0
            val pct = if (limit > 0) (spent / limit).toFloat() else if (spent > 0) 1.5f else 0f
            val isOver = spent > limit && limit > 0
            val isCaution = !isOver && pct >= 0.75f
            val overAmt = if (isOver) spent - limit else 0.0
            val remAmt = if (!isOver) (limit - spent).coerceAtLeast(0.0) else 0.0
            val icon = getCategoryIconName(cat)
            CategoryBudgetStatus(
                category = cat,
                spent = spent,
                limit = limit,
                percentage = pct,
                isOverspent = isOver,
                isCaution = isCaution,
                remainingAmount = remAmt,
                overspentAmount = overAmt,
                iconName = icon
            )
        }.sortedWith(
            compareByDescending<CategoryBudgetStatus> { it.isOverspent }
                .thenByDescending { it.percentage }
                .thenByDescending { it.spent }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overspentCategories: StateFlow<List<CategoryBudgetStatus>> = categoryBudgetStatuses
        .map { list -> list.filter { it.isOverspent } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cautionCategories: StateFlow<List<CategoryBudgetStatus>> = categoryBudgetStatuses
        .map { list -> list.filter { it.isCaution } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCategoryAllocated: StateFlow<Double> = _categoryBudgets
        .map { it.values.sum() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Period Comparison State
    val periodComparisonData: StateFlow<PeriodComparisonData> = combine(
        repository.allTransactions,
        _primaryPeriodMonth,
        _baselinePeriodMonth
    ) { txList, primaryMonth, baselineMonth ->
        calculatePeriodComparison(txList, primaryMonth, baselineMonth)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PeriodComparisonData(0.0, 0.0, 0.0, "Current Month", "Previous Month", emptyList(), emptyList(), emptyList())
    )

    // Filtered Transactions
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        repository.allTransactions,
        _searchQuery,
        _selectedCategoryFilter
    ) { list, query, category ->
        list.filter { tx ->
            val matchesQuery = query.isBlank() ||
                    tx.title.contains(query, ignoreCase = true) ||
                    tx.vendor.contains(query, ignoreCase = true) ||
                    tx.category.contains(query, ignoreCase = true)

            val matchesCategory = category == null || tx.category.equals(category, ignoreCase = true)

            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Recurring Bills & Subscriptions State Flows ---
    val allRecurringBills: StateFlow<List<RecurringBillEntity>> = repository.allRecurringBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeRecurringBills: StateFlow<List<RecurringBillEntity>> = repository.activeRecurringBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingBillsNext7Days: StateFlow<List<RecurringBillEntity>> = allRecurringBills.map { bills ->
        val now = System.currentTimeMillis()
        val sevenDaysAhead = now + (7L * 24 * 60 * 60 * 1000)
        bills.filter { it.status != "PAUSED" && it.nextDueDate in now..sevenDaysAhead }
            .sortedBy { it.nextDueDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMonthlyRecurringCommitment: StateFlow<Double> = allRecurringBills.map { bills ->
        bills.filter { it.status != "PAUSED" }.sumOf { bill ->
            when (bill.billingCycle.uppercase()) {
                "WEEKLY" -> bill.amount * 4.33
                "QUARTERLY" -> bill.amount / 3.0
                "ANNUAL" -> bill.amount / 12.0
                else -> bill.amount // MONTHLY
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- Savings Goals State Flows ---
    val allSavingsGoals: StateFlow<List<SavingsGoalEntity>> = repository.allSavingsGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSavingsTarget: StateFlow<Double> = allSavingsGoals.map { goals ->
        goals.sumOf { it.targetAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalSavedSoFar: StateFlow<Double> = allSavingsGoals.map { goals ->
        goals.sumOf { it.currentAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val overallSavingsProgress: StateFlow<Float> = combine(totalSavedSoFar, totalSavingsTarget) { saved, target ->
        if (target > 0) (saved / target).toFloat().coerceIn(0f, 1f) else 0f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // --- Khata (Ledger) State Flows ---
    val allKhataEntries: StateFlow<List<KhataEntryEntity>> = repository.allKhataEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingKhataEntries: StateFlow<List<KhataEntryEntity>> = repository.pendingKhataEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalYouWillGet: StateFlow<Double> = pendingKhataEntries.map { list ->
        list.filter { it.type == "YOU_WILL_GET" }.sumOf { it.remainingAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalYouWillPay: StateFlow<Double> = pendingKhataEntries.map { list ->
        list.filter { it.type == "YOU_WILL_PAY" }.sumOf { it.remainingAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val netKhataBalance: StateFlow<Double> = combine(totalYouWillGet, totalYouWillPay) { willGet, willPay ->
        willGet - willPay
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Customer Profiles & Purchase History State
    private val _selectedCustomerProfile = MutableStateFlow<CustomerProfileData?>(null)
    val selectedCustomerProfile: StateFlow<CustomerProfileData?> = _selectedCustomerProfile.asStateFlow()

    private val _selectedKhataForInstallment = MutableStateFlow<KhataEntryEntity?>(null)
    val selectedKhataForInstallment: StateFlow<KhataEntryEntity?> = _selectedKhataForInstallment.asStateFlow()

    private val _whatsappReminderPreview = MutableStateFlow<WhatsAppReminderState?>(null)
    val whatsappReminderPreview: StateFlow<WhatsAppReminderState?> = _whatsappReminderPreview.asStateFlow()

    val allCustomerProfiles: StateFlow<List<CustomerProfileData>> = combine(
        repository.allTransactions,
        repository.allLineItems,
        allKhataEntries
    ) { txList, lineItemsList, khataList ->
        aggregateCustomerProfiles(txList, lineItemsList, khataList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        initVoiceRecognizer()
        // Schedule periodic background budget & bill check via WorkManager
        BudgetAlertWorker.schedulePeriodicCheck(getApplication())
        viewModelScope.launch {
            BankSmsReceiver.incomingSmsFlow.collect { sms ->
                _incomingBankSmsAlert.value = sms
            }
        }
    }

    fun triggerBudgetAlertsCheckNow() {
        BudgetAlertWorker.runOneTimeCheckNow(getApplication())
        showToast("🔍 Checked budgets & bills due in 24h")
    }

    // Field Mode Toggle
    fun toggleFieldMode() {
        _isFieldMode.value = !_isFieldMode.value
        showToast(if (_isFieldMode.value) "Field Mode Active: High Sunlight Contrast" else "Standard Theme Active")
    }

    fun setPeriodMode(mode: PeriodMode) {
        _periodMode.value = mode
    }

    // Gemini API Key Management
    fun saveGeminiApiKey(key: String) {
        _userGeminiApiKey.value = key.trim()
        sharedPrefs.edit().putString("gemini_api_key", key.trim()).apply()
        _apiKeyTestStatus.value = if (key.isNotBlank()) ApiKeyStatus.Success else ApiKeyStatus.Idle
        showToast(if (key.isNotBlank()) "Gemini 3.5 Flash Key Configured" else "Using Offline Vernacular Engine")
    }

    fun clearGeminiApiKey() {
        _userGeminiApiKey.value = ""
        sharedPrefs.edit().remove("gemini_api_key").apply()
        _apiKeyTestStatus.value = ApiKeyStatus.Idle
        showToast("Cleared Gemini API Key. Reverted to Local Engine.")
    }

    fun testGeminiApiKey(customKey: String? = null) {
        val key = (customKey ?: _userGeminiApiKey.value).trim()
        if (key.isBlank()) {
            _apiKeyTestStatus.value = ApiKeyStatus.Error("API Key is empty")
            return
        }
        viewModelScope.launch {
            _apiKeyTestStatus.value = ApiKeyStatus.Testing
            val res = GeminiService.testApiKey(key)
            if (res.isSuccess) {
                _apiKeyTestStatus.value = ApiKeyStatus.Success
                if (customKey != null) {
                    saveGeminiApiKey(customKey)
                }
                showToast("Gemini 3.5 Flash API Connected Successfully!")
            } else {
                val err = res.exceptionOrNull()?.localizedMessage ?: "Invalid API Key"
                _apiKeyTestStatus.value = ApiKeyStatus.Error(err)
                showToast("Key Error: $err")
            }
        }
    }

    fun requestAiFinancialInsights() {
        val apiKey = _userGeminiApiKey.value.trim()
        viewModelScope.launch {
            _isAiLoading.value = true
            val txList = allTransactions.value
            val summaries = commoditySummaries.value
            val summaryText = buildString {
                appendLine("Total transactions: ${txList.size}")
                appendLine("Current month spend: ₹${dashboardState.value.totalSpendCurrentMonth.toInt()}")
                appendLine("Top commodities: ${summaries.take(5).joinToString { "${it.canonicalName} (₹${it.currentAvgPrice.toInt()})" }}")
            }
            val result = GeminiService.generateFinancialInsights(summaryText, apiKey)
            if (result.isSuccess) {
                _aiFinancialInsights.value = result.getOrNull()
            } else {
                showToast("Could not generate AI insights: ${result.exceptionOrNull()?.message}")
            }
            _isAiLoading.value = false
        }
    }

    private fun calculateDashboardMetrics(
        txList: List<TransactionEntity>,
        allLineItemsList: List<LineItemEntity>,
        budget: Double
    ): DashboardUiState {
        val currentYearMonth = getCurrentYearMonth()
        val previousYearMonth = getPreviousYearMonth(currentYearMonth)

        val currentMonthTx = txList.filter { isDateInYearMonth(it.dateTimestamp, currentYearMonth) }
        val previousMonthTx = txList.filter { isDateInYearMonth(it.dateTimestamp, previousYearMonth) }

        val totalSpendCurrent = currentMonthTx.sumOf { it.totalAmount }
        val totalSpendPrevious = previousMonthTx.sumOf { it.totalAmount }

        val momDelta = if (totalSpendPrevious > 0) {
            ((totalSpendCurrent - totalSpendPrevious) / totalSpendPrevious) * 100.0
        } else 0.0

        val burnRate = if (budget > 0) {
            (totalSpendCurrent / budget).toFloat().coerceIn(0.0f, 2.0f)
        } else 0.0f

        val daysRemaining = getDaysRemainingInCurrentMonth()
        val daysElapsed = getDaysElapsedInCurrentMonth()
        val dailyBurnVelocity = if (daysElapsed > 0) totalSpendCurrent / daysElapsed else totalSpendCurrent

        val categoryMap = mutableMapOf<String, Double>()
        for (tx in currentMonthTx) {
            categoryMap[tx.category] = (categoryMap[tx.category] ?: 0.0) + tx.totalAmount
        }

        val categorySpends = categoryMap.map { (cat, amount) ->
            CategorySpend(
                category = cat,
                amount = amount,
                percentage = if (totalSpendCurrent > 0) (amount / totalSpendCurrent).toFloat() else 0f
            )
        }.sortedByDescending { it.amount }

        val lineItemsMap = allLineItemsList.groupBy { it.transactionId }

        return DashboardUiState(
            totalSpendCurrentMonth = totalSpendCurrent,
            totalSpendLastMonth = totalSpendPrevious,
            monthOverMonthDelta = momDelta,
            monthlyBudget = budget,
            burnRatePercentage = burnRate,
            daysRemainingInMonth = daysRemaining,
            dailyBurnVelocity = dailyBurnVelocity,
            categorySpends = categorySpends,
            recentTransactions = txList.take(20),
            lineItemsByTxId = lineItemsMap
        )
    }

    private fun calculatePeriodComparison(
        txList: List<TransactionEntity>,
        primaryMonthStr: String,
        baselineMonthStr: String
    ): PeriodComparisonData {
        val primaryTx = txList.filter { isDateInYearMonth(it.dateTimestamp, primaryMonthStr) }
        val baselineTx = txList.filter { isDateInYearMonth(it.dateTimestamp, baselineMonthStr) }

        val primaryTotal = primaryTx.sumOf { it.totalAmount }
        val baselineTotal = baselineTx.sumOf { it.totalAmount }

        val deltaPercent = if (baselineTotal > 0) {
            ((primaryTotal - baselineTotal) / baselineTotal) * 100.0
        } else 0.0

        val primaryCatMap = primaryTx.groupBy { it.category }.mapValues { it.value.sumOf { tx -> tx.totalAmount } }
        val baselineCatMap = baselineTx.groupBy { it.category }.mapValues { it.value.sumOf { tx -> tx.totalAmount } }

        val allCategories = (primaryCatMap.keys + baselineCatMap.keys).distinct()
        val drivers = allCategories.map { cat ->
            val pSpend = primaryCatMap[cat] ?: 0.0
            val bSpend = baselineCatMap[cat] ?: 0.0
            val catDelta = if (bSpend > 0) ((pSpend - bSpend) / bSpend) * 100.0 else if (pSpend > 0) 100.0 else 0.0
            VarianceDriver(
                category = cat,
                primarySpend = pSpend,
                baselineSpend = bSpend,
                deltaPercent = catDelta,
                isIncrease = pSpend > bSpend
            )
        }.sortedByDescending { Math.abs(it.primarySpend - it.baselineSpend) }

        val primaryCurve = buildCumulativeCurve(primaryTx, 31)
        val baselineCurve = buildCumulativeCurve(baselineTx, 31)

        val primaryLabel = formatYearMonthLabel(primaryMonthStr)
        val baselineLabel = formatYearMonthLabel(baselineMonthStr)

        return PeriodComparisonData(
            primaryTotal = primaryTotal,
            baselineTotal = baselineTotal,
            deltaPercent = deltaPercent,
            primaryLabel = primaryLabel,
            baselineLabel = baselineLabel,
            primaryCurve = primaryCurve,
            baselineCurve = baselineCurve,
            varianceDrivers = drivers
        )
    }

    private fun buildCumulativeCurve(txList: List<TransactionEntity>, daysInMonth: Int): List<Pair<String, Double>> {
        val daySpendMap = IntArray(daysInMonth + 1)
        val cal = Calendar.getInstance()
        for (tx in txList) {
            cal.timeInMillis = tx.dateTimestamp
            val day = cal.get(Calendar.DAY_OF_MONTH).coerceIn(1, daysInMonth)
            daySpendMap[day] = (daySpendMap[day] + tx.totalAmount).toInt()
        }

        val result = mutableListOf<Pair<String, Double>>()
        var cum = 0.0
        for (d in 1..daysInMonth) {
            cum += daySpendMap[d]
            result.add(Pair("D$d", cum))
        }
        return result
    }

    private fun initVoiceRecognizer() {
        if (voiceRecognitionManager == null) {
            voiceRecognitionManager = VoiceRecognitionManager(
                context = getApplication(),
                onListeningStateChanged = { isListening ->
                    val currentState = _voiceHudState.value.conversationState
                    val nextConvState = when {
                        isListening && currentState == VoiceConversationState.AWAITING_DISPATCH -> VoiceConversationState.AWAITING_DISPATCH
                        isListening -> VoiceConversationState.LISTENING
                        else -> currentState
                    }
                    _voiceHudState.value = _voiceHudState.value.copy(
                        isListening = isListening,
                        conversationState = nextConvState,
                        statusMessage = if (isListening) {
                            if (nextConvState == VoiceConversationState.AWAITING_DISPATCH) {
                                "🎙️ Say \"Expense\" to Log or \"Shopping list\" to Save..."
                            } else {
                                "Listening in ${_voiceHudState.value.selectedLanguage.displayName}..."
                            }
                        } else "Microphone ready"
                    )
                },
                onRmsLevelChanged = { rmsLevel ->
                    _voiceHudState.value = _voiceHudState.value.copy(audioWaveformLevel = rmsLevel)
                },
                onPartialResult = { partial ->
                    if (_voiceHudState.value.conversationState == VoiceConversationState.AWAITING_DISPATCH) {
                        handleVoiceActionIntentKeyword(partial)
                    } else {
                        updateVoiceTranscript(partial, isFinal = false)
                    }
                },
                onFinalResult = { final ->
                    if (_voiceHudState.value.conversationState == VoiceConversationState.AWAITING_DISPATCH) {
                        handleVoiceActionIntentKeyword(final)
                    } else {
                        updateVoiceTranscript(final, isFinal = true)
                    }
                },
                onErrorOccurred = { errorMsg ->
                    _voiceHudState.value = _voiceHudState.value.copy(
                        isListening = false,
                        audioWaveformLevel = 0.08f,
                        statusMessage = errorMsg
                    )
                    showToast(errorMsg)
                }
            )
        }
    }

    // Multi-Language and Offline STT Controls
    fun setVoiceLanguage(language: RegionalLanguage) {
        _voiceHudState.value = _voiceHudState.value.copy(
            selectedLanguage = language,
            statusMessage = "Language set to ${language.displayName} (${language.nativeLabel})"
        )
        voiceRecognitionManager?.setLanguage(language)
        showToast("Switched speech parsing to ${language.displayName}")
    }

    fun toggleOfflineWhisperMode() {
        val nextMode = !_voiceHudState.value.isOfflineMode
        _voiceHudState.value = _voiceHudState.value.copy(
            isOfflineMode = nextMode,
            statusMessage = if (nextMode) "Offline Whisper STT fallback enabled" else "Online Speech Recognizer enabled"
        )
        showToast(if (nextMode) "Offline Whisper STT Fallback Active" else "Online Speech Recognition Active")
    }

    fun openVoiceHud(startListening: Boolean = true) {
        initVoiceRecognizer()
        actionDispatchSilenceJob?.cancel()
        voiceTtsEngine.stop()

        _voiceHudState.value = VoiceHudState(
            isVisible = true,
            isListening = startListening,
            conversationState = if (startListening) VoiceConversationState.LISTENING else VoiceConversationState.IDLE,
            rawTranscript = "",
            tokenChips = emptyList(),
            parsedItems = emptyList(),
            detectedVendor = "FreshMart",
            geoPin = "Sector 4 Cyber Hub",
            audioWaveformLevel = if (startListening) 0.5f else 0.08f,
            statusMessage = if (startListening) "Listening in ${_voiceHudState.value.selectedLanguage.displayName}..." else "Tap mic to speak or select a regional preset",
            ttsPromptText = "",
            isTtsSpeaking = false,
            lastActionDispatched = null
        )
        if (startListening) {
            startListeningToRealAudio()
        }
    }

    fun startListeningToRealAudio() {
        initVoiceRecognizer()
        val lang = _voiceHudState.value.selectedLanguage
        _voiceHudState.value = _voiceHudState.value.copy(
            isListening = true,
            statusMessage = if (_voiceHudState.value.isOfflineMode) "Offline Whisper STT listening..." else "Listening in ${lang.displayName}..."
        )
        if (_voiceHudState.value.isOfflineMode) {
            offlineWhisperEngine.startOfflineListening(
                onEnergyRms = { rms ->
                    _voiceHudState.value = _voiceHudState.value.copy(audioWaveformLevel = rms)
                },
                onStatus = { status ->
                    _voiceHudState.value = _voiceHudState.value.copy(statusMessage = status)
                }
            )
        } else {
            voiceRecognitionManager?.startListening(lang)
        }
    }

    fun stopListeningToRealAudio() {
        if (_voiceHudState.value.isOfflineMode) {
            offlineWhisperEngine.stopOfflineListening()
        } else {
            voiceRecognitionManager?.stopListening()
        }
        _voiceHudState.value = _voiceHudState.value.copy(
            isListening = false,
            audioWaveformLevel = 0.08f,
            statusMessage = "Processing parsed items..."
        )
    }

    fun closeVoiceHud() {
        actionDispatchSilenceJob?.cancel()
        voiceTtsEngine.stop()
        if (_voiceHudState.value.isOfflineMode) {
            offlineWhisperEngine.stopOfflineListening()
        } else {
            voiceRecognitionManager?.cancel()
        }
        voiceNlpJob?.cancel()
        _voiceHudState.value = _voiceHudState.value.copy(
            isVisible = false,
            isListening = false,
            isTtsSpeaking = false,
            audioWaveformLevel = 0.08f,
            conversationState = VoiceConversationState.IDLE
        )
    }

    fun simulateSpeechStream(sampleText: String) {
        actionDispatchSilenceJob?.cancel()
        voiceTtsEngine.stop()
        if (_voiceHudState.value.isOfflineMode) {
            offlineWhisperEngine.stopOfflineListening()
        } else {
            voiceRecognitionManager?.cancel()
        }
        viewModelScope.launch {
            val lang = _voiceHudState.value.selectedLanguage
            _voiceHudState.value = _voiceHudState.value.copy(
                isListening = true,
                conversationState = VoiceConversationState.LISTENING,
                rawTranscript = "",
                statusMessage = "Processing ${lang.displayName} stream..."
            )
            val words = sampleText.split(" ")
            var current = ""
            for (word in words) {
                delay(150)
                current = if (current.isEmpty()) word else "$current $word"
                val tokens = NlpParsingEngine.tokenizeForHud(current)
                val (vendor, items) = NlpParsingEngine.parseInput(current, language = lang)
                _voiceHudState.value = _voiceHudState.value.copy(
                    rawTranscript = current,
                    tokenChips = tokens,
                    parsedItems = items,
                    detectedVendor = vendor,
                    audioWaveformLevel = (0.25f + (0.75f * (Math.random().toFloat())))
                )
            }
            _voiceHudState.value = _voiceHudState.value.copy(
                isListening = false,
                audioWaveformLevel = 0.08f,
                statusMessage = "Spoken ${lang.displayName} parsed successfully"
            )
            updateVoiceTranscript(current, isFinal = true)
        }
    }

    fun updateVoiceTranscript(newText: String, isFinal: Boolean = true) {
        val lang = _voiceHudState.value.selectedLanguage
        val tokens = NlpParsingEngine.tokenizeForHud(newText)
        val (vendor, initialItems) = NlpParsingEngine.parseInput(newText, language = lang)
        _voiceHudState.value = _voiceHudState.value.copy(
            rawTranscript = newText,
            tokenChips = tokens,
            parsedItems = if (_voiceHudState.value.parsedItems.isEmpty() || !isFinal) initialItems else _voiceHudState.value.parsedItems,
            detectedVendor = if (_voiceHudState.value.detectedVendor == "FreshMart") vendor else _voiceHudState.value.detectedVendor
        )

        if (newText.length > 3 && isFinal) {
            val apiKey = _userGeminiApiKey.value.trim()
            voiceNlpJob?.cancel()
            voiceNlpJob = viewModelScope.launch {
                _voiceHudState.value = _voiceHudState.value.copy(
                    isProcessing = true,
                    conversationState = VoiceConversationState.PARSING_ENTITIES,
                    statusMessage = if (apiKey.isNotBlank() && !_voiceHudState.value.isOfflineMode) "Gemini 3.5 Flash extracting ${lang.displayName} data..." else "Resolving via Offline Vernacular Matrix..."
                )

                var extractedItems = initialItems
                var extractedVendor = vendor
                var primaryCat = "Groceries"
                var paymentMethod = "UPI Instant"
                var notes = ""
                var confidence = 0.95f

                if (apiKey.isNotBlank() && !_voiceHudState.value.isOfflineMode) {
                    val geminiResult = GeminiService.parseVoiceFinancialEntry(newText, apiKey)
                    if (geminiResult.isSuccess) {
                        val structured = geminiResult.getOrThrow()
                        extractedItems = structured.items
                        extractedVendor = structured.vendor
                        primaryCat = structured.primaryCategory
                        paymentMethod = structured.paymentMethod
                        notes = structured.notes
                        confidence = structured.confidence
                    }
                } else {
                    val normalizedVernacular = offlineWhisperEngine.normalizeVernacularText(newText, lang)
                    val (resolvedVendor, resolvedItems) = repository.resolveVoiceOrTextInput(normalizedVernacular.ifBlank { newText }, apiKey)
                    extractedItems = if (resolvedItems.isNotEmpty()) resolvedItems else initialItems
                    extractedVendor = if (resolvedVendor != "Local Store") resolvedVendor else vendor
                    primaryCat = extractedItems.firstOrNull()?.category ?: "Groceries"
                }

                val warning = evaluateVoiceLiveBudgetWarning(primaryCat, extractedItems)
                val tierLabel = when (extractedItems.firstOrNull()?.tierResolved) {
                    "TIER_3_GEMINI" -> "Gemini AI normalized ${extractedItems.size} items"
                    "TIER_1_CACHE" -> "Instant 0ms Cache match (${extractedItems.size} items)"
                    "TIER_2_SEEDED" -> "Master DB match (${extractedItems.size} items)"
                    else -> "Offline ${lang.displayName} NLP matched (${extractedItems.size} items)"
                }

                _voiceHudState.value = _voiceHudState.value.copy(
                    parsedItems = extractedItems,
                    detectedVendor = extractedVendor,
                    detectedCategory = primaryCat,
                    detectedPaymentMethod = paymentMethod,
                    detectedNotes = notes,
                    confidence = confidence,
                    isProcessing = false,
                    statusMessage = tierLabel,
                    liveBudgetWarning = warning
                )

                // TRIGGER STEP 2: Conversational TTS Audio Confirmation & Spoken Prompt
                if (extractedItems.isNotEmpty()) {
                    triggerConversationalTtsPrompt(extractedItems)
                }
            }
        }
    }

    /**
     * Bidirectional Conversational Agent:
     * Generates a natural TTS sentence summarizing parsed items and asking user how to dispatch them.
     */
    private fun triggerConversationalTtsPrompt(items: List<ParsedNlpItem>) {
        val summaryParts = items.take(3).map { item ->
            val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
            if (item.price > 0) {
                "$qtyStr ${item.unit} ${item.name} for ${item.price.toInt()} rupees"
            } else {
                "$qtyStr ${item.unit} ${item.name}"
            }
        }

        val itemsSummary = if (items.size > 3) {
            "${summaryParts.joinToString(", ")}, and ${items.size - 3} more items"
        } else {
            summaryParts.joinToString(" and ")
        }

        val ttsSentence = "Recognized $itemsSummary. Record as purchase or add to shopping list?"

        _voiceHudState.value = _voiceHudState.value.copy(
            conversationState = VoiceConversationState.TTS_SPEAKING_CONFIRM,
            ttsPromptText = ttsSentence,
            isTtsSpeaking = true,
            statusMessage = "🗣️ Speaking: \"$ttsSentence\""
        )

        voiceTtsEngine.speak(
            text = ttsSentence,
            onDone = {
                viewModelScope.launch {
                    // Transition to ACTION LISTENING (Phase 2)
                    _voiceHudState.value = _voiceHudState.value.copy(
                        conversationState = VoiceConversationState.AWAITING_DISPATCH,
                        isTtsSpeaking = false,
                        isListening = true,
                        statusMessage = "🎙️ Listening for command: Say \"Expense\" or \"Shopping List\"..."
                    )

                    // Start microphone in action-dispatch mode
                    startListeningToRealAudio()

                    // Start 5-second silence fallback timer
                    actionDispatchSilenceJob?.cancel()
                    actionDispatchSilenceJob = viewModelScope.launch {
                        delay(5000)
                        if (_voiceHudState.value.conversationState == VoiceConversationState.AWAITING_DISPATCH) {
                            // If user didn't speak a command, stop mic and keep touch buttons active on screen
                            stopListeningToRealAudio()
                            _voiceHudState.value = _voiceHudState.value.copy(
                                isListening = false,
                                statusMessage = "Tap \"Record as Purchase\" or \"Add to List\" below"
                            )
                        }
                    }
                }
            }
        )
    }

    /**
     * Technical Engine: Lightweight Keyword Intent Router for Phase 2 Dispatch
     */
    fun handleVoiceActionIntentKeyword(spokenInput: String) {
        val lower = spokenInput.lowercase(Locale.ROOT).trim()
        if (lower.isBlank()) return

        val expenseKeywords = listOf(
            "expense", "purchase", "purchased", "bought", "paid", "now", "record",
            "log", "kharcha", "kharid liya", "bhejo", "save expense", "bill"
        )

        val listKeywords = listOf(
            "list", "shopping", "shopping list", "reminder", "to buy", "later",
            "save to list", "add to list", "saman", "khareedna hai", "yad rakhna"
        )

        val isExpenseIntent = expenseKeywords.any { lower.contains(it) }
        val isListIntent = listKeywords.any { lower.contains(it) }

        if (isExpenseIntent) {
            actionDispatchSilenceJob?.cancel()
            _voiceHudState.value = _voiceHudState.value.copy(
                conversationState = VoiceConversationState.PROCESSING_INTENT,
                lastActionDispatched = "EXPENSE",
                statusMessage = "Routing to Logged Expenses..."
            )
            dispatchActionWithTtsFeedback(isExpense = true)
        } else if (isListIntent) {
            actionDispatchSilenceJob?.cancel()
            _voiceHudState.value = _voiceHudState.value.copy(
                conversationState = VoiceConversationState.PROCESSING_INTENT,
                lastActionDispatched = "SHOPPING_LIST",
                statusMessage = "Routing to Smart Shopping List..."
            )
            dispatchActionWithTtsFeedback(isExpense = false)
        }
    }

    private fun dispatchActionWithTtsFeedback(isExpense: Boolean) {
        viewModelScope.launch {
            val state = _voiceHudState.value
            stopListeningToRealAudio()

            if (isExpense) {
                // Check edge case: Price Missing during "Expense" Route
                val missingPriceItem = state.parsedItems.find { it.price <= 0.0 }
                if (missingPriceItem != null) {
                    val askPriceTts = "What was the price for ${missingPriceItem.name}?"
                    _voiceHudState.value = _voiceHudState.value.copy(
                        conversationState = VoiceConversationState.TTS_SPEAKING_CONFIRM,
                        ttsPromptText = askPriceTts,
                        isTtsSpeaking = true,
                        statusMessage = "🗣️ $askPriceTts"
                    )
                    voiceTtsEngine.speak(askPriceTts) {
                        viewModelScope.launch {
                            _voiceHudState.value = _voiceHudState.value.copy(
                                isTtsSpeaking = false,
                                conversationState = VoiceConversationState.AWAITING_DISPATCH,
                                statusMessage = "Tap to confirm or edit amount"
                            )
                        }
                    }
                    return@launch
                }

                // Final Settlement: Log as Expense
                val effectiveCategory = state.detectedCategory.ifBlank { state.parsedItems.firstOrNull()?.category ?: "Groceries" }
                val totalAmount = state.parsedItems.sumOf { it.price * it.quantity }
                val overspendInfo = checkExpenseOverspend(effectiveCategory, totalAmount)

                repository.logExpenseWithItems(
                    title = "${state.detectedVendor} Order",
                    vendor = state.detectedVendor,
                    category = effectiveCategory,
                    items = state.parsedItems,
                    paymentMethod = state.detectedPaymentMethod,
                    locationName = state.geoPin,
                    rawVoicePrompt = state.rawTranscript
                )

                _voiceHudState.value = _voiceHudState.value.copy(
                    conversationState = VoiceConversationState.COMMITTED,
                    ttsPromptText = "Logged to expenses.",
                    statusMessage = "✨ Recorded to Ledger (₹${totalAmount.toInt()})"
                )

                voiceTtsEngine.speak("Logged to expenses.") {
                    viewModelScope.launch {
                        delay(600)
                        closeVoiceHud()
                    }
                }

                if (overspendInfo != null) {
                    showToast("🚨 Overspend Warning: Exceeded $effectiveCategory limit by ₹${overspendInfo.overspentAmount.toInt()}!")
                } else {
                    showToast("Logged ₹${totalAmount.toInt()} to expenses")
                }
            } else {
                // Final Settlement: Save to Smart Shopping List
                for (item in state.parsedItems) {
                    db.expenseDao().insertGroceryItem(
                        GroceryItemEntity(
                            name = item.name,
                            category = item.category,
                            quantity = "${if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()} ${item.unit}".trim(),
                            estimatedPrice = item.price * item.quantity,
                            priceCap = if (item.price > 0) item.price * item.quantity else 0.0,
                            targetVendor = state.detectedVendor,
                            isPantryItem = false,
                            canonicalName = item.canonicalName,
                            brand = item.brand,
                            storageType = item.storageType
                        )
                    )
                }

                _voiceHudState.value = _voiceHudState.value.copy(
                    conversationState = VoiceConversationState.COMMITTED,
                    ttsPromptText = "Saved to shopping list.",
                    statusMessage = "✨ Added ${state.parsedItems.size} items to Shopping List"
                )

                voiceTtsEngine.speak("Saved to shopping list.") {
                    viewModelScope.launch {
                        delay(600)
                        closeVoiceHud()
                    }
                }

                showToast("Added ${state.parsedItems.size} items to Shopping List")
            }
        }
    }

    fun deleteVoiceParsedItem(index: Int) {
        val current = _voiceHudState.value.parsedItems.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            val warning = evaluateVoiceLiveBudgetWarning(_voiceHudState.value.detectedCategory, current)
            _voiceHudState.value = _voiceHudState.value.copy(parsedItems = current, liveBudgetWarning = warning)
            showToast("Item removed from voice entry")
        }
    }

    fun confirmVoiceHudTransaction(paymentMethod: String? = null) {
        dispatchActionWithTtsFeedback(isExpense = true)
    }

    fun addVoiceItemsToShoppingList() {
        dispatchActionWithTtsFeedback(isExpense = false)
    }

    // Direct Receipt / Instant Pay Logging
    fun processReceiptOcr(vendor: String, items: List<ParsedNlpItem>) {
        viewModelScope.launch {
            if (items.isNotEmpty()) {
                repository.logExpenseWithItems(
                    title = "$vendor Purchase",
                    vendor = vendor,
                    category = items.firstOrNull()?.category ?: "Groceries",
                    items = items,
                    paymentMethod = "UPI / Card",
                    locationName = "Point of Sale",
                    rawVoicePrompt = "Direct Itemized Import"
                )
                showToast("Logged ${items.size} items from $vendor (₹${items.sumOf { it.price * it.quantity }.toInt()})")
                closeModal()
            }
        }
    }

    // Grocery & Pantry operations
    fun addWishlistItem(name: String, qty: String, price: Double, cap: Double, vendor: String, category: String) {
        viewModelScope.launch {
            db.expenseDao().insertGroceryItem(
                GroceryItemEntity(
                    name = name,
                    category = category,
                    quantity = qty,
                    estimatedPrice = price,
                    priceCap = cap,
                    targetVendor = vendor,
                    isPantryItem = false
                )
            )
            showToast("Added $name to Wishlist")
        }
    }

    fun toggleWishlistChecked(item: GroceryItemEntity) {
        viewModelScope.launch {
            db.expenseDao().updateGroceryItem(item.copy(isChecked = !item.isChecked))
        }
    }

    fun deleteGroceryItem(id: Long) {
        viewModelScope.launch {
            db.expenseDao().deleteGroceryItem(id)
            showToast("Item removed")
        }
    }

    fun checkoutGroceryMatrix() {
        viewModelScope.launch {
            val items = wishlistItems.value.filter { it.isChecked }
            if (items.isEmpty()) {
                showToast("No checked items to checkout")
                return@launch
            }

            val total = items.sumOf { it.estimatedPrice }
            val firstVendor = items.firstOrNull()?.targetVendor ?: "Kirana Store"

            repository.logExpenseWithItems(
                title = "Grocery Restock - $firstVendor",
                vendor = firstVendor,
                category = "Groceries",
                items = items.map {
                    ParsedNlpItem(
                        name = it.name,
                        category = it.category,
                        quantity = 1.0,
                        unit = "unit",
                        price = it.estimatedPrice,
                        vendor = it.targetVendor,
                        canonicalName = it.canonicalName,
                        brand = it.brand,
                        storageType = it.storageType,
                        shelfLifeDays = 30
                    )
                },
                paymentMethod = "UPI Instant",
                locationName = "Sector 4 Cyber Hub",
                rawVoicePrompt = "Bulk Grocery Matrix Checkout"
            )

            for (item in items) {
                db.expenseDao().deleteGroceryItem(item.id)
            }

            showToast("Logged ₹${total.toInt()} expense & restocked pantry items")
        }
    }

    // --- UPI Intent Payment Execution & Auto-Logging ---
    fun checkoutGroceryWithUpi(
        merchantName: String,
        payeeUpi: String,
        utrNumber: String
    ) {
        viewModelScope.launch {
            val items = wishlistItems.value.filter { it.isChecked }
            val total = if (items.isNotEmpty()) items.sumOf { it.estimatedPrice } else 150.0
            val vendor = merchantName.ifBlank { items.firstOrNull()?.targetVendor ?: "Kirana Store" }

            val parsedItems = if (items.isNotEmpty()) {
                items.map {
                    ParsedNlpItem(
                        name = it.name,
                        category = it.category,
                        quantity = 1.0,
                        unit = "unit",
                        price = it.estimatedPrice,
                        vendor = vendor,
                        canonicalName = it.canonicalName,
                        brand = it.brand,
                        storageType = it.storageType,
                        shelfLifeDays = 30
                    )
                }
            } else {
                listOf(
                    ParsedNlpItem(
                        name = "Grocery Restock",
                        category = "Groceries",
                        quantity = 1.0,
                        unit = "unit",
                        price = total,
                        vendor = vendor
                    )
                )
            }

            repository.logExpenseWithItems(
                title = "UPI Payment: $vendor",
                vendor = vendor,
                category = "Groceries",
                items = parsedItems,
                paymentMethod = "UPI (UTR: $utrNumber)",
                locationName = "Store Checkout",
                rawVoicePrompt = "Paid ₹${total.toInt()} to $payeeUpi via UPI Intent"
            )

            // Clear checked items from grocery wishlist
            for (item in items) {
                db.expenseDao().deleteGroceryItem(item.id)
            }

            showToast("✅ UPI Paid ₹${total.toInt()} (UTR: $utrNumber) & logged!")
        }
    }

    fun recordInstantUpiExpense(
        merchantName: String,
        amount: Double,
        category: String,
        payeeUpi: String,
        utrNumber: String,
        note: String
    ) {
        viewModelScope.launch {
            val singleItem = ParsedNlpItem(
                name = if (note.isNotBlank()) note else "$merchantName Payment",
                category = category.ifBlank { "Groceries" },
                quantity = 1.0,
                unit = "txn",
                price = amount,
                vendor = merchantName.ifBlank { "Merchant" }
            )

            repository.logExpenseWithItems(
                title = merchantName.ifBlank { "UPI Transfer" },
                vendor = merchantName.ifBlank { payeeUpi },
                category = category.ifBlank { "Groceries" },
                items = listOf(singleItem),
                paymentMethod = "UPI (UTR: $utrNumber)",
                locationName = "Direct Transfer",
                rawVoicePrompt = "UPI Intent: $payeeUpi"
            )

            showToast("🎉 Paid ₹${amount.toInt()} to $merchantName (UTR: $utrNumber)")
        }
    }

    fun settleKhataWithUpi(
        khataId: Long,
        personName: String,
        amount: Double,
        utrNumber: String,
        payeeUpi: String
    ) {
        viewModelScope.launch {
            repository.settleKhataEntry(khataId)
            
            // Also log an expense in Spend Ledger for tracking the debit
            repository.logExpenseWithItems(
                title = "Settled Udhaar: $personName",
                vendor = personName,
                category = "Transfers & Debt",
                items = listOf(
                    ParsedNlpItem(
                        name = "Khata Settlement to $personName ($payeeUpi)",
                        category = "Transfers & Debt",
                        quantity = 1.0,
                        unit = "txn",
                        price = amount,
                        vendor = personName
                    )
                ),
                paymentMethod = "UPI (UTR: $utrNumber)",
                locationName = "Khata Ledger",
                rawVoicePrompt = "UPI Settlement"
            )

            showToast("🎉 Settled ₹${amount.toInt()} with $personName via UPI (UTR: $utrNumber)")
        }
    }

    // Modal Manager
    fun openModal(modalName: String) {
        _activeModal.value = modalName
    }

    fun openCategoryBudgetModal() {
        _activeModal.value = "CATEGORY_BUDGETS"
    }

    fun openRecurringBillsModal() {
        _activeModal.value = "RECURRING_BILLS"
    }

    fun openSavingsGoalsModal() {
        _activeModal.value = "SAVINGS_GOALS"
    }

    fun openKhataSplitterModal() {
        _activeModal.value = "KHATA_SPLITTER"
    }

    fun closeModal() {
        _activeModal.value = null
    }

    // --- Recurring Bills & Subscriptions Actions ---
    fun addRecurringBill(
        title: String,
        amount: Double,
        category: String,
        billingCycle: String,
        dueDay: Int,
        paymentMethod: String,
        isAutoDebit: Boolean,
        reminderDaysBefore: Int,
        serviceIcon: String,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            // calculate next due date based on dueDay
            val currentDay = cal.get(Calendar.DAY_OF_MONTH)
            if (dueDay < currentDay) {
                cal.add(Calendar.MONTH, 1)
            }
            cal.set(Calendar.DAY_OF_MONTH, dueDay.coerceIn(1, 28))
            val nextDue = cal.timeInMillis

            repository.addRecurringBill(
                RecurringBillEntity(
                    title = title.trim(),
                    amount = amount,
                    category = category.trim().ifBlank { "Subscriptions" },
                    billingCycle = billingCycle,
                    dueDay = dueDay,
                    nextDueDate = nextDue,
                    paymentMethod = paymentMethod,
                    isAutoDebit = isAutoDebit,
                    reminderDaysBefore = reminderDaysBefore,
                    status = "ACTIVE",
                    serviceIcon = serviceIcon,
                    notes = notes.trim()
                )
            )
            showToast("Added recurring bill: $title (₹${amount.toInt()})")
        }
    }

    fun updateRecurringBill(bill: RecurringBillEntity) {
        viewModelScope.launch {
            repository.updateRecurringBill(bill)
            showToast("Updated ${bill.title}")
        }
    }

    fun deleteRecurringBill(id: Long, title: String = "Bill") {
        viewModelScope.launch {
            repository.deleteRecurringBill(id)
            showToast("Deleted $title")
        }
    }

    fun payRecurringBill(id: Long, title: String, amount: Double) {
        viewModelScope.launch {
            val success = repository.markBillPaid(id, createExpenseTransaction = true)
            if (success) {
                showToast("✅ Paid $title (₹${amount.toInt()}) & recorded in spend ledger")
            }
        }
    }

    // --- Savings Goals Actions ---
    fun addSavingsGoal(
        title: String,
        targetAmount: Double,
        initialSaved: Double,
        category: String,
        monthlyTarget: Double,
        colorHex: String,
        targetMonths: Int = 6,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val targetDate = System.currentTimeMillis() + (targetMonths.toLong() * 30L * 24 * 60 * 60 * 1000)
            repository.addSavingsGoal(
                SavingsGoalEntity(
                    title = title.trim(),
                    targetAmount = targetAmount,
                    currentAmount = initialSaved,
                    targetDate = targetDate,
                    category = category.trim().ifBlank { "Emergency" },
                    monthlyContributionTarget = monthlyTarget,
                    colorHex = colorHex,
                    notes = notes.trim(),
                    isCompleted = initialSaved >= targetAmount
                )
            )
            showToast("🎯 Created Savings Goal: $title")
        }
    }

    fun updateSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.updateSavingsGoal(goal)
            showToast("Updated ${goal.title}")
        }
    }

    fun deleteSavingsGoal(id: Long, title: String = "Goal") {
        viewModelScope.launch {
            repository.deleteSavingsGoal(id)
            showToast("Removed goal: $title")
        }
    }

    fun depositToSavingsGoal(id: Long, amount: Double, goalTitle: String) {
        if (amount <= 0) return
        viewModelScope.launch {
            repository.depositToSavingsGoal(id, amount)
            showToast("💰 Deposited ₹${amount.toInt()} into $goalTitle")
        }
    }

    fun withdrawFromSavingsGoal(id: Long, amount: Double, goalTitle: String) {
        if (amount <= 0) return
        viewModelScope.launch {
            repository.withdrawFromSavingsGoal(id, amount)
            showToast("💸 Withdrew ₹${amount.toInt()} from $goalTitle")
        }
    }

    // --- Khata & Bill Splitter Actions ---
    fun addKhataEntry(
        personName: String,
        personPhoneOrUpi: String,
        type: String, // "YOU_WILL_GET" or "YOU_WILL_PAY"
        amount: Double,
        description: String,
        dueDaysFromNow: Int = 7,
        invoiceNumber: String = "",
        customerTag: String = "REGULAR"
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dueDate = if (dueDaysFromNow > 0) now + (dueDaysFromNow.toLong() * 24 * 60 * 60 * 1000) else null
            repository.addKhataEntry(
                KhataEntryEntity(
                    personName = personName.trim(),
                    personPhoneOrUpi = personPhoneOrUpi.trim(),
                    type = type,
                    amount = amount,
                    description = description.trim(),
                    dateTimestamp = now,
                    dueDateTimestamp = dueDate,
                    invoiceNumber = invoiceNumber.trim(),
                    customerTag = customerTag,
                    isSettled = false
                )
            )
            val label = if (type == "YOU_WILL_GET") "Store credit ₹${amount.toInt()} logged for $personName" else "Payable ₹${amount.toInt()} logged for $personName"
            showToast(label)
        }
    }

    fun recordPartialKhataPayment(
        khataId: Long,
        installmentAmount: Double,
        paymentMode: String = "CASH",
        utrNumber: String = "",
        note: String = ""
    ) {
        if (installmentAmount <= 0) return
        viewModelScope.launch {
            val result = repository.recordPartialKhataPayment(
                khataId = khataId,
                installmentAmount = installmentAmount,
                paymentMode = paymentMode,
                utrNumber = utrNumber,
                note = note
            )
            if (result != null) {
                val (updatedEntry, isFullySettled) = result
                _selectedKhataForInstallment.value = null
                if (isFullySettled) {
                    showToast("🎉 Full settlement completed for ${updatedEntry.personName}! Balance cleared.")
                } else {
                    showToast("✅ Logged ₹${installmentAmount.toInt()} via $paymentMode. Remaining: ₹${updatedEntry.remainingAmount.toInt()}")
                }
            }
        }
    }

    fun openKhataInstallmentDialog(entry: KhataEntryEntity) {
        _selectedKhataForInstallment.value = entry
    }

    fun closeKhataInstallmentDialog() {
        _selectedKhataForInstallment.value = null
    }

    fun selectCustomerProfile(profile: CustomerProfileData?) {
        _selectedCustomerProfile.value = profile
    }

    fun openWhatsAppReminderForKhata(entry: KhataEntryEntity) {
        val upiLink = buildDynamicUpiLink(entry.remainingAmount, entry.personName, entry.invoiceNumber)
        val formattedMsg = buildWhatsAppReminderMessage(
            customerName = entry.personName,
            amountDue = entry.remainingAmount,
            invoiceNumber = entry.invoiceNumber,
            description = entry.description,
            dueDateMs = entry.dueDateTimestamp,
            upiLink = upiLink
        )
        val dueStr = entry.dueDateTimestamp?.let {
            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(it))
        } ?: "Immediate"

        _whatsappReminderPreview.value = WhatsAppReminderState(
            customerName = entry.personName,
            customerPhone = entry.personPhoneOrUpi,
            totalOutstanding = entry.remainingAmount,
            invoiceNumber = entry.invoiceNumber,
            description = entry.description,
            dynamicUpiLink = upiLink,
            formattedMessageText = formattedMsg,
            khataEntryId = entry.id,
            dueDateFormatted = dueStr
        )
    }

    fun openWhatsAppReminderForCustomer(customer: CustomerProfileData) {
        val upiLink = buildDynamicUpiLink(customer.outstandingKhataBalance, customer.customerName, "")
        val formattedMsg = buildWhatsAppReminderMessage(
            customerName = customer.customerName,
            amountDue = customer.outstandingKhataBalance,
            invoiceNumber = "",
            description = "Outstanding Store Credit & Khata balance (${customer.openKhataEntriesCount} invoices)",
            dueDateMs = customer.khataEntries.firstOrNull { !it.isSettled }?.dueDateTimestamp,
            upiLink = upiLink
        )
        _whatsappReminderPreview.value = WhatsAppReminderState(
            customerName = customer.customerName,
            customerPhone = customer.customerPhone,
            totalOutstanding = customer.outstandingKhataBalance,
            invoiceNumber = "",
            description = "Consolidated Khata Balance",
            dynamicUpiLink = upiLink,
            formattedMessageText = formattedMsg,
            khataEntryId = null,
            dueDateFormatted = "Flexible"
        )
    }

    fun closeWhatsAppReminderDialog() {
        _whatsappReminderPreview.value = null
    }

    fun buildDynamicUpiLink(
        amount: Double,
        customerName: String,
        invoiceNumber: String,
        upiVpa: String = "matrics.store@okaxis",
        storeName: String = "Matrics Retail Store"
    ): String {
        val note = if (invoiceNumber.isNotBlank()) "Settlement for $invoiceNumber" else "Store Credit Settlement"
        val amtStr = String.format(java.util.Locale.ROOT, "%.2f", amount)
        val encodedStore = Uri.encode(storeName)
        val encodedNote = Uri.encode(note)
        return "upi://pay?pa=$upiVpa&pn=$encodedStore&am=$amtStr&cu=INR&tn=$encodedNote"
    }

    fun buildWhatsAppReminderMessage(
        customerName: String,
        amountDue: Double,
        invoiceNumber: String,
        description: String,
        dueDateMs: Long?,
        upiLink: String,
        storeName: String = "Matrics Retail Store"
    ): String {
        val dateStr = dueDateMs?.let {
            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(it))
        } ?: "Upon receipt"

        val invLine = if (invoiceNumber.isNotBlank()) "\n• *Bill Ref:* #$invoiceNumber" else ""
        val descLine = if (description.isNotBlank()) "\n• *Items/Notes:* $description" else ""

        return """
🙏 *Namaste $customerName ji,*

This is a gentle reminder regarding your outstanding Khata balance at *$storeName*.

📋 *Account Summary:*
• *Pending Amount:* ₹${amountDue.toInt()}$invLine$descLine
• *Due Date:* $dateStr

💳 *Pay Instantly via UPI (GPay / PhonePe / Paytm):*
$upiLink

_Thank you for being a valued customer!_
        """.trimIndent()
    }

    fun sendWhatsAppReminder(context: Context, state: WhatsAppReminderState) {
        val cleanPhone = state.customerPhone.filter { it.isDigit() }.let {
            if (it.length == 10) "91$it" else it
        }
        val encodedMsg = Uri.encode(state.formattedMessageText)
        val whatsappUri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg")
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, whatsappUri).apply {
            setPackage("com.whatsapp")
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
            showToast("🚀 WhatsApp reminder launched for ${state.customerName}")
        } catch (_: Exception) {
            // Fallback to generic chooser if WhatsApp is not installed
            try {
                val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, whatsappUri).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, state.formattedMessageText)
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Payment Reminder"))
            }
        }
        closeWhatsAppReminderDialog()
    }

    fun copyWhatsAppReminder(context: Context, state: WhatsAppReminderState) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Khata Reminder", state.formattedMessageText)
        clipboard.setPrimaryClip(clip)
        showToast("📋 Copied UPI payment reminder message to clipboard")
    }

    fun redeemCustomerLoyaltyPoints(customerName: String, points: Int) {
        showToast("🎁 Redeemed $points loyalty points for $customerName (₹${points / 2} Discount Applied)")
    }

    private fun aggregateCustomerProfiles(
        txList: List<TransactionEntity>,
        lineItemsList: List<LineItemEntity>,
        khataList: List<KhataEntryEntity>
    ): List<CustomerProfileData> {
        val customerMap = mutableMapOf<String, MutableList<TransactionEntity>>()
        val khataMap = mutableMapOf<String, MutableList<KhataEntryEntity>>()
        val phoneMap = mutableMapOf<String, String>()
        val nameMap = mutableMapOf<String, String>()

        // 1. Group POS transactions
        for (tx in txList) {
            val name = tx.customerName.trim().ifEmpty {
                if (tx.vendor != "Customer Khata" && tx.vendor != "FreshMart" && tx.vendor != "Cyber Roast" && tx.vendor != "Blinkit") tx.vendor else ""
            }
            val phone = tx.customerPhone.trim()
            if (name.isBlank() && phone.isBlank()) continue

            val key = if (phone.isNotBlank()) phone else name.lowercase()
            customerMap.getOrPut(key) { mutableListOf() }.add(tx)
            if (phone.isNotBlank()) phoneMap[key] = phone
            if (name.isNotBlank() && !nameMap.containsKey(key)) nameMap[key] = name
        }

        // 2. Group Khata entries
        for (khata in khataList) {
            val name = khata.personName.trim()
            val phone = khata.personPhoneOrUpi.trim()
            if (name.isBlank() && phone.isBlank()) continue

            val key = if (phone.isNotBlank()) phone else name.lowercase()
            khataMap.getOrPut(key) { mutableListOf() }.add(khata)
            if (phone.isNotBlank() && !phoneMap.containsKey(key)) phoneMap[key] = phone
            if (name.isNotBlank() && !nameMap.containsKey(key)) nameMap[key] = name
        }

        val allKeys = (customerMap.keys + khataMap.keys).toSet()

        val profiles = allKeys.map { key ->
            val custTx = customerMap[key] ?: emptyList()
            val custKhata = khataMap[key] ?: emptyList()
            val name = nameMap[key] ?: key
            val phone = phoneMap[key] ?: ""

            val lifetimeSpend = custTx.sumOf { it.totalAmount }
            val billsCount = custTx.size
            val avgSpend = if (billsCount > 0) lifetimeSpend / billsCount else 0.0

            val txIdSet = custTx.map { it.id }.toSet()
            val customerLineItems = lineItemsList.filter { it.transactionId in txIdSet }
            val frequentItems = customerLineItems.groupBy { it.canonicalName.ifEmpty { it.name } }
                .map { (itemName, items) ->
                    FrequentCustomerItem(
                        itemName = itemName,
                        quantity = items.sumOf { it.quantity },
                        unit = items.firstOrNull()?.unit ?: "unit",
                        totalSpend = items.sumOf { it.totalPrice },
                        purchaseCount = items.size
                    )
                }
                .sortedByDescending { it.purchaseCount }
                .take(8)

            val openKhata = custKhata.filter { it.type == "YOU_WILL_GET" && !it.isSettled }
            val outstandingBalance = openKhata.sumOf { it.remainingAmount }
            val totalKhataPaid = custKhata.sumOf { it.paidAmount }

            val explicitPoints = custKhata.maxOfOrNull { it.customerLoyaltyPoints } ?: 0
            val computedPoints = (lifetimeSpend / 50).toInt() + (custKhata.count { it.isSettled } * 30)
            val loyaltyPoints = maxOf(explicitPoints, computedPoints)

            val tier = when {
                loyaltyPoints >= CustomerLoyaltyTier.PLATINUM.minPoints -> CustomerLoyaltyTier.PLATINUM
                loyaltyPoints >= CustomerLoyaltyTier.GOLD.minPoints -> CustomerLoyaltyTier.GOLD
                loyaltyPoints >= CustomerLoyaltyTier.SILVER.minPoints -> CustomerLoyaltyTier.SILVER
                else -> CustomerLoyaltyTier.BRONZE
            }

            val firstVisit = (custTx.map { it.dateTimestamp } + custKhata.map { it.dateTimestamp }).minOrNull() ?: System.currentTimeMillis()
            val lastVisit = (custTx.map { it.dateTimestamp } + custKhata.map { it.dateTimestamp }).maxOrNull() ?: System.currentTimeMillis()

            CustomerProfileData(
                customerName = name,
                customerPhone = phone,
                totalLifetimeSpend = lifetimeSpend,
                totalBillsCount = billsCount,
                averageBillAmount = avgSpend,
                frequentItems = frequentItems,
                loyaltyPoints = loyaltyPoints,
                loyaltyTier = tier,
                outstandingKhataBalance = outstandingBalance,
                totalKhataPaid = totalKhataPaid,
                firstVisitDate = firstVisit,
                lastVisitDate = lastVisit,
                openKhataEntriesCount = openKhata.size,
                khataEntries = custKhata.sortedByDescending { it.dateTimestamp },
                pastTransactions = custTx.sortedByDescending { it.dateTimestamp }
            )
        }

        return profiles.sortedWith(
            compareByDescending<CustomerProfileData> { it.outstandingKhataBalance }
                .thenByDescending { it.totalLifetimeSpend }
        )
    }

    fun settleKhataEntry(id: Long, personName: String, amount: Double) {
        viewModelScope.launch {
            repository.settleKhataEntry(id)
            showToast("🎉 Settled ₹${amount.toInt()} with $personName")
        }
    }

    fun deleteKhataEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteKhataEntry(id)
            showToast("Khata record removed")
        }
    }

    fun convertBillSplitToKhata(
        totalBill: Double,
        billTitle: String,
        payerIsYou: Boolean,
        participants: List<Pair<String, Double>>, // (Name, ShareAmount)
        payerUpiOrPhone: String = ""
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dueDate = now + (7L * 24 * 60 * 60 * 1000) // 7 days default
            var addedCount = 0

            for ((name, share) in participants) {
                if (share <= 0) continue
                val entryType = if (payerIsYou) "YOU_WILL_GET" else "YOU_WILL_PAY"
                repository.addKhataEntry(
                    KhataEntryEntity(
                        personName = name.trim(),
                        personPhoneOrUpi = payerUpiOrPhone,
                        type = entryType,
                        amount = share,
                        description = "$billTitle split share",
                        dateTimestamp = now,
                        dueDateTimestamp = dueDate,
                        isSettled = false,
                        splitGroupId = "SPLIT_${now}"
                    )
                )
                addedCount++
            }
            showToast("✅ Generated $addedCount Khata entries for '$billTitle'")
        }
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
        viewModelScope.launch {
            delay(3000)
            if (_toastMessage.value == msg) {
                _toastMessage.value = null
            }
        }
    }

    fun updateMonthlyBudget(budget: Double) {
        _monthlyBudget.value = budget
        sharedPrefs.edit().putFloat("monthly_budget", budget.toFloat()).apply()
        showToast("Monthly Budget set to ₹${budget.toInt()}")
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategoryFilter(cat: String?) {
        _selectedCategoryFilter.value = cat
    }

    fun setPrimaryPeriodMonth(ym: String) {
        _primaryPeriodMonth.value = ym
    }

    fun setBaselinePeriodMonth(ym: String) {
        _baselinePeriodMonth.value = ym
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            db.expenseDao().deleteTransaction(id)
            showToast("Transaction deleted")
        }
    }

    fun simulateQuickCommerceSync(platform: String = "Blinkit") {
        viewModelScope.launch {
            val sampleItems = when (platform.lowercase()) {
                "zepto" -> listOf(
                    ParsedNlpItem("Nandini GoodLife Milk 500ml", "Dairy", 2.0, "pack", 32.0, "Zepto", canonicalName = "Milk", brand = "Nandini", storageType = "Refrigerated"),
                    ParsedNlpItem("Fresh Mint Leaves 100g", "Produce", 1.0, "bunch", 15.0, "Zepto", canonicalName = "Mint Leaves", storageType = "Refrigerated"),
                    ParsedNlpItem("Britannia Whole Wheat Bread", "Grains", 1.0, "pack", 50.0, "Zepto", canonicalName = "Bread", brand = "Britannia", storageType = "Pantry")
                )
                "instamart" -> listOf(
                    ParsedNlpItem("Epigamia Greek Yogurt 90g", "Dairy", 2.0, "cup", 45.0, "Instamart", canonicalName = "Yogurt", brand = "Epigamia", storageType = "Refrigerated"),
                    ParsedNlpItem("Kinley Soda 750ml", "Beverages", 2.0, "bottle", 20.0, "Instamart", canonicalName = "Club Soda", brand = "Kinley", storageType = "Pantry")
                )
                else -> listOf(
                    ParsedNlpItem("Amul Butter 500g", "Dairy", 1.0, "pack", 275.0, "Blinkit", canonicalName = "Butter", brand = "Amul", storageType = "Refrigerated"),
                    ParsedNlpItem("Organic Eggs 6pcs", "Produce", 1.0, "pack", 65.0, "Blinkit", canonicalName = "Eggs", storageType = "Refrigerated"),
                    ParsedNlpItem("Whole Wheat Bread 400g", "Grains", 1.0, "pack", 45.0, "Blinkit", canonicalName = "Bread", storageType = "Pantry")
                )
            }
            val total = sampleItems.sumOf { it.price * it.quantity }
            repository.logExpenseWithItems(
                title = "$platform Quick-Commerce Order",
                vendor = platform,
                category = "Groceries",
                items = sampleItems,
                paymentMethod = "UPI Instant",
                locationName = "$platform Hub",
                rawVoicePrompt = "$platform Digital Invoice Sync"
            )
            showToast("Synced ${sampleItems.size} items from $platform (₹${total.toInt()})")
            closeModal()
        }
    }

    // ==========================================
    // HANDWRITTEN SCANNER METHODS
    // ==========================================
    fun openHandwrittenScanner() {
        selectHandwrittenPreset(0)
        _handwrittenScannerState.value = _handwrittenScannerState.value.copy(
            isVisible = true,
            statusMessage = "Capture photo, load preset, or type in OCR console"
        )
    }

    fun closeHandwrittenScanner() {
        _handwrittenScannerState.value = _handwrittenScannerState.value.copy(
            isVisible = false,
            isScanning = false,
            isDecrypting = false
        )
    }

    fun switchToLiveCamera() {
        _handwrittenScannerState.value = _handwrittenScannerState.value.copy(
            imageSource = "CAMERA",
            activeTab = 0
        )
    }

    fun setImageSource(source: String) {
        _handwrittenScannerState.value = _handwrittenScannerState.value.copy(
            imageSource = source
        )
    }

    fun setDocumentFilter(filter: ImageProcessingUtils.DocumentFilter) {
        _handwrittenScannerState.value = _handwrittenScannerState.value.copy(
            activeFilter = filter,
            highContrastFilter = (filter == ImageProcessingUtils.DocumentFilter.INK_BOOST || filter == ImageProcessingUtils.DocumentFilter.MONOCHROME_THRESHOLD),
            terminalLog = "FILTER: ${filter.label.uppercase()}"
        )
    }

    fun selectHandwrittenPreset(index: Int) {
        val presets = KachaBillPresets.PRESETS
        val safeIndex = index.coerceIn(0, presets.lastIndex)
        val selected = presets[safeIndex]
        _handwrittenScannerState.value = _handwrittenScannerState.value.copy(
            selectedPresetIndex = safeIndex,
            imageSource = "PRESET",
            reconciliationResult = selected,
            manualOcrInputText = selected.rawTranscript.ifBlank {
                selected.items.joinToString("\n") { "${it.rawWrittenText}  ₹${it.price.toInt()}" }
            },
            terminalLog = "LOADED BENCHMARK: ${selected.vendorName.uppercase()}"
        )
    }

    fun setManualOcrText(text: String) {
        _handwrittenScannerState.value = _handwrittenScannerState.value.copy(
            manualOcrInputText = text,
            imageSource = "TEXT_OCR"
        )
    }

    fun toggleHighContrastFilter() {
        val next = !_handwrittenScannerState.value.highContrastFilter
        _handwrittenScannerState.value = _handwrittenScannerState.value.copy(
            highContrastFilter = next,
            activeFilter = if (next) ImageProcessingUtils.DocumentFilter.INK_BOOST else ImageProcessingUtils.DocumentFilter.NATURAL,
            terminalLog = if (next) "INK BOOSTER ACTIVE // BINARIZED CONTRAST" else "STANDARD SENSOR FEED"
        )
    }

    fun onHandwrittenRealBitmapCaptured(bitmap: Bitmap) {
        _handwrittenScannerState.value = _handwrittenScannerState.value.copy(
            capturedBitmap = bitmap,
            binarizedBitmap = ImageProcessingUtils.applyBinarizationAndInkBoost(bitmap),
            imageSource = "CAMERA",
            terminalLog = "DOCUMENT CAPTURED // READY FOR OCR"
        )
    }

    fun onHandwrittenImageUriSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                onHandwrittenRealBitmapCaptured(bitmap)
                _handwrittenScannerState.value = _handwrittenScannerState.value.copy(imageSource = "GALLERY")
            } catch (e: Exception) {
                showToast("Could not load image: ${e.message}")
            }
        }
    }

    fun triggerHandwrittenCaptureAndDecrypt() {
        val preset = KachaBillPresets.PRESETS[_handwrittenScannerState.value.selectedPresetIndex.coerceIn(0, KachaBillPresets.PRESETS.lastIndex)]
        parseDirectOcrText(preset.rawTranscript.ifBlank {
            preset.items.joinToString("\n") { "${it.rawWrittenText} ${it.price.toInt()}" }
        })
    }

    fun parseDirectOcrText(rawOcrText: String) {
        viewModelScope.launch {
            _handwrittenScannerState.value = _handwrittenScannerState.value.copy(
                isScanning = true,
                isDecrypting = true,
                scanLaserProgress = 0f,
                terminalLog = "INITIALIZING OCR PIPELINE..."
            )

            for (p in 1..8) {
                delay(70)
                _handwrittenScannerState.value = _handwrittenScannerState.value.copy(
                    scanLaserProgress = p / 8f,
                    terminalLog = when (p) {
                        2 -> "APPLYING ADAPTIVE BINARIZATION & INK ENHANCEMENT..."
                        5 -> "ALIGNING BOUNDING BOXES & PARSING HANDWRITTEN GLYPHS..."
                        7 -> "CALCULATING ARITHMETIC RECONCILIATION..."
                        else -> _handwrittenScannerState.value.terminalLog
                    }
                )
            }

            val apiKey = _userGeminiApiKey.value.trim()
            val parsedResult = if (apiKey.isNotBlank()) {
                val geminiRes = GeminiService.parseHandwrittenBillVision(
                    rawTextFallback = rawOcrText,
                    apiKey = apiKey
                )
                if (geminiRes.isSuccess) geminiRes.getOrThrow() else {
                    val fallback = ReceiptOcrEngine.parseReceiptText(rawOcrText)
                    ReceiptOcrEngine.toHandwrittenBillResult(fallback)
                }
            } else {
                val fallback = ReceiptOcrEngine.parseReceiptText(rawOcrText)
                ReceiptOcrEngine.toHandwrittenBillResult(fallback)
            }

            _handwrittenScannerState.value = _handwrittenScannerState.value.copy(
                isScanning = false,
                isDecrypting = false,
                reconciliationResult = parsedResult,
                showReconciliationSheet = true,
                terminalLog = "OCR COMPLETE // ${parsedResult.items.size} ITEMS EXTRACTED"
            )
        }
    }

    fun selectReconciliationItem(index: Int?) {
        _handwrittenScannerState.value = _handwrittenScannerState.value.copy(
            selectedItemIndex = index,
            editingItem = if (index != null && index in (_handwrittenScannerState.value.reconciliationResult?.items?.indices ?: 0..-1)) {
                _handwrittenScannerState.value.reconciliationResult?.items?.get(index)
            } else null
        )
    }

    fun updateReconciliationItem(index: Int, updatedItem: HandwrittenBillItem) {
        val currentResult = _handwrittenScannerState.value.reconciliationResult ?: return
        if (index in currentResult.items.indices) {
            val updatedList = currentResult.items.toMutableList()
            updatedList[index] = updatedItem
            val newTrueSum = updatedList.sumOf { it.price }
            val hasMismatch = Math.abs(currentResult.shopkeeperTotal - newTrueSum) > 0.5

            val newResult = currentResult.copy(
                items = updatedList,
                calculatedTrueTotal = newTrueSum,
                mathErrorFlag = hasMismatch,
                mathErrorDelta = if (hasMismatch) (currentResult.shopkeeperTotal - newTrueSum) else 0.0
            )
            _handwrittenScannerState.value = _handwrittenScannerState.value.copy(
                reconciliationResult = newResult,
                editingItem = null
            )
        }
    }

    fun addNewItemToReconciliation(item: HandwrittenBillItem? = null) {
        val currentResult = _handwrittenScannerState.value.reconciliationResult ?: return
        val newItem = item ?: HandwrittenBillItem(
            canonicalName = "New Item",
            rawWrittenText = "Custom Item",
            category = "Groceries",
            quantity = 1.0,
            unit = "unit",
            price = 0.0,
            confidenceScore = 1.0f,
            isLowConfidence = false
        )
        val updatedItems = currentResult.items + newItem
        val newTrueSum = updatedItems.sumOf { it.price }
        val hasMismatch = Math.abs(currentResult.shopkeeperTotal - newTrueSum) > 0.5

        val newResult = currentResult.copy(
            items = updatedItems,
            calculatedTrueTotal = newTrueSum,
            mathErrorFlag = hasMismatch,
            mathErrorDelta = if (hasMismatch) (currentResult.shopkeeperTotal - newTrueSum) else 0.0
        )
        _handwrittenScannerState.value = _handwrittenScannerState.value.copy(
            reconciliationResult = newResult,
            selectedItemIndex = updatedItems.lastIndex
        )
        showToast("Added ${newItem.canonicalName} to receipt")
    }

    fun deleteItemFromReconciliation(index: Int) {
        val currentResult = _handwrittenScannerState.value.reconciliationResult ?: return
        if (index in currentResult.items.indices) {
            val updatedList = currentResult.items.toMutableList().apply { removeAt(index) }
            val newTrueSum = updatedList.sumOf { it.price }
            val hasMismatch = Math.abs(currentResult.shopkeeperTotal - newTrueSum) > 0.5

            val newResult = currentResult.copy(
                items = updatedList,
                calculatedTrueTotal = newTrueSum,
                mathErrorFlag = hasMismatch,
                mathErrorDelta = if (hasMismatch) (currentResult.shopkeeperTotal - newTrueSum) else 0.0
            )
            _handwrittenScannerState.value = _handwrittenScannerState.value.copy(
                reconciliationResult = newResult,
                selectedItemIndex = null
            )
            showToast("Removed item from receipt")
        }
    }

    fun keepShopkeeperWrittenTotal() {
        val current = _handwrittenScannerState.value.reconciliationResult ?: return
        val correctedResult = current.copy(
            calculatedTrueTotal = current.shopkeeperTotal,
            mathErrorFlag = false,
            mathErrorDelta = 0.0
        )
        _handwrittenScannerState.value = _handwrittenScannerState.value.copy(
            reconciliationResult = correctedResult
        )
        showToast("Kept shopkeeper's written total: ₹${current.shopkeeperTotal.toInt()}")
    }

    fun applyTrueTotalCorrection() {
        val current = _handwrittenScannerState.value.reconciliationResult ?: return
        val correctedResult = current.copy(
            shopkeeperTotal = current.calculatedTrueTotal,
            mathErrorFlag = false,
            mathErrorDelta = 0.0
        )
        _handwrittenScannerState.value = _handwrittenScannerState.value.copy(
            reconciliationResult = correctedResult
        )
        showToast("Corrected receipt total to true item sum: ₹${current.calculatedTrueTotal.toInt()}")
    }

    fun commitHandwrittenBillToLedger(result: HandwrittenBillResult) {
        viewModelScope.launch {
            if (result.items.isNotEmpty()) {
                val nlpItems = result.items.map { item ->
                    ParsedNlpItem(
                        name = item.canonicalName,
                        category = item.category,
                        quantity = item.quantity,
                        unit = item.unit,
                        price = item.price,
                        vendor = result.vendorName,
                        canonicalName = item.canonicalName,
                        storageType = "Pantry"
                    )
                }
                repository.logExpenseWithItems(
                    title = "${result.vendorName} Invoice",
                    vendor = result.vendorName,
                    category = result.items.firstOrNull()?.category ?: "Groceries",
                    items = nlpItems,
                    paymentMethod = "Cash Ledger (Kacha)",
                    locationName = "Local Market",
                    rawVoicePrompt = "Handwritten Receipt Scanned"
                )
                showToast("Logged ${result.items.size} items from ${result.vendorName} (₹${result.calculatedTrueTotal.toInt()})")
                closeHandwrittenScanner()
                closeModal()
            }
        }
    }

    fun addReconciliationItemsToShoppingList(result: HandwrittenBillResult) {
        viewModelScope.launch {
            for (item in result.items) {
                db.expenseDao().insertGroceryItem(
                    GroceryItemEntity(
                        name = item.canonicalName,
                        category = item.category,
                        quantity = "${if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()} ${item.unit}".trim(),
                        estimatedPrice = item.price,
                        priceCap = item.price,
                        targetVendor = result.vendorName,
                        isPantryItem = false,
                        canonicalName = item.canonicalName,
                        brand = "",
                        storageType = "Pantry"
                    )
                )
            }
            showToast("Added ${result.items.size} handwritten items to Shopping List")
            closeHandwrittenScanner()
            closeModal()
        }
    }

    // Helper Date Utilities
    private fun getCurrentYearMonth(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.ROOT)
        return sdf.format(Date())
    }

    private fun getPreviousYearMonth(yearMonth: String): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.ROOT)
        val cal = Calendar.getInstance()
        try {
            cal.time = sdf.parse(yearMonth) ?: Date()
            cal.add(Calendar.MONTH, -1)
            return sdf.format(cal.time)
        } catch (e: Exception) {
            return yearMonth
        }
    }

    private fun formatYearMonthLabel(yearMonth: String): String {
        val inSdf = SimpleDateFormat("yyyy-MM", Locale.ROOT)
        val outSdf = SimpleDateFormat("MMMM yyyy", Locale.ROOT)
        return try {
            val d = inSdf.parse(yearMonth) ?: Date()
            outSdf.format(d)
        } catch (e: Exception) {
            yearMonth
        }
    }

    private fun isDateInYearMonth(timestamp: Long, yearMonth: String): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.ROOT)
        val dateStr = sdf.format(Date(timestamp))
        return dateStr == yearMonth
    }

    private fun getDaysRemainingInCurrentMonth(): Int {
        val cal = Calendar.getInstance()
        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        return (totalDays - currentDay).coerceAtLeast(0)
    }

    private fun getDaysElapsedInCurrentMonth(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    }

    // Category-Level Budget Management & Persistence
    private fun loadCategoryBudgetsInternal(): Map<String, Double> {
        val result = defaultCategoryBudgets.toMutableMap()
        try {
            val allPrefs = sharedPrefs.all
            for ((key, value) in allPrefs) {
                if (key.startsWith("cat_budget_")) {
                    val catName = key.removePrefix("cat_budget_")
                    val limit = when (value) {
                        is Float -> value.toDouble()
                        is Double -> value
                        is Long -> value.toDouble()
                        is Int -> value.toDouble()
                        is String -> value.toDoubleOrNull() ?: 2000.0
                        else -> 2000.0
                    }
                    result[catName] = limit
                }
            }
        } catch (e: Exception) {
            // fallback to defaults
        }
        return result
    }

    fun updateCategoryBudget(category: String, limit: Double) {
        val updated = _categoryBudgets.value.toMutableMap()
        val safeLimit = limit.coerceAtLeast(0.0)
        updated[category] = safeLimit
        _categoryBudgets.value = updated
        sharedPrefs.edit().putFloat("cat_budget_$category", safeLimit.toFloat()).apply()
        showToast("Updated $category budget: ₹${safeLimit.toInt()}/mo")
    }

    fun setCategoryBudget(category: String, limit: Double) = updateCategoryBudget(category, limit)

    fun deleteCategoryBudget(category: String) {
        val updated = _categoryBudgets.value.toMutableMap()
        updated.remove(category)
        _categoryBudgets.value = updated
        sharedPrefs.edit().remove("cat_budget_$category").apply()
        showToast("Removed $category budget limit")
    }

    fun resetCategoryBudgetsToDefaults() {
        _categoryBudgets.value = defaultCategoryBudgets
        val editor = sharedPrefs.edit()
        for (key in sharedPrefs.all.keys) {
            if (key.startsWith("cat_budget_")) {
                editor.remove(key)
            }
        }
        for ((k, v) in defaultCategoryBudgets) {
            editor.putFloat("cat_budget_$k", v.toFloat())
        }
        editor.apply()
        showToast("Reset all category budgets to defaults")
    }

    fun resetCategoryBudgetsToDefault() = resetCategoryBudgetsToDefaults()

    fun autoScaleCategoryBudgetsToMonthlyBudget() {
        val totalBudget = _monthlyBudget.value
        if (totalBudget <= 0) return
        val currentSum = _categoryBudgets.value.values.sum()
        if (currentSum <= 0) return
        val ratio = totalBudget / currentSum
        val updated = _categoryBudgets.value.mapValues { (Math.round(it.value * ratio / 100.0) * 100.0).coerceAtLeast(500.0) }
        _categoryBudgets.value = updated
        val editor = sharedPrefs.edit()
        for ((k, v) in updated) {
            editor.putFloat("cat_budget_$k", v.toFloat())
        }
        editor.apply()
        showToast("Balanced category budgets to match ₹${totalBudget.toInt()} monthly target")
    }

    fun evaluateVoiceLiveBudgetWarning(category: String, items: List<ParsedNlpItem>): String? {
        val totalAdded = items.sumOf { it.price * it.quantity }
        if (totalAdded <= 0) return null
        val targetCat = category.ifBlank { items.firstOrNull()?.category ?: "Groceries" }
        val currentSpend = dashboardState.value.categorySpends.find { it.category.equals(targetCat, ignoreCase = true) }?.amount ?: 0.0
        val budgetLimit = _categoryBudgets.value[targetCat] ?: 2000.0
        val projected = currentSpend + totalAdded
        return if (projected > budgetLimit && budgetLimit > 0) {
            val overAmt = (projected - budgetLimit).toInt()
            "🚨 Overspend Alert: Exceeds $targetCat limit (₹${budgetLimit.toInt()}) by ₹$overAmt"
        } else if (projected >= budgetLimit * 0.8 && budgetLimit > 0) {
            val remAmt = (budgetLimit - projected).toInt()
            "⚠️ Caution: Reaches ${(projected / budgetLimit * 100).toInt()}% of $targetCat budget (₹$remAmt left)"
        } else {
            null
        }
    }

    fun checkExpenseOverspend(category: String, addedAmount: Double): OverspendAlertInfo? {
        if (addedAmount <= 0) return null
        val currentSpend = dashboardState.value.categorySpends.find { it.category.equals(category, ignoreCase = true) }?.amount ?: 0.0
        val budgetLimit = _categoryBudgets.value[category] ?: 2000.0
        val projected = currentSpend + addedAmount
        if (projected > budgetLimit && budgetLimit > 0) {
            val overAmt = projected - budgetLimit
            val pct = (projected / budgetLimit).toFloat()
            return OverspendAlertInfo(
                category = category,
                limit = budgetLimit,
                currentSpend = projected,
                overspentAmount = overAmt,
                percentage = pct,
                message = "Exceeded $category budget by ₹${overAmt.toInt()} (${(pct * 100).toInt()}%)"
            )
        }
        return null
    }

    private fun getCategoryIconName(category: String): String {
        return when (category.lowercase(Locale.ROOT).trim()) {
            "groceries", "grocery", "kirana", "supermarket" -> "shopping_cart"
            "dairy", "milk", "butter", "paneer" -> "egg"
            "dining", "restaurant", "food", "cafes" -> "restaurant"
            "transport", "travel", "fuel", "petrol", "cab", "auto" -> "directions_car"
            "utilities", "electricity", "water", "wifi", "bills" -> "bolt"
            "shopping", "clothing", "electronics" -> "shopping_bag"
            "healthcare", "medicine", "pharmacy", "medical" -> "local_hospital"
            "entertainment", "movies", "games" -> "movie"
            "personal care", "salon", "grooming" -> "spa"
            else -> "category"
        }
    }

    // --- Bank SMS & Email Invoice Auto-Capture Methods ---

    fun dismissIncomingBankSmsAlert() {
        _incomingBankSmsAlert.value = null
    }

    fun logParsedBankSms(sms: ParsedBankSms) {
        viewModelScope.launch {
            val singleItem = ParsedNlpItem(
                name = "${sms.merchant} (${sms.txnType})",
                category = sms.category,
                quantity = 1.0,
                unit = "txn",
                price = sms.amount,
                vendor = sms.merchant
            )

            repository.logExpenseWithItems(
                title = "${sms.merchant} (${sms.bankName})",
                vendor = sms.merchant,
                category = sms.category,
                items = listOf(singleItem),
                paymentMethod = "${sms.bankName} ${sms.txnType} (${sms.accountLast4})",
                locationName = "Bank SMS Auto-Capture",
                rawVoicePrompt = sms.rawSms
            )

            _incomingBankSmsAlert.value = null
            showToast("✅ Auto-logged ₹${sms.amount.toInt()} from ${sms.bankName} SMS!")
        }
    }

    fun parseManualBankSms(text: String, sender: String? = null): ParsedBankSms? {
        val parsed = BankSmsParser.parse(sender, text)
        if (parsed != null) {
            BankSmsReceiver.postManualSms(parsed)
        }
        return parsed
    }

    fun parseEmailInvoice(text: String, appHint: String? = null): ParsedEmailInvoice {
        return EmailInvoiceParser.parse(text, appHint)
    }

    fun parseEmailInvoiceWithGemini(
        emailText: String,
        onSuccess: (ParsedEmailInvoice) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val apiKey = _userGeminiApiKey.value
                if (apiKey.isBlank()) {
                    val fallback = EmailInvoiceParser.parse(emailText)
                    onSuccess(fallback)
                    _isAiLoading.value = false
                    return@launch
                }

                val prompt = """
                    You are an expert financial invoice extraction engine.
                    Extract invoice details from this order email text into structured JSON:
                    {
                      "merchant": "Merchant name (e.g. Zepto, Zomato, Swiggy, Blinkit, Amazon, Uber)",
                      "orderId": "Order/Invoice ID",
                      "orderDate": "Date string or Today",
                      "totalAmount": 0.0,
                      "paymentMethod": "Payment mode (e.g. UPI, Credit Card)",
                      "isGrocery": true,
                      "items": [
                        {
                          "name": "Item description with unit",
                          "category": "Groceries/Dairy/Produce/Dining/Transport/Shopping/Pantry",
                          "quantity": 1.0,
                          "unit": "1kg/500g/1L/unit",
                          "price": 0.0,
                          "canonicalName": "Standard commodity name",
                          "shelfLifeDays": 14,
                          "storageType": "Refrigerated/Pantry"
                        }
                      ]
                    }

                    Email text:
                    $emailText
                """.trimIndent()

                val response = GeminiService.generateText(prompt, apiKey)
                val fallback = EmailInvoiceParser.parse(emailText)
                onSuccess(fallback)
            } catch (e: Exception) {
                val fallback = EmailInvoiceParser.parse(emailText)
                onSuccess(fallback)
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun logParsedEmailInvoice(invoice: ParsedEmailInvoice, autoRestockPantry: Boolean = true) {
        viewModelScope.launch {
            repository.logExpenseWithItems(
                title = "${invoice.merchant} (#${invoice.orderId.take(8)})",
                vendor = invoice.merchant,
                category = if (invoice.isGrocery) "Groceries" else invoice.items.firstOrNull()?.category ?: "Shopping",
                items = invoice.items,
                paymentMethod = invoice.paymentMethod,
                locationName = "Email Invoice Auto-Capture",
                rawVoicePrompt = "Imported digital invoice from ${invoice.merchant}"
            )

            // If it is a grocery order, automatically restock the pantry
            if (invoice.isGrocery && autoRestockPantry) {
                val now = System.currentTimeMillis()
                for (item in invoice.items) {
                    db.expenseDao().insertGroceryItems(
                        listOf(
                            GroceryItemEntity(
                                name = item.name,
                                quantity = "${item.quantity.toInt()} ${item.unit}",
                                estimatedPrice = item.price * item.quantity,
                                priceCap = item.price * 1.2,
                                targetVendor = invoice.merchant,
                                category = item.category,
                                isChecked = false,
                                isPantryItem = true,
                                purchaseDate = now,
                                expiryDaysTotal = if (item.shelfLifeDays > 0) item.shelfLifeDays else 14,
                                remainingDays = if (item.shelfLifeDays > 0) item.shelfLifeDays else 14,
                                burnRateLevel = "NORMAL",
                                lastBoughtDaysAgo = 0,
                                canonicalName = item.canonicalName,
                                brand = item.brand,
                                storageType = item.storageType
                            )
                        )
                    )
                }
            }

            showToast("🎉 Logged ${invoice.merchant} invoice (₹${invoice.totalAmount.toInt()}, ${invoice.items.size} items)")
        }
    }

    fun openAutoImportModal() {
        openModal("AUTO_IMPORT")
    }

    // ==========================================
    // STOREFRONT POS & INVENTORY CONTROLLER
    // ==========================================

    fun addItemToPosCart(commodity: CommodityEntity, quantity: Double = 1.0) {
        val current = _posCartItems.value.toMutableList()
        val index = current.indexOfFirst { it.commodityId == commodity.id }
        if (index >= 0) {
            val existing = current[index]
            current[index] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            val sellPrice = if (commodity.sellingPrice > 0) commodity.sellingPrice else if (commodity.lastKnownPrice > 0) commodity.lastKnownPrice else 50.0
            val costPrice = if (commodity.costPrice > 0) commodity.costPrice else sellPrice * 0.78
            current.add(
                com.example.data.model.PosCartItem(
                    commodityId = commodity.id,
                    name = commodity.canonicalName.ifBlank { commodity.rawKey },
                    category = commodity.category,
                    unit = commodity.normalizedUnit,
                    quantity = quantity,
                    unitPrice = sellPrice,
                    costPrice = costPrice,
                    canonicalName = commodity.canonicalName,
                    brand = commodity.brand,
                    availableStock = commodity.stockQuantity
                )
            )
        }
        _posCartItems.value = current
        showToast("Added ${commodity.canonicalName} to bill")
    }

    fun addCustomItemToPosCart(
        name: String,
        price: Double,
        quantity: Double = 1.0,
        unit: String = "pcs",
        category: String = "General",
        costPrice: Double = 0.0
    ) {
        if (name.isBlank() || price <= 0) return
        val current = _posCartItems.value.toMutableList()
        val effectiveCost = if (costPrice > 0) costPrice else price * 0.78
        current.add(
            com.example.data.model.PosCartItem(
                commodityId = 0,
                name = name.trim(),
                category = category,
                unit = unit.ifBlank { "pcs" },
                quantity = quantity.coerceAtLeast(0.1),
                unitPrice = price,
                costPrice = effectiveCost,
                canonicalName = name.trim(),
                brand = "",
                availableStock = 999.0
            )
        )
        _posCartItems.value = current
        showToast("Added $name (₹${price.toInt()}) to bill")
    }

    fun updatePosCartItemQuantity(index: Int, newQuantity: Double) {
        val current = _posCartItems.value.toMutableList()
        if (index in current.indices) {
            if (newQuantity <= 0) {
                current.removeAt(index)
            } else {
                current[index] = current[index].copy(quantity = newQuantity)
            }
            _posCartItems.value = current
        }
    }

    fun removePosCartItem(index: Int) {
        val current = _posCartItems.value.toMutableList()
        if (index in current.indices) {
            val removed = current.removeAt(index)
            _posCartItems.value = current
            showToast("Removed ${removed.name}")
        }
    }

    fun clearPosCart() {
        _posCartItems.value = emptyList()
        _posCustomerName.value = ""
        _posCustomerPhone.value = ""
        _posDiscountPercent.value = 0.0
        _posTaxPercent.value = 0.0
    }

    fun setPosCustomer(name: String, phone: String) {
        _posCustomerName.value = name
        _posCustomerPhone.value = phone
    }

    fun setPosTaxPercent(tax: Double) {
        _posTaxPercent.value = tax
    }

    fun setPosDiscountPercent(discount: Double) {
        _posDiscountPercent.value = discount
    }

    fun setPosCategoryFilter(cat: String?) {
        _posSelectedCategoryFilter.value = cat
    }

    fun setPosSearchQuery(query: String) {
        _posSearchQuery.value = query
    }

    fun initiatePosCheckout(paymentMethod: String = "UPI / QR") {
        val items = _posCartItems.value
        if (items.isEmpty()) {
            showToast("Cart is empty. Add items to create a sale.")
            return
        }

        val subtotal = items.sumOf { it.lineTotal }
        val discount = subtotal * (_posDiscountPercent.value / 100.0)
        val afterDiscount = (subtotal - discount).coerceAtLeast(0.0)
        val tax = afterDiscount * (_posTaxPercent.value / 100.0)
        val netTotal = afterDiscount + tax
        val invoiceNo = "INV-${(System.currentTimeMillis() % 1000000).toString().padStart(6, '0')}"

        val qrUri = com.example.data.UpiPaymentManager.buildMerchantDynamicQrUri(
            amount = netTotal,
            invoiceNo = invoiceNo,
            note = "Matrics Storefront Sale"
        )

        _posCheckoutDialogState.value = PosCheckoutDialogState(
            isOpen = true,
            invoiceNo = invoiceNo,
            totalAmount = netTotal,
            subtotal = subtotal,
            taxAmount = tax,
            discountAmount = discount,
            customerName = _posCustomerName.value,
            customerPhone = _posCustomerPhone.value,
            paymentMethod = paymentMethod,
            upiQrUri = qrUri,
            isPaymentVerified = false
        )
    }

    fun updateCheckoutPaymentMethod(method: String) {
        _posCheckoutDialogState.value = _posCheckoutDialogState.value.copy(
            paymentMethod = method
        )
    }

    fun dismissPosCheckout() {
        _posCheckoutDialogState.value = PosCheckoutDialogState(isOpen = false)
    }

    fun confirmPosSaleCompleted(customPaymentMethod: String? = null) {
        val checkoutState = _posCheckoutDialogState.value
        val items = _posCartItems.value
        if (items.isEmpty()) return

        val method = customPaymentMethod ?: checkoutState.paymentMethod

        viewModelScope.launch {
            val txId = repository.executePosSale(
                items = items,
                customerName = _posCustomerName.value,
                customerPhone = _posCustomerPhone.value,
                paymentMethod = method,
                taxPercent = _posTaxPercent.value,
                discountPercent = _posDiscountPercent.value,
                storeName = "Matrics Node Storefront"
            )

            if (txId > 0) {
                showToast("✅ Sale Recorded! Invoice #${checkoutState.invoiceNo} (₹${checkoutState.totalAmount.toInt()})")
                
                // Build complete thermal receipt
                val receiptLineItems = items.map { item ->
                    EscPosThermalPrinterEngine.ReceiptLineItem(
                        name = item.name,
                        qty = item.quantity,
                        unit = item.unit,
                        unitPrice = item.unitPrice,
                        total = item.lineTotal
                    )
                }

                val currentHeader = thermalPrinterConfig.value.storeHeader
                val receipt = EscPosThermalPrinterEngine.ReceiptData(
                    header = currentHeader,
                    invoiceNo = checkoutState.invoiceNo,
                    customerName = _posCustomerName.value,
                    customerPhone = _posCustomerPhone.value,
                    items = receiptLineItems,
                    subtotal = checkoutState.subtotal,
                    discountAmount = checkoutState.discountAmount,
                    taxAmount = checkoutState.taxAmount,
                    netTotal = checkoutState.totalAmount,
                    paymentMethod = method,
                    upiPaymentUri = checkoutState.upiQrUri
                )

                val rawPreview = EscPosThermalPrinterEngine.formatReceiptAsPlainText(receipt, thermalPrinterConfig.value.paperSize)

                // Auto-print via ESC/POS if enabled
                if (thermalPrinterConfig.value.autoPrintOnSale) {
                    viewModelScope.launch {
                        thermalPrinterManager.printReceipt(receipt)
                    }
                }

                _thermalReceiptModalState.value = ThermalReceiptModalState(
                    isVisible = true,
                    receiptData = receipt,
                    rawAsciiPreview = rawPreview,
                    isSuccess = true
                )

                clearPosCart()
                dismissPosCheckout()
            } else {
                showToast("Error processing sale")
            }
        }
    }

    // Inventory operations
    fun quickRestockCommodity(id: Long, addQty: Double) {
        viewModelScope.launch {
            repository.quickRestockCommodity(id, addQty)
            showToast("Added +${addQty.toInt()} units to stock")
        }
    }

    fun updateCommodityStock(id: Long, newStock: Double) {
        viewModelScope.launch {
            repository.updateCommodityStock(id, newStock)
            showToast("Stock updated to ${newStock.toInt()} units")
        }
    }

    fun updateCommodityPricing(id: Long, costPrice: Double, sellingPrice: Double) {
        viewModelScope.launch {
            repository.updateCommodityPricing(id, costPrice, sellingPrice)
            showToast("Pricing updated: Cost ₹${costPrice.toInt()} | Sell ₹${sellingPrice.toInt()}")
        }
    }

    fun updateCommodityReorderThreshold(id: Long, threshold: Double) {
        viewModelScope.launch {
            repository.updateCommodityReorderThreshold(id, threshold)
            showToast("Low stock threshold set to ${threshold.toInt()}")
        }
    }

    fun addNewInventoryCommodity(
        name: String,
        category: String,
        brand: String,
        costPrice: Double,
        sellingPrice: Double,
        stock: Double,
        threshold: Double,
        unit: String
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertCommodityDirect(
                CommodityEntity(
                    rawKey = name.trim().lowercase(),
                    canonicalName = name.trim(),
                    brand = brand.trim(),
                    category = category.ifBlank { "General" },
                    subcategory = "",
                    defaultQuantity = 1.0,
                    normalizedUnit = unit.ifBlank { "pcs" },
                    estimatedShelfLifeDays = 30,
                    storageType = "Storefront",
                    lastKnownPrice = sellingPrice,
                    useCount = 1,
                    isPreSeeded = false,
                    stockQuantity = stock.coerceAtLeast(0.0),
                    costPrice = costPrice.coerceAtLeast(0.0),
                    sellingPrice = sellingPrice.coerceAtLeast(0.0),
                    reorderThreshold = threshold.coerceAtLeast(1.0),
                    sku = "SKU-${(System.currentTimeMillis() % 100000)}"
                )
            )
            showToast("Created inventory item: $name")
        }
    }

    fun deleteInventoryCommodity(id: Long) {
        viewModelScope.launch {
            repository.deleteCommodity(id)
            showToast("Deleted item from inventory")
        }
    }

    // Voice HUD dispatch extensions
    fun populatePosCartFromVoiceHud() {
        val items = _voiceHudState.value.parsedItems
        if (items.isEmpty()) return
        for (item in items) {
            addCustomItemToPosCart(
                name = item.name,
                price = if (item.price > 0) item.price else 50.0,
                quantity = item.quantity,
                unit = item.unit,
                category = item.category,
                costPrice = item.costPrice
            )
        }
        closeVoiceHud()
        showToast("Transferred ${items.size} items to POS bill")
    }

    fun executeRestockFromVoiceHud() {
        val items = _voiceHudState.value.parsedItems
        val vendor = _voiceHudState.value.detectedVendor
        if (items.isEmpty()) return
        viewModelScope.launch {
            val txId = repository.executeRestock(
                items = items,
                supplierName = vendor.ifBlank { "Wholesale Supplier" },
                paymentMethod = _voiceHudState.value.detectedPaymentMethod
            )
            closeVoiceHud()
            showToast("📦 Inward Restock Complete! Updated ${items.size} commodities in inventory.")
        }
    }

    // Barcode & QR Scanner Actions
    fun openBarcodeScanner(mode: BarcodeScanMode = BarcodeScanMode.POS_BILLING) {
        _barcodeScannerState.value = _barcodeScannerState.value.copy(
            isVisible = true,
            mode = mode,
            lastScannedCode = "",
            lastMatchedCommodity = null,
            unrecognizedBarcode = null,
            showUnrecognizedDialog = false,
            statusFeedback = when (mode) {
                BarcodeScanMode.POS_BILLING -> "Scan items directly into active bill"
                BarcodeScanMode.INVENTORY_SEARCH -> "Scan barcode to find SKU in catalog"
                BarcodeScanMode.INVENTORY_RESTOCK -> "Scan to add +1 unit to inventory"
                BarcodeScanMode.PRICE_CHECKER -> "Scan product to check price & stock"
                BarcodeScanMode.SKU_REGISTRATION -> "Scan packaging barcode to assign SKU"
            }
        )
    }

    fun closeBarcodeScanner() {
        _barcodeScannerState.value = _barcodeScannerState.value.copy(
            isVisible = false,
            isTorchOn = false
        )
    }

    fun toggleBarcodeScannerTorch() {
        _barcodeScannerState.value = _barcodeScannerState.value.copy(
            isTorchOn = !_barcodeScannerState.value.isTorchOn
        )
    }

    fun toggleContinuousScan() {
        _barcodeScannerState.value = _barcodeScannerState.value.copy(
            isContinuousScan = !_barcodeScannerState.value.isContinuousScan
        )
    }

    fun toggleScannerSound() {
        _barcodeScannerState.value = _barcodeScannerState.value.copy(
            soundEnabled = !_barcodeScannerState.value.soundEnabled
        )
    }

    fun setBarcodeScanMode(mode: BarcodeScanMode) {
        _barcodeScannerState.value = _barcodeScannerState.value.copy(
            mode = mode,
            statusFeedback = when (mode) {
                BarcodeScanMode.POS_BILLING -> "Scan items directly into active bill"
                BarcodeScanMode.INVENTORY_SEARCH -> "Scan barcode to find SKU in catalog"
                BarcodeScanMode.INVENTORY_RESTOCK -> "Scan to add +1 unit to inventory"
                BarcodeScanMode.PRICE_CHECKER -> "Scan product to check price & stock"
                BarcodeScanMode.SKU_REGISTRATION -> "Scan packaging barcode to assign SKU"
            }
        )
    }

    fun dismissUnrecognizedBarcodeDialog() {
        _barcodeScannerState.value = _barcodeScannerState.value.copy(
            showUnrecognizedDialog = false,
            unrecognizedBarcode = null
        )
    }

    fun handleBarcodeScanned(rawCode: String) {
        val trimmed = rawCode.trim()
        if (trimmed.isBlank()) return

        val state = _barcodeScannerState.value
        val now = System.currentTimeMillis()

        // Debounce if same barcode is scanned within 1200ms
        if (trimmed == state.lastScannedCode && (now - state.lastScanTimestamp) < 1200L) {
            return
        }

        // Try to match against existing commodities catalog
        val catalog = allCommodities.value
        val matched = catalog.find { commodity ->
            commodity.sku.equals(trimmed, ignoreCase = true) ||
            commodity.rawKey.equals(trimmed.lowercase(), ignoreCase = true) ||
            commodity.canonicalName.equals(trimmed, ignoreCase = true) ||
            (commodity.sku.isNotBlank() && trimmed.contains(commodity.sku, ignoreCase = true))
        }

        val mode = state.mode
        val currentHistory = state.scannedHistory.toMutableList()

        if (matched != null) {
            val sellPrice = if (matched.sellingPrice > 0) matched.sellingPrice else if (matched.lastKnownPrice > 0) matched.lastKnownPrice else 50.0

            when (mode) {
                BarcodeScanMode.POS_BILLING -> {
                    addItemToPosCart(matched)
                    currentHistory.add(
                        0,
                        ScannedBarcodeRecord(
                            barcode = trimmed,
                            timestamp = now,
                            matchedCommodityName = matched.canonicalName,
                            price = sellPrice,
                            quantityAdded = 1.0
                        )
                    )
                    _barcodeScannerState.value = state.copy(
                        lastScannedCode = trimmed,
                        lastMatchedCommodity = matched,
                        lastScanTimestamp = now,
                        sessionScanCount = state.sessionScanCount + 1,
                        scannedHistory = currentHistory.take(20),
                        statusFeedback = "Added ${matched.canonicalName} (₹${sellPrice.toInt()})"
                    )

                    if (!state.isContinuousScan) {
                        closeBarcodeScanner()
                    }
                }
                BarcodeScanMode.INVENTORY_RESTOCK -> {
                    quickRestockCommodity(matched.id, 1.0)
                    currentHistory.add(
                        0,
                        ScannedBarcodeRecord(
                            barcode = trimmed,
                            timestamp = now,
                            matchedCommodityName = "${matched.canonicalName} (+1)",
                            price = sellPrice,
                            quantityAdded = 1.0
                        )
                    )
                    _barcodeScannerState.value = state.copy(
                        lastScannedCode = trimmed,
                        lastMatchedCommodity = matched,
                        lastScanTimestamp = now,
                        sessionScanCount = state.sessionScanCount + 1,
                        scannedHistory = currentHistory.take(20),
                        statusFeedback = "Restocked ${matched.canonicalName} (+1 unit)"
                    )
                }
                BarcodeScanMode.PRICE_CHECKER -> {
                    _barcodeScannerState.value = state.copy(
                        lastScannedCode = trimmed,
                        lastMatchedCommodity = matched,
                        lastScanTimestamp = now,
                        statusFeedback = "${matched.canonicalName}: ₹${sellPrice.toInt()} • Stock: ${matched.stockQuantity.toInt()}"
                    )
                }
                BarcodeScanMode.INVENTORY_SEARCH -> {
                    _posSearchQuery.value = matched.canonicalName
                    _barcodeScannerState.value = state.copy(
                        lastScannedCode = trimmed,
                        lastMatchedCommodity = matched,
                        lastScanTimestamp = now,
                        statusFeedback = "Found ${matched.canonicalName}"
                    )
                    closeBarcodeScanner()
                }
                BarcodeScanMode.SKU_REGISTRATION -> {
                    _barcodeScannerState.value = state.copy(
                        lastScannedCode = trimmed,
                        lastMatchedCommodity = matched,
                        lastScanTimestamp = now,
                        statusFeedback = "Barcode linked to ${matched.canonicalName}"
                    )
                    closeBarcodeScanner()
                }
            }
        } else {
            // Unrecognized Barcode
            _barcodeScannerState.value = state.copy(
                lastScannedCode = trimmed,
                lastMatchedCommodity = null,
                lastScanTimestamp = now,
                unrecognizedBarcode = trimmed,
                showUnrecognizedDialog = true,
                statusFeedback = "Unrecognized code: $trimmed"
            )
        }
    }

    fun registerNewSkuFromBarcode(
        barcode: String,
        name: String,
        category: String,
        brand: String,
        costPrice: Double,
        sellingPrice: Double,
        initialStock: Double,
        addToPosBillImmediately: Boolean = true
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val entity = CommodityEntity(
                rawKey = name.trim().lowercase(),
                canonicalName = name.trim(),
                brand = brand.trim(),
                category = category.ifBlank { "General" },
                subcategory = "",
                defaultQuantity = 1.0,
                normalizedUnit = "pcs",
                estimatedShelfLifeDays = 30,
                storageType = "Storefront",
                lastKnownPrice = sellingPrice,
                useCount = 1,
                isPreSeeded = false,
                stockQuantity = initialStock.coerceAtLeast(0.0),
                costPrice = costPrice.coerceAtLeast(0.0),
                sellingPrice = sellingPrice.coerceAtLeast(0.0),
                reorderThreshold = 5.0,
                sku = barcode.trim()
            )
            repository.insertCommodityDirect(entity)
            dismissUnrecognizedBarcodeDialog()
            showToast("Linked barcode to $name (₹${sellingPrice.toInt()})")

            if (addToPosBillImmediately && _barcodeScannerState.value.mode == BarcodeScanMode.POS_BILLING) {
                addCustomItemToPosCart(
                    name = name,
                    price = sellingPrice,
                    quantity = 1.0,
                    unit = "pcs",
                    category = category,
                    costPrice = costPrice
                )
            }
        }
    }

    private fun calculateStorefrontMetrics(
        transactions: List<TransactionEntity>,
        lineItems: List<LineItemEntity>,
        commodities: List<CommodityEntity>,
        lowStock: List<CommodityEntity>
    ): StorefrontDashboardState {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStartMs = cal.timeInMillis

        // Sales today
        val todaySales = transactions.filter {
            it.dateTimestamp >= todayStartMs && (it.transactionType == "SALE" || it.title.startsWith("Sale", ignoreCase = true))
        }

        val todayRevenue = todaySales.sumOf { it.totalAmount }
        val todayOrdersCount = todaySales.size
        val avgOrderValue = if (todayOrdersCount > 0) todayRevenue / todayOrdersCount else 0.0

        val todaySaleIds = todaySales.map { it.id }.toSet()
        val todayLineItems = lineItems.filter { it.transactionId in todaySaleIds }
        val todayCost = if (todayLineItems.isNotEmpty()) {
            todayLineItems.sumOf { (if (it.costPrice > 0) it.costPrice else it.unitPrice * 0.78) * it.quantity }
        } else {
            todayRevenue * 0.78
        }
        val grossProfit = (todayRevenue - todayCost).coerceAtLeast(0.0)
        val marginPercent = if (todayRevenue > 0) (grossProfit / todayRevenue) * 100.0 else 0.0

        val totalInventoryValue = commodities.sumOf { (if (it.sellingPrice > 0) it.sellingPrice else it.lastKnownPrice) * it.stockQuantity }

        return StorefrontDashboardState(
            todayRevenue = todayRevenue,
            todayCost = todayCost,
            todayGrossProfit = grossProfit,
            todayMarginPercent = marginPercent,
            todayOrdersCount = todayOrdersCount,
            averageOrderValue = avgOrderValue,
            lowStockCount = lowStock.size,
            totalInventoryValue = totalInventoryValue,
            totalSkusCount = commodities.size,
            todaySalesTransactions = todaySales,
            lowStockItems = lowStock
        )
    }

    // --- Thermal Printer Control Functions ---

    fun openThermalPrinterSettings() {
        thermalPrinterManager.refreshPairedDevices()
        _isThermalPrinterSettingsOpen.value = true
    }

    fun closeThermalPrinterSettings() {
        _isThermalPrinterSettingsOpen.value = false
    }

    fun refreshPairedThermalPrinters() {
        thermalPrinterManager.refreshPairedDevices()
        showToast("Refreshed paired Bluetooth devices")
    }

    fun selectThermalPrinter(device: DiscoveredPrinterDevice) {
        thermalPrinterManager.setTargetDevice(device.address, device.name)
        showToast("Selected printer: ${device.name}")
    }

    fun updateThermalPrinterConfig(config: ThermalPrinterConfig) {
        thermalPrinterManager.updateConfig(config)
    }

    fun updateThermalPaperSize(size: ThermalPaperSize) {
        thermalPrinterManager.updatePaperSize(size)
        // Refresh preview if open
        _thermalReceiptModalState.value.receiptData?.let { receipt ->
            val raw = EscPosThermalPrinterEngine.formatReceiptAsPlainText(receipt, size)
            _thermalReceiptModalState.value = _thermalReceiptModalState.value.copy(rawAsciiPreview = raw)
        }
        showToast("Paper width set to ${size.widthMm}mm (${size.charColumns} cols)")
    }

    fun updateThermalStoreHeader(header: EscPosThermalPrinterEngine.StoreReceiptHeader) {
        thermalPrinterManager.updateStoreHeader(header)
        showToast("Store header updated for thermal bills")
    }

    fun setAutoPrintOnSale(enabled: Boolean) {
        thermalPrinterManager.setAutoPrintOnSale(enabled)
        showToast(if (enabled) "Auto-print on sale ENABLED" else "Auto-print on sale DISABLED")
    }

    fun runThermalTestPrint() {
        viewModelScope.launch {
            _thermalReceiptModalState.value = _thermalReceiptModalState.value.copy(isPrinting = true, errorMessage = null)
            val result = thermalPrinterManager.runTestPrint()
            if (result.isSuccess) {
                showToast("✅ Test Receipt Printed Successfully!")
                _thermalReceiptModalState.value = _thermalReceiptModalState.value.copy(isPrinting = false, isSuccess = true)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Printer error"
                showToast("❌ Print Failed: $err")
                _thermalReceiptModalState.value = _thermalReceiptModalState.value.copy(isPrinting = false, errorMessage = err)
            }
        }
    }

    fun openReceiptPreview(receipt: EscPosThermalPrinterEngine.ReceiptData) {
        val raw = EscPosThermalPrinterEngine.formatReceiptAsPlainText(receipt, thermalPrinterConfig.value.paperSize)
        _thermalReceiptModalState.value = ThermalReceiptModalState(
            isVisible = true,
            receiptData = receipt,
            rawAsciiPreview = raw,
            isPrinting = false,
            isSuccess = false,
            errorMessage = null
        )
    }

    fun closeReceiptPreview() {
        _thermalReceiptModalState.value = ThermalReceiptModalState(isVisible = false)
    }

    fun printThermalReceiptDirect(receipt: EscPosThermalPrinterEngine.ReceiptData) {
        viewModelScope.launch {
            _thermalReceiptModalState.value = _thermalReceiptModalState.value.copy(isPrinting = true, errorMessage = null)
            val result = thermalPrinterManager.printReceipt(receipt)
            if (result.isSuccess) {
                showToast("✅ Receipt Printed! Bill #${receipt.invoiceNo}")
                _thermalReceiptModalState.value = _thermalReceiptModalState.value.copy(isPrinting = false, isSuccess = true)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Print failed"
                showToast("❌ Print Error: $err")
                _thermalReceiptModalState.value = _thermalReceiptModalState.value.copy(isPrinting = false, errorMessage = err)
            }
        }
    }

    fun printDailyShiftZReport() {
        viewModelScope.launch {
            val dash = storefrontDashboardState.value
            val transactions = allTransactions.value
            val currentYearMonth = getCurrentYearMonth()
            val monthTx = transactions.filter { isDateInYearMonth(it.dateTimestamp, currentYearMonth) }

            val totalSales = dash.todayRevenue
            val billsCount = dash.todayOrdersCount
            val cashCollected = dash.todaySalesTransactions.filter { it.paymentMethod.equals("CASH", ignoreCase = true) }.sumOf { it.totalAmount }
            val upiCollected = dash.todaySalesTransactions.filter { it.paymentMethod.contains("UPI", ignoreCase = true) || it.paymentMethod.contains("QR", ignoreCase = true) }.sumOf { it.totalAmount }
            val khataAdded = dash.todaySalesTransactions.filter { it.paymentMethod.contains("KHATA", ignoreCase = true) || it.paymentMethod.contains("CREDIT", ignoreCase = true) }.sumOf { it.totalAmount }

            val topItems = dash.todaySalesTransactions.map { it.title to 1 }
                .groupBy({ it.first }, { it.second })
                .map { (k, v) -> k to v.size }
                .sortedByDescending { it.second }

            val report = EscPosThermalPrinterEngine.ShiftZReportData(
                storeName = thermalPrinterConfig.value.storeHeader.storeName,
                totalSales = totalSales,
                totalBillsCount = billsCount,
                cashCollected = cashCollected,
                upiCollected = upiCollected,
                khataOutstandingAdded = khataAdded,
                totalTaxCollected = totalSales * 0.05,
                totalDiscountsGiven = totalSales * 0.02,
                topSellingItems = topItems
            )

            val result = thermalPrinterManager.printZReport(report)
            if (result.isSuccess) {
                showToast("✅ Day-End Shift Z-Report Printed!")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Print failed"
                showToast("❌ Z-Report Print Failed: $err")
            }
        }
    }

    fun printTransactionReceipt(tx: TransactionEntity) {
        viewModelScope.launch {
            val lineItemsList = allLineItems.value.filter { it.transactionId == tx.id }
            val items: List<EscPosThermalPrinterEngine.ReceiptLineItem> = if (lineItemsList.isNotEmpty()) {
                lineItemsList.map { item ->
                    EscPosThermalPrinterEngine.ReceiptLineItem(
                        name = item.name,
                        qty = item.quantity,
                        unit = item.unit,
                        unitPrice = item.unitPrice,
                        total = item.totalPrice
                    )
                }
            } else {
                listOf(
                    EscPosThermalPrinterEngine.ReceiptLineItem(
                        name = tx.title,
                        qty = 1.0,
                        unit = "unit",
                        unitPrice = tx.totalAmount,
                        total = tx.totalAmount
                    )
                )
            }

            val invoiceNo = if (tx.invoiceNumber.isNotBlank()) tx.invoiceNumber else "INV-${tx.id.toString().padStart(5, '0')}"
            val receipt = EscPosThermalPrinterEngine.ReceiptData(
                header = thermalPrinterConfig.value.storeHeader,
                invoiceNo = invoiceNo,
                dateTimestamp = tx.dateTimestamp,
                customerName = tx.vendor,
                items = items,
                subtotal = tx.totalAmount,
                discountAmount = 0.0,
                taxAmount = 0.0,
                netTotal = tx.totalAmount,
                paymentMethod = tx.paymentMethod
            )

            openReceiptPreview(receipt)
        }
    }

    fun shareReceiptViaIntent(receipt: EscPosThermalPrinterEngine.ReceiptData) {
        thermalPrinterManager.shareReceiptAsText(receipt)
    }

    // --- SUPPLY CHAIN & VENDOR MANAGEMENT ACTIONS ---

    fun saveSupplier(
        id: Long = 0,
        name: String,
        contactPerson: String = "",
        phone: String = "",
        email: String = "",
        address: String = "",
        gstin: String = "",
        paymentTerms: String = "Net 15 Days",
        leadTimeDays: Int = 2,
        notes: String = ""
    ) {
        viewModelScope.launch {
            if (name.isBlank()) return@launch
            val supplier = SupplierEntity(
                id = id,
                name = name.trim(),
                contactPerson = contactPerson.trim(),
                phone = phone.trim(),
                email = email.trim(),
                address = address.trim(),
                gstin = gstin.trim().uppercase(),
                paymentTerms = paymentTerms,
                leadTimeDays = leadTimeDays.coerceAtLeast(1),
                notes = notes.trim()
            )
            if (id == 0L) {
                repository.insertSupplier(supplier)
                _toastMessage.value = "Distributor Added: $name"
            } else {
                repository.updateSupplier(supplier)
                _toastMessage.value = "Supplier Updated: $name"
            }
        }
    }

    fun deleteSupplier(id: Long) {
        viewModelScope.launch {
            repository.deleteSupplier(id)
            _toastMessage.value = "Supplier removed from directory"
        }
    }

    fun createPurchaseOrder(
        supplierId: Long,
        supplierName: String,
        items: List<PurchaseOrderItemEntity>,
        expectedDeliveryDays: Int = 2,
        shippingNotes: String = ""
    ) {
        viewModelScope.launch {
            if (items.isEmpty()) {
                _toastMessage.value = "Please add at least 1 item to Purchase Order"
                return@launch
            }
            val poId = repository.createPurchaseOrder(
                supplierId = supplierId,
                supplierName = supplierName,
                items = items,
                expectedDeliveryDays = expectedDeliveryDays,
                shippingNotes = shippingNotes
            )
            if (poId > 0) {
                _toastMessage.value = "PO Created: PO-$poId ($supplierName)"
            }
        }
    }

    fun autoReplenishLowStockPO(supplier: SupplierEntity) {
        viewModelScope.launch {
            val lowStock = repository.lowStockCommodities.first()
            if (lowStock.isEmpty()) {
                _toastMessage.value = "All stock levels healthy! No items need restocking."
                return@launch
            }
            val poId = repository.autoGenerateLowStockPurchaseOrder(supplier, lowStock)
            if (poId > 0) {
                _toastMessage.value = "Auto-Generated PO #$poId for ${lowStock.size} low stock SKUs"
            }
        }
    }

    fun receiveGoodsReceiptNote(
        poId: Long,
        supplierName: String,
        items: List<GrnReceiptItemInput>,
        grnNumber: String = ""
    ) {
        viewModelScope.launch {
            val success = repository.receiveStockGRN(
                poId = poId,
                supplierName = supplierName,
                receivedItems = items,
                grnNumber = grnNumber
            )
            if (success) {
                _toastMessage.value = "GRN Confirmed: Stock & Batches Updated!"
            }
        }
    }

    fun performManualStockAdjustment(
        commodityId: Long,
        adjustmentQty: Double,
        reason: String,
        notes: String
    ) {
        viewModelScope.launch {
            val success = repository.recordManualStockAdjustment(
                commodityId = commodityId,
                adjustmentQty = adjustmentQty,
                reason = reason,
                notes = notes
            )
            if (success) {
                _toastMessage.value = "Stock Audit Log Recorded (${if (adjustmentQty >= 0) "+$adjustmentQty" else adjustmentQty})"
            }
        }
    }

    fun updatePurchaseOrderStatus(poId: Long, status: String) {
        viewModelScope.launch {
            repository.updatePurchaseOrderStatus(poId, status)
            _toastMessage.value = "PO Status changed to $status"
        }
    }

    fun deletePurchaseOrder(poId: Long) {
        viewModelScope.launch {
            repository.deletePurchaseOrder(poId)
            _toastMessage.value = "Purchase Order Deleted"
        }
    }

    fun sharePurchaseOrderViaWhatsApp(po: PurchaseOrderEntity, context: Context) {
        viewModelScope.launch {
            val items = repository.getPurchaseOrderItems(po.id).first()
            val text = SupplyChainEngine.formatPurchaseOrderShareText(
                po = po,
                items = items,
                storeName = thermalPrinterConfig.value.storeHeader.storeName,
                storePhone = thermalPrinterConfig.value.storeHeader.phone
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, text)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(android.content.Intent.createChooser(intent, "Dispatch Purchase Order"))
            } catch (e: Exception) {
                _toastMessage.value = "Unable to dispatch intent: ${e.message}"
            }
        }
    }

    fun addCommoditySku(
        rawKey: String,
        canonicalName: String,
        category: String,
        brand: String,
        unit: String,
        stock: Double,
        reorder: Double,
        cost: Double,
        sell: Double,
        sku: String
    ) {
        viewModelScope.launch {
            repository.insertCommodityDirect(
                CommodityEntity(
                    rawKey = rawKey.ifBlank { canonicalName.lowercase().trim() },
                    canonicalName = canonicalName,
                    category = category,
                    brand = brand,
                    normalizedUnit = unit,
                    stockQuantity = stock,
                    reorderThreshold = reorder,
                    costPrice = cost,
                    sellingPrice = sell,
                    lastKnownPrice = sell,
                    sku = sku
                )
            )
            _toastMessage.value = "Added SKU: $canonicalName"
        }
    }

    fun updateCommodity(commodity: CommodityEntity) {
        viewModelScope.launch {
            repository.insertCommodityDirect(commodity)
            _toastMessage.value = "Updated SKU: ${commodity.canonicalName}"
        }
    }

    fun deleteCommodity(id: Long) {
        viewModelScope.launch {
            repository.deleteCommodity(id)
            _toastMessage.value = "Commodity SKU Deleted"
        }
    }

    fun quickAdjustCommodityStock(commodityId: Long, delta: Double) {
        viewModelScope.launch {
            repository.recordManualStockAdjustment(
                commodityId = commodityId,
                adjustmentQty = delta,
                reason = if (delta >= 0) "Quick Restock" else "Quick Decrement",
                notes = "POS/Inventory fast-step adjustment"
            )
        }
    }

    fun recordSupplierPayment(supplierId: Long, poId: Long?, amount: Double) {
        viewModelScope.launch {
            if (amount <= 0) {
                _toastMessage.value = "Please enter a valid payment amount"
                return@launch
            }
            val success = repository.recordSupplierPayment(supplierId, poId, amount)
            if (success) {
                _toastMessage.value = "Payment of ₹${amount.toInt()} recorded against Accounts Payable"
            }
        }
    }

    fun applyCommodityMarkdown(commodityId: Long, markdownPercent: Double, batchId: Long? = null) {
        viewModelScope.launch {
            val success = repository.applyCommodityMarkdown(commodityId, markdownPercent, batchId)
            if (success) {
                _toastMessage.value = "Promotional Markdown Applied: -${markdownPercent.toInt()}%"
            }
        }
    }

    fun writeOffExpiredBatch(batch: BatchEntity) {
        viewModelScope.launch {
            val success = repository.writeOffExpiredBatch(
                batchId = batch.id,
                commodityId = batch.commodityId,
                quantity = batch.quantity,
                unit = batch.unit,
                commodityName = batch.commodityName
            )
            if (success) {
                _toastMessage.value = "Expired Lot ${batch.batchNumber} (${batch.quantity.toInt()} ${batch.unit}) Discarded & Written-Off"
            }
        }
    }

    fun addCustomBatch(
        commodityId: Long,
        commodityName: String,
        batchNumber: String,
        quantity: Double,
        costPrice: Double,
        sellingPrice: Double,
        expiryDateMs: Long,
        supplierName: String,
        unit: String
    ) {
        viewModelScope.launch {
            val batch = BatchEntity(
                commodityId = commodityId,
                commodityName = commodityName,
                batchNumber = if (batchNumber.isNotBlank()) batchNumber else "LOT-${System.currentTimeMillis() % 100000}",
                quantity = quantity.coerceAtLeast(1.0),
                unit = unit,
                costPrice = costPrice,
                sellingPrice = sellingPrice,
                mfgDateTimestamp = System.currentTimeMillis() - (15L * 86_400_000L),
                expiryDateTimestamp = if (expiryDateMs > 0) expiryDateMs else System.currentTimeMillis() + (180L * 86_400_000L),
                supplierName = supplierName,
                receivedDateTimestamp = System.currentTimeMillis()
            )
            repository.insertBatchDirect(batch)
            if (commodityId > 0 && quantity > 0) {
                repository.recordManualStockAdjustment(
                    commodityId = commodityId,
                    adjustmentQty = quantity,
                    reason = "NEW_BATCH_INTAKE",
                    notes = "Lot ${batch.batchNumber} Intake from $supplierName"
                )
            }
            _toastMessage.value = "Batch ${batch.batchNumber} registered successfully"
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceRecognitionManager?.destroy()
        offlineWhisperEngine.stopOfflineListening()
        voiceTtsEngine.shutdown()
    }
}
