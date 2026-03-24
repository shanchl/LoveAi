package com.loveai.model

data class VideoExportTask(
    val id: String,
    val planId: String,
    val planName: String,
    val aspectPresetKey: String,
    val status: VideoExportStatus,
    val outputPath: String? = null,
    val createdAt: Long,
    val finishedAt: Long = 0L,
    val note: String = ""
)

enum class VideoExportStatus(
    val key: String,
    val label: String
) {
    QUEUED(
        key = "queued",
        label = "\u961f\u5217\u4e2d"
    ),
    RUNNING(
        key = "running",
        label = "\u751f\u6210\u4e2d"
    ),
    COMPLETED(
        key = "completed",
        label = "\u5df2\u5b8c\u6210"
    ),
    FAILED(
        key = "failed",
        label = "\u5931\u8d25"
    );

    companion object {
        fun fromKey(key: String?): VideoExportStatus? = values().firstOrNull { it.key == key }
    }
}
