package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.local.EntitySync
import com.luis.alhendinfc.data.local.EntityWrites
import com.luis.alhendinfc.data.local.MatchDao
import com.luis.alhendinfc.data.local.PlayerDao
import com.luis.alhendinfc.data.local.PlayerEntity
import com.luis.alhendinfc.data.sync.SyncEntityType
import com.luis.alhendinfc.data.sync.SyncHooks
import com.luis.alhendinfc.domain.model.Laterality
import com.luis.alhendinfc.domain.model.Player
import com.luis.alhendinfc.domain.model.PlayerPosition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlayerRepositoryImpl(
    private val dao: PlayerDao,
    @Suppress("unused")
    private val matchDao: MatchDao? = null
) : PlayerRepository {

    override fun getPlayersByTeam(teamId: Int): Flow<List<Player>> =
        dao.getAllByTeam(teamId).map { list -> list.map { it.toDomain() } }

    override fun getPlayerById(id: Int): Flow<Player?> =
        dao.getById(id).map { it?.toDomain() }

    override suspend fun getOnce(id: Int): Player? =
        dao.getByIdOnce(id)?.toDomain()

    override suspend fun addPlayer(player: Player): Int {
        val stamped = EntityWrites.playerForInsert(player.toEntity(), EntitySync.now())
        return SyncHooks.local(SyncEntityType.PLAYER, stamped.syncId) {
            dao.insert(stamped).toInt()
        }
    }

    override suspend fun updatePlayer(player: Player) {
        val existing = dao.getByIdOnce(player.id) ?: return
        SyncHooks.local(SyncEntityType.PLAYER, existing.syncId) {
            dao.update(EntityWrites.playerForUpdate(existing, player.toEntity(), EntitySync.now()))
        }
    }

    override suspend fun deletePlayer(player: Player) {
        val existing = dao.getByIdIncludingDeleted(player.id) ?: dao.getByIdOnce(player.id) ?: return
        SyncHooks.local(SyncEntityType.PLAYER, existing.syncId) {
            dao.markDeleted(player.id, EntitySync.now())
        }
    }

    private fun PlayerEntity.toDomain() = Player(
        id = id,
        teamId = teamId,
        name = name,
        alias = alias,
        position = try {
            PlayerPosition.valueOf(position)
        } catch (_: Exception) {
            PlayerPosition.MEDIOCENTRO_DEFENSIVO
        },
        jerseyNumber = jerseyNumber,
        photoUri = photoUri,
        height = height,
        weight = weight,
        laterality = try {
            Laterality.valueOf(laterality)
        } catch (_: Exception) {
            Laterality.DERECHA
        },
        isActive = isActive,
        observations = observations,
        syncId = syncId
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
        observations = observations,
        syncId = syncId
    )
}
