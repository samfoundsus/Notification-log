package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class MonogramTone {
    PRIMARY,
    SECONDARY,
    TERTIARY
}

data class AppMonogramSpec(
    val letter: Char,
    val tone: MonogramTone
)

/**
 * Provides app icons with a small memory footprint and a deterministic monogram fallback.
 */
interface AppIconProvider {
    fun getMonogram(packageName: String, appName: String): AppMonogramSpec
    fun getCachedIcon(packageName: String): ImageBitmap?
    suspend fun loadIcon(packageName: String): ImageBitmap?
}

object MonogramHelper {
    private val tones = MonogramTone.entries

    fun computeMonogram(packageName: String, appName: String): AppMonogramSpec {
        val letter = appName.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar() ?: '?'
        var hash = 0
        for (ch in packageName) {
            hash = (hash * 31 + ch.code) and Int.MAX_VALUE
        }
        val tone = tones[hash % tones.size]
        return AppMonogramSpec(letter = letter, tone = tone)
    }
}

class PackageManagerAppIconProvider(context: Context) : AppIconProvider {

    private val appContext = context.applicationContext
    private val iconCache = object : LruCache<String, ImageBitmap>(MAX_CACHE_ENTRIES) {}
    private val missingPackages = ArrayDeque<String>(MAX_MISSING_ENTRIES)
    private val lock = Any()

    override fun getMonogram(packageName: String, appName: String): AppMonogramSpec {
        return MonogramHelper.computeMonogram(packageName, appName)
    }

    override fun getCachedIcon(packageName: String): ImageBitmap? {
        if (packageName.isBlank()) return null
        return synchronized(lock) {
            iconCache.get(packageName)
        }
    }

    override suspend fun loadIcon(packageName: String): ImageBitmap? {
        if (packageName.isBlank()) return null

        synchronized(lock) {
            iconCache.get(packageName)?.let { return it }
            if (packageName in missingPackages) return null
        }

        return withContext(Dispatchers.IO) {
            try {
                val drawable = appContext.packageManager.getApplicationIcon(packageName)
                val bitmap = drawableToBitmap(drawable, TARGET_ICON_SIZE_PX)
                val imageBitmap = bitmap.asImageBitmap()
                synchronized(lock) {
                    iconCache.put(packageName, imageBitmap)
                }
                imageBitmap
            } catch (_: Throwable) {
                synchronized(lock) {
                    if (packageName !in missingPackages) {
                        if (missingPackages.size >= MAX_MISSING_ENTRIES) {
                            missingPackages.removeFirst()
                        }
                        missingPackages.addLast(packageName)
                    }
                }
                null
            }
        }
    }

    private fun drawableToBitmap(drawable: Drawable, targetSizePx: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val src = drawable.bitmap
            if (src.width <= targetSizePx && src.height <= targetSizePx) {
                return src
            }
            return Bitmap.createScaledBitmap(src, targetSizePx, targetSizePx, true)
        }
        val bitmap = Bitmap.createBitmap(targetSizePx, targetSizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    companion object {
        private const val MAX_CACHE_ENTRIES = 32
        private const val MAX_MISSING_ENTRIES = 64
        private const val TARGET_ICON_SIZE_PX = 120
    }
}
