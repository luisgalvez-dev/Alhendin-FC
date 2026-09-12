package com.luis.alhendinfc.data.sync

object AttachmentParentType {
    const val TRAINING = "TRAINING"
    const val TASK = "TASK"
    const val MATCH = "MATCH"
    const val OPPONENT = "OPPONENT"
    const val BOARD = "BOARD"
    const val PLAYER_PHOTO = "PLAYER_PHOTO"
    const val TEAM_SHIELD = "TEAM_SHIELD"
    const val OPPONENT_SHIELD = "OPPONENT_SHIELD"

    val MEDIA_SLOTS = setOf(PLAYER_PHOTO, TEAM_SHIELD, OPPONENT_SHIELD)
    val KNOWN = setOf(TRAINING, TASK, MATCH, OPPONENT, BOARD) + MEDIA_SLOTS
}

/**
 * Contrato de Attachment. Room persiste [com.luis.alhendinfc.data.local.AttachmentEntity].
 * parentType: TRAINING, TASK, MATCH, OPPONENT, BOARD,
 * PLAYER_PHOTO, TEAM_SHIELD, OPPONENT_SHIELD.
 * [localPath] es solo local; [remotePath] es la ruta de Storage.
 */
data class AttachmentContract(
    val id: Int = 0,
    val syncId: String,
    val parentType: String,
    val parentSyncId: String,
    val mimeType: String,
    val name: String,
    val localPath: String? = null,
    val remotePath: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)
