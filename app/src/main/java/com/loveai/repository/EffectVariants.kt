package com.loveai.repository

import android.graphics.Color
import com.loveai.model.EffectType
import com.loveai.model.EffectVariant
import com.loveai.model.LoveMessages

/**
 * 效果变体工厂
 * 定义所有效果变体的具体配置，每种类型至少6个变体
 */
object EffectVariants {

    // 预定义颜色常量
    private val PINK = Color.parseColor("#FF69B4")
    private val HOT_PINK = Color.parseColor("#FF1493")
    private val DEEP_PINK = Color.parseColor("#FF0080")
    private val ROSE = Color.parseColor("#FF007F")
    private val RED = Color.parseColor("#FF0000")
    private val CRIMSON = Color.parseColor("#DC143C")
    private val CORAL = Color.parseColor("#FF7F50")
    private val ORANGE = Color.parseColor("#FFA500")
    private val GOLD = Color.parseColor("#FFD700")
    private val YELLOW = Color.parseColor("#FFFF00")
    private val PURPLE = Color.parseColor("#800080")
    private val VIOLET = Color.parseColor("#EE82EE")
    private val LAVENDER = Color.parseColor("#E6E6FA")
    private val MAGENTA = Color.parseColor("#FF00FF")
    private val BLUE = Color.parseColor("#0000FF")
    private val SKY_BLUE = Color.parseColor("#87CEEB")
    private val DEEP_BLUE = Color.parseColor("#00008B")
    private val NAVY = Color.parseColor("#000080")
    private val TEAL = Color.parseColor("#008080")
    private val CYAN = Color.parseColor("#00FFFF")
    private val GREEN = Color.parseColor("#00FF00")
    private val EMERALD = Color.parseColor("#50C878")
    private val WHITE = Color.parseColor("#FFFFFF")
    private val CREAM = Color.parseColor("#FFFDD0")
    private val PEACH = Color.parseColor("#FFCBA4")
    private val MINT = Color.parseColor("#98FB98")
    private val LILAC = Color.parseColor("#C8A2C8")
    private val SALMON = Color.parseColor("#FA8072")
    private val CHERRY = Color.parseColor("#DE3163")
    private val AMBER = Color.parseColor("#FFBF00")
    private val TURQUOISE = Color.parseColor("#40E0D0")
    private val SOFT_PINK = Color.parseColor("#FFB6C1")

    // 背景渐变色
    private val BG_PINK_GRADIENT = Color.parseColor("#1A0A0A1A")
    private val BG_DARK = Color.parseColor("#0D0D0D")
    private val BG_NIGHT = Color.parseColor("#0A0A1A")
    private val BG_SUNSET = Color.parseColor("#1A0A0A0A")
    private val BG_ROMANTIC = Color.parseColor("#1A1A0A0A")

    private var nextId = 0
    private fun id(): Int = nextId++

    /**
     * 获取所有效果变体
     * 注意：使用懒加载缓存，确保 ID 稳定
     */
    private val allVariantsCache: List<EffectVariant> by lazy {
        buildList {
            nextId = 0
            addAll(getHeartRainVariants())
            addAll(getFireworkVariants())
            addAll(getStarrySkyVariants())
            addAll(getPetalFallVariants())
            addAll(getBubbleFloatVariants())
            addAll(getTypewriterVariants())
            addAll(getHeartPulseVariants())
            addAll(getRippleVariants())
            addAll(getSnowFallVariants())
            addAll(getMeteorShowerVariants())
            addAll(getButterflyVariants())
            addAll(getAuroraVariants())
        }
    }

    fun getAllVariants(): List<EffectVariant> = allVariantsCache

