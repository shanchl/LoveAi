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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class BubbleFloatEffect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseEffectView(context, attrs) {

    private data class Bubble(
        var x: Float,
        var y: Float,
        val radius: Float,
        val speedY: Float,
        val swayAmplitude: Float,
        val swaySpeed: Float,
        var phase: Float,
        val color: Int,
        val alpha: Int,
        val layer: Int,
        val isHeart: Boolean,
        var rotation: Float,
        val rotationSpeed: Float
    )

    private data class LightDust(
        var x: Float,
        var y: Float,
        val radius: Float,
        val speedY: Float,
        val alpha: Int,
        var phase: Float
    )

    private val bubbles = mutableListOf<Bubble>()
    private val dusts = mutableListOf<LightDust>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var frameCount = 0
    private var titleAlpha = 0f
    private var titleOffset = 48f
    private var titleFloat = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return

        backgroundPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h.toFloat(),
            intArrayOf(
                Color.parseColor("#120D24"),
                adjustAlpha(primaryColor, 44),
                adjustAlpha(secondaryColor, 38),
                Color.parseColor("#090812")
            ),
            floatArrayOf(0f, 0.28f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )

        beamPaint.shader = LinearGradient(
            w * 0.2f,
            0f,
            w * 0.82f,
            h.toFloat(),
            intArrayOf(
                adjustAlpha(Color.WHITE, 18),
                adjustAlpha(primaryColor, 28),
                adjustAlpha(secondaryColor, 8)
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )

        bubbles.clear()
        val bubbleCount = particleCount.coerceIn(18, 34)
        repeat(bubbleCount) { bubbles += createBubble(w, h, true) }

        dusts.clear()
        repeat(28) {
            dusts += LightDust(
                x = Random.nextFloat() * w,
                y = Random.nextFloat() * h,
                radius = Random.nextFloat() * 3.5f + 1.2f,
                speedY = Random.nextFloat() * 0.45f + 0.15f,
                alpha = Random.nextInt(40, 120),
                phase = Random.nextFloat() * Math.PI.toFloat() * 2f
            )
        }
    }

    override fun onEffectBound(effect: Effect) {
        frameCount = 0
        titleAlpha = 0f
        titleOffset = 48f
        titleFloat = 0f
        textPaint.apply {
            color = Color.WHITE
            textSize = 46f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(12f, 0f, 2f, textGlowColor())
        }
        subTextPaint.apply {
            color = adjustAlpha(Color.WHITE, 235)
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
    }

    private fun createBubble(w: Int, h: Int, randomY: Boolean): Bubble {
        val layer = Random.nextInt(0, 3)
        val radius = when (layer) {
            0 -> Random.nextFloat() * 14f + 10f
            1 -> Random.nextFloat() * 20f + 18f
            else -> Random.nextFloat() * 26f + 30f
        }
        val palette = if (Random.nextBoolean()) primaryColor else secondaryColor
        val layerAlpha = when (layer) {
            0 -> Random.nextInt(70, 125)
            1 -> Random.nextInt(90, 150)
            else -> Random.nextInt(115, 190)
        }
        return Bubble(
            x = Random.nextFloat() * w,
            y = if (randomY) Random.nextFloat() * h else h + radius * 2f,
            radius = radius,
            speedY = (0.35f + layer * 0.2f + Random.nextFloat() * 0.55f) * animationSpeed,
            swayAmplitude = Random.nextFloat() * 18f + 10f + layer * 8f,
            swaySpeed = Random.nextFloat() * 0.02f + 0.012f,
            phase = Random.nextFloat() * Math.PI.toFloat() * 2f,
            color = palette,
            alpha = layerAlpha,
            layer = layer,
            isHeart = Random.nextFloat() < if (layer == 2) 0.45f else 0.22f,
            rotation = Random.nextFloat() * 360f,
            rotationSpeed = (Random.nextFloat() - 0.5f) * (0.8f + layer * 0.8f)
        )
    }

    private fun buildHeartPath(size: Float): Path {
        return Path().apply {
            moveTo(0f, size * 0.34f)
            cubicTo(-size, -size * 0.1f, -size * 0.86f, -size, 0f, -size * 0.36f)
            cubicTo(size * 0.86f, -size, size, -size * 0.1f, 0f, size * 0.34f)
            close()
        }
    }

    private fun drawBubble(canvas: Canvas, bubble: Bubble) {
        val alpha = bubble.alpha.coerceIn(0, 255)
        val rimColor = Color.argb(
            alpha,
            Color.red(bubble.color),
            Color.green(bubble.color),
            Color.blue(bubble.color)
        )

        if (bubble.isHeart) {
            canvas.save()
            canvas.translate(bubble.x, bubble.y)
            canvas.rotate(bubble.rotation)

            glowPaint.maskFilter = BlurMaskFilter(bubble.radius * 0.55f, BlurMaskFilter.Blur.NORMAL)
            glowPaint.color = adjustAlpha(bubble.color, (alpha * 0.45f).toInt())
            canvas.drawPath(buildHeartPath(bubble.radius), glowPaint)

            paint.shader = RadialGradient(
                0f,
                -bubble.radius * 0.2f,
                bubble.radius * 1.2f,
                intArrayOf(
                    adjustAlpha(Color.WHITE, (alpha * 0.42f).toInt()),
                    rimColor,
                    adjustAlpha(bubble.color, (alpha * 0.35f).toInt())
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
            paint.style = Paint.Style.FILL
            canvas.drawPath(buildHeartPath(bubble.radius), paint)

            paint.shader = null
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = bubble.radius * 0.1f
            paint.color = adjustAlpha(Color.WHITE, (alpha * 0.55f).toInt())
            canvas.drawPath(buildHeartPath(bubble.radius), paint)
            canvas.restore()
            return
        }

        glowPaint.maskFilter = BlurMaskFilter(bubble.radius * 0.8f, BlurMaskFilter.Blur.NORMAL)
        glowPaint.color = adjustAlpha(bubble.color, (alpha * 0.25f).toInt())
        canvas.drawCircle(bubble.x, bubble.y, bubble.radius * 1.04f, glowPaint)

        paint.shader = RadialGradient(
            bubble.x - bubble.radius * 0.25f,
            bubble.y - bubble.radius * 0.3f,
            bubble.radius * 1.35f,
            intArrayOf(
                Color.argb((alpha * 0.52f).toInt(), 255, 255, 255),
                Color.argb((alpha * 0.18f).toInt(), 255, 255, 255),
                Color.argb(0, 255, 255, 255)
            ),
            floatArrayOf(0f, 0.32f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.FILL
        canvas.drawCircle(bubble.x, bubble.y, bubble.radius, paint)

        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = (bubble.radius * 0.09f).coerceAtLeast(2f)
        paint.color = rimColor
        canvas.drawCircle(bubble.x, bubble.y, bubble.radius, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.argb((alpha * 0.55f).toInt(), 255, 255, 255)
        canvas.drawCircle(
            bubble.x - bubble.radius * 0.28f,
            bubble.y - bubble.radius * 0.34f,
            bubble.radius * 0.16f,
            paint
        )
    }

    override fun onDrawEffect(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), beamPaint)

        glowPaint.maskFilter = BlurMaskFilter(width * 0.08f, BlurMaskFilter.Blur.NORMAL)
        glowPaint.color = adjustAlpha(primaryColor, 46)
        canvas.drawCircle(width * 0.2f, height * 0.18f, width * 0.13f, glowPaint)
        glowPaint.color = adjustAlpha(secondaryColor, 34)
        canvas.drawCircle(width * 0.8f, height * 0.72f, width * 0.18f, glowPaint)
        glowPaint.maskFilter = null

        for (dust in dusts) {
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(dust.alpha, 255, 255, 255)
            canvas.drawCircle(dust.x, dust.y, dust.radius, paint)
        }

        bubbles.sortedBy { it.layer }.forEach { drawBubble(canvas, it) }

        if (message.isNotEmpty()) {
            val centerX = width / 2f
            val centerY = height * 0.64f + titleOffset + sin(titleFloat) * 5f
            textPaint.alpha = (titleAlpha * 255).toInt().coerceIn(0, 255)
            subTextPaint.alpha = (titleAlpha * 240).toInt().coerceIn(0, 255)
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

        val textProgress = (frameCount / 90f).coerceIn(0f, 1f)
        val eased = 1f - (1f - textProgress) * (1f - textProgress)
        titleAlpha = eased
        titleOffset = 48f * (1f - eased)
        titleFloat += 0.035f

        dusts.forEach { dust ->
            dust.y -= dust.speedY
            dust.x += sin(frameCount * 0.015f + dust.phase) * 0.4f
            dust.phase += 0.01f
            if (dust.y < -10f) {
                dust.y = h + 10f
                dust.x = Random.nextFloat() * w
            }
        }

        val removeList = mutableListOf<Bubble>()
        bubbles.forEach { bubble ->
            bubble.phase += bubble.swaySpeed * animationSpeed
            bubble.y -= bubble.speedY
            bubble.x += sin(bubble.phase) * bubble.swayAmplitude * 0.06f
            bubble.rotation += bubble.rotationSpeed
            if (bubble.y < -bubble.radius * 2f || bubble.x < -bubble.radius * 3f || bubble.x > w + bubble.radius * 3f) {
                removeList += bubble
            }
        }
        bubbles.removeAll(removeList)

        val targetCount = particleCount.coerceIn(18, 34)
        while (bubbles.size < targetCount) {
            bubbles += createBubble(w, h, false)
        }
    }
}
