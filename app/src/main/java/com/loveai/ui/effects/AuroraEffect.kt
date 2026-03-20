package com.loveai.ui.effects

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import com.loveai.model.Effect
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * 第三轮精修：补齐极光的层叠、漂移和地平线剪影，让它更像一张活的海报。
 */
class AuroraEffect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseEffectView(context, attrs) {

    private data class AuroraRay(
        var x: Float,
        val width: Float,
        val height: Float,
        var phase: Float,
        val phaseSpeed: Float,
        val waveAmplitude: Float,
        val waveFreq: Float,
        val color: Int,
        var alpha: Float,
        val alphaBase: Float
    )

    private data class Star(
        val x: Float,
        val y: Float,
        val size: Float,
        var twinkle: Float,
        val twinkleSpeed: Float
    )

    private val rays = mutableListOf<AuroraRay>()
    private val stars = mutableListOf<Star>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val horizonPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var frameCount = 0
    private var textAlpha = 0f
    private var textLift = 24f

    private val auroraColorSets = listOf(
        intArrayOf(Color.parseColor("#1CF4A9"), Color.parseColor("#33E8FF"), Color.parseColor("#72A7FF")),
        intArrayOf(Color.parseColor("#6DE2FF"), Color.parseColor("#B084FF"), Color.parseColor("#F27AE5")),
        intArrayOf(Color.parseColor("#80FFDB"), Color.parseColor("#48CAE4"), Color.parseColor("#C77DFF"))
    )

