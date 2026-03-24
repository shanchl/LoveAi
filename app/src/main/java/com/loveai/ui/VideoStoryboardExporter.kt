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
}
