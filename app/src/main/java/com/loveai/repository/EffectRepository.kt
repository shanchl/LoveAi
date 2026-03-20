package com.loveai.repository

import com.loveai.model.Effect
import com.loveai.model.EffectType
import com.loveai.model.EffectVariant
import kotlin.random.Random

/**
 * 效果库仓库
 * 负责生成随机效果和管理效果池
 */
class EffectRepository {

    /**
     * 从效果池中随机选择3个效果
     */
    fun generateRandomEffects(): List<Effect> {
        // 随机选择3个变体
        val selectedVariants = EffectVariants.getRandomVariants(3)

        // 为每个变体创建效果实例
        return selectedVariants.map { variant ->
            val timestamp = System.currentTimeMillis()
            Effect(
                id = "effect_${variant.id}_${timestamp}_${Random.nextInt(10000)}",
                variant = variant,
                isFavorite = false
            )
        }
    }

    /**
     * 根据变体ID获取效果
     */
    fun getEffectByVariantId(variantId: Int): Effect? {
        val variant = EffectVariants.getVariantById(variantId) ?: return null
        return Effect(
            id = "effect_${variant.id}_${System.currentTimeMillis()}_${Random.nextInt(10000)}",
            variant = variant,
            isFavorite = false
        )
    }

    /**
     * 根据类型获取随机效果
     */
    fun getEffectByType(type: EffectType): Effect {
        val allVariants = EffectVariants.getAllVariants()
        val typeVariants = allVariants.filter { it.baseType == type }
        val variant = typeVariants.random()
        
        return Effect(
            id = "effect_${variant.id}_${System.currentTimeMillis()}_${Random.nextInt(10000)}",
            variant = variant,
            isFavorite = false
        )
    }

    /**
     * 获取所有效果类型
     */
    fun getAllEffectTypes(): List<EffectType> {
        return EffectType.values().toList()
    }

    /**
     * 获取所有效果变体
     */
    fun getAllVariants(): List<EffectVariant> {
        return EffectVariants.getAllVariants()
    }

    /**
     * 获取指定数量的随机效果
     */
    fun getRandomEffects(count: Int): List<Effect> {
        val selectedVariants = EffectVariants.getRandomVariants(count)
        val timestamp = System.currentTimeMillis()
        
        return selectedVariants.mapIndexed { index, variant ->
            Effect(
                id = "effect_${variant.id}_${timestamp}_${Random.nextInt(10000)}",
                variant = variant,
                isFavorite = false
            )
        }
    }

    fun getEffectsByTypes(
        types: List<EffectType>,
        titleOverride: String? = null,
        subtitleOverride: String? = null
    ): List<Effect> {
        val timestamp = System.currentTimeMillis()
        return types.map { type ->
            val baseEffect = getEffectByType(type)
            val variant = baseEffect.variant.copy(
                message = titleOverride?.takeIf { it.isNotBlank() } ?: baseEffect.variant.message,
                subMessage = subtitleOverride ?: baseEffect.variant.subMessage
            )
            Effect(
                id = "effect_${variant.id}_${timestamp}_${Random.nextInt(10000)}",
                variant = variant,
                isFavorite = false
            )
        }
    }
}
