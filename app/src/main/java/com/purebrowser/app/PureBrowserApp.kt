package com.purebrowser.app

import android.app.Application

class PureBrowserApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Sengaja ringan: berjalan di SEMUA proses (utama + :profile),
        // jadi tidak boleh ada inisialisasi berat di sini.
    }
}
