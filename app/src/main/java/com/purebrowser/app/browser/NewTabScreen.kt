package com.purebrowser.app.browser

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.net.URLEncoder

/** Mesin pencari yang tersedia di halaman tab baru. */
enum class SearchEngine(val label: String, val searchUrl: String) {
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q="),
    GOOGLE("Google", "https://www.google.com/search?q="),
    BRAVE("Brave", "https://search.brave.com/search?q="),
    BING("Bing", "https://www.bing.com/search?q="),
}

private data class Shortcut(val name: String, val url: String, val emoji: String)

private val DEFAULT_SHORTCUTS = listOf(
    Shortcut("Google", "https://www.google.com", "🔍"),
    Shortcut("YouTube", "https://m.youtube.com", "▶️"),
    Shortcut("Wikipedia", "https://id.wikipedia.org", "📚"),
    Shortcut("GitHub", "https://github.com", "🐙"),
    Shortcut("Kamus", "https://kbbi.web.id", "📖"),
    Shortcut("Berita", "https://www.detik.com", "📰"),
)

/**
 * Halaman tab baru ala Chrome: kolom pencarian besar, pilihan mesin pencari,
 * shortcut situs, dan situs terakhir dikunjungi. Native Compose — tanpa WebView.
 */
@Composable
fun NewTabScreen(tab: Tab) {
    val focusManager = LocalFocusManager.current
    var query by remember { mutableStateOf("") }
    var engine by remember { mutableStateOf(SearchEngine.DUCKDUCKGO) }
    var engineMenuOpen by remember { mutableStateOf(false) }

    fun navigate(target: String) {
        val url = normalizeInput(target.ifBlank { return })
        if (url.isBlank()) return
        tab.isNewTabPage = false
        tab.session.loadUri(url)
        focusManager.clearFocus()
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(56.dp))

        // Logo
        Text(text = "🌐", fontSize = 56.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "PureBrowser",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Jelajah bebas, tanpa pelacak",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )

        Spacer(Modifier.height(32.dp))

        // Kolom pencarian
        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(text = "Telusuri atau ketik alamat web", fontSize = 15.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    val q = query.trim()
                    if (q.isNotBlank()) {
                        val looksLikeUrl = !q.contains(" ") &&
                            (q.contains(".") || q.contains("://") || q.contains("/"))
                        if (looksLikeUrl) navigate(q)
                        else navigate(engine.searchUrl + URLEncoder.encode(q, "UTF-8"))
                    }
                }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp),
            )
            // Pemilih mesin pencari
            Box {
                IconButton(onClick = { engineMenuOpen = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = engine.label.take(6), fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Pilih mesin pencari",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
                DropdownMenu(
                    expanded = engineMenuOpen,
                    onDismissRequest = { engineMenuOpen = false },
                ) {
                    SearchEngine.entries.forEach { e ->
                        DropdownMenuItem(
                            text = { Text(text = e.label) },
                            onClick = { engine = e; engineMenuOpen = false },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // Shortcut
        Text(
            text = "Akses cepat",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
        )
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(DEFAULT_SHORTCUTS) { s ->
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { navigate(s.url) }
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = s.emoji, fontSize = 28.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = s.name, fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Terakhir dikunjungi
        val recent = remember { GeckoEngine.history.list().take(5) }
        if (recent.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.History, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Terakhir dikunjungi",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
            }
            Spacer(Modifier.height(8.dp))
            recent.forEach { h ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { navigate(h.url) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = h.title.firstOrNull()?.uppercase() ?: "•",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = h.title.ifBlank { h.url },
                            fontSize = 14.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = h.url, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
