package com.example.eyirfitnessapp.domain.progress

import com.example.eyirfitnessapp.util.Dates

/**
 * Pure helpers behind the progress chart range selector (spec §11 "7 days /
 * 30 days / 90 days / 1 year / All time"). Kept free of Android/UI code so
 * range windows and downsampling are unit-testable (§34 rule 2).
 */
object ProgressLogic {

    /** Supported chart ranges (spec §11 Progress Chart). */
    enum class ChartRange(val label: String, val days: Int?) {
        DAYS_7("7 days", 7),
        DAYS_30("30 days", 30),
        DAYS_90("90 days", 90),
        YEAR_1("1 year", 365),
        ALL("All time", null);

        companion object {
            fun fromLabel(label: String): ChartRange = entries.firstOrNull { it.label == label } ?: DAYS_7
        }
    }

    /** Inclusive day window ending at [endDate]; null days = all recorded history. */
    fun window(range: ChartRange, endDate: String): List<String> {
        val n = range.days ?: return emptyList()
        return Dates.lastDays(endDate, n)
    }

    /**
     * Picks the bucket size for the range: daily points for short ranges,
     * weekly for a quarter, monthly for a year or more.
     */
    fun bucketSize(range: ChartRange): Int = when (range) {
        ChartRange.DAYS_7, ChartRange.DAYS_30 -> 1
        ChartRange.DAYS_90 -> 7
        ChartRange.YEAR_1, ChartRange.ALL -> 30
    }

    /**
     * Averages each bucket of [dailyValues] (already aligned to the window,
     * oldest first). Buckets with no data are dropped, so charts never draw a
     * fake zero. Value = average, label = first day of the bucket.
     */
    fun downsample(
        dailyValues: List<Pair<String, Double>>,
        bucketDays: Int
    ): List<Pair<String, Double>> {
        if (bucketDays <= 1) return dailyValues.filter { it.second > 0.0 }
        return dailyValues
            .chunked(bucketDays)
            .mapNotNull { chunk ->
                val filled = chunk.filter { it.second > 0.0 }
                if (filled.isEmpty()) {
                    null
                } else {
                    chunk.first().first to filled.sumOf { it.second } / filled.size
                }
            }
    }

    /**
     * Minutes to count for one exercise log in the "exercise minutes" chart.
     * Strength entries carry sets/reps instead of a duration, so falling back
     * to 0 would make a lifting-only week look like no training at all.
     * Uses the same estimate [AppRepository.logRoutine] applies when it derives
     * calories for a routine line.
     */
    fun effectiveMinutes(
        durationMinutes: Double?,
        sets: Int? = null,
        reps: Int? = null
    ): Double = when {
        durationMinutes != null && durationMinutes > 0 -> durationMinutes
        sets != null && sets > 0 -> sets * (reps ?: 10) * 2.5 / 60.0 * 4 + sets * 1.5
        else -> 0.0
    }

    /** Start / end / change / weekly pace for a weight series, oldest first. */
    data class WeightStats(
        val startKg: Double?,
        val currentKg: Double?,
        val changeKg: Double?,
        val weeklyChangeKg: Double?,
        val toGoalKg: Double?,
        val entries: Int
    )

    fun weightStats(
        weights: List<Pair<String, Double>>,   // (date, kg) oldest first
        goalKg: Double?
    ): WeightStats {
        if (weights.isEmpty()) {
            return WeightStats(null, null, null, null, null, 0)
        }
        val first = weights.first()
        val last = weights.last()
        val change = last.second - first.second
        val spanDays = ((Dates.parseMillis(last.first) - Dates.parseMillis(first.first)) / 86_400_000L)
            .coerceAtLeast(1L)
        val weeks = spanDays / 7.0
        return WeightStats(
            startKg = first.second,
            currentKg = last.second,
            changeKg = change,
            weeklyChangeKg = change / weeks,
            toGoalKg = goalKg?.let { last.second - it },
            entries = weights.size
        )
    }
}
