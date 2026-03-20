package com.loveai.ui.effects

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import com.loveai.model.Effect
import kotlin.math.*
import kotlin.random.Random

/**
 * 效果2：烟花绽放效果
 * 支持多种变体配置（颜色、数量等）
 */
class FireworkEffect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseEffectView(context, attrs) {

    private data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var color: Int,
        var alpha: Float,
        var size: Float,
        var life: Float,
        var maxLife: Float
    )

    private data class Firework(
        var x: Float,
        var y: Float,
        var targetY: Float,
        var vy: Float,
        var color: Int,
        var exploded: Boolean = false,
        var particles: MutableList<Particle> = mutableListOf()
    )

    private val fireworks = mutableListOf<Firework>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var frameCount = 0
    private val bgPaint = Paint()
    private var textAlpha = 0f
    private var textScale = 0.6f
    private var glowPhase = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bgPaint.color = backgroundColor
    }

    override fun onEffectBound(effect: Effect) {
        fireworks.clear()
        frameCount = 0
        textAlpha = 0f
        textScale = 0.6f
        glowPhase = 0f
        textPaint.apply {
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(10f, 0f, 0f, textGlowColor())
        }
        subTextPaint.apply {
            color = adjustAlpha(secondaryColor, 0xFF)
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
    }

    private fun generateVariantColor(): Int {
        // 基于主色调生成变体颜色
        return if (Random.nextBoolean()) {
            primaryColor
        } else {
            secondaryColor
        }
    }

    private fun launchFirework() {
        if (width == 0 || height == 0) return
        val color = generateVariantColor()
        fireworks.add(
            Firework(
                x = Random.nextFloat() * width * 0.8f + width * 0.1f,
                y = height.toFloat(),
                targetY = Random.nextFloat() * height * 0.5f + height * 0.1f,
                vy = -(Random.nextFloat() * 12f + 10f) * animationSpeed,
                color = color
            )
        )
    }

    private fun explodeFirework(fw: Firework) {
        val count = particleCount.coerceIn(40, 120)
        repeat(count) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2
            val speed = Random.nextFloat() * 8f + 2f
            val life = Random.nextFloat() * 60f + 40f
            fw.particles.add(
                Particle(
                    x = fw.x,
                    y = fw.y,
                    vx = cos(angle) * speed * animationSpeed,
                    vy = sin(angle) * speed * animationSpeed,
                    color = fw.color,
                    alpha = 1f,
                    size = Random.nextFloat() * 4f + 2f,
                    life = life,
                    maxLife = life
                )
            )
        }
        fw.exploded = true
    }

    override fun onDrawEffect(canvas: Canvas) {
        // 深色背景（带残影效果）
        paint.color = adjustAlpha(backgroundColor, 0x1A)
        paint.alpha = 30
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint.also { it.alpha = 200 })

        // 绘制烟花和粒子
        for (fw in fireworks) {
            if (!fw.exploded) {
                // 绘制上升中的烟花
                paint.color = fw.color
                paint.alpha = 255
                paint.style = Paint.Style.FILL
                canvas.drawCircle(fw.x, fw.y, 4f, paint)
            } else {
                // 绘制爆炸粒子
                for (p in fw.particles) {
                    if (p.life <= 0) continue
                    paint.color = p.color
                    paint.alpha = (p.alpha * 255).toInt().coerceIn(0, 255)
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(p.x, p.y, p.size * (p.life / p.maxLife), paint)
                }
            }
        }

        // 绘制爱意文字（带对比度保护 + 缩放弹入 + 呼吸发光）
        if (message.isNotEmpty()) {
            val centerX = width / 2f
            val centerY = height * 0.68f

            // 应用缩放 + 呼吸发光效果
            canvas.save()
            canvas.translate(centerX, centerY)
            canvas.scale(textScale, textScale)
            canvas.translate(-centerX, -centerY)

            // 呼吸发光 - 动态调整 shadowLayer
            val glowIntensity = (sin(glowPhase) * 0.5f + 0.5f) * 12f + 4f
            textPaint.setShadowLayer(glowIntensity, 0f, 0f, textGlowColor())
            textPaint.alpha = (textAlpha * 255).toInt()
            subTextPaint.alpha = (textAlpha * 255).toInt()

            drawContrastText(canvas, message, centerX, centerY, textPaint, ContrastTextType.MAIN)
            if (subMessage.isNotEmpty()) {
                drawContrastText(canvas, subMessage, centerX, centerY + 48f, subTextPaint, ContrastTextType.SUB)
            }

            textPaint.alpha = 255
            subTextPaint.alpha = 255
            canvas.restore()
        }
    }

    override fun onUpdateAnimation() {
        frameCount++
        if (width == 0 || height == 0) return

        // 缩放弹入动画（前 40 帧）
        if (frameCount < 40) {
            val progress = (frameCount / 40f).coerceIn(0f, 1f)
            // easeOutBack 弹性缓动
            val t = progress - 1f
            val eased = t * t * ((1.7f + 1f) * t + 1.7f) + 1f
            textScale = 0.6f + 0.4f * eased
            textAlpha = (progress * 1.5f).coerceIn(0f, 1f)
        } else {
            textScale = 1f
            textAlpha = 1f
        }
        // 呼吸发光相位
        glowPhase += 0.04f

        val maxFireworks = (particleCount / 15).coerceIn(3, 8)
        
        // 定期发射烟花
        val launchInterval = (40 / animationSpeed).toInt().coerceIn(20, 60)
        if (frameCount % launchInterval == 0 && fireworks.size < maxFireworks) {
            launchFirework()
        }

        val toRemove = mutableListOf<Firework>()
        for (fw in fireworks) {
            if (!fw.exploded) {
                fw.y += fw.vy
                if (fw.y <= fw.targetY) {
                    explodeFirework(fw)
                }
            } else {
                // 更新粒子
                var allDead = true
                for (p in fw.particles) {
                    if (p.life <= 0) continue
                    allDead = false
                    p.x += p.vx
                    p.y += p.vy
                    p.vy += 0.15f // 重力
                    p.vx *= 0.98f // 空气阻力
                    p.vy *= 0.98f
                    p.life -= 1f
                    p.alpha = p.life / p.maxLife
                }
                if (allDead) toRemove.add(fw)
            }
        }
        fireworks.removeAll(toRemove)
    }
}
