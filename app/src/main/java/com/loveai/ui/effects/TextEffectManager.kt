package com.loveai.ui.effects

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.sin

/**
 * 文字展示效果管理器
 * 提供多种文字展示效果
 */
object TextEffectManager {

    // 文字展示效果类型
    enum class TextEffectType {
        HORIZONTAL,      // 水平居中（默认）
        VERTICAL_RL,     // 竖向（从右向左）
        VERTICAL_LR,     // 竖向（从左向右）
        SLANT,           // 倾斜
        WAVE,            // 波浪排列
        ARC,             // 弧形
        SCATTER,         // 散开
        SCALE_IN,        // 缩放入场
        FALL_DOWN        // 飘落
    }

    /**
     * 获取当前特效类型对应的随机文字效果
     * 根据特效类型选择适合的文字效果
     */
    fun getRandomEffectForType(effectType: String): TextEffectType {
        return when (effectType) {
            // 飘落类特效适合飘落效果
            "HEART_RAIN", "PETAL_FALL", "SNOW_FALL", "METEOR_SHOWER" -> 
                listOf(TextEffectType.HORIZONTAL, TextEffectType.FALL_DOWN, TextEffectType.WAVE).random()
            
            // 爆炸类特效适合缩放入场
            "FIREWORK", "HEART_PULSE", "RIPPLE" ->
                listOf(TextEffectType.HORIZONTAL, TextEffectType.SCALE_IN, TextEffectType.ARC).random()
            
            // 静态背景适合竖向或倾斜
            "STARRY_SKY", "AURORA" ->
                listOf(TextEffectType.HORIZONTAL, TextEffectType.VERTICAL_RL, TextEffectType.SLANT).random()
            
            // 泡泡类适合波浪
            "BUBBLE_FLOAT" ->
                listOf(TextEffectType.HORIZONTAL, TextEffectType.WAVE, TextEffectType.SCATTER).random()
            
            // 蝴蝶类适合飘落或波浪
            "BUTTERFLY" ->
                listOf(TextEffectType.HORIZONTAL, TextEffectType.FALL_DOWN, TextEffectType.WAVE).random()
            
            // 打字机已有自己的效果
            "TYPEWRITER" -> TextEffectType.HORIZONTAL
            
            else -> TextEffectType.values().random()
        }
    }

    /**
     * 绘制带有效果的文字
     * @param canvas 画布
     * @param text 文字内容
     * @param x 中心X坐标
     * @param y 中心Y坐标
     * @param paint 文字画笔
     * @param effectType 效果类型
     * @param progress 动画进度 (0-1)
     */
    fun drawTextWithEffect(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: Paint,
        effectType: TextEffectType,
        progress: Float
    ) {
        when (effectType) {
            TextEffectType.HORIZONTAL -> {
                // 默认水平居中
                canvas.drawText(text, x, y, paint)
            }
            TextEffectType.VERTICAL_RL -> {
                // 竖向文字（从右向左，每个字单独绘制）
                drawVerticalText(canvas, text, x, y, paint, true, progress)
            }
            TextEffectType.VERTICAL_LR -> {
                // 竖向文字（从左向右）
                drawVerticalText(canvas, text, x, y, paint, false, progress)
            }
            TextEffectType.SLANT -> {
                // 倾斜文字
                canvas.save()
                canvas.rotate(-15f, x, y)
                canvas.drawText(text, x, y, paint)
                canvas.restore()
            }
            TextEffectType.WAVE -> {
                // 波浪排列
                drawWaveText(canvas, text, x, y, paint, progress)
            }
            TextEffectType.ARC -> {
                // 弧形文字
                drawArcText(canvas, text, x, y, paint, progress)
            }
            TextEffectType.SCATTER -> {
                // 散开效果
                drawScatterText(canvas, text, x, y, paint, progress)
            }
            TextEffectType.SCALE_IN -> {
                // 缩放入场效果
                drawScaleInText(canvas, text, x, y, paint, progress)
            }
            TextEffectType.FALL_DOWN -> {
                // 飘落效果
                drawFallDownText(canvas, text, x, y, paint, progress)
            }
        }
    }

