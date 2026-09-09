package com.luis.alhendinfc.data.sync

object AttachmentParentType {
    const val TRAINING = "TRAINING"
    const val TASK = "TASK"
    const val MATCH = "MATCH"
    const val OPPONENT = "OPPONENT"
    const val BOARD = "BOARD"

    val KNOWN = setOf(TRAINING, TASK, MATCH, OPPONENT, BOARD)
}

/**
 * Contrato de Attachment. Room persiste [com.luis.alhendinfc.data.local.AttachmentEntity].
 * parentType actuales: TRAINING, TASK. Preparado para MATCH, OPPONENT, BOARD.
 */
data class AttachmentContract(
    val id: Int = 0,
    val syncId: String,
    val parentType: String,
    val parentSyncId: String,
    val mimeType: String,
    val name: String,
    val localPath: String,
    val remotePath: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)