    /**
     * 心形粒子飘落效果 - 10个变体
     */
    private fun getHeartRainVariants(): List<EffectVariant> {
        val m = LoveMessages.heartRainMessages
        return listOf(
            EffectVariant(id(), EffectType.HEART_RAIN, "粉红浪漫心雨", PINK, HOT_PINK, BG_PINK_GRADIENT, 1.0f, 60, m[0].first, m[0].second),
            EffectVariant(id(), EffectType.HEART_RAIN, "红色热恋心雨", RED, CRIMSON, BG_DARK, 1.2f, 50, m[1].first, m[1].second),
            EffectVariant(id(), EffectType.HEART_RAIN, "紫色梦幻心雨", PURPLE, VIOLET, BG_NIGHT, 0.8f, 70, m[2].first, m[2].second),
            EffectVariant(id(), EffectType.HEART_RAIN, "金色温暖心雨", GOLD, ORANGE, BG_SUNSET, 1.0f, 55, m[3].first, m[3].second),
            EffectVariant(id(), EffectType.HEART_RAIN, "彩虹心雨", MAGENTA, CYAN, BG_ROMANTIC, 0.9f, 80, m[4].first, m[4].second),
            EffectVariant(id(), EffectType.HEART_RAIN, "玫瑰心雨", ROSE, PINK, BG_PINK_GRADIENT, 1.1f, 45, m[5].first, m[5].second),
            EffectVariant(id(), EffectType.HEART_RAIN, "珊瑚心雨", CORAL, PEACH, BG_SUNSET, 0.85f, 65, m[6].first, m[6].second),
            EffectVariant(id(), EffectType.HEART_RAIN, "薰衣草心雨", LAVENDER, LILAC, BG_NIGHT, 0.95f, 75, m[7].first, m[7].second),
            EffectVariant(id(), EffectType.HEART_RAIN, "薄荷心雨", MINT, GREEN, BG_ROMANTIC, 1.15f, 50, m[8].first, m[8].second),
            EffectVariant(id(), EffectType.HEART_RAIN, "钻石心雨", WHITE, CREAM, BG_DARK, 1.0f, 40, m[9].first, m[9].second)
        )
    }

    /**
     * 烟花绽放效果 - 10个变体
     */
    private fun getFireworkVariants(): List<EffectVariant> {
        val m = LoveMessages.fireworkMessages
        return listOf(
            EffectVariant(id(), EffectType.FIREWORK, "彩虹烟花", MAGENTA, CYAN, BG_NIGHT, 1.0f, 6, m[0].first, m[0].second),
            EffectVariant(id(), EffectType.FIREWORK, "金色喷泉", GOLD, YELLOW, BG_DARK, 1.2f, 5, m[1].first, m[1].second),
            EffectVariant(id(), EffectType.FIREWORK, "爱心烟花", PINK, RED, BG_ROMANTIC, 0.9f, 7, m[2].first, m[2].second),
            EffectVariant(id(), EffectType.FIREWORK, "星光爆炸", WHITE, SKY_BLUE, BG_NIGHT, 1.1f, 8, m[3].first, m[3].second),
            EffectVariant(id(), EffectType.FIREWORK, "玫瑰烟花", ROSE, CORAL, BG_SUNSET, 1.0f, 6, m[4].first, m[4].second),
            EffectVariant(id(), EffectType.FIREWORK, "紫色烟火", PURPLE, VIOLET, BG_NIGHT, 0.85f, 5, m[5].first, m[5].second),
            EffectVariant(id(), EffectType.FIREWORK, "蓝色流星雨烟花", BLUE, CYAN, BG_NIGHT, 1.15f, 7, m[6].first, m[6].second),
            EffectVariant(id(), EffectType.FIREWORK, "橙红火树", ORANGE, RED, BG_DARK, 1.0f, 6, m[7].first, m[7].second),
            EffectVariant(id(), EffectType.FIREWORK, "绿野仙踪烟花", EMERALD, TURQUOISE, BG_NIGHT, 0.95f, 6, m[8].first, m[8].second),
            EffectVariant(id(), EffectType.FIREWORK, "樱桃烟花", CHERRY, SOFT_PINK, BG_DARK, 1.05f, 7, m[9].first, m[9].second)
        )
    }

