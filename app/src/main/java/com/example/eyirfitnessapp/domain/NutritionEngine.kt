package com.example.eyirfitnessapp.domain

import com.example.eyirfitnessapp.domain.model.ActivityLevel
import com.example.eyirfitnessapp.domain.model.DailySummary
import com.example.eyirfitnessapp.domain.model.ExerciseLog
import com.example.eyirfitnessapp.domain.model.Food
import com.example.eyirfitnessapp.domain.model.FoodLog
import com.example.eyirfitnessapp.domain.model.GoalType
import com.example.eyirfitnessapp.domain.model.Goals
import com.example.eyirfitnessapp.domain.model.MacroGoals
import com.example.eyirfitnessapp.domain.model.MacroTargetMode
import com.example.eyirfitnessapp.domain.model.Sex
import com.example.eyirfitnessapp.domain.model.UserProfile
import com.example.eyirfitnessapp.domain.model.WaterLog
import kotlin.math.roundToInt

/**
 * Spec §34: business logic stays out of UI. Pure, testable functions.
 * Pipeline (spec §21): BMR → TDEE → Goal adjustment → Daily calorie goal → Macro distribution.
 */
object NutritionEngine {

    /** Mifflin-St Jeor BMR. */
    fun bmr(profile: UserProfile): Double {
        val base = 10 * profile.currentWeightKg + 6.25 * profile.heightCm - 5 * profile.age
        return when (profile.sex) {
            Sex.MALE -> base + 5
            Sex.FEMALE -> base - 161
        }
    }

    fun tdee(profile: UserProfile): Double =
        bmr(profile) * profile.activityLevel.factor

    /** Weekly goal in kg → daily calorie adjustment. 1 kg fat ≈ 7700 kcal. */
    fun dailyAdjustment(profile: UserProfile): Double {
        val weeklyDeltaKg = when (profile.goalType) {
            GoalType.LOSE -> -profile.weeklyGoalKg
            GoalType.GAIN -> profile.weeklyGoalKg
            // Nutrition/habit goals change behaviour, not energy balance.
            GoalType.MAINTAIN, GoalType.IMPROVE_NUTRITION, GoalType.BUILD_HABITS -> 0.0
        }
        return weeklyDeltaKg * 7700.0 / 7.0
    }

    /** The BMR/TDEE estimate itself (spec §21 pipeline). */
    fun computedCalorieGoal(profile: UserProfile): Int {
        val floor = 1200.0
        val goal = (tdee(profile) + dailyAdjustment(profile)).coerceAtLeast(floor)
        return goal.roundToInt()
    }

    /**
     * Effective daily calorie goal: a manual override always wins over the
     * estimate, so the number shown everywhere comes from one place (spec §9).
     */
    fun dailyCalorieGoal(profile: UserProfile): Int =
        profile.dailyCalorieGoal?.takeIf { it > 0 } ?: computedCalorieGoal(profile)

    /** Macro split: 30% protein / 40% carbs / 30% fat (spec §9 example). */
    fun macroGoals(profile: UserProfile, override: MacroGoals? = null): Goals {
        val calories = dailyCalorieGoal(profile)
        val c = calories.toDouble()

        if (override != null && override.mode == MacroTargetMode.GRAMS) {
            // Gram targets are used as-is (Goals screen copy), so the calorie
            // goal has to follow from them — otherwise the ring, the diary
            // "remaining" and the macro grams would disagree all day.
            val fromGrams =
                override.proteinGrams * 4.0 + override.carbsGrams * 4.0 + override.fatGrams * 9.0
            return Goals(
                dailyCalorieGoal = if (fromGrams > 0) fromGrams.roundToInt() else calories,
                proteinGrams = override.proteinGrams,
                carbsGrams = override.carbsGrams,
                fatGrams = override.fatGrams
            )
        }

        // Percentage mode (or no stored goal yet): default 30/40/30.
        val p = (override?.proteinPercent ?: 30).coerceIn(0, 100).toDouble()
        val cb = (override?.carbsPercent ?: 40).coerceIn(0, 100).toDouble()
        val f = (override?.fatPercent ?: 30).coerceIn(0, 100).toDouble()
        val sum = p + cb + f
        // Normalise so a mis-entered split still adds up to the calorie goal.
        val pf = if (sum > 0) p / sum else 1.0 / 3.0
        val cf = if (sum > 0) cb / sum else 1.0 / 3.0
        val ff = if (sum > 0) f / sum else 1.0 / 3.0

        return Goals(
            dailyCalorieGoal = calories,
            proteinGrams = (c * pf / 4.0).roundToInt(),
            carbsGrams = (c * cf / 4.0).roundToInt(),
            fatGrams = (c * ff / 9.0).roundToInt()
        )
    }

