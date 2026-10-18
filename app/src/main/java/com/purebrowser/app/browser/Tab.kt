package com.purebrowser.app.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.mozilla.geckoview.GeckoSession
import java.util.UUID

/** Satu tab = satu GeckoSession. State diobservasi Compose. */
class Tab(
    val id: String = UUID.randomUUID().toString(),
    val session: GeckoSession,
    val isPrivate: Boolean = false,
    initialUrl: String? = null,
) {
    var title by mutableStateOf(initialUrl ?: "Tab baru")
    var url by mutableStateOf(initialUrl ?: "")
    var isLoading by mutableStateOf(false)
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
    var progress by mutableStateOf(0)
}
