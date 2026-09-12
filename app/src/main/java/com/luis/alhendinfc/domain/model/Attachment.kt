package com.luis.alhendinfc.domain.model

import com.luis.alhendinfc.data.sync.AttachmentParentType

data class Attachment(
    val id: Int = 0,
    val syncId: String = "",
    val parentType: String,
    val parentSyncId: String,
    val mimeType: String,
    val name: String,
    val localPath: String? = null,
    val remotePath: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")
    val isPdf: Boolean get() = mimeType.equals("application/pdf", ignoreCase = true)
    val hasLocalCopy: Boolean get() = !localPath.isNullOrBlank()
    val isPendingUpload: Boolean
        get() = deletedAt == null && hasLocalCopy && remotePath.isNullOrBlank()
    val isPendingDownload: Boolean
        get() = deletedAt == null && !hasLocalCopy && !remotePath.isNullOrBlank()
    val typeLabel: String
        get() = when {
            isImage -> "Imagen"
            isPdf -> "PDF"
            else -> "Documento"
        }
    val transferHint: String?
        get() = when {
            isPendingUpload -> "Pendiente de subida"
            isPendingDownload -> "Descargando…"
            else -> null
        }
}

/**
 * Tipos y tamaño máximos de Attachment.
 * Límite: 50 MiB. El plan Free de Supabase Storage es 1 GB; un vídeo de 50 MB
 * cuenta entero. Board Video por encima del límite queda solo local (se rechaza).
 */
object AttachmentRules {
    const val MAX_BYTES: Long = 50L * 1024L * 1024L
    const val MAX_MEGABYTES: Long = 50L

    fun formatMegabytes(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return String.format(java.util.Locale.US, "%.1f", mb)
    }

    fun isSupportedParent(parentType: String): Boolean =
        parentType in AttachmentParentType.KNOWN

    fun isTaskImage(attachment: Attachment): Boolean =
        attachment.parentType == AttachmentParentType.TASK && attachment.isImage

    fun isAllowedMime(mimeType: String): Boolean {
        val mime = mimeType.trim().lowercase()
        if (mime.isBlank() || mime == "application/octet-stream") return false
        return mime.startsWith("image/") ||
            mime.startsWith("video/") ||
            mime == "application/pdf" ||
            mime == "text/plain" ||
            mime == "application/msword" ||
            mime == "application/vnd.ms-excel" ||
            mime == "application/vnd.ms-powerpoint" ||
            mime.startsWith("application/vnd.openxmlformats-officedocument.")
    }

    fun requireAllowedMime(mimeType: String) {
        require(isAllowedMime(mimeType)) { "Tipo de archivo no permitido: $mimeType" }
    }

    fun requireSize(bytes: Long) {
        require(bytes >= 0L) { "Tamaño de archivo inválido" }
        require(bytes <= MAX_BYTES) {
            "El archivo pesa ${formatMegabytes(bytes)} MB y supera el límite de $MAX_MEGABYTES MB"
        }
    }
}
