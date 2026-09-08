package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.local.EntitySync
import com.luis.alhendinfc.data.local.EntityWrites
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
        val stamped = EntityWrites.teamForInsert(
            team.toEntity().copy(isSelected = isFirst),
            EntitySync.now()
        )
        return dao.insertTeam(stamped).toInt()
    }

    override suspend fun updateTeam(team: Team) {
        val existing = dao.getByIdOnce(team.id) ?: return
        dao.updateTeam(EntityWrites.teamForUpdate(existing, team.toEntity(), EntitySync.now()))
    }

    override suspend fun deleteTeam(team: Team) {
        dao.markDeleted(team.id, EntitySync.now())
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