    /**
     * 星空流星效果 - 10个变体
     */
    private fun getStarrySkyVariants(): List<EffectVariant> {
        val m = LoveMessages.starrySkyMessages
        return listOf(
            EffectVariant(id(), EffectType.STARRY_SKY, "银河漫步", WHITE, SKY_BLUE, BG_NIGHT, 1.0f, 200, m[0].first, m[0].second),
            EffectVariant(id(), EffectType.STARRY_SKY, "流星雨", WHITE, YELLOW, BG_DARK, 1.5f, 150, m[1].first, m[1].second),
            EffectVariant(id(), EffectType.STARRY_SKY, "北极光", GREEN, CYAN, BG_NIGHT, 0.8f, 180, m[2].first, m[2].second),
            EffectVariant(id(), EffectType.STARRY_SKY, "紫色星云", PURPLE, MAGENTA, BG_NIGHT, 1.0f, 160, m[3].first, m[3].second),
            EffectVariant(id(), EffectType.STARRY_SKY, "粉红星空", PINK, LAVENDER, BG_ROMANTIC, 0.9f, 140, m[4].first, m[4].second),
            EffectVariant(id(), EffectType.STARRY_SKY, "金色星河", GOLD, ORANGE, BG_DARK, 1.1f, 170, m[5].first, m[5].second),
            EffectVariant(id(), EffectType.STARRY_SKY, "深蓝宇宙", DEEP_BLUE, NAVY, BG_NIGHT, 1.0f, 190, m[6].first, m[6].second),
            EffectVariant(id(), EffectType.STARRY_SKY, "彩虹星夜", MAGENTA, CYAN, BG_NIGHT, 0.85f, 130, m[7].first, m[7].second),
            EffectVariant(id(), EffectType.STARRY_SKY, "青碧星辰", TURQUOISE, EMERALD, BG_NIGHT, 0.95f, 175, m[8].first, m[8].second),
            EffectVariant(id(), EffectType.STARRY_SKY, "暖阳星空", AMBER, PEACH, BG_SUNSET, 1.05f, 145, m[9].first, m[9].second)
        )
    }

    /**
     * 花瓣飘散效果 - 10个变体
     */
    private fun getPetalFallVariants(): List<EffectVariant> {
        val m = LoveMessages.petalFallMessages
        return listOf(
            EffectVariant(id(), EffectType.PETAL_FALL, "樱花飞舞", PINK, WHITE, BG_PINK_GRADIENT, 1.0f, 40, m[0].first, m[0].second),
            EffectVariant(id(), EffectType.PETAL_FALL, "玫瑰花瓣", RED, PINK, BG_ROMANTIC, 0.9f, 35, m[1].first, m[1].second),
            EffectVariant(id(), EffectType.PETAL_FALL, "银杏落叶", GOLD, ORANGE, BG_SUNSET, 1.1f, 45, m[2].first, m[2].second),
            EffectVariant(id(), EffectType.PETAL_FALL, "紫色花海", PURPLE, LILAC, BG_NIGHT, 0.85f, 38, m[3].first, m[3].second),
            EffectVariant(id(), EffectType.PETAL_FALL, "桃花飘零", CORAL, PEACH, BG_PINK_GRADIENT, 1.0f, 42, m[4].first, m[4].second),
            EffectVariant(id(), EffectType.PETAL_FALL, "白色花瓣", WHITE, CREAM, BG_ROMANTIC, 0.95f, 50, m[5].first, m[5].second),
            EffectVariant(id(), EffectType.PETAL_FALL, "红色枫叶", CRIMSON, ORANGE, BG_SUNSET, 1.15f, 36, m[6].first, m[6].second),
            EffectVariant(id(), EffectType.PETAL_FALL, "彩虹花瓣", MAGENTA, MINT, BG_ROMANTIC, 1.0f, 48, m[7].first, m[7].second),
            EffectVariant(id(), EffectType.PETAL_FALL, "薰衣草花田", LAVENDER, SOFT_PINK, BG_NIGHT, 0.9f, 44, m[8].first, m[8].second),
            EffectVariant(id(), EffectType.PETAL_FALL, "碧绿荷叶", EMERALD, MINT, BG_NIGHT, 1.0f, 37, m[9].first, m[9].second)
        )
    }

