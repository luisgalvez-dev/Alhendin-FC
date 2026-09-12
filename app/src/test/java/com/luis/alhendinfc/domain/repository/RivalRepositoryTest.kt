package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.local.OpponentClubDao
import com.luis.alhendinfc.data.local.OpponentClubEntity
import com.luis.alhendinfc.data.local.RivalAnalysisDao
import com.luis.alhendinfc.data.local.RivalAnalysisEntity
import com.luis.alhendinfc.data.local.RivalLinkDao
import com.luis.alhendinfc.data.local.RivalLinkEntity
import com.luis.alhendinfc.data.local.SeasonFixtureDao
import com.luis.alhendinfc.data.local.SeasonFixtureEntity
import com.luis.alhendinfc.data.sync.AttachmentParentType
import com.luis.alhendinfc.data.local.OpponentPlayerDao
import com.luis.alhendinfc.data.local.OpponentPlayerEntity
import com.luis.alhendinfc.domain.model.OpponentClub
import com.luis.alhendinfc.domain.model.OpponentPlayer
import com.luis.alhendinfc.domain.model.RivalAnalysis
import com.luis.alhendinfc.domain.model.RivalLink
import com.luis.alhendinfc.domain.model.RivalLinkType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RivalRepositoryTest {

    private fun harness(): Harness {
        val clubDao = InMemoryOpponentClubDao()
        val fixtureDao = InMemoryEmptyFixtureDao()
        val analysisDao = InMemoryRivalAnalysisDao()
        val linkDao = InMemoryRivalLinkDao()
        val playerDao = InMemoryOpponentPlayerDao()
        val attachmentDao = InMemoryAttachmentDao()
        return Harness(
            SeasonCalendarRepository(clubDao, fixtureDao),
            RivalRepository(analysisDao, linkDao, playerDao),
            AttachmentRepositoryImpl(attachmentDao),
            clubDao,
            analysisDao,
            linkDao,
            playerDao,
            attachmentDao
        )
    }

    @Test
    fun existingClub_survivesCreateEditDeleteAndSearch() = runTest {
        val h = harness()
        val keptId = h.clubs.addClub(OpponentClub(teamId = 1, name = "Club Real", stadium = "Municipal"))
        val demoId = h.clubs.addClub(OpponentClub(teamId = 1, name = "[DEV] Rival A", shortName = "DEVA"))
        h.clubs.updateClub(h.clubs.getClubOnce(demoId)!!.copy(stadium = "Campo DEV A"))
        val edited = h.clubs.getClubOnce(demoId)!!
        assertEquals("Campo DEV A", edited.stadium)
        assertTrue(edited.syncId.isNotBlank())
        h.clubs.deleteClub(edited)
        val active = h.clubs.getClubs(1).first()
        assertEquals(1, active.size)
        assertEquals(keptId, active[0].id)
        assertEquals("Club Real", active[0].name)
        val tombstone = h.clubDao.getByIdIncludingDeleted(demoId)!!
        assertNotNull(tombstone.deletedAt)
        val search = h.clubs.searchClubs(1, "real").first()
        assertEquals(1, search.size)
        assertEquals(keptId, search[0].id)
    }

    @Test
    fun analysis_oneActivePerClub_preservesSyncId_andTombstoneRevive() = runTest {
        val h = harness()
        val clubId = h.clubs.addClub(OpponentClub(teamId = 1, name = "Rival"))
        val id = h.rivals.saveAnalysis(RivalAnalysis(opponentClubId = clubId, usualSystem = "1-4-3-3"))
        val created = h.rivals.getAnalysis(clubId).first()!!
        assertEquals(id, created.id)
        assertTrue(created.syncId.isNotBlank())
        Thread.sleep(2)
        h.rivals.saveAnalysis(created.copy(usualSystem = "1-4-2-3-1", syncId = "stale"))
        val edited = h.rivals.getAnalysis(clubId).first()!!
        assertEquals(created.syncId, edited.syncId)
        assertEquals(created.id, edited.id)
        assertEquals("1-4-2-3-1", edited.usualSystem)
        h.rivals.deleteAnalysis(edited)
        assertNull(h.rivals.getAnalysis(clubId).first())
        assertEquals(1, h.analysisDao.getAllOnce().size)
        assertNotNull(h.analysisDao.getAllOnce()[0].deletedAt)
        h.rivals.saveAnalysis(RivalAnalysis(opponentClubId = clubId, usualSystem = "1-3-5-2"))
        val revived = h.rivals.getAnalysis(clubId).first()!!
        assertEquals(created.syncId, revived.syncId)
        assertEquals(1, h.analysisDao.getAllOnce().size)
        assertNull(revived.deletedAt)
        assertEquals("1-3-5-2", revived.usualSystem)
    }

    @Test
    fun links_typesEditTombstoneAndOrder() = runTest {
        val h = harness()
        val clubId = h.clubs.addClub(OpponentClub(teamId = 1, name = "Rival"))
        val a = h.rivals.addLink(
            RivalLink(opponentClubId = clubId, type = RivalLinkType.RFAF, label = "RFAF", url = "https://example.com/a")
        )
        val b = h.rivals.addLink(
            RivalLink(opponentClubId = clubId, type = RivalLinkType.YOUTUBE, label = "YT", url = "https://example.com/b")
        )
        val active = h.rivals.getLinks(clubId).first()
        assertEquals(2, active.size)
        assertEquals(RivalLinkType.RFAF, active[0].type)
        h.rivals.updateLink(active[0].copy(label = "RFAF DEMO", url = "https://example.com/dev/rfaf"))
        assertEquals("RFAF DEMO", h.rivals.getLinks(clubId).first()[0].label)
        h.rivals.moveLink(clubId, a, false)
        val reordered = h.rivals.getLinks(clubId).first()
        assertEquals(b, reordered[0].id)
        h.rivals.deleteLink(reordered[0])
        val after = h.rivals.getLinks(clubId).first()
        assertEquals(1, after.size)
        assertNotNull(h.linkDao.getAllOnce().first { it.id == b }.deletedAt)
    }

    @Test
    fun playRfaf_isNotAConfigurableRivalLink() = runTest {
        val h = harness()
        val clubId = h.clubs.addClub(OpponentClub(teamId = 1, name = "Rival"))
        val error = runCatching {
            h.rivals.addLink(
                RivalLink(
                    opponentClubId = clubId,
                    type = "PLAY_RFAF",
                    label = "PlayRFAF",
                    url = "https://www.footballclub.pro/main-fc"
                )
            )
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertTrue(h.rivals.getLinks(clubId).first().isEmpty())
    }

    @Test
    fun opponentAttachments_severalAndTombstone() = runTest {
        val h = harness()
        val clubId = h.clubs.addClub(OpponentClub(teamId = 1, name = "Rival"))
        val syncId = h.clubs.getClubOnce(clubId)!!.syncId
        h.attachments.add(AttachmentParentType.OPPONENT, syncId, "image/jpeg", "a.jpg", "/files/a.jpg")
        h.attachments.add(AttachmentParentType.OPPONENT, syncId, "application/pdf", "b.pdf", "/files/b.pdf")
        val active = h.attachments.getActiveByParent(AttachmentParentType.OPPONENT, syncId).first()
        assertEquals(2, active.size)
        assertTrue(active.any { it.isImage })
        h.attachments.delete(active.first { it.isImage })
        assertEquals(1, h.attachments.getActiveByParent(AttachmentParentType.OPPONENT, syncId).first().size)
        assertEquals(2, h.attachmentDao.getAllOnce().size)
        assertEquals(1, h.attachmentDao.getAllOnce().count { it.deletedAt != null })
    }

    @Test
    fun opponentPlayers_createEditSearchTombstoneAndIsolation() = runTest {
        val h = harness()
        val a = h.clubs.addClub(OpponentClub(teamId = 1, name = "Rival A"))
        val b = h.clubs.addClub(OpponentClub(teamId = 1, name = "Rival B"))
        val id = h.rivals.addPlayer(OpponentPlayer(opponentClubId = a, name = "  Antonio Pérez  "))
        val created = h.rivals.getPlayers(a).first().first()
        assertEquals(id, created.id)
        assertEquals("Antonio Pérez", created.name)
        assertTrue(created.syncId.isNotBlank())
        Thread.sleep(2)
        h.rivals.updatePlayer(created.copy(name = "Mario López", syncId = "stale", opponentClubId = b))
        val edited = h.rivals.getPlayers(a).first().first()
        assertEquals(created.syncId, edited.syncId)
        assertEquals("Mario López", edited.name)
        assertEquals(a, edited.opponentClubId)
        h.rivals.addPlayer(OpponentPlayer(opponentClubId = a, name = "Carlos Ruiz"))
        h.rivals.addPlayer(OpponentPlayer(opponentClubId = b, name = "Diego Castro"))
        assertEquals(listOf("Carlos Ruiz", "Mario López"), h.rivals.getPlayers(a).first().map { it.name })
        assertEquals(1, h.rivals.searchPlayers(a, "car").first().size)
        h.rivals.deletePlayer(edited)
        assertEquals(1, h.rivals.getPlayers(a).first().size)
        assertEquals(2, h.playerDao.getAllOnce().count { it.opponentClubId == a })
        assertNotNull(h.playerDao.getAllOnce().first { it.id == edited.id }.deletedAt)
        assertEquals(1, h.rivals.getPlayers(b).first().size)
    }

    private data class Harness(
        val clubs: SeasonCalendarRepository,
        val rivals: RivalRepository,
        val attachments: AttachmentRepositoryImpl,
        val clubDao: InMemoryOpponentClubDao,
        val analysisDao: InMemoryRivalAnalysisDao,
        val linkDao: InMemoryRivalLinkDao,
        val playerDao: InMemoryOpponentPlayerDao,
        val attachmentDao: InMemoryAttachmentDao
    )
}

internal class InMemoryOpponentClubDao : OpponentClubDao {
    private val rows = MutableStateFlow<List<OpponentClubEntity>>(emptyList())
    private var nextId = 1

    override fun getByTeam(teamId: Int): Flow<List<OpponentClubEntity>> =
        rows.map { list -> list.filter { it.teamId == teamId && it.deletedAt == null }.sortedBy { it.id } }

    override suspend fun getById(id: Int): OpponentClubEntity? =
        rows.value.firstOrNull { it.id == id && it.deletedAt == null }

    override fun observeById(id: Int): Flow<OpponentClubEntity?> =
        rows.map { list -> list.firstOrNull { it.id == id && it.deletedAt == null } }

    override suspend fun getByIdIncludingDeleted(id: Int): OpponentClubEntity? =
        rows.value.firstOrNull { it.id == id }

    override suspend fun getByTeamAndNameIncludingDeleted(teamId: Int, name: String): OpponentClubEntity? =
        rows.value.firstOrNull { it.teamId == teamId && it.name == name }

    override suspend fun countByTeam(teamId: Int): Int =
        rows.value.count { it.teamId == teamId && it.deletedAt == null }

    override suspend fun getAllOnce(): List<OpponentClubEntity> = rows.value

    override suspend fun getBySyncIdIncludingDeleted(syncId: String): OpponentClubEntity? =
        rows.value.firstOrNull { it.syncId == syncId }

    override suspend fun insertAll(entities: List<OpponentClubEntity>) {
        entities.forEach { insert(it) }
    }

    override suspend fun insert(entity: OpponentClubEntity): Long {
        val id = if (entity.id == 0) nextId++ else entity.id
        rows.value = rows.value + entity.copy(id = id)
        return id.toLong()
    }

    override suspend fun update(entity: OpponentClubEntity) {
        rows.value = rows.value.map { if (it.id == entity.id) entity else it }
    }

    override suspend fun markDeleted(id: Int, now: Long) {
        rows.value = rows.value.map {
            if (it.id == id && it.deletedAt == null) it.copy(deletedAt = now, updatedAt = now) else it
        }
    }
}

private class InMemoryEmptyFixtureDao : SeasonFixtureDao {
    override fun getByTeam(teamId: Int) = MutableStateFlow(emptyList<SeasonFixtureEntity>())
    override suspend fun getByMatchday(teamId: Int, matchday: Int) = null
    override suspend fun getByMatchdayIncludingDeleted(teamId: Int, matchday: Int) = null
    override suspend fun getByIdOnce(id: Int) = null
    override suspend fun countByTeam(teamId: Int) = 0
    override suspend fun countActiveByTeamAndDay(teamId: Int, epochDay: Long) = 0
    override suspend fun getAllOnce() = emptyList<SeasonFixtureEntity>()
    override suspend fun getBySyncIdIncludingDeleted(syncId: String) = null
    override suspend fun insertAll(entities: List<SeasonFixtureEntity>) = Unit
    override suspend fun insert(entity: SeasonFixtureEntity) = 1L
    override suspend fun update(entity: SeasonFixtureEntity) = Unit
    override suspend fun markDeleted(id: Int, now: Long) = Unit
}

internal class InMemoryRivalAnalysisDao : RivalAnalysisDao {
    private val rows = MutableStateFlow<List<RivalAnalysisEntity>>(emptyList())
    private var nextId = 1

    override fun getActiveByClub(opponentClubId: Int): Flow<RivalAnalysisEntity?> =
        rows.map { list -> list.firstOrNull { it.opponentClubId == opponentClubId && it.deletedAt == null } }

    override suspend fun getActiveByClubOnce(opponentClubId: Int): RivalAnalysisEntity? =
        rows.value.firstOrNull { it.opponentClubId == opponentClubId && it.deletedAt == null }

    override suspend fun getByClubIncludingDeleted(opponentClubId: Int): RivalAnalysisEntity? =
        rows.value.firstOrNull { it.opponentClubId == opponentClubId }

    override suspend fun getByIdIncludingDeleted(id: Int): RivalAnalysisEntity? =
        rows.value.firstOrNull { it.id == id }

    override suspend fun getAllOnce(): List<RivalAnalysisEntity> = rows.value

    override suspend fun getBySyncIdIncludingDeleted(syncId: String): RivalAnalysisEntity? =
        rows.value.firstOrNull { it.syncId == syncId }

    override suspend fun insert(entity: RivalAnalysisEntity): Long {
        val id = if (entity.id == 0) nextId++ else entity.id
        rows.value = rows.value + entity.copy(id = id)
        return id.toLong()
    }

    override suspend fun insertAll(entities: List<RivalAnalysisEntity>) {
        entities.forEach { insert(it) }
    }

    override suspend fun update(entity: RivalAnalysisEntity) {
        rows.value = rows.value.map { if (it.id == entity.id) entity else it }
    }

    override suspend fun markDeleted(id: Int, now: Long) {
        rows.value = rows.value.map {
            if (it.id == id && it.deletedAt == null) it.copy(deletedAt = now, updatedAt = now) else it
        }
    }
}

internal class InMemoryRivalLinkDao : RivalLinkDao {
    private val rows = MutableStateFlow<List<RivalLinkEntity>>(emptyList())
    private var nextId = 1

    override fun getActiveByClub(opponentClubId: Int): Flow<List<RivalLinkEntity>> =
        rows.map { list ->
            list.filter { it.opponentClubId == opponentClubId && it.deletedAt == null }
                .sortedWith(compareBy({ it.sortOrder }, { it.id }))
        }

    override suspend fun getActiveByClubOnce(opponentClubId: Int): List<RivalLinkEntity> =
        rows.value.filter { it.opponentClubId == opponentClubId && it.deletedAt == null }
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))

    override suspend fun getByIdOnce(id: Int): RivalLinkEntity? =
        rows.value.firstOrNull { it.id == id && it.deletedAt == null }

    override suspend fun getByIdIncludingDeleted(id: Int): RivalLinkEntity? =
        rows.value.firstOrNull { it.id == id }

    override suspend fun getAllOnce(): List<RivalLinkEntity> = rows.value

    override suspend fun getBySyncIdIncludingDeleted(syncId: String): RivalLinkEntity? =
        rows.value.firstOrNull { it.syncId == syncId }

    override suspend fun insert(entity: RivalLinkEntity): Long {
        val id = if (entity.id == 0) nextId++ else entity.id
        rows.value = rows.value + entity.copy(id = id)
        return id.toLong()
    }

    override suspend fun insertAll(entities: List<RivalLinkEntity>) {
        entities.forEach { insert(it) }
    }

    override suspend fun update(entity: RivalLinkEntity) {
        rows.value = rows.value.map { if (it.id == entity.id) entity else it }
    }

    override suspend fun markDeleted(id: Int, now: Long) {
        rows.value = rows.value.map {
            if (it.id == id && it.deletedAt == null) it.copy(deletedAt = now, updatedAt = now) else it
        }
    }
}

