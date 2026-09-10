package com.luis.alhendinfc.domain.model

import com.luis.alhendinfc.data.sync.AttachmentParentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchReportsTest {

    @Test
    fun severalAttachmentsStayOnTheSameMatch() {
        val match = match(syncId = "m-a", opponentClubId = 1)
        val pdf = att(1, "m-a", "application/pdf", "Informe postpartido.pdf")
        val img = att(2, "m-a", "image/jpeg", "foto.jpg")
        val doc = att(3, "m-a", "application/msword", "notas.doc")
        val found = MatchReports.activeForMatch(listOf(pdf, img, doc), "m-a")
        assertEquals(listOf(1, 2, 3), found.map { it.id })
        assertTrue(found[0].isPdf)
        assertTrue(found[1].isImage)
        assertEquals("Documento", found[2].typeLabel)
    }

    @Test
    fun onlyTheCorrectMatchAndRivalSeeTheReport() {
        val matchA = match(syncId = "m-a", opponentClubId = 10, rival = "Rival A", date = "08/09/2026")
        val matchB = match(syncId = "m-b", opponentClubId = 20, rival = "Rival B", date = "09/09/2026")
        val reportA = att(1, "m-a", "application/pdf", "Informe postpartido.pdf")
        val reportB = att(2, "m-b", "application/pdf", "otro.pdf")
        val all = listOf(reportA, reportB)

        assertEquals(listOf(1), MatchReports.activeForMatch(all, "m-a").map { it.id })
        assertEquals(emptyList<Int>(), MatchReports.activeForMatch(all, "m-other").map { it.id })

        val forA = MatchReports.forOpponent(listOf(matchA, matchB), all, opponentClubId = 10)
        assertEquals(1, forA.size)
        assertEquals("Alhendín - Rival A · 08/09/2026", forA[0].matchHeading)
        assertSame(reportA, forA[0].attachment)

        val forB = MatchReports.forOpponent(listOf(matchA, matchB), all, opponentClubId = 20)
        assertEquals(listOf(2), forB.map { it.attachment.id })
        assertTrue(MatchReports.forOpponent(listOf(matchA, matchB), all, opponentClubId = 99).isEmpty())
    }

    @Test
    fun tombstoneAndAwayHeading_doNotDuplicateAttachment() {
        val match = match(syncId = "m-a", opponentClubId = 10, rival = "Rival A", isHome = false, date = "08/09/2026")
        val live = att(1, "m-a", "application/pdf", "a.pdf")
        val dead = att(2, "m-a", "application/pdf", "old.pdf", deletedAt = 9L)
        val refs = MatchReports.forOpponent(listOf(match), listOf(live, dead), 10)
        assertEquals(1, refs.size)
        assertSame(live, refs[0].attachment)
        assertEquals("Rival A - Alhendín · 08/09/2026", refs[0].matchHeading)
    }

    private fun match(
        syncId: String,
        opponentClubId: Int,
        rival: String = "Rival A",
        isHome: Boolean = true,
        date: String = "08/09/2026"
    ) = Match(
        id = 1,
        teamId = 1,
        rival = rival,
        date = date,
        isHome = isHome,
        opponentClubId = opponentClubId,
        syncId = syncId
    )

    private fun att(
        id: Int,
        parentSyncId: String,
        mime: String,
        name: String,
        deletedAt: Long? = null
    ) = Attachment(
        id = id,
        syncId = "att-$id",
        parentType = AttachmentParentType.MATCH,
        parentSyncId = parentSyncId,
        mimeType = mime,
        name = name,
        localPath = "/files/$name",
        deletedAt = deletedAt
    )
}
