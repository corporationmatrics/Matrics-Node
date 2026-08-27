package com.example

import com.example.data.BankSmsParser
import com.example.data.EmailInvoiceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankAndEmailParserTest {

    @Test
    fun testHdfcBankSmsParsing() {
        val sms = "Rs 450.00 debited from HDFC Bank A/C **1234 on 21-AUG-26 to SWIGGY. UPI Ref 324156298172."
        val result = BankSmsParser.parse("HDFCBK", sms)

        assertNotNull(result)
        assertEquals(450.0, result!!.amount, 0.01)
        assertEquals("SWIGGY", result.merchant)
        assertEquals("HDFC Bank", result.bankName)
        assertEquals("Dining", result.category)
        assertTrue(result.accountLast4.contains("1234"))
        assertEquals("UPI", result.txnType)
    }

    @Test
    fun testSbiBankSmsParsing() {
        val sms = "Dear SBI User, your A/C 9876 debited by Rs 1250.50 on 20Aug26 at ZEPTO. Ref No 9812734612."
        val result = BankSmsParser.parse("SBIINB", sms)

        assertNotNull(result)
        assertEquals(1250.50, result!!.amount, 0.01)
        assertEquals("ZEPTO", result.merchant)
        assertEquals("State Bank of India", result.bankName)
        assertEquals("Groceries", result.category)
        assertTrue(result.accountLast4.contains("9876"))
    }

    @Test
    fun testIciciBankSmsParsing() {
        val sms = "ICICI Bank Acct XX4455 debited for INR 2100.00 on 19-Aug-26 spent on AMAZON PAY. Bal: INR 15400."
        val result = BankSmsParser.parse("ICICIB", sms)

        assertNotNull(result)
        assertEquals(2100.0, result!!.amount, 0.01)
        assertTrue(result.merchant.contains("AMAZON", ignoreCase = true))
        assertEquals("ICICI Bank", result.bankName)
        assertTrue(result.accountLast4.contains("4455"))
    }

    @Test
    fun testZeptoEmailInvoiceParsing() {
        val rawEmail = """
            Your Zepto Order #ZP-98214 is Delivered!
            Items:
            - Amul Taaza Fresh Milk 500ml x 2 - Rs 54
            - Farm Fresh Tomato 1 kg x 1 - Rs 40
            - Fortune Sunlite Refined Sunflower Oil 1L x 1 - Rs 145
            Delivery Charge: Rs 15
            Handling Fee: Rs 5
            Total Paid: Rs 259 via UPI
        """.trimIndent()

        val parsed = EmailInvoiceParser.parse(rawEmail)
        assertEquals("Zepto", parsed.merchant)
        assertEquals(259.0, parsed.totalAmount, 0.01)
        assertTrue(parsed.items.isNotEmpty())
        assertTrue(parsed.isGrocery)
    }

    @Test
    fun testZomatoEmailInvoiceParsing() {
        val rawEmail = """
            Zomato Order Summary
            Order ID: #ZOM-771239
            Restaurant: Mainland China
            Items:
            - Veg Hakka Noodles x 2 - Rs 360
            - Chilli Paneer Gravy x 1 - Rs 310
            Taxes & Charges: Rs 65
            Total: Rs 735
            Payment: Paid Online
        """.trimIndent()

        val parsed = EmailInvoiceParser.parse(rawEmail)
        assertEquals("Zomato", parsed.merchant)
        assertEquals(735.0, parsed.totalAmount, 0.01)
        assertTrue(parsed.items.any { it.name.contains("Noodles") })
        assertTrue(parsed.items.any { it.name.contains("Paneer") })
    }
}
