package com.example.eyirfitnessapp.domain.notifications

import com.example.eyirfitnessapp.util.Dates
import java.util.Calendar

/**
 * Pure scheduling plan for reminders (spec §27). The Android scheduler
 * ([com.example.eyirfitnessapp.notifications.ReminderScheduler]) turns this
 * into alarms; keeping the "when/what" math here makes it unit-testable
 * (§34 rule 2) and lets notifications stay configurable (§27).
 */
object ReminderPlan {

    /** One pending reminder the scheduler should have registered. */
    data class Reminder(
        /** Stable request code — one per reminder type. */
        val id: Int,
        /** Spec §27 notification kind. */
        val kind: Kind,
        /** Epoch millis of the next occurrence (local time). */
        val atMillis: Long,
        /** Notification title, written in the spec's friendly tone. */
        val title: String,
        /** Notification body. */
        val body: String
    )

    enum class Kind(val prefsKeyLabel: String) {
        MEALS("Meals"),
        WATER("Water"),
        WEIGH_IN("Weigh-in"),
        FASTING("Fasting"),
        WEEKLY_REPORT("Weekly report"),
        HABITS("Habits")
    }

    // ── Time-of-day anchors (local wall clock) ────────────────────────────

    const val MEAL_HOUR = 12          // lunch-ish "don't forget to log"
    const val MEAL_MINUTE = 30
    const val WATER_HOUR = 15         // afternoon hydration nudge
    const val WATER_MINUTE = 0
    const val WEIGH_IN_HOUR = 8       // morning weigh-in
    const val WEIGH_IN_MINUTE = 0
    const val WEEKLY_REPORT_DAY = Calendar.SUNDAY
    const val WEEKLY_REPORT_HOUR = 18
    const val HABITS_HOUR = 20        // evening habit check before the day closes
    const val HABITS_MINUTE = 30

    /**
     * Millis for the next occurrence of [hour]:[minute] local time, either
     * today (if still ahead) or tomorrow.
     */
    fun nextDailyAt(now: Long, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    /** Millis for the next [dayOfWeek] (Calendar constant) at [hour]:[minute]. */
    fun nextWeeklyAt(now: Long, dayOfWeek: Int, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var daysAhead = (dayOfWeek - cal.get(Calendar.DAY_OF_WEEK) + 7) % 7
        if (daysAhead == 0 && cal.timeInMillis <= now) daysAhead = 7
        cal.add(Calendar.DAY_OF_YEAR, daysAhead)
        return cal.timeInMillis
    }

    /** Water body copy uses the remaining amount, mirroring the spec example. */
    fun waterBody(remainingMl: Int): String =
        if (remainingMl > 0) {
            "You're ${remainingMl}ml away from today's water goal."
        } else {
            "Water goal reached — nicely done!"
        }

    /** Evening habit nudge: name what's left, or celebrate an all-checked day. */
    fun habitsBody(remaining: Int): String =
        if (remaining > 0) {
            if (remaining == 1) "1 habit is still unchecked — tick it off before the day closes."
            else "$remaining habits are still unchecked — tick them off before the day closes."
        } else {
            "Every habit checked today — nicely done!"
        }

    /**
     * The full reminder set the scheduler should keep alive. Reminders whose
     * preference is off (spec §27 "configurable") are simply absent, so the
     * scheduler never posts them.
     */
    fun build(
        now: Long,
        mealsEnabled: Boolean,
        waterEnabled: Boolean,
        weighInEnabled: Boolean,
        fastingEnabled: Boolean,
        weeklyReportEnabled: Boolean,
        habitsEnabled: Boolean,
        habitsRemaining: Int,
        waterGoalMl: Int,
        waterLoggedMl: Int
    ): List<Reminder> {
        val reminders = mutableListOf<Reminder>()
        if (mealsEnabled) {
            reminders += Reminder(
                id = Kind.MEALS.ordinal,
                kind = Kind.MEALS,
                atMillis = nextDailyAt(now, MEAL_HOUR, MEAL_MINUTE),
                title = "Don't forget to log lunch",
                body = "It's ${Dates.prettyDate(Dates.today())} — capture what you ate to keep your diary honest."
            )
        }
        if (waterEnabled) {
            reminders += Reminder(
                id = Kind.WATER.ordinal,
                kind = Kind.WATER,
                atMillis = nextDailyAt(now, WATER_HOUR, WATER_MINUTE),
                title = "Time for a glass of water",
                body = waterBody((waterGoalMl - waterLoggedMl).coerceAtLeast(0))
            )
        }
        if (weighInEnabled) {
            reminders += Reminder(
                id = Kind.WEIGH_IN.ordinal,
                kind = Kind.WEIGH_IN,
                atMillis = nextDailyAt(now, WEIGH_IN_HOUR, WEIGH_IN_MINUTE),
                title = "Ready for your weigh-in?",
                body = "Step on the scale to keep your trend line going."
            )
        }
        if (fastingEnabled) {
            reminders += Reminder(
                id = Kind.FASTING.ordinal,
                kind = Kind.FASTING,
                atMillis = nextDailyAt(now, WEIGH_IN_HOUR, WEIGH_IN_MINUTE),
                title = "Planning a fast today?",
                body = "Start a fast in the morning and the timer tracks it for you."
            )
        }
        if (weeklyReportEnabled) {
            reminders += Reminder(
                id = Kind.WEEKLY_REPORT.ordinal,
                kind = Kind.WEEKLY_REPORT,
                atMillis = nextWeeklyAt(now, WEEKLY_REPORT_DAY, WEEKLY_REPORT_HOUR, 0),
                title = "Your weekly report is ready",
                body = "See your averages, trends and streaks for this week."
            )
        }
        if (habitsEnabled) {
            reminders += Reminder(
                id = Kind.HABITS.ordinal,
                kind = Kind.HABITS,
                atMillis = nextDailyAt(now, HABITS_HOUR, HABITS_MINUTE),
                title = "Evening habit check",
                body = habitsBody(habitsRemaining)
            )
        }
        return reminders
    }
}
