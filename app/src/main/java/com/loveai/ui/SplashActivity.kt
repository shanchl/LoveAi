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

    private lateinit var tvCountdown: TextView
    private lateinit var tvLoading: TextView
    private lateinit var ivHeart1: ImageView
    private lateinit var ivHeart2: ImageView
    private lateinit var ivHeart3: ImageView
    private lateinit var ivHeart4: ImageView

    private var countdown = 3
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 初始化背景音乐
        MusicManager.init(this)

        initViews()
        startAnimations()
        startCountdown()
    }

    private fun initViews() {
        tvCountdown = findViewById(R.id.tvCountdown)
        tvLoading = findViewById(R.id.tvLoading)
        ivHeart1 = findViewById(R.id.ivHeart1)
        ivHeart2 = findViewById(R.id.ivHeart2)
        ivHeart3 = findViewById(R.id.ivHeart3)
        ivHeart4 = findViewById(R.id.ivHeart4)
    }

    private fun startAnimations() {
        // 爱心浮动动画
        startFloatingAnimation(ivHeart1, 2000)
        startFloatingAnimation(ivHeart2, 2500)
        startFloatingAnimation(ivHeart3, 2200)
        startFloatingAnimation(ivHeart4, 2800)

        // Loading文字闪烁
        val loadingAnimator = ObjectAnimator.ofFloat(tvLoading, "alpha", 0.5f, 1f)
        loadingAnimator.duration = 1000
        loadingAnimator.repeatMode = ValueAnimator.REVERSE
        loadingAnimator.repeatCount = ValueAnimator.INFINITE
        loadingAnimator.start()
    }

    private fun startFloatingAnimation(view: View, duration: Long) {
        val floatUp = ObjectAnimator.ofFloat(view, "translationY", 0f, -20f)
        floatUp.duration = duration / 2
        floatUp.interpolator = AccelerateDecelerateInterpolator()

        val floatDown = ObjectAnimator.ofFloat(view, "translationY", -20f, 0f)
        floatDown.duration = duration / 2
        floatDown.interpolator = AccelerateDecelerateInterpolator()

        val animatorSet = AnimatorSet()
        animatorSet.playSequentially(floatUp, floatDown)
        animatorSet.start()

        // 循环播放
        handler.postDelayed({ startFloatingAnimation(view, duration) }, duration)
    }

    private fun startCountdown() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (countdown > 0) {
                    updateCountdown(countdown)
                    countdown--
                    handler.postDelayed(this, 1000)
                } else {
                    // 倒计时结束，跳转主界面
                    startMainActivity()
                }
            }
        }, 500)
    }

    private fun updateCountdown(num: Int) {
        tvCountdown.text = num.toString()
        
        // 数字跳动特效动画 - 包含弹跳、缩放、光晕
        tvCountdown.pivotX = tvCountdown.width / 2f
        tvCountdown.pivotY = tvCountdown.height / 2f
        
        // 1. 弹跳进入动画 (0-300ms)
        val scaleXAnim = ObjectAnimator.ofFloat(tvCountdown, "scaleX", 0.3f, 1.2f, 0.9f, 1.1f, 1f)
        val scaleYAnim = ObjectAnimator.ofFloat(tvCountdown, "scaleY", 0.3f, 1.2f, 0.9f, 1.1f, 1f)
        
        // 2. 透明度动画
        val alphaAnim = ObjectAnimator.ofFloat(tvCountdown, "alpha", 0f, 1f)
        
        // 3. 旋转动画（增加动感）
        val rotationAnim = ObjectAnimator.ofFloat(tvCountdown, "rotation", -15f, 10f, -5f, 0f)
        
        // 4. 阴影发光效果
        tvCountdown.setShadowLayer(20f, 0f, 0f, ContextCompat.getColor(this, R.color.pink_light))
        
        // 组合动画
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleXAnim, scaleYAnim, alphaAnim, rotationAnim)
        animatorSet.duration = 400
        animatorSet.interpolator = OvershootInterpolator(1.5f)
        animatorSet.start()
        
        // 显示后保持微小脉动效果
        val pulseAnim = ObjectAnimator.ofFloat(tvCountdown, "scaleX", 1f, 1.08f, 1f)
        val pulseAnimY = ObjectAnimator.ofFloat(tvCountdown, "scaleY", 1f, 1.08f, 1f)
        val pulseSet = AnimatorSet()
        pulseSet.playTogether(pulseAnim, pulseAnimY)
        pulseSet.duration = 500
        pulseSet.startDelay = 400
        pulseSet.start()
    }

    private fun startMainActivity() {
        // 淡出动画
        val fadeOut = ObjectAnimator.ofFloat(findViewById(android.R.id.content), "alpha", 1f, 0f)
        fadeOut.duration = 400
        fadeOut.start()

        handler.postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 400)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
