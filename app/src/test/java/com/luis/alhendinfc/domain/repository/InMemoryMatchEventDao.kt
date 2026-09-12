package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.local.MatchEventDao
import com.luis.alhendinfc.data.local.MatchEventEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class InMemoryMatchEventDao : MatchEventDao {
    private val rows = mutableListOf<MatchEventEntity>()
    private val state = MutableStateFlow<List<MatchEventEntity>>(emptyList())
    private var seq = 1

    private fun publish() {
        state.value = rows.toList()
    }

    override fun getEventsByMatch(matchId: Int): Flow<List<MatchEventEntity>> =
        state.map { list ->
            list.filter { it.matchId == matchId && it.deletedAt == null }
                .sortedWith(
                    compareBy<MatchEventEntity> { it.period }.thenBy { it.minute }.thenBy { it.id }
                )
        }

    override fun getEventsByTeam(teamId: Int): Flow<List<MatchEventEntity>> = state.map { emptyList() }

    override suspend fun getAllOnce(): List<MatchEventEntity> = rows.sortedBy { it.id }

    override suspend fun getBySyncIdIncludingDeleted(syncId: String): MatchEventEntity? =
        rows.firstOrNull { it.syncId == syncId }

    override suspend fun getByIdIncludingDeleted(id: Int): MatchEventEntity? =
        rows.firstOrNull { it.id == id }

    override suspend fun getByMatchIncludingDeleted(matchId: Int): List<MatchEventEntity> =
        rows.filter { it.matchId == matchId }

    override suspend fun update(entity: MatchEventEntity) {
        val index = rows.indexOfFirst { it.id == entity.id }
        if (index >= 0) {
            rows[index] = entity
            publish()
        }
    }

    override suspend fun insertAll(events: List<MatchEventEntity>) {
        events.forEach { insert(it) }
    }

    override suspend fun insert(event: MatchEventEntity): Long {
        val id = if (event.id == 0) seq++ else event.id
        if (id >= seq) seq = id + 1
        rows.add(event.copy(id = id))
        publish()
        return id.toLong()
    }

    override suspend fun markDeleted(id: Int, now: Long) {
        val index = rows.indexOfFirst { it.id == id && it.deletedAt == null }
        if (index >= 0) {
            rows[index] = rows[index].copy(deletedAt = now, updatedAt = now)
            publish()
        }
    }

    override suspend fun markDeletedByMatch(matchId: Int, now: Long) {
        rows.replaceAll { row ->
            if (row.matchId == matchId && row.deletedAt == null) row.copy(deletedAt = now, updatedAt = now)
            else row
        }
        publish()
    }
}
