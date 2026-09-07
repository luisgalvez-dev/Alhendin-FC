package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.local.MatchDao
import com.luis.alhendinfc.data.local.MatchEntity
import com.luis.alhendinfc.data.local.MatchEventDao
import com.luis.alhendinfc.data.local.MatchEventEntity
import com.luis.alhendinfc.data.local.MatchPlayerEntity
import com.luis.alhendinfc.domain.model.CallupStatus
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.MatchEvent
import com.luis.alhendinfc.domain.model.MatchPlayer
import com.luis.alhendinfc.domain.model.MatchStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MatchRepositoryImpl(
    private val dao: MatchDao,
    private val eventDao: MatchEventDao
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

    override suspend fun createMatch(match: Match): Int =
        dao.insertMatch(match.toEntity()).toInt()

    override suspend fun getMatchesByTeamOnce(teamId: Int): List<Match> =
        dao.getMatchesByTeamOnce(teamId).map { it.toDomain() }

    override suspend fun updateMatch(match: Match) =
        dao.updateMatch(match.toEntity())

    override suspend fun deleteMatch(match: Match) {
        eventDao.deleteByMatch(match.id)
        dao.deleteMatchPlayersByMatch(match.id)
        dao.deleteMatch(match.toEntity())
    }

    override suspend fun setPlayerCallup(matchId: Int, playerId: Int, status: CallupStatus) {
        if (status == CallupStatus.NONE) {
            dao.deleteMatchPlayer(matchId, playerId)
        } else {
            dao.upsertMatchPlayer(
                MatchPlayerEntity(
                    matchId = matchId,
                    playerId = playerId,
                    callupStatus = status.name,
                    isOnField = status == CallupStatus.TITULAR
                )
            )
        }
    }

    override suspend fun startLiveMatch(matchId: Int) {
        dao.clearOnField(matchId)
        dao.putTitularesOnField(matchId)
        dao.markOpenToLive(matchId)
    }

    override suspend fun setPlayerOnField(matchId: Int, playerId: Int, onField: Boolean) {
        dao.setOnField(matchId, playerId, onField)
    }

    override suspend fun addEvent(event: MatchEvent): Int =
        eventDao.insert(event.toEntity()).toInt()

    override suspend fun deleteEvent(eventId: Int) {
        eventDao.deleteById(eventId)
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
        dao.markFinished(
            matchId = matchId,
            homeScore = homeScore,
            awayScore = awayScore,
            livePeriod = livePeriod,
            liveElapsedSeconds = liveElapsedSeconds,
            fieldSecondsJson = fieldSecondsJson,
            fieldPositionsJson = fieldPositionsJson
        )
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
        rivalShieldUri = rivalShieldUri,
        livePeriod = livePeriod,
        liveElapsedSeconds = liveElapsedSeconds,
        liveClockRunning = liveClockRunning,
        liveClockAnchorWallMs = liveClockAnchorWallMs,
        fieldSecondsJson = fieldSecondsJson,
        fieldPositionsJson = fieldPositionsJson
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
        rivalShieldUri = rivalShieldUri,
        livePeriod = livePeriod,
        liveElapsedSeconds = liveElapsedSeconds,
        liveClockRunning = liveClockRunning,
        liveClockAnchorWallMs = liveClockAnchorWallMs,
        fieldSecondsJson = fieldSecondsJson,
        fieldPositionsJson = fieldPositionsJson
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
