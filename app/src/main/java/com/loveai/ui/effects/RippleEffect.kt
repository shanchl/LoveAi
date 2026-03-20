package com.loveai.ui.effects

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import com.loveai.model.Effect
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * 第三轮精修：涟漪页改成“情绪落入水面”的感觉，强调中心、回声和余波。
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

    private data class Spark(
        var angle: Float,
        var distance: Float,
        var alpha: Float
    )

    private val ripples = mutableListOf<Ripple>()
    private val sparks = mutableListOf<Spark>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var frameCount = 0
    private var centerX = 0f
    private var centerY = 0f
    private var textAlpha = 0f
    private var textOffsetY = 18f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h * 0.5f

        bgPaint.shader = RadialGradient(
            centerX,
            centerY,
            max(w, h) * 0.8f,
            intArrayOf(
                backgroundColor,
                adjustAlpha(primaryColor, 20),
                adjustAlpha(secondaryColor, 10)
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )

        sparks.clear()
        repeat(16) {
            sparks.add(
                Spark(
                    angle = Random.nextFloat() * (Math.PI * 2).toFloat(),
                    distance = Random.nextFloat() * 26f + 14f,
                    alpha = Random.nextFloat() * 0.8f + 0.2f
                )
            )
        }
    }

    override fun onEffectBound(effect: Effect) {
        frameCount = 0
        ripples.clear()
        textAlpha = 0f
        textOffsetY = 18f

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
        ripples.add(
            Ripple(
                radius = 0f,
                alpha = 1f,
                strokeWidth = 4.5f,
                color = if (Random.nextBoolean()) primaryColor else secondaryColor
            )
        )
    }

    private fun buildHeartPath(size: Float): Path {
        return Path().apply {
            moveTo(0f, size * 0.25f)
            cubicTo(-size, -size * 0.15f, -size, -size * 0.7f, 0f, -size * 0.3f)
            cubicTo(size, -size * 0.7f, size, -size * 0.15f, 0f, size * 0.25f)
            close()
        }
    }

    override fun onDrawEffect(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        ripples.forEach { ripple ->
            paint.color = ripple.color
            paint.alpha = (ripple.alpha * 180).toInt().coerceIn(0, 255)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = ripple.strokeWidth
            paint.maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)
            canvas.drawCircle(centerX, centerY, ripple.radius, paint)
            if (ripple.radius > 46f) {
                paint.alpha = (ripple.alpha * 100).toInt().coerceIn(0, 255)
                canvas.drawCircle(centerX, centerY, ripple.radius * 0.62f, paint)
            }
        }
        paint.maskFilter = null

        sparks.forEach { spark ->
            val pulse = sin(frameCount * 0.05f + spark.angle) * 0.5f + 0.5f
            val x = centerX + kotlin.math.cos(spark.angle) * (spark.distance + pulse * 8f)
            val y = centerY + kotlin.math.sin(spark.angle) * (spark.distance + pulse * 8f)
            glowPaint.color = adjustAlpha(primaryColor, (spark.alpha * 110).toInt())
            glowPaint.maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)
            canvas.drawCircle(x.toFloat(), y.toFloat(), 2.6f + pulse, glowPaint)
        }
        glowPaint.maskFilter = null

        canvas.save()
        canvas.translate(centerX, centerY)
        glowPaint.color = adjustAlpha(primaryColor, 78)
        glowPaint.maskFilter = BlurMaskFilter(34f, BlurMaskFilter.Blur.NORMAL)
        canvas.drawPath(buildHeartPath(42f + sin(frameCount * 0.09f) * 4f), glowPaint)
        glowPaint.maskFilter = null

        paint.color = primaryColor
        paint.style = Paint.Style.FILL
        canvas.drawPath(buildHeartPath(34f), paint)
        paint.color = Color.argb(100, 255, 255, 255)
        canvas.drawCircle(-10f, -8f, 4.5f, paint)
        canvas.restore()

        if (message.isNotEmpty()) {
            val messageY = height * 0.66f + textOffsetY
            textPaint.alpha = (textAlpha * 255).toInt()
            subTextPaint.alpha = (textAlpha * 235).toInt()
            drawContrastText(canvas, message, centerX, messageY, textPaint, ContrastTextType.MAIN)
            if (subMessage.isNotEmpty()) {
                drawContrastText(canvas, subMessage, centerX, messageY + 44f, subTextPaint, ContrastTextType.SUB)
            }
            textPaint.alpha = 255
            subTextPaint.alpha = 255
        }
    }

    override fun onUpdateAnimation() {
        frameCount++

        if (frameCount < 54) {
            val progress = (frameCount / 54f).coerceIn(0f, 1f)
            textAlpha = progress
            textOffsetY = 18f * (1f - progress)
        } else {
            textAlpha = 1f
            textOffsetY = sin((frameCount - 54) * 0.03f) * 3f
        }

        val rippleInterval = (30 / animationSpeed).toInt().coerceIn(18, 44)
        val maxRipples = particleCount.coerceIn(3, 7)
        if (frameCount % rippleInterval == 0 && ripples.size < maxRipples) {
            createRipple()
        }

        val maxRadius = max(width, height) * 0.74f
        val toRemove = mutableListOf<Ripple>()
        ripples.forEach { ripple ->
            ripple.radius += 4f * animationSpeed
            ripple.alpha -= 0.016f
            ripple.strokeWidth = 4.8f * ripple.alpha.coerceAtLeast(0.25f)
            if (ripple.alpha <= 0f || ripple.radius > maxRadius) {
                toRemove.add(ripple)
            }
        }
        ripples.removeAll(toRemove)
    }
}
