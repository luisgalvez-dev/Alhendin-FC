package com.luis.alhendinfc.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskRulesTest {

    @Test
    fun blankName_isRejected() {
        assertEquals("El nombre es obligatorio", TaskRules.validate("", null, null))
        assertEquals("El nombre es obligatorio", TaskRules.validate("   ", 4, 10))
    }

    @Test
    fun playerCountZeroOrNegative_isRejectedWhenSpecified() {
        assertEquals(
            "El número de jugadores debe ser mayor que 0",
            TaskRules.validate("Presión", 0, null)
        )
        assertEquals(
            "El número de jugadores debe ser mayor que 0",
            TaskRules.validate("Presión", -2, 10)
        )
    }

    @Test
    fun durationZeroOrNegative_isRejectedWhenSpecified() {
        assertEquals(
            "La duración debe ser mayor que 0 minutos",
            TaskRules.validate("Presión", null, 0)
        )
        assertEquals(
            "La duración debe ser mayor que 0 minutos",
            TaskRules.validate("Presión", 8, -1)
        )
    }

    @Test
    fun unspecifiedCounts_areAllowed() {
        assertNull(TaskRules.validate("Presión", null, null))
        assertNull(TaskRules.validate("Presión", 8, 12))
    }

    @Test
    fun searchIsPartialAndCaseInsensitive() {
        assertTrue(TaskRules.matchesName("Presión tras pérdida", "pres"))
        assertTrue(TaskRules.matchesName("Presión tras pérdida", "PRES"))
        assertTrue(TaskRules.matchesName("Presión tras pérdida", "sión"))
        assertFalse(TaskRules.matchesName("Presión tras pérdida", "rondo"))
        assertTrue(TaskRules.matchesName("Cualquiera", "  "))
    }
}
