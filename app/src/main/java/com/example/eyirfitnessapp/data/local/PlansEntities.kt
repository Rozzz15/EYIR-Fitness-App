package com.example.eyirfitnessapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Plans data layer (spec §16–§18): recipes, weekly meal plan, grocery list.
 *
 * Recipes store nutrition for the WHOLE recipe (totals summed at save time);
 * consumers divide by [RecipeEntity.servings] via perServing(), so a single
 * rule lives in the domain model instead of being baked into the stored row.
 */
@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String?,
    val servings: Int,
    val totalCalories: Double,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double,
    val createdAt: Long,
    val updatedAt: Long
)

/** One ingredient row of a recipe. Nutrition is per the given quantity. */
@Entity(
    tableName = "recipe_items",
    indices = [Index("recipeId")]
)
data class RecipeItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
 * One planned meal slot (spec §17: date + meal type). The weekly plan is the
 * set of non-deleted items; no separate header table is needed.
 *
 * [plannedKcal] is exact for recipe-backed slots and an estimate for
 * free-text suggestions — it is what "log to diary" writes, so a logged
 * plan slot never invents calories it doesn't know.
 */
@Entity(
    tableName = "meal_plan_items",
    indices = [Index("date"), Index(value = ["date", "mealType"])]
)
data class MealPlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,       // yyyy-MM-dd
    val mealType: String,   // MealType name
    val title: String,
    val recipeId: Long?,    // set when the slot points at a saved recipe
    val plannedKcal: Double?,
    val completed: Boolean = false
)

/** One grocery list item (spec §18). Single shared list, like the single profile. */
@Entity(tableName = "grocery_items")
data class GroceryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: Double?,
    val unit: String?,
    val category: String,   // Produce, Meat, Dairy, Grains, Pantry, Frozen, Other
    val completed: Boolean = false
)
