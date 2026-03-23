package com.loveai.repository

import android.content.Context
import android.content.SharedPreferences
import com.loveai.model.FavoriteSequence
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FavoriteRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("loveai_favorites", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FAVORITE_SEQUENCES = "favorite_sequences"
    }

    fun getAllFavorites(): List<FavoriteSequence> {
        val raw = prefs.getString(KEY_FAVORITE_SEQUENCES, "[]") ?: "[]"
        val array = JSONArray(raw)
        val favorites = mutableListOf<FavoriteSequence>()
        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            parseFavorite(obj)?.let { favorites += it }
        }
        return favorites.sortedByDescending { it.createdAt }
    }

    fun getFavoriteById(id: String): FavoriteSequence? {
        return getAllFavorites().firstOrNull { it.id == id }
    }

    fun isFavoriteSequence(effectVariantIds: List<Int>): Boolean {
        return getAllFavorites().any { it.effectVariantIds == effectVariantIds }
    }

    fun toggleFavorite(
        effectVariantIds: List<Int>,
        title: String,
        subtitle: String,
        songKey: String? = null,
        name: String? = null
    ): Boolean {
        val favorites = getAllFavorites().toMutableList()
        val existingIndex = favorites.indexOfFirst { it.effectVariantIds == effectVariantIds }
        if (existingIndex >= 0) {
            favorites.removeAt(existingIndex)
            persistFavorites(favorites)
            return false
        }

        favorites += FavoriteSequence(
            id = UUID.randomUUID().toString(),
            name = name?.takeIf { it.isNotBlank() } ?: buildDefaultName(),
            title = title,
            subtitle = subtitle,
            effectVariantIds = effectVariantIds,
            songKey = songKey,
            createdAt = System.currentTimeMillis()
        )
        persistFavorites(favorites)
        return true
    }

    fun deleteFavorite(id: String) {
        val updated = getAllFavorites().filterNot { it.id == id }
        persistFavorites(updated)
    }

    fun getFavoriteCount(): Int = getAllFavorites().size

    private fun persistFavorites(favorites: List<FavoriteSequence>) {
        val array = JSONArray()
        favorites.forEach { favorite ->
            array.put(
                JSONObject().apply {
                    put("id", favorite.id)
                    put("name", favorite.name)
                    put("title", favorite.title)
                    put("subtitle", favorite.subtitle)
                    put("songKey", favorite.songKey)
                    put("createdAt", favorite.createdAt)
                    put(
                        "effectVariantIds",
                        JSONArray().apply {
                            favorite.effectVariantIds.forEach { put(it) }
                        }
                    )
                }
            )
        }
        prefs.edit().putString(KEY_FAVORITE_SEQUENCES, array.toString()).apply()
    }

    private fun parseFavorite(obj: JSONObject): FavoriteSequence? {
        val idsArray = obj.optJSONArray("effectVariantIds") ?: return null
        val variantIds = mutableListOf<Int>()
        for (index in 0 until idsArray.length()) {
            val id = idsArray.optInt(index, -1)
            if (id >= 0) {
                variantIds += id
            }
        }
        if (variantIds.isEmpty()) return null

        return FavoriteSequence(
            id = obj.optString("id"),
            name = obj.optString("name"),
            title = obj.optString("title"),
            subtitle = obj.optString("subtitle"),
            effectVariantIds = variantIds,
            songKey = obj.optString("songKey").ifBlank { null },
            createdAt = obj.optLong("createdAt")
        )
    }

    private fun buildDefaultName(): String {
        val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        return "\u6536\u85cf\u52a8\u6001 ${formatter.format(Date())}"
    }
}
