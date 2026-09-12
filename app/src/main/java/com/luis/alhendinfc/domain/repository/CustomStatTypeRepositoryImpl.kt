package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.local.CustomStatTypeDao
import com.luis.alhendinfc.data.local.CustomStatTypeEntity
import com.luis.alhendinfc.data.local.EntitySync
import com.luis.alhendinfc.data.local.EntityWrites
import com.luis.alhendinfc.data.sync.SyncEntityType
import com.luis.alhendinfc.data.sync.SyncHooks
import com.luis.alhendinfc.domain.model.CustomStatAppliesTo
import com.luis.alhendinfc.domain.model.CustomStatType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class CustomStatTypeRepositoryImpl(
    private val dao: CustomStatTypeDao
) : CustomStatTypeRepository {

    override fun getByTeam(teamId: Int): Flow<List<CustomStatType>> =
        dao.getByTeam(teamId).map { list -> list.map { it.toDomain() } }

    override fun getActiveByTeam(teamId: Int): Flow<List<CustomStatType>> =
        dao.getActiveByTeam(teamId).map { list -> list.map { it.toDomain() } }

    override suspend fun add(type: CustomStatType): Int {
        val now = EntitySync.now()
        val code = type.code.ifBlank { "CUSTOM_${UUID.randomUUID().toString().take(8)}" }
        val incoming = type.copy(code = code)
        val existing = dao.getByTeamAndCodeIncludingDeleted(type.teamId, code)
        if (existing != null && existing.deletedAt != null) {
            SyncHooks.local(SyncEntityType.CUSTOM_STAT, existing.syncId) {
                dao.update(EntityWrites.statForRevive(existing, incoming.toEntity(), now))
            }
            return existing.id
        }
        val stamped = EntityWrites.statForInsert(incoming.toEntity(), now)
        return SyncHooks.local(SyncEntityType.CUSTOM_STAT, stamped.syncId) {
            dao.insert(stamped).toInt()
        }
    }

    override suspend fun update(type: CustomStatType) {
        val existing = dao.getById(type.id) ?: return
        SyncHooks.local(SyncEntityType.CUSTOM_STAT, existing.syncId) {
            dao.update(EntityWrites.statForUpdate(existing, type.toEntity(), EntitySync.now()))
        }
    }

    override suspend fun deleteOrDeactivate(type: CustomStatType) {
        val used = dao.countEventsWithCode(type.code, type.teamId)
        if (used > 0) {
            val existing = dao.getById(type.id) ?: return
            SyncHooks.local(SyncEntityType.CUSTOM_STAT, existing.syncId) {
                dao.update(
                    EntityWrites.statForUpdate(
                        existing,
                        type.copy(isActive = false).toEntity(),
                        EntitySync.now()
                    )
                )
            }
        } else {
            val existing = dao.getById(type.id) ?: return
            SyncHooks.local(SyncEntityType.CUSTOM_STAT, existing.syncId) {
                dao.markDeleted(type.id, EntitySync.now())
            }
        }
    }

    private fun CustomStatTypeEntity.toDomain() = CustomStatType(
        id = id,
        teamId = teamId,
        code = code,
        label = label,
        shortLabel = shortLabel,
        appliesTo = CustomStatAppliesTo.fromName(appliesTo),
        sortOrder = sortOrder,
        isActive = isActive,
        createdAt = createdAt
    )

    private fun CustomStatType.toEntity() = CustomStatTypeEntity(
        id = id,
        teamId = teamId,
        code = code,
        label = label,
        shortLabel = shortLabel,
        appliesTo = appliesTo.name,
        sortOrder = sortOrder,
        isActive = isActive,
        createdAt = createdAt
    )
}
