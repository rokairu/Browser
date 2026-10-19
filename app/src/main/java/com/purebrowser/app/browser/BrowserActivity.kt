package com.purebrowser.app.browser

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
import com.purebrowser.app.profile.ProfileContext
import com.purebrowser.app.profile.ProfileManager
import com.purebrowser.app.profile.ProfileRuntime
import com.purebrowser.app.ui.PureBrowserTheme
import org.mozilla.geckoview.GeckoView

class BrowserActivity : ComponentActivity() {

    private lateinit var geckoView: GeckoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pm = ProfileManager(applicationContext)
        val profiles = pm.ensureDefault()
        val targetId = intent.getStringExtra(EXTRA_PROFILE_ID)
            ?: pm.activeProfileId()
            ?: profiles.first().id
        require(pm.get(targetId) != null) { "Profil tidak ditemukan" }

        ProfileRuntime.commit(targetId)
        pm.setActiveProfileId(targetId)

        val profileCtx = ProfileContext(this, targetId)
        GeckoEngine.init(profileCtx)
        if (GeckoEngine.tabs.isEmpty()) {
            if (!GeckoEngine.restoreTabs(this)) {
                GeckoEngine.newTab()
            }
        }

        // View dibuat dengan context Activity (butuh Activity untuk prompt/permission UI);
        // isolasi profil dipegang oleh GeckoRuntime yang dibuat dengan ProfileContext.
        geckoView = GeckoView(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val tab = GeckoEngine.activeTab
                if (tab?.canGoBack == true) tab.session.goBack()
                else finish()
            }
        })

        setContent {
            PureBrowserTheme {
                BrowserScreen(
                    geckoView = geckoView,
                    onSwitchProfile = { newId -> switchProfile(newId) },
                    onOpenProfileManager = {
                        GeckoEngine.persistTabs(this)
                        startActivity(
                            Intent(this, com.purebrowser.app.MainActivity::class.java).apply {
                                putExtra(
                                    com.purebrowser.app.MainActivity.EXTRA_MANAGE_PROFILES,
                                    true,
                                )
                            }
                        )
                    },
                    onExit = {
                        GeckoEngine.shutdown()
                        finish()
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val newId = intent.getStringExtra(EXTRA_PROFILE_ID) ?: return
        if (newId != ProfileRuntime.committedProfileId) switchProfile(newId)
    }

    override fun onPause() {
        GeckoEngine.persistTabs(this)
        super.onPause()
    }

    /**
     * Ganti profil = simpan tab, jadwalkan kelahiran ulang activity,
     * lalu bunuh proses ":profile" ini. Proses baru akan commit ke
     * profil yang baru dengan direktori datanya sendiri.
     */
    private fun switchProfile(newId: String) {
        ProfileManager(applicationContext).setActiveProfileId(newId)
        GeckoEngine.shutdown()
        val intent = Intent(this, BrowserActivity::class.java).apply {
            putExtra(EXTRA_PROFILE_ID, newId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val pi = PendingIntent.getActivity(
            this, 42, intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.set(AlarmManager.RTC, System.currentTimeMillis() + 400, pi)
        Process.killProcess(Process.myPid())
    }

    companion object {
        const val EXTRA_PROFILE_ID = "profile_id"
    }
}
