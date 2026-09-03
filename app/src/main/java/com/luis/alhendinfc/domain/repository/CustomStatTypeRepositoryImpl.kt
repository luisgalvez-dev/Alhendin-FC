package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.local.CustomStatTypeDao
import com.luis.alhendinfc.data.local.CustomStatTypeEntity
import com.luis.alhendinfc.domain.model.CustomStatAppliesTo
import com.luis.alhendinfc.domain.model.CustomStatType
import com.luis.alhendinfc.domain.model.SampleCustomStats
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
        val code = type.code.ifBlank { "CUSTOM_${UUID.randomUUID().toString().take(8)}" }
        return dao.insert(
            type.copy(code = code).toEntity()
        ).toInt()
    }

    override suspend fun update(type: CustomStatType) {
        dao.update(type.toEntity())
    }

    override suspend fun deleteOrDeactivate(type: CustomStatType) {
        val used = dao.countEventsWithCode(type.code)
        if (used > 0) {
            dao.update(type.copy(isActive = false).toEntity())
        } else {
            dao.delete(type.toEntity())
        }
    }

    override suspend fun ensureSampleCustomStats(teamId: Int): Boolean {
        if (teamId <= 0) return false
        if (dao.countByTeam(teamId) > 0) return false
        dao.insertAll(SampleCustomStats.createTypes(teamId).map { it.toEntity() })
        return true
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
