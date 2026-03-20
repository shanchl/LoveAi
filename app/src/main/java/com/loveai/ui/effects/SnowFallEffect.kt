package com.loveai.ui.effects

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import com.loveai.model.Effect
import kotlin.math.*
import kotlin.random.Random

/**
 * 效果9：飘雪效果
 * 雪花从天空飘落，有大有小，旋转飘荡
 * 文字展示：闪烁星光效果（文字亮度周期变化）
 */
class SnowFallEffect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseEffectView(context, attrs) {

    private data class Snowflake(
        var x: Float,
        var y: Float,
        var size: Float,
        var speedY: Float,
        var speedX: Float,
        var alpha: Int,
        var rotation: Float,
        var rotSpeed: Float,
        var swingPhase: Float,
        var swingAmp: Float,
        var sparkle: Float  // 闪烁相位
    )

    private val snowflakes = mutableListOf<Snowflake>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var frameCount = 0
    private val bgPaint = Paint()
    private var textAlpha = 0f
    private var textTwinkle = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // 冬夜深蓝背景渐变
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(
                adjustAlpha(primaryColor, 0x15),
                adjustAlpha(secondaryColor, 0x10),
                backgroundColor
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )

        snowflakes.clear()
        val count = particleCount.coerceIn(30, 120)
        repeat(count) { snowflakes.add(createSnowflake(w, h, true)) }
    }

    override fun onEffectBound(effect: Effect) {
        textPaint.apply {
            color = Color.WHITE
            textSize = 44f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
            setShadowLayer(8f, 0f, 2f, textGlowColor())
        }
        subTextPaint.apply {
            color = adjustAlpha(secondaryColor, 0xEE)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
    }

    private fun createSnowflake(w: Int, h: Int, randomY: Boolean = false): Snowflake {
        val size = Random.nextFloat() * 12f + 3f
        return Snowflake(
            x = Random.nextFloat() * w,
            y = if (randomY) Random.nextFloat() * h else -size * 3,
            size = size,
            speedY = (Random.nextFloat() * 1.5f + 0.5f) * animationSpeed * (size / 10f),
            speedX = (Random.nextFloat() - 0.5f) * 0.8f * animationSpeed,
            alpha = Random.nextInt(100, 230),
            rotation = Random.nextFloat() * 360f,
            rotSpeed = (Random.nextFloat() - 0.5f) * 2f * animationSpeed,
            swingPhase = Random.nextFloat() * Math.PI.toFloat() * 2,
            swingAmp = Random.nextFloat() * 25f + 8f,
            sparkle = Random.nextFloat() * Math.PI.toFloat() * 2
        )
    }

    private fun drawSnowflake(canvas: Canvas, sf: Snowflake) {
        canvas.save()
        canvas.translate(sf.x, sf.y)
        canvas.rotate(sf.rotation)

        paint.color = Color.WHITE
        paint.alpha = sf.alpha
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1f, sf.size / 6f)

        val s = sf.size

        // 绘制六角雪花
        for (i in 0 until 6) {
            val angle = i * 60f
            val rad = Math.toRadians(angle.toDouble())
            val endX = cos(rad).toFloat() * s
            val endY = sin(rad).toFloat() * s
            canvas.drawLine(0f, 0f, endX, endY, paint)

            // 分支
            if (s > 6f) {
                val branchLen = s * 0.4f
                val midX = endX * 0.6f
                val midY = endY * 0.6f
                val branchAngle1 = rad + Math.toRadians(30.0)
                val branchAngle2 = rad - Math.toRadians(30.0)
                canvas.drawLine(midX, midY,
                    midX + cos(branchAngle1).toFloat() * branchLen,
                    midY + sin(branchAngle1).toFloat() * branchLen, paint)
                canvas.drawLine(midX, midY,
                    midX + cos(branchAngle2).toFloat() * branchLen,
                    midY + sin(branchAngle2).toFloat() * branchLen, paint)
            }
        }

        // 中心小点
        paint.style = Paint.Style.FILL
        canvas.drawCircle(0f, 0f, max(1f, sf.size / 8f), paint)

        canvas.restore()
    }

    override fun onDrawEffect(canvas: Canvas) {
        // 深色背景（带微弱残影营造朦胧感）
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 绘制雪花
        for (sf in snowflakes) {
            drawSnowflake(canvas, sf)
        }

        // 文字带闪烁星光效果
        if (message.isNotEmpty()) {
            val centerX = width / 2f
            val centerY = height * 0.55f

            // 闪烁效果：shadowLayer 强度周期变化
            val twinkleIntensity = (sin(textTwinkle) * 0.3f + 0.7f)
            val glowRadius = 8f + sin(textTwinkle * 1.5f) * 6f
            textPaint.setShadowLayer(glowRadius, 0f, 0f, adjustAlpha(primaryColor, (200 * twinkleIntensity).toInt()))
            textPaint.alpha = (textAlpha * 255 * twinkleIntensity).toInt().coerceIn(0, 255)
            subTextPaint.alpha = (textAlpha * 255).toInt()

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

        // 文字淡入
        if (frameCount < 60) {
            textAlpha = (frameCount / 60f).coerceIn(0f, 1f)
        } else {
            textAlpha = 1f
        }
        // 闪烁相位
        textTwinkle += 0.04f

        val targetCount = particleCount.coerceIn(30, 120)
        val toRemove = mutableListOf<Snowflake>()

        for (sf in snowflakes) {
            sf.y += sf.speedY
            sf.x += sf.speedX + sin((frameCount * 0.02f + sf.swingPhase).toDouble()).toFloat() * sf.swingAmp * 0.04f
            sf.rotation += sf.rotSpeed
            sf.sparkle += 0.05f

            if (sf.y > h + sf.size * 3) {
                toRemove.add(sf)
            }
        }
        snowflakes.removeAll(toRemove)

        while (snowflakes.size < targetCount) {
            snowflakes.add(createSnowflake(w, h, false))
        }
    }
}
