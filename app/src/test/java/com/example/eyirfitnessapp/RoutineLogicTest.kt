package com.example.eyirfitnessapp

import com.example.eyirfitnessapp.domain.model.RoutineItem
import com.example.eyirfitnessapp.domain.model.WeightUnit
import com.example.eyirfitnessapp.domain.routines.RoutineLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec §10 Workout Routine builder rules. */
class RoutineLogicTest {

    // ── Draft validation ────────────────────────────────────────────────────

    @Test
    fun `draft is invalid without a name`() {
        val draft = RoutineLogic.DraftItem(name = "", sets = 3, reps = 10)
        assertFalse(draft.isValid)
    }

    @Test
    fun `strength draft needs sets and reps`() {
        assertFalse(RoutineLogic.DraftItem(name = "Bench", sets = 3, reps = null).isValid)
        assertFalse(RoutineLogic.DraftItem(name = "Bench", sets = null, reps = 10).isValid)
        assertTrue(RoutineLogic.DraftItem(name = "Bench", sets = 3, reps = 10).isValid)
    }

    @Test
    fun `cardio draft needs positive minutes`() {
        assertFalse(RoutineLogic.DraftItem(name = "Run", durationMinutes = 0.0).isValid)
        assertTrue(RoutineLogic.DraftItem(name = "Run", durationMinutes = 30.0).isValid)
    }

    @Test
    fun `zero and negative values are stripped when converting to item`() {
        val item = RoutineLogic.DraftItem(
            name = " Bench Press ",
            sets = 0,
            reps = -5,
            weightKg = -10.0,
            durationMinutes = -1.0
        ).toItem()
        assertEquals("Bench Press", item.name)
        assertEquals(null, item.sets)
        assertEquals(null, item.reps)
        assertEquals(null, item.weightKg)
        assertEquals(null, item.durationMinutes)
    }

    // ── canSave ─────────────────────────────────────────────────────────────

    @Test
    fun `canSave requires name and at least one item`() {
        val item = RoutineItem(name = "Squats", category = "Strength", sets = 3, reps = 8)
        assertFalse(RoutineLogic.canSave("", listOf(item)))
        assertFalse(RoutineLogic.canSave("Leg Day", emptyList()))
        assertTrue(RoutineLogic.canSave("Leg Day", listOf(item)))
    }

    // ── Duration estimates (match the manual strength logger) ───────────────

    @Test
    fun `cardio estimate is the logged duration`() {
        val item = RoutineItem(name = "Run", category = "Running", durationMinutes = 42.0)
        assertEquals(42.0, RoutineLogic.estimatedMinutes(item), 0.01)
    }

    @Test
    fun `strength estimate uses sets reps and rest`() {
        // 3 × 10: 3 * 10 * 2.5 / 60 * 4 + 3 * 1.5 = 5.0 + 4.5 = 9.5
        val item = RoutineItem(name = "Squats", category = "Strength", sets = 3, reps = 10)
        assertEquals(9.5, RoutineLogic.estimatedMinutes(item), 0.01)
    }

    @Test
    fun `line without values falls back to ten minutes`() {
        val item = RoutineItem(name = "Stretch", category = "Custom")
        assertEquals(10.0, RoutineLogic.estimatedMinutes(item), 0.01)
    }

    @Test
    fun `routine estimate is the sum of its lines`() {
        val routine = com.example.eyirfitnessapp.domain.model.WorkoutRoutine(
            name = "Push Day",
            items = listOf(
                RoutineItem(name = "Bench", category = "Strength", sets = 3, reps = 10),
                RoutineItem(name = "Run", category = "Running", durationMinutes = 20.0)
            )
        )
        assertEquals(29.5, RoutineLogic.estimatedMinutes(routine), 0.01)
    }

    // ── Detail labels ───────────────────────────────────────────────────────

    @Test
    fun `strength label renders sets reps and weight in user unit`() {
        val item = RoutineItem(name = "Bench", category = "Strength", sets = 5, reps = 5, weightKg = 100.0)
        assertEquals("5 × 5 @ 100 kg", item.detailLabel(WeightUnit.KG))
    }

    @Test
    fun `cardio label renders minutes`() {
        val item = RoutineItem(name = "Run", category = "Running", durationMinutes = 30.0)
        assertEquals("30 min", item.detailLabel())
    }

    @Test
    fun `pounds are converted in labels`() {
        val item = RoutineItem(name = "Bench", category = "Strength", sets = 3, reps = 8, weightKg = 80.0)
        val label = item.detailLabel(WeightUnit.LB)
        assertTrue(label.contains("176") || label.contains("177")) // 80kg ≈ 176.4lb
    }
}
