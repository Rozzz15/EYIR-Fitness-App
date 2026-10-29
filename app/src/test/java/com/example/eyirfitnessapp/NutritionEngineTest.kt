package com.example.eyirfitnessapp

import com.example.eyirfitnessapp.domain.NutritionEngine
import com.example.eyirfitnessapp.domain.model.ActivityLevel
import com.example.eyirfitnessapp.domain.model.ExerciseLog
import com.example.eyirfitnessapp.domain.model.Food
import com.example.eyirfitnessapp.domain.model.FoodLog
import com.example.eyirfitnessapp.domain.model.GoalType
import com.example.eyirfitnessapp.domain.model.MealType
import com.example.eyirfitnessapp.domain.model.Sex
import com.example.eyirfitnessapp.domain.model.UserProfile
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionEngineTest {

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
    fun `bmr matches Mifflin-St Jeor for male`() {
        // 10*80 + 6.25*180 - 5*30 + 5 = 800 + 1125 - 150 + 5 = 1780
        assertEquals(1780.0, NutritionEngine.bmr(profile), 0.01)
    }

    @Test
    fun `bmr for female subtracts 161`() {
        val female = profile.copy(sex = Sex.FEMALE)
        // 800 + 1125 - 150 - 161 = 1614
        assertEquals(1614.0, NutritionEngine.bmr(female), 0.01)
    }

    @Test
    fun `tdee multiplies bmr by activity factor`() {
        assertEquals(1780.0 * 1.55, NutritionEngine.tdee(profile), 0.01)
    }

    @Test
    fun `maintain goal equals tdee`() {
        val goal = NutritionEngine.dailyCalorieGoal(profile)
        assertEquals(NutritionEngine.tdee(profile).toInt(), goal)
    }

    @Test
    fun `lose goal reduces calories by weekly deficit`() {
        val losing = profile.copy(goalType = GoalType.LOSE, weeklyGoalKg = 0.5)
        val expected = NutritionEngine.tdee(losing) - 0.5 * 7700.0 / 7.0
        assertEquals(expected.toInt(), NutritionEngine.dailyCalorieGoal(losing))
    }

    @Test
    fun `goal never drops below safety floor`() {
        val aggressive = profile.copy(goalType = GoalType.LOSE, weeklyGoalKg = 5.0)
        assertEquals(1200, NutritionEngine.dailyCalorieGoal(aggressive))
    }

    @Test
    fun `macro goals follow 30-40-30 split`() {
        val goals = NutritionEngine.macroGoals(profile)
        val kcal = goals.dailyCalorieGoal.toDouble()
        assertEquals((kcal * 0.30 / 4.0).roundToInt(), goals.proteinGrams)
        assertEquals((kcal * 0.40 / 4.0).roundToInt(), goals.carbsGrams)
        assertEquals((kcal * 0.30 / 9.0).roundToInt(), goals.fatGrams)
    }

    @Test
    fun `scaling food doubles nutrition at quantity 2x serving`() {
        val food = Food(name = "Egg", servingSize = 50.0, servingUnit = "g", calories = 72.0, protein = 6.3, carbs = 0.4, fat = 4.8)
        val scaled = NutritionEngine.scale(food, 100.0)
        assertEquals(144.0, scaled.calories, 0.01)
        assertEquals(12.6, scaled.protein, 0.01)
    }

    @Test
    fun `calories remaining is goal plus burned minus eaten`() {
        val summary = NutritionEngine.summarize(
            date = "2026-01-01",
            foodLogs = listOf(log(500.0), log(300.0)),
            exerciseLogs = listOf(burn(200.0)),
            waterLogs = emptyList(),
            goal = NutritionEngine.macroGoals(profile)
        )
        val goal = NutritionEngine.dailyCalorieGoal(profile)
        assertEquals(goal + 200 - 800, NutritionEngine.caloriesRemaining(summary))
    }

    @Test
    fun `calories remaining ignores burn when profile opts out`() {
        val summary = NutritionEngine.summarize(
            date = "2026-01-01",
            foodLogs = listOf(log(500.0), log(300.0)),
            exerciseLogs = listOf(burn(200.0)),
            waterLogs = emptyList(),
            goal = NutritionEngine.macroGoals(profile)
        )
        val goal = NutritionEngine.dailyCalorieGoal(profile)
        assertEquals(goal - 800, NutritionEngine.caloriesRemaining(summary, false))
        assertEquals(goal, NutritionEngine.budget(summary, false).toInt())
        assertEquals(goal + 200, NutritionEngine.budget(summary, true).toInt())
    }

    @Test
    fun `scale falls back to one unit when serving size is zero`() {
        val food = Food(name = "Broken", servingSize = 0.0, servingUnit = "serving", calories = 100.0, protein = 5.0, carbs = 0.0, fat = 0.0)
        assertEquals(200.0, NutritionEngine.scale(food, 2.0).calories, 0.01)
    }

    @Test
    fun `weight based food uses serving size as grams per serving`() {
        val food = Food(name = "Rice", servingSize = 100.0, servingUnit = "g", calories = 130.0, protein = 2.7, carbs = 28.0, fat = 0.3)
        assertTrue(NutritionEngine.isWeightBased(food))
        assertEquals(100.0, NutritionEngine.gramsPerServing(food)!!, 0.01)
    }

    @Test
    fun `count based food needs a saved gram weight`() {
        val unknown = Food(name = "Banana", servingSize = 1.0, servingUnit = "medium", calories = 105.0, protein = 1.3, carbs = 27.0, fat = 0.4)
        assertEquals(null, NutritionEngine.gramsPerServing(unknown))

        val known = unknown.copy(gramsPerServing = 118.0)
        assertEquals(118.0, NutritionEngine.gramsPerServing(known)!!, 0.01)
    }

    @Test
    fun `fromGrams converts a scale reading to the native unit`() {
        val banana = Food(
            name = "Banana", servingSize = 1.0, servingUnit = "medium",
            calories = 105.0, protein = 1.3, carbs = 27.0, fat = 0.4,
            gramsPerServing = 118.0
        )
        // 177 g ÷ 118 g/serving × 1 medium/serving = 1.5 medium
        assertEquals(1.5, NutritionEngine.fromGrams(banana, 177.0)!!, 0.01)
        assertEquals(177.0, NutritionEngine.toGrams(banana, 1.5)!!, 0.01)
        // …and the scale reading drives the normal nutrition math.
        assertEquals(157.5, NutritionEngine.scale(banana, NutritionEngine.fromGrams(banana, 177.0)!!).calories, 0.01)
    }

    @Test
    fun `fromGrams is a no-op conversion for weight based foods`() {
        val chicken = Food(name = "Chicken", servingSize = 100.0, servingUnit = "g", calories = 165.0, protein = 31.0, carbs = 0.0, fat = 3.6)
        assertEquals(150.0, NutritionEngine.fromGrams(chicken, 150.0)!!, 0.01)
        assertEquals(150.0, NutritionEngine.toGrams(chicken, 150.0)!!, 0.01)
        assertEquals(247.5, NutritionEngine.scale(chicken, NutritionEngine.fromGrams(chicken, 150.0)!!).calories, 0.01)
    }

    @Test
    fun `gram helpers refuse to convert when the weight is unknown`() {
        val banana = Food(name = "Banana", servingSize = 1.0, servingUnit = "medium", calories = 105.0, protein = 1.3, carbs = 27.0, fat = 0.4)
        assertEquals(null, NutritionEngine.fromGrams(banana, 118.0))
        assertEquals(null, NutritionEngine.toGrams(banana, 1.0))
    }

    @Test
    fun `malformed date never throws`() {
        val moved = com.example.eyirfitnessapp.util.Dates.addDays("not-a-date", 1)
        assertEquals(10, moved.length)
    }

    @Test
    fun `summary aggregates food water and exercise`() {
        val summary = NutritionEngine.summarize(
            date = "2026-01-01",
            foodLogs = listOf(log(500.0, protein = 30.0), log(300.0, protein = 20.0)),
            exerciseLogs = listOf(burn(200.0)),
            waterLogs = emptyList(),
            goal = null
        )
        assertEquals(800.0, summary.caloriesConsumed, 0.01)
        assertEquals(50.0, summary.protein, 0.01)
        assertEquals(200.0, summary.caloriesBurned, 0.01)
    }

    @Test
    fun `empty summary is zeroed`() {
        val summary = NutritionEngine.summarize("2026-01-01", emptyList(), emptyList(), emptyList(), null)
        assertEquals(0.0, summary.caloriesConsumed, 0.0)
        assertTrue(summary.waterMl == 0)
    }

    private fun log(kcal: Double, protein: Double = 10.0) = FoodLog(
        foodId = null, name = "test", mealType = MealType.LUNCH,
        quantity = 1.0, servingSize = 1.0, servingUnit = "serving",
        calories = kcal, protein = protein, carbs = 10.0, fat = 5.0,
        date = "2026-01-01", loggedAt = 0L
    )

    private fun burn(kcal: Double) = ExerciseLog(
        exerciseId = null, name = "run", category = "Running",
        durationMinutes = 30.0, sets = null, reps = null, weightKg = null,
        caloriesBurned = kcal, date = "2026-01-01", loggedAt = 0L
    )
}
