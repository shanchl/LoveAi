package com.loveai.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.loveai.R
import com.loveai.manager.MusicManager

class SplashActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var tvLoading: TextView
    private lateinit var ivHeart1: ImageView
    private lateinit var ivHeart2: ImageView
    private lateinit var ivHeart3: ImageView
    private lateinit var ivHeart4: ImageView
    private lateinit var glowView: View

    private var countdown = 3
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        MusicManager.init(this)

        initViews()
        startAnimations()
        updateCountdown(countdown)
        startCountdown()
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tvTitle)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        tvCountdown = findViewById(R.id.tvCountdown)
        tvLoading = findViewById(R.id.tvLoading)
        ivHeart1 = findViewById(R.id.ivHeart1)
        ivHeart2 = findViewById(R.id.ivHeart2)
        ivHeart3 = findViewById(R.id.ivHeart3)
        ivHeart4 = findViewById(R.id.ivHeart4)
        glowView = findViewById(R.id.vGlow)
    }

    private fun startAnimations() {
        animateHeadline(tvTitle, 0L, 0f)
        animateHeadline(tvSubtitle, 180L, 20f)

        startFloatingAnimation(ivHeart1, 2100L, 1f)
        startFloatingAnimation(ivHeart2, 2500L, -1f)
        startFloatingAnimation(ivHeart3, 2300L, 1f)
        startFloatingAnimation(ivHeart4, 2800L, -1f)

        val loadingAnimator = ObjectAnimator.ofFloat(tvLoading, "alpha", 0.45f, 1f).apply {
            duration = 1100L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }

        val glowBreath = ObjectAnimator.ofFloat(glowView, "scaleX", 0.92f, 1.08f).apply {
            duration = 2200L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
        val glowBreathY = ObjectAnimator.ofFloat(glowView, "scaleY", 0.92f, 1.08f).apply {
            duration = 2200L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
        AnimatorSet().apply {
            playTogether(glowBreath, glowBreathY, loadingAnimator)
            start()
        }
    }

    private fun animateHeadline(view: View, startDelay: Long, translationY: Float) {
        view.alpha = 0f
        view.translationY = translationY
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(startDelay)
            .setDuration(700L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun startFloatingAnimation(view: View, duration: Long, rotationDirection: Float) {
        val floatY = ObjectAnimator.ofFloat(view, "translationY", 0f, -24f, 0f).apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
        }
        val floatX = ObjectAnimator.ofFloat(view, "translationX", 0f, 8f * rotationDirection, 0f).apply {
            this.duration = duration + 400L
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
        }
        val rotate = ObjectAnimator.ofFloat(view, "rotation", -8f * rotationDirection, 8f * rotationDirection).apply {
            this.duration = duration + 200L
            interpolator = AccelerateDecelerateInterpolator()
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
        val pulseX = ObjectAnimator.ofFloat(view, "scaleX", 0.88f, 1.18f, 0.92f).apply {
            this.duration = duration
            repeatCount = ValueAnimator.INFINITE
        }
        val pulseY = ObjectAnimator.ofFloat(view, "scaleY", 0.88f, 1.18f, 0.92f).apply {
            this.duration = duration
            repeatCount = ValueAnimator.INFINITE
        }

        AnimatorSet().apply {
            playTogether(floatY, floatX, rotate, pulseX, pulseY)
            start()
        }
    }

    private fun startCountdown() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                countdown--
                if (countdown > 0) {
                    updateCountdown(countdown)
                    handler.postDelayed(this, 1000L)
                } else {
                    startMainActivity()
                }
            }
        }, 1000L)
    }

    private fun updateCountdown(num: Int) {
        tvCountdown.text = num.toString()
        tvCountdown.pivotX = tvCountdown.width / 2f
        tvCountdown.pivotY = tvCountdown.height / 2f
        tvCountdown.setShadowLayer(20f, 0f, 0f, ContextCompat.getColor(this, R.color.pink_light))

        val scaleXAnim = ObjectAnimator.ofFloat(tvCountdown, "scaleX", 0.3f, 1.22f, 0.92f, 1.08f, 1f)
        val scaleYAnim = ObjectAnimator.ofFloat(tvCountdown, "scaleY", 0.3f, 1.22f, 0.92f, 1.08f, 1f)
        val alphaAnim = ObjectAnimator.ofFloat(tvCountdown, "alpha", 0f, 1f)
        val rotationAnim = ObjectAnimator.ofFloat(tvCountdown, "rotation", -12f, 7f, -3f, 0f)

        AnimatorSet().apply {
            playTogether(scaleXAnim, scaleYAnim, alphaAnim, rotationAnim)
            duration = 420L
            interpolator = OvershootInterpolator(1.45f)
            start()
        }

        val pulseX = ObjectAnimator.ofFloat(tvCountdown, "scaleX", 1f, 1.05f, 1f).apply {
            duration = 560L
            startDelay = 420L
        }
        val pulseY = ObjectAnimator.ofFloat(tvCountdown, "scaleY", 1f, 1.05f, 1f).apply {
            duration = 560L
            startDelay = 420L
        }
        AnimatorSet().apply {
            playTogether(pulseX, pulseY)
            start()
        }
    }

    private fun startMainActivity() {
        val root = findViewById<View>(R.id.splashRoot)
        val fadeOut = ObjectAnimator.ofFloat(root, "alpha", 1f, 0f).apply {
            duration = 520L
            start()
        }

        handler.postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, fadeOut.duration)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
