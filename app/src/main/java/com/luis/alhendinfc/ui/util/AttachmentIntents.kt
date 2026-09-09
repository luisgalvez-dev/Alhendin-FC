package com.luis.alhendinfc.ui.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.luis.alhendinfc.domain.model.Attachment
import java.io.File

fun openAttachment(context: Context, attachment: Attachment) {
    val file = File(attachment.localPath)
    if (!file.isFile) return
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val mime = attachment.mimeType.ifBlank { "*/*" }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, attachment.name.ifBlank { "Abrir archivo" }))
}
