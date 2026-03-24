package com.loveai.repository

import android.content.Context
import android.content.SharedPreferences
import com.loveai.model.VideoAspectPreset
import com.loveai.model.VideoExportStatus
import com.loveai.model.VideoExportTask
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class VideoExportRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("loveai_video_exports", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TASKS = "video_tasks"
        private const val MAX_TASKS = 50
    }

    fun getAllTasks(): List<VideoExportTask> {
        val raw = prefs.getString(KEY_TASKS, "[]") ?: "[]"
        val array = JSONArray(raw)
        val tasks = mutableListOf<VideoExportTask>()
        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            parseTask(obj)?.let { tasks += it }
        }
        return tasks.sortedByDescending { it.createdAt }
    }

    fun enqueue(
        planId: String,
        planName: String,
        aspectPreset: VideoAspectPreset
    ): VideoExportTask {
        val task = VideoExportTask(
            id = UUID.randomUUID().toString(),
            planId = planId,
            planName = planName,
            aspectPresetKey = aspectPreset.key,
            status = VideoExportStatus.QUEUED,
            createdAt = System.currentTimeMillis(),
            note = "\u7b49\u5f85\u751f\u6210 ${aspectPreset.label} \u89c6\u9891\u811a\u672c\u5305"
        )
        persist((listOf(task) + getAllTasks()).take(MAX_TASKS))
        return task
    }

    fun update(task: VideoExportTask) {
        val tasks = getAllTasks().toMutableList()
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index >= 0) {
            tasks[index] = task
            persist(tasks)
        }
    }

    private fun persist(tasks: List<VideoExportTask>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(
                JSONObject().apply {
                    put("id", task.id)
                    put("planId", task.planId)
                    put("planName", task.planName)
                    put("aspectPresetKey", task.aspectPresetKey)
                    put("status", task.status.key)
                    put("outputPath", task.outputPath)
                    put("createdAt", task.createdAt)
                    put("finishedAt", task.finishedAt)
                    put("note", task.note)
                }
            )
        }
        prefs.edit().putString(KEY_TASKS, array.toString()).apply()
    }

    private fun parseTask(obj: JSONObject): VideoExportTask? {
        val status = VideoExportStatus.fromKey(obj.optString("status")) ?: return null
        val id = obj.optString("id")
        if (id.isBlank()) return null
        val preset = VideoAspectPreset.fromKey(obj.optString("aspectPresetKey")) ?: VideoAspectPreset.PORTRAIT
        return VideoExportTask(
            id = id,
            planId = obj.optString("planId"),
            planName = obj.optString("planName"),
            aspectPresetKey = preset.key,
            status = status,
            outputPath = obj.optString("outputPath").ifBlank { null },
            createdAt = obj.optLong("createdAt"),
            finishedAt = obj.optLong("finishedAt"),
            note = obj.optString("note")
        )
    }
}
