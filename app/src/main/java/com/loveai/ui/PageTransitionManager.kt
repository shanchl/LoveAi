package com.loveai.ui

import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * 页面切换动画管理器。
 * 二期第二轮将切换效果调整成更明显的景深与压暗过渡。
 */
object PageTransitionManager {

    fun applyRandomTransition(viewPager: ViewPager2) {
        applyTransition(viewPager)
    }

    fun applyTransition(viewPager: ViewPager2) {
        viewPager.setPageTransformer { page, position ->
            val absPos = abs(position)

            page.apply {
                translationX = -position * width
                translationY = absPos * 26f
                pivotY = height * 0.5f
                cameraDistance = width * 12f

                if (absPos >= 1f) {
                    alpha = 0f
                    scaleX = 0.9f
                    scaleY = 0.9f
                    rotationY = position * 8f
                } else {
                    alpha = 0.28f + (1f - absPos) * 0.72f
                    scaleX = 0.9f + (1f - absPos) * 0.1f
                    scaleY = 0.92f + (1f - absPos) * 0.08f
                    rotationY = position * 5f
                }
            }
        }
    }
}
