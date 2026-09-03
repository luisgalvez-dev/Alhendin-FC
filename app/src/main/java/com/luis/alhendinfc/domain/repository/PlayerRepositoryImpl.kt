package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.local.MatchDao
import com.luis.alhendinfc.data.local.PlayerDao
import com.luis.alhendinfc.data.local.PlayerEntity
import com.luis.alhendinfc.domain.model.Laterality
import com.luis.alhendinfc.domain.model.Player
import com.luis.alhendinfc.domain.model.PlayerPosition
import com.luis.alhendinfc.domain.model.SampleSquad
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PlayerRepositoryImpl(
    private val dao: PlayerDao,
    private val matchDao: MatchDao? = null
) : PlayerRepository {

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

    override suspend fun ensureSampleSquad(teamId: Int): Boolean {
        if (teamId <= 0) return false
        return lockFor(teamId).withLock {
            dedupeByJerseyNumber(teamId)
            if (dao.countByTeam(teamId) > 0) return@withLock false
            val entities = SampleSquad.createPlayers(teamId).map { it.toEntity() }
            dao.insertAll(entities)
            true
        }
    }

    /**
     * Elimina jugadores duplicados del mismo dorsal (efecto de semillas concurrentes)
     * y reasigna eventos/convocatorias al id que se conserva.
     */
    private suspend fun dedupeByJerseyNumber(teamId: Int) {
        val all = dao.getAllByTeamOnce(teamId)
        val groups = all.groupBy { it.jerseyNumber }.filter { it.value.size > 1 }
        if (groups.isEmpty()) return
        val md = matchDao
        groups.values.forEach { dups ->
            val sorted = dups.sortedBy { it.id }
            val keep = sorted.first()
            sorted.drop(1).forEach { dup ->
                if (md != null) {
                    mergePlayerReferences(dupId = dup.id, keepId = keep.id, matchDao = md)
                }
                dao.deleteById(dup.id)
            }
        }
    }

    private suspend fun mergePlayerReferences(dupId: Int, keepId: Int, matchDao: MatchDao) {
        matchDao.reassignEventPlayerId(dupId, keepId)
        matchDao.reassignEventRelatedPlayerId(dupId, keepId)
        val rows = matchDao.getMatchPlayersByPlayer(dupId)
        rows.forEach { row ->
            val existing = matchDao.getMatchPlayersByPlayer(keepId)
                .any { it.matchId == row.matchId }
            if (existing) {
                matchDao.deleteMatchPlayerById(row.id)
            } else {
                matchDao.updateMatchPlayerPlayerId(row.id, keepId)
            }
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

    companion object {
        private val locks = mutableMapOf<Int, Mutex>()
        private fun lockFor(teamId: Int): Mutex =
            synchronized(locks) { locks.getOrPut(teamId) { Mutex() } }
    }
}
