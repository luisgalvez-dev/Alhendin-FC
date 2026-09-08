package com.luis.alhendinfc.data.local

import com.luis.alhendinfc.domain.model.CalendarDate

/** Inserts/updates que preservan syncId/createdAt/deletedAt y recalculan dateEpochDay. */
object EntityWrites {

    fun teamForInsert(team: TeamEntity, now: Long): TeamEntity {
        val (syncId, created, updated) = EntitySync.stampInsert(now)
        return team.copy(syncId = syncId, createdAt = created, updatedAt = updated, deletedAt = null)
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
        val (syncId, created, updated) = EntitySync.stampInsert(now)
        return player.copy(syncId = syncId, createdAt = created, updatedAt = updated, deletedAt = null)
    }

    fun playerForUpdate(existing: PlayerEntity, incoming: PlayerEntity, now: Long): PlayerEntity =
        incoming.copy(
            syncId = existing.syncId,
            createdAt = existing.createdAt,
            deletedAt = existing.deletedAt,
            updatedAt = now
        )

    fun matchForInsert(match: MatchEntity, now: Long): MatchEntity {
        val (syncId, created, updated) = EntitySync.stampInsert(now)
        return match.copy(
            syncId = syncId,
            createdAt = created,
            updatedAt = updated,
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
        val (syncId, created, updated) = EntitySync.stampInsert(now)
        return club.copy(syncId = syncId, createdAt = created, updatedAt = updated, deletedAt = null)
    }

    fun clubForUpdate(existing: OpponentClubEntity, incoming: OpponentClubEntity, now: Long): OpponentClubEntity =
        incoming.copy(
            syncId = existing.syncId,
            createdAt = existing.createdAt,
            deletedAt = existing.deletedAt,
            updatedAt = now
        )

    fun fixtureForInsert(fixture: SeasonFixtureEntity, now: Long): SeasonFixtureEntity {
        val (syncId, created, updated) = EntitySync.stampInsert(now)
        return fixture.copy(
            syncId = syncId,
            createdAt = created,
            updatedAt = updated,
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

    fun eventForInsert(event: MatchEventEntity, now: Long): MatchEventEntity {
        val createdAt = if (event.createdAt > 0L) event.createdAt else now
        return event.copy(
            syncId = EntitySync.newSyncId(),
            createdAt = createdAt,
            updatedAt = createdAt,
            deletedAt = null
        )
    }
}
