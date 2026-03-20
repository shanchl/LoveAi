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
import kotlin.math.sin
import kotlin.random.Random

class PetalFallEffect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseEffectView(context, attrs) {

    private data class Petal(
        var x: Float,
        var y: Float,
        val size: Float,
        val speedY: Float,
        val driftX: Float,
        var phase: Float,
        var rotation: Float,
        val rotationSpeed: Float,
        val alpha: Int,
        val color: Int,
        val layer: Int
    )

    private val petals = mutableListOf<Petal>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val petalPath = Path()
    private var frameCount = 0
    private var titleCount = 0
    private var subAlpha = 0f
    private var textFloat = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return

        backgroundPaint.shader = LinearGradient(
            0f,
            0f,
            w.toFloat(),
            h.toFloat(),
            intArrayOf(
                Color.parseColor("#261119"),
                adjustAlpha(primaryColor, 62),
                adjustAlpha(secondaryColor, 54),
                Color.parseColor("#120A10")
            ),
            floatArrayOf(0f, 0.34f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )

        petals.clear()
        repeat(particleCount.coerceIn(22, 56)) {
            petals += createPetal(w, h, true)
        }
    }

    override fun onEffectBound(effect: Effect) {
        frameCount = 0
        titleCount = 0
        subAlpha = 0f
        textFloat = 0f
        textPaint.apply {
            color = Color.WHITE
            textSize = 44f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(12f, 0f, 2f, textGlowColor())
        }
        subTextPaint.apply {
            color = adjustAlpha(Color.WHITE, 235)
            textSize = 28f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
    }

    private fun createPetal(w: Int, h: Int, randomY: Boolean): Petal {
        val layer = Random.nextInt(0, 3)
        val size = when (layer) {
            0 -> Random.nextFloat() * 10f + 8f
            1 -> Random.nextFloat() * 15f + 16f
            else -> Random.nextFloat() * 18f + 28f
        }
        val baseColor = if (Random.nextBoolean()) primaryColor else secondaryColor
        return Petal(
            x = Random.nextFloat() * w * 1.2f - w * 0.1f,
            y = if (randomY) Random.nextFloat() * h else -size * 3f,
            size = size,
            speedY = (Random.nextFloat() * 0.8f + 0.55f + layer * 0.24f) * animationSpeed,
            driftX = (Random.nextFloat() - 0.5f) * (0.8f + layer * 0.55f) * animationSpeed,
            phase = Random.nextFloat() * Math.PI.toFloat() * 2f,
            rotation = Random.nextFloat() * 360f,
            rotationSpeed = (Random.nextFloat() - 0.5f) * (1.6f + layer * 1.1f),
            alpha = Random.nextInt(100 + layer * 35, 170 + layer * 25).coerceAtMost(235),
            color = Color.argb(
                255,
                (Color.red(baseColor) + Random.nextInt(-18, 18)).coerceIn(0, 255),
                (Color.green(baseColor) + Random.nextInt(-12, 18)).coerceIn(0, 255),
                (Color.blue(baseColor) + Random.nextInt(-16, 16)).coerceIn(0, 255)
            ),
            layer = layer
        )
    }

    private fun drawPetalShape(canvas: Canvas, petal: Petal) {
        val size = petal.size
        petalPath.reset()
        petalPath.moveTo(0f, -size)
        petalPath.cubicTo(size * 0.86f, -size * 0.58f, size * 0.82f, size * 0.44f, 0f, size * 0.36f)
        petalPath.cubicTo(-size * 0.82f, size * 0.44f, -size * 0.86f, -size * 0.58f, 0f, -size)
        petalPath.close()

        canvas.save()
        canvas.translate(petal.x, petal.y)
        canvas.rotate(petal.rotation)
        canvas.scale(1f + petal.layer * 0.04f, 0.96f)

        glowPaint.maskFilter = BlurMaskFilter(size * 0.7f, BlurMaskFilter.Blur.NORMAL)
        glowPaint.color = adjustAlpha(petal.color, (petal.alpha * 0.18f).toInt())
        canvas.drawPath(petalPath, glowPaint)

        paint.shader = RadialGradient(
            0f,
            -size * 0.3f,
            size * 1.4f,
            intArrayOf(
                adjustAlpha(Color.WHITE, (petal.alpha * 0.46f).toInt()),
                adjustAlpha(petal.color, petal.alpha),
                adjustAlpha(petal.color, (petal.alpha * 0.28f).toInt())
            ),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.FILL
        canvas.drawPath(petalPath, paint)

        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = (size * 0.08f).coerceAtLeast(1.2f)
        paint.color = adjustAlpha(Color.WHITE, (petal.alpha * 0.34f).toInt())
        canvas.drawPath(petalPath, paint)

        paint.color = adjustAlpha(Color.WHITE, (petal.alpha * 0.24f).toInt())
        paint.strokeWidth = 1f
        canvas.drawLine(0f, -size * 0.72f, 0f, size * 0.2f, paint)
        canvas.restore()
    }

    override fun onDrawEffect(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        glowPaint.maskFilter = BlurMaskFilter(width * 0.1f, BlurMaskFilter.Blur.NORMAL)
        glowPaint.color = adjustAlpha(primaryColor, 28)
        canvas.drawCircle(width * 0.16f, height * 0.18f, width * 0.18f, glowPaint)
        glowPaint.color = adjustAlpha(secondaryColor, 24)
        canvas.drawCircle(width * 0.78f, height * 0.74f, width * 0.22f, glowPaint)
        glowPaint.maskFilter = null

        petals.sortedBy { it.layer }.forEach { drawPetalShape(canvas, it) }

        paint.shader = LinearGradient(
            0f,
            height * 0.82f,
            0f,
            height.toFloat(),
            intArrayOf(Color.argb(0, 0, 0, 0), Color.argb(82, 18, 10, 18)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, height * 0.82f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        if (message.isNotEmpty()) {
            val visibleText = message.take(titleCount.coerceAtMost(message.length))
            val centerX = width / 2f
            val centerY = height * 0.58f + sin(textFloat) * 4f

            if (visibleText.isNotEmpty()) {
                drawContrastText(canvas, visibleText, centerX, centerY, textPaint, ContrastTextType.MAIN)
            }
            if (subMessage.isNotEmpty() && titleCount >= message.length) {
                subTextPaint.alpha = (subAlpha * 255).toInt().coerceIn(0, 255)
                drawContrastText(canvas, subMessage, centerX, centerY + 48f, subTextPaint, ContrastTextType.SUB)
                subTextPaint.alpha = 255
            }
        }
    }

    override fun onUpdateAnimation() {
        frameCount++
        val w = width
        val h = height
        if (w == 0 || h == 0) return

        val revealInterval = (6f / animationSpeed).toInt().coerceIn(2, 8)
        if (frameCount % revealInterval == 0 && titleCount < message.length) {
            titleCount++
        } else if (titleCount >= message.length) {
            subAlpha = (subAlpha + 0.04f).coerceAtMost(1f)
        }
        textFloat += 0.03f

        val removeList = mutableListOf<Petal>()
        petals.forEach { petal ->
            petal.phase += (0.016f + petal.layer * 0.004f) * animationSpeed
            petal.y += petal.speedY
            petal.x += petal.driftX + sin(petal.phase) * (0.5f + petal.layer * 0.4f)
            petal.rotation += petal.rotationSpeed
            if (petal.y > h + petal.size * 4f || petal.x < -petal.size * 5f || petal.x > w + petal.size * 5f) {
                removeList += petal
            }
        }
        petals.removeAll(removeList)

        val targetCount = particleCount.coerceIn(22, 56)
        while (petals.size < targetCount) {
            petals += createPetal(w, h, false)
        }
    }
}
