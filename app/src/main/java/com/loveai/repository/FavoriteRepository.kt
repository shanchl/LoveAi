package com.loveai.repository

import android.content.Context
import android.content.SharedPreferences
import com.loveai.model.Effect

/**
 * 收藏数据仓库
 * 使用 SharedPreferences 本地存储
 * 收藏的是 variantId，可以从 EffectVariants 重建完整效果
 */
class FavoriteRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("loveai_favorites", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FAVORITE_VARIANT_IDS = "favorite_variant_ids"
    }

    /**
     * 获取所有收藏的 variantId 列表
     */
    fun getFavoriteVariantIds(): Set<Int> {
        return prefs.getStringSet(KEY_FAVORITE_VARIANT_IDS, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: emptySet()
    }

    /**
     * 添加收藏（通过 variantId）
     */
    fun addFavorite(variantId: Int) {
        val favorites = getFavoriteVariantIds().toMutableSet()
        favorites.add(variantId)
        prefs.edit().putStringSet(
            KEY_FAVORITE_VARIANT_IDS,
            favorites.map { it.toString() }.toSet()
        ).apply()
    }

    /**
     * 取消收藏（通过 variantId）
     */
    fun removeFavorite(variantId: Int) {
        val favorites = getFavoriteVariantIds().toMutableSet()
        favorites.remove(variantId)
        prefs.edit().putStringSet(
            KEY_FAVORITE_VARIANT_IDS,
            favorites.map { it.toString() }.toSet()
        ).apply()
    }

    /**
     * 判断指定 variantId 是否已收藏
     */
    fun isFavorite(variantId: Int): Boolean {
        return getFavoriteVariantIds().contains(variantId)
    }

    /**
     * 切换收藏状态（通过 variantId）
     * @return 切换后的收藏状态（true=已收藏）
     */
    fun toggleFavorite(variantId: Int): Boolean {
        return if (isFavorite(variantId)) {
            removeFavorite(variantId)
            false
        } else {
            addFavorite(variantId)
            true
        }
    }

    /**
     * 获取收藏的效果列表（完整数据）
     * 从保存的 variantId 重建 Effect 对象
     */
    fun getFavoriteEffects(): List<Effect> {
        val favoriteVariantIds = getFavoriteVariantIds()
        return favoriteVariantIds.mapNotNull { variantId ->
            val variant = EffectVariants.getVariantById(variantId) ?: return@mapNotNull null
            Effect(
                id = "fav_${variant.id}",
                variant = variant,
                isFavorite = true
            )
        }
    }

    /**
     * 获取收藏数量
     */
    fun getFavoriteCount(): Int {
        return getFavoriteVariantIds().size
    }
}
