package com.luis.alhendinfc.data.backup

import com.luis.alhendinfc.data.local.AlhendinDatabase
import androidx.room.withTransaction

/**
 * Sustitución atómica de la BD: wipe + insert + verificación de conteos
 * ocurren en la misma transacción Room. Si cualquier paso lanza,
 * SQLite deshace el wipe y los inserts parciales.
 */
internal interface BackupMutator {
    suspend fun <R> inTransaction(block: suspend () -> R): R
    suspend fun deleteAllRows()
    suspend fun insertBackup(payload: ValidatedBackup)
    suspend fun readCounts(): BackupCounts
}

internal suspend fun commitValidatedBackup(
    mutator: BackupMutator,
    payload: ValidatedBackup
) {
    mutator.inTransaction {
        mutator.deleteAllRows()
        mutator.insertBackup(payload)
        val actual = mutator.readCounts()
        check(actual == payload.counts) {
            "La restauración no coincide con el backup (esperado=${payload.counts}, actual=$actual)"
        }
        actual
    }
}

internal class RoomBackupMutator(
    private val db: AlhendinDatabase
) : BackupMutator {

    override suspend fun <R> inTransaction(block: suspend () -> R): R =
        db.withTransaction(block)

    override suspend fun deleteAllRows() {
        // Wipe interno de restauración atómica: no es baja deportiva. Las filas
        // (incluidos tombstones) se sustituyen por el payload validado.
        val sql = db.openHelper.writableDatabase
        BackupRepository.ALL_TABLES.forEach { table ->
            sql.execSQL("DELETE FROM `$table`")
        }
        sql.execSQL("DELETE FROM `sync_outbox`")
        sql.execSQL("DELETE FROM `transfer_job`")
    }

    override suspend fun insertBackup(payload: ValidatedBackup) {
        if (payload.teams.isNotEmpty()) db.teamDao().insertAll(payload.teams)
        if (payload.players.isNotEmpty()) db.playerDao().insertAll(payload.players)
        if (payload.matches.isNotEmpty()) db.matchDao().insertMatches(payload.matches)
        if (payload.matchPlayers.isNotEmpty()) db.matchDao().insertMatchPlayers(payload.matchPlayers)
        if (payload.events.isNotEmpty()) db.matchEventDao().insertAll(payload.events)
        if (payload.customStatTypes.isNotEmpty()) db.customStatTypeDao().insertAll(payload.customStatTypes)
        if (payload.opponentClubs.isNotEmpty()) db.opponentClubDao().insertAll(payload.opponentClubs)
        if (payload.fixtures.isNotEmpty()) db.seasonFixtureDao().insertAll(payload.fixtures)
        if (payload.tasks.isNotEmpty()) db.taskDao().insertAll(payload.tasks)
        if (payload.trainings.isNotEmpty()) db.trainingDao().insertAll(payload.trainings)
        if (payload.trainingTasks.isNotEmpty()) db.trainingTaskDao().insertAll(payload.trainingTasks)
        if (payload.attachments.isNotEmpty()) db.attachmentDao().insertAll(payload.attachments)
        if (payload.rivalAnalyses.isNotEmpty()) db.rivalAnalysisDao().insertAll(payload.rivalAnalyses)
        if (payload.rivalLinks.isNotEmpty()) db.rivalLinkDao().insertAll(payload.rivalLinks)
        if (payload.opponentPlayers.isNotEmpty()) db.opponentPlayerDao().insertAll(payload.opponentPlayers)
        if (payload.boards.isNotEmpty()) db.boardDao().insertAll(payload.boards)
        fixSqliteSequences()
    }

    override suspend fun readCounts(): BackupCounts = BackupCounts(
        teams = db.teamDao().getAllOnce().size,
        players = db.playerDao().getAllOnce().size,
        matches = db.matchDao().getAllMatchesOnce().size,
        matchPlayers = db.matchDao().getAllMatchPlayersOnce().size,
        events = db.matchEventDao().getAllOnce().size,
        customStatTypes = db.customStatTypeDao().getAllOnce().size,
        opponentClubs = db.opponentClubDao().getAllOnce().size,
        fixtures = db.seasonFixtureDao().getAllOnce().size,
        tasks = db.taskDao().getAllOnce().size,
        trainings = db.trainingDao().getAllOnce().size,
        trainingTasks = db.trainingTaskDao().getAllOnce().size,
        attachments = db.attachmentDao().getAllOnce().size,
        rivalAnalyses = db.rivalAnalysisDao().getAllOnce().size,
        rivalLinks = db.rivalLinkDao().getAllOnce().size,
        opponentPlayers = db.opponentPlayerDao().getAllOnce().size,
        boards = db.boardDao().getAllOnce().size
    )

    private fun fixSqliteSequences() {
        val sqlDb = db.openHelper.writableDatabase
        BackupRepository.ALL_TABLES.forEach { table ->
            sqlDb.execSQL("DELETE FROM sqlite_sequence WHERE name = ?", arrayOf(table))
            sqlDb.execSQL(
                "INSERT INTO sqlite_sequence(name, seq) " +
                    "SELECT ?, IFNULL(MAX(id), 0) FROM $table",
                arrayOf(table)
            )
        }
    }
}
