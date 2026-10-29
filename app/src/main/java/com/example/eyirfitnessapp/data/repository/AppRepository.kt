package com.example.eyirfitnessapp.data.repository

import android.content.Context
import com.example.eyirfitnessapp.data.local.AppDatabase
import com.example.eyirfitnessapp.data.local.BodyMeasurementEntity
import com.example.eyirfitnessapp.data.local.ExerciseEntity
import com.example.eyirfitnessapp.data.local.ExerciseLogEntity
import com.example.eyirfitnessapp.data.local.FastingSessionEntity
import com.example.eyirfitnessapp.data.local.FoodEntity
import com.example.eyirfitnessapp.data.local.FoodLogEntity
import com.example.eyirfitnessapp.data.local.GroceryItemEntity
import com.example.eyirfitnessapp.data.local.HabitCompletionEntity
import com.example.eyirfitnessapp.data.local.HabitEntity
import com.example.eyirfitnessapp.data.local.RoutineEntity
import com.example.eyirfitnessapp.data.local.RoutineItemEntity
import com.example.eyirfitnessapp.data.local.MealPlanItemEntity
import com.example.eyirfitnessapp.data.local.MacroGoalEntity
import com.example.eyirfitnessapp.data.local.ProfileEntity
import com.example.eyirfitnessapp.data.local.RecipeEntity
import com.example.eyirfitnessapp.data.local.RecipeItemEntity
import com.example.eyirfitnessapp.data.local.SeedData
import com.example.eyirfitnessapp.data.local.WaterLogEntity
import com.example.eyirfitnessapp.data.local.WeightLogEntity
import com.example.eyirfitnessapp.domain.NutritionEngine
import com.example.eyirfitnessapp.domain.export.DataExport
import com.example.eyirfitnessapp.domain.export.RecipeIngredient
import com.example.eyirfitnessapp.domain.plans.PlanLogic
import com.example.eyirfitnessapp.domain.model.ActivityLevel
import com.example.eyirfitnessapp.domain.model.BodyMeasurement
import com.example.eyirfitnessapp.domain.model.DailySummary
import com.example.eyirfitnessapp.domain.model.Habit
import com.example.eyirfitnessapp.domain.model.Exercise
import com.example.eyirfitnessapp.domain.model.ExerciseLog
import com.example.eyirfitnessapp.domain.model.FastingSession
import com.example.eyirfitnessapp.domain.model.Food
import com.example.eyirfitnessapp.domain.model.FoodLog
import com.example.eyirfitnessapp.domain.model.FoodSize
import com.example.eyirfitnessapp.domain.model.GoalType
import com.example.eyirfitnessapp.domain.model.Goals
import com.example.eyirfitnessapp.domain.model.GroceryItem
import com.example.eyirfitnessapp.domain.model.MealType
import com.example.eyirfitnessapp.domain.model.NutrientTotals
import com.example.eyirfitnessapp.domain.model.MacroGoals
import com.example.eyirfitnessapp.domain.model.MacroTargetMode
import com.example.eyirfitnessapp.domain.model.MeasurementPart
import com.example.eyirfitnessapp.domain.model.PlannedMeal
import com.example.eyirfitnessapp.domain.model.Recipe
import com.example.eyirfitnessapp.domain.model.RoutineItem
import com.example.eyirfitnessapp.domain.model.Sex
import com.example.eyirfitnessapp.domain.model.WorkoutRoutine
import com.example.eyirfitnessapp.domain.model.ThemeMode
import com.example.eyirfitnessapp.domain.model.UserProfile
import com.example.eyirfitnessapp.domain.model.WaterLog
import com.example.eyirfitnessapp.domain.model.WeightLog
import com.example.eyirfitnessapp.domain.model.WeightUnit
import com.example.eyirfitnessapp.domain.model.WeekStartDay
import com.example.eyirfitnessapp.util.AppMessages
import com.example.eyirfitnessapp.util.Dates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Offline-first repository facade (spec §26): the diary works fully without network. */
class AppRepository private constructor(private val db: AppDatabase) {

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // ── Seeding ─────────────────────────────────────────────────────────────

    suspend fun seedIfEmpty() {
        val anyFood = db.foodDao().search("")
        if (anyFood.isEmpty()) db.foodDao().insertAll(SeedData.foods)
        // Exercises merge by NAME, not emptiness: installs from before the
        // expanded catalog already hold the short seed list and would never be
        // touched by an "if empty" check. Only rows not present yet are added,
        // so repeated seeds (and app restarts) never duplicate anything.
        val knownExerciseNames = db.exerciseDao().all()
            .map { it.name.trim().lowercase() }
            .toSet()
        val missingExercises = SeedData.exercises
            .filterNot { it.name.trim().lowercase() in knownExerciseNames }
            .map { it.copy(id = 0) }
        if (missingExercises.isNotEmpty()) db.exerciseDao().insertAll(missingExercises)
    }

    // ── Profile / Goals ─────────────────────────────────────────────────────

    fun observeProfile(): Flow<UserProfile?> =
        db.profileDao().observe().map { it?.toModel() }.guarded()

    suspend fun saveProfile(profile: UserProfile) =
        db.profileDao().upsert(profile.toEntity())

    suspend fun profileOnce(): UserProfile? = db.profileDao().get()?.toModel()

