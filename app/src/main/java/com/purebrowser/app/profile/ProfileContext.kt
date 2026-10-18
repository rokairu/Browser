package com.purebrowser.app.profile

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.os.Build
import java.io.File

/**
 * ContextWrapper yang mengalihkan direktori data aplikasi ke folder
 * khusus satu profil: <filesDir>/profiles/<profileId>/.
 *
 * GeckoView menentukan lokasi profilnya dari direktori data milik
 * Context saat GeckoRuntime dibuat — jadi dengan membungkus Context
 * seperti ini, tiap profil mendapat profil Gecko yang terisolasi penuh
 * (cookie, cache, login, site data) tanpa berbagi apa pun.
 *
 * Implementasi orisinal; nativeLibraryDir & sourceDir tetap milik APK asli.
 */
class ProfileContext(
    private val baseCtx: Context,
    private val profileId: String,
) : ContextWrapper(baseCtx) {

    private val root: File = File(baseCtx.filesDir, ProfileManager.profileDirName(profileId))
    private val files: File = File(root, "files")
    private val cache: File = File(root, "cache")
    private val noBackup: File = File(root, "no_backup")
    private val codeCache: File = File(root, "code_cache")

    private val patchedAppInfo: ApplicationInfo = ApplicationInfo(baseCtx.applicationInfo).apply {
        dataDir = root.absolutePath
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            deviceProtectedDataDir = root.absolutePath
        }
        // nativeLibraryDir, sourceDir, publicSourceDir: TIDAK diubah
    }

    init {
        root.mkdirs()
        files.mkdirs()
        cache.mkdirs()
        noBackup.mkdirs()
        codeCache.mkdirs()
    }

    override fun getApplicationContext(): Context = this

    override fun getApplicationInfo(): ApplicationInfo = patchedAppInfo

    override fun getDataDir(): File = root

    override fun getFilesDir(): File = files

    override fun getCacheDir(): File = cache

    override fun getNoBackupFilesDir(): File = noBackup

    override fun getCodeCacheDir(): File = codeCache

    override fun getFileStreamPath(name: String): File = File(files, name)

    override fun getDir(name: String, mode: Int): File =
        File(files, "app_$name").apply { mkdirs() }

    override fun getExternalFilesDir(type: String?): File? {
        val ext = baseCtx.getExternalFilesDir(type) ?: return null
        return File(File(ext.parentFile, "profiles/$profileId"), "files").apply { mkdirs() }
    }

    override fun getExternalCacheDir(): File? {
        val ext = baseCtx.externalCacheDir ?: return null
        return File(File(ext.parentFile, "profiles/$profileId"), "cache").apply { mkdirs() }
    }
}
