package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Data class representing an installed UPI Payment App on the user's Android device.
 */
data class UpiAppInfo(
    val packageName: String,
    val appName: String,
    val isPreferred: Boolean = false
)

/**
 * Result state parsed from the UPI intent activity result callback bundle.
 */
sealed class UpiPaymentResult {
    data class Success(
        val txnId: String,
        val responseCode: String,
        val approvalRefNo: String, // Bank UTR
        val txnRef: String,
        val rawResponse: String
    ) : UpiPaymentResult()

    data class Failure(
        val errorMessage: String,
        val rawResponse: String
    ) : UpiPaymentResult()

    data class Cancelled(
        val rawResponse: String
    ) : UpiPaymentResult()
}

/**
 * Parameters to construct the standard NPCI compliant UPI Deep Link URI.
 */
data class UpiPaymentParams(
    val payeeVpa: String,          // pa: Payee UPI Address e.g. shopkeeper@okhdfcbank
    val payeeName: String,         // pn: Merchant / Payee Name e.g. "Ramesh Kirana"
    val amount: Double,            // am: e.g. 150.00
    val transactionRefId: String,  // tr: Unique merchant tracking reference
    val transactionNote: String,   // tn: Note/Description e.g. "Grocery checklist"
    val merchantCode: String = "", // mc: Merchant category code if applicable
    val currency: String = "INR"   // cu: Currency, always INR
)

data class MerchantStoreProfile(
    val storeName: String = "Matrics Storefront",
    val merchantVpa: String = "matrics.pos@okhdfcbank",
    val merchantCode: String = "5411", // Grocery Stores, Supermarkets
    val storeGstNumber: String = "29AAAAA0000A1Z5",
    val terminalId: String = "POS-01"
)

object UpiPaymentManager {
    private const val TAG = "UpiPaymentManager"

    // Default merchant configuration
    var currentMerchantProfile = MerchantStoreProfile()

    /**
     * Builds dynamic P2M (Person-to-Merchant) QR code URI string:
     * upi://pay?pa=merchant@upi&pn=StoreName&am=450.00&tr=INV-102938&tn=POS%20Bill&cu=INR&mc=5411
     */
    fun buildMerchantDynamicQrUri(
        amount: Double,
        invoiceNo: String,
        profile: MerchantStoreProfile = currentMerchantProfile,
        note: String = "Storefront POS Bill"
    ): String {
        val params = UpiPaymentParams(
            payeeVpa = profile.merchantVpa,
            payeeName = profile.storeName,
            amount = amount,
            transactionRefId = invoiceNo,
            transactionNote = "$note #$invoiceNo",
            merchantCode = profile.merchantCode
        )
        return buildUpiUri(params).toString()
    }

    // Known popular UPI application package names in India
    val KNOWN_UPI_PACKAGES = listOf(
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "com.phonepe.app" to "PhonePe",
        "net.one97.paytm" to "Paytm",
        "in.org.npci.upiapp" to "BHIM UPI",
        "com.dreamplug.androidapp" to "CRED",
        "com.whatsapp" to "WhatsApp Pay",
        "com.amazon.mShop.android.shopping" to "Amazon Pay"
    )

    /**
     * Builds standard NPCI UPI Deep Link URI:
     * upi://pay?pa=shopkeeper@bank&pn=VendorName&am=150.00&tr=123456&tn=Pantry&cu=INR
     */
    fun buildUpiUri(params: UpiPaymentParams): Uri {
        val encodedVpa = URLEncoder.encode(params.payeeVpa.trim(), StandardCharsets.UTF_8.name())
        val encodedName = URLEncoder.encode(params.payeeName.trim(), StandardCharsets.UTF_8.name())
        val encodedNote = URLEncoder.encode(params.transactionNote.trim(), StandardCharsets.UTF_8.name())
        val encodedTr = URLEncoder.encode(params.transactionRefId.trim(), StandardCharsets.UTF_8.name())
        val formattedAmount = String.format(Locale.US, "%.2f", params.amount)

        val uriBuilder = StringBuilder("upi://pay?")
            .append("pa=").append(encodedVpa)
            .append("&pn=").append(encodedName)
            .append("&am=").append(formattedAmount)
            .append("&tr=").append(encodedTr)
            .append("&tn=").append(encodedNote)
            .append("&cu=").append(params.currency)

        if (params.merchantCode.isNotBlank()) {
            uriBuilder.append("&mc=").append(URLEncoder.encode(params.merchantCode.trim(), StandardCharsets.UTF_8.name()))
        }

        return Uri.parse(uriBuilder.toString())
    }

    /**
     * Creates an Intent to launch the UPI Chooser or a specific UPI app.
     */
    fun createUpiIntent(params: UpiPaymentParams, targetPackage: String? = null): Intent {
        val uri = buildUpiUri(params)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (!targetPackage.isNullOrBlank()) {
            intent.setPackage(targetPackage)
        }
        return intent
    }

