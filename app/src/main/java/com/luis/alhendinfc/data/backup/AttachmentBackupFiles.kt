package com.luis.alhendinfc.data.backup

import com.luis.alhendinfc.data.files.LocalFileStore
import com.luis.alhendinfc.data.local.AttachmentEntity
import java.io.File

/**
 * Copia los binarios de Attachment desde el ZIP extraído a rutas internas nuevas.
 * No modifica el ZIP original ni los ficheros de extractDir (solo lectura).
 */
internal fun remapRestoredAttachments(
    extractDir: File,
    attachments: List<AttachmentEntity>,
    store: LocalFileStore
): List<AttachmentEntity> = attachments.map { att ->
    val candidates = listOfNotNull(
        att.localPath?.let { File(extractDir, it) },
        File(extractDir, "attachments/${att.syncId}")
    )
    val src = candidates.firstOrNull { it.isFile }
    if (src != null) {
        src.inputStream().use { input ->
            att.copy(localPath = store.importStream(input, att.name.ifBlank { "file" }, att.syncId))
        }
    } else {
        att
    }
}
