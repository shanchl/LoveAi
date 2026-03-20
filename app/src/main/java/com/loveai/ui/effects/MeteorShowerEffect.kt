package com.loveai.ui.effects

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import com.loveai.model.Effect
import kotlin.math.*
import kotlin.random.Random

/**
 * 效果10：流星雨动画
 * 流星从右上方高速划过夜空，带发光拖尾
 * 文字展示：星光闪烁渐入效果
 */
class MeteorShowerEffect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseEffectView(context, attrs) {

    private data class Meteor(
        var x: Float,
        var y: Float,
        val angle: Float,       // 运动角度（弧度）
        val speed: Float,
        val length: Float,      // 拖尾长度
        val thickness: Float,
        val color: Int,
        var alpha: Int,
        var life: Float,        // 0~1，1为存活，0为消亡
        var fadeIn: Float       // 淡入进度
    )

    private data class StarPoint(
        val x: Float,
        val y: Float,
        val size: Float,
        var twinkle: Float,
        val twinkleSpeed: Float
    )

    private val meteors = mutableListOf<Meteor>()
    private val stars = mutableListOf<StarPoint>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint()
    private var frameCount = 0
    private var textAlpha = 0f
    private var textOffset = 60f  // 文字从上方滑入的偏移

    // 流星颜色池（偏白蓝冷色系）
    private val meteorColors = intArrayOf(
        Color.WHITE,
        Color.parseColor("#E0F0FF"),
        Color.parseColor("#C0E8FF"),
        Color.parseColor("#FFE8C0"),
        Color.parseColor("#FFD0A0")
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // 深邃星空背景
        bgPaint.shader = RadialGradient(
            w / 2f, h * 0.3f,
            h * 0.8f,
            intArrayOf(
                Color.parseColor("#1A1A3A"),
                Color.parseColor("#0A0A1E"),
                Color.parseColor("#050510")
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )

        // 初始化背景星点
        stars.clear()
        val starCount = 120
        repeat(starCount) {
            stars.add(
                StarPoint(
                    x = Random.nextFloat() * w,
                    y = Random.nextFloat() * h,
                    size = Random.nextFloat() * 2.5f + 0.5f,
                    twinkle = Random.nextFloat() * Math.PI.toFloat() * 2,
                    twinkleSpeed = Random.nextFloat() * 0.04f + 0.01f
                )
            )
        }

        // 初始化初始流星
        meteors.clear()
        val count = (particleCount / 8).coerceIn(3, 10)
        repeat(count) {
            meteors.add(createMeteor(w, h, true))
        }
    }

    override fun onEffectBound(effect: Effect) {
        textPaint.apply {
            color = Color.WHITE
            textSize = 44f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
            setShadowLayer(12f, 0f, 3f, textGlowColor())
        }
        subTextPaint.apply {
            color = adjustAlpha(secondaryColor, 0xCC)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
    }

    private fun createMeteor(w: Int, h: Int, randomPos: Boolean = false): Meteor {
        // 流星从屏幕上方/右方随机位置出发，向左下方运动
        val baseAngle = (Math.PI * 0.6).toFloat()  // ~108度，大约左下方向
        val angleVariance = (Math.PI * 0.1).toFloat()
        val angle = baseAngle + (Random.nextFloat() - 0.5f) * angleVariance

        val speed = (Random.nextFloat() * 15f + 10f) * animationSpeed
        val startX = if (randomPos) {
            Random.nextFloat() * w * 1.5f + w * 0.2f
        } else {
            w.toFloat() + Random.nextFloat() * w * 0.5f
        }
        val startY = if (randomPos) {
            Random.nextFloat() * h * 0.5f - h * 0.2f
        } else {
            Random.nextFloat() * h * 0.3f - h * 0.1f
        }

        val baseColor = meteorColors[Random.nextInt(meteorColors.size)]
        return Meteor(
            x = startX,
            y = startY,
            angle = angle,
            speed = speed,
            length = Random.nextFloat() * 200f + 80f,
            thickness = Random.nextFloat() * 2.5f + 1f,
            color = baseColor,
            alpha = Random.nextInt(180, 255),
            life = 1f,
            fadeIn = 0f
        )
    }

    private fun drawMeteor(canvas: Canvas, meteor: Meteor) {
        val tailX = meteor.x - cos(meteor.angle) * meteor.length
        val tailY = meteor.y - sin(meteor.angle) * meteor.length

        val alphaFactor = meteor.life * meteor.fadeIn
        val meteAlpha = (meteor.alpha * alphaFactor).toInt().coerceIn(0, 255)
        if (meteAlpha <= 0) return

        // 主体拖尾（线性渐变：头部亮，尾部透明）
        val gradient = LinearGradient(
            meteor.x, meteor.y,
            tailX, tailY,
            intArrayOf(
                Color.argb(meteAlpha, Color.red(meteor.color), Color.green(meteor.color), Color.blue(meteor.color)),
                Color.argb(0, Color.red(meteor.color), Color.green(meteor.color), Color.blue(meteor.color))
            ),
            null,
            Shader.TileMode.CLAMP
        )

        paint.shader = gradient
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = meteor.thickness
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(meteor.x, meteor.y, tailX, tailY, paint)

        // 流星头部发光点
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(meteAlpha, 255, 255, 255)
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((meteAlpha * 0.4f).toInt(), Color.red(meteor.color), Color.green(meteor.color), Color.blue(meteor.color))
            maskFilter = BlurMaskFilter(meteor.thickness * 4f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(meteor.x, meteor.y, meteor.thickness * 2f, glowPaint)
        canvas.drawCircle(meteor.x, meteor.y, meteor.thickness, paint)
    }

    override fun onDrawEffect(canvas: Canvas) {
        // 背景
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 背景星点（闪烁）
        for (star in stars) {
            val brightness = (sin(star.twinkle) * 0.4f + 0.6f)
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.color = Color.argb((brightness * 200).toInt(), 255, 255, 255)
            canvas.drawCircle(star.x, star.y, star.size * brightness, paint)
        }

        // 流星
        for (meteor in meteors) {
            drawMeteor(canvas, meteor)
        }

        // 文字（从上方滑入淡入）
        if (message.isNotEmpty()) {
            val centerX = width / 2f
            val centerY = height * 0.55f + textOffset

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
        val w = width
        val h = height
        if (w == 0 || h == 0) return

        // 文字从上方滑入
        if (frameCount < 80) {
            val progress = (frameCount / 80f).coerceIn(0f, 1f)
            // 缓动函数：ease-out
            val eased = 1f - (1f - progress) * (1f - progress)
            textAlpha = eased
            textOffset = 60f * (1f - eased)
        } else {
            textAlpha = 1f
            textOffset = 0f
        }

        // 更新背景星点闪烁
        for (star in stars) {
            star.twinkle += star.twinkleSpeed
        }

        // 更新流星
        val toRemove = mutableListOf<Meteor>()
        for (meteor in meteors) {
            meteor.x += cos(meteor.angle) * meteor.speed
            meteor.y += sin(meteor.angle) * meteor.speed
            meteor.fadeIn = (meteor.fadeIn + 0.06f).coerceAtMost(1f)
            // 飞出屏幕后标记删除
            if (meteor.x < -meteor.length * 2 || meteor.y > h + meteor.length) {
                toRemove.add(meteor)
            }
        }
        meteors.removeAll(toRemove)

        // 补充流星（控制密度）
        val targetCount = (particleCount / 8).coerceIn(3, 10)
        // 随机时机生成新流星
        if (meteors.size < targetCount && Random.nextFloat() < 0.12f) {
            meteors.add(createMeteor(w, h, false))
        }
    }
}