    /** Rename the profile (spec §22); whitespace-only names are ignored. */
    suspend fun renameProfile(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        profileOnce()?.let { saveProfile(it.copy(name = clean)) }
    }

    /**
     * Swap the profile photo. [path] is a file the UI copied into app-private
     * storage (the photo picker's content URI permission expires), so the
     * stored reference stays valid across restarts. The previous photo is
     * deleted best-effort so picking again never stacks orphaned copies.
     */
    suspend fun setAvatar(path: String?) {
        val current = profileOnce() ?: return
        if (path == current.avatarUri) return
        val previous = current.avatarUri
        saveProfile(current.copy(avatarUri = path))
        previous?.takeIf { it.isNotBlank() }?.let { oldPath ->
            try {
                java.io.File(oldPath).takeIf { it.isFile }?.delete()
            } catch (_: Exception) {
                // Deleting a stale photo is cosmetic; never block the swap.
            }
        }
    }

    /** Stored macro target (spec §24 `macro_goals`); null → engine default split. */
    fun observeMacroGoals(): Flow<MacroGoals?> =
        db.macroGoalDao().observe().map { it?.toModel() }.guarded()

    suspend fun saveMacroGoals(goal: MacroGoals) = db.macroGoalDao().upsert(goal.toEntity())

    /**
     * The one source of truth every screen should use for targets: profile
     * (calorie goal, weights) + stored macro target resolved together.
     */
    fun observeGoals(): Flow<Goals?> =
        combine(observeProfile(), observeMacroGoals()) { profile, macros ->
            profile?.let { NutritionEngine.macroGoals(it, macros) }
        }.guarded()

    // ── Foods ───────────────────────────────────────────────────────────────

    suspend fun searchFoods(query: String): List<Food> =
        db.foodDao().search(query.trim()).map { it.toModel() }

    suspend fun foodById(id: Long): Food? = db.foodDao().byId(id)?.toModel()

    suspend fun foodByBarcode(barcode: String): Food? =
        db.foodDao().byBarcode(barcode)?.toModel()

    suspend fun createFood(food: Food): Long = db.foodDao().insert(food.toEntity())

    /** Spec §8 Favorites — one row flag, so search results carry it automatically. */
    fun observeFavorites(): Flow<List<Food>> =
        db.foodDao().observeFavorites().map { list -> list.map { it.toModel() } }.guarded()

    suspend fun setFavorite(food: Food, favorite: Boolean) =
        db.foodDao().setFavorite(food.id, favorite)

    /**
     * Remember how many grams ONE serving of a count-based food weighs
     * ("1 medium = 118 g"), asked once when the user logs by scale weight.
     */
    suspend fun setGramsPerServing(food: Food, grams: Double) {
        val updated = food.copy(gramsPerServing = grams)
        db.foodDao().update(updated.toEntity())
    }

    /**
     * Spec §2 "Frequent foods": ranked by how often they were actually logged.
     * Foods deleted since their logs are skipped rather than failing the read.
     */
    suspend fun frequentFoods(limit: Int = 8): List<Food> =
        db.foodLogDao().frequentFoodIds(limit).mapNotNull { row ->
            db.foodDao().byId(row.foodId)?.toModel()
        }

    suspend fun recentLogs(limit: Int = 15): List<FoodLog> =
        db.foodLogDao().recent(limit).map { it.toModel() }

    // ── Food logging ────────────────────────────────────────────────────────

    fun observeFoodLogs(date: String): Flow<List<FoodLog>> =
        db.foodLogDao().observeForDate(date).map { list -> list.map { it.toModel() } }.guarded()

    suspend fun addFoodLog(log: FoodLog): Long =
        db.foodLogDao().insert(log.toEntity()).also { ensureHabitDone("LOG_FOOD", log.date) }

    /**
     * Log a planned meal slot to the diary (spec §17 "log meal"). Calories are
     * the slot's planned value: exact for recipe-backed slots, estimate otherwise.
     * A recipe-backed slot also carries its P/C/F, so the diary macro rings stay
     * honest; plain text ideas have no macro split to record.
     */
    suspend fun logPlannedMeal(slot: PlannedMeal): Long {
        val perServing = slot.recipeId
            ?.let { db.recipeDao().byId(it) }
            ?.toModel()
            ?.perServing()
        return db.foodLogDao().insert(
            FoodLogEntity(
                foodId = null,
                name = slot.title,
                brand = null,
                mealType = slot.mealType.name,
                quantity = 1.0,
                servingSize = 1.0,
                servingUnit = "serving",
                calories = perServing?.totalCalories ?: slot.plannedKcal ?: 0.0,
                protein = perServing?.totalProtein ?: 0.0,
                carbs = perServing?.totalCarbs ?: 0.0,
                fat = perServing?.totalFat ?: 0.0,
                date = slot.date,
                loggedAt = System.currentTimeMillis()
            )
        ).also { ensureHabitDone("LOG_FOOD", slot.date) }
    }

