package com.luis.alhendinfc.ui.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.luis.alhendinfc.domain.model.RivalLinkRules

/**
 * Abre una URL http(s). Prefiere Custom Tabs para reutilizar la sesión del navegador.
 * Fallback [Intent.ACTION_VIEW]. No gestiona credenciales ni cookies.
 */
fun openWebUrl(context: Context, url: String): Boolean {
    if (!RivalLinkRules.isOpenableUrl(url)) return false
    val uri = Uri.parse(RivalLinkRules.normalizeUrl(url))
    return try {
        val tabs = CustomTabsIntent.Builder().setShowTitle(true).build()
        if (context !is Activity) {
            tabs.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        tabs.launchUrl(context, uri)
        true
    } catch (_: ActivityNotFoundException) {
        openExternalUrl(context, uri)
    } catch (_: Exception) {
        openExternalUrl(context, uri)
    }
}

/** Abre una URL http(s) en el navegador. Devuelve false si no hay app capaz. */
fun openExternalUrl(context: Context, url: String): Boolean {
    if (!RivalLinkRules.isOpenableUrl(url)) return false
    return openExternalUrl(context, Uri.parse(RivalLinkRules.normalizeUrl(url)))
}

private fun openExternalUrl(context: Context, uri: Uri): Boolean {
    return try {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: Exception) {
        false
    }
}
