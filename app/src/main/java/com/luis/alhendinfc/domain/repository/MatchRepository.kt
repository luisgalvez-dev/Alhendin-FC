package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.domain.model.CallupStatus
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.MatchEvent
import com.luis.alhendinfc.domain.model.MatchPlayer
import kotlinx.coroutines.flow.Flow

interface MatchRepository {
    fun getMatchesByTeam(teamId: Int): Flow<List<Match>>
    fun getMatchById(id: Int): Flow<Match?>
    fun getMatchPlayers(matchId: Int): Flow<List<MatchPlayer>>
    fun getMatchEvents(matchId: Int): Flow<List<MatchEvent>>
    fun getTeamEvents(teamId: Int): Flow<List<MatchEvent>>
    /** Convocatorias (titular/suplente) en partidos finalizados. */
    fun getFinishedCallupsByTeam(teamId: Int): Flow<List<MatchPlayer>>
    suspend fun createMatch(match: Match): Int
    suspend fun getMatchesByTeamOnce(teamId: Int): List<Match>
    suspend fun updateMatch(match: Match)
    suspend fun deleteMatch(match: Match)
    suspend fun setPlayerCallup(matchId: Int, playerId: Int, status: CallupStatus)
    /** Coloca titulares en el campo sin pasar a LIVE. */
    suspend fun prepareLiveField(matchId: Int)
    suspend fun startLiveMatch(matchId: Int)
    suspend fun setPlayerOnField(matchId: Int, playerId: Int, onField: Boolean)
    suspend fun addEvent(event: MatchEvent): Int
    suspend fun deleteEvent(eventId: Int)
    suspend fun finishMatch(match: Match)
    suspend fun markMatchFinished(
        matchId: Int,
        homeScore: Int,
        awayScore: Int,
        livePeriod: Int,
        liveElapsedSeconds: Int,
        fieldSecondsJson: String,
        fieldPositionsJson: String
    )
    suspend fun updateFieldPositions(matchId: Int, fieldPositionsJson: String)
    suspend fun updateLiveClock(
        matchId: Int,
        elapsedSeconds: Int,
        running: Boolean,
        anchorWallMs: Long,
        period: Int,
        fieldSecondsJson: String
    )
    suspend fun updateLiveScore(matchId: Int, homeScore: Int, awayScore: Int)
}
