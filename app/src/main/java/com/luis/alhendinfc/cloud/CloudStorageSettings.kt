package com.luis.alhendinfc.cloud

import com.luis.alhendinfc.BuildConfig
import com.luis.alhendinfc.cloud.storage.BinaryStorageConfig

object CloudStorageSettings {
    val url: String get() = BuildConfig.SUPABASE_URL.trim()
    val publishableKey: String get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()
    val bucket: String
        get() = BuildConfig.SUPABASE_BUCKET.trim().ifBlank { BinaryStorageConfig.DEFAULT_BUCKET }
    val isConfigured: Boolean
        get() = BinaryStorageConfig.isConfigured(url, publishableKey)
}
