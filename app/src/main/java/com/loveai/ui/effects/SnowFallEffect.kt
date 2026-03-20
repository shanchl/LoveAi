package com.loveai.ui.effects

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
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

class SnowFallEffect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseEffectView(context, attrs) {

    private data class Snowflake(
        var x: Float,
        var y: Float,
        val size: Float,
        val speedY: Float,
        val speedX: Float,
        val alpha: Int,
        var rotation: Float,
        val rotationSpeed: Float,
        var phase: Float,
        val layer: Int
    )

    private val snowflakes = mutableListOf<Snowflake>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ridgePath = Path()
    private var frameCount = 0
    private var textAlpha = 0f
    private var textOffset = 28f
    private var textPulse = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return

        backgroundPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h.toFloat(),
            intArrayOf(
                Color.parseColor("#13223F"),
                Color.parseColor("#0A1329"),
                Color.parseColor("#050912")
            ),
            floatArrayOf(0f, 0.46f, 1f),
            Shader.TileMode.CLAMP
        )

        snowflakes.clear()
        repeat(particleCount.coerceIn(34, 110)) {
            snowflakes += createSnowflake(w, h, true)
        }
    }

    override fun onEffectBound(effect: Effect) {
        frameCount = 0
        textAlpha = 0f
        textOffset = 28f
        textPulse = 0f
        textPaint.apply {
            color = Color.WHITE
            textSize = 44f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(12f, 0f, 1f, textGlowColor())
        }
        subTextPaint.apply {
            color = adjustAlpha(Color.WHITE, 230)
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
    }

    private fun createSnowflake(w: Int, h: Int, randomY: Boolean): Snowflake {
        val layer = Random.nextInt(0, 3)
        val size = when (layer) {
            0 -> Random.nextFloat() * 6f + 3f
            1 -> Random.nextFloat() * 8f + 7f
            else -> Random.nextFloat() * 12f + 12f
        }
        return Snowflake(
            x = Random.nextFloat() * w,
            y = if (randomY) Random.nextFloat() * h else -size * 4f,
            size = size,
            speedY = (Random.nextFloat() * 0.7f + 0.35f + layer * 0.28f) * animationSpeed,
            speedX = (Random.nextFloat() - 0.5f) * (0.5f + layer * 0.35f) * animationSpeed,
            alpha = Random.nextInt(105 + layer * 35, 180 + layer * 25).coerceAtMost(245),
            rotation = Random.nextFloat() * 360f,
            rotationSpeed = (Random.nextFloat() - 0.5f) * (0.8f + layer * 0.6f),
            phase = Random.nextFloat() * Math.PI.toFloat() * 2f,
            layer = layer
        )
    }

    private fun drawSnowflake(canvas: Canvas, snowflake: Snowflake) {
        val alpha = snowflake.alpha.coerceIn(0, 255)
        canvas.save()
        canvas.translate(snowflake.x, snowflake.y)
        canvas.rotate(snowflake.rotation)

        glowPaint.maskFilter = BlurMaskFilter(snowflake.size * 0.7f, BlurMaskFilter.Blur.NORMAL)
        glowPaint.color = Color.argb((alpha * 0.22f).toInt(), 210, 235, 255)
        canvas.drawCircle(0f, 0f, snowflake.size * 0.82f, glowPaint)

        paint.shader = RadialGradient(
            0f,
            0f,
            snowflake.size * 1.15f,
            intArrayOf(
                Color.argb((alpha * 0.45f).toInt(), 255, 255, 255),
                Color.argb(alpha, 214, 234, 255),
                Color.argb(0, 214, 234, 255)
            ),
            floatArrayOf(0f, 0.35f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1f, snowflake.size / 6f)
        val armCount = 6
        repeat(armCount) { index ->
            canvas.save()
            canvas.rotate(index * 60f)
            canvas.drawLine(0f, 0f, 0f, -snowflake.size, paint)
            if (snowflake.size > 6f) {
                canvas.drawLine(0f, -snowflake.size * 0.55f, snowflake.size * 0.2f, -snowflake.size * 0.72f, paint)
                canvas.drawLine(0f, -snowflake.size * 0.55f, -snowflake.size * 0.2f, -snowflake.size * 0.72f, paint)
            }
            canvas.restore()
        }
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = Color.argb((alpha * 0.85f).toInt(), 255, 255, 255)
        canvas.drawCircle(0f, 0f, max(1f, snowflake.size / 8f), paint)
        canvas.restore()
    }

    private fun drawSnowGround(canvas: Canvas) {
        ridgePath.reset()
        ridgePath.moveTo(0f, height.toFloat())
        ridgePath.lineTo(0f, height * 0.82f)
        ridgePath.cubicTo(width * 0.18f, height * 0.74f, width * 0.32f, height * 0.88f, width * 0.48f, height * 0.8f)
        ridgePath.cubicTo(width * 0.62f, height * 0.72f, width * 0.82f, height * 0.9f, width.toFloat(), height * 0.8f)
        ridgePath.lineTo(width.toFloat(), height.toFloat())
        ridgePath.close()

        paint.shader = LinearGradient(
            0f,
            height * 0.74f,
            0f,
            height.toFloat(),
            intArrayOf(
                Color.argb(60, 210, 228, 255),
                Color.argb(165, 182, 208, 240),
                Color.argb(220, 130, 156, 194)
            ),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.FILL
        canvas.drawPath(ridgePath, paint)
        paint.shader = null
    }

    override fun onDrawEffect(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        glowPaint.maskFilter = BlurMaskFilter(width * 0.12f, BlurMaskFilter.Blur.NORMAL)
        glowPaint.color = adjustAlpha(primaryColor, 18)
        canvas.drawCircle(width * 0.24f, height * 0.2f, width * 0.16f, glowPaint)
        glowPaint.color = Color.argb(28, 190, 220, 255)
        canvas.drawCircle(width * 0.72f, height * 0.26f, width * 0.12f, glowPaint)
        glowPaint.maskFilter = null

        snowflakes.sortedBy { it.layer }.forEach { drawSnowflake(canvas, it) }
        drawSnowGround(canvas)

        paint.shader = LinearGradient(
            0f,
            height * 0.66f,
            0f,
            height.toFloat(),
            intArrayOf(Color.argb(0, 255, 255, 255), Color.argb(52, 170, 190, 220)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, height * 0.66f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        if (message.isNotEmpty()) {
            val centerX = width / 2f
            val centerY = height * 0.55f + textOffset
            val shimmer = sin(textPulse) * 0.18f + 0.82f
            textPaint.alpha = (textAlpha * 255 * shimmer).toInt().coerceIn(0, 255)
            subTextPaint.alpha = (textAlpha * 235).toInt().coerceIn(0, 255)
            drawContrastText(canvas, message, centerX, centerY, textPaint, ContrastTextType.MAIN)
            if (subMessage.isNotEmpty()) {
                drawContrastText(canvas, subMessage, centerX, centerY + 48f, subTextPaint, ContrastTextType.SUB)
            }
            textPaint.alpha = 255
            subTextPaint.alpha = 255
        }
    }

    override fun onUpdateAnimation() {
        frameCount++
        val w = width
        val h = height
        if (w == 0 || h == 0) return

        textAlpha = (frameCount / 70f).coerceIn(0f, 1f)
        textOffset = 28f * (1f - textAlpha)
        textPulse += 0.04f

        val removeList = mutableListOf<Snowflake>()
        snowflakes.forEach { snowflake ->
            snowflake.phase += (0.018f + snowflake.layer * 0.004f) * animationSpeed
            snowflake.y += snowflake.speedY
            snowflake.x += snowflake.speedX + sin(snowflake.phase) * (0.4f + snowflake.layer * 0.45f)
            snowflake.rotation += snowflake.rotationSpeed
            if (snowflake.y > h + snowflake.size * 4f || snowflake.x < -snowflake.size * 4f || snowflake.x > w + snowflake.size * 4f) {
                removeList += snowflake
            }
        }
        snowflakes.removeAll(removeList)

        val targetCount = particleCount.coerceIn(34, 110)
        while (snowflakes.size < targetCount) {
            snowflakes += createSnowflake(w, h, false)
        }
    }
}
