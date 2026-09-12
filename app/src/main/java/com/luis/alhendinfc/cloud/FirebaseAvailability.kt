package com.luis.alhendinfc.cloud

import android.content.Context

object FirebaseAvailability {
    fun isConfigured(context: Context): Boolean {
        val resId = context.resources.getIdentifier("google_app_id", "string", context.packageName)
        if (resId == 0) return false
        return try {
            context.getString(resId).isNotBlank()
        } catch (_: Exception) {
            false
        }
    }

    const val NOT_CONFIGURED = "Firebase no configurado. Añade google-services.json al módulo app."
}
