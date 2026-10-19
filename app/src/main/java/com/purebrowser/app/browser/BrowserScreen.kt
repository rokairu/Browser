package com.purebrowser.app.browser

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.purebrowser.app.MainActivity
import com.purebrowser.app.profile.ProfileManager
import com.purebrowser.app.profile.ProfileRuntime
import org.mozilla.geckoview.GeckoView
import java.net.URLEncoder

@Composable
fun BrowserScreen(
    geckoView: GeckoView,
    onSwitchProfile: (String) -> Unit,
    onOpenProfileManager: () -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val pm = remember { ProfileManager(context.applicationContext) }
    val profile = remember { pm.get(ProfileRuntime.committedProfileId ?: "") ?: pm.ensureDefault().first() }

    val tabs = GeckoEngine.tabs
    val tab = GeckoEngine.activeTab

    var menuOpen by remember { mutableStateOf(false) }
    var showTabs by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showProfiles by remember { mutableStateOf(false) }
    var showExtensions by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // ---- Bilah atas ----
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Chip profil ala Chrome desktop
            Row(
                modifier = Modifier.clip(CircleShape)
                    .clickable { showProfiles = true }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape)
                        .background(Color(profile.colorArgb)),
                    contentAlignment = Alignment.Center,
                ) { Text(text = profile.icon, fontSize = 18.sp) }
            }
            IconButton(
                onClick = { tab?.session?.goBack() },
                enabled = tab?.canGoBack == true,
            ) { Icon(Icons.Default.ArrowBack, contentDescription = "Kembali") }
            IconButton(
                onClick = { tab?.session?.goForward() },
                enabled = tab?.canGoForward == true,
            ) { Icon(Icons.Default.ArrowForward, contentDescription = "Maju") }

            AddressBar(
                tab = tab,
                modifier = Modifier.weight(1f),
            )

            val bookmarked = tab?.let {
                it.url.isNotBlank() && GeckoEngine.bookmarks.isBookmarked(it.url)
            } == true
            IconButton(onClick = {
                tab?.let {
                    if (it.url.isNotBlank()) GeckoEngine.bookmarks.toggle(it.url, it.title)
                }
            }) {
                Icon(
                    if (bookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Bookmark",
                    tint = if (bookmarked) Color(0xFFFDD663) else MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = { tab?.session?.reload() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Muat ulang")
            }
            Box {
                IconButton(onClick = { showTabs = true }) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${tabs.size}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.background(
                                MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)
                            ).padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(text = "Tab baru") },
                        onClick = { menuOpen = false; GeckoEngine.newTab() },
                        leadingIcon = { Icon(Icons.Default.Add, null) },
                    )
                    DropdownMenuItem(
                        text = { Text(text = "Tab privat baru") },
                        onClick = { menuOpen = false; GeckoEngine.newTab(isPrivate = true) },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                    )
                    DropdownMenuItem(
                        text = { Text(text = "Bookmark") },
                        onClick = { menuOpen = false; showBookmarks = true },
                        leadingIcon = { Icon(Icons.Default.Bookmark, null) },
                    )
                    DropdownMenuItem(
                        text = { Text(text = "Riwayat") },
                        onClick = { menuOpen = false; showHistory = true },
                        leadingIcon = { Icon(Icons.Default.History, null) },
                    )
                    DropdownMenuItem(
                        text = { Text(text = "Unduhan") },
                        onClick = {
                            menuOpen = false
                            context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                        },
                        leadingIcon = { Icon(Icons.Default.Download, null) },
                    )
                    DropdownMenuItem(
                        text = { Text(text = "VPN") },
                        onClick = {
                            menuOpen = false
                            // VPN dikelola di proses utama
                            context.startActivity(
                                Intent(context, MainActivity::class.java).apply {
                                    putExtra(MainActivity.EXTRA_AUTO_VPN, true)
                                }
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.VpnKey, null) },
                    )
                    DropdownMenuItem(
                        text = { Text(text = "Ekstensi") },
                        onClick = { menuOpen = false; showExtensions = true },
                        leadingIcon = { Icon(Icons.Default.Extension, null) },
                    )
                    DropdownMenuItem(
                        text = { Text(text = "Ganti profil") },
                        onClick = { menuOpen = false; showProfiles = true },
                        leadingIcon = { Icon(Icons.Default.People, null) },
                    )
                    DropdownMenuItem(
                        text = { Text(text = "Keluar") },
                        onClick = { menuOpen = false; onExit() },
                        leadingIcon = { Icon(Icons.Default.ExitToApp, null) },
                    )
                }
            }
        }

        // ---- Progress ----
        if (tab?.isLoading == true) {
            LinearProgressIndicator(
                progress = { (tab.progress.coerceIn(0, 100)) / 100f },
                modifier = Modifier.fillMaxWidth().height(3.dp),
            )
        }

        // ---- Area web / halaman tab baru ----
        if (tab != null) {
            if (tab.isNewTabPage) {
                NewTabScreen(tab = tab)
            } else {
                AndroidView(
                    factory = { geckoView },
                    update = { view -> view.setSession(tab.session) },
                    modifier = Modifier.fillMaxSize().weight(1f),
                )
            }
        } else {
            Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text(text = "Tidak ada tab — buka tab baru dari menu", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }

    // ---- Dialog-dialog ----
    if (showTabs) {
        TabsDialog(
            onClose = { showTabs = false },
            onNewTab = { GeckoEngine.newTab(); showTabs = false },
        )
    }
    if (showBookmarks) {
        BookmarksDialog(onClose = { showBookmarks = false })
    }
    if (showHistory) {
        HistoryDialog(onClose = { showHistory = false })
    }
    if (showExtensions) {
        ExtensionsDialog(onClose = { showExtensions = false })
    }
    if (showProfiles) {
        ProfileSwitchDialog(
            pm = pm,
            currentId = profile.id,
            onPick = { showProfiles = false; onSwitchProfile(it) },
            onManage = { showProfiles = false; onOpenProfileManager() },
            onClose = { showProfiles = false },
        )
    }
}

