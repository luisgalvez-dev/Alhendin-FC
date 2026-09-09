package com.luis.alhendinfc.data.backup

import com.luis.alhendinfc.data.local.CustomStatTypeEntity
import com.luis.alhendinfc.data.local.EntitySync
import com.luis.alhendinfc.data.local.MatchEntity
import com.luis.alhendinfc.data.local.MatchEventEntity
import com.luis.alhendinfc.data.local.MatchPlayerEntity
import com.luis.alhendinfc.data.local.OpponentClubEntity
import com.luis.alhendinfc.data.local.PlayerEntity
import com.luis.alhendinfc.data.local.SeasonFixtureEntity
import com.luis.alhendinfc.data.local.TeamEntity
import com.luis.alhendinfc.domain.model.CalendarDate

object BackupUpgrade {

    fun toV15(payload: ValidatedBackup, now: Long = EntitySync.now()): ValidatedBackup {
        require(payload.schemaVersion == 14) {
            "BackupUpgrade.toV15 solo acepta schema 14 (recibido ${payload.schemaVersion})"
        }
        return payload.copy(
            schemaVersion = 15,
            teams = payload.teams.map { stamp(it, now) },
            players = payload.players.map { stamp(it, now) },
            matches = payload.matches.map { stamp(it, now) },
            matchPlayers = payload.matchPlayers.map { stamp(it, now) },
            events = payload.events.map { stamp(it, now) },
            customStatTypes = payload.customStatTypes.map { stamp(it, now) },
            opponentClubs = payload.opponentClubs.map { stamp(it, now) },
            fixtures = payload.fixtures.map { stamp(it, now) }
        )
    }

    private fun stamp(team: TeamEntity, now: Long) = team.copy(
        syncId = EntitySync.newSyncId(),
        createdAt = now,
        updatedAt = now,
        deletedAt = null
    )

    private fun stamp(player: PlayerEntity, now: Long) = player.copy(
        syncId = EntitySync.newSyncId(),
        createdAt = now,
        updatedAt = now,
        deletedAt = null
    )

    private fun stamp(match: MatchEntity, now: Long) = match.copy(
        syncId = EntitySync.newSyncId(),
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
        dateEpochDay = CalendarDate.toEpochDay(match.date)
    )

    private fun stamp(row: MatchPlayerEntity, now: Long) = row.copy(
        syncId = EntitySync.newSyncId(),
        createdAt = now,
        updatedAt = now,
        deletedAt = null
    )

    private fun stamp(event: MatchEventEntity, now: Long): MatchEventEntity {
        val created = if (event.createdAt > 0L) event.createdAt else now
        return event.copy(
            syncId = EntitySync.newSyncId(),
            createdAt = created,
            updatedAt = created,
            deletedAt = null
        )
    }

    private fun stamp(stat: CustomStatTypeEntity, now: Long): CustomStatTypeEntity {
        val created = if (stat.createdAt > 0L) stat.createdAt else now
        return stat.copy(
            syncId = EntitySync.newSyncId(),
            createdAt = created,
            updatedAt = created,
            deletedAt = null
        )
    }

    private fun stamp(club: OpponentClubEntity, now: Long) = club.copy(
        syncId = EntitySync.newSyncId(),
        createdAt = now,
        updatedAt = now,
        deletedAt = null
    )

    private fun stamp(fixture: SeasonFixtureEntity, now: Long) = fixture.copy(
        syncId = EntitySync.newSyncId(),
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
        dateEpochDay = CalendarDate.toEpochDay(fixture.date)
    )

    fun toV16(payload: ValidatedBackup): ValidatedBackup {
        require(payload.schemaVersion == 15) {
            "BackupUpgrade.toV16 solo acepta schema 15 (recibido ${payload.schemaVersion})"
        }
        return payload.copy(
            schemaVersion = 16,
            tasks = emptyList(),
            counts = payload.counts.copy(tasks = 0)
        )
    }

    fun toV17(payload: ValidatedBackup): ValidatedBackup {
        require(payload.schemaVersion == 16) {
            "BackupUpgrade.toV17 solo acepta schema 16 (recibido ${payload.schemaVersion})"
        }
        return payload.copy(
            schemaVersion = 17,
            trainings = emptyList(),
            trainingTasks = emptyList(),
            attachments = emptyList(),
            counts = payload.counts.copy(trainings = 0, trainingTasks = 0, attachments = 0)
        )
    }

    fun toV18(payload: ValidatedBackup): ValidatedBackup {
        require(payload.schemaVersion == 17) {
            "BackupUpgrade.toV18 solo acepta schema 17 (recibido ${payload.schemaVersion})"
        }
        return payload.copy(
            schemaVersion = 18,
            rivalAnalyses = emptyList(),
            rivalLinks = emptyList(),
            counts = payload.counts.copy(rivalAnalyses = 0, rivalLinks = 0)
        )
    }

    fun toV19(payload: ValidatedBackup): ValidatedBackup {
        require(payload.schemaVersion == 18) {
            "BackupUpgrade.toV19 solo acepta schema 18 (recibido ${payload.schemaVersion})"
        }
        return payload.copy(
            schemaVersion = 19,
            opponentPlayers = emptyList(),
            counts = payload.counts.copy(opponentPlayers = 0)
        )
    }

    fun toV20(payload: ValidatedBackup): ValidatedBackup {
        require(payload.schemaVersion == 19) {
            "BackupUpgrade.toV20 solo acepta schema 19 (recibido ${payload.schemaVersion})"
        }
        return payload.copy(
            schemaVersion = 20,
            boards = emptyList(),
            counts = payload.counts.copy(boards = 0)
        )
    }
}
