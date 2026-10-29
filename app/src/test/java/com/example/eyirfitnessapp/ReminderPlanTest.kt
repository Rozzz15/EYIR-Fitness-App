package com.example.eyirfitnessapp

import com.example.eyirfitnessapp.domain.notifications.ReminderPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** Spec §27 Notifications: configurable reminders with sane next-fire times. */
class ReminderPlanTest {

    // 2026-09-29 is a Tuesday. 10:00 local.
    private val now: Long = calendar(2026, Calendar.SEPTEMBER, 29, 10, 0).timeInMillis

    private fun calendar(year: Int, month: Int, day: Int, hour: Int, minute: Int): Calendar =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }

    // ── Next-occurrence math ────────────────────────────────────────────────

    @Test
    fun `daily reminder later today stays today`() {
        val at = ReminderPlan.nextDailyAt(now, hour = 15, minute = 0)
        val expected = calendar(2026, Calendar.SEPTEMBER, 29, 15, 0).timeInMillis
        assertEquals(expected, at)
    }

    @Test
    fun `daily reminder already past moves to tomorrow`() {
        val at = ReminderPlan.nextDailyAt(now, hour = 8, minute = 0)
        val expected = calendar(2026, Calendar.SEPTEMBER, 30, 8, 0).timeInMillis
        assertEquals(expected, at)
    }

    @Test
    fun `daily reminder exactly now moves to tomorrow`() {
        val at = ReminderPlan.nextDailyAt(now, hour = 10, minute = 0)
        val expected = calendar(2026, Calendar.SEPTEMBER, 30, 10, 0).timeInMillis
        assertEquals(expected, at)
    }

    @Test
    fun `weekly reminder lands on Sunday evening`() {
        // Tuesday → next Sunday is Oct 4.
        val at = ReminderPlan.nextWeeklyAt(now, Calendar.SUNDAY, 18, 0)
        val expected = calendar(2026, Calendar.OCTOBER, 4, 18, 0).timeInMillis
        assertEquals(expected, at)
    }

    @Test
    fun `weekly reminder later on the same weekday stays today`() {
        // Tuesday 18:00 requested at Tuesday 10:00.
        val tuesday = calendar(2026, Calendar.SEPTEMBER, 29, 10, 0).timeInMillis
        val at = ReminderPlan.nextWeeklyAt(tuesday, Calendar.TUESDAY, 18, 0)
        val expected = calendar(2026, Calendar.SEPTEMBER, 29, 18, 0).timeInMillis
        assertEquals(expected, at)
    }

    // ── Preference gating (§27 "configurable") ──────────────────────────────

    @Test
    fun `all on by default produces six reminders`() {
        val plan = ReminderPlan.build(
            now = now,
            mealsEnabled = true,
            waterEnabled = true,
            weighInEnabled = true,
            fastingEnabled = true,
            weeklyReportEnabled = true,
            habitsEnabled = true,
            habitsRemaining = 2,
            waterGoalMl = 2500,
            waterLoggedMl = 1000
        )
        assertEquals(6, plan.size)
        assertEquals(
            ReminderPlan.Kind.entries.toSet(),
            plan.map { it.kind }.toSet()
        )
    }

    @Test
    fun `disabled reminders are absent from the plan`() {
        val plan = ReminderPlan.build(
            now = now,
            mealsEnabled = false,
            waterEnabled = false,
            weighInEnabled = true,
            fastingEnabled = false,
            weeklyReportEnabled = false,
            habitsEnabled = false,
            habitsRemaining = 0,
            waterGoalMl = 2500,
            waterLoggedMl = 1000
        )
        assertEquals(listOf(ReminderPlan.Kind.WEIGH_IN), plan.map { it.kind })
    }

    @Test
    fun `all disabled produces empty plan`() {
        val plan = ReminderPlan.build(
            now = now, mealsEnabled = false, waterEnabled = false, weighInEnabled = false,
            fastingEnabled = false, weeklyReportEnabled = false, habitsEnabled = false,
            habitsRemaining = 0,
            waterGoalMl = 0, waterLoggedMl = 0
        )
        assertTrue(plan.isEmpty())
    }

    @Test
    fun `reminder ids are stable per kind`() {
        val plan = ReminderPlan.build(
            now = now, mealsEnabled = true, waterEnabled = true, weighInEnabled = true,
            fastingEnabled = true, weeklyReportEnabled = true, habitsEnabled = true,
            habitsRemaining = 1,
            waterGoalMl = 2500, waterLoggedMl = 0
        )
        plan.forEach { reminder ->
            assertEquals(reminder.kind.ordinal, reminder.id)
        }
    }

    @Test
    fun `habit reminder fires in the evening with remaining count`() {
        val plan = ReminderPlan.build(
            now = now, mealsEnabled = false, waterEnabled = false, weighInEnabled = false,
            fastingEnabled = false, weeklyReportEnabled = false, habitsEnabled = true,
            habitsRemaining = 3,
            waterGoalMl = 0, waterLoggedMl = 0
        )
        val habits = plan.single()
        assertEquals(ReminderPlan.Kind.HABITS, habits.kind)
        assertEquals(ReminderPlan.HABITS_HOUR, java.util.Calendar.getInstance().apply {
            timeInMillis = habits.atMillis
        }.get(java.util.Calendar.HOUR_OF_DAY))
        assertTrue(habits.body.contains("3 habits"))
        // Already-past time today → next occurrence is tomorrow evening.
        assertTrue(habits.atMillis > now)
    }

    @Test
    fun `habit body celebrates a fully checked day`() {
        assertEquals("Every habit checked today — nicely done!", ReminderPlan.habitsBody(0))
        assertTrue(ReminderPlan.habitsBody(1).contains("1 habit is still unchecked"))
        assertTrue(ReminderPlan.habitsBody(4).contains("4 habits are still unchecked"))
    }

    @Test
    fun `water body uses remaining amount like the spec example`() {
        assertEquals(
            "You're 500ml away from today's water goal.",
            ReminderPlan.waterBody(500)
        )
        assertEquals(
            "Water goal reached — nicely done!",
            ReminderPlan.waterBody(0)
        )
        // Over goal clamps at zero.
        assertEquals(
            "Water goal reached — nicely done!",
            ReminderPlan.waterBody(-200)
        )
    }

    @Test
    fun `every reminder carries title and body`() {
        val plan = ReminderPlan.build(
            now = now, mealsEnabled = true, waterEnabled = true, weighInEnabled = true,
            fastingEnabled = true, weeklyReportEnabled = true, habitsEnabled = true,
            habitsRemaining = 2,
            waterGoalMl = 2500, waterLoggedMl = 0
        )
        plan.forEach { reminder ->
            assertTrue(reminder.title.isNotBlank())
            assertTrue(reminder.body.isNotBlank())
            assertTrue(reminder.atMillis > now)
        }
    }
}
