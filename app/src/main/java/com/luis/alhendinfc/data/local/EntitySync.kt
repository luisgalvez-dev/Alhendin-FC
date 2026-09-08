package com.luis.alhendinfc.data.local

import java.util.UUID

internal object EntitySync {
    fun newSyncId(): String = UUID.randomUUID().toString()

    fun now(): Long = System.currentTimeMillis()

    fun stampInsert(now: Long = now()): Triple<String, Long, Long> =
        Triple(newSyncId(), now, now)
}
