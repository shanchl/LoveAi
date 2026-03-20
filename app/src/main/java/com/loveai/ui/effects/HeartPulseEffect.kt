package com.loveai.ui.effects

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import com.loveai.model.Effect
import kotlin.math.*

/**
 * 效果7：爱心脉冲效果
 * 支持多种变体配置（颜色、速度等）
 */
class HeartPulseEffect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseEffectView(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var frameCount = 0
    private var scale = 1f
    private var scaleDir = 1f
    private var glowAlpha = 0f

    private val bgPaint = Paint()
    private val heartPath = Path()

    private data class PulseRing(
        var radius: Float,
        var alpha: Float,
        var maxRadius: Float
    )
    private val pulseRings = mutableListOf<PulseRing>()
    private var textAlpha = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bgPaint.shader = RadialGradient(
            w / 2f, h * 0.45f, max(w, h) * 0.8f,
            intArrayOf(
                backgroundColor,
                adjustAlpha(primaryColor, 0x1A),
                adjustAlpha(secondaryColor, 0x0D)
            ),
            null,
            Shader.TileMode.CLAMP
        )
    }

    override fun onEffectBound(effect: Effect) {
        textPaint.apply {
            color = Color.WHITE
            textSize = 40f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.05f
            setShadowLayer(10f, 0f, 0f, textGlowColor())
        }
        subTextPaint.apply {
            color = adjustAlpha(secondaryColor, 0xFF)
            textSize = 26f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
    }

    private fun buildHeartPath(cx: Float, cy: Float, size: Float): Path {
        val path = Path()
        path.moveTo(cx, cy + size * 0.3f)
        path.cubicTo(cx - size * 1.0f, cy - size * 0.2f, cx - size * 1.0f, cy - size * 0.8f, cx, cy - size * 0.4f)
        path.cubicTo(cx + size * 1.0f, cy - size * 0.8f, cx + size * 1.0f, cy - size * 0.2f, cx, cy + size * 0.3f)
        path.close()
        return path
    }

    override fun onDrawEffect(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val centerX = width / 2f
        val centerY = height * 0.4f
        val baseSize = min(width, height) * 0.18f

        // 绘制脉冲环
        for (ring in pulseRings) {
            glowPaint.color = primaryColor
            glowPaint.alpha = (ring.alpha * 100).toInt().coerceIn(0, 255)
            glowPaint.style = Paint.Style.STROKE
            glowPaint.strokeWidth = 3f
            glowPaint.maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
            canvas.drawCircle(centerX, centerY, ring.radius, glowPaint)
        }
        glowPaint.maskFilter = null

        // 绘制外发光
        glowPaint.color = primaryColor
        glowPaint.alpha = (glowAlpha * 80).toInt().coerceIn(0, 255)
        glowPaint.style = Paint.Style.FILL
        glowPaint.maskFilter = BlurMaskFilter(60f, BlurMaskFilter.Blur.NORMAL)
        canvas.drawPath(buildHeartPath(centerX, centerY, baseSize * scale * 1.2f), glowPaint)
        glowPaint.maskFilter = null

        // 绘制主爱心
        paint.color = primaryColor
        paint.style = Paint.Style.FILL
        canvas.drawPath(buildHeartPath(centerX, centerY, baseSize * scale), paint)

        // 绘制爱心高光
        paint.color = secondaryColor
        paint.alpha = 120
        val highlightSize = baseSize * scale * 0.3f
        canvas.drawCircle(centerX - baseSize * scale * 0.3f, centerY - baseSize * scale * 0.25f, highlightSize * 0.4f, paint)

        if (message.isNotEmpty()) {
            // 文字随心跳脉动缩放
            val textScaleVal = 1f + (scale - 1f) * 0.3f  // 跟随心跳，幅度减小
            val centerX = width / 2f
            val centerY = height * 0.62f

            canvas.save()
            canvas.translate(centerX, centerY)
            canvas.scale(textScaleVal, textScaleVal)
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

        // 文字淡入（前 50 帧）
        if (frameCount < 50) {
            textAlpha = (frameCount / 50f).coerceIn(0f, 1f)
        } else {
            textAlpha = 1f
        }

        // 爱心脉冲缩放 - 使用变体速度
        scale += scaleDir * 0.012f * animationSpeed
        if (scale > 1.15f) {
            scaleDir = -1f
            pulseRings.add(PulseRing(radius = min(width, height) * 0.18f, alpha = 1f, maxRadius = min(width, height) * 0.6f))
        }
        if (scale < 0.85f) {
            scaleDir = 1f
            pulseRings.add(PulseRing(radius = min(width, height) * 0.18f, alpha = 1f, maxRadius = min(width, height) * 0.6f))
        }

        glowAlpha = (sin(frameCount * 0.08f) + 1f) / 2f

        val toRemove = mutableListOf<PulseRing>()
        for (ring in pulseRings) {
            ring.radius += 3f * animationSpeed
            ring.alpha -= 0.015f
            if (ring.alpha <= 0 || ring.radius > ring.maxRadius) {
                toRemove.add(ring)
            }
        }
        pulseRings.removeAll(toRemove)
    }
}
