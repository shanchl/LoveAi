package com.loveai.repository

import android.content.Context
import android.content.SharedPreferences
import com.loveai.model.EffectType
import com.loveai.model.LovePlan
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class PlanRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("loveai_plans", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PLANS = "plans"
        private val MAX_EFFECT_COUNT = EffectType.values().size
    }

    fun getAllPlans(): List<LovePlan> {
        val raw = prefs.getString(KEY_PLANS, "[]") ?: "[]"
        val array = JSONArray(raw)
        val plans = mutableListOf<LovePlan>()
        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            parsePlan(obj)?.let { plans += it }
        }
        return plans.sortedByDescending { it.createdAt }
    }

    fun getPlanById(id: String): LovePlan? {
        return getAllPlans().firstOrNull { it.id == id }
    }

    fun savePlan(
        name: String,
        title: String,
        subtitle: String,
        effectTypes: List<EffectType>,
        themeKey: String? = null,
        songKey: String? = null,
        existingId: String? = null
    ): LovePlan {
        val plans = getAllPlans().toMutableList()
        val plan = LovePlan(
            id = existingId ?: UUID.randomUUID().toString(),
            name = name.ifBlank { "\u6211\u7684\u65b9\u6848" },
            title = title.ifBlank { "\u7ed9\u4f60\u7684\u6d6a\u6f2b\u7247\u6bb5" },
            subtitle = subtitle,
            effectTypes = effectTypes.take(MAX_EFFECT_COUNT),
            themeKey = themeKey,
            songKey = songKey,
            createdAt = System.currentTimeMillis()
        )

        val existingIndex = plans.indexOfFirst { it.id == plan.id }
        if (existingIndex >= 0) {
            plans[existingIndex] = plan
        } else {
            plans += plan
        }
        persistPlans(plans)
        return plan
    }

    fun deletePlan(id: String) {
        val updated = getAllPlans().filterNot { it.id == id }
        persistPlans(updated)
    }

    private fun persistPlans(plans: List<LovePlan>) {
        val array = JSONArray()
        plans.forEach { plan ->
            array.put(
                JSONObject().apply {
                    put("id", plan.id)
                    put("name", plan.name)
                    put("title", plan.title)
                    put("subtitle", plan.subtitle)
                    put("themeKey", plan.themeKey)
                    put("songKey", plan.songKey)
                    put("createdAt", plan.createdAt)
                    put(
                        "effectTypes",
                        JSONArray().apply {
                            plan.effectTypes.forEach { put(it.name) }
                        }
                    )
                }
            )
        }
        prefs.edit().putString(KEY_PLANS, array.toString()).apply()
    }

    private fun parsePlan(obj: JSONObject): LovePlan? {
        val typeArray = obj.optJSONArray("effectTypes") ?: return null
        val types = mutableListOf<EffectType>()
        for (index in 0 until typeArray.length()) {
            val typeName = typeArray.optString(index)
            runCatching { EffectType.valueOf(typeName) }.getOrNull()?.let { types += it }
        }
        if (types.isEmpty()) return null

        return LovePlan(
            id = obj.optString("id"),
            name = obj.optString("name"),
            title = obj.optString("title"),
            subtitle = obj.optString("subtitle"),
            effectTypes = types.take(MAX_EFFECT_COUNT),
            themeKey = obj.optString("themeKey").ifBlank { null },
            songKey = obj.optString("songKey").ifBlank { null },
            createdAt = obj.optLong("createdAt")
        )
    }
}
