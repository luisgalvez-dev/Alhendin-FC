package com.luis.alhendinfc.data.backup

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class BackupAtomicRestoreTest {

    private val original = BackupCounts(
        teams = 2,
        players = 11,
        matches = 3,
        matchPlayers = 20,
        events = 8,
        customStatTypes = 4,
        opponentClubs = 6,
        fixtures = 5
    )

    private val incoming = BackupCounts(
        teams = 1,
        players = 5,
        matches = 1,
        matchPlayers = 5,
        events = 2,
        customStatTypes = 1,
        opponentClubs = 1,
        fixtures = 1
    )

    @Test
    fun invalidJson_doesNotMutateExistingData() = runTest {
        val mutator = FakeBackupMutator(original, failOnInsert = false)
        try {
            val payload = BackupValidator.validateJson("{")
            commitValidatedBackup(mutator, payload)
            fail("La validación debería fallar")
        } catch (_: Exception) {
        }
        assertEquals(original, mutator.counts)
    }

    @Test
    fun incompatibleSchema_doesNotMutateExistingData() = runTest {
        val mutator = FakeBackupMutator(original, failOnInsert = false)
        try {
            val payload = BackupValidator.validateJson(
                BackupValidatorTest.validJson(13)
            )
            commitValidatedBackup(mutator, payload)
            fail("La validación debería fallar")
        } catch (_: Exception) {
        }
        assertEquals(original, mutator.counts)
    }

    @Test
    fun insertFailure_rollsBackToOriginalCounts() = runTest {
        val mutator = FakeBackupMutator(original, failOnInsert = true)
        try {
            commitValidatedBackup(mutator, emptyPayload(incoming))
            fail("La restauración debería fallar")
        } catch (_: IllegalStateException) {
        }
        assertEquals(original, mutator.counts)
    }

    @Test
    fun validBackup_replacesCounts() = runTest {
        val mutator = FakeBackupMutator(original, failOnInsert = false)
        commitValidatedBackup(mutator, emptyPayload(incoming))
        assertEquals(incoming, mutator.counts)
    }

    @Test
    fun countMismatchAfterInsert_rollsBack() = runTest {
        val mutator = FakeBackupMutator(original, failOnInsert = false, distortCounts = true)
        try {
            commitValidatedBackup(mutator, emptyPayload(incoming))
            fail("La verificación de conteos debería fallar")
        } catch (_: IllegalStateException) {
        }
        assertEquals(original, mutator.counts)
    }

    private fun emptyPayload(counts: BackupCounts) = ValidatedBackup(
        schemaVersion = 15,
        teams = emptyList(),
        players = emptyList(),
        matches = emptyList(),
        matchPlayers = emptyList(),
        events = emptyList(),
        customStatTypes = emptyList(),
        opponentClubs = emptyList(),
        fixtures = emptyList(),
        homeLayout = null,
        counts = counts
    )
}

private class FakeBackupMutator(
    initial: BackupCounts,
    private val failOnInsert: Boolean,
    private val distortCounts: Boolean = false
) : BackupMutator {
    var counts: BackupCounts = initial

    override suspend fun <R> inTransaction(block: suspend () -> R): R {
        val snapshot = counts
        return try {
            block()
        } catch (t: Throwable) {
            counts = snapshot
            throw t
        }
    }

    override suspend fun deleteAllRows() {
        counts = BackupCounts(0, 0, 0, 0, 0, 0, 0, 0)
    }

    override suspend fun insertBackup(payload: ValidatedBackup) {
        if (failOnInsert) error("insert failed")
        counts = if (distortCounts) payload.counts.copy(teams = payload.counts.teams + 99)
        else payload.counts
    }

    override suspend fun readCounts(): BackupCounts = counts
}
