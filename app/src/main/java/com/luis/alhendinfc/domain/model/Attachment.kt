package com.luis.alhendinfc.domain.model

import com.luis.alhendinfc.data.sync.AttachmentParentType

data class Attachment(
    val id: Int = 0,
    val syncId: String = "",
    val parentType: String,
    val parentSyncId: String,
    val mimeType: String,
    val name: String,
    val localPath: String,
    val remotePath: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")
    val isPdf: Boolean get() = mimeType.equals("application/pdf", ignoreCase = true)
    val typeLabel: String
        get() = when {
            isImage -> "Imagen"
            isPdf -> "PDF"
            else -> "Documento"
        }
}

object AttachmentRules {
    fun isSupportedParent(parentType: String): Boolean =
        parentType in AttachmentParentType.KNOWN

    fun isTaskImage(attachment: Attachment): Boolean =
        attachment.parentType == AttachmentParentType.TASK && attachment.isImage
}
