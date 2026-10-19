package com.purebrowser.app.browser

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.purebrowser.app.profile.ProfileRuntime
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.WebResponse

/**
 * Mesin browser per-proses. Hidup di proses ":profile" dan terikat ke
 * SATU profil (lihat ProfileRuntime). Satu GeckoRuntime, N tab (GeckoSession).
 */
object GeckoEngine {

    private var runtime: GeckoRuntime? = null
    private var profileContext: Context? = null

    val tabs = mutableStateListOf<Tab>()
    var activeTabId by mutableStateOf<String?>(null)

    val activeTab: Tab? get() = tabs.find { it.id == activeTabId }

    lateinit var history: HistoryStore
        private set
    lateinit var bookmarks: BookmarksStore
        private set

    fun init(profileCtx: Context) {
        if (runtime != null) return
        profileContext = profileCtx
        history = HistoryStore(profileCtx)
        bookmarks = BookmarksStore(profileCtx)

        val cb = ContentBlocking.Settings.Builder()
            .antiTracking(ContentBlocking.AntiTracking.STRICT)
            .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
            .strictSocialTrackingProtection(true)
            .build()
        val settings = GeckoRuntimeSettings.Builder()
            .contentBlocking(cb)
            .javaScriptEnabled(true)
            .build()
        runtime = GeckoRuntime.create(profileCtx, settings)
        ExtensionManager.init(requireNotNull(runtime))
    }

    fun newTab(url: String? = null, isPrivate: Boolean = false, select: Boolean = true): Tab {
        val rt = requireNotNull(runtime) { "GeckoEngine belum di-init" }
        val sessionSettings = GeckoSessionSettings.Builder()
            .usePrivateMode(isPrivate)
            .build()
        val session = GeckoSession(sessionSettings)
        val tab = Tab(session = session, isPrivate = isPrivate, initialUrl = url)
        wireDelegates(tab)
        session.open(rt)
        tabs.add(tab)
        if (select) selectTab(tab.id)
        // JANGAN panggil loadUri() di sini — session belum ditempel ke GeckoView.
        // pendingUrl akan dimuat oleh BrowserScreen setelah view attached.
        return tab
    }

    fun selectTab(id: String) {
        tabs.forEach { it.session.setActive(it.id == id) }
        activeTabId = id
        // Beri tahu ekstensi tentang tab aktif (API tabs.*).
        tabs.find { it.id == id }?.let { ExtensionManager.setTabActive(it.session) }
    }

    /**
     * Muat URL di tab dengan benar:
     * - Jika tab masih halaman tab baru: set pendingUrl, biarkan BrowserScreen
     *   memuat setelah session ditempel ke view (hindari bug blank).
     * - Jika tab sudah web biasa: loadUri langsung (session sudah attached).
     */
    fun loadUrl(tab: Tab, url: String) {
        if (tab.isNewTabPage) {
            tab.pendingUrl = url
            tab.isNewTabPage = false
        } else {
            // Session sudah attached ke view — aman load langsung.
            // Tapi jika pendingUrl masih ada (belum dimuat), gabungkan.
            if (tab.pendingUrl != null) {
                tab.pendingUrl = url
            } else {
                tab.session.loadUri(url)
            }
        }
    }

    fun closeTab(id: String) {
        val tab = tabs.find { it.id == id } ?: return
        val wasActive = id == activeTabId
        tabs.remove(tab)
        try { tab.session.close() } catch (_: Exception) { }
        if (wasActive) {
            val next = tabs.lastOrNull()
            if (next != null) selectTab(next.id) else activeTabId = null
        }
    }

    fun closeAllTabs() {
        tabs.toList().forEach { closeTab(it.id) }
    }

    /** Simpan URL tab yang terbuka agar bisa dipulihkan setelah restart proses. */
    fun persistTabs(context: Context) {
        try {
            val urls = tabs.filter { !it.isPrivate }.mapNotNull { it.url.ifBlank { null } }
            context.getSharedPreferences("purebrowser", Context.MODE_PRIVATE)
                .edit().putStringSet("open_tabs", urls.toSet()).apply()
        } catch (_: Exception) { }
    }

    fun restoreTabs(context: Context): Boolean {
        val urls = context.getSharedPreferences("purebrowser", Context.MODE_PRIVATE)
            .getStringSet("open_tabs", null)?.toList() ?: return false
        if (urls.isEmpty()) return false
        urls.forEachIndexed { i, u -> newTab(u, select = i == 0) }
        return true
    }

    private fun wireDelegates(tab: Tab) {
        val session = tab.session
        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(
                session: GeckoSession, url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean,
            ) {
                // Abaikan about:blank (status awal session) agar tidak menimpa URL/tab baru.
                if (url != null && url != "about:blank") {
                    tab.url = url
                    tab.isNewTabPage = false
                    if (!tab.isPrivate) {
                        try { history.record(url, tab.title) } catch (_: Exception) { }
                    }
                }
            }
            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                tab.canGoBack = canGoBack
            }
            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                tab.canGoForward = canGoForward
            }
        }
        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                tab.isLoading = true
            }
            override fun onPageStop(session: GeckoSession, success: Boolean) {
                tab.isLoading = false
                tab.progress = 100
            }
            override fun onProgressChange(session: GeckoSession, progress: Int) {
                tab.progress = progress
            }
        }
        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                if (title != null) {
                    tab.title = title
                    if (!tab.isPrivate && tab.url.isNotBlank()) {
                        try { history.record(tab.url, title) } catch (_: Exception) { }
                    }
                }
            }
            override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
                downloadExternally(response.uri)
            }
        }
    }

    private fun downloadExternally(uri: String) {
        val ctx = profileContext ?: return
        try {
            val req = DownloadManager.Request(Uri.parse(uri))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    Uri.parse(uri).lastPathSegment ?: "unduhan",
                )
            val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(req)
        } catch (_: Exception) { }
    }

    fun shutdown() {
        persistTabs(requireNotNull(profileContext))
        closeAllTabs()
        // Runtime sengaja tidak di-shutdown: proses akan dimatikan.
    }
}
