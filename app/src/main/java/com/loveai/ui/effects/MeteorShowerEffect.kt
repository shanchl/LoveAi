package com.loveai.ui.effects

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import com.loveai.model.Effect
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class MeteorShowerEffect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseEffectView(context, attrs) {

    private data class Meteor(
        var x: Float,
        var y: Float,
        val angle: Float,
        val speed: Float,
        val length: Float,
        val thickness: Float,
        val color: Int,
        val layer: Int,
        var life: Float,
        val maxLife: Float
    )

    private data class Star(
        val x: Float,
        val y: Float,
        val size: Float,
        var twinkle: Float,
        val twinkleSpeed: Float,
        val alpha: Int
    )

    private val meteors = mutableListOf<Meteor>()
    private val stars = mutableListOf<Star>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var frameCount = 0
    private var textAlpha = 0f
    private var textOffset = 54f
    private var textFloat = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return

        backgroundPaint.shader = RadialGradient(
            w * 0.55f,
            h * 0.18f,
            h * 0.95f,
            intArrayOf(
                Color.parseColor("#23386A"),
                Color.parseColor("#0B1228"),
                Color.parseColor("#03050C")
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )

        stars.clear()
        repeat(120) {
            stars += Star(
                x = Random.nextFloat() * w,
                y = Random.nextFloat() * h,
                size = Random.nextFloat() * 2.6f + 0.5f,
                twinkle = Random.nextFloat() * Math.PI.toFloat() * 2f,
                twinkleSpeed = Random.nextFloat() * 0.045f + 0.01f,
                alpha = Random.nextInt(60, 180)
            )
        }

        meteors.clear()
        repeat((particleCount / 9).coerceIn(4, 10)) {
            meteors += createMeteor(w, h, true)
        }
    }

    override fun onEffectBound(effect: Effect) {
        frameCount = 0
        textAlpha = 0f
        textOffset = 54f
        textFloat = 0f
        textPaint.apply {
            color = Color.WHITE
            textSize = 44f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            textAlign = Paint.Align.CENTER
            setShadowLayer(14f, 0f, 3f, textGlowColor())
        }
        subTextPaint.apply {
            color = adjustAlpha(Color.WHITE, 230)
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
    }

    private fun createMeteor(w: Int, h: Int, randomPos: Boolean): Meteor {
        val layer = Random.nextInt(0, 3)
        val angle = Math.toRadians((110 + Random.nextInt(-7, 9)).toDouble()).toFloat()
        val length = when (layer) {
            0 -> Random.nextFloat() * 100f + 90f
            1 -> Random.nextFloat() * 140f + 140f
            else -> Random.nextFloat() * 180f + 210f
        }
        val speed = (Random.nextFloat() * 8f + 9f + layer * 4f) * animationSpeed
        val life = Random.nextFloat() * 55f + 42f + layer * 20f
        val palette = when (Random.nextInt(4)) {
            0 -> Color.WHITE
            1 -> Color.parseColor("#CDEBFF")
            2 -> Color.parseColor("#FFD6A8")
            else -> secondaryColor
        }
        return Meteor(
            x = if (randomPos) Random.nextFloat() * w * 1.2f + w * 0.1f else w + Random.nextFloat() * w * 0.35f,
            y = if (randomPos) Random.nextFloat() * h * 0.42f - h * 0.12f else Random.nextFloat() * h * 0.24f - h * 0.08f,
            angle = angle,
            speed = speed,
            length = length,
            thickness = Random.nextFloat() * 2.2f + 1.2f + layer * 0.5f,
            color = palette,
            layer = layer,
            life = life,
            maxLife = life
        )
    }

    private fun drawMeteor(canvas: Canvas, meteor: Meteor) {
        if (meteor.life <= 0f) return
        val progress = meteor.life / meteor.maxLife
        val alpha = ((0.18f + progress * 0.82f) * 255).toInt().coerceIn(0, 255)
        val dx = cos(meteor.angle)
        val dy = sin(meteor.angle)
        val tailX = meteor.x - dx * meteor.length
        val tailY = meteor.y - dy * meteor.length

        paint.shader = LinearGradient(
            meteor.x,
            meteor.y,
            tailX,
            tailY,
            intArrayOf(
                Color.argb(alpha, 255, 255, 255),
                Color.argb((alpha * 0.88f).toInt(), Color.red(meteor.color), Color.green(meteor.color), Color.blue(meteor.color)),
                Color.argb(0, Color.red(meteor.color), Color.green(meteor.color), Color.blue(meteor.color))
            ),
            floatArrayOf(0f, 0.26f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = meteor.thickness
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(meteor.x, meteor.y, tailX, tailY, paint)

        glowPaint.maskFilter = BlurMaskFilter(meteor.thickness * 5f, BlurMaskFilter.Blur.NORMAL)
        glowPaint.color = adjustAlpha(meteor.color, (alpha * 0.34f).toInt())
        canvas.drawCircle(meteor.x, meteor.y, meteor.thickness * 2.6f, glowPaint)

        glowPaint.maskFilter = null
        glowPaint.color = Color.argb(alpha, 255, 255, 255)
        canvas.drawCircle(meteor.x, meteor.y, meteor.thickness * 0.95f, glowPaint)

        paint.shader = null
    }

    override fun onDrawEffect(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        glowPaint.maskFilter = BlurMaskFilter(width * 0.11f, BlurMaskFilter.Blur.NORMAL)
        glowPaint.color = adjustAlpha(primaryColor, 26)
        canvas.drawCircle(width * 0.22f, height * 0.2f, width * 0.16f, glowPaint)
        glowPaint.color = adjustAlpha(secondaryColor, 20)
        canvas.drawCircle(width * 0.84f, height * 0.14f, width * 0.12f, glowPaint)
        glowPaint.maskFilter = null

        stars.forEach { star ->
            val brightness = (sin(star.twinkle) * 0.35f + 0.65f)
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.color = Color.argb((star.alpha * brightness).toInt().coerceIn(0, 255), 255, 255, 255)
            canvas.drawCircle(star.x, star.y, star.size * (0.8f + brightness * 0.4f), paint)
        }

        meteors.sortedBy { it.layer }.forEach { drawMeteor(canvas, it) }

        if (message.isNotEmpty()) {
            val centerX = width / 2f
            val centerY = height * 0.58f + textOffset + sin(textFloat) * 4f
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
        val w = width
        val h = height
        if (w == 0 || h == 0) return

        val progress = (frameCount / 84f).coerceIn(0f, 1f)
        val eased = 1f - (1f - progress) * (1f - progress)
        textAlpha = eased
        textOffset = 54f * (1f - eased)
        textFloat += 0.03f

        stars.forEach { it.twinkle += it.twinkleSpeed }

        val removeList = mutableListOf<Meteor>()
        meteors.forEach { meteor ->
            meteor.x += cos(meteor.angle) * meteor.speed
            meteor.y += sin(meteor.angle) * meteor.speed
            meteor.life -= 1f
            if (meteor.life <= 0f || meteor.x < -meteor.length || meteor.y > h + meteor.length) {
                removeList += meteor
            }
        }
        meteors.removeAll(removeList)

        val target = (particleCount / 9).coerceIn(4, 10)
        if (meteors.size < target && Random.nextFloat() < 0.18f) {
            meteors += createMeteor(w, h, false)
        }
    }
}
