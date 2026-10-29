package com.example.eyirfitnessapp

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.eyirfitnessapp.data.local.AppDatabase
import com.example.eyirfitnessapp.data.local.SeedData
import com.example.eyirfitnessapp.data.repository.AppRepository
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
 * Data-layer checks that JVM unit tests cannot make (Room + SQLite on device):
 * offline seeding on a fresh install (spec §26) and the v3→v6 migrations that
 * must preserve user data (§34 rule 4).
 *
 * Schema JSONs for the old versions come from `app/schemas`, wired into the
 * androidTest assets in build.gradle.kts.
 */
@RunWith(AndroidJUnit4::class)
class DataLayerTest {

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

    // ── Fresh install seeding (spec §26) ──────────────────────────────────

    @Test
    fun freshInstallSeedsFoodsExercisesAndAllSevenBarcodes() = runBlocking {
        repo.seedIfEmpty()

        val foods = db.foodDao().search("")
        assertEquals(SeedData.foods.size, foods.size)
        assertEquals(SeedData.exercises.size, db.exerciseDao().all().size)

        val barcodes = SeedData.foods.mapNotNull { it.barcode }
        assertEquals(7, barcodes.size)
        barcodes.forEach { code ->
            assertNotNull("seed barcode $code missing", db.foodDao().byBarcode(code))
        }
    }

    @Test
    fun searchMatchesBarcodeLikeNameAndBrand() = runBlocking {
        repo.seedIfEmpty()

        // Add Food's search field advertises "Name, brand or barcode" — the
        // query has to honour all three, with the exact barcode ranked first.
        val byBarcode = db.foodDao().search("1000000000015")
        assertEquals("1000000000015", byBarcode.first().barcode)
        assertEquals("Cheddar Crackers", byBarcode.first().name)

        val byBrand = db.foodDao().search("Snack Co")
        assertTrue(byBrand.any { it.name == "Protein Bar" })

        val byName = db.foodDao().search("Cheddar")
        assertTrue(byName.any { it.name == "Cheddar Crackers" })
        assertTrue(byName.any { it.name == "Cheddar Cheese" })
    }

    @Test
    fun seedingTwiceDoesNotDuplicateRows() = runBlocking {
        repo.seedIfEmpty()
        val foodCount = db.foodDao().search("").size
        val exerciseCount = db.exerciseDao().all().size

        repo.seedIfEmpty()

        assertEquals(foodCount, db.foodDao().search("").size)
        assertEquals(exerciseCount, db.exerciseDao().all().size)
    }

    @Test
    fun favoritesFlagRoundTripsThroughTheDatabase() = runBlocking {
        repo.seedIfEmpty()
        val banana = db.foodDao().search("banana").first()
        assertFalse(banana.favorite)

        db.foodDao().setFavorite(banana.id, true)
        assertTrue(db.foodDao().byId(banana.id)!!.favorite)
        assertTrue(db.foodDao().observeFavorites().first().any { it.id == banana.id })

        db.foodDao().setFavorite(banana.id, false)
        assertFalse(db.foodDao().byId(banana.id)!!.favorite)
        assertTrue(db.foodDao().observeFavorites().first().isEmpty())
    }

    // ── Migrations (spec §34 rule 4: data survives) ───────────────────────

