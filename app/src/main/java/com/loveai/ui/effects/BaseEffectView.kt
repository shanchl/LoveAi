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
import com.loveai.model.EffectType
import com.loveai.model.EffectVariant
import kotlin.math.abs
import kotlin.math.sin

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
        val style = resolveTextDecorStyle(textType)
        val rectLeft = x - textWidth / 2f - if (textType == ContrastTextType.MAIN) 24f else 20f
        val rectTop = y + fontMetrics.ascent - if (textType == ContrastTextType.MAIN) 14f else 12f
        val rectRight = x + textWidth / 2f + if (textType == ContrastTextType.MAIN) 24f else 20f
        val rectBottom = y + fontMetrics.descent + if (textType == ContrastTextType.MAIN) 14f else 12f
        val radius = if (textType == ContrastTextType.MAIN) 20f else 16f

        drawTextPanel(canvas, rectLeft, rectTop, rectRight, rectBottom, radius, style, textType)
        drawStyledText(canvas, text, x, y, paint, style, textType)
    }

    private fun resolveTextDecorStyle(textType: ContrastTextType): TextDecorStyle {
        return when (effect?.type) {
            EffectType.FIREWORK, EffectType.HEART_PULSE, EffectType.RIPPLE -> {
                if (textType == ContrastTextType.MAIN) TextDecorStyle.BURST else TextDecorStyle.SOFT_GLOW
            }
            EffectType.STARRY_SKY, EffectType.AURORA, EffectType.METEOR_SHOWER -> {
                if (textType == ContrastTextType.MAIN) TextDecorStyle.STARDUST else TextDecorStyle.GALAXY
            }
            EffectType.PETAL_FALL, EffectType.BUTTERFLY, EffectType.BUBBLE_FLOAT -> {
                if (textType == ContrastTextType.MAIN) TextDecorStyle.FLOATING else TextDecorStyle.RIBBON
            }
            EffectType.TYPEWRITER -> {
                if (textType == ContrastTextType.MAIN) TextDecorStyle.LETTER else TextDecorStyle.RIBBON
            }
            else -> {
                if (textType == ContrastTextType.MAIN) TextDecorStyle.SOFT_GLOW else TextDecorStyle.RIBBON
            }
        }
    }

    private fun drawTextPanel(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radius: Float,
        decorStyle: TextDecorStyle,
        textType: ContrastTextType
    ) {
        val frame = currentRenderFrame().toFloat()
        val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.style = Paint.Style.FILL }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = if (textType == ContrastTextType.MAIN) 1.8f else 1.2f
        }
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(if (textType == ContrastTextType.MAIN) 26f else 18f, BlurMaskFilter.Blur.NORMAL)
        }

        when (decorStyle) {
            TextDecorStyle.SOFT_GLOW -> {
                panelPaint.color = Color.argb(if (textType == ContrastTextType.MAIN) 150 else 214, 12, 12, 20)
                borderPaint.color = adjustAlpha(primaryColor, if (textType == ContrastTextType.MAIN) 82 else 62)
                glowPaint.color = adjustAlpha(primaryColor, if (textType == ContrastTextType.MAIN) 42 else 24)
                canvas.drawRoundRect(left, top, right, bottom, radius, radius, panelPaint)
                canvas.drawRoundRect(left, top, right, bottom, radius, radius, borderPaint)
                canvas.drawRoundRect(left - 4f, top - 4f, right + 4f, bottom + 4f, radius + 4f, radius + 4f, glowPaint)
            }

            TextDecorStyle.BURST -> {
                panelPaint.shader = LinearGradient(
                    left,
                    top,
                    right,
                    bottom,
                    intArrayOf(Color.argb(164, 18, 10, 24), adjustAlpha(primaryColor, 76), adjustAlpha(secondaryColor, 62)),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
                borderPaint.color = adjustAlpha(Color.WHITE, 96)
                glowPaint.color = adjustAlpha(primaryColor, 48)
                canvas.drawRoundRect(left, top, right, bottom, radius + 6f, radius + 6f, panelPaint)
                canvas.drawRoundRect(left, top, right, bottom, radius + 6f, radius + 6f, borderPaint)
                canvas.drawCircle((left + right) / 2f, (top + bottom) / 2f, (right - left) * 0.34f, glowPaint)
                panelPaint.shader = null
            }

            TextDecorStyle.STARDUST, TextDecorStyle.GALAXY -> {
                panelPaint.shader = LinearGradient(
                    left,
                    top,
                    right,
                    bottom,
                    intArrayOf(Color.argb(126, 8, 12, 26), Color.argb(168, 10, 12, 24), Color.argb(110, 18, 24, 38)),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP
                )
                borderPaint.color = adjustAlpha(Color.WHITE, if (decorStyle == TextDecorStyle.STARDUST) 68 else 52)
                canvas.drawRoundRect(left, top, right, bottom, radius, radius, panelPaint)
                canvas.drawRoundRect(left, top, right, bottom, radius, radius, borderPaint)
                repeat(if (decorStyle == TextDecorStyle.STARDUST) 6 else 4) { index ->
                    val starX = left + (right - left) * (0.14f + index * 0.14f)
                    val starY = top + (bottom - top) * (0.2f + ((index + frame.toInt()) % 3) * 0.2f)
                    val alpha = (96 + abs(sin(frame * 0.05f + index) * 60f).toInt()).coerceAtMost(170)
                    borderPaint.style = Paint.Style.FILL
                    borderPaint.color = Color.argb(alpha, 220, 232, 255)
                    canvas.drawCircle(starX, starY, if (decorStyle == TextDecorStyle.STARDUST) 1.8f else 1.4f, borderPaint)
                    borderPaint.style = Paint.Style.STROKE
                }
                panelPaint.shader = null
            }

            TextDecorStyle.FLOATING, TextDecorStyle.RIBBON -> {
                panelPaint.shader = LinearGradient(
                    left,
                    top,
                    right,
                    bottom,
                    intArrayOf(Color.argb(174, 18, 12, 20), adjustAlpha(primaryColor, 60), Color.argb(130, 34, 20, 32)),
                    floatArrayOf(0f, 0.48f, 1f),
                    Shader.TileMode.CLAMP
                )
                borderPaint.color = adjustAlpha(primaryColor, 76)
                canvas.drawRoundRect(left, top, right, bottom, radius + 8f, radius + 8f, panelPaint)
                canvas.drawRoundRect(left, top, right, bottom, radius + 8f, radius + 8f, borderPaint)
                val ribbonY = bottom + if (decorStyle == TextDecorStyle.FLOATING) 8f else 5f
                borderPaint.strokeWidth = 2.2f
                borderPaint.color = adjustAlpha(secondaryColor, 96)
                canvas.drawLine(left + 18f, ribbonY, right - 18f, ribbonY, borderPaint)
                panelPaint.shader = null
            }

            TextDecorStyle.LETTER -> {
                panelPaint.color = Color.argb(214, 244, 236, 222)
                borderPaint.color = adjustAlpha(primaryColor, 72)
                canvas.drawRoundRect(left, top, right, bottom, 10f, 10f, panelPaint)
                canvas.drawRoundRect(left, top, right, bottom, 10f, 10f, borderPaint)
                borderPaint.color = adjustAlpha(secondaryColor, 72)
                borderPaint.strokeWidth = 1f
                val baseY = bottom - 8f
                canvas.drawLine(left + 14f, baseY, right - 14f, baseY, borderPaint)
            }
        }
    }

    private fun drawStyledText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        baselineY: Float,
        paint: Paint,
        decorStyle: TextDecorStyle,
        textType: ContrastTextType
    ) {
        val savedAlpha = paint.alpha
        val savedAlign = paint.textAlign
        val savedColor = paint.color
        val savedShader = paint.shader
        val savedStyle = paint.style
        val savedShadowRadius =  if (textType == ContrastTextType.MAIN) 10f else 7f
        val outlinePaint = Paint(paint).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = if (textType == ContrastTextType.MAIN) (paint.textSize / 14f).coerceAtLeast(2f) else (paint.textSize / 18f).coerceAtLeast(1.2f)
            color = Color.argb(if (decorStyle == TextDecorStyle.LETTER) 90 else 140, 0, 0, 0)
            strokeJoin = Paint.Join.ROUND
            strokeMiter = 10f
            textAlign = Paint.Align.LEFT
        }

        paint.textAlign = Paint.Align.LEFT
        paint.style = Paint.Style.FILL

        if (decorStyle == TextDecorStyle.LETTER) {
            paint.color = Color.parseColor("#533B3B")
            paint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
        } else if (decorStyle == TextDecorStyle.STARDUST || decorStyle == TextDecorStyle.GALAXY) {
            paint.shader = LinearGradient(
                centerX - paint.measureText(text) / 2f,
                baselineY - paint.textSize,
                centerX + paint.measureText(text) / 2f,
                baselineY,
                intArrayOf(Color.WHITE, adjustAlpha(Color.WHITE, 230), adjustAlpha(primaryColor, 220)),
                floatArrayOf(0f, 0.58f, 1f),
                Shader.TileMode.CLAMP
            )
        } else {
            paint.setShadowLayer(savedShadowRadius, 0f, 2f, textGlowColor())
        }

        val frame = currentRenderFrame().toFloat()
        val totalWidth = paint.measureText(text)
        var cursorX = centerX - totalWidth / 2f

        text.forEachIndexed { index, ch ->
            val char = ch.toString()
            val charWidth = paint.measureText(char)
            val normalized = index - (text.length - 1) / 2f
            val offsetY = when (decorStyle) {
                TextDecorStyle.BURST -> -abs(normalized) * 0.7f + sin(frame * 0.08f + index * 0.6f) * 1.8f
                TextDecorStyle.STARDUST -> sin(frame * 0.05f + index * 0.45f) * 1.6f
                TextDecorStyle.GALAXY -> sin(frame * 0.04f + index * 0.35f) * 1.2f - abs(normalized) * 0.25f
                TextDecorStyle.FLOATING -> sin(frame * 0.06f + index * 0.55f) * 2.1f
                TextDecorStyle.RIBBON -> sin(frame * 0.05f + index * 0.5f) * 1.3f
                TextDecorStyle.LETTER -> 0f
                TextDecorStyle.SOFT_GLOW -> 0f
            }
            val offsetX = when (decorStyle) {
                TextDecorStyle.BURST -> normalized * 0.6f
                TextDecorStyle.FLOATING -> sin(frame * 0.03f + index * 0.4f) * 0.8f
                else -> 0f
            }
            val charX = cursorX + offsetX
            val charY = baselineY + offsetY
            canvas.drawText(char, charX, charY, outlinePaint)
            canvas.drawText(char, charX, charY, paint)
            cursorX += charWidth
        }

        paint.alpha = savedAlpha
        paint.color = savedColor
        paint.textAlign = savedAlign
        paint.shader = savedShader
        paint.style = savedStyle
        paint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
    }

    protected enum class ContrastTextType {
        MAIN,
        SUB
    }

    private enum class TextDecorStyle {
        SOFT_GLOW,
        BURST,
        STARDUST,
        GALAXY,
        FLOATING,
        RIBBON,
        LETTER
    }
}