    /** Log one serving of a saved recipe to the diary (spec §16 one-tap logging). */
    suspend fun logRecipeServing(recipeId: Long, meal: MealType, date: String): Long? {
        val recipe = db.recipeDao().byId(recipeId) ?: return null
        val per = recipe.toModel().perServing()
        return db.foodLogDao().insert(
            FoodLogEntity(
                foodId = null,
                name = recipe.name,
                brand = null,
                mealType = meal.name,
                quantity = 1.0,
                servingSize = 1.0,
                servingUnit = "serving",
                calories = per.totalCalories,
                protein = per.totalProtein,
                carbs = per.totalCarbs,
                fat = per.totalFat,
                date = date,
                loggedAt = System.currentTimeMillis()
            )
        ).also { ensureHabitDone("LOG_FOOD", date) }
    }

    suspend fun updateFoodLog(log: FoodLog) = db.foodLogDao().update(log.toEntity())

    suspend fun deleteFoodLog(id: Long) = db.foodLogDao().delete(id)

    suspend fun logsForDate(date: String): List<FoodLog> =
        db.foodLogDao().forDate(date).map { it.toModel() }

    fun observeLogsBetween(from: String, to: String): Flow<List<FoodLog>> =
        db.foodLogDao().observeBetween(from, to).map { list -> list.map { it.toModel() } }.guarded()

    /** Copy every log from one day onto another (spec §8 "copy previous day"). */
    suspend fun copyDay(from: String, to: String): Int {
        val source = db.foodLogDao().forDate(from)
        source.forEach { log ->
            db.foodLogDao().insert(
                log.copy(id = 0, date = to, loggedAt = System.currentTimeMillis(), synced = false)
            )
        }
        if (source.isNotEmpty()) ensureHabitDone("LOG_FOOD", to)
        return source.size
    }

    // ── Exercise ────────────────────────────────────────────────────────────

    suspend fun searchExercises(query: String): List<Exercise> {
        val all = db.exerciseDao().all().map { it.toModel() }
        val q = query.trim()
        return if (q.isEmpty()) all else all.filter {
            it.name.contains(q, ignoreCase = true) || it.category.contains(q, ignoreCase = true)
        }
    }

    fun observeExerciseLogs(date: String): Flow<List<ExerciseLog>> =
        db.exerciseDao().observeLogsForDate(date).map { list -> list.map { it.toModel() } }.guarded()

    /** Exercise logs across a window of whole days (spec §20 trends). */
    fun observeExerciseLogsBetween(from: String, to: String): Flow<List<ExerciseLog>> =
        db.exerciseDao().observeLogsBetween(from, to).map { list -> list.map { it.toModel() } }.guarded()

    /** Convenience for "last n days ending today" windows. */
    fun observeExerciseLogsForDays(firstDay: String, dayCount: Int): Flow<List<ExerciseLog>> =
        observeExerciseLogsBetween(firstDay, Dates.addDays(firstDay, dayCount - 1))

    suspend fun addExerciseLog(log: ExerciseLog): Long =
        db.exerciseDao().insertLog(log.toEntity()).also { ensureHabitDone("EXERCISE", log.date) }

    suspend fun deleteExerciseLog(id: Long) = db.exerciseDao().deleteLog(id)

    /** MET-based burn: kcal = MET × 3.5 × kg × minutes / 200 (approximation). */
    fun cardioCalories(met: Double, weightKg: Double, minutes: Double): Double =
        (met * 3.5 * weightKg * minutes / 200.0).roundToInt().toDouble()

    // ── Water ───────────────────────────────────────────────────────────────

    fun observeWaterLogs(date: String): Flow<List<WaterLog>> =
        db.waterDao().observeForDate(date).map { list -> list.map { it.toModel() } }.guarded()

    suspend fun addWater(amountMl: Int, date: String = today()) {
        db.waterDao().insert(WaterLogEntity(amountMl = amountMl, date = date, loggedAt = System.currentTimeMillis()))
        // Reaching the day's goal counts the "Water" habit automatically (§12).
        val goal = profileOnce()?.waterGoalMl ?: 2500
        if (waterBetween(date, date).sumOf { it.amountMl } >= goal) {
            ensureHabitDone("WATER", date)
        }
    }

    suspend fun deleteWater(id: Long) = db.waterDao().delete(id)

    suspend fun waterBetween(from: String, to: String): List<WaterLog> =
        db.waterDao().between(from, to).map { it.toModel() }

    // ── Weight ──────────────────────────────────────────────────────────────

    fun observeWeightLogs(): Flow<List<WeightLog>> =
        db.weightDao().observeAll().map { list -> list.map { it.toModel() } }.guarded()

    suspend fun addWeight(log: WeightLog) {
        db.weightDao().insert(log.toEntity())
        profileOnce()?.let { profile ->
            saveProfile(profile.copy(currentWeightKg = log.weightKg))
        }
        ensureHabitDone("LOG_WEIGHT", log.date)
    }

    suspend fun deleteWeight(id: Long) = db.weightDao().delete(id)

    // ── Recipes (§16) ───────────────────────────────────────────────────────

    fun observeRecipes(): Flow<List<Recipe>> =
        db.recipeDao().observeAll().map { list -> list.map { it.toModel() } }.guarded()

