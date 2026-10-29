package com.example.eyirfitnessapp.util

import com.example.eyirfitnessapp.domain.model.WeekStartDay
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Dates {
    private const val PATTERN = "yyyy-MM-dd"

    // New formatter per call: SimpleDateFormat is not thread-safe, and these
    // helpers are invoked from arbitrary coroutines.
    private fun iso(): SimpleDateFormat = SimpleDateFormat(PATTERN, Locale.US)

    private fun parseOrNull(date: String): Date = try {
        iso().parse(date) ?: Date()
    } catch (e: ParseException) {
        Date()
    }

    fun today(): String = iso().format(Date())

    fun addDays(date: String, days: Int): String {
        val cal = Calendar.getInstance()
        cal.time = parseOrNull(date)
        cal.add(Calendar.DAY_OF_MONTH, days)
        return iso().format(cal.time)
    }

    fun weekdayShort(date: String): String =
        SimpleDateFormat("EEE", Locale.US).format(parseOrNull(date))

    /** Last n days ending at [endDate], oldest first. */
    fun lastDays(endDate: String, n: Int): List<String> =
        (n - 1 downTo 0).map { addDays(endDate, -it) }

    /** First day of the week containing [date], honouring the user's start day (spec §22). */
    fun weekStart(date: String, startDay: WeekStartDay = WeekStartDay.MONDAY): String {
        val cal = Calendar.getInstance()
        cal.time = parseOrNull(date)
        val dow = cal.get(Calendar.DAY_OF_WEEK) // Sunday = 1 … Saturday = 7
        val anchor = if (startDay == WeekStartDay.SUNDAY) Calendar.SUNDAY else Calendar.MONDAY
        val back = (dow - anchor + 7) % 7
        return addDays(date, -back)
    }

    fun prettyDate(date: String): String =
        SimpleDateFormat("EEE, MMM d", Locale.US).format(parseOrNull(date))

    /** Epoch millis for midnight local time of [date]; malformed input → 0. */
    fun parseMillis(date: String): Long = try {
        iso().parse(date)?.time ?: 0L
    } catch (e: ParseException) {
        0L
    }

    /** yyyy-MM-dd for an epoch-millis timestamp (local time). */
    fun isoOf(millis: Long): String = iso().format(Date(millis))
}
