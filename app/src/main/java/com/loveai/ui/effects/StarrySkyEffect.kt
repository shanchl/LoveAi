package com.loveai.ui.effects

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import com.loveai.model.Effect
import kotlin.math.*
import kotlin.random.Random

/**
 * 效果3：星空流星效果
 * 支持多种变体配置（星星数量、颜色等）
 */
class StarrySkyEffect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseEffectView(context, attrs) {

    private data class Star(
        val x: Float,
        val y: Float,
        val radius: Float,
        var alpha: Float,
        var alphaDir: Float,
        val twinkleSpeed: Float
    )

    private data class Meteor(
        var x: Float,
        var y: Float,
        var length: Float,
        var speed: Float,
        var alpha: Float,
        var angle: Float,
        var life: Float,
        var maxLife: Float
    )

    private val stars = mutableListOf<Star>()
    private val meteors = mutableListOf<Meteor>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var frameCount = 0
    private val bgPaint = Paint()
    private var textSlideX = 0f      // 水平滚动偏移
    private var textAlpha = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // 使用变体的背景色
        val bgColors = intArrayOf(
            backgroundColor,
            adjustAlpha(primaryColor, 0x1A),
            adjustAlpha(secondaryColor, 0x0D)
        )
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            bgColors,
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        // 初始化星星 - 使用变体的粒子数量
        stars.clear()
        val starCount = particleCount.coerceIn(80, 300)
        repeat(starCount) {
            stars.add(
                Star(
                    x = Random.nextFloat() * w,
                    y = Random.nextFloat() * h,
                    radius = Random.nextFloat() * 2.5f + 0.5f,
                    alpha = Random.nextFloat(),
                    alphaDir = if (Random.nextBoolean()) 1f else -1f,
                    twinkleSpeed = Random.nextFloat() * 0.02f + 0.005f
                )
            )
        }
    }

    override fun onEffectBound(effect: Effect) {
        meteors.clear()
        frameCount = 0
        textSlideX = 0f
        textAlpha = 0f
        textPaint.apply {
            color = Color.WHITE
            textSize = 50f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
            setShadowLayer(12f, 0f, 0f, textGlowColor())
        }
        subTextPaint.apply {
            color = adjustAlpha(secondaryColor, 0xDD)
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.08f  // 字间距
        }
    }

    private fun createMeteor(): Meteor {
        val angle = Random.nextFloat() * 30f + 20f
        val life = Random.nextFloat() * 60f + 40f
        return Meteor(
            x = Random.nextFloat() * width * 1.5f - width * 0.25f,
            y = Random.nextFloat() * height * 0.5f,
            length = Random.nextFloat() * 180f + 80f,
            speed = (Random.nextFloat() * 15f + 8f) * animationSpeed,
            alpha = 1f,
            angle = angle,
            life = life,
            maxLife = life
        )
    }

    override fun onDrawEffect(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 绘制星星
        for (star in stars) {
            paint.color = Color.WHITE
            paint.alpha = (star.alpha * 200).toInt().coerceIn(30, 200)
            paint.style = Paint.Style.FILL
            if (star.radius > 1.8f) {
                paint.maskFilter = BlurMaskFilter(star.radius * 2f, BlurMaskFilter.Blur.NORMAL)
            } else {
                paint.maskFilter = null
            }
            canvas.drawCircle(star.x, star.y, star.radius, paint)
        }
        paint.maskFilter = null

        // 绘制流星
        for (meteor in meteors) {
            if (meteor.life <= 0) continue
            val alpha = (meteor.alpha * (meteor.life / meteor.maxLife) * 255).toInt()
            val angleRad = Math.toRadians(meteor.angle.toDouble())
            val dx = cos(angleRad).toFloat()
            val dy = sin(angleRad).toFloat()

            val gradient = LinearGradient(
                meteor.x, meteor.y,
                meteor.x - dx * meteor.length,
                meteor.y - dy * meteor.length,
                intArrayOf(
                    Color.argb(alpha, 255, 255, 255),
                    adjustAlpha(primaryColor, 0)
                ),
                null,
                Shader.TileMode.CLAMP
            )
            paint.shader = gradient
            paint.strokeWidth = 2.5f
            paint.style = Paint.Style.STROKE
            canvas.drawLine(
                meteor.x, meteor.y,
                meteor.x - dx * meteor.length,
                meteor.y - dy * meteor.length,
                paint
            )
            paint.shader = null
        }

        if (message.isNotEmpty()) {
            val centerX = width / 2f

            // 水平滚动入场动画
            textPaint.alpha = (textAlpha * 255).toInt()
            subTextPaint.alpha = (textAlpha * 255).toInt()

            drawContrastText(canvas, message, centerX + textSlideX, height * 0.55f, textPaint, ContrastTextType.MAIN)
            if (subMessage.isNotEmpty()) {
                drawContrastText(canvas, subMessage, centerX + textSlideX * 0.6f, height * 0.55f + 50f, subTextPaint, ContrastTextType.SUB)
            }

            textPaint.alpha = 255
            subTextPaint.alpha = 255
        }
    }

    override fun onUpdateAnimation() {
        frameCount++
        if (width == 0 || height == 0) return

        // 水平滚动入场动画（前 80 帧）
        if (frameCount < 80) {
            val progress = (frameCount / 80f).coerceIn(0f, 1f)
            // easeOutQuart 缓动
            val eased = 1f - (1f - progress).let { it * it * it * it }
            textSlideX = (width.toFloat() + 100f) * (1f - eased)
            textAlpha = (progress * 2f).coerceIn(0f, 1f)
        } else {
            textSlideX = 0f
            textAlpha = 1f
        }

        for (star in stars) {
            star.alpha += star.alphaDir * star.twinkleSpeed
            if (star.alpha >= 1f) { star.alpha = 1f; star.alphaDir = -1f }
            if (star.alpha <= 0f) { star.alpha = 0f; star.alphaDir = 1f }
        }

        val meteorInterval = (80 / animationSpeed).toInt().coerceIn(40, 120)
        if (frameCount % meteorInterval == 0 && meteors.size < 4) {
            meteors.add(createMeteor())
        }

        val toRemove = mutableListOf<Meteor>()
        for (meteor in meteors) {
            val angleRad = Math.toRadians(meteor.angle.toDouble())
            meteor.x += cos(angleRad).toFloat() * meteor.speed
            meteor.y += sin(angleRad).toFloat() * meteor.speed
            meteor.life -= 1f
            if (meteor.life <= 0 || meteor.x > width + 200 || meteor.y > height + 200) {
                toRemove.add(meteor)
            }
        }
        meteors.removeAll(toRemove)
    }
}
