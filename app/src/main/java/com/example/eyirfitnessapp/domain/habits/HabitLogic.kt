package com.example.eyirfitnessapp.domain.habits

import com.example.eyirfitnessapp.domain.model.Habit
import com.example.eyirfitnessapp.util.Dates

/**
 * Pure habit-tracking math (spec §12 "Habits" + "Streak"): completion rates
 * and consecutive-day streaks. Kept free of Android/UI code (§34 rule 2).
 */
object HabitLogic {

    /** The habit catalog offered on the Habits screen (spec §12 list). */
    data class CatalogHabit(val key: String, val label: String, val icon: String)

    val CATALOG: List<CatalogHabit> = listOf(
        CatalogHabit("WATER", "Water", "water_drop"),
        CatalogHabit("STEPS", "Steps", "directions_walk"),
        CatalogHabit("EXERCISE", "Exercise", "fitness_center"),
        CatalogHabit("LOG_FOOD", "Food logging", "restaurant"),
        CatalogHabit("LOG_WEIGHT", "Weight logging", "monitor_weight"),
        CatalogHabit("SLEEP", "Sleep", "bedtime"),
        CatalogHabit("FASTING", "Fasting", "schedule")
    )

    /**
     * How many of the last [days] days (ending at [today], today included)
     * the habit was completed. Completion rows only exist for days the user
     * actually checked, so the stored history is trusted as-is.
     */
    fun completionsInWindow(habit: Habit, today: String, days: Int): Int {
        if (days <= 0) return 0
        var count = 0
        for (offset in 0 until days) {
            if (Dates.addDays(today, -offset) in habit.completions) count++
        }
        return count
    }

    /** 0..1 completion rate over the last [days] days ending at [today]. */
    fun completionRate(habit: Habit, today: String, days: Int): Double {
        val done = completionsInWindow(habit, today, days)
        if (done == 0) return 0.0
        return done.toDouble() / days.coerceAtLeast(1)
    }

    /**
     * Consecutive completed days ending at [today], or ending yesterday when
     * today isn't done yet (a streak never breaks just because the day is young).
     */
    fun streak(habit: Habit, today: String): Int {
        var cursor = today
        if (cursor !in habit.completions) cursor = Dates.addDays(cursor, -1)
        var streak = 0
        while (cursor in habit.completions) {
            streak++
            cursor = Dates.addDays(cursor, -1)
        }
        return streak
    }

    /** True when the habit is checked for [today]. */
    fun isDoneToday(habit: Habit, today: String): Boolean = today in habit.completions

    /**
     * Streak milestone at every 7th consecutive day (1 week, 2 weeks …).
     * Returns the crossed milestone when [newStreak] just reached one, else
     * null — the celebration shows once per crossing, never on reopen.
     */
    fun milestone(previousStreak: Int, newStreak: Int): Int? =
        if (newStreak > previousStreak && newStreak > 0 && newStreak % 7 == 0) newStreak else null

    /**
     * Overall logging consistency for the weekly report (spec §20): share of
     * tracked habits completed on each day, averaged over [days].
     */
    fun consistency(habits: List<Habit>, today: String, days: Int): Double {
        if (habits.isEmpty() || days <= 0) return 0.0
        var total = 0.0
        for (offset in 0 until days) {
            val day = Dates.addDays(today, -offset)
            val done = habits.count { day in it.completions }
            total += done.toDouble() / habits.size
        }
        return total / days
    }
}