    /**
     * Queries the system for all installed apps that can handle the `upi://pay` URI.
     */
    fun getInstalledUpiApps(context: Context): List<UpiAppInfo> {
        val testUri = Uri.parse("upi://pay?pa=test@upi&pn=Test&am=1.00&cu=INR")
        val testIntent = Intent(Intent.ACTION_VIEW, testUri)
        val pm = context.packageManager

        return try {
            val resolveInfos = pm.queryIntentActivities(testIntent, PackageManager.MATCH_DEFAULT_ONLY)
            val apps = mutableListOf<UpiAppInfo>()

            for (resolveInfo in resolveInfos) {
                val pkgName = resolveInfo.activityInfo.packageName
                val label = resolveInfo.loadLabel(pm)?.toString() ?: pkgName
                val isKnown = KNOWN_UPI_PACKAGES.any { it.first == pkgName }
                apps.add(UpiAppInfo(packageName = pkgName, appName = label, isPreferred = isKnown))
            }

            // Also check known packages if queryIntentActivities returned empty in certain sandboxes
            if (apps.isEmpty()) {
                for ((pkg, name) in KNOWN_UPI_PACKAGES) {
                    try {
                        pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES)
                        apps.add(UpiAppInfo(packageName = pkg, appName = name, isPreferred = true))
                    } catch (_: PackageManager.NameNotFoundException) {
                        // Not installed
                    }
                }
            }

            apps.distinctBy { it.packageName }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying UPI apps: ${e.message}")
            emptyList()
        }
    }

    /**
     * Parses the returning Activity Result payload from Google Pay, PhonePe, Paytm, etc.
     * Response is typically key-value pairs formatted as:
     * "txnId=AXI123&responseCode=00&ApprovalRefNo=123456789012&Status=SUCCESS&txnRef=TXN123"
     */
    fun parseUpiResponse(rawResponse: String?): UpiPaymentResult {
        if (rawResponse.isNullOrBlank()) {
            return UpiPaymentResult.Cancelled(rawResponse = "")
        }

        Log.d(TAG, "Parsing UPI Response: $rawResponse")
        val paramsMap = mutableMapOf<String, String>()

        // Some apps return '&' delimited strings, some return lowercase/uppercase keys
        val tokens = rawResponse.split("&")
        for (token in tokens) {
            val parts = token.split("=")
            if (parts.size >= 2) {
                paramsMap[parts[0].trim().lowercase(Locale.ROOT)] = parts.subList(1, parts.size).joinToString("=").trim()
            }
        }

        val status = paramsMap["status"]?.uppercase(Locale.ROOT) ?: ""
        val txnId = paramsMap["txnid"] ?: paramsMap["txn_id"] ?: ""
        val responseCode = paramsMap["responsecode"] ?: paramsMap["response_code"] ?: ""
        val approvalRefNo = paramsMap["approvalrefno"] ?: paramsMap["approval_ref_no"] ?: paramsMap["bank_ref_num"] ?: txnId
        val txnRef = paramsMap["txnref"] ?: paramsMap["tr"] ?: ""

        return when {
            status == "SUCCESS" || status == "SUBMITTED" || responseCode == "00" -> {
                UpiPaymentResult.Success(
                    txnId = txnId.ifBlank { "TXN_${System.currentTimeMillis()}" },
                    responseCode = responseCode.ifBlank { "00" },
                    approvalRefNo = approvalRefNo.ifBlank { "UTR_${System.currentTimeMillis()}" },
                    txnRef = txnRef,
                    rawResponse = rawResponse
                )
            }
            status == "FAILURE" || status == "FAILED" || status == "FAIL" -> {
                UpiPaymentResult.Failure(
                    errorMessage = paramsMap["message"] ?: paramsMap["errormessage"] ?: "Payment rejected by bank or insufficient funds.",
                    rawResponse = rawResponse
                )
            }
            status.contains("USER_CANCEL") || status.contains("CANCEL") -> {
                UpiPaymentResult.Cancelled(rawResponse = rawResponse)
            }
            else -> {
                // If status keyword is missing but raw response says "SUCCESS"
                if (rawResponse.contains("SUCCESS", ignoreCase = true)) {
                    UpiPaymentResult.Success(
                        txnId = txnId.ifBlank { "TXN_${System.currentTimeMillis()}" },
                        responseCode = "00",
                        approvalRefNo = approvalRefNo.ifBlank { "UTR_${System.currentTimeMillis()}" },
                        txnRef = txnRef,
                        rawResponse = rawResponse
                    )
                } else {
                    UpiPaymentResult.Cancelled(rawResponse = rawResponse)
                }
            }
        }
    }
}
