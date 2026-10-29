package com.example.eyirfitnessapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Phase 4/6 data layer (spec §10, §11, §12): reusable workout routines, body
 * measurements and habit tracking. Added in DB v7 — see [AppDatabase.MIGRATION_6_7].
 */

/** A reusable workout routine (spec §10 "Workout Routine", e.g. "Push Day"). */
@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)

/** One exercise line inside a routine. Copy of the fields the diary needs. */
@Entity(
    tableName = "routine_items",
    indices = [Index("routineId")]
)
data class RoutineItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val exerciseId: Long?,      // nullable: custom/free-text entry
    val name: String,
    val category: String,
    val durationMinutes: Double?,
    val sets: Int?,
    val reps: Int?,
    val weightKg: Double?
)

/** Body measurement snapshot (spec §11). One row per part+date; cm stored. */
@Entity(
    tableName = "body_measurements",
    indices = [Index(value = ["part", "date"], unique = true)]
)
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val part: String,        // MeasurementPart name (WAIST, CHEST, …)
    val centimeters: Double,
    val date: String,        // yyyy-MM-dd
    val loggedAt: Long
)

/**
 * A habit the user chose to track (spec §12 "Habits"). Rows live here once the
 * user turns the habit on; the catalog of possible habits is UI-level.
 */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Stable key shared with the UI catalog (WATER, EXERCISE, LOG_FOOD, …). */
    val key: String,
    val label: String,
    val icon: String,        // Material icon shorthand, resolved in the UI
    val archived: Boolean = false,
    val createdAt: Long
)

/** One day a habit was completed. Unique per habit+date. */
@Entity(
    tableName = "habit_completions",
    indices = [Index(value = ["habitId", "date"], unique = true)]
)
data class HabitCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val date: String         // yyyy-MM-dd
)
