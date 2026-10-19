package com.purebrowser.app.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Ikon profil berbasis Material ImageVector (pengganti emoji).
 * Disimpan sebagai key string agar tetap kompatibel dengan JSON profil.
 */
object ProfileIcons {

    val OPTIONS: List<Pair<String, ImageVector>> = listOf(
        "globe" to Icons.Filled.Language,
        "work" to Icons.Filled.Work,
        "game" to Icons.Filled.SportsEsports,
        "book" to Icons.Filled.Book,
        "shop" to Icons.Filled.ShoppingCart,
        "movie" to Icons.Filled.Movie,
        "chat" to Icons.Filled.Chat,
        "lock" to Icons.Filled.Lock,
        "rocket" to Icons.Filled.RocketLaunch,
        "moon" to Icons.Filled.DarkMode,
        "coffee" to Icons.Filled.Coffee,
        "music" to Icons.Filled.Headphones,
        "code" to Icons.Filled.Code,
        "art" to Icons.Filled.Brush,
    )

    val DEFAULT_KEY = "globe"

    /** Peta kompatibilitas mundur: emoji lama -> key baru. */
    private val EMOJI_MAP = mapOf(
        "🌐" to "globe",
        "💼" to "work",
        "🎮" to "game",
        "📚" to "book",
        "🛒" to "shop",
        "🎬" to "movie",
        "💬" to "chat",
        "🔒" to "lock",
        "🚀" to "rocket",
        "🌙" to "moon",
        "☕" to "coffee",
        "🎧" to "music",
    )

    private val byKey = OPTIONS.toMap()

    /** Resolve string tersimpan (key baru atau emoji lama) menjadi ImageVector. */
    fun resolve(stored: String): ImageVector {
        val key = EMOJI_MAP[stored] ?: stored
        return byKey[key] ?: byKey.getValue(DEFAULT_KEY)
    }

    /** Normalisasi string tersimpan menjadi key baru (untuk migrasi saat save). */
    fun normalize(stored: String): String = EMOJI_MAP[stored] ?: stored
}
