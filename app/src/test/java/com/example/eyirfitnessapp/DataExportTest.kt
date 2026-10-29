package com.example.eyirfitnessapp

import com.example.eyirfitnessapp.domain.export.DataExport
import com.example.eyirfitnessapp.domain.export.RecipeIngredient
import com.example.eyirfitnessapp.domain.model.ActivityLevel
import com.example.eyirfitnessapp.domain.model.ExerciseLog
import com.example.eyirfitnessapp.domain.model.FoodLog
import com.example.eyirfitnessapp.domain.model.GoalType
import com.example.eyirfitnessapp.domain.model.MealType
import com.example.eyirfitnessapp.domain.model.Sex
import com.example.eyirfitnessapp.domain.model.ThemeMode
import com.example.eyirfitnessapp.domain.model.UserProfile
import com.example.eyirfitnessapp.domain.model.WaterLog
import com.example.eyirfitnessapp.domain.model.WeightLog
import com.example.eyirfitnessapp.domain.model.WeekStartDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec §22 Account → Export data. */
class DataExportTest {

    private fun sampleProfile(name: String = "Ada") = UserProfile(
        name = name,
        email = "ada@example.com",
        sex = Sex.FEMALE,
        age = 34,
        heightCm = 168.0,
        currentWeightKg = 62.5,
        goalWeightKg = 60.0,
        activityLevel = ActivityLevel.LIGHT,
        goalType = GoalType.LOSE,
        weeklyGoalKg = 0.25,
        waterGoalMl = 2200,
        exerciseCaloriesAddToBudget = false,
        dailyCalorieGoal = 1800,
        themeMode = ThemeMode.DARK,
        weekStartDay = WeekStartDay.SUNDAY,
        onboardingComplete = true
    )

    private fun foodLog(name: String = "Oats") = FoodLog(
        id = 7,
        foodId = 11,
        name = name,
        brand = "Brand \"X\"",
        mealType = MealType.BREAKFAST,
        quantity = 1.5,
        servingSize = 100.0,
        servingUnit = "g",
        calories = 210.0,
        protein = 12.0,
        carbs = 30.0,
        fat = 4.0,
        date = "2026-09-29",
        loggedAt = 1_700_000_000_000L,
        synced = false
    )

    private fun build(
        profile: UserProfile? = sampleProfile(),
        foodLogs: List<FoodLog> = listOf(foodLog()),
        exerciseLogs: List<ExerciseLog> = emptyList(),
        weightLogs: List<WeightLog> = emptyList(),
        waterLogs: List<WaterLog> = emptyList(),
        fasting: List<com.example.eyirfitnessapp.domain.model.FastingSession> = emptyList(),
        measurements: List<com.example.eyirfitnessapp.domain.model.BodyMeasurement> = emptyList(),
        habits: List<com.example.eyirfitnessapp.domain.model.Habit> = emptyList(),
        macroGoals: com.example.eyirfitnessapp.domain.model.MacroGoals? = null,
        recipes: List<com.example.eyirfitnessapp.domain.model.Recipe> = emptyList(),
        recipeIngredients: List<RecipeIngredient> = emptyList(),
        mealPlan: List<com.example.eyirfitnessapp.domain.model.PlannedMeal> = emptyList(),
        groceryItems: List<com.example.eyirfitnessapp.domain.model.GroceryItem> = emptyList(),
        routines: List<com.example.eyirfitnessapp.domain.model.WorkoutRoutine> = emptyList()
    ) = DataExport.build(
        profile = profile,
        foodLogs = foodLogs,
        exerciseLogs = exerciseLogs,
        weightLogs = weightLogs,
        waterLogs = waterLogs,
        fastingSessions = fasting,
        exportedAtMillis = 1_700_000_000_000L,
        exportedAtIso = DataExport.isoTimestamp(1_700_000_000_000L),
        bodyMeasurements = measurements,
        habits = habits,
        macroGoals = macroGoals,
        recipes = recipes,
        recipeIngredients = recipeIngredients,
        mealPlan = mealPlan,
        groceryItems = groceryItems,
        routines = routines
    )

    @Test
    fun `export carries header, format version and timestamp`() {
        val json = build()
        assertTrue(json.contains("\"format\": \"eyirfitness-export\""))
        assertTrue(json.contains("\"formatVersion\": 1"))
        assertTrue(json.contains("\"exportedAtMillis\": 1700000000000"))
        assertTrue(json.contains("\"exportedAt\": \"2023-11-14T"))
    }