    /** Inserts the recipe + its items in one transaction-ish block. */    suspend fun saveRecipe(
        recipe: Recipe,
        items: List<Triple<String, Double, PlanLogic.IngredientContribution>>
    ): Long {
        val now = System.currentTimeMillis()
        val recipeId = db.recipeDao().insertRecipe(
            recipe.copy(createdAt = if (recipe.id == 0L) now else recipe.createdAt, updatedAt = now).toEntity()
        )
        // REPLACE keeps PK 0 → new row; reuse the returned id for item links.
        db.recipeDao().deleteItemsFor(recipeId)
        db.recipeDao().insertItems(
            items.map { (name, qty, contrib) ->
                RecipeItemEntity(
                    recipeId = recipeId,
                    name = name,
                    quantity = qty,
                    unit = "g",
                    calories = contrib.calories,
                    protein = contrib.protein,
                    carbs = contrib.carbs,
                    fat = contrib.fat
                )
            }
        )
        return recipeId
    }

    suspend fun deleteRecipe(id: Long) {
        // Orphan-free: plan slots pointing at this recipe drop to plain text ideas.
        db.mealPlanDao().forRecipe(id).forEach { slot ->
            db.mealPlanDao().update(slot.copy(recipeId = null))
        }
        db.recipeDao().deleteItemsFor(id)
        db.recipeDao().delete(id)
    }

    // ── Meal plan (§17) ─────────────────────────────────────────────────────

    fun observePlan(from: String, to: String): Flow<List<PlannedMeal>> =
        db.mealPlanDao().observeBetween(from, to).map { list -> list.map { it.toModel() } }.guarded()

    /** One slot per date+mealType (spec §17 layout). Re-planning a slot overwrites it. */
    suspend fun setPlanSlot(date: String, mealType: MealType, title: String, recipeId: Long?, plannedKcal: Double?) {
        db.mealPlanDao().forSlot(date, mealType.name)?.let { existing ->
            db.mealPlanDao().delete(existing.id)
        }
        db.mealPlanDao().insert(
            MealPlanItemEntity(
                date = date,
                mealType = mealType.name,
                title = title,
                recipeId = recipeId,
                plannedKcal = plannedKcal
            )
        )
    }

    suspend fun deletePlanSlot(id: Long) = db.mealPlanDao().delete(id)

    suspend fun setPlanSlotCompleted(id: Long, completed: Boolean) {
        db.mealPlanDao().forSlotById(id)?.let { db.mealPlanDao().update(it.copy(completed = completed)) }
    }

    // ── Grocery (§18) ───────────────────────────────────────────────────────

    fun observeGrocery(): Flow<List<GroceryItem>> =
        db.groceryDao().observeAll().map { list -> list.map { it.toModel() } }.guarded()

    suspend fun addGroceryItem(name: String, quantity: Double? = null, unit: String? = null, category: String = PlanLogic.categorize(name)) {
        db.groceryDao().insert(
            GroceryItemEntity(name = name, quantity = quantity, unit = unit, category = category)
        )
    }

    suspend fun setGroceryCompleted(id: Long, completed: Boolean) =
        db.groceryDao().setCompleted(id, completed)

    suspend fun deleteGroceryItem(id: Long) = db.groceryDao().delete(id)

    suspend fun clearCompletedGrocery() = db.groceryDao().deleteCompleted()

    /**
     * Spec §18: extract ingredients from planned recipes into the grocery list.
     * Skips titles already on the list (case-insensitive) to avoid duplicates.
     * Returns how many items were added.
     */
    suspend fun extractGroceryFromPlan(from: String, to: String): Int {
        val slots = db.mealPlanDao().between(from, to)
        val existing = db.groceryDao().all().mapTo(mutableSetOf()) { it.name.lowercase() }
        val toAdd = mutableListOf<GroceryItemEntity>()
        for (slot in slots) {
            val recipeId = slot.recipeId ?: continue
            for (item in db.recipeDao().itemsFor(recipeId)) {
                if (item.name.lowercase() in existing) continue
                existing += item.name.lowercase()
                toAdd += GroceryItemEntity(
                    name = item.name,
                    quantity = item.quantity,
                    unit = item.unit,
                    category = PlanLogic.categorize(item.name)
                )
            }
        }
        if (toAdd.isNotEmpty()) db.groceryDao().insertAll(toAdd)
        return toAdd.size
    }

    // ── Fasting (§19) ───────────────────────────────────────────────────────

    /** Newest-first stream of fasts; the UI derives the active one from it. */
    fun observeFasts(): Flow<List<FastingSession>> =
        db.fastingDao().observeAll().map { list -> list.map { it.toModel() } }.guarded()

    /**
     * Starts a fast. An already-active session is returned untouched instead of
     * stacked, so a half-finished fast (app killed mid-session) can never leave
     * two ACTIVE rows that [endFast] would have to guess between.
     */
    suspend fun startFast(targetHours: Int): Long {
        db.fastingDao().active()?.let { return it.id }
        return db.fastingDao().insert(
            FastingSessionEntity(
                startTime = System.currentTimeMillis(),
                endTime = null,
                targetHours = targetHours,
                status = "ACTIVE"
            )
        ).also { ensureHabitDone("FASTING") }
    }

    /**
     * Ends the active fast. Reaching the target is judged from the clock at the
     * moment of ending, not from stored state, so a fast can never be marked
     * COMPLETED without the full duration actually elapsing.
     */
    suspend fun endFast(session: FastingSession, now: Long = System.currentTimeMillis()) {
        val completed = session.elapsedMs(now) >= session.targetMs
        db.fastingDao().finish(
            id = session.id,
            endTime = now,
            status = if (completed) "COMPLETED" else "ABANDONED"
        )
        ensureHabitDone("FASTING")
    }

