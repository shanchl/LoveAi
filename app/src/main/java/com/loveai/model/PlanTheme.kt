package com.loveai.model

enum class PlanTheme(
    val key: String,
    val label: String,
    val description: String,
    val defaultPlanName: String,
    val defaultTitle: String,
    val defaultSubtitle: String,
    val recommendedEffects: List<EffectType>
) {
    CONFESSION(
        key = "confession",
        label = "\u544a\u767d",
        description = "\u9002\u5408\u521d\u6b21\u8868\u8fbe\u5fc3\u610f",
        defaultPlanName = "\u544a\u767d\u65b9\u6848",
        defaultTitle = "\u60f3\u628a\u5fc3\u91cc\u8bdd\u8bf4\u7ed9\u4f60",
        defaultSubtitle = "\u8fd9\u4e00\u8f6e\u6d6a\u6f2b\uff0c\u53ea\u4e3a\u4f60\u51c6\u5907",
        recommendedEffects = listOf(
            EffectType.HEART_RAIN,
            EffectType.HEART_PULSE,
            EffectType.TYPEWRITER,
            EffectType.FIREWORK,
            EffectType.BUBBLE_FLOAT,
            EffectType.BUTTERFLY,
            EffectType.STARRY_SKY,
            EffectType.AURORA
        )
    ),
    ANNIVERSARY(
        key = "anniversary",
        label = "\u7eaa\u5ff5\u65e5",
        description = "\u9002\u5408\u56de\u987e\u966a\u4f34\u548c\u6210\u957f",
        defaultPlanName = "\u7eaa\u5ff5\u65e5\u65b9\u6848",
        defaultTitle = "\u8fd9\u4e00\u8def\uff0c\u8c22\u8c22\u4f60\u90fd\u5728",
        defaultSubtitle = "\u628a\u6211\u4eec\u7684\u65f6\u5149\uff0c\u518d\u6e29\u67d4\u5730\u770b\u4e00\u904d",
        recommendedEffects = listOf(
            EffectType.PETAL_FALL,
            EffectType.RIPPLE,
            EffectType.STARRY_SKY,
            EffectType.HEART_RAIN,
            EffectType.BUTTERFLY,
            EffectType.SNOW_FALL,
            EffectType.METEOR_SHOWER,
            EffectType.AURORA
        )
    ),
    BIRTHDAY(
        key = "birthday",
        label = "\u751f\u65e5",
        description = "\u9002\u5408\u660e\u4eae\u70ed\u95f9\u7684\u795d\u798f\u573a\u666f",
        defaultPlanName = "\u751f\u65e5\u60ca\u559c",
        defaultTitle = "\u4eca\u5929\u7684\u5149\uff0c\u90fd\u4e3a\u4f60\u800c\u4eae",
        defaultSubtitle = "\u613f\u4f60\u7684\u65b0\u4e00\u5c81\uff0c\u4ecd\u7136\u88ab\u7231\u548c\u60ca\u559c\u5305\u56f4",
        recommendedEffects = listOf(
            EffectType.FIREWORK,
            EffectType.BUBBLE_FLOAT,
            EffectType.BUTTERFLY,
            EffectType.HEART_PULSE,
            EffectType.HEART_RAIN,
            EffectType.PETAL_FALL,
            EffectType.STARRY_SKY,
            EffectType.TYPEWRITER
        )
    ),
    LONG_DISTANCE(
        key = "long_distance",
        label = "\u5f02\u5730\u601d\u5ff5",
        description = "\u9002\u5408\u542b\u84c4\u3001\u6162\u70ed\u7684\u60f3\u5ff5\u611f",
        defaultPlanName = "\u60f3\u5ff5\u65b9\u6848",
        defaultTitle = "\u865a\u62df\u7684\u8ddd\u79bb\uff0c\u6321\u4e0d\u4f4f\u771f\u5b9e\u7684\u5fc3\u52a8",
        defaultSubtitle = "\u613f\u8fd9\u4e00\u6bb5\u5149\u5f71\uff0c\u66ff\u6211\u5148\u62b1\u4f60\u4e00\u4e0b",
        recommendedEffects = listOf(
            EffectType.METEOR_SHOWER,
            EffectType.AURORA,
            EffectType.STARRY_SKY,
            EffectType.SNOW_FALL,
            EffectType.RIPPLE,
            EffectType.TYPEWRITER,
            EffectType.HEART_PULSE,
            EffectType.BUBBLE_FLOAT
        )
    );

    companion object {
        fun fromKey(key: String?): PlanTheme? = values().firstOrNull { it.key == key }
    }
}
