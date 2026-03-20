package com.loveai.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import kotlin.random.Random

/**
 * 结尾页面
 * 展示浪漫的结束画面，提供重播和分享功能
 */
class EndingActivity : AppCompatActivity() {

    private lateinit var heartContainer: FrameLayout
    private lateinit var tvTitle: TextView
    private lateinit var tvMessage: TextView
    private lateinit var btnReplay: Button
    private lateinit var btnShare: Button
    private lateinit var btnPlayPause: ImageButton
    private lateinit var tvMusicName: TextView
    private lateinit var musicControlBar: LinearLayout

    private val handler = Handler(Looper.getMainLooper())
    private var isPlaying = true
    private var heartRunnable: Runnable? = null

    // 爱心相关
    private val hearts = mutableListOf<Heart>()
    private val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private data class Heart(
        var x: Float,
        var y: Float,
        var size: Float,
        var speed: Float,
        var alpha: Int,
        var rotation: Float,
        var rotationSpeed: Float
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ending)

        initViews()
        initAnimations()
        startHeartAnimation()
        initMusic()
    }

    private fun initViews() {
        heartContainer = findViewById(R.id.heartContainer)
        tvTitle = findViewById(R.id.tvTitle)
        tvMessage = findViewById(R.id.tvMessage)
        btnReplay = findViewById(R.id.btnReplay)
        btnShare = findViewById(R.id.btnShare)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        tvMusicName = findViewById(R.id.tvMusicName)
        musicControlBar = findViewById(R.id.musicControlBar)

        // 随机选择一条感谢语
        val messages = listOf(
            "感谢这段美好的时光\n愿我们的爱永远绽放",
            "和你在一起的每一天\n都是最珍贵的回忆",
            "love is in the air\n永远爱你",
            "星河万顷你是唯一的风景\n我爱",
            "愿执子之手 与子偕老\n永远在一起"
        )
        tvMessage.text = messages.random()

        btnReplay.setOnClickListener {
            startMainActivity()
        }

        btnShare.setOnClickListener {
            shareMoment()
        }

        btnPlayPause.setOnClickListener {
            toggleMusic()
        }
    }

    private fun initAnimations() {
        // 标题淡入 + 上浮
        tvTitle.alpha = 0f
        tvTitle.translationY = 50f
        tvTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(1000)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // 消息延迟显示
        handler.postDelayed({
            tvMessage.alpha = 0f
            tvMessage.animate()
                .alpha(1f)
                .setDuration(800)
                .start()
        }, 500)

        // 按钮延迟显示
        handler.postDelayed({
            btnReplay.alpha = 0f
            btnReplay.translationX = -50f
            btnShare.alpha = 0f
            btnShare.translationX = 50f

            btnReplay.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(500)
                .start()

            btnShare.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(500)
                .start()
        }, 1000)
    }

    private fun startHeartAnimation() {
        heartRunnable = object : Runnable {
            override fun run() {
                // 添加新爱心
                addHeart()
                
                // 更新现有爱心
                updateHearts()
                
                // 重绘
                heartContainer.invalidate()
                
                // 继续循环
                handler.postDelayed(this, 50)
            }
        }
        handler.post(heartRunnable!!)
    }

    private fun addHeart() {
        val width = heartContainer.width
        if (width == 0) return

        val heart = Heart(
            x = Random.nextFloat() * width,
            y = -50f,
            size = Random.nextFloat() * 30f + 15f,
            speed = Random.nextFloat() * 2f + 1f,
            alpha = Random.nextInt(150, 255),
            rotation = Random.nextFloat() * 30f - 15f,
            rotationSpeed = Random.nextFloat() * 2f - 1f
        )
        hearts.add(heart)

        // 限制爱心数量
        if (hearts.size > 30) {
            hearts.removeAt(0)
        }
    }

    private fun updateHearts() {
        val height = heartContainer.height.toFloat()
        val iterator = hearts.iterator()
        while (iterator.hasNext()) {
            val heart = iterator.next()
            heart.y += heart.speed
            heart.rotation += heart.rotationSpeed
            
            if (heart.y > height + 50) {
                iterator.remove()
            }
        }
    }

    // 自定义 View 来绘制爱心
    private fun initHeartView() {
        val customView = object : View(this) {
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                for (heart in hearts) {
                    drawHeart(canvas, heart)
                }
            }
        }
        heartContainer.addView(customView)
    }

    private fun drawHeart(canvas: Canvas, heart: Heart) {
        heartPaint.color = Color.argb(heart.alpha, 255, 100, 150)
        heartPaint.style = Paint.Style.FILL

        canvas.save()
        canvas.translate(heart.x, heart.y)
        canvas.rotate(heart.rotation)

        val path = android.graphics.Path()
        val size = heart.size
        path.moveTo(0f, -size / 2)
        path.cubicTo(size / 2, -size, size, -size / 3, 0f, size)
        path.cubicTo(-size, -size / 3, -size / 2, -size, 0f, -size / 2)

        canvas.drawPath(path, heartPaint)
        canvas.restore()
    }

    private fun initMusic() {
        // 确保音乐继续播放
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
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun shareMoment() {
        // 分享功能（简化版）
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "LoveAI - 浪漫特效，送给最爱的你 ❤️")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "分享爱意"))
    }

    override fun onResume() {
        super.onResume()
        if (isPlaying) {
            MusicManager.play()
        }
        updateMusicName()
    }

    override fun onPause() {
        super.onPause()
        // 不停止音乐，让它在后台继续播放
    }

    override fun onDestroy() {
        super.onDestroy()
        heartRunnable?.let { handler.removeCallbacks(it) }
    }
}
