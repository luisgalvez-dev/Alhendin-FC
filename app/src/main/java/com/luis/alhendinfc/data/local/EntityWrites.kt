package com.luis.alhendinfc.data.local

import com.luis.alhendinfc.domain.model.CalendarDate

/** Inserts/updates que preservan syncId/createdAt/deletedAt y recalculan dateEpochDay. */
object EntityWrites {

    fun teamForInsert(team: TeamEntity, now: Long): TeamEntity {
        val stamp = EntitySync.stampInsert(now)
        return team.copy(
            syncId = stamp.syncId,
            createdAt = stamp.createdAt,
            updatedAt = stamp.updatedAt,
            deletedAt = null
        )
    }

    fun teamForUpdate(existing: TeamEntity, incoming: TeamEntity, now: Long): TeamEntity {
        val sportsChanged =
            existing.name != incoming.name ||
                existing.category != incoming.category ||
                existing.season != incoming.season ||
                existing.shieldUri != incoming.shieldUri
        return incoming.copy(
            syncId = existing.syncId,
            createdAt = existing.createdAt,
            deletedAt = existing.deletedAt,
            isSelected = existing.isSelected,
            updatedAt = if (sportsChanged) now else existing.updatedAt
        )
    }

    fun playerForInsert(player: PlayerEntity, now: Long): PlayerEntity {
        val stamp = EntitySync.stampInsert(now)
        return player.copy(
            syncId = stamp.syncId,
            createdAt = stamp.createdAt,
            updatedAt = stamp.updatedAt,
            deletedAt = null
        )
    }

    fun playerForUpdate(existing: PlayerEntity, incoming: PlayerEntity, now: Long): PlayerEntity =
        incoming.copy(
            syncId = existing.syncId,
            createdAt = existing.createdAt,
            deletedAt = existing.deletedAt,
            updatedAt = now
        )

    fun matchForInsert(match: MatchEntity, now: Long): MatchEntity {
        val stamp = EntitySync.stampInsert(now)
        return match.copy(
            syncId = stamp.syncId,
            createdAt = stamp.createdAt,
            updatedAt = stamp.updatedAt,
            deletedAt = null,
            dateEpochDay = CalendarDate.toEpochDay(match.date)
        )
    }

    fun matchForUpdate(existing: MatchEntity, incoming: MatchEntity, now: Long): MatchEntity =
        incoming.copy(
            syncId = existing.syncId,
            createdAt = existing.createdAt,
            deletedAt = existing.deletedAt,
            updatedAt = now,
            dateEpochDay = CalendarDate.toEpochDay(incoming.date),
            livePeriod = existing.livePeriod,
            liveElapsedSeconds = existing.liveElapsedSeconds,
            liveClockRunning = existing.liveClockRunning,
            liveClockAnchorWallMs = existing.liveClockAnchorWallMs,
            fieldSecondsJson = existing.fieldSecondsJson,
            fieldPositionsJson = existing.fieldPositionsJson
        )

    fun clubForInsert(club: OpponentClubEntity, now: Long): OpponentClubEntity {
        val stamp = EntitySync.stampInsert(now)
        return club.copy(
            syncId = stamp.syncId,
            createdAt = stamp.createdAt,
            updatedAt = stamp.updatedAt,
            deletedAt = null
        )
    }

    fun clubForUpdate(existing: OpponentClubEntity, incoming: OpponentClubEntity, now: Long): OpponentClubEntity =
        incoming.copy(
            syncId = existing.syncId,
            createdAt = existing.createdAt,
            deletedAt = existing.deletedAt,
            updatedAt = now
        )

    fun clubForRevive(existing: OpponentClubEntity, incoming: OpponentClubEntity, now: Long): OpponentClubEntity =
        clubForUpdate(existing, incoming, now).copy(deletedAt = null, updatedAt = now)

    fun fixtureForInsert(fixture: SeasonFixtureEntity, now: Long): SeasonFixtureEntity {
        val stamp = EntitySync.stampInsert(now)
        return fixture.copy(
            syncId = stamp.syncId,
            createdAt = stamp.createdAt,
            updatedAt = stamp.updatedAt,
            deletedAt = null,
            dateEpochDay = CalendarDate.toEpochDay(fixture.date)
        )
    }

