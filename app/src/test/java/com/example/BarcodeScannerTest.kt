package com.example

import com.example.data.model.CommodityEntity
import com.example.ui.viewmodel.BarcodeScanMode
import com.example.ui.viewmodel.ScannedBarcodeRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeScannerTest {

    @Test
    fun testBarcodeMatchByExactSku() {
        val commodities = listOf(
            CommodityEntity(
                id = 1,
                rawKey = "maggi_noodles_70g",
                canonicalName = "Maggi 2-Minute Noodles",
                brand = "Nestle",
                category = "Food & Grocery",
                normalizedUnit = "pack",
                sku = "8901058852391",
                sellingPrice = 14.0,
                stockQuantity = 50.0
            ),
            CommodityEntity(
                id = 2,
                rawKey = "amul_butter_100g",
                canonicalName = "Amul Pasteurised Butter",
                brand = "Amul",
                category = "Dairy",
                normalizedUnit = "g",
                sku = "8901262010054",
                sellingPrice = 58.0,
                stockQuantity = 30.0
            )
        )

        val scannedCode = "8901058852391"
        val matched = commodities.firstOrNull { it.sku.equals(scannedCode, ignoreCase = true) }
        
        assertNotNull(matched)
        assertEquals("Maggi 2-Minute Noodles", matched?.canonicalName)
        assertEquals(14.0, matched?.sellingPrice ?: 0.0, 0.01)
    }

    @Test
    fun testBarcodeMatchFallbackToRawKeyOrName() {
        val commodities = listOf(
            CommodityEntity(
                id = 1,
                rawKey = "coca_cola_can_300ml",
                canonicalName = "Coca Cola Can",
                brand = "Coca-Cola",
                category = "Beverages",
                normalizedUnit = "can",
                sku = "",
                sellingPrice = 40.0,
                stockQuantity = 100.0
            )
        )

        val query = "coca_cola_can_300ml"
        val matched = commodities.firstOrNull { 
            it.sku.equals(query, ignoreCase = true) || it.rawKey.equals(query, ignoreCase = true) 
        }

        assertNotNull(matched)
        assertEquals("Coca Cola Can", matched?.canonicalName)
    }

    @Test
    fun testUnrecognizedBarcodeHandling() {
        val commodities = listOf(
            CommodityEntity(
                id = 1,
                rawKey = "parle_g_biscuits",
                canonicalName = "Parle-G Gold Biscuits",
                brand = "Parle",
                category = "Snacks",
                normalizedUnit = "pack",
                sku = "8901719101018",
                sellingPrice = 10.0,
                stockQuantity = 120.0
            )
        )

        val unknownBarcode = "9999999999999"
        val matched = commodities.firstOrNull { it.sku.equals(unknownBarcode, ignoreCase = true) }
        
        assertNull(matched)
    }

    @Test
    fun testScannedBarcodeRecordCreation() {
        val record = ScannedBarcodeRecord(
            barcode = "8901058852391",
            matchedCommodityName = "Maggi 2-Minute Noodles",
            price = 14.0,
            quantityAdded = 1.0,
            timestamp = System.currentTimeMillis()
        )

        assertEquals("8901058852391", record.barcode)
        assertEquals("Maggi 2-Minute Noodles", record.matchedCommodityName)
        assertEquals(14.0, record.price, 0.01)
        assertEquals(1.0, record.quantityAdded, 0.01)
    }
}

