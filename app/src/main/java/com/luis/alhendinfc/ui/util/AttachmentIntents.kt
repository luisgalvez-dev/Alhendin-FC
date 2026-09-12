package com.luis.alhendinfc.ui.util

import android.content.Context
import androidx.core.content.FileProvider
import android.content.Intent
import com.luis.alhendinfc.data.sync.TransferHooks
import com.luis.alhendinfc.domain.model.Attachment
import java.io.File

fun openAttachment(context: Context, attachment: Attachment) {
    val path = attachment.localPath ?: return
    val file = File(path)
    if (!file.isFile) {
        TransferHooks.enqueueDownload(attachment.syncId)
        return
    }
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

fun openOrDownloadAttachment(
    context: Context,
    attachment: Attachment,
    onViewImage: (String) -> Unit
) {
    val path = attachment.localPath
    if (!path.isNullOrBlank() && File(path).isFile) {
        if (attachment.isImage) onViewImage(path) else openAttachment(context, attachment)
    } else {
        TransferHooks.enqueueDownload(attachment.syncId)
    }
}