internal class InMemoryOpponentPlayerDao : OpponentPlayerDao {
    private val rows = MutableStateFlow<List<OpponentPlayerEntity>>(emptyList())
    private var nextId = 1

    override fun getActiveByClub(opponentClubId: Int): Flow<List<OpponentPlayerEntity>> =
        rows.map { list ->
            list.filter { it.opponentClubId == opponentClubId && it.deletedAt == null }
                .sortedWith(
                    compareBy<OpponentPlayerEntity, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
                        .thenBy { it.id }
                )
        }

    override fun searchByClubAndName(opponentClubId: Int, query: String): Flow<List<OpponentPlayerEntity>> =
        getActiveByClub(opponentClubId).map { list ->
            list.filter { it.name.contains(query, ignoreCase = true) }
        }

    override suspend fun getByIdOnce(id: Int): OpponentPlayerEntity? =
        rows.value.firstOrNull { it.id == id && it.deletedAt == null }

    override suspend fun getByIdIncludingDeleted(id: Int): OpponentPlayerEntity? =
        rows.value.firstOrNull { it.id == id }

    override suspend fun getAllOnce(): List<OpponentPlayerEntity> = rows.value

    override suspend fun getBySyncIdIncludingDeleted(syncId: String): OpponentPlayerEntity? =
        rows.value.firstOrNull { it.syncId == syncId }

    override suspend fun insert(entity: OpponentPlayerEntity): Long {
        val id = if (entity.id == 0) nextId++ else entity.id
        rows.value = rows.value + entity.copy(id = id)
        return id.toLong()
    }

    override suspend fun insertAll(entities: List<OpponentPlayerEntity>) {
        entities.forEach { insert(it) }
    }

    override suspend fun update(entity: OpponentPlayerEntity) {
        rows.value = rows.value.map { if (it.id == entity.id) entity else it }
    }

    override suspend fun markDeleted(id: Int, now: Long) {
        rows.value = rows.value.map {
            if (it.id == id && it.deletedAt == null) it.copy(deletedAt = now, updatedAt = now) else it
        }
    }
}
