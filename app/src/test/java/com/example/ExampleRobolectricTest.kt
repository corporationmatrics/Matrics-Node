package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.NlpParsingEngine
import com.example.ui.viewmodel.ApiKeyStatus
import com.example.ui.viewmodel.CyphrViewModel
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Matrics Node", appName)
  }

  @Test
  fun `test nlp parser extracts items and vendor`() {
    val input = "Rice at 50, Amul Butter 500g at 100 from FreshMart"
    val (vendor, items) = NlpParsingEngine.parseInput(input)
    assertEquals("FreshMart", vendor)
    assertEquals(2, items.size)
    assertEquals("Rice", items[0].name)
    assertEquals(50.0, items[0].price, 0.01)
    assertEquals("Amul Butter", items[1].name)
    assertEquals(100.0, items[1].price, 0.01)
  }

  @Test
  fun `test viewmodel api key management`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = CyphrViewModel(app)

    viewModel.saveGeminiApiKey("AIzaSyTestApiKey12345")
    assertEquals("AIzaSyTestApiKey12345", viewModel.userGeminiApiKey.value)

    viewModel.clearGeminiApiKey()
    assertEquals("", viewModel.userGeminiApiKey.value)
  }

  @Test
  fun `test voice transcript processing updates items`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = CyphrViewModel(app)

    viewModel.openVoiceHud(startListening = false)
    assertTrue(viewModel.voiceHudState.value.isVisible)

    viewModel.updateVoiceTranscript("Organic Eggs 12 pcs at 90 from Nature Basket", isFinal = true)
    val state = viewModel.voiceHudState.value
    assertEquals("Nature Basket", state.detectedVendor)
    assertTrue(state.parsedItems.isNotEmpty())
    assertEquals("Organic Eggs", state.parsedItems[0].name)
    assertEquals(90.0, state.parsedItems[0].price, 0.01)
  }

  @Test
  fun `test room database transaction entity persistence`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
        context,
        com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()

    val dao = db.expenseDao()
    val testTx = com.example.data.model.TransactionEntity(
        title = "SuperMart Grocery",
        vendor = "SuperMart",
        category = "Groceries",
        totalAmount = 450.0,
        dateTimestamp = System.currentTimeMillis(),
        paymentMethod = "UPI Instant",
        locationName = "Downtown Hub",
        itemCount = 2,
        isVerified = true
    )

    val txId = dao.insertTransaction(testTx)
    assertTrue(txId > 0)

    val allTxs = dao.getAllTransactions().first()
    assertTrue(allTxs.any { it.vendor == "SuperMart" && it.totalAmount == 450.0 })
    db.close()
  }

  @Test
  fun `test 3-tier commodity engine resolution and learning`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
        context,
        com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()

    val dao = db.expenseDao()
    val engine = com.example.data.CommodityEngine(dao)

    // Pre-seed an item in Tier 2
    dao.insertCommodity(
        com.example.data.model.CommodityEntity(
            rawKey = "amul butter",
            canonicalName = "Butter",
            brand = "Amul",
            category = "Dairy",
            subcategory = "Spreads",
            defaultQuantity = 500.0,
            normalizedUnit = "g",
            estimatedShelfLifeDays = 180,
            storageType = "Refrigerated",
            lastKnownPrice = 300.0,
            useCount = 1,
            isPreSeeded = true
        )
    )

    // Resolve via engine (offline mode)
    val (vendor, items) = engine.resolveInput("Amul Butter at 300 from FreshMart", apiKey = "")
    assertEquals("FreshMart", vendor)
    assertTrue(items.isNotEmpty())
    assertEquals("Butter", items[0].canonicalName)
    assertEquals("Amul", items[0].brand)
    assertEquals("Dairy", items[0].category)
    assertEquals("Refrigerated", items[0].storageType)
    assertEquals(180, items[0].shelfLifeDays)
    assertEquals("TIER_2_SEEDED", items[0].tierResolved)

    db.close()
  }

  @Test
  fun `test CommodityPrepopulationHelper parses JSON string correctly`() {
    val sampleJson = """
      [
        {
          "rawKey": "sourdough bread",
          "canonicalName": "Artisan Sourdough Bread",
          "brand": "Artisan Bakery",
          "category": "Grains",
          "subcategory": "Bakery",
          "defaultQuantity": 1.0,
          "normalizedUnit": "loaf",
          "estimatedShelfLifeDays": 5,
          "storageType": "Pantry",
          "lastKnownPrice": 140.0,
          "useCount": 3
        },
        {
          "rawKey": "dish soap",
          "canonicalName": "Dishwashing Liquid Gel",
          "brand": "Vim / Pril",
          "category": "Household",
          "subcategory": "Dish Care",
          "defaultQuantity": 750.0,
          "normalizedUnit": "ml",
          "estimatedShelfLifeDays": 730,
          "storageType": "Pantry",
          "lastKnownPrice": 145.0,
          "useCount": 4
        }
      ]
    """.trimIndent()

    val entities = com.example.data.CommodityPrepopulationHelper.parseCommoditiesFromJson(sampleJson)
    assertEquals(2, entities.size)

    val bread = entities[0]
    assertEquals("sourdough bread", bread.rawKey)
    assertEquals("Artisan Sourdough Bread", bread.canonicalName)
    assertEquals("Artisan Bakery", bread.brand)
    assertEquals("Grains", bread.category)
    assertEquals("Bakery", bread.subcategory)
    assertEquals(5, bread.estimatedShelfLifeDays)
    assertEquals("Pantry", bread.storageType)
    assertEquals(140.0, bread.lastKnownPrice, 0.01)
    assertTrue(bread.isPreSeeded)

    val soap = entities[1]
    assertEquals("dish soap", soap.rawKey)
    assertEquals("Dishwashing Liquid Gel", soap.canonicalName)
    assertEquals("Household", soap.category)
  }

  @Test
  fun `test CommodityPrepopulationHelper seeds database from assets`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
        context,
        com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()

    val dao = db.expenseDao()

    // Test seeding from asset file
    val seededCount = com.example.data.CommodityPrepopulationHelper.prePopulateFromAsset(context, dao)
    assertTrue(seededCount > 0)

    val countInDb = dao.getCommoditiesCount()
    assertTrue(countInDb >= seededCount)

    // Verify lookup of a seeded commodity
    val butter = dao.getCommodityByKey("amul butter")
    assertNotNull(butter)
    assertEquals("Butter", butter?.canonicalName)
    assertEquals("Amul", butter?.brand)
    assertEquals("Refrigerated", butter?.storageType)

    // Test engine resolution with newly seeded data
    val engine = com.example.data.CommodityEngine(dao)
    val (_, items) = engine.resolveInput("Avocado at 180 from FreshMart", apiKey = "")
    assertTrue(items.isNotEmpty())
    assertEquals("Avocado Hass", items[0].canonicalName)
    assertEquals("TIER_2_SEEDED", items[0].tierResolved)

    db.close()
  }

  @Test
  fun `test handwritten kacha bill preset 1 detects math error and hinglish translation`() = kotlinx.coroutines.runBlocking {
    val preset = com.example.data.model.KachaBillPresets.PRESETS.first { it.vendorName.contains("Sharma") }
    assertEquals("Sharma Kirana & General Store", preset.vendorName)
    assertEquals(5, preset.items.size)
    
    // Check Hinglish entity normalization
    val atta = preset.items[0]
    assertEquals("Whole Wheat Flour (Atta)", atta.canonicalName)
    assertEquals("Aashirvaad", atta.brand)
    assertEquals("Grains", atta.category)
    assertEquals(210.0, atta.price, 0.01)

    // Check colloquial unit "1 pav" -> 250g
    val butter = preset.items[1]
    assertEquals("Salted Table Butter", butter.canonicalName)
    assertEquals("Amul", butter.brand)
    assertEquals(250.0, butter.quantity, 0.01)
    assertEquals("g", butter.unit)
    assertTrue(butter.isLowConfidence) // Bad scribble flagged

    // Check forensic math verification: 210 + 80 + 90 + 35 + 140 = 555. Shopkeeper wrote 585 (+30 mismatch!)
    val computedSum = preset.items.sumOf { it.price }
    assertEquals(555.0, computedSum, 0.01)
    assertEquals(585.0, preset.shopkeeperTotal, 0.01)
    assertTrue(preset.mathErrorFlag)
    assertEquals(30.0, preset.mathErrorDelta, 0.01)
  }

  @Test
  fun `test handwritten kacha bill preset 2 extracts khata running balance`() = kotlinx.coroutines.runBlocking {
    val preset = com.example.data.model.KachaBillPresets.PRESETS.first { it.vendorName.contains("Gupta") }
    assertEquals("Gupta Provision Store", preset.vendorName)
    assertEquals(4, preset.items.size)
    assertNotNull(preset.khataOldBalance)
    assertEquals(650.0, preset.khataOldBalance!!, 0.01)
    assertEquals(420.0, preset.calculatedTrueTotal, 0.01)
    assertEquals(1070.0, preset.khataNewBalance!!, 0.01)
  }

  @Test
  fun `test committing handwritten bill to ledger updates Room database and restocks pantry`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
        context,
        com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()

    val dao = db.expenseDao()
    val repo = com.example.data.ExpenseRepository(dao)

    val preset = com.example.data.model.KachaBillPresets.PRESETS.first { it.vendorName.contains("Sharma") }
    val parsedItems = preset.items.map { item ->
        com.example.data.model.ParsedNlpItem(
            name = item.canonicalName,
            category = item.category,
            quantity = item.quantity,
            unit = item.unit,
            price = item.price,
            vendor = preset.vendorName,
            canonicalName = item.canonicalName,
            brand = item.brand,
            subcategory = item.subcategory,
            storageType = item.storageType,
            shelfLifeDays = item.shelfLifeDays,
            tierResolved = "TIER_3_GEMINI_VISION"
        )
    }

    val txId = repo.logExpenseWithItems(
        title = "${preset.vendorName} (Kacha Bill)",
        vendor = preset.vendorName,
        category = "Groceries",
        items = parsedItems,
        paymentMethod = "Cash / Kirana Bill",
        locationName = "Local Kirana Store",
        rawVoicePrompt = "Handwritten Kacha Bill"
    )

    assertTrue(txId > 0)

    val txs = repo.allTransactions.first()
    assertTrue(txs.any { it.vendor == "Sharma Kirana & General Store" })

    val lineItems = repo.allLineItems.first()
    assertTrue(lineItems.any { it.name.contains("Flour") || it.name.contains("Atta") || it.canonicalName.contains("Flour") })

    // Verify line item metadata preserved shelf life and storage type
    val flourItem = lineItems.first { it.name.contains("Flour") || it.canonicalName.contains("Flour") }
    assertEquals("Grains", flourItem.category)
    assertEquals("Pantry", flourItem.storageType)
    assertEquals(90, flourItem.shelfLifeDays)

    db.close()
  }

  @Test
  fun `test image processing scaling and binarization`() {
    val bitmap = android.graphics.Bitmap.createBitmap(2000, 1000, android.graphics.Bitmap.Config.ARGB_8888)
    val scaled = com.example.util.ImageProcessingUtils.scaleBitmap(bitmap, maxDimension = 1000)
    assertEquals(1000, scaled.width)
    assertEquals(500, scaled.height)

    val binarized = com.example.util.ImageProcessingUtils.applyBinarizationAndInkBoost(scaled)
    assertNotNull(binarized)
    assertEquals(1000, binarized.width)
    assertEquals(500, binarized.height)
  }

  @Test
  fun `test self-learning commodity loop persists newly resolved items`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
        context,
        com.example.data.AppDatabase::class.java
    ).allowMainThreadQueries().build()

    val dao = db.expenseDao()
    val repo = com.example.data.ExpenseRepository(dao)

    val item = com.example.data.model.ParsedNlpItem(
        name = "Dragonfruit Organic",
        category = "Produce",
        quantity = 2.0,
        unit = "pcs",
        price = 220.0,
        vendor = "Exotic Fruit Basket",
        canonicalName = "Dragon Fruit (Pitaya)",
        brand = "Farm Fresh",
        subcategory = "Exotic Fruit",
        storageType = "Refrigerated",
        shelfLifeDays = 7,
        tierResolved = "TIER_3_GEMINI"
    )

    // First time learned
    repo.learnCommodity(item)
    val stored = dao.getCommodityByKey("dragonfruit organic")
    assertNotNull(stored)
    assertEquals("Dragon Fruit (Pitaya)", stored?.canonicalName)
    assertEquals("Farm Fresh", stored?.brand)
    assertEquals("Refrigerated", stored?.storageType)
    assertEquals(1, stored?.useCount)

    // Second time learned -> usage counter incremented
    repo.learnCommodity(item)
    val updated = dao.getCommodityByKey("dragonfruit organic")
    assertEquals(2, updated?.useCount)

    db.close()
  }

  @Test
  fun `test handwritten scanner viewmodel receives captured bitmap and generates binarized preview`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = CyphrViewModel(app)

    viewModel.openHandwrittenScanner()
    assertTrue(viewModel.handwrittenScannerState.value.isVisible)

    val testBmp = android.graphics.Bitmap.createBitmap(800, 600, android.graphics.Bitmap.Config.ARGB_8888)
    viewModel.onHandwrittenRealBitmapCaptured(testBmp)

    val state = viewModel.handwrittenScannerState.value
    assertEquals("CAMERA", state.imageSource)
    assertNotNull(state.capturedBitmap)
    assertNotNull(state.binarizedBitmap)
    assertEquals(800, state.capturedBitmap?.width)
  }
}

