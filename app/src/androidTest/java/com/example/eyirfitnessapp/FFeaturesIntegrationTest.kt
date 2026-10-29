package com.example.eyirfitnessapp

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.eyirfitnessapp.data.local.AppDatabase
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.domain.habits.HabitLogic
import com.example.eyirfitnessapp.domain.model.MealType
import com.example.eyirfitnessapp.domain.model.MeasurementPart
import com.example.eyirfitnessapp.domain.model.RoutineItem
import com.example.eyirfitnessapp.domain.model.WorkoutRoutine
import com.example.eyirfitnessapp.util.Dates
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented data-layer tests for the F features (spec §10, §11, §12):
 * routines, measurements and habits against real Room + SQLite, plus the
 * v6→v7 migration that must preserve user data (§34 rule 4).
 */
@RunWith(AndroidJUnit4::class)
class FFeaturesIntegrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val db get() = AppDatabase.get(context)
    private val repo get() = AppRepository.get(context)

    @Before
    fun reset() = runBlocking {
        // Same app database across tests in this process — start each one empty.
        repo.wipeAllData()
    }

    // ── Routines (§10) ──────────────────────────────────────────────────────

    @Test
    fun routineSaveLoadAndLogToDiary() = runBlocking {
        repo.seedIfEmpty()
        val routineId = repo.saveRoutine(
            WorkoutRoutine(
                name = "Push Day",
                items = listOf(
                    RoutineItem(name = "Bench Press", category = "Strength", sets = 3, reps = 10, weightKg = 40.0),
                    RoutineItem(name = "Walking (brisk)", category = "Walking", durationMinutes = 20.0)
                )
            )
        )
        assertTrue(routineId > 0)

        val loaded = repo.routineWithItems(routineId)
        assertNotNull(loaded)
        assertEquals("Push Day", loaded!!.name)
        assertEquals(2, loaded.items.size)
        assertTrue(loaded.items.any { it.sets == 3 && it.reps == 10 })

        // Logging the routine writes one exercise log per line for today.
        val written = repo.logRoutine(loaded, repo.today(), weightKg = 80.0)
        assertEquals(2, written)
        val todaysLogs = repo.observeExerciseLogs(repo.today()).first()
        assertEquals(2, todaysLogs.size)
        assertTrue(todaysLogs.all { it.caloriesBurned > 0.0 })
    }

    @Test
    fun routineUpdateReplacesItemsAndDeleteRemovesEverything() = runBlocking {
        val id = repo.saveRoutine(
            WorkoutRoutine(name = "Leg Day", items = listOf(RoutineItem(name = "Squats", category = "Strength", sets = 3, reps = 8)))
        )
        // Re-save with different items (edit flow) under the same id.
        repo.saveRoutine(
            WorkoutRoutine(id = id, name = "Leg Day", items = listOf(RoutineItem(name = "Deadlift", category = "Strength", sets = 5, reps = 5)))
        )
        val updated = repo.routineWithItems(id)!!
        assertEquals(1, updated.items.size)
        assertEquals("Deadlift", updated.items.first().name)

        repo.deleteRoutine(id)
        assertNull(repo.routineWithItems(id))
    }

    // ── Body measurements (§11) ─────────────────────────────────────────────

    @Test
    fun measurementUpsertKeepsOneRowPerPartPerDay() = runBlocking {
        repo.addMeasurement(MeasurementPart.WAIST, 84.0, "2026-09-28")
        repo.addMeasurement(MeasurementPart.WAIST, 83.2, "2026-09-28") // same day → replace
        repo.addMeasurement(MeasurementPart.CHEST, 98.0, "2026-09-28")

        val all = repo.observeMeasurements().first()
        assertEquals(2, all.size)
        val waist = all.first { it.part == MeasurementPart.WAIST }
        assertEquals(83.2, waist.centimeters, 0.001)
    }

    @Test
    fun measurementDeleteRemovesOnlyThatRow() = runBlocking {
        repo.addMeasurement(MeasurementPart.WAIST, 84.0)
        val row = repo.observeMeasurements().first().first()
        repo.deleteMeasurement(row.id)
        assertTrue(repo.observeMeasurements().first().isEmpty())
    }

    // ── Habits (§12) ────────────────────────────────────────────────────────

    @Test
    fun habitAddToggleAndStreakReadback() = runBlocking {
        repo.addHabit("WATER", "Water", "water_drop")
        val habit = repo.observeHabits().first().first()
        assertEquals("WATER", habit.key)

        val today = Dates.today()
        repo.toggleHabitDay(habit.id, today)
        var reloaded = repo.observeHabits().first().first()
        assertTrue(today in reloaded.completions)
        assertEquals(1, HabitLogic.streak(reloaded, today))

        // Toggle again removes the check (unique index keeps one row per day).
        repo.toggleHabitDay(habit.id, today)
        reloaded = repo.observeHabits().first().first()
        assertFalse(today in reloaded.completions)
    }

    @Test
    fun habitRemoveArchivesButKeepsHistory() = runBlocking {
        repo.addHabit("SLEEP", "Sleep", "bedtime")
        val habit = repo.observeHabits().first().first()
        repo.toggleHabitDay(habit.id, Dates.today())

        repo.removeHabit(habit.id)
        assertTrue(repo.observeHabits().first().isEmpty())
        // History is archived, not erased.
        assertEquals(1, db.habitDao().allCompletions().size)
    }

    // ── Exercise log window (§20 chart ranges) ──────────────────────────────

    @Test
    fun exerciseLogWindowFeedsRangeCharts() = runBlocking {
        val today = Dates.today()
        val yesterday = Dates.addDays(today, -1)
        repo.addExerciseLog(
            com.example.eyirfitnessapp.domain.model.ExerciseLog(
                exerciseId = null, name = "Run", category = "Running",
                durationMinutes = 30.0, sets = null, reps = null, weightKg = null,
                caloriesBurned = 300.0, date = today, loggedAt = 1L
            )
        )
        repo.addExerciseLog(
            com.example.eyirfitnessapp.domain.model.ExerciseLog(
                exerciseId = null, name = "Walk", category = "Walking",
                durationMinutes = 20.0, sets = null, reps = null, weightKg = null,
                caloriesBurned = 100.0, date = yesterday, loggedAt = 2L
            )
        )
        val window = repo.observeExerciseLogsBetween(Dates.addDays(today, -6), today).first()
        assertEquals(2, window.size)
        val todayOnly = repo.observeExerciseLogs(today).first()
        assertEquals(1, todayOnly.size)
    }

    // ── Export includes the new data (§22) ──────────────────────────────────

    @Test
    fun exportIncludesMeasurementsAndHabitCompletions() = runBlocking {
        repo.addMeasurement(MeasurementPart.WAIST, 84.0)
        repo.addHabit("WATER", "Water", "water_drop")
        val habit = repo.observeHabits().first().first()
        repo.toggleHabitDay(habit.id, Dates.today())

        val json = repo.exportData()
        assertTrue(json.contains("\"bodyMeasurements\": ["))
        assertTrue(json.contains("\"WAIST\""))
        assertTrue(json.contains("\"habits\": ["))
        assertTrue(json.contains("\"WATER\""))
        assertTrue(json.contains("\"${Dates.today()}\""))
    }

    // ── v6 → v7 migration (§34 rule 4: data survives) ───────────────────────

    @Test
    fun migrate6To7CreatesNewTablesWithDefaults() {
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL(PROFILE_ROW_V6)
            execSQL(FOOD_LOG_ROW_V6)
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 7, true, AppDatabase.MIGRATION_6_7).use { migrated ->
            migrated.query(
                "SELECT name, notifMeals, notifWater, notifWeighIn, notifFasting, notifWeeklyReport, themeMode " +
                    "FROM profile WHERE id = 1"
            ).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Migrated User", c.getString(0))
                // Notification prefs default to ON (spec §27).
                assertEquals(1, c.getInt(1))
                assertEquals(1, c.getInt(2))
                assertEquals(1, c.getInt(3))
                assertEquals(1, c.getInt(4))
                assertEquals(1, c.getInt(5))
                assertEquals("DARK", c.getString(6))
            }
            migrated.query("SELECT COUNT(*) FROM food_logs").use { c ->
                c.moveToFirst()
                assertEquals(1, c.getInt(0))
            }
            listOf("routines", "routine_items", "body_measurements", "habits", "habit_completions").forEach { table ->
                migrated.query("SELECT COUNT(*) FROM $table").use { c ->
                    c.moveToFirst()
                    assertEquals("table $table should exist and be empty", 0, c.getInt(0))
                }
            }
        }
    }

    @Test
    fun migrate5To7ChainKeepsUserDataIntact() {
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL(PROFILE_ROW_V5)
            execSQL(FOOD_LOG_ROW_V6)
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 7, true, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7).use { migrated ->
            migrated.query("SELECT name, themeMode, notifMeals FROM profile WHERE id = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Chain User", c.getString(0))
                assertEquals("SYSTEM", c.getString(1)) // v5→v6 default
                assertEquals(1, c.getInt(2))           // v6→v7 default
            }
            migrated.query("SELECT mealType FROM food_logs").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(MealType.LUNCH.name, c.getString(0))
            }
        }
    }

    private companion object {
        const val TEST_DB = "f-features-migration-test"

        /** v6 profile row: settings columns exist, notification columns do not. */
        const val PROFILE_ROW_V6 =
            "INSERT INTO profile (id, name, email, sex, age, heightCm, currentWeightKg, goalWeightKg, " +
                "activityLevel, goalType, weeklyGoalKg, weightUnit, waterGoalMl, exerciseCaloriesAddToBudget, " +
                "dailyCalorieGoal, themeMode, weekStartDay, onboardingComplete) VALUES " +
                "(1, 'Migrated User', 'm@e.com', 'MALE', 41, 180.5, 88.25, 80.0, 'MODERATE', 'LOSE', 0.5, 'LB', " +
                "3000, 1, 1800, 'DARK', 'MONDAY', 1)"

        /** v5 profile row: settings columns do not exist yet. */
        const val PROFILE_ROW_V5 =
            "INSERT INTO profile (id, name, email, sex, age, heightCm, currentWeightKg, goalWeightKg, " +
                "activityLevel, goalType, weeklyGoalKg, weightUnit, waterGoalMl, exerciseCaloriesAddToBudget, " +
                "dailyCalorieGoal, onboardingComplete) VALUES " +
                "(1, 'Chain User', 'c@e.com', 'FEMALE', 30, 165.0, 62.0, 58.0, 'LIGHT', 'MAINTAIN', 0.25, 'KG', " +
                "2200, 0, 1900, 1)"

        const val FOOD_LOG_ROW_V6 =
            "INSERT INTO food_logs (foodId, name, brand, mealType, quantity, servingSize, servingUnit, " +
                "calories, protein, carbs, fat, date, loggedAt, synced) VALUES " +
                "(NULL, 'Oats', NULL, 'LUNCH', 1.0, 100.0, 'g', 150.0, 5.0, 27.0, 3.0, '2026-09-28', 1, 0)"
    }
}
