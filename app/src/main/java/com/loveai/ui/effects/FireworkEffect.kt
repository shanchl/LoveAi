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
        CHRYSANTHEMUM,
        WILLOW,
        FAN
    }

    private data class TrailSpark(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var previousX: Float,
        var previousY: Float,
        val color: Int,
        val width: Float,
        var alpha: Float,
        var life: Float,
        val maxLife: Float,
        val drag: Float,
        val gravity: Float,
        val tailFactor: Float
    )

    private data class FireworkShell(
        var x: Float,
        var y: Float,
        val targetY: Float,
        val mainColor: Int,
        val accentColor: Int,
        val shellType: ShellType,
        var vy: Float,
        var exploded: Boolean = false,
        val sparks: MutableList<TrailSpark> = mutableListOf()
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
    private var textScale = 0.86f
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
                Color.parseColor("#160913"),
                Color.parseColor("#0D0A1A"),
                Color.parseColor("#05060D")
            ),
            floatArrayOf(0f, 0.34f, 1f),
            Shader.TileMode.CLAMP
        )

        stars.clear()
        repeat(42) {
            stars += SkyStar(
                x = Random.nextFloat() * w,
                y = Random.nextFloat() * h * 0.42f,
                size = Random.nextFloat() * 2.3f + 0.6f,
                alpha = Random.nextInt(36, 128),
                twinkle = Random.nextFloat() * Math.PI.toFloat() * 2f,
                speed = Random.nextFloat() * 0.05f + 0.015f
            )
        }
    }

    override fun onEffectBound(effect: Effect) {
        shells.clear()
        frameCount = 0
        textAlpha = 0f
        textScale = 0.86f
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
            1 -> secondaryColor to Color.parseColor("#FFF1C7")
            2 -> Color.parseColor("#FFD560") to Color.parseColor("#FFF9EA")
            3 -> Color.parseColor("#9FD7FF") to Color.WHITE
            else -> primaryColor to secondaryColor
        }
    }

    private fun launchShell() {
        val (mainColor, accentColor) = randomPalette()
        shells += FireworkShell(
            x = width * (0.18f + Random.nextFloat() * 0.64f),
            y = height + 30f,
            targetY = height * (0.17f + Random.nextFloat() * 0.22f),
            mainColor = mainColor,
            accentColor = accentColor,
            shellType = ShellType.values()[Random.nextInt(ShellType.values().size)],
            vy = -(13f + Random.nextFloat() * 4.6f) * animationSpeed
        )
    }

    private fun addSpark(
        shell: FireworkShell,
        angle: Float,
        speed: Float,
        color: Int,
        width: Float,
        life: Float,
        drag: Float,
        gravity: Float,
        tailFactor: Float
    ) {
        shell.sparks += TrailSpark(
            x = shell.x,
            y = shell.y,
            vx = cos(angle) * speed * animationSpeed,
            vy = sin(angle) * speed * animationSpeed,
            previousX = shell.x,
            previousY = shell.y,
            color = color,
            width = width,
            alpha = 1f,
            life = life,
            maxLife = life,
            drag = drag,
            gravity = gravity,
            tailFactor = tailFactor
        )
    }

    private fun explodeShell(shell: FireworkShell) {
        val baseCount = particleCount.coerceIn(56, 132)
        when (shell.shellType) {
            ShellType.CHRYSANTHEMUM -> {
                repeat(baseCount) { index ->
                    val angle = (Math.PI * 2 * index / baseCount).toFloat()
                    addSpark(
                        shell = shell,
                        angle = angle,
                        speed = Random.nextFloat() * 3.4f + 6.2f,
                        color = if (index % 5 == 0) shell.accentColor else shell.mainColor,
                        width = Random.nextFloat() * 2f + 1.8f,
                        life = Random.nextFloat() * 30f + 30f,
                        drag = 0.986f,
                        gravity = 0.08f,
                        tailFactor = Random.nextFloat() * 16f + 18f
                    )
                }
            }

            ShellType.WILLOW -> {
                repeat(baseCount + 16) {
                    val angle = Random.nextFloat() * (Math.PI * 2).toFloat()
                    addSpark(
                        shell = shell,
                        angle = angle,
                        speed = Random.nextFloat() * 2.6f + 4.4f,
                        color = if (Random.nextBoolean()) shell.mainColor else shell.accentColor,
                        width = Random.nextFloat() * 2.2f + 1.5f,
                        life = Random.nextFloat() * 42f + 44f,
                        drag = 0.992f,
                        gravity = 0.16f,
                        tailFactor = Random.nextFloat() * 22f + 26f
                    )
                }
            }

            ShellType.FAN -> {
                repeat(baseCount) {
                    val angle = Math.toRadians((210 + Random.nextInt(-42, 42)).toDouble()).toFloat()
                    addSpark(
                        shell = shell,
                        angle = angle,
                        speed = Random.nextFloat() * 3.2f + 4.8f,
                        color = if (Random.nextInt(4) == 0) shell.accentColor else shell.mainColor,
                        width = Random.nextFloat() * 1.8f + 1.6f,
                        life = Random.nextFloat() * 28f + 26f,
                        drag = 0.987f,
                        gravity = 0.1f,
                        tailFactor = Random.nextFloat() * 14f + 16f
                    )
                }
            }
        }
        shell.exploded = true
    }

    private fun drawSky(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        glowPaint.maskFilter = BlurMaskFilter(width * 0.12f, BlurMaskFilter.Blur.NORMAL)
        glowPaint.color = adjustAlpha(primaryColor, 20)
        canvas.drawCircle(width * 0.22f, height * 0.18f, width * 0.16f, glowPaint)
        glowPaint.color = adjustAlpha(secondaryColor, 16)
        canvas.drawCircle(width * 0.8f, height * 0.2f, width * 0.14f, glowPaint)
        glowPaint.maskFilter = null

        stars.forEach { star ->
            val shimmer = sin(star.twinkle) * 0.35f + 0.65f
            paint.style = Paint.Style.FILL
            paint.color = Color.argb((star.alpha * shimmer).toInt().coerceIn(0, 255), 255, 255, 255)
            canvas.drawCircle(star.x, star.y, star.size * (0.82f + shimmer * 0.32f), paint)
        }
    }

    private fun drawRisingShell(canvas: Canvas, shell: FireworkShell) {
        glowPaint.maskFilter = BlurMaskFilter(16f, BlurMaskFilter.Blur.NORMAL)
        glowPaint.color = adjustAlpha(shell.mainColor, 150)
        canvas.drawCircle(shell.x, shell.y, 5.8f, glowPaint)

        paint.shader = LinearGradient(
            shell.x,
            shell.y,
            shell.x,
            shell.y + 86f,
            intArrayOf(
                adjustAlpha(shell.accentColor, 176),
                adjustAlpha(shell.mainColor, 100),
                Color.argb(0, Color.red(shell.mainColor), Color.green(shell.mainColor), Color.blue(shell.mainColor))
            ),
            floatArrayOf(0f, 0.24f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 3.4f
        canvas.drawLine(shell.x, shell.y, shell.x, shell.y + 86f, paint)
        paint.shader = null

        paint.style = Paint.Style.FILL
        paint.color = shell.accentColor
        canvas.drawCircle(shell.x, shell.y, 3.2f, paint)
    }

    private fun drawSpark(canvas: Canvas, spark: TrailSpark) {
        val intensity = (spark.life / spark.maxLife).coerceIn(0f, 1f)
        val alpha = (spark.alpha * 255).toInt().coerceIn(0, 255)
        val tailX = spark.x - (spark.x - spark.previousX) * spark.tailFactor
        val tailY = spark.y - (spark.y - spark.previousY) * spark.tailFactor

        paint.shader = LinearGradient(
            spark.x,
            spark.y,
            tailX,
            tailY,
            intArrayOf(
                Color.argb(alpha, 255, 255, 255),
                Color.argb((alpha * 0.9f).toInt(), Color.red(spark.color), Color.green(spark.color), Color.blue(spark.color)),
                Color.argb(0, Color.red(spark.color), Color.green(spark.color), Color.blue(spark.color))
            ),
            floatArrayOf(0f, 0.32f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = spark.width * (0.75f + intensity * 0.45f)
        canvas.drawLine(spark.x, spark.y, tailX, tailY, paint)
        paint.shader = null

        glowPaint.maskFilter = BlurMaskFilter(spark.width * 3.8f, BlurMaskFilter.Blur.NORMAL)
        glowPaint.color = adjustAlpha(spark.color, (alpha * 0.34f).toInt())
        canvas.drawCircle(spark.x, spark.y, spark.width * 1.5f, glowPaint)

        paint.style = Paint.Style.FILL
        paint.color = Color.argb(alpha, 255, 255, 255)
        canvas.drawCircle(spark.x, spark.y, spark.width * 0.42f, paint)
    }

    override fun onDrawEffect(canvas: Canvas) {
        drawSky(canvas)

        shells.forEach { shell ->
            if (!shell.exploded) {
                drawRisingShell(canvas, shell)
            } else {
                shell.sparks.forEach { spark ->
                    if (spark.life > 0f) {
                        drawSpark(canvas, spark)
                    }
                }
            }
        }
        glowPaint.maskFilter = null

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
        textScale = 0.86f + eased * 0.14f
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
                var allDead = true
                shell.sparks.forEach { spark ->
                    if (spark.life <= 0f) return@forEach
                    allDead = false
                    spark.previousX = spark.x
                    spark.previousY = spark.y
                    spark.x += spark.vx
                    spark.y += spark.vy
                    spark.vx *= spark.drag
                    spark.vy = spark.vy * spark.drag + spark.gravity
                    spark.life -= 1f
                    spark.alpha = (spark.life / spark.maxLife).coerceIn(0f, 1f)
                }
                if (allDead) {
                    removeList += shell
                }
            }
        }
        shells.removeAll(removeList)
    }
}
