package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

sealed interface AppGlyph {
    data class Monochrome(val bitmap: ImageBitmap) : AppGlyph
    data class CategoryVector(@DrawableRes val drawableResId: Int) : AppGlyph
}

interface AppIconProvider {
    fun getCachedGlyph(packageName: String): AppGlyph?
    fun getCategoryFallback(packageName: String): AppGlyph.CategoryVector
    suspend fun resolveGlyph(packageName: String): AppGlyph
}

class PackageManagerAppIconProvider(context: Context) : AppIconProvider {

    private val appContext = context.applicationContext
    private val glyphCache = object : LruCache<String, AppGlyph>(MAX_CACHE_ENTRIES) {}
    private val lock = Any()

    override fun getCachedGlyph(packageName: String): AppGlyph? {
        if (packageName.isBlank()) return null
        return synchronized(lock) {
            glyphCache.get(packageName)
        }
    }

    override fun getCategoryFallback(packageName: String): AppGlyph.CategoryVector {
        return AppGlyph.CategoryVector(resolveCategoryDrawable(packageName))
    }

    override suspend fun resolveGlyph(packageName: String): AppGlyph {
        if (packageName.isBlank()) {
            return AppGlyph.CategoryVector(R.drawable.ic_glyph_bell)
        }

        synchronized(lock) {
            glyphCache.get(packageName)?.let { return it }
        }

        return withContext(Dispatchers.IO) {
            val resolved = loadMonochromeGlyphIfAvailable(packageName)
                ?: AppGlyph.CategoryVector(resolveCategoryDrawable(packageName))

            synchronized(lock) {
                glyphCache.put(packageName, resolved)
            }
            resolved
        }
    }

    private fun loadMonochromeGlyphIfAvailable(packageName: String): AppGlyph.Monochrome? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        return try {
            val drawable = appContext.packageManager.getApplicationIcon(packageName)
            if (drawable !is AdaptiveIconDrawable) return null
            val mono = drawable.monochrome?.mutate() ?: return null

            val bitmap = Bitmap.createBitmap(GLYPH_BITMAP_PX, GLYPH_BITMAP_PX, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            mono.colorFilter = PorterDuffColorFilter(android.graphics.Color.WHITE, PorterDuff.Mode.SRC_IN)
            // AdaptiveIconDrawable monochrome layers are authored for a ~66% safe zone inside 108dp.
            // Expanding bounds by 12px on each side maps the central 66.7% safe zone onto the 48x48 bitmap.
            mono.setBounds(
                -SAFE_ZONE_INSET_PX,
                -SAFE_ZONE_INSET_PX,
                GLYPH_BITMAP_PX + SAFE_ZONE_INSET_PX,
                GLYPH_BITMAP_PX + SAFE_ZONE_INSET_PX
            )
            mono.draw(canvas)

            AppGlyph.Monochrome(bitmap.asImageBitmap())
        } catch (_: Throwable) {
            null
        }
    }

    @DrawableRes
    private fun resolveCategoryDrawable(packageName: String): Int {
        val pkg = packageName.lowercase(Locale.ROOT)
        for ((keywords, drawableRes) in CATEGORY_RULES) {
            if (keywords.any { keyword -> pkg.contains(keyword) }) {
                return drawableRes
            }
        }
        return R.drawable.ic_glyph_bell
    }

    companion object {
        private const val MAX_CACHE_ENTRIES = 32
        private const val GLYPH_BITMAP_PX = 48
        private const val SAFE_ZONE_INSET_PX = 12

        private val CATEGORY_RULES: List<Pair<List<String>, Int>> = listOf(
            // Chat & messaging
            listOf(
                "whatsapp", "telegram", "signal", "messaging", "mms", "sms",
                "orca", "discord", "slack", "teams", "viber", "line.android", "chat"
            ) to R.drawable.ic_glyph_chat,

            // Mail
            listOf(
                "gmail", "android.gm", "outlook", "protonmail", "mail", "email",
                "thunderbird", "spark", "zoho"
            ) to R.drawable.ic_glyph_mail,

            // Phone & calls
            listOf(
                "dialer", "incallui", "telecom", "truecaller", "phone", "call"
            ) to R.drawable.ic_glyph_phone,

            // Payments, banking & wallets
            listOf(
                "paytm", "phonepe", "gpay", "nbu.paisa", "bank", "upi", "wallet",
                "paypal", "cred", "bharatpe", "razorpay", "stripe", "zerodha", "groww", "finance"
            ) to R.drawable.ic_glyph_payments,

            // Shopping & food delivery
            listOf(
                "amazon", "flipkart", "meesho", "swiggy", "zomato", "blinkit",
                "zepto", "bigbasket", "myntra", "ajio", "nykaa", "shopping", "ebay"
            ) to R.drawable.ic_glyph_shopping,

            // Social & people
            listOf(
                "instagram", "facebook", "twitter", "x.android", "linkedin", "snapchat",
                "reddit", "threads", "pinterest", "tumblr", "mastodon", "bluesky", "social"
            ) to R.drawable.ic_glyph_social,

            // Video & streaming
            listOf(
                "youtube", "netflix", "hotstar", "primevideo", "avod", "twitch",
                "vlc", "mxtech", "plex", "jiocinema", "sonyliv", "video"
            ) to R.drawable.ic_glyph_video,

            // Camera & photos
            listOf(
                "camera", "photos", "gallery", "lightroom", "snapseed", "vsco"
            ) to R.drawable.ic_glyph_camera,

            // Music & audio
            listOf(
                "spotify", "music", "saavn", "gaana", "wynk", "soundcloud",
                "audible", "podcast", "shazam"
            ) to R.drawable.ic_glyph_music,

            // Maps, navigation & transit
            listOf(
                "maps", "navigation", "uber", "ola", "rapido", "waze",
                "whereismytrain", "irctc", "makemytrip", "airbnb"
            ) to R.drawable.ic_glyph_map,

            // Calendar, reminders & clock
            listOf(
                "calendar", "deskclock", "clock", "alarm", "reminder", "tasks", "ticktick"
            ) to R.drawable.ic_glyph_calendar,

            // Downloads, app stores & files
            listOf(
                "vending", "packageinstaller", "downloads", "download", "fdroid",
                "aurora", "documentsui", "drive", "dropbox"
            ) to R.drawable.ic_glyph_download,

            // Security, VPN & authenticators
            listOf(
                "authenticator", "bitwarden", "1password", "keepass", "security",
                "antivirus", "vpn", "wireguard", "tailscale", "permissioncontroller"
            ) to R.drawable.ic_glyph_security,

            // System & settings (checked last so com.google.android.* apps match their specific categories first)
            listOf(
                "systemui", "settings", "google.android.gms", "bluetooth", "nfc", "wifi", "android"
            ) to R.drawable.ic_glyph_system
        )
    }
}
