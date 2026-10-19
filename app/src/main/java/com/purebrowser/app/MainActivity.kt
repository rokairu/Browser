package com.purebrowser.app

import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purebrowser.app.browser.BrowserActivity
import com.purebrowser.app.profile.Profile
import com.purebrowser.app.profile.ProfileManager
import com.purebrowser.app.ui.PureBrowserTheme

/**
 * Pemilih profil ala Chrome desktop — layar pertama aplikasi.
 * Berjalan di proses utama (bukan :profile).
 */
class MainActivity : ComponentActivity() {

    private lateinit var pm: ProfileManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pm = ProfileManager(applicationContext)

        // Buka langsung ke browser kecuali diminta kelola profil secara eksplisit.
        // (Dari menu browser: "Ganti profil" -> "Kelola profil".)
        val manageProfiles = intent.getBooleanExtra(EXTRA_MANAGE_PROFILES, false)
        val autoVpn = intent.getBooleanExtra(EXTRA_AUTO_VPN, false)
        if (!manageProfiles && !autoVpn) {
            val profiles = pm.ensureDefault()
            val targetId = pm.activeProfileId() ?: profiles.first().id
            pm.setActiveProfileId(targetId)
            val i = Intent(this, BrowserActivity::class.java).apply {
                putExtra(BrowserActivity.EXTRA_PROFILE_ID, targetId)
            }
            startActivity(i)
            finish()
            return
        }
        render(autoVpn = autoVpn)
    }

    override fun onResume() {
        super.onResume()
        // Refresh daftar setiap kembali ke layar ini
        render(autoVpn = false)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        render(autoVpn = intent.getBooleanExtra(EXTRA_AUTO_VPN, false))
    }

    private fun render(autoVpn: Boolean) {
        setContent {
            PureBrowserTheme {
                ProfilePickerScreen(
                    pm = pm,
                    autoOpenVpn = autoVpn,
                    onOpen = { profile ->
                        pm.setActiveProfileId(profile.id)
                        val i = Intent(this, BrowserActivity::class.java).apply {
                            putExtra(BrowserActivity.EXTRA_PROFILE_ID, profile.id)
                        }
                        startActivity(i)
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_AUTO_VPN = "auto_vpn"
        const val EXTRA_MANAGE_PROFILES = "manage_profiles"
    }
}

@Composable
private fun ProfilePickerScreen(
    pm: ProfileManager,
    autoOpenVpn: Boolean,
    onOpen: (Profile) -> Unit,
) {
    var profiles by remember { mutableStateOf(pm.ensureDefault()) }
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Profile?>(null) }
    var deleteTarget by remember { mutableStateOf<Profile?>(null) }
    var showVpn by remember { mutableStateOf(autoOpenVpn) }
    val vpnState by com.purebrowser.app.vpn.VpnController.state
        .collectAsState(initial = com.wireguard.android.backend.Tunnel.State.DOWN)

    fun refresh() { profiles = pm.ensureDefault() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah profil")
            }
        },
    ) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "PureBrowser",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                // Indikator + tombol VPN (dikelola di proses utama)
                val vpnUp = vpnState == com.wireguard.android.backend.Tunnel.State.UP
                IconButton(onClick = { showVpn = true }) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "VPN",
                            tint = if (vpnUp) Color(0xFF81C995)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                        if (vpnUp) {
                            Box(
                                modifier = Modifier.align(Alignment.TopEnd).size(8.dp)
                                    .clip(CircleShape).background(Color(0xFF81C995)),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(text = "Siapa yang browsing?", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tiap profil punya cookie, riwayat, bookmark, dan login sendiri.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(20.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(profiles, key = { it.id }) { p ->
                    ProfileCard(
                        profile = p,
                        isActive = pm.activeProfileId() == p.id,
                        onOpen = { onOpen(p) },
                        onEdit = { editTarget = p },
                        onDelete = { deleteTarget = p },
                    )
                }
            }
        }
    }

    if (showVpn) {
        com.purebrowser.app.vpn.VpnDialog(onClose = { showVpn = false })
    }

    if (showAdd) {
        ProfileEditorDialog(
            title = "Profil baru",
            onDismiss = { showAdd = false },
            onSave = { name, color, icon ->
                pm.add(name, color, icon); refresh(); showAdd = false
            },
        )
    }
    editTarget?.let { p ->
        ProfileEditorDialog(
            title = "Ubah profil",
            initial = p,
            onDismiss = { editTarget = null },
            onSave = { name, color, icon ->
                pm.rename(p.id, name); pm.recolor(p.id, color, icon)
                refresh(); editTarget = null
            },
        )
    }
    deleteTarget?.let { p ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(text = "Hapus profil?") },
            text = { Text(text = "Profil \"${p.name}\" dan seluruh datanya (riwayat, bookmark, login) akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = {
                    if (pm.delete(p.id)) {
                        // Kalau profil yang aktif dihapus saat proses browser hidup, bunuh prosesnya
                        killProfileProcess()
                    }
                    refresh(); deleteTarget = null
                }) { Text(text = "Hapus", color = Color(0xFFF6AEA9)) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(text = "Batal") } },
        )
    }
}

/** Bunuh proses :profile jika sedang hidup (dipanggil dari proses utama). */
private fun killProfileProcess() {
    try {
        // Cari PID proses ":profile" milik aplikasi ini lalu bunuh.
        val self = Process.myPid()
        val dir = java.io.File("/proc")
        dir.listFiles { f -> f.name.all { c -> c.isDigit() } }?.forEach { pidDir ->
            try {
                val cmdline = java.io.File(pidDir, "cmdline").readText()
                if (cmdline.contains("com.purebrowser.app:profile")) {
                    val pid = pidDir.name.toInt()
                    if (pid != self) Process.killProcess(pid)
                }
            } catch (_: Exception) { }
        }
    } catch (_: Exception) { }
}

@Composable
private fun ProfileCard(
    profile: Profile,
    isActive: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape)
                    .background(Color(profile.colorArgb)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    com.purebrowser.app.profile.ProfileIcons.resolve(profile.icon),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(text = profile.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            if (isActive) {
                Text(text = "Aktif", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(4.dp))
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Ubah", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileEditorDialog(
    title: String,
    initial: Profile? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, color: Int, icon: String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var color by remember { mutableStateOf(initial?.colorArgb ?: Profile.PALETTE[0]) }
    var icon by remember { mutableStateOf(initial?.icon ?: Profile.ICONS[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = { onSave(name, color, icon) }) {
                Text(text = "Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Batal")
            }
        },
        title = { Text(text = title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text = "Nama profil") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Warna",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Profile.PALETTE.forEach { c ->
                        val selected = c == color
                        val boxSize = if (selected) 36.dp else 30.dp
                        Box(
                            modifier = Modifier
                                .size(boxSize)
                                .clip(CircleShape)
                                .background(Color(c))
                                .clickable { color = c }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Ikon",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Profile.ICONS.take(7).forEach { ic ->
                        val selected = ic == icon
                        val bgColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else Color.Transparent
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                                .background(bgColor)
                                .clickable { icon = ic },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                com.purebrowser.app.profile.ProfileIcons.resolve(ic),
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Profile.ICONS.drop(7).forEach { ic ->
                        val selected = ic == icon
                        val bgColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else Color.Transparent
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                                .background(bgColor)
                                .clickable { icon = ic },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                com.purebrowser.app.profile.ProfileIcons.resolve(ic),
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }
    )
}