    /**
     * Fiber / sugar / sodium for a day. They are not denormalised onto
     * `food_logs` (spec §24), so each logged food is re-read. Logs with no
     * foodId (plan slots, recipes) contribute nothing rather than a guess.
     */
    suspend fun nutrientTotals(logs: List<FoodLog>): NutrientTotals {
        val cache = mutableMapOf<Long, Food>()
        var fiber = 0.0
        var sugar = 0.0
        var sodium = 0.0
        for (log in logs) {
            val id = log.foodId ?: continue
            val food = cache[id] ?: db.foodDao().byId(id)?.toModel()?.also { cache[id] = it } ?: continue
            val serving = log.servingSize.takeIf { it > 0 } ?: food.servingSize.takeIf { it > 0 } ?: 1.0
            val factor = log.quantity / serving
            fiber += food.fiber * factor
            sugar += food.sugar * factor
            sodium += food.sodium * factor
        }
        return NutrientTotals(fiber = fiber, sugar = sugar, sodium = sodium)
    }

    // ── Exercise routines (§10) ────────────────────────────────────────────

    /**
     * Newest-first stream of routines WITH their exercise lines. The routine
     * row alone maps to `items = emptyList()`, which made cards read
     * "0 exercises" and "Log to today" insert nothing.
     */
    fun observeRoutines(): Flow<List<WorkoutRoutine>> =
        db.routineDao().observeAllWithItems()
            .map { list ->
                list.map { row -> row.routine.toModel(row.items.map { it.toModel() }) }
            }
            .guarded()

    /** Whole routine with its items, or null when the routine was deleted. */
    suspend fun routineWithItems(id: Long): WorkoutRoutine? {
        val routine = db.routineDao().byId(id) ?: return null
        return routine.toModel(items = db.routineDao().itemsFor(id).map { it.toModel() })
    }

    /** Insert or update the routine and replace its item list in one pass. */
    suspend fun saveRoutine(routine: WorkoutRoutine): Long {
        val now = System.currentTimeMillis()
        val routineId = db.routineDao().insertRoutine(
            routine.copy(createdAt = if (routine.id == 0L) now else routine.createdAt, updatedAt = now).toEntity()
        )
        db.routineDao().deleteItemsFor(routineId)
        db.routineDao().insertItems(routine.items.map { it.toEntity(routineId) })
        return routineId
    }

    suspend fun deleteRoutine(id: Long) {
        db.routineDao().deleteItemsFor(id)
        db.routineDao().delete(id)
    }

    /**
     * Log every item of a routine as exercise entries for [date] (spec §10
     * "log routine"). Burn uses the catalog MET when the line links to one,
     * falling back to a moderate 5.0 so a custom line still gets an estimate.
     */
    suspend fun logRoutine(routine: WorkoutRoutine, date: String, weightKg: Double): Int {
        val catalog = db.exerciseDao().all().associateBy { it.id }
        var logged = 0
        for (item in routine.items) {
            val met = item.exerciseId?.let { catalog[it]?.metValue } ?: 5.0
            val minutes = item.durationMinutes
                ?: item.sets?.let { s -> s * (item.reps ?: 10) * 2.5 / 60.0 * 4 + s * 1.5 }
                ?: 10.0
            db.exerciseDao().insertLog(
                ExerciseLogEntity(
                    exerciseId = item.exerciseId,
                    name = item.name,
                    category = item.category,
                    durationMinutes = item.durationMinutes,
                    sets = item.sets,
                    reps = item.reps,
                    weightKg = item.weightKg,
                    caloriesBurned = cardioCalories(met, weightKg, minutes),
                    date = date,
                    loggedAt = System.currentTimeMillis()
                )
            )
            logged++
        }
        if (logged > 0) ensureHabitDone("EXERCISE", date)
        return logged
    }

    // ── Body measurements (§11) ────────────────────────────────────────────

    fun observeMeasurements(): Flow<List<BodyMeasurement>> =
        db.bodyMeasurementDao().observeAll().map { list -> list.mapNotNull { it.toModel() } }.guarded()

    /** One measurement per part per day; re-saving overwrites the day's value. */
    suspend fun addMeasurement(part: MeasurementPart, cm: Double, date: String = today()) {
        db.bodyMeasurementDao().upsert(
            BodyMeasurementEntity(part = part.name, centimeters = cm, date = date, loggedAt = System.currentTimeMillis())
        )
    }

    suspend fun deleteMeasurement(id: Long) = db.bodyMeasurementDao().delete(id)

    // ── Habits (§12) ───────────────────────────────────────────────────────

    /** Active habits joined with their completion dates, ordered as created. */
    fun observeHabits(): Flow<List<Habit>> =
        combine(db.habitDao().observeActive(), db.habitDao().observeCompletions()) { habits, completions ->
            val byHabit = completions.groupBy { it.habitId }
            habits.map { h -> h.toModel(byHabit[h.id].orEmpty().map { it.date }.toSet()) }
        }.guarded()

