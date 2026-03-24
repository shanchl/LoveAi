package com.loveai.model

enum class VideoAspectPreset(
    val key: String,
    val label: String,
    val width: Int,
    val height: Int
) {
    PORTRAIT(
        key = "portrait_9_16",
        label = "9:16 \u7ad6\u5c4f",
        width = 1080,
        height = 1920
    ),
    SQUARE(
        key = "square_1_1",
        label = "1:1 \u65b9\u56fe",
        width = 1080,
        height = 1080
    ),
    LANDSCAPE(
        key = "landscape_16_9",
        label = "16:9 \u6a2a\u5c4f",
        width = 1920,
        height = 1080
    );

    companion object {
        fun fromKey(key: String?): VideoAspectPreset? = values().firstOrNull { it.key == key }
    }
}
