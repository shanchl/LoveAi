package com.loveai.ui.effects

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import com.loveai.model.Effect
import kotlin.math.sin
import kotlin.random.Random

/**
 * 效果6：打字机效果
 * 支持多种变体配置（颜色、速度等）
 * 改进版：文字出现后有动态效果，不再闪出
 */
class TypewriterEffect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseEffectView(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var displayedText = ""
    private var charIndex = 0
    private var frameCount = 0
    private var cycleCount = 0
    private var showCursor = true
    
    // 文字动画状态
    private var textAnimationState = 0  // 0: 打字中, 1: 完成后展示中(永久)
    private var displayTimeCounter = 0  // 文字显示计时器
    private var textYOffset = 0f         // 文字Y轴偏移（用于浮动效果）
    private var textAlpha = 255         // 文字透明度
    private var glowIntensity = 0f       // 发光强度
    private var textScale = 1f          // 文字缩放（用于呼吸效果）

    private var gradientOffset = 0f
    private val bgPaint = Paint()

    // 文字完成展示后不再重置，一直保持动画效果
    // 由外部页面切换来结束此特效

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateBackground()
    }

    override fun onEffectBound(effect: Effect) {
        textPaint.apply {
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
            setShadowLayer(8f, 0f, 2f, textGlowColor())
        }
        subTextPaint.apply {
            color = adjustAlpha(primaryColor, 0xFF)
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        cursorPaint.apply {
            color = primaryColor
            strokeWidth = 3f
        }
        glowPaint.apply {
            color = primaryColor
            maskFilter = BlurMaskFilter(30f, BlurMaskFilter.Blur.NORMAL)
        }
        
        // 重置打字机状态
        displayedText = ""
        charIndex = 0
        cycleCount = 0
        textAnimationState = 0
        displayTimeCounter = 0
        textYOffset = 0f
        textAlpha = 255
        glowIntensity = 0f
        textScale = 1f
    }

    private fun updateBackground() {
        val colors = intArrayOf(
            backgroundColor,
            adjustAlpha(primaryColor, 0x1A),
            adjustAlpha(secondaryColor, 0x0F)
        )
        bgPaint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            colors,
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDrawEffect(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        if (message.isNotEmpty()) {
            val centerX = width / 2f
            val centerY = height * 0.45f

            // 主文字区域 - 根据状态应用不同动画效果
            if (displayedText.isNotEmpty()) {
                val animatedY = centerY + textYOffset
                
                // 应用缩放效果（仅在展示状态）
                if (textAnimationState >= 1 && textScale != 1f) {
                    canvas.save()
                    canvas.scale(textScale, textScale, centerX, animatedY)
                }
                
                // 状态1：打字完成后的展示阶段 - 添加发光效果
                if (textAnimationState >= 1) {
                    // 绘制发光背景
                    if (glowIntensity > 0) {
                        glowPaint.alpha = (glowIntensity * 100).toInt()
                        val textWidth = textPaint.measureText(displayedText)
                        canvas.drawText(displayedText, centerX, animatedY, glowPaint)
                    }
                    
                    // 绘制带阴影的文字
                    textPaint.setShadowLayer(12f + glowIntensity * 8, 0f, 2f, 
                        Color.argb((glowIntensity * 200).toInt(), 
                            Color.red(primaryColor), 
                            Color.green(primaryColor), 
                            Color.blue(primaryColor)))
                    textPaint.alpha = textAlpha
                }
                
                drawContrastText(canvas, displayedText, centerX, animatedY, textPaint, ContrastTextType.MAIN)
                
                // 恢复画布缩放状态
                if (textAnimationState >= 1 && textScale != 1f) {
                    canvas.restore()
                }
                
                // 恢复阴影设置
                textPaint.setShadowLayer(8f, 0f, 2f, textGlowColor())
            }

            // 光标 - 打字过程中显示
            if (textAnimationState == 0 && showCursor && charIndex <= message.length) {
                val textWidth = textPaint.measureText(displayedText)
                val startX = centerX - textWidth / 2f + textWidth + 5f
                cursorPaint.alpha = 255
                canvas.drawLine(startX, centerY - 35f + textYOffset, startX, centerY + 10f + textYOffset, cursorPaint)
            }

            // 副标题淡入 - 在打字完成后显示
            if (subMessage.isNotEmpty() && charIndex > message.length) {
                val subAlpha = ((charIndex - message.length) / 10f).coerceIn(0f, 1f)
                subTextPaint.alpha = (subAlpha * 255).toInt()
                drawContrastText(canvas, subMessage, centerX, centerY + 65f + textYOffset, subTextPaint, ContrastTextType.SUB)
            }
        }
    }

    override fun onUpdateAnimation() {
        frameCount++

        // 光标闪烁
        if (frameCount % 15 == 0) showCursor = !showCursor

        val typeSpeed = (4 / animationSpeed).toInt().coerceIn(2, 8)
        
        when (textAnimationState) {
            0 -> {
                // 状态0：打字中
                if (frameCount % typeSpeed == 0) {
                    if (charIndex <= message.length + 10) {
                        if (charIndex < message.length) {
                            displayedText = message.substring(0, charIndex + 1)
                        }
                        charIndex++
                    } else {
                        // 打字完成，切换到展示状态
                        textAnimationState = 1
                        displayTimeCounter = 0
                    }
                }
            }
            1 -> {
                // 状态1：文字展示中 - 永久持续动态效果，不再重置
                displayTimeCounter++
                
                // 1. 浮动效果 (正弦波)
                textYOffset = sin(displayTimeCounter * 0.05f) * 10f
                
                // 2. 呼吸发光效果
                glowIntensity = (sin(displayTimeCounter * 0.03f) + 1f) / 2f
                
                // 3. 轻微的透明度呼吸效果
                textAlpha = (240 + sin(displayTimeCounter * 0.025f) * 15).toInt()
                
                // 4. 文字缩放微调效果
                textScale = 1f + sin(displayTimeCounter * 0.04f) * 0.03f
                
                // 状态1永久保持，不再进入状态2重置
            }
        }

        gradientOffset += 0.002f
    }
}
