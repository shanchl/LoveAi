package com.loveai.ui

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * 页面切换动画管理器
 * 柔和的交叉淡入淡出效果
 */
object PageTransitionManager {

    fun applyRandomTransition(viewPager: ViewPager2) {
        applyTransition(viewPager)
    }

    fun applyTransition(viewPager: ViewPager2) {
        viewPager.setPageTransformer { page, position ->
            val absPos = abs(position)

            page.apply {
                // 抵消默认滑动，让页面重叠
                translationX = -position * width

                if (absPos >= 1f) {
                    // 完全离屏的页面隐藏
                    alpha = 0f
                    scaleX = 0.95f
                    scaleY = 0.95f
                } else {
                    // 柔和透明度变化
                    alpha = 0.4f + (1f - absPos) * 0.6f

                    // 轻微缩放
                    scaleX = 0.95f + (1f - absPos) * 0.05f
                    scaleY = 0.95f + (1f - absPos) * 0.05f
                }
            }
        }
    }
}