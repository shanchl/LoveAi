package com.loveai.ui.effects

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import com.loveai.model.Effect
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class FireworkEffect @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseEffectView(context, attrs) {

    private enum class ShellType {
        CROWN,
        WILLOW,
        BOUQUET
    }

    private data class Spark(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val color: Int,
        val size: Float,
        var alpha: Float,
        var life: Float,
        val maxLife: Float,
        val drag: Float,
        val gravity: Float,
        val twinkleSpeed: Float
    )

    private data class Shockwave(
        var radius: Float,
        var alpha: Float,
        val color: Int,
        val strokeWidth: Float
    )

    private data class FireworkShell(
        var x: Float,
        var y: Float,
        val targetY: Float,
        val color: Int,
        val accentColor: Int,
        val shellType: ShellType,
        var vy: Float,
        var exploded: Boolean = false,
        val sparks: MutableList<Spark> = mutableListOf(),
        val shockwaves: MutableList<Shockwave> = mutableListOf()
    )

    private data class SkyStar(
        val x: Float,
        val y: Float,
        val size: Float,
        val alpha: Int,
        var twinkle: Float,
        val speed: Float
    )

    private val shells = mutableListOf<FireworkShell>()
    private val stars = mutableListOf<SkyStar>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var frameCount = 0
    private var textAlpha = 0f
    private var textScale = 0.84f
    private var textFloat = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return

        backgroundPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h.toFloat(),
            intArrayOf(
                Color.parseColor("#170914"),
                Color.parseColor("#0E0C1E"),
                Color.parseColor("#05060E")
            ),
            floatArrayOf(0f, 0.32f, 1f),
            Shader.TileMode.CLAMP
        )

        stars.clear()
        repeat(46) {
            stars += SkyStar(
                x = Random.nextFloat() * w,
                y = Random.nextFloat() * h * 0.44f,
                size = Random.nextFloat() * 2.4f + 0.6f,
                alpha = Random.nextInt(40, 135),
                twinkle = Random.nextFloat() * Math.PI.toFloat() * 2f,
                speed = Random.nextFloat() * 0.05f + 0.015f
            )
        }
    }

    override fun onEffectBound(effect: Effect) {
        shells.clear()
        frameCount = 0
        textAlpha = 0f
        textScale = 0.84f
        textFloat = 0f

        textPaint.apply {
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(14f, 0f, 2f, textGlowColor())
        }
        subTextPaint.apply {
            color = adjustAlpha(Color.WHITE, 235)
            textSize = 30f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
    }

    private fun randomPalette(): Pair<Int, Int> {
        return when (Random.nextInt(5)) {
            0 -> primaryColor to Color.WHITE
            1 -> secondaryColor to Color.parseColor("#FFDFA8")
            2 -> Color.parseColor("#FFD45D") to Color.parseColor("#FFF7D6")
            3 -> Color.parseColor("#A8D8FF") to Color.WHITE
            else -> primaryColor to secondaryColor
        }
    }

    private fun launchShell() {
        if (width == 0 || height == 0) return
        val (mainColor, accentColor) = randomPalette()
        val shellType = ShellType.values()[Random.nextInt(ShellType.values().size)]
        shells += FireworkShell(
            x = width * (0.18f + Random.nextFloat() * 0.64f),
            y = height + 36f,
            targetY = height * (0.18f + Random.nextFloat() * 0.24f),
            color = mainColor,
            accentColor = accentColor,
            shellType = shellType,
            vy = -(13f + Random.nextFloat() * 5f) * animationSpeed
        )
    }

    private fun addSpark(
        shell: FireworkShell,
        angle: Float,
        speed: Float,
        color: Int,
        size: Float,
        life: Float,
        drag: Float,
        gravity: Float,
        alpha: Float = 1f
    ) {
        shell.sparks += Spark(
            x = shell.x,
            y = shell.y,
            vx = cos(angle) * speed * animationSpeed,
            vy = sin(angle) * speed * animationSpeed,
            color = color,
            size = size,
            alpha = alpha,
            life = life,
            maxLife = life,
            drag = drag,
            gravity = gravity,
            twinkleSpeed = Random.nextFloat() * 0.1f + 0.04f
        )
    }

    private fun explodeShell(shell: FireworkShell) {
        val baseCount = particleCount.coerceIn(60, 150)
        when (shell.shellType) {
            ShellType.CROWN -> {
                repeat(baseCount) { index ->
                    val angle = (Math.PI * 2 * index / baseCount).toFloat()
                    val speed = Random.nextFloat() * 3.6f + 6.8f
                    addSpark(shell, angle, speed, shell.color, Random.nextFloat() * 3.4f + 2.2f, Random.nextFloat() * 34f + 42f, 0.986f, 0.085f)
                }
                repeat(baseCount / 3) {
                    val angle = Random.nextFloat() * (Math.PI * 2).toFloat()
                    addSpark(shell, angle, Random.nextFloat() * 2.6f + 3.2f, shell.accentColor, Random.nextFloat() * 2.4f + 1.2f, Random.nextFloat() * 24f + 26f, 0.982f, 0.08f)
                }
            }

            ShellType.WILLOW -> {
                repeat(baseCount + 20) {
                    val angle = Random.nextFloat() * (Math.PI * 2).toFloat()
                    addSpark(shell, angle, Random.nextFloat() * 2.8f + 4.2f, shell.color, Random.nextFloat() * 4f + 2f, Random.nextFloat() * 48f + 58f, 0.992f, 0.16f)
                }
                repeat(baseCount / 4) {
                    val angle = Random.nextFloat() * (Math.PI * 2).toFloat()
                    addSpark(shell, angle, Random.nextFloat() * 1.8f + 2.1f, shell.accentColor, Random.nextFloat() * 2f + 1f, Random.nextFloat() * 30f + 28f, 0.988f, 0.12f, 0.85f)
                }
            }

            ShellType.BOUQUET -> {
                val bursts = 5
                repeat(bursts) { cluster ->
                    val clusterAngle = (Math.PI * 2 * cluster / bursts + Random.nextFloat() * 0.25f).toFloat()
                    val clusterSpeed = Random.nextFloat() * 2.2f + 3.6f
                    repeat(baseCount / bursts) {
                        val angle = clusterAngle + (Random.nextFloat() - 0.5f) * 0.7f
                        addSpark(shell, angle, clusterSpeed + Random.nextFloat() * 2.4f, if (Random.nextBoolean()) shell.color else shell.accentColor, Random.nextFloat() * 3.1f + 1.8f, Random.nextFloat() * 32f + 34f, 0.984f, 0.1f)
                    }
                }
            }
        }

        shell.shockwaves += Shockwave(0f, 1f, shell.accentColor, 3.6f)
        shell.shockwaves += Shockwave(18f, 0.72f, Color.WHITE, 2.1f)
        shell.exploded = true
    }

    private fun drawSky(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        glowPaint.maskFilter = BlurMaskFilter(width * 0.12f, BlurMaskFilter.Blur.NORMAL)
        glowPaint.color = adjustAlpha(primaryColor, 22)
        canvas.drawCircle(width * 0.2f, height * 0.18f, width * 0.16f, glowPaint)
        glowPaint.color = adjustAlpha(secondaryColor, 18)
        canvas.drawCircle(width * 0.8f, height * 0.22f, width * 0.14f, glowPaint)
        glowPaint.maskFilter = null

        stars.forEach { star ->
            val shimmer = sin(star.twinkle) * 0.35f + 0.65f
            paint.color = Color.argb((star.alpha * shimmer).toInt().coerceIn(0, 255), 255, 255, 255)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(star.x, star.y, star.size * (0.8f + shimmer * 0.35f), paint)
        }
    }

    private fun drawShell(canvas: Canvas, shell: FireworkShell) {
        if (!shell.exploded) {
            glowPaint.maskFilter = BlurMaskFilter(18f, BlurMaskFilter.Blur.NORMAL)
            glowPaint.color = adjustAlpha(shell.color, 140)
            canvas.drawCircle(shell.x, shell.y, 6.8f, glowPaint)

            paint.shader = LinearGradient(
                shell.x,
                shell.y,
                shell.x,
                shell.y + 92f,
                intArrayOf(
                    adjustAlpha(shell.accentColor, 170),
                    adjustAlpha(shell.color, 84),
                    Color.argb(0, Color.red(shell.color), Color.green(shell.color), Color.blue(shell.color))
                ),
                floatArrayOf(0f, 0.3f, 1f),
                Shader.TileMode.CLAMP
            )
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3.2f
            canvas.drawLine(shell.x, shell.y, shell.x, shell.y + 92f, paint)
            paint.shader = null

            paint.style = Paint.Style.FILL
            paint.color = shell.accentColor
            canvas.drawCircle(shell.x, shell.y, 3.8f, paint)
            return
        }

        shell.shockwaves.forEach { wave ->
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = wave.strokeWidth
            paint.color = adjustAlpha(wave.color, (wave.alpha * 255).toInt().coerceIn(0, 255))
            canvas.drawCircle(shell.x, shell.y, wave.radius, paint)
        }

        shell.sparks.forEachIndexed { index, spark ->
            if (spark.life <= 0f) return@forEachIndexed
            val flicker = sin(frameCount * spark.twinkleSpeed + index * 0.35f) * 0.22f + 0.78f
            val alpha = (spark.alpha * 255 * flicker).toInt().coerceIn(0, 255)

            glowPaint.maskFilter = BlurMaskFilter(spark.size * 4f, BlurMaskFilter.Blur.NORMAL)
            glowPaint.color = adjustAlpha(spark.color, (alpha * 0.38f).toInt())
            canvas.drawCircle(spark.x, spark.y, spark.size * 1.9f, glowPaint)

            paint.style = Paint.Style.FILL
            paint.color = adjustAlpha(spark.color, alpha)
            canvas.drawCircle(spark.x, spark.y, spark.size * (0.58f + spark.life / spark.maxLife * 0.54f), paint)
        }
        glowPaint.maskFilter = null
    }

    override fun onDrawEffect(canvas: Canvas) {
        drawSky(canvas)
        shells.forEach { drawShell(canvas, it) }

        if (message.isNotEmpty()) {
            val centerX = width / 2f
            val centerY = height * 0.72f + sin(textFloat) * 4f
            canvas.save()
            canvas.scale(textScale, textScale, centerX, centerY)
            textPaint.alpha = (textAlpha * 255).toInt().coerceIn(0, 255)
            subTextPaint.alpha = (textAlpha * 240).toInt().coerceIn(0, 255)
            drawContrastText(canvas, message, centerX, centerY, textPaint, ContrastTextType.MAIN)
            if (subMessage.isNotEmpty()) {
                drawContrastText(canvas, subMessage, centerX, centerY + 48f, subTextPaint, ContrastTextType.SUB)
            }
            canvas.restore()
            textPaint.alpha = 255
            subTextPaint.alpha = 255
        }
    }

    override fun onUpdateAnimation() {
        frameCount++
        if (width == 0 || height == 0) return

        val progress = (frameCount / 56f).coerceIn(0f, 1f)
        val eased = 1f - (1f - progress) * (1f - progress)
        textAlpha = eased
        textScale = 0.84f + eased * 0.16f
        textFloat += 0.03f

        stars.forEach { it.twinkle += it.speed }

        val maxShells = (particleCount / 18).coerceIn(4, 8)
        val launchInterval = (34 / animationSpeed).toInt().coerceIn(16, 46)
        if (frameCount % launchInterval == 0 && shells.size < maxShells) {
            launchShell()
        }

        val removeList = mutableListOf<FireworkShell>()
        shells.forEach { shell ->
            if (!shell.exploded) {
                shell.y += shell.vy
                if (shell.y <= shell.targetY) {
                    explodeShell(shell)
                }
            } else {
                shell.shockwaves.forEach { wave ->
                    wave.radius += 4.8f * animationSpeed
                    wave.alpha -= 0.032f
                }
                shell.shockwaves.removeAll { it.alpha <= 0f }

                var allDead = true
                shell.sparks.forEach { spark ->
                    if (spark.life <= 0f) return@forEach
                    allDead = false
                    spark.x += spark.vx
                    spark.y += spark.vy
                    spark.vx *= spark.drag
                    spark.vy = spark.vy * spark.drag + spark.gravity
                    spark.life -= 1f
                    spark.alpha = (spark.life / spark.maxLife).coerceIn(0f, 1f)
                }
                if (allDead && shell.shockwaves.isEmpty()) {
                    removeList += shell
                }
            }
        }
        shells.removeAll(removeList)
    }
}
