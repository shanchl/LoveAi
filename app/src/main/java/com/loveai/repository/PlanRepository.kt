package com.loveai.repository

import android.content.Context
import android.content.SharedPreferences
import com.loveai.model.EffectType
import com.loveai.model.LovePlan
import com.loveai.model.PlanPageText
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class PlanRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("loveai_plans", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PLANS = "plans"
        private const val MAX_EFFECT_COUNT = 8
    }

    enum class SortMode {
        RECENT,
        CREATED,
        NAME
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
        pageTexts: List<PlanPageText> = emptyList(),
        themeKey: String? = null,
        coverKey: String? = null,
        tags: List<String> = emptyList(),
        songKey: String? = null,
        existingId: String? = null
    ): LovePlan {
        val plans = getAllPlans().toMutableList()
        val existingPlan = existingId?.let { id -> plans.firstOrNull { it.id == id } }
        val plan = LovePlan(
            id = existingId ?: UUID.randomUUID().toString(),
            name = name.ifBlank { "\u6211\u7684\u65b9\u6848" },
            title = title.ifBlank { "\u7ed9\u4f60\u7684\u6d6a\u6f2b\u7247\u6bb5" },
            subtitle = subtitle,
            effectTypes = effectTypes.take(MAX_EFFECT_COUNT),
            pageTexts = pageTexts.take(MAX_EFFECT_COUNT),
            themeKey = themeKey,
            coverKey = coverKey,
            tags = tags.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(6),
            songKey = songKey,
            createdAt = existingPlan?.createdAt ?: System.currentTimeMillis(),
            lastOpenedAt = existingPlan?.lastOpenedAt ?: 0L,
            playCount = existingPlan?.playCount ?: 0
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

    fun duplicatePlan(id: String): LovePlan? {
        val original = getPlanById(id) ?: return null
        return savePlan(
            name = "${original.name} \u526f\u672c",
            title = original.title,
            subtitle = original.subtitle,
            effectTypes = original.effectTypes,
            pageTexts = original.pageTexts,
            themeKey = original.themeKey,
            coverKey = original.coverKey,
            tags = original.tags,
            songKey = original.songKey
        )
    }

    fun markPlanOpened(id: String) {
        val plans = getAllPlans().toMutableList()
        val index = plans.indexOfFirst { it.id == id }
        if (index < 0) return

        val target = plans[index]
        plans[index] = target.copy(
            lastOpenedAt = System.currentTimeMillis(),
            playCount = target.playCount + 1
        )
        persistPlans(plans)
    }

    fun queryPlans(
        keyword: String = "",
        themeKey: String? = null,
        sortMode: SortMode = SortMode.RECENT
    ): List<LovePlan> {
        val normalizedKeyword = keyword.trim()
        return getAllPlans()
            .asSequence()
            .filter { plan ->
                themeKey.isNullOrBlank() || plan.themeKey == themeKey
            }
            .filter { plan ->
                normalizedKeyword.isBlank() ||
                    plan.name.contains(normalizedKeyword, ignoreCase = true) ||
                    plan.title.contains(normalizedKeyword, ignoreCase = true) ||
                    plan.subtitle.contains(normalizedKeyword, ignoreCase = true) ||
                    plan.tags.any { it.contains(normalizedKeyword, ignoreCase = true) }
            }
            .sortedWith(sortComparator(sortMode))
            .toList()
    }

    private fun sortComparator(sortMode: SortMode): Comparator<LovePlan> {
        return when (sortMode) {
            SortMode.RECENT -> compareByDescending<LovePlan> {
                if (it.lastOpenedAt > 0L) it.lastOpenedAt else it.createdAt
            }.thenByDescending { it.createdAt }
            SortMode.CREATED -> compareByDescending<LovePlan> { it.createdAt }
            SortMode.NAME -> compareBy<LovePlan> { it.name.lowercase() }
        }
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
                    put(
                        "pageTexts",
                        JSONArray().apply {
                            plan.pageTexts.forEach { pageText ->
                                put(
                                    JSONObject().apply {
                                        put("title", pageText.title)
                                        put("subtitle", pageText.subtitle)
                                    }
                                )
                            }
                        }
                    )
                    put("themeKey", plan.themeKey)
                    put("coverKey", plan.coverKey)
                    put("songKey", plan.songKey)
                    put("createdAt", plan.createdAt)
                    put("lastOpenedAt", plan.lastOpenedAt)
                    put("playCount", plan.playCount)
                    put(
                        "tags",
                        JSONArray().apply {
                            plan.tags.forEach { put(it) }
                        }
                    )
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
        val pageTextsArray = obj.optJSONArray("pageTexts")
        val pageTexts = mutableListOf<PlanPageText>()
        if (pageTextsArray != null) {
            for (index in 0 until pageTextsArray.length()) {
                val pageObj = pageTextsArray.optJSONObject(index) ?: continue
                pageTexts += PlanPageText(
                    title = pageObj.optString("title"),
                    subtitle = pageObj.optString("subtitle")
                )
            }
        }
        val tagsArray = obj.optJSONArray("tags")
        val tags = mutableListOf<String>()
        if (tagsArray != null) {
            for (index in 0 until tagsArray.length()) {
                val tag = tagsArray.optString(index).trim()
                if (tag.isNotBlank()) {
                    tags += tag
                }
            }
        }

        return LovePlan(
            id = obj.optString("id"),
            name = obj.optString("name"),
            title = obj.optString("title"),
            subtitle = obj.optString("subtitle"),
            effectTypes = types.take(MAX_EFFECT_COUNT),
            pageTexts = pageTexts.take(MAX_EFFECT_COUNT),
            themeKey = obj.optString("themeKey").ifBlank { null },
            coverKey = obj.optString("coverKey").ifBlank { null },
            tags = tags,
            songKey = obj.optString("songKey").ifBlank { null },
            createdAt = obj.optLong("createdAt"),
            lastOpenedAt = obj.optLong("lastOpenedAt"),
            playCount = obj.optInt("playCount")
        )
    }
}
