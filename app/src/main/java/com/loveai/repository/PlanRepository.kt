package com.loveai.repository

import android.content.Context
import android.content.SharedPreferences
import com.loveai.model.EffectType
import com.loveai.model.LovePlan
import com.loveai.model.PlanPageText
import com.loveai.model.PlanStatus
import com.loveai.model.PlanVersion
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class PlanRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("loveai_plans", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PLANS = "plans"
        private const val KEY_PLAN_VERSIONS = "plan_versions"
        private const val MAX_EFFECT_COUNT = 8
        private const val MAX_VERSION_COUNT = 12
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
        status: PlanStatus = PlanStatus.DRAFT,
        existingId: String? = null
    ): LovePlan {
        val plans = getAllPlans().toMutableList()
        val existingPlan = existingId?.let { id -> plans.firstOrNull { it.id == id } }
        val now = System.currentTimeMillis()
        val newVersion = (existingPlan?.currentVersion ?: 0) + 1
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
            createdAt = existingPlan?.createdAt ?: now,
            updatedAt = now,
            publishedAt = when {
                status == PlanStatus.PUBLISHED -> now
                else -> existingPlan?.publishedAt ?: 0L
            },
            lastOpenedAt = existingPlan?.lastOpenedAt ?: 0L,
            playCount = existingPlan?.playCount ?: 0,
            status = status,
            currentVersion = newVersion
        )

        val existingIndex = plans.indexOfFirst { it.id == plan.id }
        if (existingIndex >= 0) {
            plans[existingIndex] = plan
        } else {
            plans += plan
        }
        persistPlans(plans)
        persistVersion(plan)
        return plan
    }

    fun deletePlan(id: String) {
        val updated = getAllPlans().filterNot { it.id == id }
        persistPlans(updated)
        deleteVersions(id)
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
            songKey = original.songKey,
            status = PlanStatus.DRAFT
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
        status: PlanStatus? = null,
        sortMode: SortMode = SortMode.RECENT
    ): List<LovePlan> {
        val normalizedKeyword = keyword.trim()
        return getAllPlans()
            .asSequence()
            .filter { plan ->
                themeKey.isNullOrBlank() || plan.themeKey == themeKey
            }
            .filter { plan ->
                status == null || plan.status == status
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

    fun getPlanVersions(planId: String): List<PlanVersion> {
        val root = JSONObject(prefs.getString(KEY_PLAN_VERSIONS, "{}") ?: "{}")
        val array = root.optJSONArray(planId) ?: JSONArray()
        val versions = mutableListOf<PlanVersion>()
        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            parseVersion(obj)?.let { versions += it }
        }
        return versions.sortedByDescending { it.version }
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
                    put("updatedAt", plan.updatedAt)
                    put("publishedAt", plan.publishedAt)
                    put("lastOpenedAt", plan.lastOpenedAt)
                    put("playCount", plan.playCount)
                    put("status", plan.status.key)
                    put("currentVersion", plan.currentVersion)
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

    private fun persistVersion(plan: LovePlan) {
        val root = JSONObject(prefs.getString(KEY_PLAN_VERSIONS, "{}") ?: "{}")
        val existingArray = root.optJSONArray(plan.id) ?: JSONArray()
        val versions = mutableListOf<JSONObject>()
        for (index in 0 until existingArray.length()) {
            existingArray.optJSONObject(index)?.let { versions += it }
        }
        versions += JSONObject().apply {
            put("planId", plan.id)
            put("version", plan.currentVersion)
            put("name", plan.name)
            put("title", plan.title)
            put("subtitle", plan.subtitle)
            put("themeKey", plan.themeKey)
            put("coverKey", plan.coverKey)
            put("songKey", plan.songKey)
            put("status", plan.status.key)
            put("savedAt", plan.updatedAt)
            put("note", if (plan.status == PlanStatus.PUBLISHED) "\u53d1\u5e03\u7248" else "\u8349\u7a3f\u4fdd\u5b58")
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
        }

        val trimmed = versions
            .sortedByDescending { it.optInt("version") }
            .take(MAX_VERSION_COUNT)
            .sortedBy { it.optInt("version") }

        root.put(
            plan.id,
            JSONArray().apply {
                trimmed.forEach { put(it) }
            }
        )
        prefs.edit().putString(KEY_PLAN_VERSIONS, root.toString()).apply()
    }

    private fun deleteVersions(planId: String) {
        val root = JSONObject(prefs.getString(KEY_PLAN_VERSIONS, "{}") ?: "{}")
        if (!root.has(planId)) return
        root.remove(planId)
        prefs.edit().putString(KEY_PLAN_VERSIONS, root.toString()).apply()
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
            updatedAt = obj.optLong("updatedAt").takeIf { it > 0L } ?: obj.optLong("createdAt"),
            publishedAt = obj.optLong("publishedAt"),
            lastOpenedAt = obj.optLong("lastOpenedAt"),
            playCount = obj.optInt("playCount"),
            status = PlanStatus.fromKey(obj.optString("status")) ?: PlanStatus.DRAFT,
            currentVersion = obj.optInt("currentVersion").takeIf { it > 0 } ?: 1
        )
    }

    private fun parseVersion(obj: JSONObject): PlanVersion? {
        val planId = obj.optString("planId")
        if (planId.isBlank()) return null

        val effectTypesArray = obj.optJSONArray("effectTypes") ?: return null
        val effectTypes = mutableListOf<EffectType>()
        for (index in 0 until effectTypesArray.length()) {
            runCatching { EffectType.valueOf(effectTypesArray.optString(index)) }
                .getOrNull()
                ?.let { effectTypes += it }
        }
        if (effectTypes.isEmpty()) return null

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
                tagsArray.optString(index).trim().takeIf { it.isNotBlank() }?.let { tags += it }
            }
        }

        return PlanVersion(
            planId = planId,
            version = obj.optInt("version").takeIf { it > 0 } ?: 1,
            name = obj.optString("name"),
            title = obj.optString("title"),
            subtitle = obj.optString("subtitle"),
            effectTypes = effectTypes.take(MAX_EFFECT_COUNT),
            pageTexts = pageTexts.take(MAX_EFFECT_COUNT),
            themeKey = obj.optString("themeKey").ifBlank { null },
            coverKey = obj.optString("coverKey").ifBlank { null },
            tags = tags,
            songKey = obj.optString("songKey").ifBlank { null },
            status = PlanStatus.fromKey(obj.optString("status")) ?: PlanStatus.DRAFT,
            savedAt = obj.optLong("savedAt"),
            note = obj.optString("note").ifBlank { "\u7248\u672c\u5feb\u7167" }
        )
    }
}
