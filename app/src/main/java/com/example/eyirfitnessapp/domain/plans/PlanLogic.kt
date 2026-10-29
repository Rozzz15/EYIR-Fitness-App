package com.example.eyirfitnessapp.domain.plans

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pure plans logic (spec §16–§18). Kept free of Android/UI dependencies so it
 * is unit-testable, per the spec's "transparent calculation service" rule.
 */
object PlanLogic {

    // ── Recipe math (§16) ───────────────────────────────────────────────────

    data class RecipeTotals(
        val totalCalories: Double,
        val totalProtein: Double,
        val totalCarbs: Double,
        val totalFat: Double
    ) {
        /** Per-serving values; servings is coerced to ≥ 1 to avoid division by zero. */
        fun perServing(servings: Int): RecipeTotals {
            val n = servings.coerceAtLeast(1).toDouble()
            return RecipeTotals(
                totalCalories = totalCalories / n,
                totalProtein = totalProtein / n,
                totalCarbs = totalCarbs / n,
                totalFat = totalFat / n
            )
        }
    }

    /** One ingredient contribution; [calories] etc. are for the given quantity. */
    data class IngredientContribution(
        val calories: Double,
        val protein: Double,
        val carbs: Double,
        val fat: Double
    )

    fun totalsOf(ingredients: List<IngredientContribution>): RecipeTotals =
        RecipeTotals(
            totalCalories = ingredients.sumOf { it.calories },
            totalProtein = ingredients.sumOf { it.protein },
            totalCarbs = ingredients.sumOf { it.carbs },
            totalFat = ingredients.sumOf { it.fat }
        )

    /**
     * Ingredient nutrition scaled from per-100g values to [grams].
     * Matches NutritionEngine.scale semantics (per-serving → actual amount).
     */
    fun scaleFromPer100g(
        grams: Double,
        kcal100: Double,
        protein100: Double,
        carbs100: Double,
        fat100: Double
    ): IngredientContribution {
        val f = grams.coerceAtLeast(0.0) / 100.0
        return IngredientContribution(
            calories = kcal100 * f,
            protein = protein100 * f,
            carbs = carbs100 * f,
            fat = fat100 * f
        )
    }

    // ── Weekly plan (§17) ───────────────────────────────────────────────────

    /** Original reference meal ideas with per-serving calorie tags. */
    data class Idea(
        val title: String,
        val kcal: Int
    )

    val IDEAS: List<Idea> = listOf(
        Idea("Oats with fruit", 320),
        Idea("Chicken & rice", 560),
        Idea("Salmon & greens", 480),
        Idea("Egg toast", 340),
        Idea("Greek yogurt bowl", 260),
        Idea("Lentil curry", 430),
        Idea("Pasta & broccoli", 590),
        Idea("Turkey wrap", 410),
        Idea("Tofu stir-fry", 390),
        Idea("Beef & sweet potato", 620)
    )

    /**
     * Fills one day × meal slot. Keeps the running daily total within ±15% of
     * [dailyKcalGoal] by preferring the idea whose kcal lands closest to the
     * remaining budget; ties go to the list order (deterministic, testable —
     * no hidden randomness in the picker itself).
     */
    fun pickIdea(
        mealIndex: Int,
        usedTitles: Set<String>,
        remainingKcal: Double,
        dailyKcalGoal: Int?
    ): Idea {
        val pool = IDEAS.filter { it.title !in usedTitles }.ifEmpty { IDEAS }
        val goal = dailyKcalGoal ?: return pool[mealIndex % pool.size]
        // Typical macro split puts dinner-size meals at ~35% of the day.
        val target = (goal * 0.35).coerceAtMost(remainingKcal.coerceAtLeast(0.0))
        return pool.minByOrNull { abs(it.kcal - target) } ?: pool[mealIndex % pool.size]
    }

    /** True when a planned day lands within ±15% of the goal — the generator's tolerance. */
    fun withinGoalTolerance(dayTotalKcal: Double, dailyKcalGoal: Int): Boolean =
        abs(dayTotalKcal - dailyKcalGoal) <= dailyKcalGoal * 0.15

    // ── Grocery categories (§18) ────────────────────────────────────────────

    val CATEGORIES = listOf("Produce", "Meat", "Dairy", "Grains", "Pantry", "Frozen", "Other")

    private val CATEGORY_KEYWORDS: Map<String, List<String>> = mapOf(
        "Produce" to listOf(
            "banana", "apple", "orange", "berry", "berries", "avocado", "broccoli", "spinach",
            "lettuce", "tomato", "onion", "garlic", "potato", "carrot", "pepper", "cucumber",
            "lemon", "lime", "fruit", "salad", "greens", "kale", "mushroom", "zucchini", "grape"
        ),
        "Meat" to listOf(
            "chicken", "beef", "pork", "turkey", "salmon", "fish", "tuna", "steak", "bacon",
            "sausage", "lamb", "shrimp", "ham", "mince", "anchovy"
        ),
        "Dairy" to listOf(
            "milk", "yogurt", "yoghurt", "cheese", "cheddar", "mozzarella", "butter", "cream",
            "egg", "eggs", "feta", "parmesan"
        ),
        "Grains" to listOf(
            "bread", "rice", "pasta", "oats", "oat", "flour", "tortilla", "noodle", "cereal",
            "quinoa", "bagel", "wrap", "bun", "couscous", "barley"
        ),
        "Frozen" to listOf("frozen", "ice cream")
    )

    /** Keyword → category classification with an explicit Other fallback. */
    fun categorize(name: String): String {
        val n = name.lowercase()
        // Frozen must win over the base food ("frozen peas" → Frozen, not Produce).
        for (kw in CATEGORY_KEYWORDS["Frozen"]!!) {
            if (kw in n) return "Frozen"
        }
        for ((category, keywords) in CATEGORY_KEYWORDS) {
            if (category == "Frozen") continue
            if (keywords.any { it in n }) return category
        }
        return "Other"
    }
}
