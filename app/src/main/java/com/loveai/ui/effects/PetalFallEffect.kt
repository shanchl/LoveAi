package com.loveai.ui.effects

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import com.loveai.model.Effect
import kotlin.math.*
import kotlin.random.Random

/**
 * 效果4：花瓣飘散效果
 * 支持多种变体配置（颜色、数量等）
 */
class PetalFallEffect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseEffectView(context, attrs) {

    private data class Petal(
        var x: Float,
        var y: Float,
        var size: Float,
        var speedY: Float,
        var speedX: Float,
        var rotation: Float,
        var rotSpeed: Float,
        var alpha: Int,
        var swingPhase: Float,
        var color: Int,
        var scaleX: Float = 1f,
        var scaleXDir: Float = 1f
    )

    private val petals = mutableListOf<Petal>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var frameCount = 0
    private val bgPaint = Paint()
    private var displayCharCount = 0   // 逐字展开显示的字符数
    private var subTextAlpha = 0f      // 副标题透明度

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // 使用变体的颜色
        bgPaint.shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(
                adjustAlpha(primaryColor, 0xF5),
                adjustAlpha(primaryColor, 0xE4),
                adjustAlpha(secondaryColor, 0xD6),
                adjustAlpha(secondaryColor, 0xCD)
            ),
            floatArrayOf(0f, 0.33f, 0.66f, 1f),
            Shader.TileMode.CLAMP
        )
        
        petals.clear()
        val count = particleCount.coerceIn(20, 60)
        repeat(count) { petals.add(createPetal(w, h, true)) }
    }

    override fun onEffectBound(effect: Effect) {
        textPaint.apply {
            color = Color.WHITE
            textSize = 44f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            setShadowLayer(8f, 0f, 2f, textGlowColor())
        }
        subTextPaint.apply {
            color = Color.WHITE
            textSize = 30f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
    }

    private fun createPetal(w: Int, h: Int, randomY: Boolean = false): Petal {
        // 基于变体颜色生成花瓣颜色
        val r = Color.red(primaryColor)
        val g = Color.green(primaryColor)
        val b = Color.blue(primaryColor)
        val color = Color.argb(
            Random.nextInt(150, 240),
            (r + Random.nextInt(-20, 20)).coerceIn(0, 255),
            (g + Random.nextInt(-20, 20)).coerceIn(0, 255),
            (b + Random.nextInt(-20, 20)).coerceIn(0, 255)
        )
        
        return Petal(
            x = Random.nextFloat() * w * 1.2f - w * 0.1f,
            y = if (randomY) Random.nextFloat() * h else -30f,
            size = Random.nextFloat() * 20f + 8f,
            speedY = (Random.nextFloat() * 1.8f + 0.6f) * animationSpeed,
            speedX = (Random.nextFloat() - 0.5f) * 1.2f * animationSpeed,
            rotation = Random.nextFloat() * 360f,
            rotSpeed = (Random.nextFloat() - 0.5f) * 3f * animationSpeed,
            alpha = Random.nextInt(150, 240),
            swingPhase = Random.nextFloat() * Math.PI.toFloat() * 2,
            color = color
        )
    }

    private fun drawPetal(canvas: Canvas, petal: Petal) {
        canvas.save()
        canvas.translate(petal.x, petal.y)
        canvas.rotate(petal.rotation)
        canvas.scale(petal.scaleX, 1f)

        paint.color = petal.color
        paint.alpha = petal.alpha
        paint.style = Paint.Style.FILL

        val petalPath = Path()
        val s = petal.size
        petalPath.moveTo(0f, -s)
        petalPath.cubicTo(s * 0.8f, -s * 0.5f, s * 0.8f, s * 0.5f, 0f, s * 0.3f)
        petalPath.cubicTo(-s * 0.8f, s * 0.5f, -s * 0.8f, -s * 0.5f, 0f, -s)
        petalPath.close()
        canvas.drawPath(petalPath, paint)

        canvas.restore()
    }

    override fun onDrawEffect(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        for (petal in petals) {
            drawPetal(canvas, petal)
        }

        // 逐字展开文字效果
        if (message.isNotEmpty()) {
            val centerX = width / 2f
            val showText = message.substring(0, displayCharCount.coerceAtMost(message.length))

            if (showText.isNotEmpty()) {
                drawContrastText(canvas, showText, centerX, height * 0.58f, textPaint, ContrastTextType.MAIN)
            }

            // 主标题显示完后，副标题淡入
            if (subMessage.isNotEmpty() && displayCharCount > message.length) {
                subTextPaint.alpha = (subTextAlpha * 255).toInt()
                drawContrastText(canvas, subMessage, centerX, height * 0.58f + 46f, subTextPaint, ContrastTextType.SUB)
                subTextPaint.alpha = 255
            }
        }
    }

    override fun onUpdateAnimation() {
        frameCount++
        val w = width
        val h = height
        if (w == 0 || h == 0) return

        // 逐字展开动画
        val expandSpeed = 6 / animationSpeed
        if (frameCount % expandSpeed.toInt().coerceIn(2, 8) == 0) {
            if (displayCharCount <= message.length) {
                displayCharCount++
            }
        }
        // 副标题淡入
        if (displayCharCount > message.length) {
            subTextAlpha = ((displayCharCount - message.length) / 15f).coerceIn(0f, 1f)
        }

        val targetCount = particleCount.coerceIn(20, 60)
        val toRemove = mutableListOf<Petal>()
        for (petal in petals) {
            petal.y += petal.speedY
            petal.x += petal.speedX + sin((frameCount * 0.02f + petal.swingPhase).toDouble()).toFloat() * 0.8f
            petal.rotation += petal.rotSpeed
            petal.scaleX += petal.scaleXDir * 0.02f
            if (petal.scaleX > 1f) { petal.scaleX = 1f; petal.scaleXDir = -1f }
            if (petal.scaleX < -1f) { petal.scaleX = -1f; petal.scaleXDir = 1f }

            if (petal.y > h + 50f) toRemove.add(petal)
        }
        petals.removeAll(toRemove)
        while (petals.size < targetCount) {
            petals.add(createPetal(w, h, false))
        }
    }
}