    @Test
    fun `export contains every profile preference`() {
        val json = build()
        assertTrue(json.contains("\"name\": \"Ada\""))
        assertTrue(json.contains("\"email\": \"ada@example.com\""))
        assertTrue(json.contains("\"dailyCalorieGoal\": 1800"))
        assertTrue(json.contains("\"themeMode\": \"DARK\""))
        assertTrue(json.contains("\"weekStartDay\": \"SUNDAY\""))
        assertTrue(json.contains("\"weightUnit\": \"KG\""))
        assertTrue(json.contains("\"notifMeals\": true"))
        assertTrue(json.contains("\"notifWeeklyReport\": true"))
        assertTrue(json.contains("\"notifHabits\": true"))
        assertTrue(json.contains("\"onboardingComplete\": true"))
    }

    @Test
    fun `export contains food log rows and escapes quotes`() {
        val json = build()
        assertTrue(json.contains("\"mealType\": \"BREAKFAST\""))
        assertTrue(json.contains("\"date\": \"2026-09-29\""))
        assertTrue(json.contains("\"name\": \"Oats\""))
        assertTrue(json.contains("\"brand\": \"Brand \\\"X\\\"\""))
        assertTrue(json.contains("\"calories\": 210.0"))
    }

    @Test
    fun `empty collections export as empty arrays`() {
        val json = build(profile = null, foodLogs = emptyList())
        assertTrue(json.contains("\"profile\": null"))
        assertTrue(json.contains("\"foodLogs\": []"))
        assertTrue(json.contains("\"weightLogs\": []"))
        assertTrue(json.contains("\"fastingSessions\": []"))
        assertTrue(json.contains("\"exerciseLogs\": []"))
        assertTrue(json.contains("\"waterLogs\": []"))
    }

    @Test
    fun `exported json is brace balanced`() {
        val json = build()
        assertEquals(json.count { it == '{' }, json.count { it == '}' })
        assertEquals(json.count { it == '[' }, json.count { it == ']' })
        assertTrue(json.trimEnd().endsWith("}"))
    }

    @Test
    fun `quote escapes control characters`() {
        assertEquals("\"plain\"", DataExport.quote("plain"))
        assertEquals("\"a\\\"b\"", DataExport.quote("a\"b"))
        assertEquals("\"c\\\\d\"", DataExport.quote("c\\d"))
        assertEquals("\"line\\nbreak\"", DataExport.quote("line\nbreak"))
        assertEquals("\"tab\\there\"", DataExport.quote("tab\there"))
        assertEquals("\"\\u0001\"", DataExport.quote("\u0001"))
        assertEquals("null", DataExport.quote(null))
    }

    @Test
    fun `isoTimestamp is UTC with a trailing Z`() {
        val stamp = DataExport.isoTimestamp(0L)
        assertEquals("1970-01-01T00:00:00Z", stamp)
        assertTrue(stamp.endsWith("Z"))
        assertFalse(stamp.contains(" "))
    }

    @Test
    fun `measurements and habits export with completions`() {
        val json = build(
            foodLogs = emptyList(),
            measurements = listOf(
                com.example.eyirfitnessapp.domain.model.BodyMeasurement(
                    id = 3,
                    part = com.example.eyirfitnessapp.domain.model.MeasurementPart.WAIST,
                    centimeters = 82.5,
                    date = "2026-09-28",
                    loggedAt = 9L
                )
            ),
            habits = listOf(
                com.example.eyirfitnessapp.domain.model.Habit(
                    id = 4,
                    key = "WATER",
                    label = "Water",
                    icon = "water_drop",
                    completions = setOf("2026-09-27", "2026-09-28")
                )
            )
        )
        assertTrue(json.contains("\"bodyMeasurements\": ["))
        assertTrue(json.contains("\"part\": \"WAIST\""))
        assertTrue(json.contains("\"centimeters\": 82.5"))
        assertTrue(json.contains("\"habits\": ["))
        assertTrue(json.contains("\"key\": \"WATER\""))
        assertTrue(json.contains("\"2026-09-27\""))
        // Default (no measurements/habits) exports as empty arrays.
        assertTrue(build(foodLogs = emptyList()).contains("\"bodyMeasurements\": []"))
    }

