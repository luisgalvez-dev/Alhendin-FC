package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.domain.model.CallupStatus
import com.luis.alhendinfc.domain.model.Match
import com.luis.alhendinfc.domain.model.MatchPlayer
import kotlinx.coroutines.flow.Flow

interface MatchRepository {
    fun getMatchesByTeam(teamId: Int): Flow<List<Match>>
    fun getMatchById(id: Int): Flow<Match?>
    fun getMatchPlayers(matchId: Int): Flow<List<MatchPlayer>>
    suspend fun createMatch(match: Match): Int
    suspend fun updateMatch(match: Match)
    suspend fun deleteMatch(match: Match)
    suspend fun setPlayerCallup(matchId: Int, playerId: Int, status: CallupStatus)
}