    // 绘制竖向文字
    private fun drawVerticalText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        centerY: Float,
        paint: Paint,
        rightToLeft: Boolean,
        progress: Float
    ) {
        val charList = text.toCharArray()
        val charHeight = paint.fontSpacing
        val totalHeight = charList.size * charHeight
        val startY = centerY - totalHeight / 2

        charList.forEachIndexed { index, char ->
            val delay = index * 0.1f
            val charProgress = (progress - delay).coerceIn(0f, 1f)
            
            if (charProgress > 0) {
                val x = if (rightToLeft) centerX + 20f else centerX - 20f
                val alpha = (charProgress * 255).toInt().coerceIn(0, 255)
                val y = startY + index * charHeight + charProgress * charHeight
                
                paint.alpha = alpha
                canvas.drawText(char.toString(), x, y, paint)
            }
        }
        paint.alpha = 255
    }

    // 绘制波浪文字
    private fun drawWaveText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        centerY: Float,
        paint: Paint,
        progress: Float
    ) {
        val charList = text.toCharArray()
        val charWidth = paint.measureText("中") // 估算宽度
        val totalWidth = charList.size * charWidth
        val startX = centerX - totalWidth / 2

        charList.forEachIndexed { index, char ->
            val delay = index * 0.08f
            val charProgress = (progress - delay).coerceIn(0f, 1f)
            
            if (charProgress > 0) {
                val waveOffset = sin(charProgress * Math.PI.toFloat() * 2) * 15f
                val x = startX + index * charWidth + charWidth / 2
                val y = centerY + waveOffset
                
                paint.alpha = (charProgress * 255).toInt().coerceIn(0, 255)
                canvas.drawText(char.toString(), x, y, paint)
            }
        }
        paint.alpha = 255
    }

    // 绘制弧形文字
    private fun drawArcText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        centerY: Float,
        paint: Paint,
        progress: Float
    ) {
        val charList = text.toCharArray()
        val radius = 150f
        val angleStep = 8f // 每字符的角度
        val totalAngle = (charList.size - 1) * angleStep
        val startAngle = -totalAngle / 2 - 90f // 从顶部开始

        charList.forEachIndexed { index, char ->
            val delay = index * 0.1f
            val charProgress = (progress - delay).coerceIn(0f, 1f)
            
            if (charProgress > 0) {
                val angle = Math.toRadians((startAngle + index * angleStep).toDouble())
                val x = centerX + radius * cos(angle).toFloat()
                val y = centerY + radius * sin(angle).toFloat()
                
                canvas.save()
                canvas.rotate(startAngle + index * angleStep + 90f, x, y)
                paint.alpha = (charProgress * 255).toInt().coerceIn(0, 255)
                canvas.drawText(char.toString(), x, y, paint)
                canvas.restore()
            }
        }
        paint.alpha = 255
    }

    // 绘制散开文字
    private fun drawScatterText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        centerY: Float,
        paint: Paint,
        progress: Float
    ) {
        val charList = text.toCharArray()
        val charWidth = paint.measureText("中")
        val totalWidth = charList.size * charWidth
        val startX = centerX - totalWidth / 2

        charList.forEachIndexed { index, char ->
            val delay = index * 0.05f
            val charProgress = (progress - delay).coerceIn(0f, 1f)
            
            if (charProgress > 0) {
                // 从中心向外散开
                val targetX = startX + index * charWidth
                val targetY = centerY
                val startXOffset = centerX
                val startYOffset = centerY
                
                val currentX = startXOffset + (targetX - startXOffset) * charProgress
                val currentY = startYOffset + (targetY - startYOffset) * charProgress
                
                // 添加随机偏移
                val randomOffset = (1 - charProgress) * 30f
                
                paint.alpha = (charProgress * 255).toInt().coerceIn(0, 255)
                canvas.drawText(char.toString(), currentX, currentY + randomOffset, paint)
            }
        }
        paint.alpha = 255
    }

    // 绘制缩放入场文字
    private fun drawScaleInText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: Paint,
        progress: Float
    ) {
        val scale = progress
        val alpha = (progress * 255).toInt().coerceIn(0, 255)
        
        canvas.save()
        canvas.scale(scale, scale, x, y)
        paint.alpha = alpha
        canvas.drawText(text, x, y, paint)
        canvas.restore()
    }

    // 绘制飘落文字
    private fun drawFallDownText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        centerY: Float,
        paint: Paint,
        progress: Float
    ) {
        val charList = text.toCharArray()
        val charWidth = paint.measureText("中")
        val totalWidth = charList.size * charWidth
        val startX = centerX - totalWidth / 2

        charList.forEachIndexed { index, char ->
            val delay = index * 0.08f
            val charProgress = (progress - delay).coerceIn(0f, 1f)
            
            if (charProgress > 0) {
                val x = startX + index * charWidth + charWidth / 2
                // 从上往下飘落，带摆动
                val startY = centerY - 100f
                val endY = centerY
                val currentY = startY + (endY - startY) * charProgress
                val swingX = sin(charProgress * Math.PI.toFloat() * 3) * 10f
                
                paint.alpha = (charProgress * 255).toInt().coerceIn(0, 255)
                canvas.drawText(char.toString(), x + swingX, currentY, paint)
            }
        }
        paint.alpha = 255
    }
}
