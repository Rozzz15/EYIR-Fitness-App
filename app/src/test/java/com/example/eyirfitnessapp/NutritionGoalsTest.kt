package com.example.eyirfitnessapp

import com.example.eyirfitnessapp.domain.NutritionEngine
import com.example.eyirfitnessapp.domain.model.ActivityLevel
import com.example.eyirfitnessapp.domain.model.GoalType
import com.example.eyirfitnessapp.domain.model.MacroGoals
import com.example.eyirfitnessapp.domain.model.MacroTargetMode
import com.example.eyirfitnessapp.domain.model.Sex
import com.example.eyirfitnessapp.domain.model.UserProfile
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec §9 (percentage vs grams) and §21 (goal calculation) — Goals & Nutrition batch. */
class NutritionGoalsTest {

    private val profile = UserProfile(
        name = "Test",
        sex = Sex.MALE,
        age = 30,
        heightCm = 180.0,
        currentWeightKg = 80.0,
        goalWeightKg = 75.0,
        activityLevel = ActivityLevel.MODERATE,
        goalType = GoalType.MAINTAIN
    )

    @Test
    fun `manual calorie goal overrides the BMR estimate`() {
        val estimate = NutritionEngine.computedCalorieGoal(profile)
        val manual = profile.copy(dailyCalorieGoal = 1500)
        assertEquals(1500, NutritionEngine.dailyCalorieGoal(manual))
        assertEquals(estimate, NutritionEngine.computedCalorieGoal(manual))
    }

    @Test
    fun `a zero or negative override falls back to the estimate`() {
        assertEquals(
            NutritionEngine.computedCalorieGoal(profile),
            NutritionEngine.dailyCalorieGoal(profile.copy(dailyCalorieGoal = 0))
        )
        assertEquals(
            NutritionEngine.computedCalorieGoal(profile),
            NutritionEngine.dailyCalorieGoal(profile.copy(dailyCalorieGoal = -500))
        )
    }

    @Test
    fun `percentage mode converts calories to grams`() {
        val goals = NutritionEngine.macroGoals(
            profile.copy(dailyCalorieGoal = 2000),
            MacroGoals(
                mode = MacroTargetMode.PERCENT,
                proteinPercent = 30,
                carbsPercent = 40,
                fatPercent = 30
            )
        )
        assertEquals(2000, goals.dailyCalorieGoal)
        assertEquals(150, goals.proteinGrams)
        assertEquals(200, goals.carbsGrams)
        assertEquals(67, goals.fatGrams) // 2000 * 0.30 / 9
    }

    @Test
    fun `grams mode passes stored grams through untouched and derives the calorie total`() {
        val goals = NutritionEngine.macroGoals(
            profile.copy(dailyCalorieGoal = 1800),
            MacroGoals(
                mode = MacroTargetMode.GRAMS,
                proteinGrams = 150,
                carbsGrams = 200,
                fatGrams = 65
            )
        )
        // The Goals screen promises "the calorie total follows from them": 150x4 + 200x4 + 65x9.
        assertEquals(150 * 4 + 200 * 4 + 65 * 9, goals.dailyCalorieGoal)
        assertEquals(150, goals.proteinGrams)
        assertEquals(200, goals.carbsGrams)
        assertEquals(65, goals.fatGrams)
    }

    @Test
    fun `grams mode ignores the TDEE-derived calorie goal`() {
        val goals = NutritionEngine.macroGoals(
            profile.copy(dailyCalorieGoal = 1800),
            MacroGoals(mode = MacroTargetMode.GRAMS, proteinGrams = 0, carbsGrams = 0, fatGrams = 0)
        )
        // No grams set yet: fall back to the profile calorie goal rather than 0.
        assertEquals(NutritionEngine.dailyCalorieGoal(profile.copy(dailyCalorieGoal = 1800)), goals.dailyCalorieGoal)
    }

    @Test
    fun `a split that does not sum to 100 is normalised, not trusted`() {
        val goals = NutritionEngine.macroGoals(
            profile.copy(dailyCalorieGoal = 1800),
            MacroGoals(mode = MacroTargetMode.PERCENT, proteinPercent = 50, carbsPercent = 50, fatPercent = 50)
        )
        // Equal thirds of 1800 kcal → 150g protein, 150g carbs, 67g fat.
        assertEquals(150, goals.proteinGrams)
        assertEquals(150, goals.carbsGrams)
        assertEquals(67, goals.fatGrams)
    }

    @Test
    fun `stored grams keep their calorie meaning within rounding`() {
        val goals = NutritionEngine.macroGoals(
            profile.copy(dailyCalorieGoal = 2000),
            MacroGoals(mode = MacroTargetMode.PERCENT, proteinPercent = 30, carbsPercent = 40, fatPercent = 30)
        )
        val fromGrams = goals.proteinGrams * 4 + goals.carbsGrams * 4 + goals.fatGrams * 9
        assertTrue("macros should land near the calorie goal", Math.abs(fromGrams - 2000) <= 60)
    }

    @Test
    fun `no stored goal falls back to the 30-40-30 default`() {
        val goals = NutritionEngine.macroGoals(profile, null)
        val kcal = goals.dailyCalorieGoal.toDouble()
        assertEquals((kcal * 0.30 / 4.0).roundToInt(), goals.proteinGrams)
        assertEquals((kcal * 0.40 / 4.0).roundToInt(), goals.carbsGrams)
        assertEquals((kcal * 0.30 / 9.0).roundToInt(), goals.fatGrams)
    }

    @Test
    fun `nutrition and habit goals do not shift energy balance`() {
        val maintain = NutritionEngine.computedCalorieGoal(profile)
        assertEquals(maintain, NutritionEngine.computedCalorieGoal(profile.copy(goalType = GoalType.IMPROVE_NUTRITION)))
        assertEquals(maintain, NutritionEngine.computedCalorieGoal(profile.copy(goalType = GoalType.BUILD_HABITS)))
        assertEquals(0.0, NutritionEngine.dailyAdjustment(profile.copy(goalType = GoalType.IMPROVE_NUTRITION)), 0.001)
        assertEquals(0.0, NutritionEngine.dailyAdjustment(profile.copy(goalType = GoalType.BUILD_HABITS)), 0.001)
    }

    @Test
    fun `every goal type still produces a positive target above the floor`() {
        GoalType.entries.forEach { type ->
            val goal = NutritionEngine.dailyCalorieGoal(profile.copy(goalType = type))
            assertTrue("$type produced $goal", goal >= 1200)
        }
    }
}
