package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.local.PlayerDao
import com.luis.alhendinfc.data.local.PlayerEntity
import com.luis.alhendinfc.domain.model.Laterality
import com.luis.alhendinfc.domain.model.Player
import com.luis.alhendinfc.domain.model.PlayerPosition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlayerRepositoryImpl(private val dao: PlayerDao) : PlayerRepository {

    override fun getPlayersByTeam(teamId: Int): Flow<List<Player>> =
        dao.getAllByTeam(teamId).map { list -> list.map { it.toDomain() } }

    override fun getPlayerById(id: Int): Flow<Player?> =
        dao.getById(id).map { it?.toDomain() }

    override suspend fun addPlayer(player: Player) {
        dao.insert(player.toEntity())
    }

    override suspend fun updatePlayer(player: Player) {
        dao.update(player.toEntity())
    }

    override suspend fun deletePlayer(player: Player) {
        dao.delete(player.toEntity())
    }

    private fun PlayerEntity.toDomain() = Player(
        id = id,
        teamId = teamId,
        name = name,
        alias = alias,
        position = try { PlayerPosition.valueOf(position) } catch (_: Exception) { PlayerPosition.MEDIOCENTRO_DEFENSIVO },
        jerseyNumber = jerseyNumber,
        photoUri = photoUri,
        height = height,
        weight = weight,
        laterality = try { Laterality.valueOf(laterality) } catch (_: Exception) { Laterality.DERECHA },
        isActive = isActive,
        observations = observations
    )

    private fun Player.toEntity() = PlayerEntity(
        id = id,
        teamId = teamId,
        name = name,
        alias = alias,
        position = position.name,
        jerseyNumber = jerseyNumber,
        photoUri = photoUri,
        height = height,
        weight = weight,
        laterality = laterality.name,
        isActive = isActive,
        observations = observations
    )
}
