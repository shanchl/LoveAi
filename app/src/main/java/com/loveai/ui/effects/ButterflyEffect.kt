package com.loveai.ui.effects

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import com.loveai.model.Effect
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class ButterflyEffect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseEffectView(context, attrs) {

    private data class Butterfly(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var wingPhase: Float,
        val wingSpeed: Float,
        val size: Float,
        val colorA: Int,
        val colorB: Int,
        val layer: Int,
        var targetX: Float,
        var targetY: Float,
        var phase: Float,
        var targetTimer: Int
    )

    private data class Flower(
        val x: Float,
        val y: Float,
        val radius: Float,
        val petals: Int,
        val color: Int,
        val rotation: Float,
        val alpha: Int
    )

    private val butterflies = mutableListOf<Butterfly>()
    private val flowers = mutableListOf<Flower>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hazePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wingPath = Path()
    private var frameCount = 0
    private var textScale = 0.72f
    private var textAlpha = 0f
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
                Color.parseColor("#1A281C"),
                adjustAlpha(primaryColor, 40),
                adjustAlpha(secondaryColor, 34),
                Color.parseColor("#100C18")
            ),
            floatArrayOf(0f, 0.32f, 0.68f, 1f),
            Shader.TileMode.CLAMP
        )

        flowers.clear()
        repeat(20) {
            flowers += Flower(
                x = Random.nextFloat() * w,
                y = h * (0.52f + Random.nextFloat() * 0.44f),
                radius = Random.nextFloat() * 16f + 10f,
                petals = Random.nextInt(4, 7),
                color = if (Random.nextBoolean()) primaryColor else secondaryColor,
                rotation = Random.nextFloat() * 360f,
                alpha = Random.nextInt(34, 85)
            )
        }

        butterflies.clear()
        repeat((particleCount / 8).coerceIn(5, 12)) {
            butterflies += createButterfly(w, h)
        }
    }

    override fun onEffectBound(effect: Effect) {
        frameCount = 0
        textScale = 0.72f
        textAlpha = 0f
        textFloat = 0f
        textPaint.apply {
            color = Color.WHITE
            textSize = 46f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
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

    private fun createButterfly(w: Int, h: Int): Butterfly {
        val layer = Random.nextInt(0, 3)
        val size = when (layer) {
            0 -> Random.nextFloat() * 10f + 14f
            1 -> Random.nextFloat() * 14f + 20f
            else -> Random.nextFloat() * 16f + 28f
        }
        val targetX = Random.nextFloat() * w
        val targetY = h * (0.14f + Random.nextFloat() * 0.6f)
        return Butterfly(
            x = Random.nextFloat() * w,
            y = Random.nextFloat() * h * 0.7f,
            vx = (Random.nextFloat() - 0.5f) * 2.2f,
            vy = (Random.nextFloat() - 0.5f) * 1.6f,
            wingPhase = Random.nextFloat() * Math.PI.toFloat() * 2f,
            wingSpeed = Random.nextFloat() * 0.18f + 0.12f + layer * 0.02f,
            size = size,
            colorA = shiftColor(primaryColor, Random.nextInt(-16, 20)),
            colorB = shiftColor(secondaryColor, Random.nextInt(-12, 24)),
            layer = layer,
            targetX = targetX,
            targetY = targetY,
            phase = Random.nextFloat() * Math.PI.toFloat() * 2f,
            targetTimer = Random.nextInt(40, 120)
        )
    }

    private fun shiftColor(color: Int, delta: Int): Int {
        return Color.argb(
            255,
            (Color.red(color) + delta).coerceIn(0, 255),
            (Color.green(color) + delta / 2).coerceIn(0, 255),
            (Color.blue(color) + delta).coerceIn(0, 255)
        )
    }

    private fun drawFlower(canvas: Canvas, flower: Flower) {
        canvas.save()
        canvas.translate(flower.x, flower.y)
        canvas.rotate(flower.rotation)

        val petalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = adjustAlpha(flower.color, flower.alpha)
            style = Paint.Style.FILL
        }

        repeat(flower.petals) { index ->
            canvas.save()
            canvas.rotate(index * (360f / flower.petals))
            canvas.drawOval(
                -flower.radius * 0.32f,
                -flower.radius,
                flower.radius * 0.32f,
                0f,
                petalPaint
            )
            canvas.restore()
        }

        petalPaint.color = adjustAlpha(Color.WHITE, (flower.alpha * 1.3f).toInt().coerceAtMost(180))
        canvas.drawCircle(0f, 0f, flower.radius * 0.24f, petalPaint)
        canvas.restore()
    }

    private fun drawWing(canvas: Canvas, width: Float, height: Float, colorA: Int, colorB: Int, alpha: Int) {
        wingPath.reset()
        wingPath.moveTo(0f, 0f)
        wingPath.cubicTo(-width * 0.22f, -height * 0.2f, -width, -height * 0.14f, -width * 0.88f, height * 0.38f)
        wingPath.cubicTo(-width * 0.78f, height * 0.88f, -width * 0.18f, height * 0.68f, 0f, 0f)
        wingPath.close()

        paint.shader = LinearGradient(
            -width,
            -height * 0.3f,
            0f,
            height * 0.7f,
            intArrayOf(
                adjustAlpha(colorA, alpha),
                adjustAlpha(colorB, (alpha * 0.78f).toInt()),
                adjustAlpha(Color.WHITE, (alpha * 0.3f).toInt())
            ),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.FILL
        canvas.drawPath(wingPath, paint)

        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1.3f, width * 0.03f)
        paint.color = adjustAlpha(Color.WHITE, (alpha * 0.28f).toInt())
        canvas.drawPath(wingPath, paint)

        paint.color = adjustAlpha(Color.BLACK, (alpha * 0.22f).toInt())
        paint.strokeWidth = max(0.8f, width * 0.016f)
        canvas.drawLine(0f, 0f, -width * 0.74f, height * 0.25f, paint)
        canvas.drawLine(0f, 0f, -width * 0.7f, -height * 0.04f, paint)
    }

    private fun drawButterfly(canvas: Canvas, butterfly: Butterfly) {
        val wingAngle = sin(butterfly.wingPhase) * (34f + butterfly.layer * 8f)
        val direction = atan2(butterfly.vy, butterfly.vx) * (180f / Math.PI.toFloat())
        val alpha = when (butterfly.layer) {
            0 -> 150
            1 -> 195
            else -> 230
        }

        canvas.save()
        canvas.translate(butterfly.x, butterfly.y)
        canvas.rotate(direction + 90f)

        hazePaint.maskFilter = BlurMaskFilter(butterfly.size * 0.65f, BlurMaskFilter.Blur.NORMAL)
        hazePaint.color = adjustAlpha(butterfly.colorA, (alpha * 0.24f).toInt())
        canvas.drawCircle(0f, 0f, butterfly.size * 1.25f, hazePaint)

        canvas.save()
        canvas.translate(-butterfly.size * 0.08f, -butterfly.size * 0.1f)
        canvas.rotate(-wingAngle)
        drawWing(canvas, butterfly.size * 1.12f, butterfly.size * 0.96f, butterfly.colorA, butterfly.colorB, alpha)
        canvas.restore()

        canvas.save()
        canvas.translate(butterfly.size * 0.08f, -butterfly.size * 0.1f)
        canvas.rotate(wingAngle)
        canvas.scale(-1f, 1f)
        drawWing(canvas, butterfly.size * 1.12f, butterfly.size * 0.96f, butterfly.colorA, butterfly.colorB, alpha)
        canvas.restore()

        canvas.save()
        canvas.translate(-butterfly.size * 0.05f, butterfly.size * 0.44f)
        canvas.rotate(-wingAngle * 0.75f)
        drawWing(canvas, butterfly.size * 0.78f, butterfly.size * 0.58f, butterfly.colorB, butterfly.colorA, (alpha * 0.86f).toInt())
        canvas.restore()

        canvas.save()
        canvas.translate(butterfly.size * 0.05f, butterfly.size * 0.44f)
        canvas.rotate(wingAngle * 0.75f)
        canvas.scale(-1f, 1f)
        drawWing(canvas, butterfly.size * 0.78f, butterfly.size * 0.58f, butterfly.colorB, butterfly.colorA, (alpha * 0.86f).toInt())
        canvas.restore()

        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(alpha, 40, 24, 16)
        canvas.drawRoundRect(
            -butterfly.size * 0.08f,
            -butterfly.size * 0.65f,
            butterfly.size * 0.08f,
            butterfly.size * 0.68f,
            butterfly.size * 0.08f,
            butterfly.size * 0.08f,
            paint
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = adjustAlpha(Color.WHITE, (alpha * 0.4f).toInt())
        canvas.drawLine(-butterfly.size * 0.03f, -butterfly.size * 0.58f, -butterfly.size * 0.22f, -butterfly.size * 1.02f, paint)
        canvas.drawLine(butterfly.size * 0.03f, -butterfly.size * 0.58f, butterfly.size * 0.22f, -butterfly.size * 1.02f, paint)
        canvas.restore()
    }

    override fun onDrawEffect(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        hazePaint.maskFilter = BlurMaskFilter(width * 0.1f, BlurMaskFilter.Blur.NORMAL)
        hazePaint.color = adjustAlpha(primaryColor, 34)
        canvas.drawCircle(width * 0.18f, height * 0.22f, width * 0.16f, hazePaint)
        hazePaint.color = adjustAlpha(secondaryColor, 26)
        canvas.drawCircle(width * 0.78f, height * 0.76f, width * 0.2f, hazePaint)
        hazePaint.maskFilter = null

        flowers.forEach { drawFlower(canvas, it) }
        butterflies.sortedBy { it.layer }.forEach { drawButterfly(canvas, it) }

        if (message.isNotEmpty()) {
            val centerX = width / 2f
            val centerY = height * 0.58f + sin(textFloat) * 4f
            canvas.save()
            canvas.scale(textScale, textScale, centerX, centerY)
            textPaint.alpha = (textAlpha * 255).toInt().coerceIn(0, 255)
            subTextPaint.alpha = (textAlpha * 235).toInt().coerceIn(0, 255)
            drawContrastText(canvas, message, centerX, centerY, textPaint, ContrastTextType.MAIN)
            if (subMessage.isNotEmpty()) {
                drawContrastText(canvas, subMessage, centerX, centerY + 52f, subTextPaint, ContrastTextType.SUB)
            }
            canvas.restore()
            textPaint.alpha = 255
            subTextPaint.alpha = 255
        }
    }

    override fun onUpdateAnimation() {
        frameCount++
        val w = width
        val h = height
        if (w == 0 || h == 0) return

        val t = (frameCount / 88f).coerceIn(0f, 1f)
        textScale = 1f - 0.28f * exp(-t * 5.2f) * cos(t * 14f)
        textAlpha = (t * 1.5f).coerceIn(0f, 1f)
        textFloat += 0.03f

        butterflies.forEach { butterfly ->
            butterfly.phase += 0.012f
            butterfly.wingPhase += butterfly.wingSpeed * animationSpeed
            butterfly.targetTimer++

            if (butterfly.targetTimer > 100 + butterfly.layer * 30) {
                butterfly.targetX = Random.nextFloat() * w
                butterfly.targetY = h * (0.12f + Random.nextFloat() * 0.62f)
                butterfly.targetTimer = 0
            }

            val dx = butterfly.targetX - butterfly.x
            val dy = butterfly.targetY - butterfly.y
            val distance = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            val impulse = 0.07f + butterfly.layer * 0.02f
            butterfly.vx = (butterfly.vx + dx / distance * impulse) * 0.96f + sin(butterfly.phase * 2.2f) * 0.14f
            butterfly.vy = (butterfly.vy + dy / distance * impulse) * 0.96f + cos(butterfly.phase * 1.7f) * 0.11f

            val maxSpeed = 1.9f + butterfly.layer * 0.6f
            val speed = sqrt(butterfly.vx * butterfly.vx + butterfly.vy * butterfly.vy).coerceAtLeast(0.001f)
            if (speed > maxSpeed) {
                butterfly.vx = butterfly.vx / speed * maxSpeed
                butterfly.vy = butterfly.vy / speed * maxSpeed
            }

            butterfly.x += butterfly.vx * animationSpeed
            butterfly.y += butterfly.vy * animationSpeed

            val margin = 56f
            if (butterfly.x < margin) butterfly.vx += 0.24f
            if (butterfly.x > w - margin) butterfly.vx -= 0.24f
            if (butterfly.y < margin) butterfly.vy += 0.22f
            if (butterfly.y > h - margin) butterfly.vy -= 0.22f
        }
    }
}