    suspend fun addHabit(key: String, label: String, icon: String) {
        val dao = db.habitDao()
        val rows = dao.byKey(key)
        if (rows.isEmpty()) {
            dao.insert(HabitEntity(key = key, label = label, icon = icon, createdAt = System.currentTimeMillis()))
            return
        }
        // Re-adding an archived habit must restore it (and its history), never
        // insert a second row — the remove dialog promises "re-add it anytime".
        // The keeper is the active row if there is one, else the archived row
        // holding the most completions (duplicates left by older versions).
        val keep = rows.firstOrNull { !it.archived }
            ?: rows.maxByOrNull { row -> dao.completionsFor(row.id).size }
            ?: rows.first()
        for (row in rows) {
            if (row.id == keep.id) continue
            for (completion in dao.completionsFor(row.id)) {
                if (dao.completedCount(keep.id, completion.date) == 0) {
                    dao.insertCompletion(HabitCompletionEntity(habitId = keep.id, date = completion.date))
                }
            }
            dao.deleteCompletionsFor(row.id)
            dao.deleteHabit(row.id)
        }
        dao.update(keep.copy(label = label, icon = icon, archived = false))
    }

    suspend fun removeHabit(id: Long) = db.habitDao().setArchived(id, true)

    /** Toggle one day's check-mark on a habit. */
    suspend fun toggleHabitDay(habitId: Long, date: String) {
        if (db.habitDao().completedCount(habitId, date) > 0) {
            db.habitDao().deleteCompletion(habitId, date)
        } else {
            db.habitDao().insertCompletion(HabitCompletionEntity(habitId = habitId, date = date))
        }
    }

    /**
     * Auto-checks [key] for [date] when real activity satisfies it (spec §12):
     * logging food/water-to-goal/weight/exercise or starting a fast counts the
     * day without a manual tap. No-op when the habit isn't tracked, when the
     * day is already checked, so a deliberate user un-tick is never undone —
     * the next new log entry simply re-counts the day.
     */
    suspend fun ensureHabitDone(key: String, date: String = today()) {
        val dao = db.habitDao()
        val habit = dao.byKey(key).firstOrNull { !it.archived } ?: return
        if (dao.completedCount(habit.id, date) == 0) {
            dao.insertCompletion(HabitCompletionEntity(habitId = habit.id, date = date))
        }
    }

    /**
     * Backfills [date]'s auto-tickable habits from what's already logged —
     * run when the Habits screen opens so data recorded before the habit was
     * added (or before this feature existed) still counts.
     */
    suspend fun reconcileAutoHabits(date: String = today()) {
        val goal = profileOnce()?.waterGoalMl ?: 2500
        if (waterBetween(date, date).sumOf { it.amountMl } >= goal) ensureHabitDone("WATER", date)
        if (db.foodLogDao().forDate(date).isNotEmpty()) ensureHabitDone("LOG_FOOD", date)
        if (db.exerciseDao().observeLogsForDate(date).first().isNotEmpty()) ensureHabitDone("EXERCISE", date)
        if (db.weightDao().observeAll().first().any { it.date == date }) ensureHabitDone("LOG_WEIGHT", date)
        if (db.fastingDao().active() != null) ensureHabitDone("FASTING", date)
    }

    // ── Settings (§22) ─────────────────────────────────────────────────────

    /**
     * Spec §22 Account → Export data: every row this device holds, as JSON.
     * Pure builder in [DataExport] so the format is unit-testable.
     */
    suspend fun exportData(): String {
        val now = System.currentTimeMillis()
        val habits = db.habitDao().all()
        val completions = db.habitDao().allCompletions().groupBy { it.habitId }
        val recipes = db.recipeDao().all().map { it.toModel() }
        val recipeItems = recipes.flatMap { recipe ->
            db.recipeDao().itemsFor(recipe.id).map { item ->
                RecipeIngredient(
                    recipeId = recipe.id,
                    name = item.name,
                    quantity = item.quantity,
                    unit = item.unit,
                    calories = item.calories,
                    protein = item.protein,
                    carbs = item.carbs,
                    fat = item.fat
                )
            }
        }
        val routineItems = db.routineDao().allItems().groupBy { it.routineId }
        return DataExport.build(
            profile = db.profileDao().get()?.toModel(),
            foodLogs = db.foodLogDao().all().map { it.toModel() },
            exerciseLogs = db.exerciseDao().allLogs().map { it.toModel() },
            weightLogs = db.weightDao().all().map { it.toModel() },
            waterLogs = db.waterDao().all().map { it.toModel() },
            fastingSessions = db.fastingDao().all().map { it.toModel() },
            exportedAtMillis = now,
            exportedAtIso = DataExport.isoTimestamp(now),
            bodyMeasurements = db.bodyMeasurementDao().all().mapNotNull { it.toModel() },
            habits = habits.map { h ->
                h.toModel(completions[h.id].orEmpty().map { it.date }.toSet())
            },
            macroGoals = db.macroGoalDao().get()?.toModel(),
            recipes = recipes,
            recipeIngredients = recipeItems,
            mealPlan = db.mealPlanDao().all().map { it.toModel() },
            groceryItems = db.groceryDao().all().map { it.toModel() },
            routines = db.routineDao().allRoutines().map { routine ->
                routine.toModel(routineItems[routine.id].orEmpty().map { it.toModel() })
            }
        )
    }

