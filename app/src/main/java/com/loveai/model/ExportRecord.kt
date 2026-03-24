package com.loveai.model

data class ExportRecord(
    val id: String,
    val planId: String,
    val planName: String,
    val exportType: ExportType,
    val outputPath: String,
    val createdAt: Long
)

enum class ExportType(
    val key: String,
    val label: String
) {
    POSTER(
        key = "poster",
        label = "\u6d77\u62a5\u5bfc\u51fa"
    ),
    SHARE_COVER(
        key = "share_cover",
        label = "\u5c01\u9762\u5206\u4eab"
    );

    companion object {
        fun fromKey(key: String?): ExportType? = values().firstOrNull { it.key == key }
    }
}