    /**
     * 泡泡上浮效果 - 8个变体
     */
    private fun getBubbleFloatVariants(): List<EffectVariant> {
        val m = LoveMessages.bubbleMessages
        return listOf(
            EffectVariant(id(), EffectType.BUBBLE_FLOAT, "彩虹泡泡", MAGENTA, CYAN, BG_ROMANTIC, 1.0f, 25, m[0].first, m[0].second),
            EffectVariant(id(), EffectType.BUBBLE_FLOAT, "金色气泡", GOLD, YELLOW, BG_SUNSET, 0.9f, 20, m[1].first, m[1].second),
            EffectVariant(id(), EffectType.BUBBLE_FLOAT, "爱心泡泡", PINK, RED, BG_PINK_GRADIENT, 1.1f, 22, m[2].first, m[2].second),
            EffectVariant(id(), EffectType.BUBBLE_FLOAT, "透明泡泡", WHITE, SKY_BLUE, BG_NIGHT, 1.0f, 28, m[3].first, m[3].second),
            EffectVariant(id(), EffectType.BUBBLE_FLOAT, "紫色梦幻泡泡", PURPLE, VIOLET, BG_NIGHT, 0.85f, 24, m[4].first, m[4].second),
            EffectVariant(id(), EffectType.BUBBLE_FLOAT, "粉色泡泡", HOT_PINK, LAVENDER, BG_ROMANTIC, 1.05f, 26, m[5].first, m[5].second),
            EffectVariant(id(), EffectType.BUBBLE_FLOAT, "青碧泡泡", TURQUOISE, EMERALD, BG_NIGHT, 0.95f, 23, m[6].first, m[6].second),
            EffectVariant(id(), EffectType.BUBBLE_FLOAT, "珊瑚泡泡", CORAL, SALMON, BG_SUNSET, 1.0f, 21, m[7].first, m[7].second)
        )
    }

    /**
     * 打字机效果 - 8个变体
     */
    private fun getTypewriterVariants(): List<EffectVariant> {
        val m = LoveMessages.typewriterMessages
        return listOf(
            EffectVariant(id(), EffectType.TYPEWRITER, "经典打字", WHITE, CREAM, BG_DARK, 1.0f, 0, m[0].first, m[0].second),
            EffectVariant(id(), EffectType.TYPEWRITER, "霓虹闪烁", MAGENTA, CYAN, BG_NIGHT, 1.2f, 0, m[1].first, m[1].second),
            EffectVariant(id(), EffectType.TYPEWRITER, "粉色浪漫", PINK, WHITE, BG_ROMANTIC, 0.9f, 0, m[2].first, m[2].second),
            EffectVariant(id(), EffectType.TYPEWRITER, "金色光芒", GOLD, ORANGE, BG_DARK, 1.0f, 0, m[3].first, m[3].second),
            EffectVariant(id(), EffectType.TYPEWRITER, "紫色优雅", PURPLE, LAVENDER, BG_NIGHT, 1.1f, 0, m[4].first, m[4].second),
            EffectVariant(id(), EffectType.TYPEWRITER, "薄荷清新", MINT, TURQUOISE, BG_NIGHT, 0.95f, 0, m[5].first, m[5].second),
            EffectVariant(id(), EffectType.TYPEWRITER, "珊瑚暖阳", CORAL, PEACH, BG_SUNSET, 1.0f, 0, m[6].first, m[6].second),
            EffectVariant(id(), EffectType.TYPEWRITER, "深蓝诗意", DEEP_BLUE, SKY_BLUE, BG_NIGHT, 1.05f, 0, m[7].first, m[7].second)
        )
    }

