package com.loveai.model

enum class PlanCover(
    val key: String,
    val label: String,
    val description: String,
    val startColor: String,
    val endColor: String
) {
    BLUSH(
        key = "blush",
        label = "\u7c89\u91d1\u6668\u66e6",
        description = "\u6696\u7c89\u4e0e\u9999\u69df\u8272\u8c03\uff0c\u9002\u5408\u544a\u767d\u548c\u751f\u65e5",
        startColor = "#FF6F91",
        endColor = "#FF9671"
    ),
    STARDUST(
        key = "stardust",
        label = "\u661f\u5c18\u591c\u8272",
        description = "\u84dd\u7d2b\u591c\u5e55\uff0c\u9002\u5408\u7eaa\u5ff5\u4e0e\u60f3\u5ff5",
        startColor = "#5C4B8A",
        endColor = "#8C6BAE"
    ),
    SUNSET(
        key = "sunset",
        label = "\u843d\u65e5\u71c3\u7130",
        description = "\u91d1\u6a59\u4e0e\u665a\u971e\uff0c\u9002\u5408\u70ed\u70c8\u6c14\u6c1b",
        startColor = "#F9C74F",
        endColor = "#F3722C"
    ),
    OCEAN(
        key = "ocean",
        label = "\u6df1\u6d77\u6708\u5149",
        description = "\u84dd\u7eff\u6e10\u53d8\uff0c\u9002\u5408\u5b89\u9759\u548c\u6e29\u67d4",
        startColor = "#277DA1",
        endColor = "#577590"
    ),
    BLOOM(
        key = "bloom",
        label = "\u82b1\u5883\u7d2b\u971e",
        description = "\u73ab\u7470\u4e0e\u85b0\u8863\u8349\u8c03\uff0c\u9002\u5408\u82b1\u74e3\u4e0e\u8774\u8776\u7c7b\u6c14\u8d28",
        startColor = "#C06C84",
        endColor = "#6A67CE"
    );

    companion object {
        fun fromKey(key: String?): PlanCover? = values().firstOrNull { it.key == key }
    }
}
