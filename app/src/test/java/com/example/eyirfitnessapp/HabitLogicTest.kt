package com.example.eyirfitnessapp

import com.example.eyirfitnessapp.domain.habits.HabitLogic
import com.example.eyirfitnessapp.domain.model.Habit
import com.example.eyirfitnessapp.util.Dates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec §12 Habits + Streak. */
class HabitLogicTest {

    private val today = "2026-09-29"
    private val created = Dates.parseMillis(today)

    private fun habit(vararg done: String) = Habit(
        id = 1,
        key = "WATER",
        label = "Water",
        icon = "water_drop",
        createdAt = created,
        completions = done.toSet()
    )

    // ── Completion window ───────────────────────────────────────────────────

    @Test
    fun `completions in window counts today inclusive`() {
        val h = habit(today, "2026-09-27")
        assertEquals(2, HabitLogic.completionsInWindow(h, today, 7))
    }

    @Test
    fun `completions before the window do not count`() {
        val h = habit("2026-09-01")
        assertEquals(0, HabitLogic.completionsInWindow(h, today, 7))
    }

    @Test
    fun `older completions still count - stored history is trusted`() {
        // A completion row only exists because the user checked that day, so
        // even history that predates this device's habit row counts.
        val h = habit("2026-09-20")
        assertEquals(1, HabitLogic.completionsInWindow(h, today, 30))
    }

    @Test
    fun `completion rate is share of window`() {
        val h = habit(today, "2026-09-28", "2026-09-27", "2026-09-26")
        assertEquals(4.0 / 7.0, HabitLogic.completionRate(h, today, 7), 0.001)
    }

    @Test
    fun `completion rate on empty habit is zero`() {
        assertEquals(0.0, HabitLogic.completionRate(habit(), today, 7), 0.001)
    }

    // ── Streaks ─────────────────────────────────────────────────────────────

    @Test
    fun `streak counts consecutive days ending today`() {
        val h = habit(today, "2026-09-28", "2026-09-27")
        assertEquals(3, HabitLogic.streak(h, today))
    }

    @Test
    fun `streak survives when today is not done yet`() {
        // Yesterday + day before done, today not yet — streak not broken.
        val h = habit("2026-09-28", "2026-09-27")
        assertEquals(2, HabitLogic.streak(h, today))
    }

    @Test
    fun `streak breaks at a gap`() {
        val h = habit(today, "2026-09-27") // missed the 28th
        assertEquals(1, HabitLogic.streak(h, today))
    }

    @Test
    fun `streak on untracked habit is zero`() {
        assertEquals(0, HabitLogic.streak(habit(), today))
    }

    @Test
    fun `isDoneToday reflects today's check`() {
        assertTrue(HabitLogic.isDoneToday(habit(today), today))
        assertFalse(HabitLogic.isDoneToday(habit("2026-09-28"), today))
    }

    // ── Milestones (every 7th day celebration) ─────────────────────────────

    @Test
    fun `milestone fires at every seventh day`() {
        assertEquals(7, HabitLogic.milestone(6, 7))
        assertEquals(14, HabitLogic.milestone(13, 14))
        assertEquals(21, HabitLogic.milestone(20, 21))
    }

    @Test
    fun `milestone stays silent between seventh days`() {
        assertNull(HabitLogic.milestone(4, 5))
        assertNull(HabitLogic.milestone(7, 13))
        assertNull(HabitLogic.milestone(0, 0))
    }

    @Test
    fun `milestone does not re-fire for the same day or a broken streak`() {
        assertNull(HabitLogic.milestone(7, 7)) // unchanged — no re-celebrate
        assertNull(HabitLogic.milestone(8, 7)) // streak dropped
    }

    // ── Consistency (§20 logging consistency) ───────────────────────────────

    @Test
    fun `consistency averages per-day completion across habits`() {
        val a = habit(today)                     // done today
        val b = habit("2026-09-28")              // not done today
        // 1 of 2 habits done today → 0.5 for the only day considered twice.
        assertEquals(0.5, HabitLogic.consistency(listOf(a, b), today, 2), 0.001)
    }

    @Test
    fun `consistency with no habits is zero`() {
        assertEquals(0.0, HabitLogic.consistency(emptyList(), today, 7), 0.001)
    }

    // ── Catalog (§12 habit list) ────────────────────────────────────────────

    @Test
    fun `catalog covers the spec habit list`() {
        val keys = HabitLogic.CATALOG.map { it.key }
        assertTrue(keys.containsAll(listOf("WATER", "EXERCISE", "LOG_FOOD", "LOG_WEIGHT", "SLEEP", "FASTING")))
    }
}