    /**
     * 爱心脉冲效果 - 6个变体
     */
    private fun getHeartPulseVariants(): List<EffectVariant> {
        val m = LoveMessages.heartPulseMessages
        return listOf(
            EffectVariant(id(), EffectType.HEART_PULSE, "粉色心跳", PINK, HOT_PINK, BG_ROMANTIC, 1.0f, 1, m[0].first, m[0].second),
            EffectVariant(id(), EffectType.HEART_PULSE, "金色光环", GOLD, YELLOW, BG_DARK, 1.2f, 1, m[1].first, m[1].second),
            EffectVariant(id(), EffectType.HEART_PULSE, "彩虹脉冲", MAGENTA, CYAN, BG_NIGHT, 0.9f, 1, m[2].first, m[2].second),
            EffectVariant(id(), EffectType.HEART_PULSE, "玫瑰脉冲", ROSE, SOFT_PINK, BG_PINK_GRADIENT, 1.05f, 1, m[3].first, m[3].second),
            EffectVariant(id(), EffectType.HEART_PULSE, "紫色心跳", PURPLE, VIOLET, BG_NIGHT, 0.95f, 1, m[4].first, m[4].second),
            EffectVariant(id(), EffectType.HEART_PULSE, "青碧心跳", TURQUOISE, EMERALD, BG_NIGHT, 1.1f, 1, m[5].first, m[5].second)
        )
    }

    /**
     * 水波纹扩散效果 - 6个变体
     */
    private fun getRippleVariants(): List<EffectVariant> {
        val m = LoveMessages.rippleMessages
        return listOf(
            EffectVariant(id(), EffectType.RIPPLE, "涟漪扩散", SKY_BLUE, WHITE, BG_NIGHT, 1.0f, 3, m[0].first, m[0].second),
            EffectVariant(id(), EffectType.RIPPLE, "心形波纹", PINK, MAGENTA, BG_ROMANTIC, 0.9f, 4, m[1].first, m[1].second),
            EffectVariant(id(), EffectType.RIPPLE, "金色涟漪", GOLD, AMBER, BG_DARK, 1.1f, 3, m[2].first, m[2].second),
            EffectVariant(id(), EffectType.RIPPLE, "紫色涟漪", PURPLE, LAVENDER, BG_NIGHT, 0.85f, 4, m[3].first, m[3].second),
            EffectVariant(id(), EffectType.RIPPLE, "青碧波纹", TURQUOISE, CYAN, BG_NIGHT, 1.0f, 3, m[4].first, m[4].second),
            EffectVariant(id(), EffectType.RIPPLE, "珊瑚波纹", CORAL, PEACH, BG_SUNSET, 0.95f, 4, m[5].first, m[5].second)
        )
    }

    /**
     * 根据变体ID获取变体配置
     */
    fun getVariantById(id: Int): EffectVariant? {
        return allVariantsCache.find { it.id == id }
    }

    /**
     * 随机获取指定数量的变体
     * 核心策略：保证同类型不重复，且优先选满所有类型
     * 重要：每种特效类型只允许出现一次！
     */
    fun getRandomVariants(count: Int): List<EffectVariant> {
        val allVariants = getAllVariants()
        // 按 baseType 分组
        val grouped = allVariants.groupBy { it.baseType }
        val typeKeys = grouped.keys.toList()

        val result = mutableListOf<EffectVariant>()
        val usedTypes = mutableSetOf<EffectType>() // 记录已使用的类型，确保不重复

        // 第一轮：每个类型随机选一个，确保类型不重复
        val shuffledTypes = typeKeys.shuffled()
        for (type in shuffledTypes) {
            if (result.size >= count) break
            // 跳过已使用的类型（双重保险）
            if (type in usedTypes) continue
            
            val variantsOfType = grouped[type] ?: continue
            val selectedVariant = variantsOfType.random()
            
            result.add(selectedVariant)
            usedTypes.add(type) // 标记该类型已使用
        }

        // 如果还需要更多（count > 12），从剩余变体中补（但排除已选类型）
        if (result.size < count) {
            val selectedVariantIds = result.map { it.id }.toSet()
            // 排除已选变体和已选类型的变体
            val remaining = allVariants.filter { variant -> 
                variant.id !in selectedVariantIds && variant.baseType !in usedTypes
            }.shuffled().take(count - result.size)
            result.addAll(remaining)
        }

        return result
    }

