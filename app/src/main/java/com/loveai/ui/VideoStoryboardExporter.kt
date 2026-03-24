package com.loveai.ui

import android.content.Context
import com.loveai.manager.MusicManager
import com.loveai.model.LovePlan
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VideoStoryboardExporter {

    data class ExportedStoryboard(
        val file: File,
        val sceneCount: Int
    )

    data class StoryboardScene(
        val order: Int,
        val effectType: String,
        val title: String,
        val subtitle: String,
        val assetName: String?,
        val durationMs: Long
    )

    data class StoryboardPreview(
        val planName: String,
        val title: String,
        val subtitle: String,
        val songName: String?,
        val tags: List<String>,
        val scenes: List<StoryboardScene>
    )

    fun export(context: Context, plan: LovePlan): ExportedStoryboard {
        val safeName = plan.name.ifBlank { "loveai_video" }
            .replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]+"), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val baseDir = context.getExternalFilesDir("exports") ?: context.filesDir
        val dir = File(baseDir, "video_jobs").apply { mkdirs() }
        val file = File(dir, "${safeName}_${timestamp}.json")

        val json = JSONObject().apply {
            put("planId", plan.id)
            put("planName", plan.name)
            put("title", plan.title)
            put("subtitle", plan.subtitle)
            put("themeKey", plan.themeKey)
            put("coverKey", plan.coverKey)
            put("songKey", plan.songKey)
            put("songName", MusicManager.getPlaylist().firstOrNull { it.key == plan.songKey }?.name)
            put("createdAt", System.currentTimeMillis())
            put(
                "tags",
                JSONArray().apply {
                    plan.tags.forEach { put(it) }
                }
            )
            put(
                "scenes",
                JSONArray().apply {
                    plan.effectTypes.forEachIndexed { index, type ->
                        val page = plan.pageTexts.getOrNull(index)
                        put(
                            JSONObject().apply {
                                put("order", index + 1)
                                put("effectType", type.name)
                                put("title", page?.title?.takeIf { it.isNotBlank() } ?: plan.title)
                                put("subtitle", page?.subtitle?.takeIf { it.isNotBlank() } ?: plan.subtitle)
                                put("assetUri", page?.assetUri)
                                put("assetName", page?.assetName)
                                put("durationMs", 10000)
                            }
                        )
                    }
                }
            )
        }

        file.writeText(json.toString(2), Charsets.UTF_8)
        return ExportedStoryboard(file = file, sceneCount = plan.effectTypes.size)
    }

    fun parsePreview(file: File): StoryboardPreview? {
        if (!file.exists()) return null
        val json = runCatching { JSONObject(file.readText(Charsets.UTF_8)) }.getOrNull() ?: return null
        val tags = mutableListOf<String>()
        val tagsArray = json.optJSONArray("tags") ?: JSONArray()
        for (index in 0 until tagsArray.length()) {
            tagsArray.optString(index).takeIf { it.isNotBlank() }?.let { tags += it }
        }

        val scenes = mutableListOf<StoryboardScene>()
        val scenesArray = json.optJSONArray("scenes") ?: JSONArray()
        for (index in 0 until scenesArray.length()) {
            val item = scenesArray.optJSONObject(index) ?: continue
            scenes += StoryboardScene(
                order = item.optInt("order").takeIf { it > 0 } ?: (index + 1),
                effectType = item.optString("effectType"),
                title = item.optString("title"),
                subtitle = item.optString("subtitle"),
                assetName = item.optString("assetName").ifBlank { null },
                durationMs = item.optLong("durationMs").takeIf { it > 0L } ?: 10000L
            )
        }

        return StoryboardPreview(
            planName = json.optString("planName"),
            title = json.optString("title"),
            subtitle = json.optString("subtitle"),
            songName = json.optString("songName").ifBlank { null },
            tags = tags,
            scenes = scenes
        )
    }
}
