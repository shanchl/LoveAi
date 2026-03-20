package com.loveai.ui.effects

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import com.loveai.model.Effect
import kotlin.math.*
import kotlin.random.Random

/**
 * 效果11：蝴蝶飞舞动画
 * 蝴蝶在屏幕中优雅飞翔，翅膀扇动，轨迹呈8字花形
 * 文字展示：缩放弹入效果
 */
class ButterflyEffect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseEffectView(context, attrs) {

    private data class Butterfly(
        var x: Float,
        var y: Float,
        var vx: Float,           // 速度X
        var vy: Float,           // 速度Y
        var wingPhase: Float,    // 翅膀扇动相位
        val wingSpeed: Float,    // 翅膀扇动速度
        val size: Float,         // 蝴蝶大小
        val color1: Int,         // 翅膀主色
        val color2: Int,         // 翅膀次色
        var alpha: Int,
        var wanderPhase: Float,  // 漫游相位（控制飞行轨迹曲线）
        val wanderRadius: Float, // 漫游半径
        var targetX: Float,      // 目标方向
        var targetY: Float,
        var framesSinceTarget: Int  // 多少帧后换目标
    )

    private data class Petal(
        val x: Float,
        val y: Float,
        val size: Float,
        val color: Int,
        val rotation: Float
    )

    private val butterflies = mutableListOf<Butterfly>()
    private val petals = mutableListOf<Petal>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint()
    private var frameCount = 0
    private var textScale = 0.3f
    private var textAlpha = 0f
    // 路径缓存，避免每帧 new
    private val wingPath = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // 柔美花园背景渐变
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(
                Color.parseColor("#1A2A1A"),  // 深绿顶部
                adjustAlpha(primaryColor, 0x12),
                Color.parseColor("#0D0A12")   // 深紫底部
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        // 初始化背景花瓣装饰
        petals.clear()
        val petalColors = intArrayOf(primaryColor, secondaryColor,
            Color.parseColor("#FFB6C1"), Color.parseColor("#E8D5E8"))
        repeat(30) {
            petals.add(
                Petal(
                    x = Random.nextFloat() * w,
                    y = Random.nextFloat() * h,
                    size = Random.nextFloat() * 12f + 4f,
                    color = petalColors[Random.nextInt(petalColors.size)],
                    rotation = Random.nextFloat() * 360f
                )
            )
        }

        // 初始化蝴蝶
        butterflies.clear()
        val count = (particleCount / 8).coerceIn(4, 12)
        repeat(count) {
            butterflies.add(createButterfly(w, h))
        }
    }

