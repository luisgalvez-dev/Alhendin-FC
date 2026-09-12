package com.luis.alhendinfc.data.sync

enum class LwwDecision {
    APPLY_REMOTE,
    KEEP_LOCAL_AND_PUSH,
    NOOP
}

/**
 * Last-write-wins por [updatedAt] de cliente (trabajo offline).
 * Relojes desfasados entre móviles pueden hacer que un cambio antiguo gane.
 * No se usa timestamp de servidor como fuente de verdad en esta fase.
 */
object LastWriteWins {
    fun decide(localUpdatedAt: Long, localDeletedAt: Long?, remoteUpdatedAt: Long, remoteDeletedAt: Long?): LwwDecision {
        val localStamp = maxOf(localUpdatedAt, localDeletedAt ?: 0L)
        val remoteStamp = maxOf(remoteUpdatedAt, remoteDeletedAt ?: 0L)
        return when {
            remoteStamp > localStamp -> LwwDecision.APPLY_REMOTE
            localStamp > remoteStamp -> LwwDecision.KEEP_LOCAL_AND_PUSH
            else -> LwwDecision.NOOP
        }
    }
}
