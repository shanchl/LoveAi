package com.loveai.ui.effects

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import com.loveai.model.Effect
import kotlin.math.*
import kotlin.random.Random

/**
 * 效果1：心形粒子飘落
 * 支持多种变体配置（颜色、速度、数量等）
 */
class HeartRainEffect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseEffectView(context, attrs) {

    private data class Heart(
        var x: Float,
        var y: Float,
        var size: Float,
        var speed: Float,
        var alpha: Int,
        var rotation: Float,
        var rotSpeed: Float,
        var color: Int,
        var swingOffset: Float,
        var swingAmplitude: Float
    )

    private val hearts = mutableListOf<Heart>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var frameCount = 0
    private val heartPath = Path()
    private val bgPaint = Paint()
    private var textAlpha = 0f       // 文字淡入透明度
    private var textOffsetY = 30f    // 文字上浮偏移量

    override fun onEffectBound(effect: Effect) {
        textPaint.apply {
            color = Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            textAlign = Paint.Align.CENTER
            setShadowLayer(6f, 0f, 2f, textGlowColor())
        }
        subTextPaint.apply {
            color = adjustAlpha(primaryColor, 0xFF)
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // 初始化心形粒子
        hearts.clear()
        val count = particleCount.coerceIn(20, 100)
        repeat(count) {
            hearts.add(createHeart(w, h, true))
        }
        
        // 设置背景渐变 - 使用变体的主色调
        val gradientColors = intArrayOf(
            adjustAlpha(primaryColor, 0x1A),
            adjustAlpha(secondaryColor, 0x1A),
            backgroundColor
        )
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            gradientColors,
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    private fun createHeart(w: Int, h: Int, randomY: Boolean = false): Heart {
        val size = Random.nextFloat() * 50f + 15f
        val speed = (Random.nextFloat() * 2.5f + 0.8f) * animationSpeed
        val alpha = Random.nextInt(120, 255)
        
        // 基于主色调生成变体颜色
        val r = Color.red(primaryColor)
        val g = Color.green(primaryColor)
        val b = Color.blue(primaryColor)
        val color = Color.argb(
            alpha,
            (r + Random.nextInt(-30, 30)).coerceIn(0, 255),
            (g + Random.nextInt(-30, 30)).coerceIn(0, 255),
            (b + Random.nextInt(-30, 30)).coerceIn(0, 255)
        )
        
        return Heart(
            x = Random.nextFloat() * w,
            y = if (randomY) Random.nextFloat() * h else -size * 2,
            size = size,
            speed = speed,
            alpha = alpha,
            rotation = Random.nextFloat() * 360f,
            rotSpeed = (Random.nextFloat() - 0.5f) * 2f * animationSpeed,
            color = color,
            swingOffset = Random.nextFloat() * Math.PI.toFloat() * 2,
            swingAmplitude = Random.nextFloat() * 30f + 10f
        )
    }

    private fun buildHeartPath(cx: Float, cy: Float, size: Float): Path {
        val path = Path()
        // 心形贝塞尔曲线
        path.moveTo(cx, cy + size * 0.3f)
        path.cubicTo(
            cx - size * 1.0f, cy - size * 0.2f,
            cx - size * 1.0f, cy - size * 0.8f,
            cx, cy - size * 0.4f
        )
        path.cubicTo(
            cx + size * 1.0f, cy - size * 0.8f,
            cx + size * 1.0f, cy - size * 0.2f,
            cx, cy + size * 0.3f
        )
        path.close()
        return path
    }

    override fun onDrawEffect(canvas: Canvas) {
        // 绘制背景
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 绘制心形粒子
        for (heart in hearts) {
            paint.color = heart.color
            paint.alpha = heart.alpha
            canvas.save()
            canvas.translate(heart.x, heart.y)
            canvas.rotate(heart.rotation)
            canvas.drawPath(buildHeartPath(0f, 0f, heart.size), paint)
            canvas.restore()
        }

        // 绘制爱意文字（带对比度保护 + 淡入上浮动画）
        if (message.isNotEmpty()) {
            val centerX = width / 2f
            val centerY = height * 0.65f + textOffsetY

            // 淡入上浮动画：前60帧从透明+下移 渐变到 完全显示
            textPaint.alpha = (textAlpha * 255).toInt()
            subTextPaint.alpha = (textAlpha * 255).toInt()

            drawContrastText(canvas, message, centerX, centerY, textPaint, ContrastTextType.MAIN)

            if (subMessage.isNotEmpty()) {
                drawContrastText(canvas, subMessage, centerX, centerY + 48f, subTextPaint, ContrastTextType.SUB)
            }

            // 恢复 paint 的 alpha
            textPaint.alpha = 255
            subTextPaint.alpha = 255
        }
    }

    override fun onUpdateAnimation() {
        frameCount++
        val w = width
        val h = height
        if (w == 0 || h == 0) return

        // 淡入上浮动画
        if (frameCount < 90) {
            val progress = (frameCount / 90f).coerceIn(0f, 1f)
            // easeOutCubic 缓动
            val eased = 1f - (1f - progress) * (1f - progress) * (1f - progress)
            textAlpha = eased
            textOffsetY = 30f * (1f - eased)
        } else {
            textAlpha = 1f
            textOffsetY = 0f
        }

        val targetCount = particleCount.coerceIn(20, 100)
        val toRemove = mutableListOf<Heart>()
        
        for (heart in hearts) {
            heart.y += heart.speed
            heart.x += sin((frameCount * 0.03f + heart.swingOffset).toDouble()).toFloat() * heart.swingAmplitude * 0.05f
            heart.rotation += heart.rotSpeed
            if (heart.y > h + heart.size * 2) {
                toRemove.add(heart)
            }
        }
        hearts.removeAll(toRemove)

        // 补充新粒子
        while (hearts.size < targetCount) {
            hearts.add(createHeart(w, h, false))
        }
    }
}
