package com.luis.alhendinfc.cloud

/**
 * Object path portable del blob. Identidad = [attachmentSyncId], nunca el Int local.
 * Firestore guarda este path, nunca una URL firmada.
 */
object StoragePath {
    private val SEGMENT = Regex("^[A-Za-z0-9._-]{1,64}$")
    private val UNSAFE_NAME = Regex("[^A-Za-z0-9._-]")

    fun blobPath(workspaceId: String, attachmentSyncId: String, originalName: String): String {
        val workspace = requireSafeSegment(workspaceId, "workspaceId")
        val syncId = requireSafeSegment(attachmentSyncId, "attachmentSyncId")
        val fileName = sanitizeFileName(originalName)
        return "workspaces/$workspace/attachments/$syncId/$fileName"
    }

    fun sanitizeFileName(originalName: String): String {
        val trimmed = originalName
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .replace("..", "_")
            .trim()
            .ifBlank { "file" }
        val safe = UNSAFE_NAME.replace(trimmed, "_").take(80)
        return safe.ifBlank { "file" }
    }

    fun isSafeSyncId(value: String): Boolean = SEGMENT.matches(value)

    fun isPortableObjectPath(path: String): Boolean {
        val trimmed = path.trim()
        return trimmed.startsWith("workspaces/") &&
            !trimmed.contains("://") &&
            !trimmed.contains('?') &&
            !trimmed.contains('#') &&
            !trimmed.contains("token=", ignoreCase = true)
    }

    private fun requireSafeSegment(value: String, label: String): String {
        val trimmed = value.trim()
        require(isSafeSyncId(trimmed)) { "$label no es un segmento de Storage válido" }
        require(!trimmed.contains("..") && !trimmed.contains('/') && !trimmed.contains('\\')) {
            "$label no puede contener separadores"
        }
        return trimmed
    }
}
