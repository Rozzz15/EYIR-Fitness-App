package com.example.eyirfitnessapp

import com.example.eyirfitnessapp.domain.model.Recipe
import com.example.eyirfitnessapp.domain.plans.PlanLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanLogicTest {

    // ── Recipe math (§16) ───────────────────────────────────────────────────

    @Test
    fun `per serving divides totals by servings`() {
        val totals = PlanLogic.RecipeTotals(800.0, 60.0, 100.0, 20.0)
        val per = totals.perServing(4)
        assertEquals(200.0, per.totalCalories, 0.001)
        assertEquals(15.0, per.totalProtein, 0.001)
        assertEquals(25.0, per.totalCarbs, 0.001)
        assertEquals(5.0, per.totalFat, 0.001)
    }

    @Test
    fun `per serving treats zero servings as one`() {
        val totals = PlanLogic.RecipeTotals(600.0, 30.0, 0.0, 0.0)
        assertEquals(600.0, totals.perServing(0).totalCalories, 0.001)
    }

    @Test
    fun `recipe perServing never divides by zero`() {
        val recipe = Recipe(name = "R", servings = 0, totalCalories = 500.0, totalProtein = 25.0)
        assertEquals(500.0, recipe.perServing().totalCalories, 0.001)
    }

    @Test
    fun `recipe perServing computes macros`() {
        val recipe = Recipe(name = "R", servings = 2, totalCalories = 800.0, totalProtein = 40.0, totalCarbs = 80.0, totalFat = 20.0)
        val per = recipe.perServing()
        assertEquals(400.0, per.totalCalories, 0.001)
        assertEquals(20.0, per.totalProtein, 0.001)
        assertEquals(40.0, per.totalCarbs, 0.001)
        assertEquals(10.0, per.totalFat, 0.001)
    }

    @Test
    fun `totals of ingredients sums every macro`() {
        val totals = PlanLogic.totalsOf(
            listOf(
                PlanLogic.IngredientContribution(300.0, 20.0, 30.0, 10.0),
                PlanLogic.IngredientContribution(150.0, 10.0, 15.0, 5.0)
            )
        )
        assertEquals(450.0, totals.totalCalories, 0.001)
        assertEquals(30.0, totals.totalProtein, 0.001)
        assertEquals(45.0, totals.totalCarbs, 0.001)
        assertEquals(15.0, totals.totalFat, 0.001)
    }

    @Test
    fun `scaling per100g matches grams`() {
        val c = PlanLogic.scaleFromPer100g(grams = 150.0, kcal100 = 200.0, protein100 = 10.0, carbs100 = 20.0, fat100 = 8.0)
        assertEquals(300.0, c.calories, 0.001)
        assertEquals(15.0, c.protein, 0.001)
        assertEquals(30.0, c.carbs, 0.001)
        assertEquals(12.0, c.fat, 0.001)
    }

    @Test
    fun `negative grams clamp to zero`() {
        val c = PlanLogic.scaleFromPer100g(-5.0, 200.0, 10.0, 20.0, 8.0)
        assertEquals(0.0, c.calories, 0.001)
    }

    // ── Weekly plan (§17) ───────────────────────────────────────────────────

    @Test
    fun `pickIdea avoids already used titles`() {
        val used = PlanLogic.IDEAS.take(5).map { it.title }.toSet()
        val pick = PlanLogic.pickIdea(mealIndex = 0, usedTitles = used, remainingKcal = 2000.0, dailyKcalGoal = null)
        assertFalse(pick.title in used)
    }

    @Test
    fun `pickIdea falls back to full pool when all used`() {
        val all = PlanLogic.IDEAS.map { it.title }.toSet()
        val pick = PlanLogic.pickIdea(mealIndex = 2, usedTitles = all, remainingKcal = 500.0, dailyKcalGoal = 2000)
        assertTrue(PlanLogic.IDEAS.any { it.title == pick.title })
    }

    @Test
    fun `pickIdea without goal is deterministic by index`() {
        val a = PlanLogic.pickIdea(0, emptySet(), 0.0, null)
        val b = PlanLogic.pickIdea(0, emptySet(), 0.0, null)
        assertEquals(a.title, b.title)
    }

    @Test
    fun `pickIdea picks nearest to remaining budget`() {
        // Remaining 600 with goal 2000 → target min(700, 600) = 600 → closest idea is 590 (Pasta) or 620 (Beef) — 590 wins by 10.
        val pick = PlanLogic.pickIdea(mealIndex = 0, usedTitles = emptySet(), remainingKcal = 600.0, dailyKcalGoal = 2000)
        assertEquals("Pasta & broccoli", pick.title)
    }

    @Test
    fun `day within goal tolerance`() {
        assertTrue(PlanLogic.withinGoalTolerance(2000.0, 2000))
        assertTrue(PlanLogic.withinGoalTolerance(1750.0, 2000))
        assertFalse(PlanLogic.withinGoalTolerance(1600.0, 2000))
    }

    // ── Grocery categories (§18) ────────────────────────────────────────────

    @Test
    fun `categorize maps common foods`() {
        assertEquals("Produce", PlanLogic.categorize("Bananas"))
        assertEquals("Meat", PlanLogic.categorize("Chicken breast"))
        assertEquals("Dairy", PlanLogic.categorize("Greek yogurt"))
        assertEquals("Grains", PlanLogic.categorize("Rolled oats"))
        assertEquals("Other", PlanLogic.categorize("Paprika"))
    }

    @Test
    fun `frozen wins over base category`() {
        assertEquals("Frozen", PlanLogic.categorize("Frozen peas"))
        assertEquals("Frozen", PlanLogic.categorize("frozen berries"))
    }

    @Test
    fun `categorize is case insensitive`() {
        assertEquals("Dairy", PlanLogic.categorize("CHEDDAR CHEESE"))
    }
}
