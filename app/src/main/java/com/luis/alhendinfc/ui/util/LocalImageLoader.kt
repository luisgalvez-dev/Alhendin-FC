package com.luis.alhendinfc.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decodifica y cachea bitmaps locales con muestreo para avatares/escudos.
 * Evita cargar fotos a resolución completa en listas y cabeceras.
 */
object LocalImageLoader {

    /** ~24 MB: tablets con varias fotos/escudos en pantalla. */
    private val cache = object : LruCache<String, ImageBitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            (value.width * value.height * 4) / 1024
    }

    suspend fun load(
        context: Context,
        uriString: String?,
        maxSidePx: Int = 256
    ): ImageBitmap? {
        if (uriString.isNullOrBlank()) return null
        val lower = uriString.lowercase()
        if (lower.startsWith("http://") || lower.startsWith("https://")) return null
        val isContent = lower.startsWith("content://")
        val isFile = lower.startsWith("file://") || uriString.startsWith("/")
        if (!isContent && !isFile) return null

        val cacheKey = "$uriString@$maxSidePx"
        cache.get(cacheKey)?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                fun open(): InputStream? = when {
                    isContent -> context.contentResolver.openInputStream(Uri.parse(uriString))
                    isFile && lower.startsWith("file://") -> {
                        val path = Uri.parse(uriString).path ?: return null
                        FileInputStream(File(path))
                    }
                    else -> FileInputStream(File(uriString))
                }

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

                val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxSidePx)
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val bmp = open()?.use { BitmapFactory.decodeStream(it, null, opts) }
                    ?: return@withContext null
                val image = bmp.asImageBitmap()
                cache.put(cacheKey, image)
                image
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxSide: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        var w = width
        var h = height
        while (w / 2 >= maxSide || h / 2 >= maxSide) {
            w /= 2
            h /= 2
            sample *= 2
        }
        return sample.coerceAtLeast(1)
    }
}
