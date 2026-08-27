package com.example.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class BankSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages.isNullOrEmpty()) return

                val sender = messages[0].originatingAddress ?: "Unknown"
                val body = messages.joinToString("") { it.messageBody ?: "" }
                val timestamp = messages[0].timestampMillis

                val parsed = BankSmsParser.parse(sender, body, timestamp)
                if (parsed != null) {
                    Log.d("BankSmsReceiver", "Captured Bank SMS from $sender: ₹${parsed.amount} at ${parsed.merchant}")
                    _incomingSmsFlow.tryEmit(parsed)
                }
            } catch (e: Exception) {
                Log.e("BankSmsReceiver", "Error parsing SMS: ${e.message}", e)
            }
        }
    }

    companion object {
        private val _incomingSmsFlow = MutableSharedFlow<ParsedBankSms>(extraBufferCapacity = 10)
        val incomingSmsFlow = _incomingSmsFlow.asSharedFlow()

        fun postManualSms(parsed: ParsedBankSms) {
            _incomingSmsFlow.tryEmit(parsed)
        }
    }
}
