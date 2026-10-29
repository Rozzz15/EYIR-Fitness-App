package com.example.eyirfitnessapp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "foods", indices = [Index(value = ["barcode"], unique = false)])
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String?,
    val barcode: String?,
    val servingSize: Double,
    val servingUnit: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val sugar: Double,
    val sodium: Double,
    val verified: Boolean,
    val source: String,
    /** Spec §8 Favorites. Default matches the v4→v5 migration (spec §34 DB-first). */
    @ColumnInfo(defaultValue = "0") val favorite: Boolean = false,
    /** v7→v8: grams in one serving for count-based foods; null = unknown. */
    val gramsPerServing: Double? = null,
    /** v8→v9: pickable size options, encoded "Small=44,Medium=50"; null = none. */
    val sizes: String? = null
)

@Entity(tableName = "food_logs", indices = [Index("date")])
data class FoodLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodId: Long?,
    val name: String,
    val brand: String?,
    val mealType: String,
    val quantity: Double,
    val servingSize: Double,
    val servingUnit: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val date: String,
    val loggedAt: Long,
    val synced: Boolean = false
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val metValue: Double
)

@Entity(tableName = "exercise_logs", indices = [Index("date")])
data class ExerciseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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

@Entity(tableName = "weight_logs", indices = [Index("date")])
data class WeightLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weightKg: Double,
    val note: String?,
    val date: String,
    val loggedAt: Long
)

@Entity(tableName = "water_logs", indices = [Index("date")])
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMl: Int,
    val date: String,
    val loggedAt: Long
)

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Long = 1,
    val name: String,
    val email: String?,
    val sex: String,
    val age: Int,
    val heightCm: Double,
    val currentWeightKg: Double,
    val goalWeightKg: Double,
    val activityLevel: String,
    val goalType: String,
    val weeklyGoalKg: Double,
    val weightUnit: String,
    val waterGoalMl: Int,
    val exerciseCaloriesAddToBudget: Boolean,
    /** null → derive from BMR/TDEE (spec §21); a value is a manual override. */
    val dailyCalorieGoal: Int?,
    /** Spec §22 Preferences; DEFAULT literals match [MIGRATION_5_6]. */
    @ColumnInfo(defaultValue = "SYSTEM") val themeMode: String,
    @ColumnInfo(defaultValue = "MONDAY") val weekStartDay: String,
    /**
     * Spec §27 notification preferences; DEFAULT literals match [AppDatabase.MIGRATION_6_7].
     * All on by default; the scheduler reads these before posting anything.
     */
    @ColumnInfo(defaultValue = "1") val notifMeals: Boolean = true,
    @ColumnInfo(defaultValue = "1") val notifWater: Boolean = true,
    @ColumnInfo(defaultValue = "1") val notifWeighIn: Boolean = true,
    @ColumnInfo(defaultValue = "1") val notifFasting: Boolean = true,
    @ColumnInfo(defaultValue = "1") val notifWeeklyReport: Boolean = true,
    @ColumnInfo(defaultValue = "1") val notifHabits: Boolean = true,
    val onboardingComplete: Boolean,
    /** Avatar photo: path inside app-private storage (copied from the picker); null = letter initial. */
    val avatarUri: String? = null
)

/**
 * Spec §24 `macro_goals`. Single row (id = 1): this build has one local user
 * with no account system, exactly like `profile`.
 */
@Entity(tableName = "macro_goals")
data class MacroGoalEntity(
    @PrimaryKey val id: Long = 1,
    val goalMode: String,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val proteinPercent: Int,
    val carbsPercent: Int,
    val fatPercent: Int,
    val updatedAt: Long
)
