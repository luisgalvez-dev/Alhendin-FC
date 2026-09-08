package com.luis.alhendinfc.data.sync

/**
 * Contrato conceptual para la futura tabla Attachment.
 * No hay entidad Room ni UI en esta fase.
 *
 * parentType ejemplos: "match", "training", "opponent_club", "board".
 * La consulta de un informe de partido desde el rival usa parentSyncId del Match
 * y el opponentClubId del partido: no se duplica el fichero.
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
