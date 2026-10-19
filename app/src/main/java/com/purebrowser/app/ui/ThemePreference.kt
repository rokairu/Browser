package com.purebrowser.app.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Mode tema: ikut sistem, paksa terang, atau paksa gelap. */
enum class ThemeMode(val label: String) {
    SYSTEM("Ikuti sistem"),
    LIGHT("Terang"),
    DARK("Gelap"),
}

/**
 * Preferensi tema yang disimpan. Default SYSTEM (ikuti pengaturan HP).
 * User memintanya karena tidak mau UI berubah-ubah sendiri.
 */
object ThemePreference {
    private const val PREFS = "purebrowser"
    private const val KEY = "theme_mode"

    var mode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    fun load(context: Context) {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, ThemeMode.SYSTEM.name)
        mode = try {
            ThemeMode.valueOf(name ?: ThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun set(context: Context, newMode: ThemeMode) {
        mode = newMode
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, newMode.name).apply()
    }

    /** true = gelap, berdasarkan mode + pengaturan sistem. */
    fun isDark(systemDark: Boolean): Boolean = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
}
