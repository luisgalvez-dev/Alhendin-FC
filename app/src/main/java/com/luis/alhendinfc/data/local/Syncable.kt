package com.luis.alhendinfc.data.local

/**
 * Contrato mínimo de identidad y ciclo de vida para entidades actuales y futuras
 * (Task, Training, Board, Attachment, RivalAnalysis).
 *
 * Room sigue usando [id] Int local. [syncId] es la identidad estable entre dispositivos.
 * Las bajas deportivas son lógicas: [deletedAt] != null.
 */
interface Syncable {
    val syncId: String
    val createdAt: Long
    val updatedAt: Long
    val deletedAt: Long?
}

data class SyncStamp(
    val syncId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)
