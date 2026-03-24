package com.loveai.ui

import android.content.Context
import com.loveai.manager.MusicManager
import com.loveai.model.LovePlan
import com.loveai.model.VideoAspectPreset
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
        val aspectPresetLabel: String,
        val hasIntro: Boolean,
        val hasOutro: Boolean,
        val introDurationMs: Long,
        val outroDurationMs: Long,
        val tags: List<String>,
        val scenes: List<StoryboardScene>
    )

    fun export(
        context: Context,
        plan: LovePlan,
        aspectPreset: VideoAspectPreset = VideoAspectPreset.PORTRAIT
    ): ExportedStoryboard {
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
            put("aspectPresetKey", aspectPreset.key)
            put("frameWidth", aspectPreset.width)
            put("frameHeight", aspectPreset.height)
            put("hasIntro", true)
            put("hasOutro", true)
            put("introDurationMs", 3000)
            put("outroDurationMs", 3000)
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

        val preset = VideoAspectPreset.fromKey(json.optString("aspectPresetKey")) ?: VideoAspectPreset.PORTRAIT
        return StoryboardPreview(
            planName = json.optString("planName"),
            title = json.optString("title"),
            subtitle = json.optString("subtitle"),
            songName = json.optString("songName").ifBlank { null },
            aspectPresetLabel = preset.label,
            hasIntro = json.optBoolean("hasIntro", true),
            hasOutro = json.optBoolean("hasOutro", true),
            introDurationMs = json.optLong("introDurationMs").takeIf { it > 0L } ?: 3000L,
            outroDurationMs = json.optLong("outroDurationMs").takeIf { it > 0L } ?: 3000L,
            tags = tags,
            scenes = scenes
        )
    }

    fun updateSceneDuration(file: File, order: Int, durationMs: Long): Boolean {
        if (!file.exists()) return false
        val json = runCatching { JSONObject(file.readText(Charsets.UTF_8)) }.getOrNull() ?: return false
        val scenes = json.optJSONArray("scenes") ?: return false
        for (index in 0 until scenes.length()) {
            val item = scenes.optJSONObject(index) ?: continue
            val itemOrder = item.optInt("order").takeIf { it > 0 } ?: (index + 1)
            if (itemOrder == order) {
                item.put("durationMs", durationMs.coerceAtLeast(5000L))
                file.writeText(json.toString(2), Charsets.UTF_8)
                return true
            }
        }
        return false
    }

    fun moveScene(file: File, order: Int, direction: Int): Boolean {
        if (!file.exists()) return false
        val json = runCatching { JSONObject(file.readText(Charsets.UTF_8)) }.getOrNull() ?: return false
        val scenesArray = json.optJSONArray("scenes") ?: return false
        val scenes = mutableListOf<JSONObject>()
        for (index in 0 until scenesArray.length()) {
            scenesArray.optJSONObject(index)?.let { scenes += it }
        }
        if (scenes.isEmpty()) return false

        val sourceIndex = scenes.indexOfFirst {
            (it.optInt("order").takeIf { value -> value > 0 } ?: (scenes.indexOf(it) + 1)) == order
        }
        if (sourceIndex < 0) return false
        val targetIndex = (sourceIndex + direction).coerceIn(0, scenes.lastIndex)
        if (targetIndex == sourceIndex) return false

        val moving = scenes.removeAt(sourceIndex)
        scenes.add(targetIndex, moving)
        val updatedArray = JSONArray()
        scenes.forEachIndexed { index, item ->
            item.put("order", index + 1)
            updatedArray.put(item)
        }
        json.put("scenes", updatedArray)
        file.writeText(json.toString(2), Charsets.UTF_8)
        return true
    }

    fun toggleEdgeScene(file: File, edge: String): Boolean {
        if (!file.exists()) return false
        val json = runCatching { JSONObject(file.readText(Charsets.UTF_8)) }.getOrNull() ?: return false
        return when (edge) {
            "intro" -> {
                json.put("hasIntro", !json.optBoolean("hasIntro", true))
                file.writeText(json.toString(2), Charsets.UTF_8)
                true
            }
            "outro" -> {
                json.put("hasOutro", !json.optBoolean("hasOutro", true))
                file.writeText(json.toString(2), Charsets.UTF_8)
                true
            }
            else -> false
        }
    }
}
