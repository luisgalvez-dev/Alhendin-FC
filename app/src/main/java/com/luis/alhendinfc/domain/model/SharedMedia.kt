package com.luis.alhendinfc.domain.model

import com.luis.alhendinfc.data.sync.AttachmentParentType

/**
 * Fotos/escudos compartidos reutilizando Attachment.
 * Un slot activo por entidad: PLAYER_PHOTO, TEAM_SHIELD, OPPONENT_SHIELD.
 * Prioridad de visualización: shared localPath → URI legacy → fallback UI.
 */
object SharedMedia {
    const val MAX_BYTES: Long = 8L * 1024L * 1024L
    const val MAX_MEGABYTES: Long = 8L

    val SLOTS = AttachmentParentType.MEDIA_SLOTS

    fun isSlot(parentType: String): Boolean = parentType in SLOTS

    fun isPickerUri(uri: String?): Boolean {
        val v = uri?.trim().orEmpty()
        return v.startsWith("content://", ignoreCase = true)
    }

    /** Ruta que LocalImageLoader puede abrir: content, file o path absoluto. */
    fun isDisplayableLocal(uri: String?): Boolean {
        if (uri.isNullOrBlank()) return false
        val lower = uri.trim().lowercase()
        if (lower.startsWith("http://") || lower.startsWith("https://")) return false
        return lower.startsWith("content://") ||
            lower.startsWith("file://") ||
            uri.trim().startsWith("/")
    }

    /** No persistir content:// del picker como dato portable. */
    fun persistableLegacyUri(uri: String?): String? {
        val v = uri?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (isPickerUri(v)) return null
        return v
    }

    fun displayPath(shared: Attachment?, legacyUri: String?): String? {
        val local = shared?.takeIf { it.deletedAt == null }?.localPath?.trim()?.takeIf { it.isNotEmpty() }
        if (local != null) return local
        return legacyUri?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun byParentSyncId(attachments: List<Attachment>): Map<String, Attachment> =
        attachments.filter { it.deletedAt == null }.associateBy { it.parentSyncId }

    /**
     * Si el partido tiene opponentClubId, el escudo sale del rival (shared o legacy).
     * Si no, se usa Match.rivalShieldUri legacy. Nunca una copia de blob por partido.
     */
    fun rivalDisplayPath(
        opponentClubId: Int?,
        clubLegacyUri: String?,
        matchLegacyUri: String?,
        clubShared: Attachment?
    ): String? {
        if (opponentClubId != null && opponentClubId > 0) {
            return displayPath(clubShared, clubLegacyUri)
        }
        return displayPath(null, matchLegacyUri)
    }

    fun requireImageMime(mimeType: String) {
        val mime = mimeType.trim().lowercase()
        require(mime.startsWith("image/") && mime != "image/") {
            "Solo se aceptan imágenes"
        }
        AttachmentRules.requireAllowedMime(mime)
    }

    fun requireSize(bytes: Long) {
        require(bytes >= 0L) { "Tamaño de archivo inválido" }
        require(bytes <= MAX_BYTES) {
            "La imagen pesa ${AttachmentRules.formatMegabytes(bytes)} MB y supera el límite de $MAX_MEGABYTES MB"
        }
    }
}