    /**
     * Spec §22 Account → Delete account: wipes every table on this device.
     * Destructive — the screen must confirm first. Seeded reference data
     * (foods/exercises) is recreated on the next launch by [seedIfEmpty].
     */
    suspend fun wipeAllData() {
        db.clearAllTables()
    }

    // ── Summary ─────────────────────────────────────────────────────────────
    /** Combined daily summary stream: food + exercise + water + goal in one Flow. */
    fun observeSummary(date: String): Flow<DailySummary> {
        return combine(
            observeFoodLogs(date),
            observeExerciseLogs(date),
            observeWaterLogs(date),
            observeGoals()
        ) { food, exercise, water, goal ->
            NutritionEngine.summarize(date, food, exercise, water, goal)
        }.guarded()
    }

    companion object {
        @Volatile private var instance: AppRepository? = null

        fun get(context: Context): AppRepository =
            instance ?: synchronized(this) {
                instance ?: AppRepository(AppDatabase.get(context)).also { instance = it }
            }
    }

    /**
     * Turns a local read failure into a user-facing message and completes the
     * stream. Downstream `stateIn` keeps its last value, so screens degrade to
     * stale-but-rendered data instead of crashing (spec §34 error state).
     */
    private fun <T> Flow<T>.guarded(): Flow<T> =
        catch { AppMessages.error("Couldn't load your data. Try opening the screen again.") }
}

private fun Double.roundToInt(): Int = kotlin.math.round(this).toInt()

// ── Mappers ─────────────────────────────────────────────────────────────

fun FoodEntity.toModel() = Food(
    id = id, name = name, brand = brand, barcode = barcode,
    servingSize = servingSize, servingUnit = servingUnit,
    calories = calories, protein = protein, carbs = carbs, fat = fat,
    fiber = fiber, sugar = sugar, sodium = sodium, verified = verified, source = source,
    favorite = favorite, gramsPerServing = gramsPerServing,
    sizes = FoodSize.decode(sizes)
)

fun Food.toEntity() = FoodEntity(
    id = id, name = name, brand = brand, barcode = barcode,
    servingSize = servingSize, servingUnit = servingUnit,
    calories = calories, protein = protein, carbs = carbs, fat = fat,
    fiber = fiber, sugar = sugar, sodium = sodium, verified = verified, source = source,
    favorite = favorite, gramsPerServing = gramsPerServing,
    sizes = FoodSize.encode(sizes)
)

fun FoodLogEntity.toModel() = FoodLog(
    id = id, foodId = foodId, name = name, brand = brand,
    // A stray/renamed value must not take down the whole diary read.
    mealType = MealType.entries.firstOrNull { it.name == mealType } ?: MealType.BREAKFAST,
    quantity = quantity,
    servingSize = servingSize, servingUnit = servingUnit,
    calories = calories, protein = protein, carbs = carbs, fat = fat,
    date = date, loggedAt = loggedAt, synced = synced
)

fun FoodLog.toEntity() = FoodLogEntity(
    id = id, foodId = foodId, name = name, brand = brand,
    mealType = mealType.name, quantity = quantity,
    servingSize = servingSize, servingUnit = servingUnit,
    calories = calories, protein = protein, carbs = carbs, fat = fat,
    date = date, loggedAt = loggedAt, synced = synced
)

fun ExerciseEntity.toModel() = Exercise(id = id, name = name, category = category, metValue = metValue)

fun ExerciseLogEntity.toModel() = ExerciseLog(
    id = id, exerciseId = exerciseId, name = name, category = category,
    durationMinutes = durationMinutes, sets = sets, reps = reps, weightKg = weightKg,
    caloriesBurned = caloriesBurned, date = date, loggedAt = loggedAt
)

fun ExerciseLog.toEntity() = ExerciseLogEntity(
    id = id, exerciseId = exerciseId, name = name, category = category,
    durationMinutes = durationMinutes, sets = sets, reps = reps, weightKg = weightKg,
    caloriesBurned = caloriesBurned, date = date, loggedAt = loggedAt
)

fun WeightLogEntity.toModel() = WeightLog(
    id = id, weightKg = weightKg, note = note, date = date, loggedAt = loggedAt
)

fun WeightLog.toEntity() = WeightLogEntity(
    id = id, weightKg = weightKg, note = note, date = date, loggedAt = loggedAt
)

fun WaterLogEntity.toModel() = WaterLog(id = id, amountMl = amountMl, date = date, loggedAt = loggedAt)

fun FastingSessionEntity.toModel() = FastingSession(
    id = id, startTime = startTime, endTime = endTime,
    targetHours = targetHours, status = status
)

// ── Plans mappers ────────────────────────────────────────────────────────────

fun RecipeEntity.toModel() = Recipe(
    id = id, name = name, description = description, servings = servings,
    totalCalories = totalCalories, totalProtein = totalProtein,
    totalCarbs = totalCarbs, totalFat = totalFat,
    createdAt = createdAt, updatedAt = updatedAt
)

fun Recipe.toEntity() = RecipeEntity(
    id = id, name = name, description = description, servings = servings,
    totalCalories = totalCalories, totalProtein = totalProtein,
    totalCarbs = totalCarbs, totalFat = totalFat,
    createdAt = createdAt, updatedAt = updatedAt
)

