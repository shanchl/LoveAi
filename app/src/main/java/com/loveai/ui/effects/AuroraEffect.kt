package com.loveai.ui.effects

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import com.loveai.model.Effect
import kotlin.math.*
import kotlin.random.Random

/**
 * 效果12：极光波浪动画
 * 夜空中飘荡的极光帘幕，多层波浪叠加，色彩流动变换
 * 文字展示：逐字从下方浮现展开
 */
class AuroraEffect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseEffectView(context, attrs) {

    // 极光帘幕：多条竖向光带
    private data class AuroraRay(
        var x: Float,           // 底部中心X
        val width: Float,       // 帘幕宽度
        val height: Float,      // 帘幕高度
        var phase: Float,       // 波动相位
        val phaseSpeed: Float,  // 波动速度
        val waveAmplitude: Float, // 波幅
        val waveFreq: Float,    // 波频
        val color: Int,         // 主色
        var colorPhase: Float,  // 色相偏移相位（颜色慢慢变化）
        val colorSpeed: Float,
        var alpha: Float,       // 当前透明度
        val targetAlpha: Float  // 目标透明度（呼吸感）
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
    private val bgPaint = Paint()
    private var frameCount = 0

    // 文字逐字展开控制
    private var textReveal = 0f      // 0~1，控制文字展开进度
    private var textAlpha = 0f

    // 极光颜色集（绿、青、蓝、紫、粉）
    private val auroraColorSets = listOf(
        intArrayOf(
            Color.parseColor("#00FF7F"),  // 绿
            Color.parseColor("#00FFCC"),  // 青绿
            Color.parseColor("#00BFFF")   // 蓝
        ),
        intArrayOf(
            Color.parseColor("#7B2FBE"),  // 紫
            Color.parseColor("#C77DFF"),  // 淡紫
            Color.parseColor("#E0AAFF")   // 浅紫
        ),
        intArrayOf(
            Color.parseColor("#FF6EC7"),  // 粉
            Color.parseColor("#FF00FF"),  // 品红
            Color.parseColor("#9B5DE5")   // 紫
        )
    )
    private val selectedColors: IntArray by lazy {
        // 根据主色选一套色系，或用混合
        auroraColorSets[abs(primaryColor.hashCode()) % auroraColorSets.size]
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // 深夜极地背景
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(
                Color.parseColor("#020815"),
                Color.parseColor("#050E20"),
                Color.parseColor("#010610")
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        // 背景星点
        stars.clear()
        repeat(150) {
            stars.add(
                Star(
                    x = Random.nextFloat() * w,
                    y = Random.nextFloat() * h * 0.6f,  // 星星集中在上半部分
                    size = Random.nextFloat() * 2f + 0.3f,
                    twinkle = Random.nextFloat() * Math.PI.toFloat() * 2,
                    twinkleSpeed = Random.nextFloat() * 0.03f + 0.005f
                )
            )
        }

        // 初始化极光帘幕
        rays.clear()
        val colors = selectedColors
        val rayCount = 6
        for (i in 0 until rayCount) {
            val colorIndex = i % colors.size
            val rayWidth = w * (Random.nextFloat() * 0.3f + 0.25f)
            val rayHeight = h * (Random.nextFloat() * 0.35f + 0.35f)
            rays.add(
                AuroraRay(
                    x = w * (i + 0.5f) / rayCount + (Random.nextFloat() - 0.5f) * w * 0.15f,
                    width = rayWidth,
                    height = rayHeight,
                    phase = Random.nextFloat() * Math.PI.toFloat() * 2,
                    phaseSpeed = (Random.nextFloat() * 0.008f + 0.004f) * animationSpeed,
                    waveAmplitude = w * (Random.nextFloat() * 0.06f + 0.03f),
                    waveFreq = Random.nextFloat() * 2f + 1.5f,
                    color = colors[colorIndex],
                    colorPhase = Random.nextFloat() * Math.PI.toFloat() * 2,
                    colorSpeed = Random.nextFloat() * 0.005f + 0.002f,
                    alpha = Random.nextFloat() * 0.3f + 0.1f,
                    targetAlpha = Random.nextFloat() * 0.4f + 0.15f
                )
            )
        }
    }

    override fun onEffectBound(effect: Effect) {
        textPaint.apply {
            color = Color.WHITE
            textSize = 44f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
            setShadowLayer(14f, 0f, 3f, textGlowColor())
        }
        subTextPaint.apply {
            color = adjustAlpha(secondaryColor, 0xCC)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
    }

    /**
     * 绘制单条极光帘幕（用多边形模拟竖向光带）
     */
    private fun drawAuroraRay(canvas: Canvas, ray: AuroraRay, viewH: Int) {
        val segments = 30  // 分段数（控制波浪平滑度）
        val topY = viewH * 0.05f  // 极光从屏幕顶部开始
        val bottomY = topY + ray.height

        val path = Path()
        val leftPoints = mutableListOf<PointF>()
        val rightPoints = mutableListOf<PointF>()

        for (i in 0..segments) {
            val t = i.toFloat() / segments
            val y = topY + (bottomY - topY) * t
            // 波动X偏移（随高度变化，越到底部波幅越大）
            val waveX = sin(ray.phase + t * ray.waveFreq * Math.PI.toFloat() * 2) *
                    ray.waveAmplitude * t
            val centerX = ray.x + waveX
            val halfW = ray.width * 0.5f * (1f - t * 0.3f)  // 底部稍窄

            leftPoints.add(PointF(centerX - halfW, y))
            rightPoints.add(PointF(centerX + halfW, y))
        }

        // 构建路径
        path.moveTo(leftPoints[0].x, leftPoints[0].y)
        for (pt in leftPoints) path.lineTo(pt.x, pt.y)
        for (pt in rightPoints.reversed()) path.lineTo(pt.x, pt.y)
        path.close()

        // 极光颜色（顶部到底部：从纯色到透明，整体呼吸感）
        val baseAlpha = (ray.alpha * 255).toInt()
        val colorShift = sin(ray.colorPhase) * 0.3f + 0.7f  // 颜色变化

        val gradient = LinearGradient(
            0f, topY, 0f, bottomY,
            intArrayOf(
                Color.argb(0, Color.red(ray.color), Color.green(ray.color), Color.blue(ray.color)),
                Color.argb((baseAlpha * 0.6f * colorShift).toInt(),
                    Color.red(ray.color), Color.green(ray.color), Color.blue(ray.color)),
                Color.argb((baseAlpha * colorShift).toInt(),
                    Color.red(ray.color), Color.green(ray.color), Color.blue(ray.color)),
                Color.argb((baseAlpha * 0.4f * colorShift).toInt(),
                    Color.red(ray.color), Color.green(ray.color), Color.blue(ray.color)),
                Color.argb(0, Color.red(ray.color), Color.green(ray.color), Color.blue(ray.color))
            ),
            floatArrayOf(0f, 0.2f, 0.5f, 0.8f, 1f),
            Shader.TileMode.CLAMP
        )

        paint.shader = gradient
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)

        // 中心亮线（极光最亮的中轴）
        val centerLineGrad = LinearGradient(
            0f, topY, 0f, bottomY,
            intArrayOf(
                Color.argb(0, 255, 255, 255),
                Color.argb((baseAlpha * 0.5f).toInt(), 255, 255, 255),
                Color.argb(0, 255, 255, 255)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = centerLineGrad
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f

        val centerPath = Path()
        centerPath.moveTo(leftPoints[0].x + (rightPoints[0].x - leftPoints[0].x) / 2, leftPoints[0].y)
        for (i in 1..segments) {
            val midX = leftPoints[i].x + (rightPoints[i].x - leftPoints[i].x) / 2
            centerPath.lineTo(midX, leftPoints[i].y)
        }
        canvas.drawPath(centerPath, paint)
    }

    override fun onDrawEffect(canvas: Canvas) {
        // 夜空背景
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 背景星点
        for (star in stars) {
            val brightness = (sin(star.twinkle) * 0.5f + 0.5f)
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.color = Color.argb((brightness * 180 + 30).toInt(), 255, 255, 255)
            canvas.drawCircle(star.x, star.y, star.size * (0.5f + brightness * 0.5f), paint)
        }

        // 极光帘幕（按 alpha 从小到大绘制，深色在后）
        val sortedRays = rays.sortedBy { it.alpha }
        for (ray in sortedRays) {
            drawAuroraRay(canvas, ray, height)
        }

        // 地平线发光（模拟远山轮廓）
        val horizonY = height * 0.72f
        val horizonGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, horizonY - 30f, 0f, horizonY + 80f,
                intArrayOf(
                    Color.argb(0, 0, 100, 50),
                    Color.argb(60, 0, 80, 40),
                    Color.argb(0, 0, 60, 30)
                ),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, horizonY - 30f, width.toFloat(), horizonY + 80f, horizonGlow)

        // 文字（逐字浮现展开）
        if (message.isNotEmpty()) {
            val centerX = width / 2f
            val centerY = height * 0.8f

            textPaint.alpha = (textAlpha * 255).toInt()
            subTextPaint.alpha = (textAlpha * 230).toInt()
            drawContrastText(canvas, message, centerX, centerY, textPaint, ContrastTextType.MAIN)
            if (subMessage.isNotEmpty()) {
                drawContrastText(canvas, subMessage, centerX, centerY + 50f, subTextPaint, ContrastTextType.SUB)
            }
            textPaint.alpha = 255
            subTextPaint.alpha = 255
        }
    }

    override fun onUpdateAnimation() {
        frameCount++
        val w = width
        val h = height
        if (w == 0 || h == 0) return

        // 文字逐字展开（慢慢淡入，100帧完成）
        if (frameCount <= 100) {
            textAlpha = (frameCount / 100f).coerceIn(0f, 1f)
            textReveal = textAlpha
        } else {
            textAlpha = 1f
            textReveal = 1f
        }

        // 更新星点闪烁
        for (star in stars) {
            star.twinkle += star.twinkleSpeed
        }

        // 更新极光
        for (ray in rays) {
            // 波动相位推进（极光流动）
            ray.phase += ray.phaseSpeed
            // 颜色相位推进（颜色慢变）
            ray.colorPhase += ray.colorSpeed
            // 透明度呼吸感（在目标透明度附近缓慢波动）
            val breatheTarget = ray.targetAlpha + sin(frameCount * 0.015f + ray.phase) * 0.08f
            ray.alpha += (breatheTarget - ray.alpha) * 0.02f
            // X位置缓慢漂移
            ray.x += sin(ray.phase * 0.3f) * 0.2f * animationSpeed
            // 软约束X范围
            ray.x = ray.x.coerceIn(w * 0.05f, w * 0.95f)
        }
    }
}
