package com.purebrowser.app.profile

import org.json.JSONObject
import java.util.UUID

/**
 * Satu profil browser ala Chrome desktop: identitas + warna + ikon,
 * dengan seluruh data (cookie, history, login, bookmark) terisolasi
 * di direktori profilnya sendiri.
 */
data class Profile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorArgb: Int,
    val icon: String,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("colorArgb", colorArgb)
        .put("icon", icon)
        .put("createdAt", createdAt)

    companion object {
        fun fromJson(o: JSONObject) = Profile(
            id = o.getString("id"),
            name = o.getString("name"),
            colorArgb = o.getInt("colorArgb"),
            // Normalisasi: emoji lama otomatis dipetakan ke key ikon baru.
            icon = ProfileIcons.normalize(o.optString("icon", ProfileIcons.DEFAULT_KEY)),
            createdAt = o.optLong("createdAt", 0L),
        )

        /** Warna khas ala pemilih profil Chrome desktop. */
        val PALETTE = listOf(
            0xFF8AB4F8.toInt(), // biru
            0xFF81C995.toInt(), // hijau
            0xFFF6AEA9.toInt(), // merah muda
            0xFFFDD663.toInt(), // kuning
            0xFFC58AF9.toInt(), // ungu
            0xFF78D9EC.toInt(), // cyan
            0xFFFFB287.toInt(), // oranye
            0xFF9AA0A6.toInt(), // abu-abu
        )
        /** Key ikon (lihat ProfileIcons.OPTIONS). Disimpan sebagai string di JSON. */
        val ICONS = ProfileIcons.OPTIONS.map { it.first }
    }
}