    private val selectedColors: IntArray by lazy {
        auroraColorSets[abs(primaryColor.hashCode()) % auroraColorSets.size]
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        bgPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h.toFloat(),
            intArrayOf(
                Color.parseColor("#040915"),
                Color.parseColor("#071325"),
                Color.parseColor("#02060F")
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )

        horizonPaint.shader = RadialGradient(
            w / 2f,
            h * 0.76f,
            w * 0.7f,
            intArrayOf(
                adjustAlpha(primaryColor, 42),
                adjustAlpha(secondaryColor, 24),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP
        )

        stars.clear()
        repeat(180) {
            stars.add(
                Star(
                    x = Random.nextFloat() * w,
                    y = Random.nextFloat() * h * 0.62f,
                    size = Random.nextFloat() * 2f + 0.3f,
                    twinkle = Random.nextFloat() * (Math.PI * 2).toFloat(),
                    twinkleSpeed = Random.nextFloat() * 0.03f + 0.006f
                )
            )
        }

        rays.clear()
        repeat(7) { index ->
            val color = selectedColors[index % selectedColors.size]
            rays.add(
                AuroraRay(
                    x = w * (index + 0.5f) / 7f,
                    width = w * (Random.nextFloat() * 0.22f + 0.26f),
                    height = h * (Random.nextFloat() * 0.26f + 0.42f),
                    phase = Random.nextFloat() * (Math.PI * 2).toFloat(),
                    phaseSpeed = (Random.nextFloat() * 0.009f + 0.004f) * animationSpeed,
                    waveAmplitude = w * (Random.nextFloat() * 0.045f + 0.03f),
                    waveFreq = Random.nextFloat() * 1.5f + 1.4f,
                    color = color,
                    alpha = Random.nextFloat() * 0.24f + 0.1f,
                    alphaBase = Random.nextFloat() * 0.18f + 0.15f
                )
            )
        }
    }

    override fun onEffectBound(effect: Effect) {
        frameCount = 0
        textAlpha = 0f
        textLift = 24f

        textPaint.apply {
            color = Color.WHITE
            textSize = 44f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
            setShadowLayer(14f, 0f, 3f, textGlowColor())
        }
        subTextPaint.apply {
            color = adjustAlpha(secondaryColor, 0xD0)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
    }

    private fun drawAuroraRay(canvas: Canvas, ray: AuroraRay, viewHeight: Int) {
        val topY = viewHeight * 0.04f
        val bottomY = topY + ray.height
        val segments = 28
        val leftPoints = mutableListOf<PointF>()
        val rightPoints = mutableListOf<PointF>()

        repeat(segments + 1) { i ->
            val t = i / segments.toFloat()
            val y = topY + (bottomY - topY) * t
            val drift = sin(ray.phase + t * ray.waveFreq * (Math.PI * 2).toFloat()) * ray.waveAmplitude * t
            val centerX = ray.x + drift
            val halfWidth = ray.width * 0.5f * (1f - t * 0.34f)
            leftPoints.add(PointF(centerX - halfWidth, y))
            rightPoints.add(PointF(centerX + halfWidth, y))
        }

        val shape = Path().apply {
            moveTo(leftPoints.first().x, leftPoints.first().y)
            leftPoints.forEach { lineTo(it.x, it.y) }
            rightPoints.asReversed().forEach { lineTo(it.x, it.y) }
            close()
        }

        val alpha = (ray.alpha * 255).toInt().coerceIn(0, 255)
        paint.shader = LinearGradient(
            0f,
            topY,
            0f,
            bottomY,
            intArrayOf(
                Color.argb(0, Color.red(ray.color), Color.green(ray.color), Color.blue(ray.color)),
                Color.argb((alpha * 0.55f).toInt(), Color.red(ray.color), Color.green(ray.color), Color.blue(ray.color)),
                Color.argb(alpha, Color.red(ray.color), Color.green(ray.color), Color.blue(ray.color)),
                Color.argb((alpha * 0.35f).toInt(), Color.red(ray.color), Color.green(ray.color), Color.blue(ray.color)),
                Color.argb(0, Color.red(ray.color), Color.green(ray.color), Color.blue(ray.color))
            ),
            floatArrayOf(0f, 0.18f, 0.48f, 0.82f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.FILL
        canvas.drawPath(shape, paint)
    }

    private fun drawMountainSilhouette(canvas: Canvas) {
        val horizonY = height * 0.8f
        val path = Path().apply {
            moveTo(0f, height.toFloat())
            lineTo(0f, horizonY)
            lineTo(width * 0.12f, horizonY - 38f)
            lineTo(width * 0.22f, horizonY - 14f)
            lineTo(width * 0.34f, horizonY - 66f)
            lineTo(width * 0.45f, horizonY - 22f)
            lineTo(width * 0.58f, horizonY - 84f)
            lineTo(width * 0.71f, horizonY - 28f)
            lineTo(width * 0.86f, horizonY - 58f)
            lineTo(width.toFloat(), horizonY - 18f)
            lineTo(width.toFloat(), height.toFloat())
            close()
        }
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(220, 7, 10, 18)
        canvas.drawPath(path, paint)
        canvas.drawRect(0f, horizonY, width.toFloat(), height.toFloat(), horizonPaint)
    }

    override fun onDrawEffect(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        stars.forEach { star ->
            val brightness = sin(star.twinkle) * 0.5f + 0.5f
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.color = Color.argb((brightness * 180).toInt() + 30, 255, 255, 255)
            canvas.drawCircle(star.x, star.y, star.size * (0.5f + brightness * 0.7f), paint)
        }

        rays.sortedBy { it.alpha }.forEach { ray ->
            drawAuroraRay(canvas, ray, height)
        }

        drawMountainSilhouette(canvas)

        if (message.isNotEmpty()) {
            val centerX = width / 2f
            val centerY = height * 0.73f + textLift
            textPaint.alpha = (textAlpha * 255).toInt()
            subTextPaint.alpha = (textAlpha * 230).toInt()
            drawContrastText(canvas, message, centerX, centerY, textPaint, ContrastTextType.MAIN)
            if (subMessage.isNotEmpty()) {
                drawContrastText(canvas, subMessage, centerX, centerY + 52f, subTextPaint, ContrastTextType.SUB)
            }
            textPaint.alpha = 255
            subTextPaint.alpha = 255
        }
    }

    override fun onUpdateAnimation() {
        frameCount++
        if (width == 0 || height == 0) return

        if (frameCount < 90) {
            val progress = (frameCount / 90f).coerceIn(0f, 1f)
            val eased = 1f - (1f - progress) * (1f - progress)
            textAlpha = eased
            textLift = 24f * (1f - eased)
        } else {
            textAlpha = 1f
            textLift = sin((frameCount - 90) * 0.025f) * 4f
        }

        stars.forEach { star ->
            star.twinkle += star.twinkleSpeed
        }

        rays.forEach { ray ->
            ray.phase += ray.phaseSpeed
            val breathe = ray.alphaBase + sin(frameCount * 0.013f + ray.phase) * 0.08f
            ray.alpha += (breathe - ray.alpha) * 0.04f
            ray.x += sin(ray.phase * 0.4f) * 0.22f * animationSpeed
            ray.x = ray.x.coerceIn(width * 0.08f, width * 0.92f)
        }
    }
}
