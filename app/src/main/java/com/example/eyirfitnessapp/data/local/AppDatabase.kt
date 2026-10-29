package com.example.eyirfitnessapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FoodEntity::class,
        FoodLogEntity::class,
        ExerciseEntity::class,
        ExerciseLogEntity::class,
        WeightLogEntity::class,
        WaterLogEntity::class,
        ProfileEntity::class,
        MacroGoalEntity::class,
        FastingSessionEntity::class,
        RecipeEntity::class,
        RecipeItemEntity::class,
        MealPlanItemEntity::class,
        GroceryItemEntity::class,
        RoutineEntity::class,
        RoutineItemEntity::class,
        BodyMeasurementEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class
    ],
    // v3: plans data layer — recipes, meal plan items, grocery items (spec §16–§18).
    // v4: macro goals table + manual calorie-goal override (spec §9, §21, §24).
    // v5: foods.favorite (spec §8).
    // v6: profile.themeMode + profile.weekStartDay (spec §22).
    // v7: routines, body measurements, habits, notification prefs (§10, §11, §12, §27).
    // v8: foods.gramsPerServing — gram weight of one serving for scale-based logging.
    // v9: foods.sizes — pickable size options (Egg Small/Medium/Large…).
    version = 11,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun foodLogDao(): FoodLogDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun weightDao(): WeightDao
    abstract fun waterDao(): WaterDao
    abstract fun profileDao(): ProfileDao
    abstract fun macroGoalDao(): MacroGoalDao
    abstract fun fastingDao(): FastingDao
    abstract fun recipeDao(): RecipeDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun groceryDao(): GroceryDao
    abstract fun routineDao(): RoutineDao
    abstract fun bodyMeasurementDao(): BodyMeasurementDao
    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /**
         * v2 → v3: adds the plans tables. Pure CREATE TABLE / CREATE INDEX —
         * no existing table is touched, so user data survives the update.
         *
         * Column definitions intentionally have NO DEFAULT clauses: Room derives
         * the expected schema from the entity declarations only, where Kotlin
         * default values are invisible. Any DB-level default would fail
         * schema validation after migration.
         */
        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recipes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT,
                        `servings` INTEGER NOT NULL,
                        `totalCalories` REAL NOT NULL,
                        `totalProtein` REAL NOT NULL,
                        `totalCarbs` REAL NOT NULL,
                        `totalFat` REAL NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recipe_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `recipeId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `quantity` REAL NOT NULL,
                        `unit` TEXT NOT NULL,
                        `calories` REAL NOT NULL,
                        `protein` REAL NOT NULL,
                        `carbs` REAL NOT NULL,
                        `fat` REAL NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `meal_plan_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `mealType` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `recipeId` INTEGER,
                        `plannedKcal` REAL,
                        `completed` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `grocery_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `quantity` REAL,
                        `unit` TEXT,
                        `category` TEXT NOT NULL,
                        `completed` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_items_recipeId` ON `recipe_items` (`recipeId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_plan_items_date` ON `meal_plan_items` (`date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_plan_items_date_mealType` ON `meal_plan_items` (`date`, `mealType`)")
            }
        }

        /**
         * v3 → v4: macro goals (spec §24) + manual calorie-goal column.
         * Both are additive: an ALTER of a nullable column and a brand-new
         * table, so every existing row survives untouched.
         */
        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `profile` ADD COLUMN `dailyCalorieGoal` INTEGER")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `macro_goals` (
                        `id` INTEGER NOT NULL,
                        `goalMode` TEXT NOT NULL,
                        `proteinGrams` INTEGER NOT NULL,
                        `carbsGrams` INTEGER NOT NULL,
                        `fatGrams` INTEGER NOT NULL,
                        `proteinPercent` INTEGER NOT NULL,
                        `carbsPercent` INTEGER NOT NULL,
                        `fatPercent` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * v4 → v5: favorite flag on foods (spec §8). Additive only; the DEFAULT
         * literal is what the entity's `@ColumnInfo(defaultValue = "0")` expects,
         * so schema validation passes and existing rows default to "not favorite".
         */
        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `foods` ADD COLUMN `favorite` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v5 → v6: settings columns on the single profile row (spec §22
         * Preferences). Both are additive with defaults, so the existing
         * profile row keeps every value and gains SYSTEM / MONDAY.
         */
        internal val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `profile` ADD COLUMN `themeMode` TEXT NOT NULL DEFAULT 'SYSTEM'")
                db.execSQL("ALTER TABLE `profile` ADD COLUMN `weekStartDay` TEXT NOT NULL DEFAULT 'MONDAY'")
            }
        }

        /**
         * v6 → v7: routines + routine items, body measurements, habits and
         * habit completions (spec §10, §11, §12) plus per-reminder notification
         * preferences on the profile row (spec §27). All CREATE TABLE/INDEX and
         * additive ALTERs with defaults, so every existing row survives.
         */
        internal val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `routines` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `note` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `routine_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `routineId` INTEGER NOT NULL,
                        `exerciseId` INTEGER,
                        `name` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `durationMinutes` REAL,
                        `sets` INTEGER,
                        `reps` INTEGER,
                        `weightKg` REAL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `body_measurements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `part` TEXT NOT NULL,
                        `centimeters` REAL NOT NULL,
                        `date` TEXT NOT NULL,
                        `loggedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habits` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `key` TEXT NOT NULL,
                        `label` TEXT NOT NULL,
                        `icon` TEXT NOT NULL,
                        `archived` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habit_completions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `habitId` INTEGER NOT NULL,
                        `date` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_routine_items_routineId` ON `routine_items` (`routineId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_body_measurements_part_date` ON `body_measurements` (`part`, `date`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_completions_habitId_date` ON `habit_completions` (`habitId`, `date`)")
                db.execSQL("ALTER TABLE `profile` ADD COLUMN `notifMeals` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `profile` ADD COLUMN `notifWater` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `profile` ADD COLUMN `notifWeighIn` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `profile` ADD COLUMN `notifFasting` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `profile` ADD COLUMN `notifWeeklyReport` INTEGER NOT NULL DEFAULT 1")
            }
        }

        /**
         * v7 → v8: foods.gramsPerServing — how many grams ONE serving weighs
         * for count-based foods ("1 medium banana = 118 g"), so the amount can
         * be entered from a food scale. Nullable with no DEFAULT: existing rows
         * become "unknown" and the app asks the first time it's needed.
         */
        internal val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `foods` ADD COLUMN `gramsPerServing` REAL")
            }
        }

        /**
         * v8 → v9: foods.sizes — pickable size options shown as chips on the
         * add-food screen (Egg: Small/Medium/Large/X-Large). The seeded egg
         * also gains its standard large-egg weight (56 g) so size chips work
         * without the ask-once prompt; guards keep any weight the user already
         * typed untouched.
         */
        internal val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `foods` ADD COLUMN `sizes` TEXT")
                db.execSQL(
                    "UPDATE `foods` SET `sizes` = 'Small=44,Medium=50,Large=56,X-Large=63' " +
                        "WHERE `name` = 'Egg' AND `source` = 'seed' AND `sizes` IS NULL"
                )
                db.execSQL(
                    "UPDATE `foods` SET `gramsPerServing` = 56.0 " +
                        "WHERE `name` = 'Egg' AND `source` = 'seed' AND `gramsPerServing` IS NULL"
                )
            }
        }

        /**
         * v9 → v10: profile.notifHabits — the evening habit-check reminder
         * toggle (spec §27). Additive with DEFAULT 1 so existing rows keep
         * reminders working the way they did before.
         */
        internal val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `profile` ADD COLUMN `notifHabits` INTEGER NOT NULL DEFAULT 1")
            }
        }

        /**
         * v10 → v11: profile.avatarUri — a path inside app-private storage
         * pointing at the picked profile photo (nullable; null = letter
         * initial). Additive with no DEFAULT: existing rows simply have no
         * photo yet.
         */
        internal val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `profile` ADD COLUMN `avatarUri` TEXT")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "eyirfitness.db"
                )
                    .addMigrations(
                        MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                        MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
                        MIGRATION_10_11
                    )
                    .build()
                    .also { instance = it }
            }
    }
}
