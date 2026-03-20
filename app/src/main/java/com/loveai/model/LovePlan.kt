package com.loveai.model

data class LovePlan(
    val id: String,
    val name: String,
    val title: String,
    val subtitle: String,
    val effectTypes: List<EffectType>,
    val createdAt: Long
)
