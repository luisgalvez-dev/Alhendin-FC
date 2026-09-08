package com.luis.alhendinfc.data.sync

import com.luis.alhendinfc.data.local.TeamEntity

/** Proyección compartida de Team. [TeamEntity.isSelected] es preferencia local. */
data class TeamSyncProjection(
    val syncId: String,
    val name: String,
    val category: String,
    val season: String,
    val shieldUri: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)

object TeamSyncMapper {
    fun toProjection(team: TeamEntity): TeamSyncProjection =
        TeamSyncProjection(
            syncId = team.syncId,
            name = team.name,
            category = team.category,
            season = team.season,
            shieldUri = team.shieldUri,
            createdAt = team.createdAt,
            updatedAt = team.updatedAt,
            deletedAt = team.deletedAt
        )
}
