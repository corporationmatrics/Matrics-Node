package com.example

import com.example.data.CategorySuggester
import com.example.data.NlpParsingEngine
import com.example.data.model.ParsedNlpItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategorySuggesterTest {

    @Test
    fun testDairyCategorySuggestions() {
        assertEquals("Dairy", CategorySuggester.suggestCategory("Butter"))
        assertEquals("Dairy", CategorySuggester.suggestCategory("Amul Butter"))
        assertEquals("Dairy", CategorySuggester.suggestCategory("Whole Milk"))
        assertEquals("Dairy", CategorySuggester.suggestCategory("Greek Yogurt"))
        assertEquals("Dairy", CategorySuggester.suggestCategory("Paneer"))
        assertEquals("Dairy", CategorySuggester.suggestCategory("Cheddar Cheese"))
        assertEquals("Dairy", CategorySuggester.suggestCategory("Ghee"))
    }

    @Test
    fun testProduceCategorySuggestions() {
        assertEquals("Produce", CategorySuggester.suggestCategory("Avocado"))
        assertEquals("Produce", CategorySuggester.suggestCategory("Organic Eggs"))
        assertEquals("Produce", CategorySuggester.suggestCategory("Tomatoes"))
        assertEquals("Produce", CategorySuggester.suggestCategory("Spinach"))
        assertEquals("Produce", CategorySuggester.suggestCategory("Bananas"))
        assertEquals("Produce", CategorySuggester.suggestCategory("Apples"))
    }

    @Test
    fun testGrainsCategorySuggestions() {
        assertEquals("Grains", CategorySuggester.suggestCategory("Basmati Rice"))
        assertEquals("Grains", CategorySuggester.suggestCategory("Wheat Flour"))
        assertEquals("Grains", CategorySuggester.suggestCategory("Atta"))
        assertEquals("Grains", CategorySuggester.suggestCategory("Rolled Oats"))
        assertEquals("Grains", CategorySuggester.suggestCategory("Toor Dal"))
        assertEquals("Grains", CategorySuggester.suggestCategory("Sourdough Bread"))
    }

    @Test
    fun testPantryCategorySuggestions() {
        assertEquals("Pantry", CategorySuggester.suggestCategory("Extra Virgin Olive Oil"))
        assertEquals("Pantry", CategorySuggester.suggestCategory("Table Salt"))
        assertEquals("Pantry", CategorySuggester.suggestCategory("Brown Sugar"))
        assertEquals("Pantry", CategorySuggester.suggestCategory("Honey"))
        assertEquals("Pantry", CategorySuggester.suggestCategory("Garam Masala"))
        assertEquals("Pantry", CategorySuggester.suggestCategory("Soy Sauce"))
    }

    @Test
    fun testHouseholdAndPersonalCareSuggestions() {
        assertEquals("Household", CategorySuggester.suggestCategory("Laundry Detergent"))
        assertEquals("Household", CategorySuggester.suggestCategory("Dish Soap"))
        assertEquals("Household", CategorySuggester.suggestCategory("Toilet Paper"))
        assertEquals("Personal Care", CategorySuggester.suggestCategory("Shampoo"))
        assertEquals("Personal Care", CategorySuggester.suggestCategory("Toothpaste"))
        assertEquals("Personal Care", CategorySuggester.suggestCategory("Sunscreen"))
    }

    @Test
    fun testUtilitiesAndDiningSuggestions() {
        assertEquals("Utilities", CategorySuggester.suggestCategory("Electricity Bill"))
        assertEquals("Utilities", CategorySuggester.suggestCategory("Water Utility"))
        assertEquals("Utilities", CategorySuggester.suggestCategory("WiFi Bill"))
        assertEquals("Dining", CategorySuggester.suggestCategory("Pizza"))
        assertEquals("Dining", CategorySuggester.suggestCategory("Burger"))
        assertEquals("Dining", CategorySuggester.suggestCategory("Croissant"))
    }

    @Test
    fun testTransactionCategoryAggregation() {
        val groceryItems = listOf(
            ParsedNlpItem(name = "Basmati Rice 2kg", category = "Grains", quantity = 1.0, unit = "kg", price = 140.0, vendor = "DMart", canonicalName = "Basmati Rice"),
            ParsedNlpItem(name = "Amul Butter 500g", category = "Dairy", quantity = 1.0, unit = "g", price = 250.0, vendor = "DMart", canonicalName = "Butter"),
            ParsedNlpItem(name = "Organic Eggs 12pcs", category = "Produce", quantity = 1.0, unit = "pcs", price = 95.0, vendor = "DMart", canonicalName = "Eggs")
        )

        val txCategory = CategorySuggester.suggestTransactionCategory(groceryItems)
        assertEquals("Groceries", txCategory)
    }

    @Test
    fun testItemEnrichment() {
        val rawItem = ParsedNlpItem(
            name = "Amul Butter 500g",
            category = "General",
            quantity = 1.0,
            unit = "g",
            price = 250.0,
            vendor = "DMart",
            canonicalName = "Butter"
        )

        val enriched = CategorySuggester.enrichItem(rawItem)
        assertEquals("Dairy", enriched.category)
        assertEquals("Refrigerated", enriched.storageType)
        assertTrue(enriched.shelfLifeDays > 0)
    }

    @Test
    fun testNaturalLanguageSpentPhrases() {
        val (vendor, items) = NlpParsingEngine.parseInput("spent 5 dollars on coffee")
        assertEquals(1, items.size)
        assertEquals("Coffee", items[0].name)
        assertEquals(5.0, items[0].price, 0.01)
        assertEquals("Beverages", items[0].category)

        val (_, diningItems) = NlpParsingEngine.parseInput("paid 15 dollars for burger from Wendy's")
        assertEquals(1, diningItems.size)
        assertEquals("Burger", diningItems[0].name)
        assertEquals(15.0, diningItems[0].price, 0.01)
        assertEquals("Dining", diningItems[0].category)
    }
}
