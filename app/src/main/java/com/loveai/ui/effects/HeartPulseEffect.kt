package com.loveai.ui.effects

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import com.loveai.model.Effect
import kotlin.math.min
import kotlin.math.sin

/**
 * 第三轮精修：让心跳页更像“胸腔里的鼓点”，而不是单个心形缩放。
 */
class HeartPulseEffect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseEffectView(context, attrs) {

    private data class PulseRing(
        var radius: Float,
        var alpha: Float,
        var strokeWidth: Float
    )

    private val pulseRings = mutableListOf<PulseRing>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var frameCount = 0
    private var scale = 1f
    private var glowAlpha = 0f
    private var textAlpha = 0f
    private var beatMotion = 0f
    private var accentMotion = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bgPaint.shader = RadialGradient(
            w / 2f,
            h * 0.42f,
            maxOf(w, h) * 0.82f,
            intArrayOf(
                backgroundColor,
                adjustAlpha(primaryColor, 22),
                adjustAlpha(secondaryColor, 10)
            ),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onEffectBound(effect: Effect) {
        frameCount = 0
        scale = 1f
        glowAlpha = 0f
        textAlpha = 0f
        beatMotion = 0f
        accentMotion = 0f
        pulseRings.clear()

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
        return Path().apply {
            moveTo(cx, cy + size * 0.3f)
            cubicTo(cx - size, cy - size * 0.2f, cx - size, cy - size * 0.8f, cx, cy - size * 0.4f)
            cubicTo(cx + size, cy - size * 0.8f, cx + size, cy - size * 0.2f, cx, cy + size * 0.3f)
            close()
        }
    }

    override fun onDrawEffect(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val centerX = width / 2f
        val centerY = height * 0.4f
        val baseSize = min(width, height) * 0.18f

        pulseRings.forEach { ring ->
            glowPaint.color = primaryColor
            glowPaint.alpha = (ring.alpha * 125).toInt().coerceIn(0, 255)
            glowPaint.style = Paint.Style.STROKE
            glowPaint.strokeWidth = ring.strokeWidth
            glowPaint.maskFilter = BlurMaskFilter(16f, BlurMaskFilter.Blur.NORMAL)
            canvas.drawCircle(centerX, centerY, ring.radius, glowPaint)
        }
        glowPaint.maskFilter = null

        glowPaint.color = adjustAlpha(primaryColor, (glowAlpha * 90).toInt())
        glowPaint.style = Paint.Style.FILL
        glowPaint.maskFilter = BlurMaskFilter(72f, BlurMaskFilter.Blur.NORMAL)
        canvas.drawPath(buildHeartPath(centerX, centerY, baseSize * scale * 1.2f), glowPaint)
        glowPaint.maskFilter = null

        paint.color = secondaryColor
        paint.alpha = 90
        canvas.drawPath(buildHeartPath(centerX, centerY, baseSize * scale * 1.08f), paint)

        paint.color = primaryColor
        paint.alpha = 255
        canvas.drawPath(buildHeartPath(centerX, centerY, baseSize * scale), paint)

        paint.color = Color.argb(135, 255, 255, 255)
        canvas.drawCircle(centerX - baseSize * 0.22f, centerY - baseSize * 0.18f, baseSize * 0.11f, paint)

        if (message.isNotEmpty()) {
            val textScaleVal = 1f + beatMotion * 0.09f + accentMotion * 0.06f
            val messageY = height * 0.62f - beatMotion * 8f - accentMotion * 5f

            canvas.save()
            canvas.scale(textScaleVal, textScaleVal, centerX, messageY)
            textPaint.alpha = (textAlpha * 255).toInt()
            subTextPaint.alpha = (textAlpha * 235).toInt()
            drawContrastText(canvas, message, centerX, messageY, textPaint, ContrastTextType.MAIN)
            if (subMessage.isNotEmpty()) {
                drawContrastText(canvas, subMessage, centerX, messageY + 44f, subTextPaint, ContrastTextType.SUB)
            }
            canvas.restore()

            textPaint.alpha = 255
            subTextPaint.alpha = 255
        }
    }

    override fun onUpdateAnimation() {
        frameCount++

        textAlpha = (frameCount / 55f).coerceIn(0f, 1f)

        val beat = sin(frameCount * 0.14f).coerceAtLeast(0f)
        val accent = sin(frameCount * 0.28f).coerceAtLeast(0f)
        beatMotion = beat
        accentMotion = accent
        scale = 1f + beatMotion * 0.08f + accentMotion * 0.05f
        glowAlpha = (sin(frameCount * 0.08f) * 0.5f + 0.5f)

        if (frameCount % 18 == 0) {
            pulseRings.add(
                PulseRing(
                    radius = min(width, height) * 0.14f,
                    alpha = 1f,
                    strokeWidth = 4.2f
                )
            )
        }

        val maxRadius = min(width, height) * 0.62f
        val toRemove = mutableListOf<PulseRing>()
        pulseRings.forEach { ring ->
            ring.radius += 4.2f * animationSpeed
            ring.alpha -= 0.022f
            ring.strokeWidth = 5f * ring.alpha.coerceAtLeast(0.2f)
            if (ring.alpha <= 0f || ring.radius > maxRadius) {
                toRemove.add(ring)
            }
        }
        pulseRings.removeAll(toRemove)
    }
}