fun MealPlanItemEntity.toModel() = PlannedMeal(
    id = id,
    date = date,
    // A stray/renamed value must not take down the whole plan read.
    mealType = MealType.entries.firstOrNull { it.name == mealType } ?: MealType.BREAKFAST,
    title = title,
    recipeId = recipeId,
    plannedKcal = plannedKcal,
    completed = completed
)

fun GroceryItemEntity.toModel() = GroceryItem(
    id = id, name = name, quantity = quantity, unit = unit,
    category = category, completed = completed
)

fun ProfileEntity.toModel() = UserProfile(
    id = id, name = name, email = email,
    sex = Sex.valueOf(sex), age = age, heightCm = heightCm,
    currentWeightKg = currentWeightKg, goalWeightKg = goalWeightKg,
    activityLevel = ActivityLevel.valueOf(activityLevel),
    goalType = GoalType.valueOf(goalType),
    weeklyGoalKg = weeklyGoalKg,
    weightUnit = WeightUnit.valueOf(weightUnit),
    waterGoalMl = waterGoalMl,
    exerciseCaloriesAddToBudget = exerciseCaloriesAddToBudget,
    dailyCalorieGoal = dailyCalorieGoal,
    themeMode = ThemeMode.entries.firstOrNull { it.name == themeMode } ?: ThemeMode.SYSTEM,
    weekStartDay = WeekStartDay.entries.firstOrNull { it.name == weekStartDay } ?: WeekStartDay.MONDAY,
    notifMeals = notifMeals,
    notifWater = notifWater,
    notifWeighIn = notifWeighIn,
    notifFasting = notifFasting,
    notifWeeklyReport = notifWeeklyReport,
    notifHabits = notifHabits,
    onboardingComplete = onboardingComplete,
    avatarUri = avatarUri
)

fun MacroGoalEntity.toModel() = MacroGoals(
    mode = MacroTargetMode.entries.firstOrNull { it.name == goalMode } ?: MacroTargetMode.PERCENT,
    proteinGrams = proteinGrams,
    carbsGrams = carbsGrams,
    fatGrams = fatGrams,
    proteinPercent = proteinPercent,
    carbsPercent = carbsPercent,
    fatPercent = fatPercent
)

fun MacroGoals.toEntity() = MacroGoalEntity(
    id = 1,
    goalMode = mode.name,
    proteinGrams = proteinGrams,
    carbsGrams = carbsGrams,
    fatGrams = fatGrams,
    proteinPercent = proteinPercent,
    carbsPercent = carbsPercent,
    fatPercent = fatPercent,
    updatedAt = System.currentTimeMillis()
)

// ── Routines / measurements / habits mappers ─────────────────────────────

fun RoutineEntity.toModel(items: List<RoutineItem> = emptyList()) = WorkoutRoutine(
    id = id, name = name, note = note, items = items,
    createdAt = createdAt, updatedAt = updatedAt
)

fun WorkoutRoutine.toEntity() = RoutineEntity(
    id = id, name = name, note = note, createdAt = createdAt, updatedAt = updatedAt
)

fun RoutineItemEntity.toModel() = RoutineItem(
    id = id, exerciseId = exerciseId, name = name, category = category,
    durationMinutes = durationMinutes, sets = sets, reps = reps, weightKg = weightKg
)

fun RoutineItem.toEntity(routineId: Long) = RoutineItemEntity(
    id = id, routineId = routineId, exerciseId = exerciseId, name = name, category = category,
    durationMinutes = durationMinutes, sets = sets, reps = reps, weightKg = weightKg
)

/** Unknown part names (renamed/removed) drop out rather than failing the read. */
fun BodyMeasurementEntity.toModel(): BodyMeasurement? =
    MeasurementPart.entries.firstOrNull { it.name == part }?.let {
        BodyMeasurement(id = id, part = it, centimeters = centimeters, date = date, loggedAt = loggedAt)
    }

fun BodyMeasurement.toEntity() = BodyMeasurementEntity(
    id = id, part = part.name, centimeters = centimeters, date = date, loggedAt = loggedAt
)

fun HabitEntity.toModel(completions: Set<String> = emptySet()) = Habit(
    id = id, key = key, label = label, icon = icon, archived = archived,
    createdAt = createdAt, completions = completions
)

fun UserProfile.toEntity() = ProfileEntity(
    id = 1, name = name, email = email, sex = sex.name, age = age,
    heightCm = heightCm, currentWeightKg = currentWeightKg, goalWeightKg = goalWeightKg,
    activityLevel = activityLevel.name, goalType = goalType.name,
    weeklyGoalKg = weeklyGoalKg, weightUnit = weightUnit.name,
    waterGoalMl = waterGoalMl,
    exerciseCaloriesAddToBudget = exerciseCaloriesAddToBudget,
    dailyCalorieGoal = dailyCalorieGoal,
    themeMode = themeMode.name,
    weekStartDay = weekStartDay.name,
    notifMeals = notifMeals,
    notifWater = notifWater,
    notifWeighIn = notifWeighIn,
    notifFasting = notifFasting,
    notifWeeklyReport = notifWeeklyReport,
    notifHabits = notifHabits,
    onboardingComplete = onboardingComplete,
    avatarUri = avatarUri
)
