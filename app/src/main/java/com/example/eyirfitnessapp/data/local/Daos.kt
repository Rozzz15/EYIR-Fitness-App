package com.example.eyirfitnessapp.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foods: List<FoodEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(food: FoodEntity): Long

    @Update
    suspend fun update(food: FoodEntity)

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun byId(id: Long): FoodEntity?

    @Query(
        """
        SELECT * FROM foods
        WHERE name LIKE '%' || :query || '%'
           OR brand LIKE '%' || :query || '%'
           OR barcode LIKE '%' || :query || '%'
        ORDER BY (barcode = :query) DESC, favorite DESC, verified DESC, name ASC
        LIMIT 50
        """
    )
    suspend fun search(query: String): List<FoodEntity>

    @Query("SELECT * FROM foods WHERE barcode = :barcode LIMIT 1")
    suspend fun byBarcode(barcode: String): FoodEntity?

    @Query("UPDATE foods SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("SELECT * FROM foods WHERE favorite = 1 ORDER BY name ASC")
    fun observeFavorites(): Flow<List<FoodEntity>>
}

@Dao
interface FoodLogDao {
    @Insert
    suspend fun insert(log: FoodLogEntity): Long

    @Update
    suspend fun update(log: FoodLogEntity)

    @Query("DELETE FROM food_logs WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM food_logs WHERE date = :date ORDER BY loggedAt ASC")
    fun observeForDate(date: String): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_logs WHERE date = :date ORDER BY loggedAt ASC")
    suspend fun forDate(date: String): List<FoodLogEntity>

    @Query("SELECT * FROM food_logs WHERE date BETWEEN :from AND :to ORDER BY date ASC, loggedAt ASC")
    fun observeBetween(from: String, to: String): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_logs ORDER BY loggedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<FoodLogEntity>

    /** Everything in chronological order — data export (spec §22). */
    @Query("SELECT * FROM food_logs ORDER BY date ASC, loggedAt ASC")
    suspend fun all(): List<FoodLogEntity>

    /** Spec §2 "Frequent foods": how often each food has actually been logged. */
    @Query(
        """
        SELECT foodId AS foodId, COUNT(*) AS frequency
        FROM food_logs
        WHERE foodId IS NOT NULL
        GROUP BY foodId
        ORDER BY frequency DESC, MAX(loggedAt) DESC
        LIMIT :limit
        """
    )
    suspend fun frequentFoodIds(limit: Int): List<FoodFrequency>
}

/** Read-only projection for [FoodLogDao.frequentFoodIds] — not a table. */
data class FoodFrequency(val foodId: Long, val frequency: Int)

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    suspend fun all(): List<ExerciseEntity>

    // LIMIT 150: the expanded catalog is 500+ rows and a wider first page
    // keeps the picker listing meaningful before the user types anything.
    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' ORDER BY name ASC LIMIT 150")
    suspend fun search(query: String): List<ExerciseEntity>

    @Insert
    suspend fun insertLog(log: ExerciseLogEntity): Long

    @Query("DELETE FROM exercise_logs WHERE id = :id")
    suspend fun deleteLog(id: Long)

    @Query("SELECT * FROM exercise_logs WHERE date = :date ORDER BY loggedAt ASC")
    fun observeLogsForDate(date: String): Flow<List<ExerciseLogEntity>>

    /** Whole-day window in one stream — chart range selector (spec §11, §20). */
    @Query("SELECT * FROM exercise_logs WHERE date BETWEEN :from AND :to ORDER BY date ASC, loggedAt ASC")
    fun observeLogsBetween(from: String, to: String): Flow<List<ExerciseLogEntity>>

    @Query("SELECT * FROM exercise_logs ORDER BY date ASC, loggedAt ASC")
    suspend fun allLogs(): List<ExerciseLogEntity>
}

@Dao
interface WeightDao {
    @Insert
    suspend fun insert(log: WeightLogEntity): Long

