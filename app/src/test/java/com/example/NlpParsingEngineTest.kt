package com.example

import com.example.data.NlpParsingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NlpParsingEngineTest {

    @Test
    fun testMultiItemVoiceParsing() {
        val input = "Rice at 50, Amul Butter 500 grams at 100 from FreshMart"
        val (vendor, items) = NlpParsingEngine.parseInput(input)

        assertEquals("FreshMart", vendor)
        assertEquals(2, items.size)

        val item1 = items[0]
        assertTrue(item1.name.contains("Rice", ignoreCase = true))
        assertEquals(50.0, item1.price, 0.01)
        assertEquals("Grains", item1.category)

        val item2 = items[1]
        assertTrue(item2.name.contains("Amul Butter", ignoreCase = true))
        assertEquals(100.0, item2.price, 0.01)
        assertEquals("Dairy", item2.category)
        assertEquals(500.0, item2.quantity, 0.01)
    }

    @Test
    fun testTokenizeForHud() {
        val input = "Rice at 50 from FreshMart"
        val tokens = NlpParsingEngine.tokenizeForHud(input)
        assertTrue(tokens.any { it.type == NlpParsingEngine.TokenType.PRICE })
        assertTrue(tokens.any { it.type == NlpParsingEngine.TokenType.VENDOR })
    }

    @Test
    fun testConversationalVoicePhrasing() {
        val input = "Rice 5 kg at 250 and Amul Butter 500g at 100"
        val (_, items) = NlpParsingEngine.parseInput(input)
        assertEquals(2, items.size)

        val rice = items[0]
        assertTrue(rice.name.contains("Rice", ignoreCase = true))
        assertEquals(250.0, rice.price, 0.01)

        val butter = items[1]
        assertTrue(butter.name.contains("Butter", ignoreCase = true))
        assertEquals(100.0, butter.price, 0.01)
    }
}
