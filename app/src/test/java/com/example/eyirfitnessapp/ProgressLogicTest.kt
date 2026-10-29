package com.example.eyirfitnessapp

import com.example.eyirfitnessapp.domain.progress.ProgressLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec §11 Progress Chart ranges + §11 Metrics. */
class ProgressLogicTest {

    private val today = "2026-09-29"

    // ── Windows ──────────────────────────────────────────────────────────────

    @Test
    fun `seven day window ends today and has seven entries`() {
        val days = ProgressLogic.window(ProgressLogic.ChartRange.DAYS_7, today)
        assertEquals(7, days.size)
        assertEquals(today, days.last())
        assertEquals("2026-09-23", days.first())
    }

    @Test
    fun `thirty day window spans a month`() {
        val days = ProgressLogic.window(ProgressLogic.ChartRange.DAYS_30, today)
        assertEquals(30, days.size)
        assertEquals("2026-08-31", days.first())
    }

    @Test
    fun `all time window is empty marker`() {
        assertTrue(ProgressLogic.window(ProgressLogic.ChartRange.ALL, today).isEmpty())
    }

    @Test
    fun `every range label round trips`() {
        ProgressLogic.ChartRange.entries.forEach { range ->
            assertEquals(range, ProgressLogic.ChartRange.fromLabel(range.label))
        }
    }

    @Test
    fun `unknown label falls back to seven days`() {
        assertEquals(ProgressLogic.ChartRange.DAYS_7, ProgressLogic.ChartRange.fromLabel("nope"))
    }

    // ── Bucketing ────────────────────────────────────────────────────────────

    @Test
    fun `short ranges are daily`() {
        assertEquals(1, ProgressLogic.bucketSize(ProgressLogic.ChartRange.DAYS_7))
        assertEquals(1, ProgressLogic.bucketSize(ProgressLogic.ChartRange.DAYS_30))
    }

    @Test
    fun `ninety days is weekly and a year is monthly`() {
        assertEquals(7, ProgressLogic.bucketSize(ProgressLogic.ChartRange.DAYS_90))
        assertEquals(30, ProgressLogic.bucketSize(ProgressLogic.ChartRange.YEAR_1))
    }

    @Test
    fun `downsample averages each bucket`() {
        val daily = listOf(
            "d1" to 100.0,
            "d2" to 200.0,
            "d3" to 300.0,
            "d4" to 400.0
        )
        val buckets = ProgressLogic.downsample(daily, bucketDays = 2)
        assertEquals(2, buckets.size)
        assertEquals(150.0, buckets[0].second, 0.01)
        assertEquals(350.0, buckets[1].second, 0.01)
        assertEquals("d1", buckets[0].first)
    }

    @Test
    fun `downsample drops empty buckets so charts never draw fake zeros`() {
        val daily = listOf("d1" to 0.0, "d2" to 0.0, "d3" to 250.0, "d4" to 250.0)
        val buckets = ProgressLogic.downsample(daily, bucketDays = 2)
        assertEquals(1, buckets.size)
        assertEquals(250.0, buckets[0].second, 0.01)
    }

    @Test
    fun `daily bucket keeps only logged days`() {
        val daily = listOf("d1" to 0.0, "d2" to 120.0, "d3" to 0.0)
        val kept = ProgressLogic.downsample(daily, bucketDays = 1)
        assertEquals(listOf("d2"), kept.map { it.first })
    }

    // ── Weight stats (§11 Metrics) ───────────────────────────────────────────

    @Test
    fun `weight stats computes change and weekly pace`() {
        // 30 days apart: -3.0 kg → weekly pace = -3.0 / (30/7)
        val stats = ProgressLogic.weightStats(
            weights = listOf("2026-08-30" to 80.0, "2026-09-29" to 77.0),
            goalKg = 75.0
        )
        assertEquals(80.0, stats.startKg!!, 0.01)
        assertEquals(77.0, stats.currentKg!!, 0.01)
        assertEquals(-3.0, stats.changeKg!!, 0.01)
        assertEquals(-3.0 / (30.0 / 7.0), stats.weeklyChangeKg!!, 0.01)
        assertEquals(2.0, stats.toGoalKg!!, 0.01)
        assertEquals(2, stats.entries)
    }

    @Test
    fun `weight stats without goal leaves remaining null`() {
        val stats = ProgressLogic.weightStats(listOf("2026-09-29" to 70.0), goalKg = null)
        assertNull(stats.toGoalKg)
    }

    @Test
    fun `weight stats on empty history is fully null`() {
        val stats = ProgressLogic.weightStats(emptyList(), goalKg = 70.0)
        assertNull(stats.startKg)
        assertNull(stats.changeKg)
        assertEquals(0, stats.entries)
    }

    @Test
    fun `single weigh-in has zero-span guard so pace is finite`() {
        val stats = ProgressLogic.weightStats(
            listOf("2026-09-29" to 70.0, "2026-09-29" to 70.5),
            goalKg = 70.0
        )
        assertTrue(stats.weeklyChangeKg!!.isFinite())
    }

    // ── Exercise minutes (§20 frequency chart) ───────────────────────────────

    @Test
    fun `cardio minutes are used as logged`() {
        assertEquals(45.0, ProgressLogic.effectiveMinutes(45.0, sets = null, reps = null), 0.01)
        assertEquals(30.0, ProgressLogic.effectiveMinutes(durationMinutes = 30.0), 0.01)
    }

    @Test
    fun `strength logs without a duration get a set-based estimate instead of zero`() {
        val minutes = ProgressLogic.effectiveMinutes(durationMinutes = null, sets = 4, reps = 10)
        assertTrue("lifting should still count minutes", minutes > 0.0)
        // 4 sets x 10 reps x 2.5 s/rep -> 6.7 min of work + 4 x 1.5 min rest
        assertEquals(4 * 10 * 2.5 / 60.0 * 4 + 4 * 1.5, minutes, 0.01)
    }

    @Test
    fun `logs with neither duration nor sets contribute nothing`() {
        assertEquals(0.0, ProgressLogic.effectiveMinutes(null, sets = null, reps = null), 0.0)
        assertEquals(0.0, ProgressLogic.effectiveMinutes(null, sets = 0, reps = 10), 0.0)
    }
}
