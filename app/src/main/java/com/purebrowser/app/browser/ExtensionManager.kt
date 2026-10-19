package com.purebrowser.app.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.os.Handler
import android.os.Looper
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController
import java.io.File

/** Jalankan di main thread — callback GeckoResult tidak dijamin di main thread. */
private val mainHandler = Handler(Looper.getMainLooper())
private fun onMain(block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) block()
    else mainHandler.post { block() }
}

/**
 * Pengelola ekstensi WebExtension (mesin yang sama dengan Firefox Android).
 *
 * Batasan jujur v1:
 * - Hanya ekstensi yang DITANDATANGANI Mozilla yang bisa di-install
 *   (praktisnya: yang diunduh dari addons.mozilla.org). File .xpi rakitan
 *   sendiri akan ditolak dengan ERROR_SIGNEDSTATE_REQUIRED.
 * - Tidak semua ekstensi desktop berfungsi di Android (beberapa API
 *   WebExtension tidak tersedia di mobile).
 * - Data ekstensi tersimpan per-profil secara otomatis (mengikuti
 *   direktori profil Gecko yang sudah diisolasi per profil).
 */
object ExtensionManager {

    private var controller: WebExtensionController? = null

    val extensions = mutableStateListOf<WebExtension>()
    var isBusy by mutableStateOf(false)
        private set
    var lastError by mutableStateOf<String?>(null)
        private set

    fun init(runtime: GeckoRuntime) {
        if (controller != null) return
        val c = runtime.webExtensionController
        controller = c

        // WAJIB: delegate prompt instalasi. v1: auto-grant (beri semua izin
        // yang diminta ekstensi). Nanti bisa diganti UI persetujuan sendiri.
        c.setPromptDelegate(object : WebExtensionController.PromptDelegate {
            override fun onInstallPromptRequest(
                extension: WebExtension,
                permissions: Array<String>,
                origins: Array<String>,
                dataCollectionPermissions: Array<String>,
            ): GeckoResult<WebExtension.PermissionPromptResponse> {
                return GeckoResult.fromValue(
                    WebExtension.PermissionPromptResponse(true, true, true)
                )
            }
        })

        c.setAddonManagerDelegate(object : WebExtensionController.AddonManagerDelegate {
            override fun onInstalled(extension: WebExtension) { refresh() }
            override fun onUninstalled(extension: WebExtension) { refresh() }
            override fun onEnabled(extension: WebExtension) { refresh() }
            override fun onDisabled(extension: WebExtension) { refresh() }
            override fun onInstallationFailed(
                extension: WebExtension?,
                exception: WebExtension.InstallException,
            ) {
                isBusy = false
                lastError = describeInstallError(exception)
            }
        })

        c.setExtensionProcessDelegate(object : WebExtensionController.ExtensionProcessDelegate {
            override fun onDisabledProcessSpawning() {
                // Proses ekstensi crash berulang — coba nyalakan lagi.
                try { c.enableExtensionProcessSpawning() } catch (_: Exception) { }
            }
        })

        refresh()
    }

    fun refresh() {
        val c = controller ?: return
        c.list().accept(
            { list -> onMain {
                extensions.clear()
                extensions.addAll((list ?: emptyList()).sortedBy { it.metaData?.name ?: it.id })
            } },
            { err -> onMain {
                lastError = err?.message ?: "Gagal memuat daftar ekstensi"
            } },
        )
    }

    /** Install dari file .xpi lokal. File dihapus otomatis setelah selesai (sukses/gagal). */
    fun installFromFile(xpiFile: File) {
        val c = controller ?: run {
            lastError = "Pengelola ekstensi belum siap"
            try { xpiFile.delete() } catch (_: Exception) { }
            return
        }
        isBusy = true
        lastError = null
        // Encode path untuk URI file:// yang valid.
        val uri = "file://" + java.net.URLEncoder.encode(
            xpiFile.absolutePath, "UTF-8"
        ).replace("%2F", "/")
        c.install(uri, WebExtensionController.INSTALLATION_METHOD_FROM_FILE).accept(
            { onMain {
                isBusy = false
                try { xpiFile.delete() } catch (_: Exception) { }
                refresh()
            } },
            { err -> onMain {
                isBusy = false
                try { xpiFile.delete() } catch (_: Exception) { }
                lastError = describeInstallError(err)
            } },
        )
    }

    /** Install langsung dari URL https (mis. link unduhan AMO). */
    fun installFromUrl(url: String) {
        val c = controller ?: run {
            lastError = "Pengelola ekstensi belum siap"
            return
        }
        isBusy = true
        lastError = null
        c.install(url, WebExtensionController.INSTALLATION_METHOD_FROM_FILE).accept(
            { onMain { isBusy = false; refresh() } },
            { err -> onMain {
                isBusy = false
                lastError = describeInstallError(err)
            } },
        )
    }

    fun setEnabled(extension: WebExtension, enable: Boolean) {
        val c = controller ?: return
        lastError = null
        val result = if (enable) {
            c.enable(extension, WebExtensionController.EnableSource.USER)
        } else {
            c.disable(extension, WebExtensionController.EnableSource.USER)
        }
        // WebExtension immutable: enable/disable mengembalikan instance baru.
        result.accept(
            { updated -> onMain {
                if (updated != null) {
                    val i = extensions.indexOfFirst { it.id == updated.id }
                    if (i >= 0) extensions[i] = updated else refresh()
                } else refresh()
            } },
            { err -> onMain {
                lastError = err?.message ?: "Gagal mengubah status ekstensi"
            } },
        )
    }

    fun uninstall(extension: WebExtension) {
        val c = controller ?: return
        lastError = null
        c.uninstall(extension).accept(
            { onMain { refresh() } },
            { err -> onMain {
                lastError = err?.message ?: "Gagal menghapus ekstensi"
            } },
        )
    }

    /** Beri tahu ekstensi tentang tab aktif (agar API tabs.* berfungsi). */
    fun setTabActive(session: GeckoSession) {
        try { controller?.setTabActive(session, true) } catch (_: Exception) { }
    }

    fun clearError() { lastError = null }

    private fun describeInstallError(err: Throwable?): String {
        if (err == null) return "Gagal memasang ekstensi (kesalahan tidak diketahui)"
        val ie = err as? WebExtension.InstallException
        return when (ie?.code) {
            WebExtension.InstallException.ErrorCodes.ERROR_SIGNEDSTATE_REQUIRED ->
                "Ekstensi harus ditandatangani Mozilla. Unduh dari addons.mozilla.org — file .xpi rakitan sendiri tidak bisa dipasang."
            WebExtension.InstallException.ErrorCodes.ERROR_NETWORK_FAILURE ->
                "Gagal mengunduh ekstensi. Periksa koneksi internet."
            WebExtension.InstallException.ErrorCodes.ERROR_CORRUPT_FILE,
            WebExtension.InstallException.ErrorCodes.ERROR_INCORRECT_HASH ->
                "File .xpi rusak atau tidak valid."
            WebExtension.InstallException.ErrorCodes.ERROR_BLOCKLISTED,
            WebExtension.InstallException.ErrorCodes.ERROR_SOFT_BLOCKED ->
                "Ekstensi diblokir oleh Mozilla (blocklist)."
            WebExtension.InstallException.ErrorCodes.ERROR_INCOMPATIBLE ->
                "Ekstensi tidak kompatibel dengan versi mesin ini."
            WebExtension.InstallException.ErrorCodes.ERROR_FILE_ACCESS ->
                "Tidak bisa membaca file .xpi."
            else -> "Gagal memasang ekstensi: ${err.message ?: err.javaClass.simpleName}"
        }
    }
}
