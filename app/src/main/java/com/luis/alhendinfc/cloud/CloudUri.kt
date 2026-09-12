package com.luis.alhendinfc.cloud

object CloudUri {
    fun isLocal(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        val v = value.trim()
        return v.contains("content://") ||
            v.contains("file://") ||
            v.contains("/data/") ||
            v.startsWith("/") ||
            v.startsWith("file:")
    }

    fun portableOrNull(value: String?): String? {
        if (value.isNullOrBlank()) return null
        if (isLocal(value)) return null
        return value
    }
}
