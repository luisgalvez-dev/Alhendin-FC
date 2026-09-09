package com.luis.alhendinfc.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RfafStandingsTest {

    @Test
    fun url_isSingleHttpsConstant() {
        assertTrue(RfafStandings.URL.startsWith("https://www.rfaf.es/"))
        assertTrue(RfafStandings.isOpenableUrl())
        assertTrue(RfafStandings.URL.contains("NFG_VisClasificacion"))
        assertTrue(RfafStandings.URL.contains("codcompeticion=48465911"))
        assertFalse(RfafStandings.isOpenableUrl("rfaf.es"))
        assertNull(OpponentPlayerRules.validate("Antonio Pérez"))
        assertEquals("El nombre es obligatorio", OpponentPlayerRules.validate("  "))
    }
}
