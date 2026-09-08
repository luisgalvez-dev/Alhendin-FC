package com.luis.alhendinfc.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarDateTest {

    @Test
    fun paddedAndUnpadded_sameEpochDay() {
        val a = CalendarDate.toEpochDay("07/09/2026")
        val b = CalendarDate.toEpochDay("7/9/2026")
        assertEquals(a, b)
        assertEquals(java.time.LocalDate.of(2026, 9, 7).toEpochDay(), a)
    }

    @Test
    fun invalidDates_returnNull() {
        assertNull(CalendarDate.toEpochDay(""))
        assertNull(CalendarDate.toEpochDay("   "))
        assertNull(CalendarDate.toEpochDay("abc"))
        assertNull(CalendarDate.toEpochDay("31/02/2024"))
        assertNull(CalendarDate.toEpochDay("Fecha por definir"))
        assertNull(CalendarDate.toEpochDay("07-09-2026"))
    }

    @Test
    fun doesNotRewriteInput() {
        val original = "07/09/2026"
        CalendarDate.toEpochDay(original)
        assertEquals("07/09/2026", original)
    }

    @Test
    fun chronologicalOrder_januaryAfterDecemberLexicographic() {
        val jan = CalendarDate.toEpochDay("31/01/2026")!!
        val feb = CalendarDate.toEpochDay("01/02/2026")!!
        assertTrue(jan < feb)
        assertTrue("31/01/2026" > "01/02/2026")
    }
}
