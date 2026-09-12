package com.luis.alhendinfc.domain.model

import com.luis.alhendinfc.data.sync.AttachmentParentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedMediaTest {

    @Test
    fun playerWithoutShared_usesLegacyThenNull() {
        val player = Player(id = 1, teamId = 1, name = "Luis", photoUri = "content://media/a.jpg")
        assertEquals("content://media/a.jpg", SharedMedia.displayPath(null, player.photoUri))
        assertNull(SharedMedia.displayPath(null, null))
        assertNull(SharedMedia.displayPath(null, "  "))
    }

    @Test
    fun sharedLocalPath_beatsLegacyUri() {
        val shared = slot(AttachmentParentType.PLAYER_PHOTO, "player-a", "/data/files/new.jpg")
        assertEquals(
            "/data/files/new.jpg",
            SharedMedia.displayPath(shared, "content://media/old.jpg")
        )
    }

    @Test
    fun contentUri_isPicker_andNotPortableLegacy() {
        assertTrue(SharedMedia.isPickerUri("content://media/picker"))
        assertFalse(SharedMedia.isPickerUri("/data/user/0/files/escudo.png"))
        assertNull(SharedMedia.persistableLegacyUri("content://media/picker"))
        assertEquals("/data/user/0/files/escudo.png", SharedMedia.persistableLegacyUri("/data/user/0/files/escudo.png"))
        assertFalse(com.luis.alhendinfc.cloud.CloudUri.isLocal("workspaces/ws/attachments/x/a.png"))
        assertTrue(com.luis.alhendinfc.cloud.CloudUri.isLocal("content://media/old.jpg"))
        assertNull(com.luis.alhendinfc.cloud.CloudUri.portableOrNull("content://media/old.jpg"))
        assertNull(com.luis.alhendinfc.cloud.CloudUri.portableOrNull("file:///data/photo.jpg"))
        assertNull(com.luis.alhendinfc.cloud.CloudUri.portableOrNull("/data/user/0/escudo.png"))
    }

    @Test
    fun byParentSyncId_keepsPlayersIndependent() {
        val a = slot(AttachmentParentType.PLAYER_PHOTO, "player-a", "/a.jpg")
        val b = slot(AttachmentParentType.PLAYER_PHOTO, "player-b", "/b.jpg")
        val map = SharedMedia.byParentSyncId(listOf(a, b))
        assertEquals("/a.jpg", map["player-a"]?.localPath)
        assertEquals("/b.jpg", map["player-b"]?.localPath)
        assertNotEquals(map["player-a"]?.parentSyncId, map["player-b"]?.parentSyncId)
    }

    @Test
    fun tombstonedShared_isIgnoredForDisplay() {
        val dead = slot(AttachmentParentType.TEAM_SHIELD, "team-1", "/new.png").copy(deletedAt = 9L)
        assertEquals("legacy://old", SharedMedia.displayPath(dead, "legacy://old"))
    }

    @Test
    fun matchWithOpponentClub_usesClubSharedNotMatchCopy() {
        val clubShared = slot(AttachmentParentType.OPPONENT_SHIELD, "club-a", "/club.png")
        val path = SharedMedia.rivalDisplayPath(
            opponentClubId = 9,
            clubLegacyUri = "content://legacy-club.png",
            matchLegacyUri = "content://match-copy.png",
            clubShared = clubShared
        )
        assertEquals("/club.png", path)
    }

    @Test
    fun matchWithOpponentClub_withoutShared_usesClubLegacyNotMatchCopy() {
        val path = SharedMedia.rivalDisplayPath(
            opponentClubId = 9,
            clubLegacyUri = "file:///club.png",
            matchLegacyUri = "content://match-copy.png",
            clubShared = null
        )
        assertEquals("file:///club.png", path)
    }

    @Test
    fun matchWithoutOpponentClub_keepsMatchLegacy() {
        val path = SharedMedia.rivalDisplayPath(
            opponentClubId = null,
            clubLegacyUri = "file:///club.png",
            matchLegacyUri = "content://match-only.png",
            clubShared = slot(AttachmentParentType.OPPONENT_SHIELD, "club-a", "/club.png")
        )
        assertEquals("content://match-only.png", path)
    }

    @Test
    fun matchWithZeroClubId_doesNotDropMatchLegacy() {
        val path = SharedMedia.rivalDisplayPath(
            opponentClubId = 0,
            clubLegacyUri = null,
            matchLegacyUri = "content://match-only.png",
            clubShared = null
        )
        assertEquals("content://match-only.png", path)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPdfMime() {
        SharedMedia.requireImageMime("application/pdf")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOversizedImage() {
        SharedMedia.requireSize(SharedMedia.MAX_BYTES + 1)
    }

    private fun slot(type: String, parent: String, path: String) = Attachment(
        id = 1,
        syncId = "att-$parent",
        parentType = type,
        parentSyncId = parent,
        mimeType = "image/png",
        name = "slot.png",
        localPath = path
    )
}