    @Query("DELETE FROM weight_logs WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM weight_logs ORDER BY date ASC, loggedAt ASC")
    fun observeAll(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs ORDER BY date ASC, loggedAt ASC")
    suspend fun all(): List<WeightLogEntity>
}

@Dao
interface WaterDao {
    @Insert
    suspend fun insert(log: WaterLogEntity): Long

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM water_logs WHERE date = :date ORDER BY loggedAt ASC")
    fun observeForDate(date: String): Flow<List<WaterLogEntity>>

    /** Ordered by insertion time so "undo last" really removes the last glass. */
    @Query("SELECT * FROM water_logs WHERE date BETWEEN :from AND :to ORDER BY date ASC, loggedAt ASC")
    suspend fun between(from: String, to: String): List<WaterLogEntity>

    @Query("SELECT * FROM water_logs ORDER BY date ASC, loggedAt ASC")
    suspend fun all(): List<WaterLogEntity>
}

@Dao
interface FastingDao {
    @Insert
    suspend fun insert(session: FastingSessionEntity): Long

    @Query("UPDATE fasting_sessions SET status = :status, endTime = :endTime WHERE id = :id")
    suspend fun finish(id: Long, endTime: Long, status: String)

    @Query("SELECT * FROM fasting_sessions WHERE status = 'ACTIVE' ORDER BY startTime DESC LIMIT 1")
    suspend fun active(): FastingSessionEntity?

    @Query("SELECT * FROM fasting_sessions ORDER BY startTime DESC")
    fun observeAll(): Flow<List<FastingSessionEntity>>

    @Query("SELECT * FROM fasting_sessions ORDER BY startTime DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<FastingSessionEntity>

    @Query("SELECT * FROM fasting_sessions ORDER BY startTime ASC")
    suspend fun all(): List<FastingSessionEntity>
}

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileEntity)

    @Query("SELECT * FROM profile WHERE id = 1")
    fun observe(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun get(): ProfileEntity?
}

@Dao
interface MacroGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: MacroGoalEntity)

    @Query("SELECT * FROM macro_goals WHERE id = 1")
    fun observe(): Flow<MacroGoalEntity?>

    @Query("SELECT * FROM macro_goals WHERE id = 1")
    suspend fun get(): MacroGoalEntity?
}

@Dao
interface RecipeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity): Long

    @Insert
    suspend fun insertItems(items: List<RecipeItemEntity>)

    @Query("SELECT * FROM recipes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<RecipeEntity>>

    /** Full table read for the JSON export (spec §22). */
    @Query("SELECT * FROM recipes ORDER BY updatedAt DESC")
    suspend fun all(): List<RecipeEntity>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun byId(id: Long): RecipeEntity?

    @Query("SELECT * FROM recipe_items WHERE recipeId = :recipeId")
    suspend fun itemsFor(recipeId: Long): List<RecipeItemEntity>

    @Query("DELETE FROM recipe_items WHERE recipeId = :recipeId")
    suspend fun deleteItemsFor(recipeId: Long)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface MealPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MealPlanItemEntity): Long

    @Update
    suspend fun update(item: MealPlanItemEntity)

    @Query("DELETE FROM meal_plan_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM meal_plan_items WHERE date BETWEEN :from AND :to")
    fun observeBetween(from: String, to: String): Flow<List<MealPlanItemEntity>>

    @Query("SELECT * FROM meal_plan_items WHERE date BETWEEN :from AND :to")
    suspend fun between(from: String, to: String): List<MealPlanItemEntity>

    /** Full table read for the JSON export (spec §22). */
    @Query("SELECT * FROM meal_plan_items ORDER BY date ASC, mealType ASC")
    suspend fun all(): List<MealPlanItemEntity>

    @Query("SELECT * FROM meal_plan_items WHERE id = :id")
    suspend fun forSlotById(id: Long): MealPlanItemEntity?

    @Query("SELECT * FROM meal_plan_items WHERE date = :date AND mealType = :mealType LIMIT 1")
    suspend fun forSlot(date: String, mealType: String): MealPlanItemEntity?

    @Query("SELECT * FROM meal_plan_items WHERE recipeId = :recipeId")
    suspend fun forRecipe(recipeId: Long): List<MealPlanItemEntity>
}

