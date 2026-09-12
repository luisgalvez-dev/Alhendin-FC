package com.luis.alhendinfc.cloud

import com.luis.alhendinfc.data.local.AttachmentEntity
import com.luis.alhendinfc.data.local.BoardEntity
import com.luis.alhendinfc.data.local.CustomStatTypeEntity
import com.luis.alhendinfc.data.local.MatchEntity
import com.luis.alhendinfc.data.local.MatchEventEntity
import com.luis.alhendinfc.data.local.MatchPlayerEntity
import com.luis.alhendinfc.data.local.OpponentClubEntity
import com.luis.alhendinfc.data.local.OpponentPlayerEntity
import com.luis.alhendinfc.data.local.PlayerEntity
import com.luis.alhendinfc.data.local.RivalAnalysisEntity
import com.luis.alhendinfc.data.local.RivalLinkEntity
import com.luis.alhendinfc.data.local.SeasonFixtureEntity
import com.luis.alhendinfc.data.local.TaskEntity
import com.luis.alhendinfc.data.local.TeamEntity
import com.luis.alhendinfc.data.local.TrainingEntity
import com.luis.alhendinfc.data.local.TrainingTaskEntity
import com.luis.alhendinfc.data.sync.BoardCloudSanitizer
import com.luis.alhendinfc.data.sync.LiveMatchGuard
import com.luis.alhendinfc.domain.model.MatchStatus

/**
 * DTOs cloud: documentId = syncId. Relaciones por *SyncId, nunca Int local.
 * No serializa entidades Room.
 */
object CloudMappers {
    private val INT_IDENTITY_KEYS = setOf(
        "id", "teamId", "playerId", "matchId", "opponentClubId",
        "trainingId", "taskId", "relatedPlayerId", "boardId"
    )

    fun assertNoIntIdentity(data: Map<String, Any?>) {
        INT_IDENTITY_KEYS.forEach { key ->
            check(!data.containsKey(key)) { "El DTO cloud no puede incluir '$key'" }
        }
    }

    fun team(entity: TeamEntity): CloudDoc {
        val data = meta(entity.syncId, entity.createdAt, entity.updatedAt, entity.deletedAt) + mapOf(
            "name" to entity.name,
            "category" to entity.category,
            "season" to entity.season,
            "shieldUri" to CloudUri.portableOrNull(entity.shieldUri)
        )
        assertNoIntIdentity(data)
        return CloudDoc(entity.syncId, data)
    }

    fun player(entity: PlayerEntity, teamSyncId: String): CloudDoc {
        val data = meta(entity.syncId, entity.createdAt, entity.updatedAt, entity.deletedAt) + mapOf(
            "teamSyncId" to teamSyncId,
            "name" to entity.name,
            "alias" to entity.alias,
            "position" to entity.position,
            "jerseyNumber" to entity.jerseyNumber,
            "height" to entity.height,
            "weight" to entity.weight,
            "laterality" to entity.laterality,
            "isActive" to entity.isActive,
            "observations" to entity.observations
        )
        assertNoIntIdentity(data)
        return CloudDoc(entity.syncId, data)
    }

    fun opponentClub(entity: OpponentClubEntity, teamSyncId: String): CloudDoc {
        val data = meta(entity.syncId, entity.createdAt, entity.updatedAt, entity.deletedAt) + mapOf(
            "teamSyncId" to teamSyncId,
            "name" to entity.name,
            "shortName" to entity.shortName,
            "stadium" to entity.stadium,
            "shieldUri" to CloudUri.portableOrNull(entity.shieldUri),
            "kitColors" to entity.kitColors,
            "sortOrder" to entity.sortOrder
        )
        assertNoIntIdentity(data)
        return CloudDoc(entity.syncId, data)
    }

    fun opponentPlayer(entity: OpponentPlayerEntity, opponentClubSyncId: String): CloudDoc {
        val data = meta(entity.syncId, entity.createdAt, entity.updatedAt, entity.deletedAt) + mapOf(
            "opponentClubSyncId" to opponentClubSyncId,
            "name" to entity.name
        )
        assertNoIntIdentity(data)
        return CloudDoc(entity.syncId, data)
    }

