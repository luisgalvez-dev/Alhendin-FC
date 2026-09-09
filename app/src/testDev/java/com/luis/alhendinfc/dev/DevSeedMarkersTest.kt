package com.luis.alhendinfc.dev

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DevSeedMarkersTest {

    @Test
    fun prefix_isStableAndDetectable() {
        assertEquals("[DEV] Presión tras pérdida", DevSeedMarkers.labeled("Presión tras pérdida"))
        assertTrue(DevSeedMarkers.isDemo("[DEV] Rival A"))
        assertEquals("[DEV] Ficha RFAF DEMO", DevSeedMarkers.labeled("Ficha RFAF DEMO"))
        assertFalse(DevSeedMarkers.isDemo("Jaén FS"))
        assertFalse(DevSeedMarkers.isDemo(null))
        assertFalse(DevSeedMarkers.isDemo("DEV Rival A"))
        assertTrue(DevSeedMarkers.isDemoTeam("[DEV] Equipo de prueba"))
        assertFalse(DevSeedMarkers.isDemoTeam("Equipo de prueba"))
        assertFalse(DevSeedMarkers.isDemoTeam("Alhendín CF"))
    }

    @Test
    fun nextFree_skipsOccupiedDaysAndMarksThem() {
        val occupied = mutableSetOf(10L, 11L)
        val past = DevSeedCalendar.nextFree(11L, occupied, -1L)
        assertEquals(9L, past)
        val future = DevSeedCalendar.nextFree(10L, occupied, 1L)
        assertEquals(12L, future)
        assertTrue(occupied.containsAll(listOf(9L, 10L, 11L, 12L)))
    }
}