    /**
     * 飘雪效果 - 8个变体
     */
    private fun getSnowFallVariants(): List<EffectVariant> {
        val m = LoveMessages.snowFallMessages
        return listOf(
            EffectVariant(id(), EffectType.SNOW_FALL, "冬日纯白雪", WHITE, SKY_BLUE, BG_NIGHT, 0.9f, 60, m[0].first, m[0].second),
            EffectVariant(id(), EffectType.SNOW_FALL, "粉雪飘零", SOFT_PINK, WHITE, BG_ROMANTIC, 0.8f, 50, m[1].first, m[1].second),
            EffectVariant(id(), EffectType.SNOW_FALL, "蓝色冰雪", SKY_BLUE, CYAN, BG_NIGHT, 1.0f, 70, m[2].first, m[2].second),
            EffectVariant(id(), EffectType.SNOW_FALL, "金色雪晶", GOLD, CREAM, BG_DARK, 0.85f, 55, m[3].first, m[3].second),
            EffectVariant(id(), EffectType.SNOW_FALL, "紫色雪夜", LAVENDER, VIOLET, BG_NIGHT, 0.95f, 65, m[4].first, m[4].second),
            EffectVariant(id(), EffectType.SNOW_FALL, "薄荷雪原", MINT, TURQUOISE, BG_NIGHT, 1.1f, 45, m[5].first, m[5].second),
            EffectVariant(id(), EffectType.SNOW_FALL, "樱花雪", PINK, WHITE, BG_PINK_GRADIENT, 0.9f, 60, m[6].first, m[6].second),
            EffectVariant(id(), EffectType.SNOW_FALL, "深蓝极寒雪", DEEP_BLUE, WHITE, BG_NIGHT, 1.05f, 75, m[7].first, m[7].second)
        )
    }

    /**
     * 流星雨效果 - 8个变体
     */
    private fun getMeteorShowerVariants(): List<EffectVariant> {
        val m = LoveMessages.meteorShowerMessages
        return listOf(
            EffectVariant(id(), EffectType.METEOR_SHOWER, "白色流星雨", WHITE, SKY_BLUE, BG_NIGHT, 1.0f, 40, m[0].first, m[0].second),
            EffectVariant(id(), EffectType.METEOR_SHOWER, "金色陨石雨", GOLD, AMBER, BG_DARK, 1.3f, 35, m[1].first, m[1].second),
            EffectVariant(id(), EffectType.METEOR_SHOWER, "浪漫粉流星", SOFT_PINK, PINK, BG_ROMANTIC, 0.9f, 30, m[2].first, m[2].second),
            EffectVariant(id(), EffectType.METEOR_SHOWER, "星河流星", CYAN, TURQUOISE, BG_NIGHT, 1.1f, 45, m[3].first, m[3].second),
            EffectVariant(id(), EffectType.METEOR_SHOWER, "紫色流星", VIOLET, LAVENDER, BG_NIGHT, 1.0f, 38, m[4].first, m[4].second),
            EffectVariant(id(), EffectType.METEOR_SHOWER, "橙红流星", ORANGE, CORAL, BG_SUNSET, 1.2f, 32, m[5].first, m[5].second),
            EffectVariant(id(), EffectType.METEOR_SHOWER, "蓝白流星", DEEP_BLUE, WHITE, BG_NIGHT, 0.85f, 42, m[6].first, m[6].second),
            EffectVariant(id(), EffectType.METEOR_SHOWER, "彩色流星", MAGENTA, CYAN, BG_NIGHT, 1.15f, 36, m[7].first, m[7].second)
        )
    }