    fun rivalAnalysis(entity: RivalAnalysisEntity, opponentClubSyncId: String): CloudDoc {
        val data = meta(entity.syncId, entity.createdAt, entity.updatedAt, entity.deletedAt) + mapOf(
            "opponentClubSyncId" to opponentClubSyncId,
            "usualSystem" to entity.usualSystem,
            "variants" to entity.variants,
            "buildUp" to entity.buildUp,
            "progression" to entity.progression,
            "finalThird" to entity.finalThird,
            "highPress" to entity.highPress,
            "midBlock" to entity.midBlock,
            "lowBlock" to entity.lowBlock,
            "transAttackToDefense" to entity.transAttackToDefense,
            "transDefenseToAttack" to entity.transDefenseToAttack,
            "cornersOffensive" to entity.cornersOffensive,
            "cornersDefensive" to entity.cornersDefensive,
            "setPieces" to entity.setPieces,
            "strengths" to entity.strengths,
            "weaknesses" to entity.weaknesses,
            "keyPlayers" to entity.keyPlayers,
            "generalNotes" to entity.generalNotes
        )
        assertNoIntIdentity(data)
        return CloudDoc(entity.syncId, data)
    }

    fun rivalLink(entity: RivalLinkEntity, opponentClubSyncId: String): CloudDoc {
        val data = meta(entity.syncId, entity.createdAt, entity.updatedAt, entity.deletedAt) + mapOf(
            "opponentClubSyncId" to opponentClubSyncId,
            "type" to entity.type,
            "label" to entity.label,
            "url" to entity.url,
            "sortOrder" to entity.sortOrder
        )
        assertNoIntIdentity(data)
        return CloudDoc(entity.syncId, data)
    }

    fun seasonFixture(
        entity: SeasonFixtureEntity,
        teamSyncId: String,
        opponentClubSyncId: String
    ): CloudDoc {
        val data = meta(entity.syncId, entity.createdAt, entity.updatedAt, entity.deletedAt) + mapOf(
            "teamSyncId" to teamSyncId,
            "opponentClubSyncId" to opponentClubSyncId,
            "matchday" to entity.matchday,
            "isHome" to entity.isHome,
            "date" to entity.date,
            "time" to entity.time,
            "stadiumOverride" to entity.stadiumOverride,
            "dateEpochDay" to entity.dateEpochDay
        )
        assertNoIntIdentity(data)
        return CloudDoc(entity.syncId, data)
    }

    fun match(
        entity: MatchEntity,
        teamSyncId: String,
        opponentClubSyncId: String?
    ): CloudDoc {
        val finished = entity.status == MatchStatus.FINISHED.name
        val data = buildMap {
            putAll(meta(entity.syncId, entity.createdAt, entity.updatedAt, entity.deletedAt))
            put("teamSyncId", teamSyncId)
            put("opponentClubSyncId", opponentClubSyncId)
            put("rival", entity.rival)
            put("stadium", entity.stadium)
            put("date", entity.date)
            put("time", entity.time)
            put("matchday", entity.matchday)
            put("isHome", entity.isHome)
            put("durationPerPart", entity.durationPerPart)
            put("numParts", entity.numParts)
            put("formation", entity.formation)
            put("notes", entity.notes)
            put("status", entity.status)
            put("homeScore", entity.homeScore)
            put("awayScore", entity.awayScore)
            put("rivalShieldUri", CloudUri.portableOrNull(entity.rivalShieldUri))
            put("dateEpochDay", entity.dateEpochDay)
            if (finished && LiveMatchGuard.finishedCarriesFieldSeconds(entity.status, entity.fieldSecondsJson)) {
                put("fieldSecondsJson", entity.fieldSecondsJson)
            }
        }
        assertNoIntIdentity(data)
        return CloudDoc(entity.syncId, data)
    }

    fun matchPlayer(
        entity: MatchPlayerEntity,
        matchSyncId: String,
        playerSyncId: String
    ): CloudDoc {
        val data = meta(entity.syncId, entity.createdAt, entity.updatedAt, entity.deletedAt) + mapOf(
            "matchSyncId" to matchSyncId,
            "playerSyncId" to playerSyncId,
            "callupStatus" to entity.callupStatus
        )
        assertNoIntIdentity(data)
        return CloudDoc(entity.syncId, data)
    }