@Dao
interface RoutineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Insert
    suspend fun insertItems(items: List<RoutineItemEntity>)

    @Query("SELECT * FROM routines ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<RoutineEntity>>

    /**
     * Routines joined with their exercise lines in one query. The plain
     * [observeAll] stream only carries the routine row, so every card, dialog
     * and "log to today" call used to see an empty item list.
     */
    @Transaction
    @Query("SELECT * FROM routines ORDER BY updatedAt DESC")
    fun observeAllWithItems(): Flow<List<RoutineWithItems>>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun byId(id: Long): RoutineEntity?

    /** Full table reads for the JSON export (spec §22). */
    @Query("SELECT * FROM routines ORDER BY updatedAt DESC")
    suspend fun allRoutines(): List<RoutineEntity>

    @Query("SELECT * FROM routine_items ORDER BY routineId ASC, id ASC")
    suspend fun allItems(): List<RoutineItemEntity>

    @Query("SELECT * FROM routine_items WHERE routineId = :routineId ORDER BY id ASC")
    suspend fun itemsFor(routineId: Long): List<RoutineItemEntity>

    @Query("DELETE FROM routine_items WHERE routineId = :routineId")
    suspend fun deleteItemsFor(routineId: Long)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun delete(id: Long)
}

/** One routine plus its ordered exercise lines — projection for the routine list. */
data class RoutineWithItems(
    @Embedded val routine: RoutineEntity,
    @Relation(parentColumn = "id", entityColumn = "routineId")
    val items: List<RoutineItemEntity>
)

@Dao
interface BodyMeasurementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: BodyMeasurementEntity): Long

    @Query("SELECT * FROM body_measurements ORDER BY date ASC, loggedAt ASC")
    fun observeAll(): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM body_measurements ORDER BY date ASC, loggedAt ASC")
    suspend fun all(): List<BodyMeasurementEntity>

    @Query("DELETE FROM body_measurements WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface HabitDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    /** Every row ever written for this catalog key — active and archived. */
    @Query("SELECT * FROM habits WHERE `key` = :key ORDER BY id ASC")
    suspend fun byKey(key: String): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY createdAt ASC, id ASC")
    fun observeActive(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY createdAt ASC, id ASC")
    suspend fun all(): List<HabitEntity>

    @Query("UPDATE habits SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabit(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletion(completion: HabitCompletionEntity): Long

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY date ASC")
    suspend fun completionsFor(habitId: Long): List<HabitCompletionEntity>

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId")
    suspend fun deleteCompletionsFor(habitId: Long)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun deleteCompletion(habitId: Long, date: String)

    @Query("SELECT * FROM habit_completions ORDER BY date ASC")
    fun observeCompletions(): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions ORDER BY date ASC")
    suspend fun allCompletions(): List<HabitCompletionEntity>

    @Query("SELECT COUNT(*) FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun completedCount(habitId: Long, date: String): Int
}

@Dao
interface GroceryDao {
    @Insert
    suspend fun insert(item: GroceryItemEntity): Long

    @Insert
    suspend fun insertAll(items: List<GroceryItemEntity>)

    @Update
    suspend fun update(item: GroceryItemEntity)

    @Query("UPDATE grocery_items SET completed = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM grocery_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM grocery_items WHERE completed = 1")
    suspend fun deleteCompleted()

    @Query("SELECT * FROM grocery_items ORDER BY id ASC")
    fun observeAll(): Flow<List<GroceryItemEntity>>

    @Query("SELECT * FROM grocery_items ORDER BY id ASC")
    suspend fun all(): List<GroceryItemEntity>
}
