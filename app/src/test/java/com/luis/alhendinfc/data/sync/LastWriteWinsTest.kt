package com.luis.alhendinfc.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class LastWriteWinsTest {

    @Test
    fun remoteNewer_appliesRemote() {
        assertEquals(
            LwwDecision.APPLY_REMOTE,
            LastWriteWins.decide(10L, null, 20L, null)
        )
    }

    @Test
    fun localNewer_keepsLocalAndPush() {
        assertEquals(
            LwwDecision.KEEP_LOCAL_AND_PUSH,
            LastWriteWins.decide(30L, null, 20L, null)
        )
    }

    @Test
    fun equalStamps_noop() {
        assertEquals(
            LwwDecision.NOOP,
            LastWriteWins.decide(15L, null, 15L, null)
        )
    }

    @Test
    fun tombstoneUsesMaxUpdatedOrDeleted() {
        assertEquals(
            LwwDecision.APPLY_REMOTE,
            LastWriteWins.decide(10L, 12L, 11L, 40L)
        )
        assertEquals(
            LwwDecision.KEEP_LOCAL_AND_PUSH,
            LastWriteWins.decide(10L, 50L, 11L, 40L)
        )
    }
}