    /**
     * 蝴蝶飞舞效果 - 8个变体
     */
    private fun getButterflyVariants(): List<EffectVariant> {
        val m = LoveMessages.butterflyMessages
        return listOf(
            EffectVariant(id(), EffectType.BUTTERFLY, "粉蝶飞舞", PINK, HOT_PINK, BG_PINK_GRADIENT, 1.0f, 40, m[0].first, m[0].second),
            EffectVariant(id(), EffectType.BUTTERFLY, "彩蝶翩翩", MAGENTA, CYAN, BG_ROMANTIC, 0.9f, 48, m[1].first, m[1].second),
            EffectVariant(id(), EffectType.BUTTERFLY, "金蝶飞翔", GOLD, ORANGE, BG_SUNSET, 1.1f, 36, m[2].first, m[2].second),
            EffectVariant(id(), EffectType.BUTTERFLY, "蓝蝴蝶", SKY_BLUE, CYAN, BG_NIGHT, 0.85f, 44, m[3].first, m[3].second),
            EffectVariant(id(), EffectType.BUTTERFLY, "紫蝶恋花", PURPLE, LAVENDER, BG_NIGHT, 1.05f, 40, m[4].first, m[4].second),
            EffectVariant(id(), EffectType.BUTTERFLY, "翠蝶飞舞", EMERALD, MINT, BG_NIGHT, 0.95f, 38, m[5].first, m[5].second),
            EffectVariant(id(), EffectType.BUTTERFLY, "玫瑰蝴蝶", ROSE, SOFT_PINK, BG_ROMANTIC, 1.0f, 42, m[6].first, m[6].second),
            EffectVariant(id(), EffectType.BUTTERFLY, "白蝴蝶花园", WHITE, CREAM, BG_DARK, 0.9f, 35, m[7].first, m[7].second)
        )
    }

    /**
     * 极光效果 - 8个变体
     */
    private fun getAuroraVariants(): List<EffectVariant> {
        val m = LoveMessages.auroraMessages
        return listOf(
            EffectVariant(id(), EffectType.AURORA, "绿色极光", EMERALD, CYAN, BG_NIGHT, 1.0f, 6, m[0].first, m[0].second),
            EffectVariant(id(), EffectType.AURORA, "紫色极光", PURPLE, VIOLET, BG_NIGHT, 0.9f, 6, m[1].first, m[1].second),
            EffectVariant(id(), EffectType.AURORA, "粉红极光", PINK, MAGENTA, BG_ROMANTIC, 1.1f, 6, m[2].first, m[2].second),
            EffectVariant(id(), EffectType.AURORA, "蓝碧极光", SKY_BLUE, TURQUOISE, BG_NIGHT, 0.85f, 6, m[3].first, m[3].second),
            EffectVariant(id(), EffectType.AURORA, "金橙极光", GOLD, ORANGE, BG_DARK, 1.05f, 6, m[4].first, m[4].second),
            EffectVariant(id(), EffectType.AURORA, "彩虹极光", MAGENTA, GREEN, BG_NIGHT, 0.95f, 6, m[5].first, m[5].second),
            EffectVariant(id(), EffectType.AURORA, "深紫极光", DEEP_BLUE, VIOLET, BG_NIGHT, 1.0f, 6, m[6].first, m[6].second),
            EffectVariant(id(), EffectType.AURORA, "薄荷极光", MINT, TEAL, BG_NIGHT, 1.15f, 6, m[7].first, m[7].second)
        )
    }
}
