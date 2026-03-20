package com.loveai.ui.effects

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import com.loveai.model.Effect
import kotlin.math.*
import kotlin.random.Random

/**
 * 效果5：泡泡上浮效果
 * 支持多种变体配置（颜色、数量等）
 */
class BubbleFloatEffect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseEffectView(context, attrs) {

    private data class Bubble(
        var x: Float,
        var y: Float,
        var radius: Float,
        var speedY: Float,
        var alpha: Int,
        var color: Int,
        var swingPhase: Float,
        var swingAmp: Float,
        var isHeart: Boolean,
        var rotation: Float = 0f,
        var rotSpeed: Float = 0f
    )

    private val bubbles = mutableListOf<Bubble>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var frameCount = 0
    private val bgPaint = Paint()
    private var textAlpha = 0f
    private var textBounceScale = 0f
    private var textBouncePhase = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // 使用变体颜色
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(
                backgroundColor,
                adjustAlpha(primaryColor, 0x1A),
                adjustAlpha(secondaryColor, 0x1A)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        
        bubbles.clear()
        val count = particleCount.coerceIn(15, 40)
        repeat(count) { bubbles.add(createBubble(w, h, true)) }
    }

    override fun onEffectBound(effect: Effect) {
        frameCount = 0
        textAlpha = 0f
        textBounceScale = 0.65f
        textBouncePhase = 0f
        textPaint.apply {
            color = Color.WHITE
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(8f, 0f, 2f, textGlowColor())
        }
        subTextPaint.apply {
            color = adjustAlpha(secondaryColor, 0xFF)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
    }

    private fun createBubble(w: Int, h: Int, randomY: Boolean = false): Bubble {
        val isHeart = Random.nextFloat() < 0.3f
        
        // 基于变体颜色
        val color = if (Random.nextBoolean()) primaryColor else secondaryColor
        
        return Bubble(
            x = Random.nextFloat() * w,
            y = if (randomY) Random.nextFloat() * h else h.toFloat() + 50f,
            radius = Random.nextFloat() * 30f + 10f,
            speedY = (Random.nextFloat() * 1.5f + 0.5f) * animationSpeed,
            alpha = Random.nextInt(80, 200),
            color = color,
            swingPhase = Random.nextFloat() * Math.PI.toFloat() * 2,
            swingAmp = Random.nextFloat() * 20f + 5f,
            isHeart = isHeart,
            rotSpeed = if (isHeart) (Random.nextFloat() - 0.5f) * 2f else 0f
        )
    }

    private fun buildHeartPath(size: Float): Path {
        val path = Path()
        path.moveTo(0f, size * 0.3f)
        path.cubicTo(-size * 1.0f, -size * 0.2f, -size * 1.0f, -size * 0.8f, 0f, -size * 0.4f)
        path.cubicTo(size * 1.0f, -size * 0.8f, size * 1.0f, -size * 0.2f, 0f, size * 0.3f)
        path.close()
        return path
    }

    override fun onDrawEffect(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        for (bubble in bubbles) {
            paint.alpha = bubble.alpha
            if (bubble.isHeart) {
                paint.color = bubble.color
                paint.style = Paint.Style.FILL
                canvas.save()
                canvas.translate(bubble.x, bubble.y)
                canvas.rotate(bubble.rotation)
                canvas.drawPath(buildHeartPath(bubble.radius), paint)
                canvas.restore()
            } else {
                paint.color = bubble.color
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2.5f
                canvas.drawCircle(bubble.x, bubble.y, bubble.radius, paint)

                paint.color = Color.WHITE
                paint.alpha = (bubble.alpha * 0.6f).toInt()
                paint.style = Paint.Style.FILL
                canvas.drawCircle(
                    bubble.x - bubble.radius * 0.3f,
                    bubble.y - bubble.radius * 0.3f,
                    bubble.radius * 0.2f, paint
                )
            }
        }

        // 弹跳出现文字效果
        if (message.isNotEmpty()) {
            val centerX = width / 2f
            val centerY = height * 0.65f

            canvas.save()
            canvas.translate(centerX, centerY)
            canvas.scale(textBounceScale, textBounceScale)
            canvas.translate(-centerX, -centerY)

            textPaint.alpha = (textAlpha * 255).toInt()
            subTextPaint.alpha = (textAlpha * 255).toInt()

            drawContrastText(canvas, message, centerX, centerY, textPaint, ContrastTextType.MAIN)
            if (subMessage.isNotEmpty()) {
                drawContrastText(canvas, subMessage, centerX, centerY + 44f, subTextPaint, ContrastTextType.SUB)
            }

            textPaint.alpha = 255
            subTextPaint.alpha = 255
            canvas.restore()
        }
    }

    override fun onUpdateAnimation() {
        frameCount++
        val w = width
        val h = height
        if (w == 0 || h == 0) return

        // 弹跳出现动画（前 60 帧）
        if (frameCount < 60) {
            val progress = (frameCount / 60f).coerceIn(0f, 1f)
            // 弹跳效果：overshoot 然后回弹
            if (progress < 0.5f) {
                // 弹起
                val t = progress / 0.5f
                textBounceScale = t * 1.15f
            } else {
                // 回落稳定
                val t = (progress - 0.5f) / 0.5f
                textBounceScale = 1.15f - 0.15f * t
            }
            textAlpha = (progress * 1.5f).coerceIn(0f, 1f)
        } else {
            textBounceScale = 1f
            textAlpha = 1f
        }
        textBouncePhase += 0.03f

        val targetCount = particleCount.coerceIn(15, 40)
        val toRemove = mutableListOf<Bubble>()
        for (bubble in bubbles) {
            bubble.y -= bubble.speedY
            bubble.x += sin((frameCount * 0.025f + bubble.swingPhase).toDouble()).toFloat() * bubble.swingAmp * 0.06f
            bubble.rotation += bubble.rotSpeed
            if (bubble.y < -bubble.radius * 2) toRemove.add(bubble)
        }
        bubbles.removeAll(toRemove)
        while (bubbles.size < targetCount) {
            bubbles.add(createBubble(w, h, false))
        }
    }
}
