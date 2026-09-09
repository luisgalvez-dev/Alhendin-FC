package com.luis.alhendinfc.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Abre una URL http(s) en el navegador. Devuelve false si no hay app capaz. */
fun openExternalUrl(context: Context, url: String): Boolean {
    return try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url.trim())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: Exception) {
        false
    }
}
