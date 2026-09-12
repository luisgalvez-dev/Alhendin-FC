package com.luis.alhendinfc.data.files

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.InputStream

/**
 * Copia ficheros al almacenamiento interno privado (`files/attachments`).
 * [localPath] es una ruta absoluta propia de la app, no un `content://` externo.
 *
 * Al marcar un Attachment como tombstone NO se borra el fichero físico:
 * así se puede revivir o incluir en un backup futuro sin romper sync.
 * El restore de backup copia a rutas nuevas y deja el ZIP original intacto.
 */
interface LocalFileStore {
    fun importStream(input: InputStream, displayName: String, syncId: String): String
    fun exists(localPath: String?): Boolean
    fun file(localPath: String): File
}

class DiskFileStore(private val root: File) : LocalFileStore {

    init {
        root.mkdirs()
    }

    override fun importStream(input: InputStream, displayName: String, syncId: String): String {
        root.mkdirs()
        val safe = sanitize(displayName)
        val dest = File(root, "${syncId}_$safe")
        input.use { src -> dest.outputStream().use { src.copyTo(it) } }
        return dest.absolutePath
    }

    override fun exists(localPath: String?): Boolean {
        if (localPath.isNullOrBlank()) return false
        val file = File(localPath)
        return file.isFile
    }

    override fun file(localPath: String): File = File(localPath)

    fun copyFile(sourcePath: String, displayName: String, syncId: String): String {
        return File(sourcePath).inputStream().use { importStream(it, displayName, syncId) }
    }

    private fun sanitize(name: String): String {
        val trimmed = name.substringAfterLast('/').substringAfterLast('\\').ifBlank { "file" }
        val safe = trimmed.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return safe.take(80).ifBlank { "file" }
    }
}

class AndroidAttachmentStore(
    private val context: Context,
    private val disk: LocalFileStore = DiskFileStore(File(context.filesDir, DIR))
) : LocalFileStore by disk {

    fun importUri(uri: Uri, fallbackName: String, syncId: String): String {
        val name = queryDisplayName(uri) ?: fallbackName
        val stream = context.contentResolver.openInputStream(uri)
            ?: error("No se pudo leer el fichero seleccionado")
        return disk.importStream(stream, name, syncId)
    }

    fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx < 0) null else cursor.getString(idx)
                }
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }

    fun queryMimeType(uri: Uri): String? = context.contentResolver.getType(uri)

    fun querySize(uri: Uri): Long? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idx < 0) null else cursor.getLong(idx).takeIf { it >= 0L }
                }
        } catch (_: Exception) {
            null
        }
    }

    fun importUriValidated(uri: Uri, fallbackName: String, syncId: String, mimeType: String): String {
        com.luis.alhendinfc.domain.model.AttachmentRules.requireAllowedMime(mimeType)
        querySize(uri)?.let { com.luis.alhendinfc.domain.model.AttachmentRules.requireSize(it) }
        val path = importUri(uri, fallbackName, syncId)
        val file = File(path)
        try {
            com.luis.alhendinfc.domain.model.AttachmentRules.requireSize(file.length())
        } catch (e: IllegalArgumentException) {
            file.delete()
            throw e
        }
        return path
    }

    companion object {
        const val DIR = "attachments"
    }
}
