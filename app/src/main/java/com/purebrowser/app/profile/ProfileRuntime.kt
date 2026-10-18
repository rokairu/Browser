package com.purebrowser.app.profile

/**
 * Identitas profil yang di-commit untuk proses ini.
 *
 * Gecko hanya mengizinkan SATU GeckoRuntime per proses, dan satu profil
 * Gecko tidak boleh dipakai dua proses bersamaan (file lock). Karena itu
 * proses ":profile" meng-commit tepat satu profil saat lahir, dan tidak
 * pernah berganti — ganti profil = lahirkan ulang prosesnya.
 */
object ProfileRuntime {
    @Volatile
    var committedProfileId: String? = null
        private set

    fun commit(profileId: String) {
        if (committedProfileId != null && committedProfileId != profileId) {
            error("Proses ini sudah terikat ke profil $committedProfileId")
        }
        committedProfileId = profileId
    }
}
