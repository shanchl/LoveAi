package com.loveai.model

data class PlanVersion(
    val planId: String,
    val version: Int,
    val name: String,
    val title: String,
    val subtitle: String,
    val effectTypes: List<EffectType>,
    val pageTexts: List<PlanPageText> = emptyList(),
    val themeKey: String? = null,
    val coverKey: String? = null,
    val tags: List<String> = emptyList(),
    val songKey: String? = null,
    val status: PlanStatus = PlanStatus.DRAFT,
    val savedAt: Long,
    val note: String
) {
    fun toPlan(base: LovePlan): LovePlan {
        return base.copy(
            name = name,
            title = title,
            subtitle = subtitle,
            effectTypes = effectTypes,
            pageTexts = pageTexts,
            themeKey = themeKey,
            coverKey = coverKey,
            tags = tags,
            songKey = songKey,
            status = status
        )
    }
}
