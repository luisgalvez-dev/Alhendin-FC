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
    suspend fun updateMatch(match: Match)
    suspend fun deleteMatch(match: Match)
    suspend fun setPlayerCallup(matchId: Int, playerId: Int, status: CallupStatus)
    suspend fun startLiveMatch(matchId: Int)
    suspend fun setPlayerOnField(matchId: Int, playerId: Int, onField: Boolean)
    suspend fun addEvent(event: MatchEvent): Int
    suspend fun deleteEvent(eventId: Int)
    suspend fun finishMatch(match: Match)
}
