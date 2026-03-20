package com.loveai.ui.effects

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import com.loveai.model.Effect
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 第三轮精修：加入升空拖尾、二次爆闪和残留火星，让烟花更像演出而不是粒子散开。
 */
class FireworkEffect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseEffectView(context, attrs) {

    private data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var color: Int,
        var alpha: Float,
        var size: Float,
        var life: Float,
        var maxLife: Float
    )

    private data class BurstRing(
        var radius: Float,
        var alpha: Float,
        val color: Int
    )

    private data class Firework(
        var x: Float,
        var y: Float,
        var targetY: Float,
        var vy: Float,
        var color: Int,
        var exploded: Boolean = false,
        val particles: MutableList<Particle> = mutableListOf(),
        val rings: MutableList<BurstRing> = mutableListOf()
    )

    private val fireworks = mutableListOf<Firework>()
    private val stars = mutableListOf<Pair<Float, Float>>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var frameCount = 0
    private var textAlpha = 0f
    private var textScale = 0.75f
    private var glowPhase = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        bgPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h.toFloat(),
            intArrayOf(
                Color.parseColor("#120812"),
                adjustAlpha(primaryColor, 18),
                backgroundColor
            ),
            floatArrayOf(0f, 0.28f, 1f),
            Shader.TileMode.CLAMP
        )

        stars.clear()
        repeat(36) {
            stars.add(Random.nextFloat() * w to Random.nextFloat() * h * 0.45f)
        }
    }

    override fun onEffectBound(effect: Effect) {
        fireworks.clear()
        frameCount = 0
        textAlpha = 0f
        textScale = 0.75f
        glowPhase = 0f

        textPaint.apply {
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(12f, 0f, 0f, textGlowColor())
        }
        subTextPaint.apply {
            color = adjustAlpha(secondaryColor, 0xFF)
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
    }

    private fun variantColor(): Int {
        return if (Random.nextBoolean()) primaryColor else secondaryColor
    }

    private fun launchFirework() {
        if (width == 0 || height == 0) return
        fireworks.add(
            Firework(
                x = Random.nextFloat() * width * 0.7f + width * 0.15f,
                y = height.toFloat() + 40f,
                targetY = Random.nextFloat() * height * 0.35f + height * 0.12f,
                vy = -(Random.nextFloat() * 10f + 12f) * animationSpeed,
                color = variantColor()
            )
        )
    }

    private fun explodeFirework(firework: Firework) {
        val count = particleCount.coerceIn(44, 140)
        repeat(count) {
            val angle = Random.nextFloat() * (Math.PI * 2).toFloat()
            val speed = Random.nextFloat() * 7.5f + 2.5f
            val life = Random.nextFloat() * 54f + 42f
            firework.particles.add(
                Particle(
                    x = firework.x,
                    y = firework.y,
                    vx = cos(angle) * speed * animationSpeed,
                    vy = sin(angle) * speed * animationSpeed,
                    color = firework.color,
                    alpha = 1f,
                    size = Random.nextFloat() * 4.5f + 1.6f,
                    life = life,
                    maxLife = life
                )
            )
        }

        firework.rings.add(BurstRing(radius = 0f, alpha = 1f, color = firework.color))
        firework.rings.add(BurstRing(radius = 12f, alpha = 0.8f, color = Color.WHITE))
        firework.exploded = true
    }

    override fun onDrawEffect(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        stars.forEach { (x, y) ->
            val alpha = ((sin(frameCount * 0.03f + x * 0.01f) * 0.5f + 0.5f) * 130).toInt() + 30
            paint.color = Color.argb(alpha, 255, 255, 255)
            canvas.drawCircle(x, y, 1.4f, paint)
        }

        fireworks.forEach { fw ->
            if (!fw.exploded) {
                glowPaint.color = adjustAlpha(fw.color, 140)
                glowPaint.maskFilter = BlurMaskFilter(16f, BlurMaskFilter.Blur.NORMAL)
                canvas.drawCircle(fw.x, fw.y, 5.5f, glowPaint)
                glowPaint.maskFilter = null

                paint.color = fw.color
                paint.strokeWidth = 2.4f
                paint.style = Paint.Style.STROKE
                canvas.drawLine(fw.x, fw.y, fw.x, fw.y + 36f, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(fw.x, fw.y, 3.6f, paint)
            } else {
                fw.rings.forEach { ring ->
                    paint.color = ring.color
                    paint.alpha = (ring.alpha * 180).toInt().coerceIn(0, 255)
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2.8f
                    canvas.drawCircle(fw.x, fw.y, ring.radius, paint)
                }

                fw.particles.forEach { p ->
                    if (p.life <= 0f) return@forEach
                    glowPaint.color = adjustAlpha(p.color, (p.alpha * 120).toInt().coerceAtMost(120))
                    glowPaint.maskFilter = BlurMaskFilter(p.size * 3f, BlurMaskFilter.Blur.NORMAL)
                    canvas.drawCircle(p.x, p.y, p.size * 1.6f, glowPaint)
                    glowPaint.maskFilter = null

                    paint.color = p.color
                    paint.alpha = (p.alpha * 255).toInt().coerceIn(0, 255)
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(p.x, p.y, p.size * (p.life / p.maxLife).coerceAtLeast(0.35f), paint)
                }
            }
        }

        if (message.isNotEmpty()) {
            val centerX = width / 2f
            val centerY = height * 0.69f

            canvas.save()
            canvas.scale(textScale, textScale, centerX, centerY)
            val glow = (sin(glowPhase) * 0.5f + 0.5f) * 14f + 6f
            textPaint.setShadowLayer(glow, 0f, 0f, textGlowColor())
            textPaint.alpha = (textAlpha * 255).toInt()
            subTextPaint.alpha = (textAlpha * 240).toInt()

            drawContrastText(canvas, message, centerX, centerY, textPaint, ContrastTextType.MAIN)
            if (subMessage.isNotEmpty()) {
                drawContrastText(canvas, subMessage, centerX, centerY + 48f, subTextPaint, ContrastTextType.SUB)
            }
            canvas.restore()

            textPaint.alpha = 255
            subTextPaint.alpha = 255
        }
    }

    override fun onUpdateAnimation() {
        frameCount++
        if (width == 0 || height == 0) return

        if (frameCount < 52) {
            val progress = (frameCount / 52f).coerceIn(0f, 1f)
            val t = progress - 1f
            val eased = t * t * ((1.8f + 1f) * t + 1.8f) + 1f
            textScale = 0.75f + 0.25f * eased
            textAlpha = (progress * 1.3f).coerceIn(0f, 1f)
        } else {
            textScale = 1f + sin((frameCount - 52) * 0.025f) * 0.02f
            textAlpha = 1f
        }
        glowPhase += 0.04f

        val maxFireworks = (particleCount / 15).coerceIn(3, 9)
        val launchInterval = (36 / animationSpeed).toInt().coerceIn(18, 52)
        if (frameCount % launchInterval == 0 && fireworks.size < maxFireworks) {
            launchFirework()
        }

        val toRemove = mutableListOf<Firework>()
        fireworks.forEach { fw ->
            if (!fw.exploded) {
                fw.y += fw.vy
                if (fw.y <= fw.targetY) {
                    explodeFirework(fw)
                }
            } else {
                fw.rings.forEach { ring ->
                    ring.radius += 4.2f * animationSpeed
                    ring.alpha -= 0.03f
                }

                var allDead = true
                fw.particles.forEach { p ->
                    if (p.life <= 0f) return@forEach
                    allDead = false
                    p.x += p.vx
                    p.y += p.vy
                    p.vy += 0.12f
                    p.vx *= 0.985f
                    p.vy *= 0.985f
                    p.life -= 1f
                    p.alpha = p.life / p.maxLife
                }

                fw.rings.removeAll { it.alpha <= 0f }
                if (allDead && fw.rings.isEmpty()) {
                    toRemove.add(fw)
                }
            }
        }
        fireworks.removeAll(toRemove)
    }
}