    fun fixtureForUpdate(
        existing: SeasonFixtureEntity,
        incoming: SeasonFixtureEntity,
        now: Long
    ): SeasonFixtureEntity =
        incoming.copy(
            id = existing.id,
            syncId = existing.syncId,
            createdAt = existing.createdAt,
            deletedAt = existing.deletedAt,
            updatedAt = now,
            dateEpochDay = CalendarDate.toEpochDay(incoming.date)
        )

    fun fixtureForRevive(
        existing: SeasonFixtureEntity,
        incoming: SeasonFixtureEntity,
        now: Long
    ): SeasonFixtureEntity =
        fixtureForUpdate(existing, incoming, now).copy(deletedAt = null, updatedAt = now)

    fun statForInsert(stat: CustomStatTypeEntity, now: Long): CustomStatTypeEntity {
        val createdAt = if (stat.createdAt > 0L) stat.createdAt else now
        return stat.copy(
            syncId = EntitySync.newSyncId(),
            createdAt = createdAt,
            updatedAt = createdAt,
            deletedAt = null
        )
    }

    fun statForUpdate(
        existing: CustomStatTypeEntity,
        incoming: CustomStatTypeEntity,
        now: Long
    ): CustomStatTypeEntity =
        incoming.copy(
            syncId = existing.syncId,
            createdAt = existing.createdAt,
            deletedAt = existing.deletedAt,
            updatedAt = now
        )

    fun statForRevive(
        existing: CustomStatTypeEntity,
        incoming: CustomStatTypeEntity,
        now: Long
    ): CustomStatTypeEntity =
        statForUpdate(existing, incoming, now).copy(deletedAt = null, isActive = true, updatedAt = now)

    fun eventForInsert(event: MatchEventEntity, now: Long): MatchEventEntity {
        val createdAt = if (event.createdAt > 0L) event.createdAt else now
        return event.copy(
            syncId = EntitySync.newSyncId(),
            createdAt = createdAt,
            updatedAt = createdAt,
            deletedAt = null
        )
    }

    fun taskForInsert(task: TaskEntity, now: Long): TaskEntity {
        val stamp = EntitySync.stampInsert(now)
        return task.copy(
            syncId = stamp.syncId,
            createdAt = stamp.createdAt,
            updatedAt = stamp.updatedAt,
            deletedAt = null
        )
    }

    fun taskForUpdate(existing: TaskEntity, incoming: TaskEntity, now: Long): TaskEntity =
        incoming.copy(
            id = existing.id,
            syncId = existing.syncId,
            createdAt = existing.createdAt,
            deletedAt = existing.deletedAt,
            updatedAt = now
        )

    fun trainingForInsert(training: TrainingEntity, now: Long): TrainingEntity {
        val stamp = EntitySync.stampInsert(now)
        val epoch = CalendarDate.toEpochDay(training.date)
            ?: error("La fecha del entrenamiento no es válida")
        return training.copy(
            syncId = stamp.syncId,
            createdAt = stamp.createdAt,
            updatedAt = stamp.updatedAt,
            deletedAt = null,
            dateEpochDay = epoch
        )
    }

    fun trainingForUpdate(existing: TrainingEntity, incoming: TrainingEntity, now: Long): TrainingEntity =
        incoming.copy(
            id = existing.id,
            syncId = existing.syncId,
            createdAt = existing.createdAt,
            deletedAt = existing.deletedAt,
            updatedAt = now,
            date = existing.date,
            dateEpochDay = existing.dateEpochDay,
            teamId = existing.teamId
        )

    fun trainingTaskForInsert(row: TrainingTaskEntity, now: Long): TrainingTaskEntity {
        val stamp = EntitySync.stampInsert(now)
        return row.copy(
            syncId = stamp.syncId,
            createdAt = stamp.createdAt,
            updatedAt = stamp.updatedAt,
            deletedAt = null
        )
    }

    fun trainingTaskForRevive(existing: TrainingTaskEntity, sortOrder: Int, now: Long): TrainingTaskEntity =
        existing.copy(deletedAt = null, updatedAt = now, sortOrder = sortOrder)

