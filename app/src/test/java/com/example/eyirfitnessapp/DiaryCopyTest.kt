package com.example.eyirfitnessapp

import com.example.eyirfitnessapp.domain.diary.DiaryLogic
import com.example.eyirfitnessapp.domain.model.FoodLog
import com.example.eyirfitnessapp.domain.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Copy food / copy meal / copy previous meal (spec §2 Food Logging). */
class DiaryCopyTest {

    private val day = "2026-09-29"

    private fun log(
        name: String,
        meal: MealType = MealType.BREAKFAST,
        date: String = day,
        id: Long = 1L
    ) = FoodLog(
        id = id,
        foodId = 10L,
        name = name,
        mealType = meal,
        quantity = 2.0,
        servingSize = 100.0,
        servingUnit = "g",
        calories = 200.0,
        protein = 20.0,
        carbs = 30.0,
        fat = 5.0,
        date = date,
        loggedAt = 1_000L,
        synced = true
    )

    @Test
    fun `duplicated entry becomes a fresh row`() {
        val copy = DiaryLogic.duplicated(log("Oats"), date = day, now = 5_000L)
        assertEquals(0L, copy.id)
        assertEquals(day, copy.date)
        assertEquals(MealType.BREAKFAST, copy.mealType)
        assertEquals(5_000L, copy.loggedAt)
        assertEquals(false, copy.synced)
        assertEquals("Oats", copy.name)
        assertEquals(200.0, copy.calories, 0.0)
    }

    @Test
    fun `duplicated entry can move to another meal`() {
        val copy = DiaryLogic.duplicated(log("Oats"), date = day, meal = MealType.LUNCH, now = 5_000L)
        assertEquals(MealType.LUNCH, copy.mealType)
        assertEquals(day, copy.date)
    }

    @Test
    fun `duplicated entry keeps nutrition and serving but never the row id`() {
        val source = log("Yogurt", id = 42L)
        val copy = DiaryLogic.duplicated(source, date = day, now = 9_000L)
        assertNotEquals(source.id, copy.id)
        assertEquals(source.quantity, copy.quantity, 0.0)
        assertEquals(source.servingSize, copy.servingSize, 0.0)
        assertEquals(source.protein, copy.protein, 0.0)
        assertEquals(source.carbs, copy.carbs, 0.0)
        assertEquals(source.fat, copy.fat, 0.0)
        assertEquals(source.foodId, copy.foodId)
    }

    @Test
    fun `mealLogs picks only that meal on that day`() {
        val logs = listOf(
            log("Oats", MealType.BREAKFAST),
            log("Eggs", MealType.BREAKFAST),
            log("Salad", MealType.LUNCH),
            log("Oats", MealType.BREAKFAST, date = "2026-09-28")
        )
        val breakfast = DiaryLogic.mealLogs(logs, day, MealType.BREAKFAST)
        assertEquals(2, breakfast.size)
        assertTrue(breakfast.all { it.mealType == MealType.BREAKFAST && it.date == day })
    }

    @Test
    fun `copyMealLogs duplicates only the source meal`() {
        val logs = listOf(
            log("Oats", MealType.BREAKFAST),
            log("Eggs", MealType.BREAKFAST),
            log("Salad", MealType.LUNCH)
        )
        val copies = DiaryLogic.copyMealLogs(logs, day, MealType.BREAKFAST, MealType.DINNER)
        assertEquals(2, copies.size)
        assertTrue(copies.all { it.mealType == MealType.DINNER && it.date == day && it.id == 0L })
        assertEquals(setOf("Oats", "Eggs"), copies.map { it.name }.toSet())
    }

    @Test
    fun `copyMealLogs from an empty meal copies nothing`() {
        val copies = DiaryLogic.copyMealLogs(listOf(log("Salad", MealType.LUNCH)), day, MealType.DINNER, MealType.LUNCH)
        assertTrue(copies.isEmpty())
    }

    @Test
    fun `previousDay rolls the date back one day`() {
        assertEquals("2026-09-28", DiaryLogic.previousDay(day))
        assertEquals("2026-01-01", DiaryLogic.previousDay("2026-01-02"))
    }

    @Test
    fun `previousMealLogs brings yesterday's meal onto today`() {
        val yesterday = DiaryLogic.previousDay(day)
        val previousLogs = listOf(
            log("Oats", MealType.BREAKFAST, date = yesterday),
            log("Salad", MealType.LUNCH, date = yesterday),
            log("Old breakfast", MealType.BREAKFAST, date = "2026-09-20")
        )
        val copies = DiaryLogic.previousMealLogs(previousLogs, day, MealType.BREAKFAST)
        assertEquals(1, copies.size)
        assertEquals("Oats", copies.single().name)
        assertEquals(day, copies.single().date)
        assertEquals(MealType.BREAKFAST, copies.single().mealType)
        assertEquals(0L, copies.single().id)
    }

    @Test
    fun `previousMealLogs copies nothing when yesterday's meal was empty`() {
        val yesterday = DiaryLogic.previousDay(day)
        val copies = DiaryLogic.previousMealLogs(
            listOf(log("Salad", MealType.LUNCH, date = yesterday)),
            day,
            MealType.DINNER
        )
        assertTrue(copies.isEmpty())
    }

    @Test
    fun `copied rows keep the original logged order`() {
        val logs = listOf(log("First", id = 1L), log("Second", id = 2L), log("Third", id = 3L))
        val copies = DiaryLogic.copyMealLogs(logs, day, MealType.BREAKFAST, MealType.LUNCH)
        assertEquals(listOf("First", "Second", "Third"), copies.map { it.name })
    }
}
