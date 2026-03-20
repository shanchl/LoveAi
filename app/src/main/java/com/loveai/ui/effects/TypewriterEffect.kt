package com.loveai.ui.effects

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import com.loveai.model.Effect
import kotlin.math.sin
import kotlin.random.Random

/**
 * 第三轮精修：把打字机做成“夜里写情书”的感觉，而不是纯文字逐字出现。
 */
class TypewriterEffect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseEffectView(context, attrs) {

    private data class Dust(
        var x: Float,
        var y: Float,
        var radius: Float,
        var alpha: Int,
        var driftPhase: Float,
        var driftSpeed: Float
    )

    private val dusts = mutableListOf<Dust>()
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val panelStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dustPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var displayedText = ""
    private var charIndex = 0
    private var frameCount = 0
    private var showCursor = true
    private var panelAlpha = 0f
    private var textAlpha = 0f
    private var floatPhase = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        bgPaint.shader = LinearGradient(
            0f,
            0f,
            w.toFloat(),
            h.toFloat(),
            intArrayOf(
                backgroundColor,
                adjustAlpha(primaryColor, 16),
                adjustAlpha(secondaryColor, 24)
            ),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )

        dusts.clear()
        repeat(42) {
            dusts.add(
                Dust(
                    x = Random.nextFloat() * w,
                    y = Random.nextFloat() * h,
                    radius = Random.nextFloat() * 2.6f + 0.8f,
                    alpha = Random.nextInt(18, 90),
                    driftPhase = Random.nextFloat() * (Math.PI * 2).toFloat(),
                    driftSpeed = Random.nextFloat() * 0.03f + 0.008f
                )
            )
        }
    }

    override fun onEffectBound(effect: Effect) {
        displayedText = ""
        charIndex = 0
        frameCount = 0
        showCursor = true
        panelAlpha = 0f
        textAlpha = 0f
        floatPhase = 0f

        textPaint.apply {
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
            setShadowLayer(8f, 0f, 2f, textGlowColor())
        }
        subTextPaint.apply {
            color = adjustAlpha(primaryColor, 0xF0)
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        cursorPaint.apply {
            color = primaryColor
            strokeWidth = 3f
        }
    }

    override fun onDrawEffect(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        dusts.forEach { dust ->
            dustPaint.color = Color.argb(dust.alpha, 255, 255, 255)
            canvas.drawCircle(dust.x, dust.y, dust.radius, dustPaint)
        }

        val panelRect = RectF(
            width * 0.1f,
            height * 0.18f,
            width * 0.9f,
            height * 0.74f
        )
        panelPaint.color = Color.argb((panelAlpha * 185).toInt(), 14, 14, 24)
        canvas.drawRoundRect(panelRect, 34f, 34f, panelPaint)

        panelStrokePaint.style = Paint.Style.STROKE
        panelStrokePaint.strokeWidth = 2f
        panelStrokePaint.color = adjustAlpha(primaryColor, (panelAlpha * 80).toInt())
        canvas.drawRoundRect(panelRect, 34f, 34f, panelStrokePaint)

        val titleY = panelRect.top + 72f + sin(floatPhase) * 4f
        val contentY = panelRect.centerY() - 10f + sin(floatPhase * 0.7f) * 5f

        if (displayedText.isNotEmpty()) {
            textPaint.alpha = (textAlpha * 255).toInt()
            drawContrastText(canvas, displayedText, panelRect.centerX(), contentY, textPaint, ContrastTextType.MAIN)
        }

        if (textAlpha > 0.65f && subMessage.isNotEmpty()) {
            subTextPaint.alpha = (((textAlpha - 0.65f) / 0.35f).coerceIn(0f, 1f) * 255).toInt()
            drawContrastText(canvas, subMessage, panelRect.centerX(), contentY + 84f, subTextPaint, ContrastTextType.SUB)
        }

        textPaint.alpha = ((panelAlpha * 200).toInt()).coerceAtLeast(0)
        textPaint.textSize = 18f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("写给你的情书", panelRect.centerX(), titleY, textPaint)
        textPaint.textSize = 48f
        textPaint.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)

        if (showCursor && charIndex <= message.length) {
            val textWidth = textPaint.measureText(displayedText)
            val cursorX = panelRect.centerX() - textWidth / 2f + textWidth + 8f
            canvas.drawLine(cursorX, contentY - 36f, cursorX, contentY + 10f, cursorPaint)
        }
    }

    override fun onUpdateAnimation() {
        frameCount++
        floatPhase += 0.035f

        if (frameCount % 18 == 0) {
            showCursor = !showCursor
        }

        if (frameCount < 38) {
            panelAlpha = (frameCount / 38f).coerceIn(0f, 1f)
        } else {
            panelAlpha = 1f
        }

        val typeSpeed = (4 / animationSpeed).toInt().coerceIn(2, 8)
        if (frameCount % typeSpeed == 0 && charIndex < message.length) {
            charIndex++
            displayedText = message.substring(0, charIndex)
        }

        val targetAlpha = if (message.isEmpty() || charIndex >= message.length) {
            1f
        } else {
            (charIndex / message.length.toFloat()).coerceIn(0f, 1f)
        }
        textAlpha += (targetAlpha - textAlpha) * 0.14f

        dusts.forEach { dust ->
            dust.y -= 0.18f + dust.radius * 0.03f
            dust.x += sin(dust.driftPhase) * 0.4f
            dust.driftPhase += dust.driftSpeed
            if (dust.y < -10f) {
                dust.y = height + 10f
                dust.x = Random.nextFloat() * width
            }
        }
    }
}
