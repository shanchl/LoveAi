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

class StarrySkyEffect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseEffectView(context, attrs) {

    private data class Star(
        val x: Float,
        val y: Float,
        val radius: Float,
        var twinkle: Float,
        val twinkleSpeed: Float,
        val alpha: Int,
        val layer: Int
    )

    private data class Meteor(
        var x: Float,
        var y: Float,
        val length: Float,
        val speed: Float,
        val angle: Float,
        var life: Float,
        val maxLife: Float
    )

    private data class ConstellationLine(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val alpha: Int
    )

    private val stars = mutableListOf<Star>()
    private val meteors = mutableListOf<Meteor>()
    private val constellationLines = mutableListOf<ConstellationLine>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hillPath = Path()
    private var frameCount = 0
    private var textAlpha = 0f
    private var textSlideY = 32f
    private var textFloat = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return

        backgroundPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h.toFloat(),
            intArrayOf(
                Color.parseColor("#101734"),
                Color.parseColor("#090E22"),
                Color.parseColor("#04060F")
            ),
            floatArrayOf(0f, 0.46f, 1f),
            Shader.TileMode.CLAMP
        )

        stars.clear()
        repeat(particleCount.coerceIn(90, 260)) {
            val layer = Random.nextInt(0, 3)
            stars += Star(
                x = Random.nextFloat() * w,
                y = Random.nextFloat() * h * 0.78f,
                radius = Random.nextFloat() * (if (layer == 2) 2.4f else 1.5f) + if (layer == 2) 1.2f else 0.5f,
                twinkle = Random.nextFloat() * Math.PI.toFloat() * 2f,
                twinkleSpeed = Random.nextFloat() * 0.03f + 0.006f,
                alpha = Random.nextInt(50 + layer * 35, 130 + layer * 40).coerceAtMost(235),
                layer = layer
            )
        }

        constellationLines.clear()
        repeat(8) {
            val x1 = w * (0.1f + Random.nextFloat() * 0.8f)
            val y1 = h * (0.12f + Random.nextFloat() * 0.34f)
            val x2 = x1 + Random.nextFloat() * 120f - 60f
            val y2 = y1 + Random.nextFloat() * 80f - 40f
            constellationLines += ConstellationLine(x1, y1, x2, y2, Random.nextInt(24, 56))
        }

        meteors.clear()
    }

    override fun onEffectBound(effect: Effect) {
        frameCount = 0
        textAlpha = 0f
        textSlideY = 32f
        textFloat = 0f
        meteors.clear()
        textPaint.apply {
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            textAlign = Paint.Align.CENTER
            setShadowLayer(14f, 0f, 2f, textGlowColor())
        }
        subTextPaint.apply {
            color = adjustAlpha(Color.WHITE, 230)
            textSize = 27f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.05f
        }
    }

    private fun createMeteor(): Meteor {
        val life = Random.nextFloat() * 42f + 34f
        return Meteor(
            x = Random.nextFloat() * width * 1.1f + width * 0.05f,
            y = Random.nextFloat() * height * 0.35f,
            length = Random.nextFloat() * 110f + 90f,
            speed = (Random.nextFloat() * 8f + 10f) * animationSpeed,
            angle = Math.toRadians((118 + Random.nextInt(-10, 12)).toDouble()).toFloat(),
            life = life,
            maxLife = life
        )
    }

    private fun drawHills(canvas: Canvas) {
        hillPath.reset()
        hillPath.moveTo(0f, height.toFloat())
        hillPath.lineTo(0f, height * 0.84f)
        hillPath.cubicTo(width * 0.16f, height * 0.72f, width * 0.3f, height * 0.88f, width * 0.46f, height * 0.8f)
        hillPath.cubicTo(width * 0.64f, height * 0.7f, width * 0.8f, height * 0.9f, width.toFloat(), height * 0.8f)
        hillPath.lineTo(width.toFloat(), height.toFloat())
        hillPath.close()

        paint.shader = LinearGradient(
            0f,
            height * 0.72f,
            0f,
            height.toFloat(),
            intArrayOf(
                Color.argb(26, 84, 96, 132),
                Color.argb(180, 20, 24, 40),
                Color.argb(230, 7, 9, 15)
            ),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.FILL
        canvas.drawPath(hillPath, paint)
        paint.shader = null
    }

    override fun onDrawEffect(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        glowPaint.maskFilter = BlurMaskFilter(width * 0.13f, BlurMaskFilter.Blur.NORMAL)
        glowPaint.color = adjustAlpha(primaryColor, 24)
        canvas.drawCircle(width * 0.22f, height * 0.18f, width * 0.18f, glowPaint)
        glowPaint.color = adjustAlpha(secondaryColor, 20)
        canvas.drawCircle(width * 0.74f, height * 0.12f, width * 0.14f, glowPaint)
        glowPaint.maskFilter = null

        constellationLines.forEach { line ->
            paint.shader = null
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.2f
            paint.color = Color.argb(line.alpha, 190, 214, 255)
            canvas.drawLine(line.startX, line.startY, line.endX, line.endY, paint)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(line.startX, line.startY, 2.4f, paint)
            canvas.drawCircle(line.endX, line.endY, 2.4f, paint)
        }

        stars.sortedBy { it.layer }.forEach { star ->
            val shimmer = sin(star.twinkle) * 0.35f + 0.65f
            paint.style = Paint.Style.FILL
            paint.shader = null
            paint.color = Color.argb((star.alpha * shimmer).toInt().coerceIn(0, 255), 255, 255, 255)
            if (star.layer == 2) {
                glowPaint.maskFilter = BlurMaskFilter(star.radius * 4f, BlurMaskFilter.Blur.NORMAL)
                glowPaint.color = adjustAlpha(primaryColor, (star.alpha * 0.18f * shimmer).toInt())
                canvas.drawCircle(star.x, star.y, star.radius * 1.9f, glowPaint)
            }
            canvas.drawCircle(star.x, star.y, star.radius * (0.82f + shimmer * 0.34f), paint)
        }

        meteors.forEach { meteor ->
            val progress = meteor.life / meteor.maxLife
            val alpha = ((0.18f + progress * 0.82f) * 255).toInt().coerceIn(0, 255)
            val dx = cos(meteor.angle)
            val dy = sin(meteor.angle)
            paint.shader = LinearGradient(
                meteor.x,
                meteor.y,
                meteor.x - dx * meteor.length,
                meteor.y - dy * meteor.length,
                intArrayOf(
                    Color.argb(alpha, 255, 255, 255),
                    Color.argb((alpha * 0.75f).toInt(), 210, 232, 255),
                    Color.argb(0, 210, 232, 255)
                ),
                floatArrayOf(0f, 0.28f, 1f),
                Shader.TileMode.CLAMP
            )
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = 2.4f
            canvas.drawLine(meteor.x, meteor.y, meteor.x - dx * meteor.length, meteor.y - dy * meteor.length, paint)
            paint.shader = null
        }

        drawHills(canvas)

        if (message.isNotEmpty()) {
            val centerX = width / 2f
            val centerY = height * 0.56f + textSlideY + sin(textFloat) * 3f
            textPaint.alpha = (textAlpha * 255).toInt().coerceIn(0, 255)
            subTextPaint.alpha = (textAlpha * 235).toInt().coerceIn(0, 255)
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
        if (width == 0 || height == 0) return

        val progress = (frameCount / 82f).coerceIn(0f, 1f)
        val eased = 1f - (1f - progress) * (1f - progress) * (1f - progress)
        textAlpha = eased
        textSlideY = 32f * (1f - eased)
        textFloat += 0.024f

        stars.forEach { star ->
            star.twinkle += star.twinkleSpeed
        }

        val meteorInterval = (90 / animationSpeed).toInt().coerceIn(42, 120)
        if (frameCount % meteorInterval == 0 && meteors.size < 3) {
            meteors += createMeteor()
        }

        val removeList = mutableListOf<Meteor>()
        meteors.forEach { meteor ->
            meteor.x += cos(meteor.angle) * meteor.speed
            meteor.y += sin(meteor.angle) * meteor.speed
            meteor.life -= 1f
            if (meteor.life <= 0f || meteor.x < -meteor.length || meteor.y > height + meteor.length) {
                removeList += meteor
            }
        }
        meteors.removeAll(removeList)
    }
}
