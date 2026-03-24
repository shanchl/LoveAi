package com.loveai.model

enum class PlanStatus(
    val key: String,
    val label: String
) {
    DRAFT(
        key = "draft",
        label = "\u8349\u7a3f"
    ),
    PUBLISHED(
        key = "published",
        label = "\u5df2\u53d1\u5e03"
    );

    companion object {
        fun fromKey(key: String?): PlanStatus? = values().firstOrNull { it.key == key }
    }
}