    @Test
    fun migrate3To4AddsCalorieColumnAndMacroGoalsTable() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(PROFILE_ROW_V3)
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 4, true, AppDatabase.MIGRATION_3_4).use { migrated ->
            migrated.query("SELECT name, dailyCalorieGoal, onboardingComplete FROM profile WHERE id = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Migrated User", c.getString(0))
                assertTrue("new column must start null", c.isNull(1))
                assertEquals(1, c.getInt(2))
            }
            migrated.query("SELECT COUNT(*) FROM macro_goals").use { c ->
                c.moveToFirst()
                assertEquals(0, c.getInt(0))
            }
        }
    }

    @Test
    fun migrate4To5AddsFavoriteDefaultingToExistingFoods() {
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL(PROFILE_ROW_V3_WITH_CALORIES)
            execSQL(FOOD_ROW_V4)
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 5, true, AppDatabase.MIGRATION_4_5).use { migrated ->
            migrated.query("SELECT name, barcode, favorite FROM foods WHERE id = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Test Food", c.getString(0))
                assertEquals("1234567890123", c.getString(1))
                assertEquals("existing food must default to not-favorite", 0, c.getInt(2))
            }
            migrated.query("SELECT COUNT(*) FROM profile").use { c ->
                c.moveToFirst()
                assertEquals(1, c.getInt(0))
            }
        }
    }

    @Test
    fun migrate5To6AddsSettingsDefaultsWithoutTouchingData() {
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL(PROFILE_ROW_V3_WITH_CALORIES)
            execSQL(FOOD_ROW_V4)
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 6, true, AppDatabase.MIGRATION_5_6).use { migrated ->
            migrated.query(
                "SELECT name, email, dailyCalorieGoal, themeMode, weekStartDay FROM profile WHERE id = 1"
            ).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Migrated User", c.getString(0))
                assertEquals("m@e.com", c.getString(1))
                assertEquals(1800, c.getInt(2))
                assertEquals("SYSTEM", c.getString(3))
                assertEquals("MONDAY", c.getString(4))
            }
            migrated.query("SELECT COUNT(*) FROM foods").use { c ->
                c.moveToFirst()
                assertEquals(1, c.getInt(0))
            }
        }
    }

    @Test
    fun chain3To6KeepsProfileAndFoodRowsIntact() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(PROFILE_ROW_V3)
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB, 6, true,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6
        ).use { migrated ->
            migrated.query(
                "SELECT name, sex, age, heightCm, currentWeightKg, weightUnit, waterGoalMl, themeMode, weekStartDay " +
                    "FROM profile WHERE id = 1"
            ).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Migrated User", c.getString(0))
                assertEquals("MALE", c.getString(1))
                assertEquals(41, c.getInt(2))
                assertEquals(180.5, c.getDouble(3), 0.0)
                assertEquals(88.25, c.getDouble(4), 0.0)
                assertEquals("LB", c.getString(5))
                assertEquals(3000, c.getInt(6))
                assertEquals("SYSTEM", c.getString(7))
                assertEquals("MONDAY", c.getString(8))
            }
            migrated.query("SELECT COUNT(*) FROM macro_goals").use { c ->
                c.moveToFirst()
                assertEquals(0, c.getInt(0))
            }
        }
    }

    @Test
    fun migrate3To4LeavesDailyCalorieGoalReadableAsNull() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(PROFILE_ROW_V3)
            close()
        }
        helper.runMigrationsAndValidate(TEST_DB, 4, true, AppDatabase.MIGRATION_3_4).use { migrated ->
            migrated.query("SELECT dailyCalorieGoal FROM profile WHERE id = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertNull(c.getString(0))
            }
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"

        /** v3 profile row: no dailyCalorieGoal, no settings columns yet. */
        const val PROFILE_ROW_V3 =
            "INSERT INTO profile (id, name, email, sex, age, heightCm, currentWeightKg, goalWeightKg, " +
                "activityLevel, goalType, weeklyGoalKg, weightUnit, waterGoalMl, exerciseCaloriesAddToBudget, " +
                "onboardingComplete) VALUES (1, 'Migrated User', 'm@e.com', 'MALE', 41, 180.5, 88.25, 80.0, " +
                "'MODERATE', 'LOSE', 0.5, 'LB', 3000, 1, 1)"

        /** Same row as it exists on v4/v5 (calorie goal column present). */
        const val PROFILE_ROW_V3_WITH_CALORIES =
            "INSERT INTO profile (id, name, email, sex, age, heightCm, currentWeightKg, goalWeightKg, " +
                "activityLevel, goalType, weeklyGoalKg, weightUnit, waterGoalMl, exerciseCaloriesAddToBudget, " +
                "dailyCalorieGoal, onboardingComplete) VALUES (1, 'Migrated User', 'm@e.com', 'MALE', 41, 180.5, " +
                "88.25, 80.0, 'MODERATE', 'LOSE', 0.5, 'LB', 3000, 1, 1800, 1)"

        /** v4/v5 foods row (favorite column added by MIGRATION_4_5). */
        const val FOOD_ROW_V4 =
            "INSERT INTO foods (id, name, brand, barcode, servingSize, servingUnit, calories, protein, carbs, " +
                "fat, fiber, sugar, sodium, verified, source) VALUES (1, 'Test Food', 'Brand', '1234567890123', " +
                "100.0, 'g', 200.0, 10.0, 20.0, 5.0, 2.0, 3.0, 4.0, 1, 'seed')"
    }
}
