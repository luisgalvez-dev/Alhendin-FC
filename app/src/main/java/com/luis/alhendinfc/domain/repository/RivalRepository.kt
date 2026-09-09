package com.luis.alhendinfc.domain.repository

import com.luis.alhendinfc.data.local.EntitySync
import com.luis.alhendinfc.data.local.EntityWrites
import com.luis.alhendinfc.data.local.OpponentPlayerDao
import com.luis.alhendinfc.data.local.OpponentPlayerEntity
import com.luis.alhendinfc.data.local.RivalAnalysisDao
import com.luis.alhendinfc.data.local.RivalAnalysisEntity
import com.luis.alhendinfc.data.local.RivalLinkDao
import com.luis.alhendinfc.data.local.RivalLinkEntity
import com.luis.alhendinfc.domain.model.OpponentPlayer
import com.luis.alhendinfc.domain.model.OpponentPlayerRules
import com.luis.alhendinfc.domain.model.RivalAnalysis
import com.luis.alhendinfc.domain.model.RivalLink
import com.luis.alhendinfc.domain.model.RivalLinkRules
import com.luis.alhendinfc.domain.model.RivalLinkType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RivalRepository(
    private val analysisDao: RivalAnalysisDao,
    private val linkDao: RivalLinkDao,
    private val playerDao: OpponentPlayerDao
) {
    fun getAnalysis(opponentClubId: Int): Flow<RivalAnalysis?> =
        analysisDao.getActiveByClub(opponentClubId).map { it?.toDomain() }

    suspend fun saveAnalysis(analysis: RivalAnalysis): Int {
        val now = EntitySync.now()
        val existing = analysisDao.getByClubIncludingDeleted(analysis.opponentClubId)
        if (existing != null) {
            val updated = if (existing.deletedAt != null) {
                EntityWrites.rivalAnalysisForRevive(existing, analysis.toEntity(), now)
            } else {
                EntityWrites.rivalAnalysisForUpdate(existing, analysis.toEntity(), now)
            }
            analysisDao.update(updated)
            return existing.id
        }
        return analysisDao.insert(EntityWrites.rivalAnalysisForInsert(analysis.toEntity(), now)).toInt()
    }

    suspend fun deleteAnalysis(analysis: RivalAnalysis) {
        analysisDao.markDeleted(analysis.id, EntitySync.now())
    }

    fun getLinks(opponentClubId: Int): Flow<List<RivalLink>> =
        linkDao.getActiveByClub(opponentClubId).map { list -> list.map { it.toDomain() } }

    suspend fun addLink(link: RivalLink): Int {
        require(RivalLinkRules.isKnownType(link.type)) { "Tipo de enlace no soportado" }
        val now = EntitySync.now()
        val nextOrder = (linkDao.getActiveByClubOnce(link.opponentClubId).maxOfOrNull { it.sortOrder } ?: -1) + 1
        return linkDao.insert(
            EntityWrites.rivalLinkForInsert(link.toEntity().copy(sortOrder = nextOrder), now)
        ).toInt()
    }

    suspend fun updateLink(link: RivalLink) {
        require(RivalLinkRules.isKnownType(link.type)) { "Tipo de enlace no soportado" }
        val existing = linkDao.getByIdOnce(link.id) ?: return
        linkDao.update(EntityWrites.rivalLinkForUpdate(existing, link.toEntity(), EntitySync.now()))
    }

    suspend fun deleteLink(link: RivalLink) {
        linkDao.markDeleted(link.id, EntitySync.now())
    }

    suspend fun moveLink(opponentClubId: Int, linkId: Int, up: Boolean) {
        val rows = linkDao.getActiveByClubOnce(opponentClubId).toMutableList()
        val index = rows.indexOfFirst { it.id == linkId }
        if (index < 0) return
        val swapWith = if (up) index - 1 else index + 1
        if (swapWith !in rows.indices) return
        val now = EntitySync.now()
        val a = rows[index]
        val b = rows[swapWith]
        linkDao.update(a.copy(sortOrder = b.sortOrder, updatedAt = now))
        linkDao.update(b.copy(sortOrder = a.sortOrder, updatedAt = now))
    }

    fun getPlayers(opponentClubId: Int): Flow<List<OpponentPlayer>> =
        playerDao.getActiveByClub(opponentClubId).map { list -> list.map { it.toDomain() } }

    fun searchPlayers(opponentClubId: Int, query: String): Flow<List<OpponentPlayer>> {
        val needle = query.trim()
        if (needle.isEmpty()) return getPlayers(opponentClubId)
        return playerDao.searchByClubAndName(opponentClubId, needle).map { list -> list.map { it.toDomain() } }
    }

    suspend fun addPlayer(player: OpponentPlayer): Int {
        val error = OpponentPlayerRules.validate(player.name)
        require(error == null) { error!! }
        return playerDao.insert(
            EntityWrites.opponentPlayerForInsert(player.toEntity(), EntitySync.now())
        ).toInt()
    }

    suspend fun updatePlayer(player: OpponentPlayer) {
        val error = OpponentPlayerRules.validate(player.name)
        require(error == null) { error!! }
        val existing = playerDao.getByIdOnce(player.id) ?: return
        playerDao.update(EntityWrites.opponentPlayerForUpdate(existing, player.toEntity(), EntitySync.now()))
    }

    suspend fun deletePlayer(player: OpponentPlayer) {
        playerDao.markDeleted(player.id, EntitySync.now())
    }

    private fun RivalAnalysisEntity.toDomain() = RivalAnalysis(
        id = id,
        syncId = syncId,
        opponentClubId = opponentClubId,
        usualSystem = usualSystem,
        variants = variants,
        buildUp = buildUp,
        progression = progression,
        finalThird = finalThird,
        highPress = highPress,
        midBlock = midBlock,
        lowBlock = lowBlock,
        transAttackToDefense = transAttackToDefense,
        transDefenseToAttack = transDefenseToAttack,
        cornersOffensive = cornersOffensive,
        cornersDefensive = cornersDefensive,
        setPieces = setPieces,
        strengths = strengths,
        weaknesses = weaknesses,
        keyPlayers = keyPlayers,
        generalNotes = generalNotes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    private fun RivalAnalysis.toEntity() = RivalAnalysisEntity(
        id = id,
        syncId = syncId,
        opponentClubId = opponentClubId,
        usualSystem = usualSystem,
        variants = variants,
        buildUp = buildUp,
        progression = progression,
        finalThird = finalThird,
        highPress = highPress,
        midBlock = midBlock,
        lowBlock = lowBlock,
        transAttackToDefense = transAttackToDefense,
        transDefenseToAttack = transDefenseToAttack,
        cornersOffensive = cornersOffensive,
        cornersDefensive = cornersDefensive,
        setPieces = setPieces,
        strengths = strengths,
        weaknesses = weaknesses,
        keyPlayers = keyPlayers,
        generalNotes = generalNotes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    private fun RivalLinkEntity.toDomain() = RivalLink(
        id = id,
        syncId = syncId,
        opponentClubId = opponentClubId,
        type = type.ifBlank { RivalLinkType.CUSTOM },
        label = label,
        url = url,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    private fun RivalLink.toEntity() = RivalLinkEntity(
        id = id,
        syncId = syncId,
        opponentClubId = opponentClubId,
        type = type,
        label = label,
        url = url,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    private fun OpponentPlayerEntity.toDomain() = OpponentPlayer(
        id = id,
        syncId = syncId,
        opponentClubId = opponentClubId,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    private fun OpponentPlayer.toEntity() = OpponentPlayerEntity(
        id = id,
        syncId = syncId,
        opponentClubId = opponentClubId,
        name = name.trim(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )
}