    fun matchEvent(
        entity: MatchEventEntity,
        matchSyncId: String,
        playerSyncId: String?,
        relatedPlayerSyncId: String?
    ): CloudDoc {
        val data = meta(entity.syncId, entity.createdAt, entity.updatedAt, entity.deletedAt) + mapOf(
            "matchSyncId" to matchSyncId,
            "playerSyncId" to playerSyncId,
            "relatedPlayerSyncId" to relatedPlayerSyncId,
            "typeCode" to entity.typeCode,
            "minute" to entity.minute,
            "period" to entity.period,
            "value" to entity.value
        )
        assertNoIntIdentity(data)
        return CloudDoc(entity.syncId, data)
    }

    fun customStat(entity: CustomStatTypeEntity, teamSyncId: String): CloudDoc {
        val data = meta(entity.syncId, entity.createdAt, entity.updatedAt, entity.deletedAt) + mapOf(
            "teamSyncId" to teamSyncId,
            "code" to entity.code,
            "label" to entity.label,
            "shortLabel" to entity.shortLabel,
            "appliesTo" to entity.appliesTo,
            "sortOrder" to entity.sortOrder,
            "isActive" to entity.isActive
        )
        assertNoIntIdentity(data)
        return CloudDoc(entity.syncId, data)
    }

    fun task(entity: TaskEntity, teamSyncId: String, boardSyncId: String?): CloudDoc {
        val data = meta(entity.syncId, entity.createdAt, entity.updatedAt, entity.deletedAt) + mapOf(
            "teamSyncId" to teamSyncId,
            "boardSyncId" to boardSyncId,
            "name" to entity.name,
            "objective" to entity.objective,
            "playerCount" to entity.playerCount,
            "durationMinutes" to entity.durationMinutes,
            "description" to entity.description
        )
        assertNoIntIdentity(data)
        return CloudDoc(entity.syncId, data)
    }

    fun training(
        entity: TrainingEntity,
        teamSyncId: String,
        opponentClubSyncId: String?
    ): CloudDoc {
        val data = meta(entity.syncId, entity.createdAt, entity.updatedAt, entity.deletedAt) + mapOf(
            "teamSyncId" to teamSyncId,
            "opponentClubSyncId" to opponentClubSyncId,
            "date" to entity.date,
            "dateEpochDay" to entity.dateEpochDay,
            "notes" to entity.notes
        )
        assertNoIntIdentity(data)
        return CloudDoc(entity.syncId, data)
    }

    fun trainingTask(
        entity: TrainingTaskEntity,
        trainingSyncId: String,
        taskSyncId: String
    ): CloudDoc {
        val data = meta(entity.syncId, entity.createdAt, entity.updatedAt, entity.deletedAt) + mapOf(
            "trainingSyncId" to trainingSyncId,
            "taskSyncId" to taskSyncId,
            "sortOrder" to entity.sortOrder
        )
        assertNoIntIdentity(data)
        return CloudDoc(entity.syncId, data)
    }

    fun board(entity: BoardEntity, teamSyncId: String): CloudDoc {
        val data = meta(entity.syncId, entity.createdAt, entity.updatedAt, entity.deletedAt) + mapOf(
            "teamSyncId" to teamSyncId,
            "name" to entity.name,
            "sceneVersion" to entity.sceneVersion,
            "sceneJson" to BoardCloudSanitizer.portableSceneJson(entity.sceneJson)
        )
        assertNoIntIdentity(data)
        return CloudDoc(entity.syncId, data)
    }

    fun attachment(entity: AttachmentEntity): CloudDoc {
        val remotePath = CloudUri.portableOrNull(entity.remotePath)
        val data = meta(entity.syncId, entity.createdAt, entity.updatedAt, entity.deletedAt) + mapOf(
            "parentType" to entity.parentType,
            "parentSyncId" to entity.parentSyncId,
            "mime" to entity.mimeType,
            "name" to entity.name,
            "remotePath" to remotePath
        )
        check(!data.containsKey("localPath")) { "El DTO cloud no puede incluir localPath" }
        assertNoIntIdentity(data)
        return CloudDoc(entity.syncId, data)
    }

    fun mergeLocalUri(local: String?, remote: String?): String? {
        val portable = CloudUri.portableOrNull(remote)
        if (portable != null) return portable
        if (CloudUri.isLocal(local)) return local
        return portable
    }

    private fun meta(syncId: String, createdAt: Long, updatedAt: Long, deletedAt: Long?): Map<String, Any?> =
        mapOf(
            "syncId" to syncId,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "deletedAt" to deletedAt
        )
}