@Composable
private fun AddressBar(tab: Tab?, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    var input by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val url = tab?.url ?: ""

    LaunchedEffect(tab?.id, url) {
        if (!focused) input = url
    }

    TextField(
        value = if (focused) input else url,
        onValueChange = { input = it },
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) input = url
            },
        placeholder = { Text(text = "Cari atau ketik alamat", fontSize = 14.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = {
            val target = normalizeInput(input)
            if (target.isNotBlank()) {
                val t = tab ?: GeckoEngine.newTab()
                t.session.loadUri(target)
            }
            focusManager.clearFocus()
        }),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
    )
}

internal fun normalizeInput(raw: String): String {
    val t = raw.trim()
    if (t.isEmpty()) return ""
    if (t.startsWith("about:")) return t
    val looksLikeUrl = !t.contains(" ") && (t.contains(".") || t.contains("://") || t.contains("/"))
    if (!looksLikeUrl) {
        return "https://duckduckgo.com/?q=" + URLEncoder.encode(t, "UTF-8")
    }
    return if (t.contains("://")) t else "https://$t"
}

@Composable
private fun TabsDialog(onClose: () -> Unit, onNewTab: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(text = "Tab (${GeckoEngine.tabs.size})") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                items(GeckoEngine.tabs.toList(), key = { it.id }) { t ->
                    val selected = t.id == GeckoEngine.activeTabId
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.surfaceVariant
                                else Color.Transparent
                            )
                            .clickable { GeckoEngine.selectTab(t.id); onClose() }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = (if (t.isPrivate) "🕵️ " else "") + t.title.ifBlank { "Tab baru" },
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = t.url, fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = { GeckoEngine.closeTab(t.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup tab")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onNewTab) { Text(text = "+ Tab baru") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(text = "Tutup") } },
    )
}

@Composable
private fun BookmarksDialog(onClose: () -> Unit) {
    var items by remember { mutableStateOf(GeckoEngine.bookmarks.list()) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(text = "Bookmark") },
        text = {
            if (items.isEmpty()) {
                Text(text = "Belum ada bookmark. Ketuk ikon bintang di bilah alamat untuk menyimpan halaman ini.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                    items(items, key = { it.url }) { b ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                GeckoEngine.activeTab?.session?.loadUri(b.url) ?: GeckoEngine.newTab(b.url)
                                onClose()
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(text = b.title.ifBlank { b.url }, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(text = b.url, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = {
                                GeckoEngine.bookmarks.remove(b.url)
                                items = GeckoEngine.bookmarks.list()
                            }) { Icon(Icons.Default.Delete, contentDescription = "Hapus") }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(text = "Tutup") } },
    )
}

@Composable
private fun HistoryDialog(onClose: () -> Unit) {
    var items by remember { mutableStateOf(GeckoEngine.history.list()) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(text = "Riwayat") },
        text = {
            if (items.isEmpty()) {
                Text(text = "Riwayat kosong.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                    items(items, key = { it.url + it.time }) { h ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                GeckoEngine.activeTab?.session?.loadUri(h.url) ?: GeckoEngine.newTab(h.url)
                                onClose()
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(text = h.title.ifBlank { h.url }, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(text = h.url, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = {
                                GeckoEngine.history.remove(h.url, h.time)
                                items = GeckoEngine.history.list()
                            }) { Icon(Icons.Default.Delete, contentDescription = "Hapus") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                if (items.isNotEmpty()) {
                    TextButton(onClick = { GeckoEngine.history.clear(); items = emptyList() }) {
                        Text(text = "Hapus semua", color = Color(0xFFF6AEA9))
                    }
                }
                TextButton(onClick = onClose) { Text(text = "Tutup") }
            }
        },
    )
}

@Composable
private fun ProfileSwitchDialog(
    pm: ProfileManager,
    currentId: String,
    onPick: (String) -> Unit,
    onManage: () -> Unit,
    onClose: () -> Unit,
) {
    val profiles = remember { pm.ensureDefault() }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(text = "Ganti profil") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(profiles, key = { it.id }) { p ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = p.id != currentId) { onPick(p.id) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                                .background(Color(p.colorArgb)),
                            contentAlignment = Alignment.Center,
                        ) { Text(text = p.icon, fontSize = 20.sp) }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = p.name + if (p.id == currentId) " (aktif)" else "",
                            fontWeight = if (p.id == currentId) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onManage) { Text(text = "Kelola profil") } },
        dismissButton = { TextButton(onClick = onClose) { Text(text = "Batal") } },
    )
}
