package com.loveai.ui.effects

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.loveai.model.Effect
import com.loveai.model.EffectVariant

/**
 * 所有动态效果视图的基类
 * 提供动画循环的基础框架和变体配置支持
 */
abstract class BaseEffectView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    protected var effect: Effect? = null
    protected var variant: EffectVariant? = null
    protected var isPlaying = true
    private val frameDelay = 16L // ~60fps

    // 变体配置的便捷访问属性
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

    /**
     * 子类实现：绑定效果后的初始化
     */
    protected open fun onEffectBound(effect: Effect) {}

    /**
     * 子类实现：绘制效果
     */
    abstract fun onDrawEffect(canvas: Canvas)

    /**
     * 子类实现：更新动画状态（每帧调用）
     */
    abstract fun onUpdateAnimation()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        onDrawEffect(canvas)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
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

    // ========== 文字对比度工具方法 ==========

    /**
     * 修正颜色的 alpha 通道
     */
    protected fun adjustAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    /**
     * 计算颜色的感知亮度 (0~255)
     */
    protected fun luminance(color: Int): Float {
        val r = Color.red(color) / 255f
        val g = Color.green(color) / 255f
        val b = Color.blue(color) / 255f
        return 0.299f * r + 0.587f * g + 0.114f * b
    }

    /**
     * 根据背景亮度计算合适的文字颜色（自适应对比度）
     * 在深色背景上返回白色，在浅色背景上返回深色
     */
    protected fun autoTextColor(bgColor: Int): Int {
        return if (luminance(bgColor) > 0.5f) {
            Color.parseColor("#1A1A2E") // 深蓝黑色，适合浅色背景
        } else {
            Color.WHITE
        }
    }

    /**
     * 计算文字阴影颜色：用主题色的强调色
     */
    protected fun textGlowColor(): Int {
        return adjustAlpha(primaryColor, 0xB0)
    }

    /**
     * 绘制带对比度保护的文字
     * 自动在文字下方绘制半透明暗色底板，确保任何背景下都能清晰阅读
     *
     * @param canvas 画布
     * @param text 文字内容
     * @param x 文字中心 X
     * @param y 文字基线 Y
     * @param paint 已配置好样式(字体/大小/对齐方式)的 Paint
     * @param textType 文字类型：MAIN（主标题）或 SUB（副标题）
     */
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
        val textHeight = fontMetrics.descent - fontMetrics.ascent

        when (textType) {
            ContrastTextType.MAIN -> {
                // 主标题：绘制圆角暗色底板
                val paddingH = 24f
                val paddingV = 14f
                val rectLeft = x - textWidth / 2f - paddingH
                val rectTop = y + fontMetrics.ascent - paddingV
                val rectRight = x + textWidth / 2f + paddingH
                val rectBottom = y + fontMetrics.descent + paddingV
                val radius = 20f

                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                bgPaint.color = Color.argb(0x99, 0x10, 0x10, 0x1A) // 半透明深色
                bgPaint.style = Paint.Style.FILL
                canvas.drawRoundRect(rectLeft, rectTop, rectRight, rectBottom, radius, radius, bgPaint)

                // 再叠一层模糊光晕
                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                glowPaint.color = adjustAlpha(primaryColor, 0x25)
                glowPaint.maskFilter = BlurMaskFilter(25f, BlurMaskFilter.Blur.NORMAL)
                canvas.drawRoundRect(rectLeft - 5f, rectTop - 5f, rectRight + 5f, rectBottom + 5f, radius + 5f, radius + 5f, glowPaint)
                glowPaint.maskFilter = null

                // 绘制文字
                canvas.drawText(text, x, y, paint)
            }
            ContrastTextType.SUB -> {
                // 副标题：增强对比度处理，更深的底板 + 阴影
                // 注意：无论文字透明度如何变化，底板始终保持足够不透明度
                val paddingH = 20f
                val paddingV = 12f
                val rectLeft = x - textWidth / 2f - paddingH
                val rectTop = y + fontMetrics.ascent - paddingV
                val rectRight = x + textWidth / 2f + paddingH
                val rectBottom = y + fontMetrics.descent + paddingV
                val radius = 16f

                // 1. 先绘制更深的半透明底板（始终保持90%不透明度，不受文字alpha影响）
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                bgPaint.color = Color.argb(0xE6, 0x08, 0x08, 0x12) // 90%不透明度的深色底板
                bgPaint.style = Paint.Style.FILL
                canvas.drawRoundRect(rectLeft, rectTop, rectRight, rectBottom, radius, radius, bgPaint)

                // 2. 文字阴影不受paint alpha影响，始终保持清晰
                val savedAlpha = paint.alpha // 保存原始alpha
                paint.alpha = 255 // 临时设为不透明绘制阴影
                paint.setShadowLayer(8f, 0f, 3f, Color.argb(0xDD, 0x00, 0x00, 0x00))
                canvas.drawText(text, x, y, paint)
                // 恢复原始alpha和阴影设置
                paint.alpha = savedAlpha
                paint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
            }
        }
    }

    /**
     * 对比度文字类型
     */
    protected enum class ContrastTextType {
        MAIN,   // 主标题 - 更大的底板和光晕
        SUB     // 副标题 - 轻量级底板
    }
}
