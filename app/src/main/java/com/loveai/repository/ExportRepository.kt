package com.loveai.repository

import android.content.Context
import android.content.SharedPreferences
import com.loveai.model.ExportRecord
import com.loveai.model.ExportType
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ExportRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("loveai_exports", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_EXPORTS = "exports"
        private const val MAX_EXPORT_RECORDS = 40
    }

    fun getAllRecords(): List<ExportRecord> {
        val raw = prefs.getString(KEY_EXPORTS, "[]") ?: "[]"
        val array = JSONArray(raw)
        val records = mutableListOf<ExportRecord>()
        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            parseRecord(obj)?.let { records += it }
        }
        return records.sortedByDescending { it.createdAt }
    }

    fun addRecord(
        planId: String,
        planName: String,
        exportType: ExportType,
        outputPath: String
    ) {
        val updated = getAllRecords().toMutableList()
        updated += ExportRecord(
            id = UUID.randomUUID().toString(),
            planId = planId,
            planName = planName,
            exportType = exportType,
            outputPath = outputPath,
            createdAt = System.currentTimeMillis()
        )
        persist(updated.sortedByDescending { it.createdAt }.take(MAX_EXPORT_RECORDS))
    }

    fun clearAll() {
        prefs.edit().putString(KEY_EXPORTS, "[]").apply()
    }

    private fun persist(records: List<ExportRecord>) {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject().apply {
                    put("id", record.id)
                    put("planId", record.planId)
                    put("planName", record.planName)
                    put("exportType", record.exportType.key)
                    put("outputPath", record.outputPath)
                    put("createdAt", record.createdAt)
                }
            )
        }
        prefs.edit().putString(KEY_EXPORTS, array.toString()).apply()
    }

    private fun parseRecord(obj: JSONObject): ExportRecord? {
        val exportType = ExportType.fromKey(obj.optString("exportType")) ?: return null
        val id = obj.optString("id")
        if (id.isBlank()) return null
        return ExportRecord(
            id = id,
            planId = obj.optString("planId"),
            planName = obj.optString("planName"),
            exportType = exportType,
            outputPath = obj.optString("outputPath"),
            createdAt = obj.optLong("createdAt")
        )
    }
}
