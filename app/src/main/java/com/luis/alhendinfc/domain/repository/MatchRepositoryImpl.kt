package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.local.AttachmentDao
import com.luis.alhendinfc.data.local.EntitySync
import com.luis.alhendinfc.data.local.EntityWrites
import com.luis.alhendinfc.data.local.MatchDao
import com.luis.alhendinfc.data.local.MatchEntity
import com.luis.alhendinfc.data.local.MatchEventDao
import com.luis.alhendinfc.data.local.MatchEventEntity
import com.luis.alhendinfc.data.local.MatchPlayerEntity
import com.luis.alhendinfc.data.local.OpponentClubDao
import com.luis.alhendinfc.data.sync.AttachmentParentType
import com.luis.alhendinfc.data.sync.LiveMatchGuard
import com.luis.alhendinfc.data.sync.SyncEntityType
import com.luis.alhendinfc.data.sync.SyncHooks
import com.luis.alhendinfc.data.sync.TransferHooks
import com.luis.alhendinfc.domain.model.CallupStatus
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.MatchEvent
import com.luis.alhendinfc.domain.model.MatchPlayer
import com.luis.alhendinfc.domain.model.MatchStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MatchRepositoryImpl(
    private val dao: MatchDao,
    private val eventDao: MatchEventDao,
    private val attachmentDao: AttachmentDao? = null,
    private val clubDao: OpponentClubDao? = null
) : MatchRepository {

    override fun getMatchesByTeam(teamId: Int): Flow<List<Match>> =
        dao.getMatchesByTeam(teamId).map { list -> list.map { it.toDomain() } }

    override fun getMatchById(id: Int): Flow<Match?> =
        dao.getMatchById(id).map { it?.toDomain() }

    override fun getMatchPlayers(matchId: Int): Flow<List<MatchPlayer>> =
        dao.getMatchPlayersByMatch(matchId).map { list -> list.map { it.toDomain() } }

    override fun getMatchEvents(matchId: Int): Flow<List<MatchEvent>> =
        eventDao.getEventsByMatch(matchId).map { list -> list.map { it.toDomain() } }

    override fun getTeamEvents(teamId: Int): Flow<List<MatchEvent>> =
        eventDao.getEventsByTeam(teamId).map { list -> list.map { it.toDomain() } }

    override fun getFinishedCallupsByTeam(teamId: Int): Flow<List<MatchPlayer>> =
        dao.getFinishedCallupsByTeam(teamId).map { list -> list.map { it.toDomain() } }

    override suspend fun createMatch(match: Match): Int {
        val stamped = EntityWrites.matchForInsert(stampOpponentSync(match.toEntity()), EntitySync.now())
        return SyncHooks.local(SyncEntityType.MATCH, stamped.syncId) {
            dao.insertMatch(stamped).toInt()
        }
    }

    override suspend fun getMatchesByTeamOnce(teamId: Int): List<Match> =
        dao.getMatchesByTeamOnce(teamId).map { it.toDomain() }

    override suspend fun updateMatch(match: Match) {
        val existing = dao.getByIdOnce(match.id) ?: return
        val incoming = stampOpponentSync(match.toEntity())
        if (LiveMatchGuard.isLocalLive(existing.status)) {
            dao.updateMatch(EntityWrites.matchForUpdate(existing, incoming, EntitySync.now()))
            return
        }
        SyncHooks.local(SyncEntityType.MATCH, existing.syncId) {
            dao.updateMatch(EntityWrites.matchForUpdate(existing, incoming, EntitySync.now()))
        }
    }

    override suspend fun deleteMatch(match: Match) {
        val now = EntitySync.now()
        val existing = dao.getByIdOnce(match.id) ?: return
        val events = eventDao.getByMatchIncludingDeleted(match.id)
        val players = dao.getMatchPlayersByMatchIncludingDeleted(match.id)
        val syncId = match.syncId.ifBlank { existing.syncId }
        val attachments = if (syncId.isNotBlank()) {
            attachmentDao?.getActiveByParentOnce(AttachmentParentType.MATCH, syncId).orEmpty()
        } else {
            emptyList()
        }
        val items = buildList {
            add(SyncEntityType.MATCH to existing.syncId)
            events.forEach { add(SyncEntityType.MATCH_EVENT to it.syncId) }
            players.forEach { add(SyncEntityType.MATCH_PLAYER to it.syncId) }
        }
        SyncHooks.localMany(items) {
            eventDao.markDeletedByMatch(match.id, now)
            dao.markDeletedPlayersByMatch(match.id, now)
            if (syncId.isNotBlank()) {
                attachmentDao?.markDeletedByParent(AttachmentParentType.MATCH, syncId, now)
            }
            dao.markDeleted(match.id, now)
        }
        attachments.forEach { TransferHooks.onLocalTombstone(it) }
    }

    override suspend fun setPlayerCallup(matchId: Int, playerId: Int, status: CallupStatus) {
        val now = EntitySync.now()
        if (status == CallupStatus.NONE) {
            val row = dao.getMatchPlayerOnce(matchId, playerId)
            SyncHooks.local(SyncEntityType.MATCH_PLAYER, row?.syncId.orEmpty()) {
                dao.markDeletedPlayer(matchId, playerId, now)
            }
        } else {
            val existing = dao.getMatchPlayerOnce(matchId, playerId)
            val syncId = existing?.syncId?.takeIf { it.isNotBlank() } ?: EntitySync.newSyncId()
            SyncHooks.local(SyncEntityType.MATCH_PLAYER, syncId) {
                dao.upsertMatchPlayerPreservingIdentity(
                    matchId = matchId,
                    playerId = playerId,
                    callupStatus = status.name,
                    isOnField = status == CallupStatus.TITULAR,
                    syncId = syncId,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                    deletedAt = null
                )
            }
        }
    }

    override suspend fun prepareLiveField(matchId: Int) {
        dao.clearOnField(matchId)
        dao.putTitularesOnField(matchId)
    }

    override suspend fun startLiveMatch(matchId: Int) {
        prepareLiveField(matchId)
        dao.markOpenToLive(matchId, EntitySync.now())
    }

    override suspend fun setPlayerOnField(matchId: Int, playerId: Int, onField: Boolean) {
        dao.setOnField(matchId, playerId, onField)
    }

    override suspend fun addEvent(event: MatchEvent): Int {
        val stamped = EntityWrites.eventForInsert(event.toEntity(), EntitySync.now())
        val match = dao.getByIdOnce(event.matchId)
        return if (LiveMatchGuard.shouldPushMatchEvent(match?.status)) {
            SyncHooks.local(SyncEntityType.MATCH_EVENT, stamped.syncId) {
                eventDao.insert(stamped).toInt()
            }
        } else {
            eventDao.insert(stamped).toInt()
        }
    }

    override suspend fun deleteEvent(eventId: Int) {
        val existing = eventDao.getByIdIncludingDeleted(eventId) ?: return
        val match = dao.getByIdOnce(existing.matchId)
        if (LiveMatchGuard.shouldPushMatchEvent(match?.status)) {
            SyncHooks.local(SyncEntityType.MATCH_EVENT, existing.syncId) {
                eventDao.markDeleted(eventId, EntitySync.now())
            }
        } else {
            eventDao.markDeleted(eventId, EntitySync.now())
        }
    }

    override suspend fun finishMatch(match: Match) {
        markMatchFinished(
            matchId = match.id,
            homeScore = match.homeScore ?: 0,
            awayScore = match.awayScore ?: 0,
            livePeriod = match.livePeriod,
            liveElapsedSeconds = match.liveElapsedSeconds,
            fieldSecondsJson = match.fieldSecondsJson,
            fieldPositionsJson = match.fieldPositionsJson
        )
    }

    override suspend fun markMatchFinished(
        matchId: Int,
        homeScore: Int,
        awayScore: Int,
        livePeriod: Int,
        liveElapsedSeconds: Int,
        fieldSecondsJson: String,
        fieldPositionsJson: String
    ) {
        val match = dao.getByIdOnce(matchId) ?: return
        val events = eventDao.getByMatchIncludingDeleted(matchId)
        val players = dao.getMatchPlayersByMatchIncludingDeleted(matchId)
        val items = buildList {
            add(SyncEntityType.MATCH to match.syncId)
            events.forEach { add(SyncEntityType.MATCH_EVENT to it.syncId) }
            players.forEach { add(SyncEntityType.MATCH_PLAYER to it.syncId) }
        }
        SyncHooks.localMany(items) {
            dao.markFinished(
                matchId = matchId,
                homeScore = homeScore,
                awayScore = awayScore,
                livePeriod = livePeriod,
                liveElapsedSeconds = liveElapsedSeconds,
                fieldSecondsJson = fieldSecondsJson,
                fieldPositionsJson = fieldPositionsJson,
                updatedAt = EntitySync.now()
            )
        }
    }

    override suspend fun updateFieldPositions(matchId: Int, fieldPositionsJson: String) {
        dao.updateFieldPositions(matchId, fieldPositionsJson)
    }

    override suspend fun updateLiveClock(
        matchId: Int,
        elapsedSeconds: Int,
        running: Boolean,
        anchorWallMs: Long,
        period: Int,
        fieldSecondsJson: String
    ) {
        dao.updateLiveClock(
            matchId = matchId,
            elapsedSeconds = elapsedSeconds,
            running = running,
            anchorWallMs = anchorWallMs,
            period = period,
            fieldSecondsJson = fieldSecondsJson
        )
    }

    override suspend fun updateLiveScore(matchId: Int, homeScore: Int, awayScore: Int) {
        dao.updateLiveScore(matchId, homeScore, awayScore)
    }

    private suspend fun stampOpponentSync(entity: MatchEntity): MatchEntity {
        val clubId = entity.opponentClubId?.takeIf { it > 0 }
        if (clubId == null) return entity.copy(opponentClubSyncId = null)
        val fromClub = clubDao?.getByIdIncludingDeleted(clubId)?.syncId?.trim()?.takeIf { it.isNotEmpty() }
        if (fromClub != null) return entity.copy(opponentClubSyncId = fromClub)
        return entity.copy(opponentClubSyncId = entity.opponentClubSyncId?.trim()?.takeIf { it.isNotEmpty() })
    }

    private fun MatchEntity.toDomain() = Match(
        id = id,
        teamId = teamId,
        rival = rival,
        stadium = stadium,
        date = date,
        time = time,
        matchday = matchday,
        isHome = isHome,
        durationPerPart = durationPerPart,
        numParts = numParts,
        formation = formation,
        notes = notes,
        status = try {
            MatchStatus.valueOf(status)
        } catch (_: Exception) {
            MatchStatus.OPEN
        },
        homeScore = homeScore,
        awayScore = awayScore,
        opponentClubId = opponentClubId,
        opponentClubSyncId = opponentClubSyncId,
        rivalShieldUri = rivalShieldUri,
        livePeriod = livePeriod,
        liveElapsedSeconds = liveElapsedSeconds,
        liveClockRunning = liveClockRunning,
        liveClockAnchorWallMs = liveClockAnchorWallMs,
        fieldSecondsJson = fieldSecondsJson,
        fieldPositionsJson = fieldPositionsJson,
        syncId = syncId
    )

    private fun Match.toEntity() = MatchEntity(
        id = id,
        teamId = teamId,
        rival = rival,
        stadium = stadium,
        date = date,
        time = time,
        matchday = matchday,
        isHome = isHome,
        durationPerPart = durationPerPart,
        numParts = numParts,
        formation = formation,
        notes = notes,
        status = status.name,
        homeScore = homeScore,
        awayScore = awayScore,
        opponentClubId = opponentClubId,
        opponentClubSyncId = opponentClubSyncId,
        rivalShieldUri = rivalShieldUri,
        livePeriod = livePeriod,
        liveElapsedSeconds = liveElapsedSeconds,
        liveClockRunning = liveClockRunning,
        liveClockAnchorWallMs = liveClockAnchorWallMs,
        fieldSecondsJson = fieldSecondsJson,
        fieldPositionsJson = fieldPositionsJson,
        syncId = syncId
    )

    private fun MatchPlayerEntity.toDomain() = MatchPlayer(
        id = id,
        matchId = matchId,
        playerId = playerId,
        callupStatus = try {
            CallupStatus.valueOf(callupStatus)
        } catch (_: Exception) {
            CallupStatus.NONE
        },
        isOnField = isOnField
    )

    private fun MatchEventEntity.toDomain() = MatchEvent(
        id = id,
        matchId = matchId,
        typeCode = typeCode,
        playerId = playerId,
        relatedPlayerId = relatedPlayerId,
        minute = minute,
        period = period,
        value = value,
        createdAt = createdAt
    )

    private fun MatchEvent.toEntity() = MatchEventEntity(
        id = id,
        matchId = matchId,
        typeCode = typeCode,
        playerId = playerId,
        relatedPlayerId = relatedPlayerId,
        minute = minute,
        period = period,
        value = value,
        createdAt = createdAt
    )
}
