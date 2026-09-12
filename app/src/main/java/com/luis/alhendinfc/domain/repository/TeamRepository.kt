package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.domain.model.Team
import kotlinx.coroutines.flow.Flow

interface TeamRepository {
    fun getAllTeams(): Flow<List<Team>>
    fun getSelectedTeam(): Flow<Team?>
    suspend fun getOnce(id: Int): Team?
    suspend fun addTeam(team: Team): Int
    suspend fun updateTeam(team: Team)
    suspend fun deleteTeam(team: Team)
    suspend fun selectTeam(teamId: Int)
}
