package com.luis.alhendinfc.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayRfafTest {

    @Test
    fun url_isGlobalFootballClubAndNotARivalLink() {
        assertEquals("https://www.footballclub.pro/main-fc", PlayRfaf.URL)
        assertTrue(PlayRfaf.isOpenableUrl())
        assertTrue(PlayRfaf.URL.startsWith("https://www.footballclub.pro/"))
        assertFalse(RivalLinkRules.isKnownType("PLAY_RFAF"))
        assertEquals(
            "Buscar partidos grabados de U.D. Maracena",
            PlayRfaf.searchHint("U.D. Maracena")
        )
        assertNotEquals(PlayRfaf.URL, RfafStandings.URL)
        assertTrue(RfafStandings.URL.contains("NFG_VisClasificacion"))
    }
}
