package com.loveai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.loveai.model.Effect
import com.loveai.repository.EffectRepository
import com.loveai.repository.FavoriteRepository

/**
 * LoveAI 主 ViewModel
 * 管理效果展示、轮播、收藏等业务逻辑
 */
class LoveViewModel(application: Application) : AndroidViewModel(application) {

    private val effectRepository = EffectRepository()
    private val favoriteRepository = FavoriteRepository(application)

    // 当前展示的效果列表
    private val _effects = MutableLiveData<List<Effect>>()
    val effects: LiveData<List<Effect>> = _effects

    // 当前展示的效果索引
    private val _currentIndex = MutableLiveData<Int>()
    val currentIndex: LiveData<Int> = _currentIndex

    // 是否正在自动播放
    private val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> = _isPlaying

    // 收藏数量
    private val _favoriteCount = MutableLiveData<Int>()
    val favoriteCount: LiveData<Int> = _favoriteCount

    // 收藏列表
    private val _favoriteEffects = MutableLiveData<List<Effect>>()
    val favoriteEffects: LiveData<List<Effect>> = _favoriteEffects

    // 自动轮播间隔（毫秒）- 至少8秒，确保每个页面有足够停留时间
    private var autoSlideInterval: Long = 10000L // 10秒，满足不少于8秒的要求

    init {
        _currentIndex.value = 0
        _isPlaying.value = true
        _favoriteCount.value = 0
        generateRandomEffects()
        updateFavoriteCount()
    }

    /**
     * 生成随机效果（固定8个）
     */
    fun generateRandomEffects() {
        val count = 8
        val newEffects = effectRepository.getRandomEffects(count)
        // 恢复收藏状态
        val updatedEffects = newEffects.map { effect ->
            effect.copy(isFavorite = favoriteRepository.isFavorite(effect.variant.id))
        }
        _effects.value = updatedEffects
        _currentIndex.value = 0
    }

    /**
     * 跳转到下一个效果
     */
    fun nextEffect() {
        val current = _currentIndex.value ?: 0
        val total = _effects.value?.size ?: 0
        if (total > 0) {
            _currentIndex.value = (current + 1) % total
        }
    }

    /**
     * 跳转到上一个效果
     */
    fun previousEffect() {
        val current = _currentIndex.value ?: 0
        val total = _effects.value?.size ?: 0
        if (total > 0) {
            _currentIndex.value = if (current == 0) total - 1 else current - 1
        }
    }

    /**
     * 跳转到指定索引
     */
    fun setCurrentIndex(index: Int) {
        val total = _effects.value?.size ?: 0
        if (index in 0 until total) {
            _currentIndex.value = index
        }
    }

    /**
     * 切换播放/暂停
     */
    fun togglePlayPause() {
        _isPlaying.value = !(_isPlaying.value ?: true)
    }

    /**
     * 开始自动播放
     */
    fun startAutoSlide() {
        _isPlaying.value = true
    }

    /**
     * 停止自动播放
     */
    fun stopAutoSlide() {
        _isPlaying.value = false
    }

    /**
     * 切换收藏状态（通过 variantId）
     */
    fun toggleFavoriteByVariantId(variantId: Int) {
        val isFav = favoriteRepository.toggleFavorite(variantId)
        // 更新效果列表中的收藏状态
        _effects.value = _effects.value?.map { effect ->
            if (effect.variant.id == variantId) {
                effect.copy(isFavorite = isFav)
            } else {
                effect
            }
        }
        updateFavoriteCount()
    }

    /**
     * 收藏当前效果
     */
    fun toggleCurrentFavorite() {
        val current = _currentIndex.value ?: 0
        val effects = _effects.value ?: return
        if (current in effects.indices) {
            toggleFavoriteByVariantId(effects[current].variant.id)
        }
    }

    /**
     * 获取当前效果
     */
    fun getCurrentEffect(): Effect? {
        val current = _currentIndex.value ?: 0
        val effects = _effects.value ?: return null
        return if (current in effects.indices) effects[current] else null
    }

    /**
     * 更新收藏数量
     */
    private fun updateFavoriteCount() {
        _favoriteCount.value = favoriteRepository.getFavoriteCount()
    }

    /**
     * 加载收藏列表
     */
    fun loadFavorites() {
        _favoriteEffects.value = favoriteRepository.getFavoriteEffects()
    }

    /**
     * 设置自动轮播间隔
     */
    fun setAutoSlideInterval(intervalMs: Long) {
        autoSlideInterval = intervalMs
    }

    /**
     * 获取自动轮播间隔
     */
    fun getAutoSlideInterval(): Long = autoSlideInterval
}
