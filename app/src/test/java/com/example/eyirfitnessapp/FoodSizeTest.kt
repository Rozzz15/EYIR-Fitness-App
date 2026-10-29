package com.example.eyirfitnessapp

import com.example.eyirfitnessapp.domain.NutritionEngine
import com.example.eyirfitnessapp.domain.model.Food
import com.example.eyirfitnessapp.domain.model.FoodSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodSizeTest {

    @Test
    fun `encode then decode round trips size options`() {
        val sizes = listOf(
            FoodSize("Small", 44.0),
            FoodSize("Medium", 50.0),
            FoodSize("Large", 56.0),
            FoodSize("X-Large", 63.0)
        )
        val raw = FoodSize.encode(sizes)
        assertEquals("Small=44.0,Medium=50.0,Large=56.0,X-Large=63.0", raw)
        assertEquals(sizes, FoodSize.decode(raw))
    }

    @Test
    fun `encode returns null for no sizes`() {
        assertEquals(null, FoodSize.encode(emptyList()))
        assertEquals(emptyList<FoodSize>(), FoodSize.decode(null))
    }

    @Test
    fun `decode skips malformed entries instead of failing`() {
        // "", "=50" (no label), "Large=abc" (not a number), "XL=-3" (non-positive)
        val sizes = FoodSize.decode("Small=44,,=50,Large=abc,XL=-3,Good=60")
        assertEquals(listOf(FoodSize("Small", 44.0), FoodSize("Good", 60.0)), sizes)
    }

    private val egg = Food(
        name = "Egg", servingSize = 1.0, servingUnit = "large",
        calories = 72.0, protein = 6.3, carbs = 0.4, fat = 4.8,
        gramsPerServing = 56.0,
        sizes = listOf(
            FoodSize("Small", 44.0),
            FoodSize("Medium", 50.0),
            FoodSize("Large", 56.0),
            FoodSize("X-Large", 63.0)
        )
    )

    @Test
    fun `medium egg scales from the large base`() {
        val medium = NutritionEngine.fromGrams(egg, 50.0)!!
        // 50 g ÷ 56 g per large = 0.892857 large → 72 kcal × that ≈ 64.3 kcal
        assertEquals(64.3, NutritionEngine.scale(egg, medium).calories, 0.1)
        assertEquals(50.0, NutritionEngine.toGrams(egg, medium)!!, 0.01)
    }

    @Test
    fun `large size matches the default serving`() {
        val large = NutritionEngine.fromGrams(egg, 56.0)!!
        assertEquals(72.0, NutritionEngine.scale(egg, large).calories, 0.01)
    }

    @Test
    fun `size chips cover every stored option`() {
        assertTrue(egg.sizes.isNotEmpty())
        egg.sizes.forEach { size ->
            val native = NutritionEngine.fromGrams(egg, size.grams)
            assertEquals(size.grams, NutritionEngine.toGrams(egg, native!!)!!, 0.01)
        }
    }
}
