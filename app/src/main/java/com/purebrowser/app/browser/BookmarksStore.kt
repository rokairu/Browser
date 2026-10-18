package com.purebrowser.app.browser

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Bookmark(val url: String, val title: String, val time: Long)

class BookmarksStore(context: Context) {
    private val file = File(context.filesDir, "bookmarks.json")

    fun list(): List<Bookmark> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Bookmark(o.getString("url"), o.optString("title", ""), o.optLong("time", 0))
            }
        } catch (_: Exception) { emptyList() }
    }

    fun isBookmarked(url: String): Boolean = list().any { it.url == url }

    fun toggle(url: String, title: String): Boolean {
        val all = list().toMutableList()
        val existing = all.find { it.url == url }
        return if (existing != null) {
            all.remove(existing); save(all); false
        } else {
            all.add(0, Bookmark(url, title.ifBlank { url }, System.currentTimeMillis())); save(all); true
        }
    }

    fun remove(url: String) = save(list().filterNot { it.url == url })

    private fun save(bookmarks: List<Bookmark>) {
        try {
            val arr = JSONArray()
            bookmarks.forEach {
                arr.put(JSONObject().put("url", it.url).put("title", it.title).put("time", it.time))
            }
            file.parentFile?.mkdirs()
            file.writeText(arr.toString())
        } catch (_: Exception) { }
    }
}
