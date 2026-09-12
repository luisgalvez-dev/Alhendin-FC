package com.luis.alhendinfc.domain.model

import com.luis.alhendinfc.data.sync.AttachmentParentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RivalFichaTest {

    @Test
    fun shieldInitials_prefersShortName() {
        assertEquals("M", RivalFicha.shieldInitials("U.D. Maracena", "Maracena"))
        assertEquals("UM", RivalFicha.shieldInitials("U.D. Maracena"))
        assertEquals("P", RivalFicha.shieldInitials("   "))
    }

    @Test
    fun emptyStates_arePlainSpanish() {
        assertEquals("No hay jugadores añadidos", RivalFicha.emptyPlayers(queryBlank = true))
        assertEquals("Ningún jugador coincide con la búsqueda.", RivalFicha.emptyPlayers(queryBlank = false))
        assertEquals("No hay análisis del rival", RivalFicha.emptyAnalysis())
        assertEquals("No hay archivos", RivalFicha.emptyFiles())
        assertEquals("No hay informes de partido", RivalFicha.emptyReports())
        assertEquals("No hay enlaces configurados", RivalFicha.emptyLinks())
        assertEquals("1 jugador", RivalFicha.playerCountLabel(1))
        assertEquals("4 jugadores", RivalFicha.playerCountLabel(4))
    }

    @Test
    fun linkDisplay_hidesLongUrlAndShowsHost() {
        val link = RivalLink(
            opponentClubId = 1,
            type = RivalLinkType.YOUTUBE,
            label = "Resumen jornada 4",
            url = "https://www.youtube.com/watch?v=abc123very-long-id"
        )
        assertEquals("Resumen jornada 4", RivalFicha.linkTitle(link))
        assertEquals("YouTube · youtube.com", RivalFicha.linkSubtitle(link))
        assertEquals(RivalFicha.LinkGlyph.PLAY, RivalFicha.linkGlyph(RivalLinkType.YOUTUBE))
        assertEquals(RivalFicha.LinkGlyph.STAR, RivalFicha.linkGlyph(RivalLinkType.RFAF))
        assertEquals(RivalFicha.LinkGlyph.INFO, RivalFicha.linkGlyph(RivalLinkType.CUSTOM))
        val unlabeled = link.copy(label = "  ")
        assertEquals("YouTube", RivalFicha.linkTitle(unlabeled))
    }

    @Test
    fun analysis_detectsEmptyAndPreview() {
        val empty = RivalAnalysis(opponentClubId = 3)
        assertFalse(RivalFicha.analysisHasContent(empty))
        assertFalse(RivalFicha.analysisHasContent(null))
        assertEquals(0, RivalFicha.analysisFilledCount(empty))
        assertNull(RivalFicha.analysisPreview(empty))

        val filled = empty.copy(
            usualSystem = "1-4-3-3",
            strengths = "Presión alta",
            updatedAt = 1_726_142_400_000L
        )
        assertTrue(RivalFicha.analysisHasContent(filled))
        assertEquals(2, RivalFicha.analysisFilledCount(filled))
        assertTrue(RivalFicha.analysisPreview(filled)!!.startsWith("Sistema habitual: 1-4-3-3"))
        assertTrue(RivalFicha.analysisUpdatedLabel(filled.updatedAt)!!.startsWith("Actualizado el "))
        assertNull(RivalFicha.analysisUpdatedLabel(0L))
    }

    @Test
    fun kitSwatches_fromNamesAndHex() {
        val swatches = RivalFicha.kitSwatches("Blanco y verde #1565C0")
        assertEquals(3, swatches.size)
        assertTrue(swatches.any { it.argb == 0xFFF5F5F5L })
        assertTrue(swatches.any { it.argb == 0xFF2E7D32L })
        assertTrue(swatches.any { it.argb == 0xFF1565C0L })
        assertTrue(RivalFicha.kitSwatches("equipación titular").isEmpty())
        assertEquals("blanco y verde", RivalFicha.kitLine("  blanco y verde  "))
        assertNull(RivalFicha.stadiumLine(" "))
    }

    @Test
    fun filesAndReports_sortedNewestFirstWithoutTechnicalPaths() {
        val older = Attachment(
            id = 1,
            parentType = AttachmentParentType.OPPONENT,
            parentSyncId = "club",
            mimeType = "application/pdf",
            name = "scouting.pdf",
            remotePath = "secret/remote.pdf",
            createdAt = 100L,
            updatedAt = 100L
        )
        val newer = older.copy(id = 2, name = "foto.png", mimeType = "image/png", updatedAt = 200L)
        assertEquals(listOf(2, 1), RivalFicha.sortAttachments(listOf(older, newer)).map { it.id })
        assertEquals("scouting.pdf", RivalFicha.fileDisplayName(older))
        assertEquals("PDF", older.typeLabel)
        assertEquals("Imagen", newer.typeLabel)

        val matchOld = Match(
            id = 1,
            teamId = 1,
            rival = "Maracena",
            date = "01/09/2026",
            opponentClubId = 3,
            syncId = "m-old"
        )
        val matchNew = matchOld.copy(id = 2, date = "12/09/2026", syncId = "m-new")
        val reports = listOf(
            MatchReportRef(matchOld, older),
            MatchReportRef(matchNew, newer)
        )
        assertEquals(listOf("m-new", "m-old"), RivalFicha.sortReports(reports).map { it.match.syncId })
    }
}
