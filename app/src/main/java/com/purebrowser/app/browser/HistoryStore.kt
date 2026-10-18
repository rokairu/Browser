package com.purebrowser.app.browser

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class HistoryEntry(val url: String, val title: String, val time: Long)

class HistoryStore(context: Context) {
    private val file = File(context.filesDir, "history.json")

    fun record(url: String, title: String) {
        if (url.isBlank() || url.startsWith("about:")) return
        val entries = read().toMutableList()
        // Hindari duplikat beruntun
        if (entries.firstOrNull()?.url == url) return
        entries.add(0, HistoryEntry(url, title.ifBlank { url }, System.currentTimeMillis()))
        write(entries.take(500))
    }

    fun list(): List<HistoryEntry> = read()

    fun clear() = write(emptyList())

    fun remove(url: String, time: Long) =
        write(read().filterNot { it.url == url && it.time == time })

    private fun read(): List<HistoryEntry> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                HistoryEntry(o.getString("url"), o.optString("title", ""), o.optLong("time", 0))
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun write(entries: List<HistoryEntry>) {
        try {
            val arr = JSONArray()
            entries.forEach {
                arr.put(JSONObject().put("url", it.url).put("title", it.title).put("time", it.time))
            }
            file.parentFile?.mkdirs()
            file.writeText(arr.toString())
        } catch (_: Exception) { }
    }
}
