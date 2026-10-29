package com.example.eyirfitnessapp.domain.model

/** Spec §24 data model — UI-facing immutable models. */

enum class MealType(val label: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACKS("Snacks")
}

enum class GoalType(val label: String) {
    LOSE("Lose weight"),
    MAINTAIN("Maintain weight"),
    GAIN("Gain weight"),
    IMPROVE_NUTRITION("Improve nutrition"),
    BUILD_HABITS("Build healthy habits");

    /** Whether a weekly pace (kg/week) applies — only meaningful when weight changes. */
    val hasWeeklyPace: Boolean get() = this == LOSE || this == GAIN
}

/** Spec §9: targets may be entered as grams or as a calorie percentage. */
enum class MacroTargetMode(val label: String) {
    GRAMS("Grams"),
    PERCENT("Percentage")
}

/**
 * Spec §24 `macro_goals` — the user's stored macro target. `null` (no row yet)
 * means "use the engine default split" (30/40/30).
 */
data class MacroGoals(
    val mode: MacroTargetMode = MacroTargetMode.PERCENT,
    val proteinGrams: Int = 0,
    val carbsGrams: Int = 0,
    val fatGrams: Int = 0,
    val proteinPercent: Int = 30,
    val carbsPercent: Int = 40,
    val fatPercent: Int = 30
)

enum class ActivityLevel(val label: String, val factor: Double) {
    SEDENTARY("Sedentary", 1.2),
    LIGHT("Lightly active", 1.375),
    MODERATE("Moderately active", 1.55),
    VERY_ACTIVE("Very active", 1.725),
    EXTRA_ACTIVE("Extra active", 1.9)
}

enum class Sex(val label: String) {
    MALE("Male"),
    FEMALE("Female")
}

enum class WeightUnit(val label: String) {
    KG("kg"), LB("lb");

    fun toKg(value: Double): Double = if (this == KG) value else value * 0.45359237
    fun fromKg(kg: Double): Double = if (this == KG) kg else kg / 0.45359237
}

/** Spec §22 Preferences → Theme. */
enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    /** Whether the UI should render dark, given the system setting. */
    fun isDark(isSystemInDark: Boolean): Boolean = when (this) {
        DARK -> true
        LIGHT -> false
        SYSTEM -> isSystemInDark
    }
}

/** Spec §22 Preferences → Start of week; drives the weekly planner windows. */
enum class WeekStartDay(val label: String) {
    MONDAY("Monday"),
    SUNDAY("Sunday")
}

data class Food(
    val id: Long = 0,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val servingSize: Double = 100.0,
    val servingUnit: String = "g",
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double = 0.0,
    val sugar: Double = 0.0,
    val sodium: Double = 0.0,
    val verified: Boolean = false,
    val source: String = "local",
    /** Spec §8 Favorites — persisted with the food row itself. */
    val favorite: Boolean = false,
    /**
     * Gram weight of ONE serving for count-based foods ("1 medium banana = 118 g").
     * Asked once when the user logs by weight and remembered on the food row.
     * null = unknown; foods whose servingUnit is "g" use servingSize instead.
     */
    val gramsPerServing: Double? = null,
    /**
     * Pickable size options ("Egg: Small 44 g, Medium 50 g, Large 56 g…").
     * Empty = no size row in the UI. Selecting one sets the amount to that
     * weight, so nutrition scales through the grams machinery.
     */
    val sizes: List<FoodSize> = emptyList()
)

/** One pickable size of a food; [grams] is the weight it stands for. */
data class FoodSize(val label: String, val grams: Double) {

    companion object {
        /**
         * Persistence codec for [Food.sizes]: `"Small=44,Medium=50,Large=56"`.
         * Plain delimited text keeps the schema free of a JSON dependency;
         * malformed entries are skipped rather than failing the read.
         */
        fun encode(sizes: List<FoodSize>): String? =
            sizes.takeIf { it.isNotEmpty() }
                ?.joinToString(",") { "${it.label}=${it.grams}" }

        fun decode(raw: String?): List<FoodSize> =
            raw?.split(',')
                ?.mapNotNull { part ->
                    val idx = part.indexOf('=')
                    if (idx <= 0) return@mapNotNull null
                    val label = part.substring(0, idx).trim()
                    val grams = part.substring(idx + 1).trim().toDoubleOrNull()
                    if (label.isEmpty() || grams == null || grams <= 0) null
                    else FoodSize(label, grams)
                }
                ?: emptyList()
    }
}

data class FoodLog(
    val id: Long = 0,
    val foodId: Long?,
    val name: String,
    val brand: String? = null,
    val mealType: MealType,
    val quantity: Double,
    val servingSize: Double,
    val servingUnit: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val date: String,        // yyyy-MM-dd
    val loggedAt: Long,
    val synced: Boolean = false
)

data class Exercise(
    val id: Long = 0,
    val name: String,
    val category: String,
    val metValue: Double
)

data class ExerciseLog(
    val id: Long = 0,
    val exerciseId: Long?,
    val name: String,
    val category: String,
    val durationMinutes: Double?,
    val sets: Int?,
    val reps: Int?,
    val weightKg: Double?,
    val caloriesBurned: Double,
    val date: String,
    val loggedAt: Long
)

data class WeightLog(
    val id: Long = 0,
    val weightKg: Double,
    val note: String? = null,
    val date: String,
    val loggedAt: Long
)

data class WaterLog(
    val id: Long = 0,
    val amountMl: Int,
    val date: String,
    val loggedAt: Long
)