    override fun onEffectBound(effect: Effect) {
        textPaint.apply {
            color = Color.WHITE
            textSize = 46f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
            setShadowLayer(10f, 0f, 2f, textGlowColor())
        }
        subTextPaint.apply {
            color = adjustAlpha(secondaryColor, 0xDD)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
    }

    private fun createButterfly(w: Int, h: Int): Butterfly {
        val size = Random.nextFloat() * 20f + 14f
        val hue = Random.nextFloat() * 60f  // 色相偏移
        // 基于主/次色生成蝴蝶颜色
        val baseHsv = FloatArray(3)
        Color.colorToHSV(primaryColor, baseHsv)
        baseHsv[0] = (baseHsv[0] + hue) % 360f
        val c1 = Color.HSVToColor(baseHsv)
        Color.colorToHSV(secondaryColor, baseHsv)
        baseHsv[0] = (baseHsv[0] + hue * 0.5f) % 360f
        val c2 = Color.HSVToColor(baseHsv)

        val tx = Random.nextFloat() * w
        val ty = Random.nextFloat() * h
        return Butterfly(
            x = Random.nextFloat() * w,
            y = Random.nextFloat() * h,
            vx = (Random.nextFloat() - 0.5f) * 2f,
            vy = (Random.nextFloat() - 0.5f) * 2f,
            wingPhase = Random.nextFloat() * Math.PI.toFloat() * 2,
            wingSpeed = Random.nextFloat() * 0.15f + 0.1f,
            size = size,
            color1 = c1,
            color2 = c2,
            alpha = Random.nextInt(160, 240),
            wanderPhase = Random.nextFloat() * Math.PI.toFloat() * 2,
            wanderRadius = Random.nextFloat() * 80f + 40f,
            targetX = tx,
            targetY = ty,
            framesSinceTarget = Random.nextInt(0, 120)
        )
    }

    /**
     * 绘制单只蝴蝶（由两对翅膀组成）
     */
    private fun drawButterfly(canvas: Canvas, bf: Butterfly) {
        canvas.save()
        canvas.translate(bf.x, bf.y)

        // 翅膀扇动角度：用正弦波模拟
        val wingAngle = sin(bf.wingPhase) * 40f  // 最大40度扇动

        // 计算方向（朝向运动方向旋转蝴蝶）
        val dir = atan2(bf.vy, bf.vx) * (180f / Math.PI.toFloat())
        canvas.rotate(dir + 90f)  // +90 让蝴蝶朝上

        val alphaFactor = bf.alpha
        val s = bf.size

        // 上翅（较大）
        drawWingPair(canvas, bf, s * 1.2f, s * 0.9f, wingAngle, alphaFactor, isUpper = true)
        // 下翅（较小）
        drawWingPair(canvas, bf, s * 0.8f, s * 0.6f, wingAngle * 0.85f, alphaFactor, isUpper = false)

        // 身体（椭圆）
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = Color.argb((alphaFactor * 0.9f).toInt(), 40, 20, 10)
        canvas.drawOval(-s * 0.08f, -s * 0.7f, s * 0.08f, s * 0.7f, paint)

        // 触角
        paint.color = Color.argb(alphaFactor, 60, 40, 20)
        paint.strokeWidth = 1.5f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(-s * 0.05f, -s * 0.65f, -s * 0.25f, -s * 1.1f, paint)
        canvas.drawLine(s * 0.05f, -s * 0.65f, s * 0.25f, -s * 1.1f, paint)
        // 触角顶端小球
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(alphaFactor, 80, 50, 30)
        canvas.drawCircle(-s * 0.25f, -s * 1.1f, s * 0.06f, paint)
        canvas.drawCircle(s * 0.25f, -s * 1.1f, s * 0.06f, paint)

        canvas.restore()
    }

    private fun drawWingPair(
        canvas: Canvas, bf: Butterfly,
        wingW: Float, wingH: Float,
        wingAngle: Float, alpha: Int,
        isUpper: Boolean
    ) {
        val yOffset = if (isUpper) -wingH * 0.1f else wingH * 0.55f

        // 左翅
        canvas.save()
        canvas.translate(-wingW * 0.05f, yOffset)
        canvas.rotate(-wingAngle)
        drawSingleWing(canvas, bf, wingW, wingH, alpha, isLeft = true)
        canvas.restore()

        // 右翅（镜像）
        canvas.save()
        canvas.translate(wingW * 0.05f, yOffset)
        canvas.rotate(wingAngle)
        canvas.scale(-1f, 1f)
        drawSingleWing(canvas, bf, wingW, wingH, alpha, isLeft = false)
        canvas.restore()
    }

    private fun drawSingleWing(
        canvas: Canvas, bf: Butterfly,
        w: Float, h: Float, alpha: Int, isLeft: Boolean
    ) {
        wingPath.reset()
        // 翅膀形状（贝塞尔曲线模拟）
        wingPath.moveTo(0f, 0f)
        wingPath.cubicTo(
            -w * 0.3f, -h * 0.3f,
            -w * 1.1f, -h * 0.2f,
            -w * 0.9f, h * 0.4f
        )
        wingPath.cubicTo(
            -w * 0.7f, h * 0.9f,
            -w * 0.2f, h * 0.6f,
            0f, 0f
        )
        wingPath.close()

        // 翅膀渐变（主色到次色）
        val gradient = LinearGradient(
            -w, -h * 0.3f, 0f, h * 0.5f,
            intArrayOf(
                Color.argb(alpha, Color.red(bf.color1), Color.green(bf.color1), Color.blue(bf.color1)),
                Color.argb((alpha * 0.7f).toInt(), Color.red(bf.color2), Color.green(bf.color2), Color.blue(bf.color2))
            ),
            null, Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        paint.style = Paint.Style.FILL
        canvas.drawPath(wingPath, paint)

        // 翅膀纹路（半透明深色线条）
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(0.8f, w * 0.02f)
        paint.color = Color.argb((alpha * 0.3f).toInt(), 20, 10, 5)
        canvas.drawPath(wingPath, paint)

        // 中心纹脉
        paint.color = Color.argb((alpha * 0.25f).toInt(), 0, 0, 0)
        paint.strokeWidth = 1f
        canvas.drawLine(0f, 0f, -w * 0.7f, h * 0.3f, paint)
        canvas.drawLine(0f, 0f, -w * 0.8f, -h * 0.1f, paint)
    }

    override fun onDrawEffect(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 背景花瓣装饰（静态）
        for (petal in petals) {
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.color = adjustAlpha(petal.color, 0x30)
            canvas.save()
            canvas.translate(petal.x, petal.y)
            canvas.rotate(petal.rotation)
            // 简单椭圆模拟花瓣
            canvas.drawOval(-petal.size, -petal.size * 0.5f, petal.size, petal.size * 0.5f, paint)
            canvas.restore()
        }

        // 蝴蝶
        for (bf in butterflies) {
            drawButterfly(canvas, bf)
        }

        // 文字（缩放弹入）
        if (message.isNotEmpty()) {
            val centerX = width / 2f
            val centerY = height * 0.56f

            canvas.save()
            canvas.scale(textScale, textScale, centerX, centerY)
            textPaint.alpha = (textAlpha * 255).toInt()
            subTextPaint.alpha = (textAlpha * 230).toInt()
            drawContrastText(canvas, message, centerX, centerY, textPaint, ContrastTextType.MAIN)
            if (subMessage.isNotEmpty()) {
                drawContrastText(canvas, subMessage, centerX, centerY + 52f, subTextPaint, ContrastTextType.SUB)
            }
            canvas.restore()
            textPaint.alpha = 255
            subTextPaint.alpha = 255
        }
    }

    override fun onUpdateAnimation() {
        frameCount++
        val w = width
        val h = height
        if (w == 0 || h == 0) return

        // 文字缩放弹入（弹簧效果）
        if (frameCount <= 90) {
            val t = frameCount / 90f
            // 弹簧公式：逐渐收敛到1.0
            textScale = 1f - 0.7f * exp(-t * 5f) * cos(t * 18f)
            textAlpha = (t * 1.5f).coerceIn(0f, 1f)
        } else {
            textScale = 1f
            textAlpha = 1f
        }

        // 更新蝴蝶
        for (bf in butterflies) {
            bf.wingPhase += bf.wingSpeed * animationSpeed
            bf.wanderPhase += 0.01f
            bf.framesSinceTarget++

            // 定期更换目标位置（模拟蝴蝶漫飞）
            if (bf.framesSinceTarget > 150 + Random.nextInt(100)) {
                bf.targetX = Random.nextFloat() * w
                bf.targetY = Random.nextFloat() * h * 0.9f
                bf.framesSinceTarget = 0
            }

            // 向目标方向移动（加入曲线游走）
            val dx = bf.targetX - bf.x
            val dy = bf.targetY - bf.y
            val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
            val baseSpeed = (Random.nextFloat() * 0.5f + 0.8f) * animationSpeed

            // 追踪目标 + 正弦曲线游走叠加
            bf.vx = (bf.vx + dx / dist * 0.15f) * 0.92f +
                    sin(bf.wanderPhase * 2.3f) * 0.5f
            bf.vy = (bf.vy + dy / dist * 0.15f) * 0.92f +
                    cos(bf.wanderPhase * 1.7f) * 0.5f

            // 限速
            val speed = sqrt(bf.vx * bf.vx + bf.vy * bf.vy).coerceAtLeast(0.01f)
            val maxSpeed = 3f * baseSpeed
            if (speed > maxSpeed) {
                bf.vx = bf.vx / speed * maxSpeed
                bf.vy = bf.vy / speed * maxSpeed
            }

            bf.x += bf.vx
            bf.y += bf.vy

            // 边界软约束（靠近边界时反弹）
            val margin = 60f
            if (bf.x < margin) bf.vx += 0.3f
            if (bf.x > w - margin) bf.vx -= 0.3f
            if (bf.y < margin) bf.vy += 0.3f
            if (bf.y > h - margin) bf.vy -= 0.3f
        }
    }
}
