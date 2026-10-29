package com.example.eyirfitnessapp.domain.routines

import com.example.eyirfitnessapp.domain.model.RoutineItem
import com.example.eyirfitnessapp.domain.model.WorkoutRoutine

/**
 * Pure helpers behind the routine builder (spec §10 "Workout Routine").
 * Kept free of Android/UI code so edit rules and burn estimates are
 * unit-testable (§34 rule 2).
 */
object RoutineLogic {

    /** A draft line being edited in the routine builder. */
    data class DraftItem(
        val name: String = "",
        val category: String = "Strength",
        val durationMinutes: Double? = null,
        val sets: Int? = null,
        val reps: Int? = null,
        val weightKg: Double? = null,
        val exerciseId: Long? = null
    ) {
        /** A line is complete when it has a name and either cardio or strength values. */
        val isValid: Boolean
            get() = name.isNotBlank() &&
                (durationMinutes?.takeIf { it > 0 } != null ||
                    (sets?.takeIf { it > 0 } != null && reps?.takeIf { it > 0 } != null))

        fun toItem(): RoutineItem = RoutineItem(
            exerciseId = exerciseId,
            name = name.trim(),
            category = category,
            durationMinutes = durationMinutes?.takeIf { it > 0 },
            sets = sets?.takeIf { it > 0 },
            reps = reps?.takeIf { it > 0 },
            weightKg = weightKg?.takeIf { it > 0 }
        )
    }

    /** A routine can be saved when it has a name and at least one valid line. */
    fun canSave(name: String, items: List<RoutineItem>): Boolean =
        name.isNotBlank() && items.isNotEmpty()

    /** A blank new routine pre-seeded with one empty strength line. */
    fun newDraft(): Pair<String, List<DraftItem>> = "" to listOf(DraftItem())

    /**
     * Estimated minutes for one item — same formula the manual strength logger
     * uses (ExerciseViewModel): ~2.5s per rep, 4s transition overhead per rep
     * cycle, plus rest between sets. Cardio lines just use their duration.
     */
    fun estimatedMinutes(item: RoutineItem): Double = when {
        item.durationMinutes != null -> item.durationMinutes
        item.sets != null -> item.sets * (item.reps ?: 10) * 2.5 / 60.0 * 4 + item.sets * 1.5
        else -> 10.0
    }

    /** Rough whole-routine duration; used for the "Log routine" confirmation. */
    fun estimatedMinutes(routine: WorkoutRoutine): Double =
        routine.items.sumOf { estimatedMinutes(it) }
}
