package com.loveai.model

data class LovePlan(
    val id: String,
    val name: String,
    val title: String,
    val subtitle: String,
    val effectTypes: List<EffectType>,
    val pageTexts: List<PlanPageText> = emptyList(),
    val themeKey: String? = null,
    val coverKey: String? = null,
    val tags: List<String> = emptyList(),
    val songKey: String? = null,
    val createdAt: Long,
    val lastOpenedAt: Long = 0L,
    val playCount: Int = 0
)
