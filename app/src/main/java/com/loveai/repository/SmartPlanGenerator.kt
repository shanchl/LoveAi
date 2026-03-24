package com.loveai.repository

import com.loveai.model.EffectType
import com.loveai.model.GeneratedPlanDraft
import com.loveai.model.GenerationTone
import com.loveai.model.PlanCover
import com.loveai.model.PlanPageText
import com.loveai.model.PlanTheme

class SmartPlanGenerator {

    fun generate(
        theme: PlanTheme?,
        tone: GenerationTone,
        keywords: String,
        relation: String,
        pageCount: Int
    ): GeneratedPlanDraft {
        val normalizedKeywords = keywords
            .split(' ', ',', '\uff0c', '\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        val themeLabel = theme?.label ?: "\u81ea\u5b9a\u4e49"
        val relationLabel = relation.ifBlank { "\u4f60" }
        val keywordLead = normalizedKeywords.take(2).joinToString("\u3001")

        val planName = when (tone) {
            GenerationTone.SWEET -> "${themeLabel}\u751c\u871c\u65b9\u6848"
            GenerationTone.CINEMATIC -> "${themeLabel}\u753b\u9762\u5f0f\u65b9\u6848"
            GenerationTone.GENTLE -> "${themeLabel}\u6e29\u67d4\u7248"
            GenerationTone.PASSIONATE -> "${themeLabel}\u9ad8\u80fd\u7248"
        }

        val title = when (tone) {
            GenerationTone.SWEET -> if (keywordLead.isBlank()) "\u60f3\u628a\u5fc3\u91cc\u7684\u751c\u90fd\u7559\u7ed9$relationLabel" else "$keywordLead\u90fd\u60f3\u548c$relationLabel\u4e00\u8d77"
            GenerationTone.CINEMATIC -> if (keywordLead.isBlank()) "\u8fd9\u4e00\u573a\u6d6a\u6f2b\uff0c\u4e3b\u89d2\u662f$relationLabel" else "\u4ece$keywordLead\u5f00\u59cb\uff0c\u628a\u955c\u5934\u90fd\u7ed9$relationLabel"
            GenerationTone.GENTLE -> if (keywordLead.isBlank()) "\u60f3\u628a\u6e29\u67d4\u6162\u6162\u8bf4\u7ed9$relationLabel" else "$keywordLead\u7684\u65e5\u5b50\uff0c\u6700\u60f3\u548c$relationLabel\u5206\u4eab"
            GenerationTone.PASSIONATE -> if (keywordLead.isBlank()) "\u6240\u6709\u70ed\u70c8\u7684\u60c5\u7eea\uff0c\u90fd\u60f3\u7ed9$relationLabel" else "$keywordLead\u90fd\u6bd4\u4e0d\u4e0a\u6211\u5bf9$relationLabel\u7684\u5fc3\u52a8"
        }

        val subtitle = when (tone) {
            GenerationTone.SWEET -> "\u7528 ${themeLabel.lowercase()} \u7684\u65b9\u5f0f\uff0c\u628a\u5c0f\u5fc3\u601d\u53d8\u6210\u4e00\u5957\u4e13\u5c5e\u52a8\u6001"
            GenerationTone.CINEMATIC -> "\u8ba9\u6bcf\u4e00\u9875\u90fd\u50cf\u9884\u544a\u7247\u4e00\u6837\uff0c\u4e3a$relationLabel\u63a8\u8fdb\u60c5\u7eea"
            GenerationTone.GENTLE -> "\u4e0d\u7528\u592a\u54cd\u4eae\uff0c\u4e5f\u80fd\u628a\u60f3\u5ff5\u8bf4\u5f97\u5f88\u6df1"
            GenerationTone.PASSIONATE -> "\u4ece\u5149\u5f71\u3001\u6587\u5b57\u5230\u8282\u594f\uff0c\u90fd\u6309\u9ad8\u80fd\u6c14\u8d28\u53bb\u63a8"
        }

        val baseEffects = (theme?.recommendedEffects ?: defaultEffectsByTone(tone))
            .shuffled()
            .take(pageCount.coerceIn(5, 8))

        val pageTexts = baseEffects.mapIndexed { index, type ->
            buildPageText(
                position = index + 1,
                type = type,
                tone = tone,
                relation = relationLabel,
                keywords = normalizedKeywords
            )
        }

        val cover = when (theme) {
            PlanTheme.CONFESSION -> PlanCover.BLUSH
            PlanTheme.ANNIVERSARY -> PlanCover.BLOOM
            PlanTheme.BIRTHDAY -> PlanCover.SUNSET
            PlanTheme.LONG_DISTANCE -> PlanCover.OCEAN
            null -> when (tone) {
                GenerationTone.SWEET -> PlanCover.BLUSH
                GenerationTone.CINEMATIC -> PlanCover.STARDUST
                GenerationTone.GENTLE -> PlanCover.OCEAN
                GenerationTone.PASSIONATE -> PlanCover.SUNSET
            }
        }

        return GeneratedPlanDraft(
            name = planName,
            title = title,
            subtitle = subtitle,
            themeKey = theme?.key,
            coverKey = cover.key,
            tags = (listOf(themeLabel, tone.label) + normalizedKeywords).distinct().take(6),
            effectTypes = baseEffects,
            pageTexts = pageTexts
        )
    }

    private fun buildPageText(
        position: Int,
        type: EffectType,
        tone: GenerationTone,
        relation: String,
        keywords: List<String>
    ): PlanPageText {
        val keyword = keywords.getOrNull((position - 1) % keywords.size.coerceAtLeast(1)) ?: ""
        val title = when (tone) {
            GenerationTone.SWEET -> when (type) {
                EffectType.HEART_RAIN -> "\u60f3\u628a\u5fc3\u52a8\u90fd\u4e0b\u7ed9$relation"
                EffectType.FIREWORK -> if (keyword.isBlank()) "\u70ed\u95f9\u8981\u4e3a$relation\u7ed9\u6ee1" else "$keyword\u7684\u591c\u665a\u4e5f\u8981\u4e3a$relation\u53d1\u4eae"
                else -> if (keyword.isBlank()) "\u7b2c $position \u9875\uff0c\u7ee7\u7eed\u559c\u6b22$relation" else "\u7b2c $position \u9875\uff0c\u60f3\u548c$relation\u53bb$keyword"
            }
            GenerationTone.CINEMATIC -> if (keyword.isBlank()) "\u955c\u5934\u63a8\u8fdb\u7684\u65b9\u5411\uff0c\u4e00\u76f4\u662f$relation" else "$keyword\u662f\u8fd9\u4e00\u5e55\u9001\u7ed9$relation\u7684\u5f15\u5b50"
            GenerationTone.GENTLE -> if (keyword.isBlank()) "\u6162\u6162\u8bf4\uff0c\u6211\u5f88\u5728\u4e4e$relation" else "$keyword\u8fd9\u4ef6\u5c0f\u4e8b\uff0c\u603b\u4f1a\u8ba9\u6211\u60f3\u5230$relation"
            GenerationTone.PASSIONATE -> if (keyword.isBlank()) "\u8fd9\u4e00\u9875\u7684\u60c5\u7eea\uff0c\u8981\u5168\u90e8\u7ed9$relation" else "$keyword\u90fd\u4e0d\u53ca\u6211\u5bf9$relation\u7684\u51b2\u52a8"
        }
        val subtitle = when (tone) {
            GenerationTone.SWEET -> "\u8ba9\u6d6a\u6f2b\u518d\u505c\u7559\u4e00\u4f1a\uff0c\u611f\u89c9\u5c31\u4f1a\u66f4\u751c\u4e00\u70b9"
            GenerationTone.CINEMATIC -> "\u8fd9\u4e2a\u8f6c\u573a\u4e0d\u662f\u4e3a\u4e86\u70ab\uff0c\u800c\u662f\u4e3a\u4e86\u628a\u60c5\u7eea\u63a8\u9ad8"
            GenerationTone.GENTLE -> "\u65e0\u9700\u592a\u7528\u529b\uff0c\u53ea\u8981\u7ec6\u7ec6\u5730\u843d\u5728\u5fc3\u91cc\u5c31\u597d"
            GenerationTone.PASSIONATE -> "\u80fd\u91cf\u53ef\u4ee5\u5f88\u9ad8\uff0c\u4f46\u4e3b\u89d2\u59cb\u7ec8\u53ea\u6709$relation"
        }
        return PlanPageText(title = title, subtitle = subtitle)
    }

    private fun defaultEffectsByTone(tone: GenerationTone): List<EffectType> {
        return when (tone) {
            GenerationTone.SWEET -> listOf(
                EffectType.HEART_RAIN,
                EffectType.BUBBLE_FLOAT,
                EffectType.BUTTERFLY,
                EffectType.HEART_PULSE,
                EffectType.TYPEWRITER,
                EffectType.PETAL_FALL,
                EffectType.STARRY_SKY,
                EffectType.FIREWORK
            )
            GenerationTone.CINEMATIC -> listOf(
                EffectType.FIREWORK,
                EffectType.METEOR_SHOWER,
                EffectType.AURORA,
                EffectType.STARRY_SKY,
                EffectType.RIPPLE,
                EffectType.TYPEWRITER,
                EffectType.HEART_RAIN,
                EffectType.BUTTERFLY
            )
            GenerationTone.GENTLE -> listOf(
                EffectType.RIPPLE,
                EffectType.SNOW_FALL,
                EffectType.AURORA,
                EffectType.STARRY_SKY,
                EffectType.TYPEWRITER,
                EffectType.PETAL_FALL,
                EffectType.BUBBLE_FLOAT,
                EffectType.HEART_PULSE
            )
            GenerationTone.PASSIONATE -> listOf(
                EffectType.FIREWORK,
                EffectType.HEART_PULSE,
                EffectType.METEOR_SHOWER,
                EffectType.HEART_RAIN,
                EffectType.RIPPLE,
                EffectType.BUTTERFLY,
                EffectType.AURORA,
                EffectType.STARRY_SKY
            )
        }
    }
}