    /** Nutrition for a food scaled by logged quantity (absolute amount in the food's serving unit). */
    fun scale(food: Food, quantity: Double): ScaledNutrition {
        val servingSize = if (food.servingSize > 0) food.servingSize else 1.0
        val factor = quantity.coerceAtLeast(0.0) / servingSize
        return ScaledNutrition(
            calories = food.calories * factor,
            protein = food.protein * factor,
            carbs = food.carbs * factor,
            fat = food.fat * factor,
            fiber = food.fiber * factor,
            sugar = food.sugar * factor,
            sodium = food.sodium * factor
        )
    }

    /** True when the food's serving is already a weight (unit "g"). */
    fun isWeightBased(food: Food): Boolean =
        food.servingUnit.equals("g", ignoreCase = true)

    /**
     * Grams in ONE serving. Weight-based foods use their serving size directly;
     * count-based foods need the weight the user saved ("1 medium = 118 g").
     * null = unknown → the app must ask before gram entry is possible.
     */
    fun gramsPerServing(food: Food): Double? =
        if (isWeightBased(food)) food.servingSize.takeIf { it > 0 }
        else food.gramsPerServing?.takeIf { it > 0 }

    /** Grams for a quantity expressed in the food's serving unit. */
    fun toGrams(food: Food, quantity: Double): Double? {
        val perServing = gramsPerServing(food) ?: return null
        val servingSize = if (food.servingSize > 0) food.servingSize else 1.0
        return quantity / servingSize * perServing
    }

    /**
     * Inverse of [toGrams]: a scale reading in grams → quantity in the food's
     * serving unit, ready for [scale]. null when the gram weight is unknown.
     */
    fun fromGrams(food: Food, grams: Double): Double? {
        val perServing = gramsPerServing(food) ?: return null
        val servingSize = if (food.servingSize > 0) food.servingSize else 1.0
        return grams / perServing * servingSize
    }

    /** Daily calorie budget: goal, plus exercise burn when the profile opts in. */
    fun budget(summary: DailySummary, addExerciseToBudget: Boolean = true): Double {
        val goal = summary.goal?.dailyCalorieGoal ?: 0
        return goal + if (addExerciseToBudget) summary.caloriesBurned else 0.0
    }

    /** Spec §9: Remaining = Goal + Exercise − Food. */
    fun caloriesRemaining(summary: DailySummary, addExerciseToBudget: Boolean = true): Int =
        (budget(summary, addExerciseToBudget) - summary.caloriesConsumed).roundToInt()

    fun summarize(
        date: String,
        foodLogs: List<FoodLog>,
        exerciseLogs: List<ExerciseLog>,
        waterLogs: List<WaterLog>,
        goal: Goals?
    ): DailySummary {
        val round = { v: Double -> Math.round(v * 10) / 10.0 }
        return DailySummary(
            date = date,
            caloriesConsumed = foodLogs.sumOf { it.calories },
            caloriesBurned = exerciseLogs.sumOf { it.caloriesBurned },
            protein = round(foodLogs.sumOf { it.protein }),
            carbs = round(foodLogs.sumOf { it.carbs }),
            fat = round(foodLogs.sumOf { it.fat }),
            waterMl = waterLogs.sumOf { it.amountMl },
            goal = goal
        )
    }
}

data class ScaledNutrition(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val sugar: Double,
    val sodium: Double
)