    @Test
    fun `exercise, weight and water rows are present when provided`() {
        val json = build(
            foodLogs = emptyList(),
            exerciseLogs = listOf(
                ExerciseLog(
                    id = 1, exerciseId = 2, name = "Run", category = "Cardio",
                    durationMinutes = 30.0, sets = null, reps = null, weightKg = null,
                    caloriesBurned = 250.0, date = "2026-09-29", loggedAt = 5L
                )
            ),
            weightLogs = listOf(WeightLog(id = 1, weightKg = 62.5, date = "2026-09-29", loggedAt = 6L)),
            waterLogs = listOf(WaterLog(id = 1, amountMl = 250, date = "2026-09-29", loggedAt = 7L))
        )
        assertTrue(json.contains("\"name\": \"Run\""))
        assertTrue(json.contains("\"caloriesBurned\": 250.0"))
        assertTrue(json.contains("\"weightKg\": 62.5"))
        assertTrue(json.contains("\"amountMl\": 250"))
    }

    @Test
    fun `plans, recipes, grocery, macro goals and routines export too`() {
        val json = build(
            foodLogs = emptyList(),
            macroGoals = com.example.eyirfitnessapp.domain.model.MacroGoals(
                mode = com.example.eyirfitnessapp.domain.model.MacroTargetMode.GRAMS,
                proteinGrams = 150,
                carbsGrams = 200,
                fatGrams = 65
            ),
            recipes = listOf(
                com.example.eyirfitnessapp.domain.model.Recipe(
                    id = 1, name = "Chili", servings = 4, totalCalories = 1200.0
                )
            ),
            recipeIngredients = listOf(
                RecipeIngredient(
                    recipeId = 1, name = "Beans", quantity = 400.0, unit = "g",
                    calories = 440.0, protein = 30.0, carbs = 76.0, fat = 2.0
                )
            ),
            mealPlan = listOf(
                com.example.eyirfitnessapp.domain.model.PlannedMeal(
                    date = "2026-09-29", mealType = MealType.LUNCH,
                    title = "Chili", recipeId = 1, plannedKcal = 300.0, completed = true
                )
            ),
            groceryItems = listOf(
                com.example.eyirfitnessapp.domain.model.GroceryItem(
                    name = "Beans", quantity = 1.0, unit = "can", category = "Pantry"
                )
            ),
            routines = listOf(
                com.example.eyirfitnessapp.domain.model.WorkoutRoutine(
                    id = 1,
                    name = "Push Day",
                    items = listOf(
                        com.example.eyirfitnessapp.domain.model.RoutineItem(
                            name = "Bench Press", category = "Strength", sets = 3, reps = 10
                        )
                    )
                )
            )
        )
        assertTrue(json.contains("\"macroGoals\": {"))
        assertTrue(json.contains("\"proteinGrams\": 150"))
        assertTrue(json.contains("\"recipes\": ["))
        assertTrue(json.contains("\"recipeIngredients\": ["))
        assertTrue(json.contains("\"name\": \"Beans\""))
        assertTrue(json.contains("\"mealPlan\": ["))
        assertTrue(json.contains("\"title\": \"Chili\""))
        assertTrue(json.contains("\"completed\": true"))
        assertTrue(json.contains("\"groceryItems\": ["))
        assertTrue(json.contains("\"category\": \"Pantry\""))
        assertTrue(json.contains("\"routines\": ["))
        assertTrue(json.contains("\"name\": \"Push Day\""))
        assertTrue(json.contains("\"sets\": 3"))
        assertEquals(json.count { it == '{' }, json.count { it == '}' })
        assertEquals(json.count { it == '[' }, json.count { it == ']' })
    }

    @Test
    fun `optional plan sections default to empty arrays`() {
        val json = build(profile = null, foodLogs = emptyList())
        assertTrue(json.contains("\"macroGoals\": null"))
        assertTrue(json.contains("\"recipes\": []"))
        assertTrue(json.contains("\"recipeIngredients\": []"))
        assertTrue(json.contains("\"mealPlan\": []"))
        assertTrue(json.contains("\"groceryItems\": []"))
        assertTrue(json.contains("\"routines\": []"))
    }
}
