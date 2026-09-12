package com.luis.alhendinfc.data.files

import android.net.Uri
import com.luis.alhendinfc.data.sync.AttachmentParentType
import com.luis.alhendinfc.domain.model.SharedMedia
import com.luis.alhendinfc.domain.repository.AttachmentRepository
import java.io.File
import java.util.UUID

/** Copia una imagen al almacén privado y la registra como slot compartido. */
class SharedMediaWriter(
    private val attachments: AttachmentRepository,
    private val files: AndroidAttachmentStore
) {
    suspend fun setFromUri(slot: String, parentSyncId: String, uri: Uri): String {
        require(SharedMedia.isSlot(slot)) { "Slot de media no soportado" }
        require(parentSyncId.isNotBlank()) { "parentSyncId vacío" }
        val attachmentSync = UUID.randomUUID().toString()
        val name = files.queryDisplayName(uri) ?: "imagen"
        val mime = files.queryMimeType(uri)?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
        SharedMedia.requireImageMime(mime)
        files.querySize(uri)?.let { SharedMedia.requireSize(it) }
        val path = files.importUriValidated(uri, name, attachmentSync, mime)
        try {
            SharedMedia.requireSize(File(path).length())
        } catch (e: IllegalArgumentException) {
            File(path).delete()
            throw e
        }
        attachments.setSlotImage(slot, parentSyncId, mime, name, path, attachmentSync)
        return path
    }

    suspend fun clear(slot: String, parentSyncId: String) {
        if (parentSyncId.isBlank()) return
        attachments.clearSlot(slot, parentSyncId)
    }

    suspend fun setPlayerPhoto(parentSyncId: String, uri: Uri) =
        setFromUri(AttachmentParentType.PLAYER_PHOTO, parentSyncId, uri)

    suspend fun setTeamShield(parentSyncId: String, uri: Uri) =
        setFromUri(AttachmentParentType.TEAM_SHIELD, parentSyncId, uri)

    suspend fun setOpponentShield(parentSyncId: String, uri: Uri) =
        setFromUri(AttachmentParentType.OPPONENT_SHIELD, parentSyncId, uri)

    suspend fun applyPicked(
        slot: String,
        parentSyncId: String,
        picked: Uri?,
        clear: Boolean
    ) {
        if (parentSyncId.isBlank()) return
        when {
            clear -> clear(slot, parentSyncId)
            picked != null -> setFromUri(slot, parentSyncId, picked)
        }
    }
}
