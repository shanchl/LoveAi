package com.loveai.ui.effects

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import com.loveai.model.Effect
import kotlin.math.*
import kotlin.random.Random

/**
 * 效果8：水波纹扩散效果
 * 支持多种变体配置（颜色、数量等）
 */
class RippleEffect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseEffectView(context, attrs) {

    private data class Ripple(
        var radius: Float,
        var alpha: Float,
        var strokeWidth: Float,
        var color: Int
    )

    private val ripples = mutableListOf<Ripple>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint()

    private var frameCount = 0
    private var centerX = 0f
    private var centerY = 0f
    private var textRevealCount = 0   // 逐字揭示数量
    private var subTextAlpha = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h * 0.5f
        bgPaint.shader = RadialGradient(
            centerX, centerY, max(w, h) * 0.8f,
            intArrayOf(
                backgroundColor,
                adjustAlpha(primaryColor, 0x1A),
                adjustAlpha(secondaryColor, 0x0D)
            ),
            null,
            Shader.TileMode.CLAMP
        )
    }

    override fun onEffectBound(effect: Effect) {
        frameCount = 0
        ripples.clear()
        textRevealCount = 0
        subTextAlpha = 0f
        textPaint.apply {
            color = Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(8f, 0f, 2f, textGlowColor())
        }
        subTextPaint.apply {
            color = adjustAlpha(secondaryColor, 0xFF)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.06f
        }
    }

    private fun createRipple() {
        val color = if (Random.nextBoolean()) primaryColor else secondaryColor
        ripples.add(
            Ripple(
                radius = 0f,
                alpha = 1f,
                strokeWidth = 4f,
                color = color
            )
        )
    }

    override fun onDrawEffect(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 绘制波纹
        for (ripple in ripples) {
            paint.color = ripple.color
            paint.alpha = (ripple.alpha * 180).toInt().coerceIn(0, 255)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = ripple.strokeWidth
            paint.maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)

            canvas.drawCircle(centerX, centerY, ripple.radius, paint)

            // 内圈
            if (ripple.radius > 50f) {
                paint.alpha = (ripple.alpha * 100).toInt().coerceIn(0, 255)
                canvas.drawCircle(centerX, centerY, ripple.radius * 0.6f, paint)
            }
        }
        paint.maskFilter = null

        // 中心爱心
        paint.color = primaryColor
        paint.style = Paint.Style.FILL
        paint.alpha = 200
        val heartSize = 35f
        val heartPath = Path()
        heartPath.moveTo(centerX, centerY + heartSize * 0.25f)
        heartPath.cubicTo(centerX - heartSize, centerY - heartSize * 0.15f, centerX - heartSize, centerY - heartSize * 0.7f, centerX, centerY - heartSize * 0.3f)
        heartPath.cubicTo(centerX + heartSize, centerY - heartSize * 0.7f, centerX + heartSize, centerY - heartSize * 0.15f, centerX, centerY + heartSize * 0.25f)
        heartPath.close()
        canvas.drawPath(heartPath, paint)

        // 波纹式逐字淡入效果
        if (message.isNotEmpty()) {
            val revealText = message.substring(0, textRevealCount.coerceAtMost(message.length))

            if (revealText.isNotEmpty()) {
                drawContrastText(canvas, revealText, centerX, height * 0.65f, textPaint, ContrastTextType.MAIN)
            }

            // 主标题显示完后，副标题淡入
            if (subMessage.isNotEmpty() && textRevealCount > message.length) {
                subTextPaint.alpha = (subTextAlpha * 255).toInt()
                drawContrastText(canvas, subMessage, centerX, height * 0.65f + 44f, subTextPaint, ContrastTextType.SUB)
                subTextPaint.alpha = 255
            }
        }
    }

    override fun onUpdateAnimation() {
        frameCount++

        // 逐字揭示动画（与波纹同步节奏）
        val revealSpeed = (8 / animationSpeed).toInt().coerceIn(3, 12)
        if (frameCount % revealSpeed == 0) {
            if (textRevealCount <= message.length) {
                textRevealCount++
            }
        }
        // 副标题淡入
        if (textRevealCount > message.length) {
            subTextAlpha = ((textRevealCount - message.length) / 12f).coerceIn(0f, 1f)
        }

        // 波纹创建间隔 - 使用粒子数量控制
        val rippleInterval = (35 / animationSpeed).toInt().coerceIn(20, 50)
        val maxRipples = particleCount.coerceIn(2, 6)
        
        if (frameCount % rippleInterval == 0 && ripples.size < maxRipples) {
            createRipple()
        }

        val maxRadius = max(width, height) * 0.7f
        val toRemove = mutableListOf<Ripple>()
        for (ripple in ripples) {
            ripple.radius += 3.5f * animationSpeed
            ripple.alpha -= 0.012f
            ripple.strokeWidth = 4f * ripple.alpha
            if (ripple.alpha <= 0 || ripple.radius > maxRadius) {
                toRemove.add(ripple)
            }
        }
        ripples.removeAll(toRemove)
    }
}
