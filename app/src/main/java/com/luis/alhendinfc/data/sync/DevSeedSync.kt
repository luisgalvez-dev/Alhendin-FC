package com.luis.alhendinfc.data.sync

import androidx.room.withTransaction
import com.luis.alhendinfc.cloud.CloudDoc
import com.luis.alhendinfc.data.local.AlhendinDatabase
import com.luis.alhendinfc.data.local.EntitySync

/**
 * Política DEV para no duplicar `[DEV] ` al unirse a un workspace ya inicializado.
 * Prefijo alineado con DevSeedMarkers. No altera datos reales (sin prefijo).
 */
object DevSeedSync {
    const val PREFIX = "[DEV] "

    fun isDemo(value: String?): Boolean = value?.startsWith(PREFIX) == true

    fun isDemoDoc(doc: CloudDoc): Boolean =
        isDemo(doc.strOrNull("name")) ||
            isDemo(doc.strOrNull("notes")) ||
            isDemo(doc.strOrNull("rival")) ||
            isDemo(doc.strOrNull("label")) ||
            isDemo(doc.strOrNull("generalNotes")) ||
            isDemo(doc.strOrNull("stadiumOverride"))

    suspend fun retractLocalDemosNotIn(db: AlhendinDatabase, remoteSyncIds: Set<String>) {
        val now = EntitySync.now()
        db.withTransaction {
            val unpublishedTeams = db.teamDao().getAllOnce()
                .filter { it.deletedAt == null && isDemo(it.name) && it.syncId !in remoteSyncIds }
            unpublishedTeams.forEach {
                db.teamDao().markDeleted(it.id, now)
                db.syncOutboxDao().delete(SyncEntityType.TEAM, it.syncId)
            }

            db.playerDao().getAllOnce()
                .filter { it.deletedAt == null && isDemo(it.name) && it.syncId !in remoteSyncIds }
                .forEach {
                    db.playerDao().markDeleted(it.id, now)
                    db.syncOutboxDao().delete(SyncEntityType.PLAYER, it.syncId)
                }

            db.taskDao().getAllOnce()
                .filter { it.deletedAt == null && isDemo(it.name) && it.syncId !in remoteSyncIds }
                .forEach {
                    db.taskDao().markDeleted(it.id, now)
                    db.syncOutboxDao().delete(SyncEntityType.TASK, it.syncId)
                }

            db.boardDao().getAllOnce()
                .filter { it.deletedAt == null && isDemo(it.name) && it.syncId !in remoteSyncIds }
                .forEach {
                    db.boardDao().markDeleted(it.id, now)
                    db.syncOutboxDao().delete(SyncEntityType.BOARD, it.syncId)
                }

            val unpublishedDemoClubs = db.opponentClubDao().getAllOnce()
                .filter { it.deletedAt == null && isDemo(it.name) && it.syncId !in remoteSyncIds }
            val unpublishedDemoClubIds = unpublishedDemoClubs.map { it.id }.toSet()
            unpublishedDemoClubs.forEach {
                db.opponentClubDao().markDeleted(it.id, now)
                db.syncOutboxDao().delete(SyncEntityType.OPPONENT_CLUB, it.syncId)
            }

            db.opponentPlayerDao().getAllOnce()
                .filter {
                    it.deletedAt == null &&
                        it.syncId !in remoteSyncIds &&
                        (isDemo(it.name) || it.opponentClubId in unpublishedDemoClubIds)
                }
                .forEach {
                    db.opponentPlayerDao().markDeleted(it.id, now)
                    db.syncOutboxDao().delete(SyncEntityType.OPPONENT_PLAYER, it.syncId)
                }

            val unpublishedMatches = db.matchDao().getAllMatchesOnce()
                .filter {
                    it.deletedAt == null &&
                        it.syncId !in remoteSyncIds &&
                        (isDemo(it.rival) || isDemo(it.notes))
                }
            unpublishedMatches.forEach { match ->
                db.matchEventDao().getByMatchIncludingDeleted(match.id).forEach { event ->
                    db.syncOutboxDao().delete(SyncEntityType.MATCH_EVENT, event.syncId)
                }
                db.matchDao().getMatchPlayersByMatchIncludingDeleted(match.id).forEach { row ->
                    db.syncOutboxDao().delete(SyncEntityType.MATCH_PLAYER, row.syncId)
                }
                db.matchEventDao().markDeletedByMatch(match.id, now)
                db.matchDao().markDeletedPlayersByMatch(match.id, now)
                db.matchDao().markDeleted(match.id, now)
                db.syncOutboxDao().delete(SyncEntityType.MATCH, match.syncId)
            }

            db.seasonFixtureDao().getAllOnce()
                .filter {
                    it.deletedAt == null && isDemo(it.stadiumOverride) && it.syncId !in remoteSyncIds
                }
                .forEach {
                    db.seasonFixtureDao().markDeleted(it.id, now)
                    db.syncOutboxDao().delete(SyncEntityType.SEASON_FIXTURE, it.syncId)
                }

            val unpublishedTrainings = db.trainingDao().getAllOnce()
                .filter { it.deletedAt == null && isDemo(it.notes) && it.syncId !in remoteSyncIds }
            unpublishedTrainings.forEach { training ->
                db.trainingTaskDao().getAllOnce()
                    .filter { it.trainingId == training.id }
                    .forEach { row ->
                        db.syncOutboxDao().delete(SyncEntityType.TRAINING_TASK, row.syncId)
                    }
                db.trainingTaskDao().markDeletedByTraining(training.id, now)
                db.trainingDao().markDeleted(training.id, now)
                db.syncOutboxDao().delete(SyncEntityType.TRAINING, training.syncId)
            }

            db.rivalLinkDao().getAllOnce()
                .filter {
                    it.deletedAt == null &&
                        it.syncId !in remoteSyncIds &&
                        (isDemo(it.label) || it.opponentClubId in unpublishedDemoClubIds)
                }
                .forEach {
                    db.rivalLinkDao().markDeleted(it.id, now)
                    db.syncOutboxDao().delete(SyncEntityType.RIVAL_LINK, it.syncId)
                }

            db.rivalAnalysisDao().getAllOnce()
                .filter {
                    it.deletedAt == null &&
                        it.syncId !in remoteSyncIds &&
                        (isDemo(it.generalNotes) || it.opponentClubId in unpublishedDemoClubIds)
                }
                .forEach {
                    db.rivalAnalysisDao().markDeleted(it.id, now)
                    db.syncOutboxDao().delete(SyncEntityType.RIVAL_ANALYSIS, it.syncId)
                }
            }
    }
}
