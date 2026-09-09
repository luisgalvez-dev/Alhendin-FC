package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.local.PlayerDao
import com.luis.alhendinfc.data.local.PlayerEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class InMemoryPlayerDao : PlayerDao {
    private val rows = mutableListOf<PlayerEntity>()
    private val state = MutableStateFlow<List<PlayerEntity>>(emptyList())
    private var seq = 1

    private fun publish() {
        state.value = rows.toList()
    }

    override fun getAllByTeam(teamId: Int): Flow<List<PlayerEntity>> =
        state.map { list ->
            list.filter { it.teamId == teamId && it.deletedAt == null }
                .sortedWith(compareBy<PlayerEntity> { it.jerseyNumber }.thenBy { it.id })
        }

    override fun getById(id: Int): Flow<PlayerEntity?> =
        state.map { list -> list.firstOrNull { it.id == id && it.deletedAt == null } }

    override suspend fun getByIdOnce(id: Int): PlayerEntity? =
        rows.firstOrNull { it.id == id && it.deletedAt == null }

    override suspend fun getAllOnce(): List<PlayerEntity> = rows.sortedBy { it.id }

    override suspend fun insert(player: PlayerEntity): Long {
        val id = if (player.id == 0) seq++ else player.id
        if (id >= seq) seq = id + 1
        rows.add(player.copy(id = id))
        publish()
        return id.toLong()
    }

    override suspend fun insertAll(players: List<PlayerEntity>) {
        players.forEach { insert(it) }
    }

    override suspend fun countByTeam(teamId: Int): Int =
        rows.count { it.teamId == teamId && it.deletedAt == null }

    override suspend fun getAllByTeamOnce(teamId: Int): List<PlayerEntity> =
        rows.filter { it.teamId == teamId && it.deletedAt == null }
            .sortedWith(compareBy<PlayerEntity> { it.jerseyNumber }.thenBy { it.id })

    override suspend fun update(player: PlayerEntity) {
        val index = rows.indexOfFirst { it.id == player.id }
        if (index >= 0) {
            rows[index] = player
            publish()
        }
    }

    override suspend fun markDeleted(id: Int, now: Long) {
        val index = rows.indexOfFirst { it.id == id && it.deletedAt == null }
        if (index >= 0) {
            rows[index] = rows[index].copy(deletedAt = now, updatedAt = now)
            publish()
        }
    }
}