data class UserProfile(
    val id: Long = 0,
    val name: String,
    val email: String? = null,
    val sex: Sex = Sex.MALE,
    val age: Int = 30,
    val heightCm: Double = 175.0,
    val currentWeightKg: Double = 75.0,
    val goalWeightKg: Double = 70.0,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val goalType: GoalType = GoalType.MAINTAIN,
    val weeklyGoalKg: Double = 0.5,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val waterGoalMl: Int = 2500,
    val exerciseCaloriesAddToBudget: Boolean = true,
    /** null = derive from BMR/TDEE (spec §21); a value is a manual override. */
    val dailyCalorieGoal: Int? = null,
    /** Spec §22 Preferences. */
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val weekStartDay: WeekStartDay = WeekStartDay.MONDAY,
    /** Spec §27: which reminders the scheduler may post. All default on. */
    val notifMeals: Boolean = true,
    val notifWater: Boolean = true,
    val notifWeighIn: Boolean = true,
    val notifFasting: Boolean = true,
    val notifWeeklyReport: Boolean = true,
    val notifHabits: Boolean = true,
    val onboardingComplete: Boolean = false,
    /** Profile photo: path in app-private storage; null = show the name's letter initial. */
    val avatarUri: String? = null
)

data class Goals(
    val dailyCalorieGoal: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int
)

/** Spec §9 optional micronutrients we actually store: fiber, sugar, sodium. */
data class NutrientTotals(
    val fiber: Double = 0.0,
    val sugar: Double = 0.0,
    val sodium: Double = 0.0
)

data class DailySummary(
    val date: String,
    val caloriesConsumed: Double,
    val caloriesBurned: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val waterMl: Int,
    val goal: Goals?
)

// ── Plans (spec §16–§18) ──────────────────────────────────────────────────

/** Saved recipe; totals are for the WHOLE recipe — divide by servings per portion. */
data class Recipe(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val servings: Int = 1,
    val totalCalories: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFat: Double = 0.0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
) {
    /** Nutrition for one serving; servings < 1 is treated as 1 (never divide by zero). */
    fun perServing(): Recipe = copy(
        totalCalories = totalCalories / servings.coerceAtLeast(1),
        totalProtein = totalProtein / servings.coerceAtLeast(1),
        totalCarbs = totalCarbs / servings.coerceAtLeast(1),
        totalFat = totalFat / servings.coerceAtLeast(1)
    )
}

/** One planned meal slot in the weekly plan (spec §17). */
data class PlannedMeal(
    val id: Long = 0,
    val date: String,        // yyyy-MM-dd
    val mealType: MealType,
    val title: String,
    val recipeId: Long? = null,
    val plannedKcal: Double? = null,
    val completed: Boolean = false
)

/** One grocery list entry (spec §18). */
data class GroceryItem(
    val id: Long = 0,
    val name: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val category: String = "Other",
    val completed: Boolean = false
)

// ── Fasting (spec §19) ────────────────────────────────────────────────────

/** One intermittent-fast session. [status] is `ACTIVE`, `COMPLETED` or `ABANDONED`. */
data class FastingSession(
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val targetHours: Int,
    val status: String
) {
    /** Milliseconds fasted so far (or in total, once ended). */
    fun elapsedMs(atMillis: Long = System.currentTimeMillis()): Long =
        ((endTime ?: atMillis) - startTime).coerceAtLeast(0L)

    val targetMs: Long get() = targetHours * 3_600_000L

    /** True only when the target duration was actually reached. */
    val completedOnTarget: Boolean get() = status == "COMPLETED"
}

// ── Exercise routines (spec §10) ──────────────────────────────────────────

/** Reusable workout routine — a named list of exercise lines, e.g. "Push Day". */
data class WorkoutRoutine(
    val id: Long = 0,
    val name: String,
    val note: String? = null,
    val items: List<RoutineItem> = emptyList(),
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

/** One exercise line in a routine (cardio minutes or strength sets/reps/weight). */
data class RoutineItem(
    val id: Long = 0,
    val exerciseId: Long? = null,
    val name: String,
    val category: String,
    val durationMinutes: Double? = null,
    val sets: Int? = null,
    val reps: Int? = null,
    val weightKg: Double? = null
) {
    /** True when the line is cardio-shaped (duration) rather than strength. */
    val isCardio: Boolean get() = durationMinutes != null

    /** "30 min" / "3 × 10 @ 40 kg" — how the line renders in lists. */
    fun detailLabel(unit: WeightUnit = WeightUnit.KG): String = when {
        durationMinutes != null -> "${durationMinutes.toInt()} min"
        sets != null && reps != null ->
            "$sets × $reps" + (weightKg?.takeIf { it > 0 }?.let { " @ ${"%.0f".format(unit.fromKg(it))} ${unit.label}" } ?: "")
        else -> "Logged"
    }
}

// ── Body measurements (spec §11) ─────────────────────────────────────────

/** Spec §11 measurement sites; all values stored in centimeters. */
enum class MeasurementPart(val label: String) {
    WAIST("Waist"),
    CHEST("Chest"),
    HIPS("Hips"),
    NECK("Neck"),
    ARMS("Arms"),
    THIGHS("Thighs")
}

data class BodyMeasurement(
    val id: Long = 0,
    val part: MeasurementPart,
    val centimeters: Double,
    val date: String,
    val loggedAt: Long
)

// ── Habits (spec §12) ────────────────────────────────────────────────────

/** A tracked habit with its completion history; [completions] are yyyy-MM-dd dates. */
data class Habit(
    val id: Long = 0,
    val key: String,
    val label: String,
    val icon: String,
    val archived: Boolean = false,
    val createdAt: Long = 0,
    val completions: Set<String> = emptySet()
)
