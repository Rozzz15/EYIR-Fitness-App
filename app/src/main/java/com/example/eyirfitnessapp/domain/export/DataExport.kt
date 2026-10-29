package com.example.eyirfitnessapp.domain.export

import com.example.eyirfitnessapp.domain.model.BodyMeasurement
import com.example.eyirfitnessapp.domain.model.ExerciseLog
import com.example.eyirfitnessapp.domain.model.FastingSession
import com.example.eyirfitnessapp.domain.model.FoodLog
import com.example.eyirfitnessapp.domain.model.GroceryItem
import com.example.eyirfitnessapp.domain.model.Habit
import com.example.eyirfitnessapp.domain.model.MacroGoals
import com.example.eyirfitnessapp.domain.model.PlannedMeal
import com.example.eyirfitnessapp.domain.model.Recipe
import com.example.eyirfitnessapp.domain.model.UserProfile
import com.example.eyirfitnessapp.domain.model.WaterLog
import com.example.eyirfitnessapp.domain.model.WeightLog
import com.example.eyirfitnessapp.domain.model.WorkoutRoutine

/** One recipe ingredient row (spec §16) as exported — the domain layer has no item model. */
data class RecipeIngredient(
    val recipeId: Long,
    val name: String,
    val quantity: Double,
    val unit: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

/**
 * Spec §22 Account → Export data: a JSON snapshot of everything the app keeps
 * on this device. Hand-rolled (no serializer dependency) so it stays pure and
 * unit-testable on the JVM (§34 rule 2).
 *
 * Every value is written exactly as stored: weights in kg, energy in kcal,
 * dates as yyyy-MM-dd.
 */
object DataExport {
    const val FORMAT = "eyirfitness-export"
    const val FORMAT_VERSION = 1

    /** Whole export as pretty-printed JSON with a trailing newline. */
    fun build(
        profile: UserProfile?,
        foodLogs: List<FoodLog>,
        exerciseLogs: List<ExerciseLog>,
        weightLogs: List<WeightLog>,
        waterLogs: List<WaterLog>,
        fastingSessions: List<FastingSession>,
        exportedAtMillis: Long,
        exportedAtIso: String,
        bodyMeasurements: List<BodyMeasurement> = emptyList(),
        habits: List<Habit> = emptyList(),
        macroGoals: MacroGoals? = null,
        recipes: List<Recipe> = emptyList(),
        recipeIngredients: List<RecipeIngredient> = emptyList(),
        mealPlan: List<PlannedMeal> = emptyList(),
        groceryItems: List<GroceryItem> = emptyList(),
        routines: List<WorkoutRoutine> = emptyList()
    ): String = buildString {
        append("{\n")
        append("  \"format\": ${quote(FORMAT)},\n")
        append("  \"formatVersion\": $FORMAT_VERSION,\n")
        append("  \"exportedAt\": ${quote(exportedAtIso)},\n")
        append("  \"exportedAtMillis\": $exportedAtMillis,\n")
        append("  \"note\": ${quote("Stored locally on one device. Weights in kg, energy in kcal, dates as yyyy-MM-dd.")},\n")
        append("  \"profile\": ${profileJson(profile)},\n")
        append("  \"macroGoals\": ${macroGoalsJson(macroGoals)},\n")
        append("  \"foodLogs\": ${arrayJson(foodLogs, ::foodLogJson)},\n")
        append("  \"exerciseLogs\": ${arrayJson(exerciseLogs, ::exerciseLogJson)},\n")
        append("  \"weightLogs\": ${arrayJson(weightLogs, ::weightLogJson)},\n")
        append("  \"waterLogs\": ${arrayJson(waterLogs, ::waterLogJson)},\n")
        append("  \"fastingSessions\": ${arrayJson(fastingSessions, ::fastingJson)},\n")
        append("  \"bodyMeasurements\": ${arrayJson(bodyMeasurements, ::measurementJson)},\n")
        append("  \"habits\": ${arrayJson(habits, ::habitJson)},\n")
        append("  \"recipes\": ${arrayJson(recipes, ::recipeJson)},\n")
        append("  \"recipeIngredients\": ${arrayJson(recipeIngredients, ::ingredientJson)},\n")
        append("  \"mealPlan\": ${arrayJson(mealPlan, ::plannedMealJson)},\n")
        append("  \"groceryItems\": ${arrayJson(groceryItems, ::groceryJson)},\n")
        append("  \"routines\": ${arrayJson(routines, ::routineJson)}\n")
        append("}\n")
    }

    /** ISO-8601 UTC timestamp for `exportedAt` (locale-independent). */
    fun isoTimestamp(millis: Long): String =
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date(millis))

    /** A JSON string literal (quotes included), or `null`. */
    fun quote(value: String?): String {        if (value == null) return "null"
        return buildString {
            append('"')
            value.forEach { c ->
                when (c) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> if (c.code < 0x20) append("\\u%04x".format(c.code)) else append(c)
                }
            }
            append('"')
        }
    }

    private fun <T> arrayJson(items: List<T>, item: (T) -> String): String =
        if (items.isEmpty()) "[]"
        else items.joinToString(separator = ",\n    ", prefix = "[\n    ", postfix = "\n  ]") { item(it) }

    private fun profileJson(p: UserProfile?): String {
        if (p == null) return "null"
        return buildString {
            append("{\n")
            append("    \"name\": ${quote(p.name)},\n")
            append("    \"email\": ${quote(p.email)},\n")
            append("    \"sex\": ${quote(p.sex.name)},\n")
            append("    \"age\": ${p.age},\n")
            append("    \"heightCm\": ${p.heightCm},\n")
            append("    \"currentWeightKg\": ${p.currentWeightKg},\n")
            append("    \"goalWeightKg\": ${p.goalWeightKg},\n")
            append("    \"activityLevel\": ${quote(p.activityLevel.name)},\n")
            append("    \"goalType\": ${quote(p.goalType.name)},\n")
            append("    \"weeklyGoalKg\": ${p.weeklyGoalKg},\n")
            append("    \"weightUnit\": ${quote(p.weightUnit.name)},\n")
            append("    \"waterGoalMl\": ${p.waterGoalMl},\n")
            append("    \"exerciseCaloriesAddToBudget\": ${p.exerciseCaloriesAddToBudget},\n")
            append("    \"dailyCalorieGoal\": ${p.dailyCalorieGoal ?: "null"},\n")
            append("    \"themeMode\": ${quote(p.themeMode.name)},\n")
            append("    \"weekStartDay\": ${quote(p.weekStartDay.name)},\n")
            append("    \"notifMeals\": ${p.notifMeals},\n")
            append("    \"notifWater\": ${p.notifWater},\n")
            append("    \"notifWeighIn\": ${p.notifWeighIn},\n")
            append("    \"notifFasting\": ${p.notifFasting},\n")
            append("    \"notifWeeklyReport\": ${p.notifWeeklyReport},\n")
            append("    \"notifHabits\": ${p.notifHabits},\n")
            append("    \"onboardingComplete\": ${p.onboardingComplete}\n")
            append("  }")
        }
    }

    private fun foodLogJson(l: FoodLog) = buildString {
        append("{")
        append("\"id\": ${l.id}, \"foodId\": ${l.foodId ?: "null"}, \"name\": ${quote(l.name)}, ")
        append("\"brand\": ${quote(l.brand)}, \"mealType\": ${quote(l.mealType.name)}, ")
        append("\"quantity\": ${l.quantity}, \"servingSize\": ${l.servingSize}, \"servingUnit\": ${quote(l.servingUnit)}, ")
        append("\"calories\": ${l.calories}, \"protein\": ${l.protein}, \"carbs\": ${l.carbs}, \"fat\": ${l.fat}, ")
        append("\"date\": ${quote(l.date)}, \"loggedAt\": ${l.loggedAt}")
        append("}")
    }

    private fun exerciseLogJson(l: ExerciseLog) = buildString {
        append("{")
        append("\"id\": ${l.id}, \"exerciseId\": ${l.exerciseId ?: "null"}, \"name\": ${quote(l.name)}, ")
        append("\"category\": ${quote(l.category)}, \"durationMinutes\": ${l.durationMinutes ?: "null"}, ")
        append("\"sets\": ${l.sets ?: "null"}, \"reps\": ${l.reps ?: "null"}, \"weightKg\": ${l.weightKg ?: "null"}, ")
        append("\"caloriesBurned\": ${l.caloriesBurned}, \"date\": ${quote(l.date)}, \"loggedAt\": ${l.loggedAt}")
        append("}")
    }

    private fun weightLogJson(l: WeightLog) = buildString {
        append("{")
        append("\"id\": ${l.id}, \"weightKg\": ${l.weightKg}, \"note\": ${quote(l.note)}, ")
        append("\"date\": ${quote(l.date)}, \"loggedAt\": ${l.loggedAt}")
        append("}")
    }

    private fun waterLogJson(l: WaterLog) = buildString {
        append("{")
        append("\"id\": ${l.id}, \"amountMl\": ${l.amountMl}, \"date\": ${quote(l.date)}, \"loggedAt\": ${l.loggedAt}")
        append("}")
    }

    private fun fastingJson(s: FastingSession) = buildString {
        append("{")
        append("\"id\": ${s.id}, \"startTime\": ${s.startTime}, \"endTime\": ${s.endTime ?: "null"}, ")
        append("\"targetHours\": ${s.targetHours}, \"status\": ${quote(s.status)}")
        append("}")
    }

    private fun measurementJson(m: BodyMeasurement) = buildString {
        append("{")
        append("\"part\": ${quote(m.part.name)}, \"centimeters\": ${m.centimeters}, ")
        append("\"date\": ${quote(m.date)}, \"loggedAt\": ${m.loggedAt}")
        append("}")
    }

    private fun habitJson(h: Habit) = buildString {
        append("{")
        append("\"key\": ${quote(h.key)}, \"label\": ${quote(h.label)}, ")
        append("\"completions\": ${h.completions.sorted().joinToString(separator = ",", prefix = "[", postfix = "]") { quote(it) }}")
        append("}")
    }

    private fun macroGoalsJson(g: MacroGoals?): String {
        if (g == null) return "null"
        return buildString {
            append("{")
            append("\"mode\": ${quote(g.mode.name)}, ")
            append("\"proteinGrams\": ${g.proteinGrams}, \"carbsGrams\": ${g.carbsGrams}, \"fatGrams\": ${g.fatGrams}, ")
            append("\"proteinPercent\": ${g.proteinPercent}, \"carbsPercent\": ${g.carbsPercent}, \"fatPercent\": ${g.fatPercent}")
            append("}")
        }
    }

    private fun recipeJson(r: Recipe) = buildString {
        append("{")
        append("\"id\": ${r.id}, \"name\": ${quote(r.name)}, \"description\": ${quote(r.description)}, ")
        append("\"servings\": ${r.servings}, \"totalCalories\": ${r.totalCalories}, ")
        append("\"totalProtein\": ${r.totalProtein}, \"totalCarbs\": ${r.totalCarbs}, \"totalFat\": ${r.totalFat}")
        append("}")
    }

    private fun ingredientJson(i: RecipeIngredient) = buildString {
        append("{")
        append("\"recipeId\": ${i.recipeId}, \"name\": ${quote(i.name)}, ")
        append("\"quantity\": ${i.quantity}, \"unit\": ${quote(i.unit)}, ")
        append("\"calories\": ${i.calories}, \"protein\": ${i.protein}, \"carbs\": ${i.carbs}, \"fat\": ${i.fat}")
        append("}")
    }

    private fun plannedMealJson(m: PlannedMeal) = buildString {
        append("{")
        append("\"date\": ${quote(m.date)}, \"mealType\": ${quote(m.mealType.name)}, \"title\": ${quote(m.title)}, ")
        append("\"recipeId\": ${m.recipeId ?: "null"}, \"plannedKcal\": ${m.plannedKcal ?: "null"}, ")
        append("\"completed\": ${m.completed}")
        append("}")
    }

    private fun groceryJson(g: GroceryItem) = buildString {
        append("{")
        append("\"name\": ${quote(g.name)}, \"quantity\": ${g.quantity ?: "null"}, ")
        append("\"unit\": ${quote(g.unit)}, \"category\": ${quote(g.category)}, \"completed\": ${g.completed}")
        append("}")
    }

    private fun routineJson(r: WorkoutRoutine) = buildString {
        append("{")
        append("\"name\": ${quote(r.name)}, \"note\": ${quote(r.note)}, ")
        append("\"items\": ${arrayJson(r.items, ::routineItemJson)}")
        append("}")
    }

    private fun routineItemJson(item: com.example.eyirfitnessapp.domain.model.RoutineItem) = buildString {
        append("{")
        append("\"name\": ${quote(item.name)}, \"category\": ${quote(item.category)}, ")
        append("\"durationMinutes\": ${item.durationMinutes ?: "null"}, ")
        append("\"sets\": ${item.sets ?: "null"}, \"reps\": ${item.reps ?: "null"}, ")
        append("\"weightKg\": ${item.weightKg ?: "null"}")
        append("}")
    }
}
