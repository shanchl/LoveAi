package com.loveai.model

enum class VideoEdgeStyle(
    val key: String,
    val label: String
) {
    CINEMATIC(
        key = "cinematic",
        label = "\u7535\u5f71\u5f0f"
    ),
    SOFT_GLOW(
        key = "soft_glow",
        label = "\u67d4\u5149"
    ),
    LETTER(
        key = "letter",
        label = "\u4fe1\u7eb8"
    );

    companion object {
        fun fromKey(key: String?): VideoEdgeStyle? = values().firstOrNull { it.key == key }

        fun next(currentKey: String?): VideoEdgeStyle {
            val current = fromKey(currentKey) ?: CINEMATIC
            return values()[(current.ordinal + 1) % values().size]
        }
    }
}
