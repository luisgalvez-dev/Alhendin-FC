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

    @Test
    fun sameClubSyncId_differentRoomIds_resolvesTheReport() {
        val migueMatch = match(
            syncId = "m-maracena",
            opponentClubId = 4,
            opponentClubSyncId = "club-maracena",
            rival = "U.D. Maracena"
        )
        val report = att(1, "m-maracena", "application/pdf", "informe.pdf")
        val analistaClub = OpponentClub(
            id = 7,
            teamId = 1,
            name = "U.D. Maracena",
            syncId = "club-maracena"
        )
        val found = MatchReports.forOpponent(
            listOf(migueMatch),
            listOf(report),
            analistaClub
        )
        assertEquals(1, found.size)
        assertSame(report, found[0].attachment)
        assertTrue(MatchReports.belongsToOpponent(migueMatch, analistaClub))
    }

    @Test
    fun distinctClubs_doNotMixReports() {
        val matchA = match(syncId = "m-a", opponentClubId = 4, opponentClubSyncId = "club-a")
        val matchB = match(syncId = "m-b", opponentClubId = 7, opponentClubSyncId = "club-b")
        val reportA = att(1, "m-a", "application/pdf", "a.pdf")
        val reportB = att(2, "m-b", "application/pdf", "b.pdf")
        val clubA = OpponentClub(id = 99, teamId = 1, name = "A", syncId = "club-a")
        val clubB = OpponentClub(id = 100, teamId = 1, name = "B", syncId = "club-b")
        val all = listOf(reportA, reportB)
        assertEquals(listOf(1), MatchReports.forOpponent(listOf(matchA, matchB), all, clubA).map { it.attachment.id })
        assertEquals(listOf(2), MatchReports.forOpponent(listOf(matchA, matchB), all, clubB).map { it.attachment.id })
    }

    @Test
    fun legacyLocalId_withoutPortableSync_stillResolvesOnSameDevice() {
        val match = match(syncId = "m-legacy", opponentClubId = 4, opponentClubSyncId = null)
        val report = att(1, "m-legacy", "application/pdf", "legacy.pdf")
        val club = OpponentClub(id = 4, teamId = 1, name = "Maracena", syncId = "")
        val found = MatchReports.forOpponent(listOf(match), listOf(report), club)
        assertEquals(1, found.size)
        assertSame(report, found[0].attachment)
    }

    @Test
    fun legacyWithoutStableRef_doesNotCrashOrInventLink() {
        val match = match(syncId = "m-orphan", opponentClubId = null, opponentClubSyncId = null)
        val report = att(1, "m-orphan", "application/pdf", "x.pdf")
        val club = OpponentClub(id = 7, teamId = 1, name = "Maracena", syncId = "club-maracena")
        assertTrue(MatchReports.forOpponent(listOf(match), listOf(report), club).isEmpty())
        assertTrue(MatchReports.forOpponent(emptyList(), emptyList(), club).isEmpty())
    }

    @Test
    fun matchAttachment_isNotDuplicatedAcrossDevices() {
        val match = match(syncId = "m-shared", opponentClubId = 4, opponentClubSyncId = "club-maracena")
        val report = att(1, "m-shared", "application/pdf", "unico.pdf")
        val migue = OpponentClub(id = 4, teamId = 1, name = "Maracena", syncId = "club-maracena")
        val analista = OpponentClub(id = 7, teamId = 1, name = "Maracena", syncId = "club-maracena")
        val fromMigue = MatchReports.forOpponent(listOf(match), listOf(report), migue)
        val fromAnalista = MatchReports.forOpponent(listOf(match), listOf(report), analista)
        assertEquals(1, fromMigue.size)
        assertEquals(1, fromAnalista.size)
        assertSame(fromMigue[0].attachment, fromAnalista[0].attachment)
        assertEquals("att-1", fromAnalista[0].attachment.syncId)
    }

    private fun match(
        syncId: String,
        opponentClubId: Int?,
        rival: String = "Rival A",
        isHome: Boolean = true,
        date: String = "08/09/2026",
        opponentClubSyncId: String? = null
    ) = Match(
        id = 1,
        teamId = 1,
        rival = rival,
        date = date,
        isHome = isHome,
        opponentClubId = opponentClubId,
        opponentClubSyncId = opponentClubSyncId,
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
