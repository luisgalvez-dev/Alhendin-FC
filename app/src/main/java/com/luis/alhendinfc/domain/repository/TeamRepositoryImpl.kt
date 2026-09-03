package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.local.TeamDao
import com.luis.alhendinfc.data.local.TeamEntity
import com.luis.alhendinfc.domain.model.Team
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TeamRepositoryImpl(private val dao: TeamDao) : TeamRepository {

    override fun getAllTeams(): Flow<List<Team>> =
        dao.getAllTeams().map { list -> list.map { it.toDomain() } }

    override fun getSelectedTeam(): Flow<Team?> =
        dao.getSelectedTeam().map { it?.toDomain() }

    override suspend fun addTeam(team: Team): Int {
        val isFirst = dao.getTeamCount() == 0
        return dao.insertTeam(team.toEntity().copy(isSelected = isFirst)).toInt()
    }

    override suspend fun updateTeam(team: Team) {
        dao.updateTeam(team.toEntity())
    }

    override suspend fun deleteTeam(team: Team) {
        dao.deleteTeam(team.toEntity())
    }

    override suspend fun selectTeam(teamId: Int) {
        dao.setSelectedTeam(teamId)
    }

    private fun TeamEntity.toDomain() = Team(
        id = id,
        name = name,
        category = category,
        season = season,
        shieldUri = shieldUri,
        isSelected = isSelected
    )

    private fun Team.toEntity() = TeamEntity(
        id = id,
        name = name,
        category = category,
        season = season,
        shieldUri = shieldUri,
        isSelected = isSelected
    )
}
