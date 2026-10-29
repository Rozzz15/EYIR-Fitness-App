package com.example.eyirfitnessapp.util

import com.example.eyirfitnessapp.domain.model.WeekStartDay
import org.junit.Assert.assertEquals
import org.junit.Test

class DatesTest {
    @Test
    fun `addDays crosses month boundary`() {
        assertEquals("2026-03-01", Dates.addDays("2026-02-28", 1))
    }

    @Test
    fun `lastDays returns n days ending at end date`() {
        val days = Dates.lastDays("2026-01-07", 7)
        assertEquals(7, days.size)
        assertEquals("2026-01-01", days.first())
        assertEquals("2026-01-07", days.last())
    }

    // 2026-09-29 is a Tuesday; 2026-10-04 is a Sunday.

    @Test
    fun `weekStart defaults to Monday`() {
        assertEquals("2026-09-28", Dates.weekStart("2026-09-29"))
    }

    @Test
    fun `weekStart honours Monday start`() {
        assertEquals("2026-09-28", Dates.weekStart("2026-09-29", WeekStartDay.MONDAY))
        assertEquals("2026-09-28", Dates.weekStart("2026-10-04", WeekStartDay.MONDAY))
        assertEquals("2026-10-05", Dates.weekStart("2026-10-05", WeekStartDay.MONDAY))
    }

    @Test
    fun `weekStart honours Sunday start`() {
        assertEquals("2026-09-27", Dates.weekStart("2026-09-29", WeekStartDay.SUNDAY))
        assertEquals("2026-10-04", Dates.weekStart("2026-10-04", WeekStartDay.SUNDAY))
        assertEquals("2026-09-27", Dates.weekStart("2026-10-03", WeekStartDay.SUNDAY))
    }

    @Test
    fun `weekStart is idempotent on the start day itself`() {
        assertEquals("2026-09-28", Dates.weekStart("2026-09-28", WeekStartDay.MONDAY))
        assertEquals("2026-09-27", Dates.weekStart("2026-09-27", WeekStartDay.SUNDAY))
    }
}
