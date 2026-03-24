package com.loveai.model

enum class GenerationTone(
    val key: String,
    val label: String,
    val description: String
) {
    SWEET(
        key = "sweet",
        label = "\u751c\u871c",
        description = "\u8f7b\u677e\u3001\u53ef\u7231\u3001\u9760\u8fd1\u751f\u6d3b"
    ),
    CINEMATIC(
        key = "cinematic",
        label = "\u7535\u5f71\u611f",
        description = "\u66f4\u5f3a\u7684\u753b\u9762\u611f\u548c\u4eea\u5f0f\u611f"
    ),
    GENTLE(
        key = "gentle",
        label = "\u6e29\u67d4",
        description = "\u5b89\u9759\u3001\u7ec6\u817b\u3001\u6162\u6162\u8bf4"
    ),
    PASSIONATE(
        key = "passionate",
        label = "\u70ed\u70c8",
        description = "\u60c5\u7eea\u66f4\u6d53\u3001\u66f4\u9002\u5408\u9ad8\u80fd\u573a\u666f"
    );

    companion object {
        fun fromKey(key: String?): GenerationTone? = values().firstOrNull { it.key == key }
    }
}
