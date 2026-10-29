package com.example.eyirfitnessapp.domain.diary

import com.example.eyirfitnessapp.domain.model.FoodLog
import com.example.eyirfitnessapp.domain.model.MealType
import com.example.eyirfitnessapp.util.Dates

/**
 * Pure helpers behind the diary copy actions (spec §2 Food Logging):
 * copy food, copy meal, copy previous meal. Kept out of the UI so each
 * operation is unit-testable (§34 rule 2).
 */
object DiaryLogic {

    /** A fresh row that mirrors [log] but lands on [date], optionally in another meal. */
    fun duplicated(
        log: FoodLog,
        date: String,
        meal: MealType = log.mealType,
        now: Long = System.currentTimeMillis()
    ): FoodLog = log.copy(
        id = 0,
        date = date,
        mealType = meal,
        loggedAt = now,
        synced = false
    )

    /** Entries of one meal section on [date], in logged order. */
    fun mealLogs(logs: List<FoodLog>, date: String, meal: MealType): List<FoodLog> =
        logs.filter { it.date == date && it.mealType == meal }

    /** The day before [date]; source day for "copy yesterday's meal". */
    fun previousDay(date: String): String = Dates.addDays(date, -1)

    /** Copy plan for a whole meal section onto itself or another meal, same day. */
    fun copyMealLogs(logs: List<FoodLog>, date: String, from: MealType, to: MealType): List<FoodLog> =
        mealLogs(logs, date, from).map { duplicated(it, date, to) }

    /**
     * Yesterday's [meal] entries duplicated onto [date].
     * Rows from any other day are ignored, so a wider log list stays safe.
     */
    fun previousMealLogs(previousDayLogs: List<FoodLog>, date: String, meal: MealType): List<FoodLog> =
        mealLogs(previousDayLogs, previousDay(date), meal).map { duplicated(it, date, meal) }
}
