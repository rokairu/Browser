package com.purebrowser.app.browser

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mozilla.geckoview.WebExtension
import java.io.File

/** Rekomendasi ekstensi yang kompatibel Android (diunduh dari AMO, sudah signed). */
private data class RecommendedExt(val name: String, val desc: String, val url: String)

private val RECOMMENDED = listOf(
    RecommendedExt(
        "uBlock Origin",
        "Pemblokir iklan & pelacak yang ringan",
        "https://addons.mozilla.org/firefox/downloads/latest/ublock-origin/latest.xpi",
    ),
    RecommendedExt(
        "Privacy Badger",
        "Blokir pelacak tersembunyi otomatis",
        "https://addons.mozilla.org/firefox/downloads/latest/privacy-badger17/latest.xpi",
    ),
    RecommendedExt(
        "Dark Reader",
        "Mode gelap untuk semua situs",
        "https://addons.mozilla.org/firefox/downloads/latest/darkreader/latest.xpi",
    ),
)

@Composable
fun ExtensionsDialog(onClose: () -> Unit) {
    val context = LocalContext.current
    val extensions = ExtensionManager.extensions
    val isBusy = ExtensionManager.isBusy
    val lastError = ExtensionManager.lastError
    var confirmUninstall by remember { mutableStateOf<WebExtension?>(null) }
    var copyError by remember { mutableStateOf<String?>(null) }
    // Gabungkan error dari copy file dan dari ExtensionManager.
    val displayError = copyError ?: lastError

    // Pemilih file .xpi — disalin ke penyimpanan privat sebelum install.
    // File sementara dihapus otomatis oleh ExtensionManager setelah install.
    val pickXpi = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val dir = File(context.filesDir, "extensions").apply { mkdirs() }
                val dest = File(dir, "ext-${System.currentTimeMillis()}.xpi")
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw java.io.IOException("Tidak bisa membaca file yang dipilih")
                input.use { ins ->
                    dest.outputStream().use { out -> ins.copyTo(out) }
                }
                if (dest.length() == 0L) {
                    dest.delete()
                    throw java.io.IOException("File kosong atau gagal disalin")
                }
                ExtensionManager.installFromFile(dest)
            } catch (e: Exception) {
                // Tampilkan error langsung via state lokal dialog.
                copyError = "Gagal menyiapkan file: ${e.message ?: "kesalahan tidak diketahui"}"
            }
        }
    }

    // Refresh daftar tiap dialog dibuka.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        ExtensionManager.refresh()
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(text = "Ekstensi (${extensions.size})") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (displayError != null) {
                    Text(
                        text = displayError!! + "\n\n(Ketuk untuk menutup)",
                        fontSize = 13.sp,
                        color = Color(0xFFF6AEA9),
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF5A1F1A).copy(alpha = 0.4f))
                            .clickable {
                                copyError = null
                                ExtensionManager.clearError()
                            }
                            .padding(10.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (isBusy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text = "Memasang ekstensi…", fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    if (extensions.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    Icons.Default.Extension, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(8.dp),
                                )
                                Text(
                                    text = "Belum ada ekstensi terpasang.",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                )
                            }
                        }
                    }
                    items(extensions, key = { it.id }) { ext ->
                        val meta = ext.metaData
                        val enabled = meta?.enabled != false
                        val hasOptions = !meta?.optionsPageUrl.isNullOrBlank()
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = meta?.name ?: ext.id,
                                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "v${meta?.version ?: "?"}${meta?.description?.let { " • $it" } ?: ""}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                                )
                            }
                            // Tombol buka halaman pengaturan ekstensi (jika ada).
                            if (hasOptions) {
                                IconButton(onClick = {
                                    val url = meta?.optionsPageUrl ?: return@IconButton
                                    GeckoEngine.newTab(url)
                                    onClose()
                                }) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "Pengaturan ekstensi",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            Switch(
                                checked = enabled,
                                onCheckedChange = { ExtensionManager.setEnabled(ext, it) },
                            )
                            IconButton(onClick = { confirmUninstall = ext }) {
                                Icon(
                                    Icons.Default.Delete, contentDescription = "Hapus ekstensi",
                                    tint = Color(0xFFF6AEA9),
                                )
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Rekomendasi",
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    items(RECOMMENDED) { rec ->
                        val alreadyInstalled = extensions.any {
                            it.metaData?.name?.contains(rec.name, ignoreCase = true) == true
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !alreadyInstalled && !isBusy) {
                                    ExtensionManager.installFromUrl(rec.url)
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(text = rec.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = rec.desc, fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                )
                            }
                            if (alreadyInstalled) {
                                Text(
                                    text = "Terpasang", fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Icon(
                                    Icons.Default.Download, contentDescription = "Pasang",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Hanya ekstensi bertanda tangan Mozilla (dari addons.mozilla.org) yang bisa dipasang. Tidak semua ekstensi desktop cocok untuk Android.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { pickXpi.launch(arrayOf("application/x-xpinstall", "application/octet-stream")) },
                enabled = !isBusy,
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(text = "Dari file .xpi")
            }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(text = "Tutup") } },
    )

    // Konfirmasi hapus
    confirmUninstall?.let { ext ->
        AlertDialog(
            onDismissRequest = { confirmUninstall = null },
            title = { Text(text = "Hapus ekstensi?") },
            text = {
                Text(text = "\"${ext.metaData?.name ?: ext.id}\" akan dihapus beserta seluruh datanya.")
            },
            confirmButton = {
                TextButton(onClick = {
                    ExtensionManager.uninstall(ext)
                    confirmUninstall = null
                }) { Text(text = "Hapus", color = Color(0xFFF6AEA9)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmUninstall = null }) { Text(text = "Batal") }
            },
        )
    }
}
