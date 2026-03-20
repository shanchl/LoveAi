package com.loveai.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.loveai.R
import com.loveai.manager.MusicManager
import kotlin.math.sin
import kotlin.random.Random

/**
 * 结尾页，负责做收尾展示、提供 Replay/Share 入口并延续音乐。
 */
class EndingActivity : AppCompatActivity() {

    private lateinit var heartContainer: FrameLayout
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvMessage: TextView
    private lateinit var btnReplay: Button
    private lateinit var btnShare: Button
    private lateinit var btnPlayPause: ImageButton
    private lateinit var tvMusicName: TextView
    private lateinit var musicControlBar: LinearLayout
    private lateinit var contentPanel: View
    private lateinit var haloView: View

    private val handler = Handler(Looper.getMainLooper())
    private var isPlaying = true
    private var heartRunnable: Runnable? = null
    private lateinit var heartCanvasView: HeartCanvasView

    private val hearts = mutableListOf<Heart>()
    private val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private data class Heart(
        var x: Float,
        var y: Float,
        var size: Float,
        var speed: Float,
        var alpha: Int,
        var rotation: Float,
        var rotationSpeed: Float,
        var driftPhase: Float,
        var driftSpeed: Float
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ending)

        initViews()
        initHeartView()
        initAnimations()
        startHeartAnimation()
        initMusic()
    }

    private fun initViews() {
        heartContainer = findViewById(R.id.heartContainer)
        tvTitle = findViewById(R.id.tvTitle)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        tvMessage = findViewById(R.id.tvMessage)
        btnReplay = findViewById(R.id.btnReplay)
        btnShare = findViewById(R.id.btnShare)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        tvMusicName = findViewById(R.id.tvMusicName)
        musicControlBar = findViewById(R.id.musicControlBar)
        contentPanel = findViewById(R.id.contentPanel)
        haloView = findViewById(R.id.vHalo)

        val messages = listOf(
            "Thank you for staying through every frame.\nSome feelings deserve a slower ending.",
            "This story does not rush the last heartbeat.\nIt lingers where love still glows.",
            "The lights can fade softly now.\nWhat stays is the warmth between us.",
            "Every page was only a way of saying it again:\nI still choose you."
        )
        tvMessage.text = messages.random()

        btnReplay.setOnClickListener { startMainActivity() }
        btnShare.setOnClickListener { shareMoment() }
        btnPlayPause.setOnClickListener { toggleMusic() }
    }

    private fun initHeartView() {
        heartCanvasView = HeartCanvasView(this)
        heartContainer.addView(
            heartCanvasView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun initAnimations() {
        contentPanel.alpha = 0f
        contentPanel.translationY = 36f
        haloView.alpha = 0.18f

        contentPanel.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(900L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        tvTitle.alpha = 0f
        tvTitle.translationY = 28f
        tvTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(900L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        handler.postDelayed({
            tvSubtitle.alpha = 0f
            tvSubtitle.animate().alpha(1f).setDuration(700L).start()
            tvMessage.alpha = 0f
            tvMessage.translationY = 24f
            tvMessage.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(800L)
                .start()
        }, 220L)

        handler.postDelayed({
            btnReplay.alpha = 0f
            btnReplay.translationX = -40f
            btnShare.alpha = 0f
            btnShare.translationX = 40f

            btnReplay.animate().alpha(1f).translationX(0f).setDuration(520L).start()
            btnShare.animate().alpha(1f).translationX(0f).setDuration(520L).start()
        }, 520L)

        val haloX = ObjectAnimator.ofFloat(haloView, "scaleX", 0.92f, 1.1f).apply {
            duration = 2400L
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }
        val haloY = ObjectAnimator.ofFloat(haloView, "scaleY", 0.92f, 1.1f).apply {
            duration = 2400L
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }
        val haloAlpha = ObjectAnimator.ofFloat(haloView, "alpha", 0.14f, 0.28f).apply {
            duration = 2400L
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }
        AnimatorSet().apply {
            playTogether(haloX, haloY, haloAlpha)
            start()
        }
    }

    private fun startHeartAnimation() {
        heartRunnable = object : Runnable {
            override fun run() {
                addHeart()
                updateHearts()
                heartCanvasView.invalidate()
                handler.postDelayed(this, 33L)
            }
        }
        handler.post(heartRunnable!!)
    }

    private fun addHeart() {
        val width = heartContainer.width
        if (width == 0) return

        hearts.add(
            Heart(
                x = Random.nextFloat() * width,
                y = heartContainer.height + 80f,
                size = Random.nextFloat() * 24f + 12f,
                speed = Random.nextFloat() * 2.2f + 0.8f,
                alpha = Random.nextInt(90, 190),
                rotation = Random.nextFloat() * 36f - 18f,
                rotationSpeed = Random.nextFloat() * 1.4f - 0.7f,
                driftPhase = Random.nextFloat() * (Math.PI * 2).toFloat(),
                driftSpeed = Random.nextFloat() * 0.05f + 0.015f
            )
        )

        if (hearts.size > 42) {
            hearts.removeAt(0)
        }
    }

    private fun updateHearts() {
        val height = heartContainer.height.toFloat()
        val iterator = hearts.iterator()
        while (iterator.hasNext()) {
            val heart = iterator.next()
            heart.y -= heart.speed
            heart.x += sin(heart.driftPhase) * 1.2f
            heart.rotation += heart.rotationSpeed
            heart.driftPhase += heart.driftSpeed
            heart.alpha = (heart.alpha - 1).coerceAtLeast(0)

            if (heart.y < -80f || heart.alpha <= 0 || heart.y > height + 80f) {
                iterator.remove()
            }
        }
    }

    private inner class HeartCanvasView(context: android.content.Context) : View(context) {
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            hearts.forEach { drawHeart(canvas, it) }
        }
    }

    private fun drawHeart(canvas: Canvas, heart: Heart) {
        heartPaint.color = Color.argb(heart.alpha, 255, 122, 170)
        heartPaint.style = Paint.Style.FILL
        heartPaint.setShadowLayer(18f, 0f, 0f, Color.argb((heart.alpha * 0.4f).toInt(), 255, 120, 180))

        canvas.save()
        canvas.translate(heart.x, heart.y)
        canvas.rotate(heart.rotation)

        val size = heart.size
        val path = Path().apply {
            moveTo(0f, -size / 2f)
            cubicTo(size / 2f, -size, size, -size / 3f, 0f, size)
            cubicTo(-size, -size / 3f, -size / 2f, -size, 0f, -size / 2f)
            close()
        }
        canvas.drawPath(path, heartPaint)
        canvas.restore()
    }

    private fun initMusic() {
        MusicManager.play()
        isPlaying = true
        updateMusicButton()
        updateMusicName()
    }

    private fun updateMusicName() {
        tvMusicName.text = MusicManager.getCurrentSongName()
    }

    private fun toggleMusic() {
        isPlaying = !isPlaying
        if (isPlaying) {
            MusicManager.play()
        } else {
            MusicManager.pause()
        }
        updateMusicButton()
    }

    private fun updateMusicButton() {
        btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun shareMoment() {
        val shareText = buildString {
            append("LoveAI\n")
            append(tvMessage.text)
            append("\n\nNow playing: ")
            append(MusicManager.getCurrentSongName())
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share"))
    }

    override fun onResume() {
        super.onResume()
        if (isPlaying) {
            MusicManager.play()
        }
        updateMusicName()
    }

    override fun onDestroy() {
        super.onDestroy()
        heartRunnable?.let { handler.removeCallbacks(it) }
    }
}
