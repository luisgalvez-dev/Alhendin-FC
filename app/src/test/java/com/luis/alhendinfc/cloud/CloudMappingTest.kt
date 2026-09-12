package com.luis.alhendinfc.cloud

import com.luis.alhendinfc.data.local.MatchEntity
import com.luis.alhendinfc.data.local.MatchPlayerEntity
import com.luis.alhendinfc.data.local.PlayerEntity
import com.luis.alhendinfc.data.local.TeamEntity
import com.luis.alhendinfc.data.sync.LiveMatchGuard
import com.luis.alhendinfc.domain.model.MatchStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudMappingTest {

    @Test
    fun teamCloud_hasNoIsSelected_andStripsLocalShield() {
        val team = TeamEntity(
            id = 3,
            name = "Alhendin",
            category = "1",
            season = "25/26",
            shieldUri = "content://media/shield.png",
            isSelected = true,
            syncId = "team-sync",
            createdAt = 1L,
            updatedAt = 2L
        )
        val doc = CloudMappers.team(team)
        assertEquals("team-sync", doc.id)
        assertFalse(doc.data.containsKey("isSelected"))
        assertFalse(doc.data.containsKey("id"))
        assertFalse(doc.data.containsKey("teamId"))
        assertNull(doc.data["shieldUri"])
        CloudMappers.assertNoIntIdentity(doc.data)
    }

    @Test
    fun playerCloud_usesTeamSyncId_notPhoto() {
        val player = PlayerEntity(
            id = 9,
            teamId = 1,
            name = "Luis",
            photoUri = "file:///data/photo.jpg",
            syncId = "p-sync",
            createdAt = 1L,
            updatedAt = 2L
        )
        val doc = CloudMappers.player(player, "team-sync")
        assertEquals("team-sync", doc.data["teamSyncId"])
        assertFalse(doc.data.containsKey("teamId"))
        assertFalse(doc.data.containsKey("photoUri"))
        CloudMappers.assertNoIntIdentity(doc.data)
    }

    @Test
    fun matchCloud_excludesLiveAndLocalUri_finishedCarriesFieldSeconds() {
        val live = MatchEntity(
            id = 4,
            teamId = 1,
            rival = "Jaen",
            status = MatchStatus.LIVE.name,
            homeScore = 1,
            awayScore = 0,
            livePeriod = 2,
            liveElapsedSeconds = 900,
            liveClockRunning = true,
            fieldSecondsJson = "1:90",
            fieldPositionsJson = "1:0.5:0.5",
            rivalShieldUri = "/data/user/0/escudo.png",
            syncId = "m-live",
            createdAt = 1L,
            updatedAt = 2L
        )
        val liveDoc = CloudMappers.match(live, "team-sync", "club-sync")
        assertEquals("team-sync", liveDoc.data["teamSyncId"])
        assertEquals("club-sync", liveDoc.data["opponentClubSyncId"])
        assertFalse(liveDoc.data.containsKey("teamId"))
        assertFalse(liveDoc.data.containsKey("opponentClubId"))
        assertFalse(liveDoc.data.containsKey("liveElapsedSeconds"))
        assertFalse(liveDoc.data.containsKey("fieldPositionsJson"))
        assertFalse(liveDoc.data.containsKey("fieldSecondsJson"))
        assertNull(liveDoc.data["rivalShieldUri"])
        CloudMappers.assertNoIntIdentity(liveDoc.data)

        val finished = live.copy(
            status = MatchStatus.FINISHED.name,
            homeScore = 2,
            awayScore = 1,
            fieldSecondsJson = "final-clock"
        )
        val fin = CloudMappers.match(finished, "team-sync", null)
        assertEquals(2, fin.data["homeScore"])
        assertEquals(1, fin.data["awayScore"])
        assertEquals("FINISHED", fin.data["status"])
        assertEquals("final-clock", fin.data["fieldSecondsJson"])
        assertTrue(LiveMatchGuard.finishedCarriesFieldSeconds(finished.status, finished.fieldSecondsJson))
    }

    @Test
    fun matchPlayerCloud_hasNoIsOnField() {
        val row = MatchPlayerEntity(
            id = 1,
            matchId = 9,
            playerId = 5,
            callupStatus = "TITULAR",
            isOnField = true,
            syncId = "mp",
            createdAt = 1L,
            updatedAt = 1L
        )
        val doc = CloudMappers.matchPlayer(row, "match-sync", "player-sync")
        assertEquals("match-sync", doc.data["matchSyncId"])
        assertEquals("player-sync", doc.data["playerSyncId"])
        assertFalse(doc.data.containsKey("isOnField"))
        assertFalse(doc.data.containsKey("matchId"))
        assertFalse(doc.data.containsKey("playerId"))
        CloudMappers.assertNoIntIdentity(doc.data)
    }

    @Test
    fun attachmentCloud_neverIncludesLocalPath_andKeepsRemotePath() {
        val att = com.luis.alhendinfc.data.local.AttachmentEntity(
            id = 4,
            syncId = "att-sync-1",
            parentType = "TASK",
            parentSyncId = "task-sync",
            mimeType = "image/jpeg",
            name = "foto.jpg",
            localPath = "/data/user/0/files/foto.jpg",
            remotePath = "workspaces/alhendin-dev/attachments/att-sync-1/foto.jpg",
            createdAt = 1L,
            updatedAt = 2L
        )
        val doc = CloudMappers.attachment(att)
        assertEquals("att-sync-1", doc.id)
        assertEquals("TASK", doc.data["parentType"])
        assertEquals("task-sync", doc.data["parentSyncId"])
        assertEquals("image/jpeg", doc.data["mime"])
        assertEquals("foto.jpg", doc.data["name"])
        assertEquals("workspaces/alhendin-dev/attachments/att-sync-1/foto.jpg", doc.data["remotePath"])
        assertTrue(StoragePath.isPortableObjectPath(doc.str("remotePath")))
        assertFalse(doc.data.containsKey("localPath"))
        assertFalse(doc.data.containsKey("id"))
        CloudMappers.assertNoIntIdentity(doc.data)
        assertFalse(CloudUri.isLocal(doc.strOrNull("remotePath")))
    }

    @Test
    fun attachmentCloud_stripsLocalRemotePath() {
        val att = com.luis.alhendinfc.data.local.AttachmentEntity(
            syncId = "att-sync-2",
            parentType = "MATCH",
            parentSyncId = "match-sync",
            mimeType = "application/pdf",
            name = "informe.pdf",
            localPath = "content://media/informe.pdf",
            remotePath = "file:///data/informe.pdf",
            createdAt = 1L,
            updatedAt = 2L
        )
        val doc = CloudMappers.attachment(att)
        assertNull(doc.data["remotePath"])
        assertFalse(doc.data.containsKey("localPath"))
    }

    @Test
    fun playerPhotoSlot_mapsParentTypeWithoutLocalPath() {
        val att = com.luis.alhendinfc.data.local.AttachmentEntity(
            syncId = "att-photo-1",
            parentType = "PLAYER_PHOTO",
            parentSyncId = "player-sync",
            mimeType = "image/jpeg",
            name = "foto.jpg",
            localPath = "/data/user/0/files/foto.jpg",
            remotePath = "workspaces/alhendin-dev/attachments/att-photo-1/foto.jpg",
            createdAt = 1L,
            updatedAt = 2L
        )
        val doc = CloudMappers.attachment(att)
        assertEquals("PLAYER_PHOTO", doc.data["parentType"])
        assertEquals("player-sync", doc.data["parentSyncId"])
        assertFalse(doc.data.containsKey("localPath"))
        assertTrue(StoragePath.isPortableObjectPath(doc.str("remotePath")))
    }

    @Test
    fun rivalLinkCloud_keepsTypeAndUrl() {
        val row = com.luis.alhendinfc.data.local.RivalLinkEntity(
            id = 9,
            syncId = "lk-yt-1",
            opponentClubId = 4,
            type = "YOUTUBE",
            label = "YouTube",
            url = "https://example.com/dev/youtube",
            sortOrder = 0,
            createdAt = 1L,
            updatedAt = 2L
        )
        val doc = CloudMappers.rivalLink(row, "club-sync")
        assertEquals("lk-yt-1", doc.id)
        assertEquals("club-sync", doc.data["opponentClubSyncId"])
        assertEquals("YOUTUBE", doc.data["type"])
        assertEquals("https://example.com/dev/youtube", doc.data["url"])
        assertFalse(doc.data.containsKey("opponentClubId"))
        assertFalse(doc.data.containsKey("id"))
        CloudMappers.assertNoIntIdentity(doc.data)
    }
}