    fun rivalAnalysisForInsert(row: RivalAnalysisEntity, now: Long): RivalAnalysisEntity {
        val stamp = EntitySync.stampInsert(now)
        return row.copy(
            syncId = stamp.syncId,
            createdAt = stamp.createdAt,
            updatedAt = stamp.updatedAt,
            deletedAt = null
        )
    }

    fun rivalAnalysisForUpdate(
        existing: RivalAnalysisEntity,
        incoming: RivalAnalysisEntity,
        now: Long
    ): RivalAnalysisEntity =
        incoming.copy(
            id = existing.id,
            opponentClubId = existing.opponentClubId,
            syncId = existing.syncId,
            createdAt = existing.createdAt,
            deletedAt = existing.deletedAt,
            updatedAt = now
        )

    fun rivalAnalysisForRevive(
        existing: RivalAnalysisEntity,
        incoming: RivalAnalysisEntity,
        now: Long
    ): RivalAnalysisEntity =
        rivalAnalysisForUpdate(existing, incoming, now).copy(deletedAt = null, updatedAt = now)

    fun rivalLinkForInsert(row: RivalLinkEntity, now: Long): RivalLinkEntity {
        val stamp = EntitySync.stampInsert(now)
        return row.copy(
            syncId = stamp.syncId,
            createdAt = stamp.createdAt,
            updatedAt = stamp.updatedAt,
            deletedAt = null
        )
    }

    fun rivalLinkForUpdate(
        existing: RivalLinkEntity,
        incoming: RivalLinkEntity,
        now: Long
    ): RivalLinkEntity =
        incoming.copy(
            id = existing.id,
            opponentClubId = existing.opponentClubId,
            syncId = existing.syncId,
            createdAt = existing.createdAt,
            deletedAt = existing.deletedAt,
            updatedAt = now
        )

    fun rivalLinkForRevive(
        existing: RivalLinkEntity,
        incoming: RivalLinkEntity,
        now: Long
    ): RivalLinkEntity =
        rivalLinkForUpdate(existing, incoming, now).copy(deletedAt = null, updatedAt = now)

    fun opponentPlayerForInsert(row: OpponentPlayerEntity, now: Long): OpponentPlayerEntity {
        val stamp = EntitySync.stampInsert(now)
        return row.copy(
            name = row.name.trim(),
            syncId = stamp.syncId,
            createdAt = stamp.createdAt,
            updatedAt = stamp.updatedAt,
            deletedAt = null
        )
    }

    fun opponentPlayerForUpdate(
        existing: OpponentPlayerEntity,
        incoming: OpponentPlayerEntity,
        now: Long
    ): OpponentPlayerEntity =
        incoming.copy(
            id = existing.id,
            opponentClubId = existing.opponentClubId,
            name =             incoming.name.trim(),
            syncId = existing.syncId,
            createdAt = existing.createdAt,
            deletedAt = existing.deletedAt,
            updatedAt = now
        )

    fun boardForInsert(row: BoardEntity, now: Long): BoardEntity {
        val stamp = EntitySync.stampInsert(now)
        return row.copy(
            name = row.name.trim(),
            syncId = stamp.syncId,
            createdAt = stamp.createdAt,
            updatedAt = stamp.updatedAt,
            deletedAt = null
        )
    }

    fun boardForUpdate(
        existing: BoardEntity,
        incoming: BoardEntity,
        now: Long
    ): BoardEntity =
        incoming.copy(
            id = existing.id,
            teamId = existing.teamId,
            name = incoming.name.trim(),
            syncId = existing.syncId,
            createdAt = existing.createdAt,
            deletedAt = existing.deletedAt,
            updatedAt = now
        )

    fun attachmentForInsert(row: AttachmentEntity, now: Long): AttachmentEntity {
        val stamp = EntitySync.stampInsert(now)
        return row.copy(
            syncId = if (row.syncId.isNotBlank()) row.syncId else stamp.syncId,
            createdAt = stamp.createdAt,
            updatedAt = stamp.updatedAt,
            deletedAt = null,
            remotePath = null
        )
    }

    fun <T> applyTombstone(copy: (deletedAt: Long, updatedAt: Long) -> T, now: Long): T {
        val stamp = EntitySync.stampTombstone(now)
        return copy(stamp.deletedAt!!, stamp.updatedAt)
    }
}
