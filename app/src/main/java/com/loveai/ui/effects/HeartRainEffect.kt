package com.loveai.ui.effects

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import com.loveai.model.Effect
import kotlin.math.sin
import kotlin.random.Random

/**
 * 第三轮精修：做出前中后景层次感，让心雨不再只是“随机往下掉”。
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
        var swingAmplitude: Float,
        var layer: Int
    )

    private val hearts = mutableListOf<Heart>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val streakPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var frameCount = 0
    private var textAlpha = 0f
    private var textOffsetY = 30f
    private var textGlow = 0f

    override fun onEffectBound(effect: Effect) {
        frameCount = 0
        textAlpha = 0f
        textOffsetY = 30f
        textGlow = 0f

        textPaint.apply {
            color = Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            textAlign = Paint.Align.CENTER
            setShadowLayer(8f, 0f, 2f, textGlowColor())
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
        hearts.clear()

        val count = particleCount.coerceIn(28, 120)
        repeat(count) {
            hearts.add(createHeart(w, h, true))
        }

        bgPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h.toFloat(),
            intArrayOf(
                adjustAlpha(primaryColor, 24),
                adjustAlpha(secondaryColor, 18),
                backgroundColor
            ),
            floatArrayOf(0f, 0.35f, 1f),
            Shader.TileMode.CLAMP
        )

        streakPaint.shader = LinearGradient(
            0f,
            0f,
            w.toFloat(),
            h.toFloat(),
            intArrayOf(
                Color.TRANSPARENT,
                adjustAlpha(primaryColor, 18),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    private fun createHeart(w: Int, h: Int, randomY: Boolean): Heart {
        val layer = Random.nextInt(3)
        val baseSize = when (layer) {
            0 -> Random.nextFloat() * 16f + 10f
            1 -> Random.nextFloat() * 24f + 16f
            else -> Random.nextFloat() * 34f + 22f
        }
        val speed = when (layer) {
            0 -> Random.nextFloat() * 1.2f + 0.8f
            1 -> Random.nextFloat() * 1.6f + 1.2f
            else -> Random.nextFloat() * 2.2f + 1.6f
        } * animationSpeed

        val alpha = when (layer) {
            0 -> Random.nextInt(60, 120)
            1 -> Random.nextInt(110, 180)
            else -> Random.nextInt(170, 245)
        }

        val colorBase = if (Random.nextBoolean()) primaryColor else secondaryColor
        val color = Color.argb(
            alpha,
            (Color.red(colorBase) + Random.nextInt(-18, 19)).coerceIn(0, 255),
            (Color.green(colorBase) + Random.nextInt(-12, 13)).coerceIn(0, 255),
            (Color.blue(colorBase) + Random.nextInt(-18, 19)).coerceIn(0, 255)
        )

        return Heart(
            x = Random.nextFloat() * w,
            y = if (randomY) Random.nextFloat() * h else -baseSize * 3f,
            size = baseSize,
            speed = speed,
            alpha = alpha,
            rotation = Random.nextFloat() * 360f,
            rotSpeed = (Random.nextFloat() - 0.5f) * (1.2f + layer),
            color = color,
            swingOffset = Random.nextFloat() * (Math.PI * 2).toFloat(),
            swingAmplitude = Random.nextFloat() * 24f + 8f + layer * 6f,
            layer = layer
        )
    }

    private fun buildHeartPath(size: Float): Path {
        return Path().apply {
            moveTo(0f, size * 0.3f)
            cubicTo(-size, -size * 0.2f, -size, -size * 0.85f, 0f, -size * 0.4f)
            cubicTo(size, -size * 0.85f, size, -size * 0.2f, 0f, size * 0.3f)
            close()
        }
    }

    override fun onDrawEffect(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), streakPaint)

        hearts.sortedBy { it.layer }.forEach { heart ->
            canvas.save()
            canvas.translate(heart.x, heart.y)
            canvas.rotate(heart.rotation)

            val scale = when (heart.layer) {
                0 -> 0.75f
                1 -> 1f
                else -> 1.15f
            }
            canvas.scale(scale, scale)

            if (heart.layer == 2) {
                glowPaint.color = adjustAlpha(primaryColor, (heart.alpha * 0.35f).toInt().coerceAtMost(120))
                glowPaint.setShadowLayer(18f, 0f, 0f, glowPaint.color)
                canvas.drawPath(buildHeartPath(heart.size * 1.08f), glowPaint)
                glowPaint.clearShadowLayer()
            }

            paint.color = heart.color
            paint.alpha = heart.alpha
            paint.style = Paint.Style.FILL
            canvas.drawPath(buildHeartPath(heart.size), paint)

            paint.color = Color.argb((heart.alpha * 0.22f).toInt(), 255, 255, 255)
            canvas.drawCircle(-heart.size * 0.24f, -heart.size * 0.22f, heart.size * 0.12f, paint)

            canvas.restore()
        }

        if (message.isNotEmpty()) {
            val centerX = width / 2f
            val centerY = height * 0.66f + textOffsetY

            val glowRadius = 7f + textGlow * 8f
            textPaint.setShadowLayer(glowRadius, 0f, 0f, textGlowColor())
            textPaint.alpha = (textAlpha * 255).toInt()
            subTextPaint.alpha = (textAlpha * 235).toInt()

            drawContrastText(canvas, message, centerX, centerY, textPaint, ContrastTextType.MAIN)
            if (subMessage.isNotEmpty()) {
                drawContrastText(canvas, subMessage, centerX, centerY + 50f, subTextPaint, ContrastTextType.SUB)
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

        if (frameCount < 90) {
            val progress = (frameCount / 90f).coerceIn(0f, 1f)
            val eased = 1f - (1f - progress) * (1f - progress) * (1f - progress)
            textAlpha = eased
            textOffsetY = 30f * (1f - eased)
        } else {
            textAlpha = 1f
            textOffsetY = sin((frameCount - 90) * 0.03f) * 6f
        }
        textGlow = (sin(frameCount * 0.04f) * 0.5f + 0.5f)

        val toRemove = mutableListOf<Heart>()
        hearts.forEach { heart ->
            val sway = sin(frameCount * (0.02f + heart.layer * 0.007f) + heart.swingOffset)
            heart.y += heart.speed
            heart.x += sway * heart.swingAmplitude * 0.05f
            heart.rotation += heart.rotSpeed

            if (heart.y > h + heart.size * 4f) {
                toRemove.add(heart)
            }
        }
        hearts.removeAll(toRemove)

        val targetCount = particleCount.coerceIn(28, 120)
        while (hearts.size < targetCount) {
            hearts.add(createHeart(w, h, false))
        }
    }
}
