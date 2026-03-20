package com.loveai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.loveai.model.Effect
import com.loveai.model.EffectType
import com.loveai.model.LovePlan
import com.loveai.repository.EffectRepository
import com.loveai.repository.FavoriteRepository

class LoveViewModel(application: Application) : AndroidViewModel(application) {

    private val effectRepository = EffectRepository()
    private val favoriteRepository = FavoriteRepository(application)
    private var activePlan: LovePlan? = null

    private val _effects = MutableLiveData<List<Effect>>()
    val effects: LiveData<List<Effect>> = _effects

    private val _currentIndex = MutableLiveData<Int>()
    val currentIndex: LiveData<Int> = _currentIndex

    private val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> = _isPlaying

    private val _favoriteCount = MutableLiveData<Int>()
    val favoriteCount: LiveData<Int> = _favoriteCount

    private val _favoriteEffects = MutableLiveData<List<Effect>>()
    val favoriteEffects: LiveData<List<Effect>> = _favoriteEffects

    private var autoSlideInterval: Long = 10_000L

    init {
        _currentIndex.value = 0
        _isPlaying.value = true
        _favoriteCount.value = 0
        generateRandomEffects()
        updateFavoriteCount()
    }

    fun generateRandomEffects() {
        activePlan = null
        val newEffects = effectRepository.getRandomEffects(8)
        val updatedEffects = newEffects.map { effect ->
            effect.copy(isFavorite = favoriteRepository.isFavorite(effect.variant.id))
        }
        _effects.value = updatedEffects
        _currentIndex.value = 0
    }

    fun loadPlan(plan: LovePlan) {
        activePlan = plan
        val newEffects = effectRepository.getEffectsByTypes(
            types = plan.effectTypes,
            titleOverride = plan.title,
            subtitleOverride = plan.subtitle
        )
        val updatedEffects = newEffects.map { effect ->
            effect.copy(isFavorite = favoriteRepository.isFavorite(effect.variant.id))
        }
        _effects.value = updatedEffects
        _currentIndex.value = 0
    }

    fun replayCurrentSequence() {
        activePlan?.let {
            loadPlan(it)
        } ?: generateRandomEffects()
    }

    fun getCurrentPlan(): LovePlan? = activePlan

    fun buildPreviewPlan(title: String, subtitle: String, effectTypes: List<EffectType>): LovePlan {
        return LovePlan(
            id = "preview",
            name = "\u9884\u89c8\u65b9\u6848",
            title = title,
            subtitle = subtitle,
            effectTypes = effectTypes.take(EffectType.values().size),
            createdAt = System.currentTimeMillis()
        )
    }

    fun nextEffect() {
        val current = _currentIndex.value ?: 0
        val total = _effects.value?.size ?: 0
        if (total > 0) {
            _currentIndex.value = (current + 1) % total
        }
    }

    fun previousEffect() {
        val current = _currentIndex.value ?: 0
        val total = _effects.value?.size ?: 0
        if (total > 0) {
            _currentIndex.value = if (current == 0) total - 1 else current - 1
        }
    }

    fun setCurrentIndex(index: Int) {
        val total = _effects.value?.size ?: 0
        if (index in 0 until total) {
            _currentIndex.value = index
        }
    }

    fun togglePlayPause() {
        _isPlaying.value = !(_isPlaying.value ?: true)
    }

    fun startAutoSlide() {
        _isPlaying.value = true
    }

    fun stopAutoSlide() {
        _isPlaying.value = false
    }

    fun toggleFavoriteByVariantId(variantId: Int) {
        val isFav = favoriteRepository.toggleFavorite(variantId)
        _effects.value = _effects.value?.map { effect ->
            if (effect.variant.id == variantId) {
                effect.copy(isFavorite = isFav)
            } else {
                effect
            }
        }
        updateFavoriteCount()
    }

    fun toggleCurrentFavorite() {
        val current = _currentIndex.value ?: 0
        val effects = _effects.value ?: return
        if (current in effects.indices) {
            toggleFavoriteByVariantId(effects[current].variant.id)
        }
    }

    fun getCurrentEffect(): Effect? {
        val current = _currentIndex.value ?: 0
        val effects = _effects.value ?: return null
        return if (current in effects.indices) effects[current] else null
    }

    private fun updateFavoriteCount() {
        _favoriteCount.value = favoriteRepository.getFavoriteCount()
    }

    fun loadFavorites() {
        _favoriteEffects.value = favoriteRepository.getFavoriteEffects()
    }

    fun setAutoSlideInterval(intervalMs: Long) {
        autoSlideInterval = intervalMs
    }

    fun getAutoSlideInterval(): Long = autoSlideInterval
}
