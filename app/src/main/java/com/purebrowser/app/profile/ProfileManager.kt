package com.purebrowser.app.profile

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import java.io.File

/**
 * Mengelola daftar profil. Berkas disimpan di direktori aplikasi utama
 * (bukan di dalam direktori per-profil) agar bisa dibaca semua proses.
 *
 * Profil "Default" tidak bisa dihapus — selalu ada minimal satu profil.
 */
class ProfileManager(private val appContext: Context) {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("purebrowser", Context.MODE_PRIVATE)
    private val storeFile = File(appContext.filesDir, "profiles.json")

    fun list(): List<Profile> {
        if (!storeFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(storeFile.readText())
            List(arr.length()) { i -> Profile.fromJson(arr.getJSONObject(i)) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Pastikan minimal ada satu profil; kembalikan daftar final. */
    fun ensureDefault(): List<Profile> {
        val existing = list()
        if (existing.isNotEmpty()) return existing
        val def = Profile(name = "Pribadi", colorArgb = Profile.PALETTE[0], icon = ProfileIcons.DEFAULT_KEY)
        save(listOf(def))
        setActiveProfileId(def.id)
        return listOf(def)
    }

    fun get(id: String): Profile? = list().find { it.id == id }

    fun add(name: String, colorArgb: Int, icon: String): Profile {
        val p = Profile(name = name.trim().ifEmpty { "Profil" }, colorArgb = colorArgb, icon = icon)
        save(list() + p)
        return p
    }

    fun rename(id: String, name: String) {
        save(list().map { if (it.id == id) it.copy(name = name.trim().ifEmpty { it.name }) else it })
    }

    fun recolor(id: String, colorArgb: Int, icon: String) {
        save(list().map { if (it.id == id) it.copy(colorArgb = colorArgb, icon = icon) else it })
    }

    /**
     * Hapus profil + seluruh datanya. Profil terakhir tidak bisa dihapus.
     * @return false jika ditolak (profil terakhir / tidak ketemu)
     */
    fun delete(id: String): Boolean {
        val all = list()
        if (all.size <= 1 || all.none { it.id == id }) return false
        save(all.filter { it.id != id })
        // Hapus direktori data profil
        File(appContext.filesDir, "profiles/$id").deleteRecursively()
        if (activeProfileId() == id) {
            setActiveProfileId(list().firstOrNull()?.id)
        }
        return true
    }

    fun activeProfileId(): String? = prefs.getString("active_profile", null)

    fun setActiveProfileId(id: String?) {
        prefs.edit().putString("active_profile", id).apply()
    }

    private fun save(profiles: List<Profile>) {
        val arr = JSONArray()
        profiles.forEach { arr.put(it.toJson()) }
        storeFile.parentFile?.mkdirs()
        storeFile.writeText(arr.toString())
    }

    companion object {
        /** Direktori data untuk satu profil (relatif ke filesDir aplikasi). */
        fun profileDirName(id: String) = "profiles/$id"
    }
}
