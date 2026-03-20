package com.loveai.ui.effects

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.loveai.model.Effect
import com.loveai.model.EffectVariant

/**
 * 所有动态特效视图的基类。
 * 统一处理动画循环、变体读取、文字对比度增强和公共氛围叠层。
 */
abstract class BaseEffectView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class AmbientOrb(
        val xFactor: Float,
        val yFactor: Float,
        val radius: Float,
        val driftX: Float,
        val driftY: Float,
        val phase: Float,
        val alpha: Int
    )

    protected var effect: Effect? = null
    protected var variant: EffectVariant? = null
    protected var isPlaying = true

    private val frameDelay = 16L
    private var renderFrame = 0
    private val ambientOrbs = mutableListOf<AmbientOrb>()
    private val ambientPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var vignetteShader: RadialGradient? = null

    protected val primaryColor: Int
        get() = variant?.primaryColor ?: Color.parseColor("#FF69B4")

    protected val secondaryColor: Int
        get() = variant?.secondaryColor ?: Color.parseColor("#FF1493")

    protected val backgroundColor: Int
        get() = variant?.backgroundColor ?: Color.parseColor("#0D0D0D")

    protected val animationSpeed: Float
        get() = variant?.speed ?: 1.0f

    protected val particleCount: Int
        get() = variant?.particleCount ?: 50

    protected val message: String
        get() = variant?.message ?: ""

    protected val subMessage: String
        get() = variant?.subMessage ?: ""

    private val animRunnable = object : Runnable {
        override fun run() {
            if (isPlaying) {
                renderFrame++
                onUpdateAnimation()
                invalidate()
                postDelayed(this, frameDelay)
            }
        }
    }

    fun bindEffect(effect: Effect) {
        this.effect = effect
        this.variant = effect.variant
        onEffectBound(effect)
    }

    protected open fun onEffectBound(effect: Effect) {}

    abstract fun onDrawEffect(canvas: Canvas)

    abstract fun onUpdateAnimation()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        onDrawEffect(canvas)
        drawAmbientOverlay(canvas)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return

        vignetteShader = RadialGradient(
            w / 2f,
            h / 2f,
            maxOf(w, h) * 0.72f,
            intArrayOf(
                Color.TRANSPARENT,
                Color.argb(26, 10, 10, 18),
                Color.argb(150, 3, 3, 8)
            ),
            floatArrayOf(0.42f, 0.78f, 1f),
            Shader.TileMode.CLAMP
        )

        ambientOrbs.clear()
        repeat(9) { index ->
            ambientOrbs.add(
                AmbientOrb(
                    xFactor = (0.1f + index * 0.1f).coerceAtMost(0.9f),
                    yFactor = 0.18f + (index % 3) * 0.18f,
                    radius = 36f + index * 8f,
                    driftX = 10f + index * 1.8f,
                    driftY = 6f + (index % 4) * 1.5f,
                    phase = index * 0.85f,
                    alpha = 12 + index * 3
                )
            )
        }
    }

    fun startAnimation() {
        isPlaying = true
        removeCallbacks(animRunnable)
        post(animRunnable)
    }

    fun stopAnimation() {
        isPlaying = false
        removeCallbacks(animRunnable)
    }

    fun togglePlayPause() {
        if (isPlaying) stopAnimation() else startAnimation()
    }

    protected fun currentRenderFrame(): Int = renderFrame

    private fun drawAmbientOverlay(canvas: Canvas) {
        if (width == 0 || height == 0) return

        for (orb in ambientOrbs) {
            val offsetX = kotlin.math.sin(renderFrame * 0.01f + orb.phase) * orb.driftX
            val offsetY = kotlin.math.cos(renderFrame * 0.008f + orb.phase * 1.3f) * orb.driftY
            ambientPaint.color = adjustAlpha(primaryColor, orb.alpha.coerceAtMost(70))
            ambientPaint.maskFilter = BlurMaskFilter(orb.radius * 0.55f, BlurMaskFilter.Blur.NORMAL)
            canvas.drawCircle(
                orb.xFactor * width + offsetX,
                orb.yFactor * height + offsetY,
                orb.radius,
                ambientPaint
            )
        }

        ambientPaint.maskFilter = null
        ambientPaint.shader = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            intArrayOf(
                adjustAlpha(primaryColor, 12),
                Color.TRANSPARENT,
                adjustAlpha(secondaryColor, 16)
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), ambientPaint)
        ambientPaint.shader = null

        vignettePaint.shader = vignetteShader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
    }

    protected fun adjustAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    protected fun luminance(color: Int): Float {
        val r = Color.red(color) / 255f
        val g = Color.green(color) / 255f
        val b = Color.blue(color) / 255f
        return 0.299f * r + 0.587f * g + 0.114f * b
    }

    protected fun autoTextColor(bgColor: Int): Int {
        return if (luminance(bgColor) > 0.5f) {
            Color.parseColor("#1A1A2E")
        } else {
            Color.WHITE
        }
    }

    protected fun textGlowColor(): Int {
        return adjustAlpha(primaryColor, 0xB0)
    }

    protected fun drawContrastText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: Paint,
        textType: ContrastTextType = ContrastTextType.MAIN
    ) {
        if (text.isEmpty()) return

        val textWidth = paint.measureText(text)
        val fontMetrics = paint.fontMetrics

        when (textType) {
            ContrastTextType.MAIN -> {
                val paddingH = 24f
                val paddingV = 14f
                val rectLeft = x - textWidth / 2f - paddingH
                val rectTop = y + fontMetrics.ascent - paddingV
                val rectRight = x + textWidth / 2f + paddingH
                val rectBottom = y + fontMetrics.descent + paddingV
                val radius = 20f

                val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(0x99, 0x10, 0x10, 0x1A)
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(rectLeft, rectTop, rectRight, rectBottom, radius, radius, panelPaint)

                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f
                    color = adjustAlpha(primaryColor, 70)
                }
                canvas.drawRoundRect(rectLeft, rectTop, rectRight, rectBottom, radius, radius, borderPaint)

                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = adjustAlpha(primaryColor, 0x25)
                    maskFilter = BlurMaskFilter(25f, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.drawRoundRect(
                    rectLeft - 5f,
                    rectTop - 5f,
                    rectRight + 5f,
                    rectBottom + 5f,
                    radius + 5f,
                    radius + 5f,
                    glowPaint
                )

                val outlinePaint = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = (paint.textSize / 14f).coerceAtLeast(2f)
                    color = Color.argb(140, 0, 0, 0)
                    strokeJoin = Paint.Join.ROUND
                    strokeMiter = 10f
                }
                canvas.drawText(text, x, y, outlinePaint)
                canvas.drawText(text, x, y, paint)
            }

            ContrastTextType.SUB -> {
                val paddingH = 20f
                val paddingV = 12f
                val rectLeft = x - textWidth / 2f - paddingH
                val rectTop = y + fontMetrics.ascent - paddingV
                val rectRight = x + textWidth / 2f + paddingH
                val rectBottom = y + fontMetrics.descent + paddingV
                val radius = 16f

                val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(0xE6, 0x08, 0x08, 0x12)
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(rectLeft, rectTop, rectRight, rectBottom, radius, radius, panelPaint)

                val savedAlpha = paint.alpha
                val outlinePaint = Paint(paint).apply {
                    alpha = 255
                    style = Paint.Style.STROKE
                    strokeWidth = (paint.textSize / 18f).coerceAtLeast(1.2f)
                    color = Color.argb(120, 0, 0, 0)
                    strokeJoin = Paint.Join.ROUND
                }
                canvas.drawText(text, x, y, outlinePaint)

                paint.alpha = 255
                paint.setShadowLayer(8f, 0f, 3f, Color.argb(0xDD, 0x00, 0x00, 0x00))
                canvas.drawText(text, x, y, paint)
                paint.alpha = savedAlpha
                paint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
            }
        }
    }

    protected enum class ContrastTextType {
        MAIN,
        SUB
    }
}
