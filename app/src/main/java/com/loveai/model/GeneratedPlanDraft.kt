package com.loveai.model

data class GeneratedPlanDraft(
    val name: String,
    val title: String,
    val subtitle: String,
    val themeKey: String?,
    val coverKey: String?,
    val tags: List<String>,
    val effectTypes: List<EffectType>,
    val pageTexts: List<PlanPageText>
)
