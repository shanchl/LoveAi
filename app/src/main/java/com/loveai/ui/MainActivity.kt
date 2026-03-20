package com.loveai.ui

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.loveai.R
import com.loveai.manager.MusicManager
import com.loveai.model.Effect
import com.loveai.model.EffectType
import com.loveai.ui.effects.*
import com.loveai.viewmodel.LoveViewModel
import com.loveai.ui.EndingActivity

class MainActivity : AppCompatActivity() {

    companion object {
        private const val ENDING_PAGE_MIN_STAY_MS = 10_000L
    }

    private lateinit var viewModel: LoveViewModel
    private lateinit var viewPager: ViewPager2
    private lateinit var indicatorContainer: LinearLayout
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnFavorite: ImageButton
    private lateinit var btnFavorites: ImageButton
    private lateinit var btnMusic: ImageButton
    private lateinit var replayContainer: LinearLayout
    private lateinit var btnReplay: Button
    private lateinit var adapter: EffectPagerAdapter
    
    // 音乐控制相关
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnPlayPauseMusic: ImageButton
    private lateinit var btnPlaylist: ImageButton
    private lateinit var tvSongName: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var autoSlideRunnable: Runnable? = null
    private var hasFinished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[LoveViewModel::class.java]
        initMusic()
        initViews()
        observeData()
        startAutoSlide()
    }

    private fun initMusic() {
        if (!MusicManager.hasInitializedPlayback() || !MusicManager.isReady() || MusicManager.getPlaylist().isEmpty()) {
            MusicManager.initAutoPlaylist(this)
            MusicManager.setPlayMode(MusicManager.PlayMode.LOOP)
            MusicManager.play()
            return
        }
    }

    private fun initViews() {
        viewPager = findViewById<ViewPager2>(R.id.viewPager)
        indicatorContainer = findViewById<LinearLayout>(R.id.indicatorContainer)
        btnPlayPause = findViewById<ImageButton>(R.id.btnPlayPause)
        btnFavorite = findViewById<ImageButton>(R.id.btnFavorite)
        btnFavorites = findViewById<ImageButton>(R.id.btnFavorites)
        btnMusic = findViewById<ImageButton>(R.id.btnMusic)
        replayContainer = findViewById<LinearLayout>(R.id.replayContainer)
        btnReplay = findViewById<Button>(R.id.btnReplay)
        
        // 音乐控制按钮
        btnPrev = findViewById<ImageButton>(R.id.btnPrev)
        btnNext = findViewById<ImageButton>(R.id.btnNext)
        btnPlayPauseMusic = findViewById<ImageButton>(R.id.btnPlayPauseMusic)
        btnPlaylist = findViewById<ImageButton>(R.id.btnPlaylist)
        tvSongName = findViewById<TextView>(R.id.tvSongName)

        adapter = EffectPagerAdapter()
        viewPager.adapter = adapter
        
        // 禁用 RecyclerView 的 item 动画，避免影响页面切换效果
        (viewPager.getChildAt(0) as? RecyclerView)?.itemAnimator = null
        
        // 设置 ViewPager 缓存页面数，避免动画问题
        viewPager.offscreenPageLimit = 1
        
        // 应用随机页面切换动画
        PageTransitionManager.applyRandomTransition(viewPager)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.setCurrentIndex(position)
                updateIndicators(position)
                checkIfFinished(position)
                
                // 用户手动滑动后，重置自动轮播计时
                if (viewModel.isPlaying.value == true && !hasFinished) {
                    stopAutoSlide()
                    handler.postDelayed(autoSlideRunnable!!, viewModel.getAutoSlideInterval())
                }
            }
            
            override fun onPageScrollStateChanged(state: Int) {
                // 滑动开始时暂停自动轮播，避免冲突
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    // 可以选择暂停，松开后会在 onPageSelected 中恢复
                }
            }
        })

        btnPlayPause.setOnClickListener {
            viewModel.togglePlayPause()
            if (viewModel.isPlaying.value == true) {
                startAutoSlide()
            } else {
                stopAutoSlide()
            }
        }

        btnFavorite.setOnClickListener {
            viewModel.toggleCurrentFavorite()
            animateFavoriteButton()
        }

        btnFavorites.setOnClickListener {
            val intent = Intent(this, FavoriteActivity::class.java)
            startActivity(intent)
        }

        btnMusic.setOnClickListener {
            val isPlaying = MusicManager.toggle()
            btnMusic.alpha = if (isPlaying) 1f else 0.5f
            updateMusicUI()
            Toast.makeText(this, if (isPlaying) "音乐已开启" else "音乐已关闭", Toast.LENGTH_SHORT).show()
        }

        btnReplay.setOnClickListener {
            replay()
        }
        
        // 音乐控制按钮事件
        btnPrev.setOnClickListener {
            MusicManager.previous()
            updateMusicUI()
        }
        
        btnNext.setOnClickListener {
            MusicManager.next()
            updateMusicUI()
        }
        
        btnPlayPauseMusic.setOnClickListener {
            MusicManager.toggle()
            btnMusic.alpha = if (MusicManager.isMusicPlaying()) 1f else 0.5f
            updateMusicUI()
        }
        
        btnPlaylist.setOnClickListener {
            showPlaylistDialog()
        }

        // 初始化音乐按钮状态
        btnMusic.alpha = if (MusicManager.isMusicPlaying()) 1f else 0.5f
        updateMusicUI()
        viewPager.alpha = 1f
    }
    
    private fun updateMusicUI() {
        tvSongName.text = MusicManager.getCurrentSongName()
        btnPlayPauseMusic.setImageResource(
            if (MusicManager.isMusicPlaying()) R.drawable.ic_pause else R.drawable.ic_play
        )
    }
    
    private fun showPlaylistDialog() {
        val playlist = MusicManager.getPlaylist()
        if (playlist.isEmpty()) {
            Toast.makeText(this, "暂无播放列表", Toast.LENGTH_SHORT).show()
            return
        }

        // 暂停自动轮播
        stopAutoSlide()
        
        // 禁用 ViewPager 绘制缓存，避免动画问题
        viewPager.isDrawingCacheEnabled = false

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_playlist, null)
        val rvPlaylist = dialogView.findViewById<RecyclerView>(R.id.rvPlaylist)
        val tvPlayMode = dialogView.findViewById<TextView>(R.id.tvPlayMode)
        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)

        // 显示当前播放模式
        updatePlayModeText(tvPlayMode)

        // 设置RecyclerView
        rvPlaylist.layoutManager = LinearLayoutManager(this)
        val adapter = PlaylistAdapter(playlist, MusicManager.getCurrentSongIndex()) { position ->
            MusicManager.playByIndex(position)
            updateMusicUI()
            // 切换歌曲后刷新 ViewPager 视图状态
            viewPager.requestLayout()
        }
        rvPlaylist.adapter = adapter

        // 创建Dialog
        val dialog = AlertDialog.Builder(this, R.style.Theme_App_Dialog)
            .setView(dialogView)
            .create()

        // 关闭按钮
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        // 点击外部关闭
        dialog.setOnDismissListener {
            updateMusicUI()
            // 恢复 ViewPager 状态
            viewPager.isDrawingCacheEnabled = true
            // 恢复自动轮播
            if (viewModel.isPlaying.value == true) {
                startAutoSlide()
            }
        }

        dialog.show()
    }

    private fun updatePlayModeText(tvPlayMode: TextView) {
        val mode = MusicManager.getPlayMode()
        tvPlayMode.text = when (mode) {
            MusicManager.PlayMode.LOOP -> "列表循环"
            MusicManager.PlayMode.SINGLE -> "单曲循环"
            MusicManager.PlayMode.RANDOM -> "随机播放"
        }
    }

    // 播放列表适配器
    inner class PlaylistAdapter(
        private val songs: List<MusicManager.Song>,
        private var currentIndex: Int,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<PlaylistAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvSongName: TextView = view.findViewById(R.id.tvSongName)
            val ivPlaying: ImageView = view.findViewById(R.id.ivPlaying)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_playlist, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val song = songs[position]
            holder.tvSongName.text = song.name
            holder.ivPlaying.visibility = if (position == currentIndex) View.VISIBLE else View.GONE
            
            // 当前播放的歌曲高亮
            holder.tvSongName.alpha = if (position == currentIndex) 1f else 0.7f
            
            holder.itemView.setOnClickListener {
                currentIndex = position
                onItemClick(position)
                notifyDataSetChanged()
            }
        }

        override fun getItemCount() = songs.size
    }

    private fun observeData() {
        viewModel.effects.observe(this) { effects ->
            adapter.submitList(effects)
            setupIndicators(effects.size)
            hasFinished = false
            replayContainer.visibility = View.GONE
        }

        viewModel.currentIndex.observe(this) { index ->
            if (viewPager.currentItem != index) {
                viewPager.currentItem = index
            }
            updateFavoriteButtonState()
            checkIfFinished(index)
        }

        viewModel.isPlaying.observe(this) { isPlaying ->
            btnPlayPause.setImageResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
        }
    }

    private fun checkIfFinished(position: Int) {
        val effects = viewModel.effects.value ?: return
        // 检查是否到达最后一个效果
        if (position == effects.size - 1 && !hasFinished) {
            hasFinished = true
            stopAutoSlide()
            viewModel.stopAutoSlide()
            
            // 延迟2秒后跳转到结尾页面，让用户看完最后一个特效
            handler.postDelayed({
                startEndingActivity()
            }, ENDING_PAGE_MIN_STAY_MS)
        }
    }

    private fun startEndingActivity() {
        val intent = Intent(this, EndingActivity::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun replay() {
        // 重新生成随机效果
        viewModel.generateRandomEffects()
        hasFinished = false
        
        // 隐藏重播按钮
        replayContainer.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(300)
            .withEndAction {
                replayContainer.visibility = View.GONE
            }
            .start()
        
        // 重置 ViewPager 状态
        viewPager.setCurrentItem(0, false)  // false 表示不带动画
        
        // 重新应用切换动画（避免动画状态问题）
        PageTransitionManager.applyRandomTransition(viewPager)
        
        // 重新开始自动播放
        viewModel.startAutoSlide()
        startAutoSlide()
    }

    private fun setupIndicators(count: Int) {
        indicatorContainer.removeAllViews()
        for (i in 0 until count) {
            val indicator = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(12, 12).apply {
                    marginStart = 4
                    marginEnd = 4
                }
                setImageResource(R.drawable.indicator_dot)
                alpha = if (i == 0) 1f else 0.4f
            }
            indicatorContainer.addView(indicator)
        }
    }

    private fun updateIndicators(position: Int) {
        for (i in 0 until indicatorContainer.childCount) {
            val indicator = indicatorContainer.getChildAt(i) as ImageView
            indicator.alpha = if (i == position) 1f else 0.4f
        }
    }

    private fun updateFavoriteButtonState() {
        val effect = viewModel.getCurrentEffect()
        btnFavorite.setImageResource(R.drawable.ic_heart)
        btnFavorite.alpha = if (effect?.isFavorite == true) 1f else 0.6f
    }

    private fun animateFavoriteButton() {
        val effect = viewModel.getCurrentEffect()
        if (effect?.isFavorite == true) {
            // 收藏成功动画
            val scaleX = ObjectAnimator.ofFloat(btnFavorite, "scaleX", 1f, 1.3f, 1f)
            val scaleY = ObjectAnimator.ofFloat(btnFavorite, "scaleY", 1f, 1.3f, 1f)
            scaleX.duration = 300
            scaleY.duration = 300
            scaleX.start()
            scaleY.start()
        }
        updateFavoriteButtonState()
    }

    private fun switchToNextPageWithFade(nextIndex: Int) {
        // 先取消可能残留的动画，避免连续触发时叠加
        viewPager.animate().cancel()
        viewPager.pivotX = viewPager.width / 2f
        viewPager.pivotY = viewPager.height / 2f

        viewPager.animate()
            .alpha(0.15f)
            .scaleX(0.965f)
            .scaleY(0.965f)
            .translationY(20f)
            .setDuration(820)
            .withEndAction {
                if (isFinishing || isDestroyed) return@withEndAction

                // 直接切页，不使用 ViewPager 默认平滑滚动
                viewPager.setCurrentItem(nextIndex, false)

                // 切过去后再淡入
                viewPager.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(0f)
                    .setDuration(860)
                    .start()
            }
            .start()
    }

    private fun startAutoSlide() {
        // 先停止已有的轮播，避免重复
        stopAutoSlide()
        
        autoSlideRunnable = object : Runnable {
            override fun run() {
                if (viewModel.isPlaying.value == true && !hasFinished) {
                    val currentIndex = viewPager.currentItem
                    val effects = viewModel.effects.value ?: return
                    
                    // 如果还有下一个效果，继续切换
                    if (currentIndex < effects.size - 1) {
                        switchToNextPageWithFade(currentIndex + 1)
                        // 每次切换后都重新设置延时，确保固定的10秒间隔
                        handler.postDelayed(this, viewModel.getAutoSlideInterval())
                    } else {
                        // 到达最后一个效果，停止自动切换
                        checkIfFinished(currentIndex)
                    }
                }
            }
        }
        // 首次启动也延时，确保稳定
        handler.postDelayed(autoSlideRunnable!!, viewModel.getAutoSlideInterval())
    }

    private fun stopAutoSlide() {
        autoSlideRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isPlaying.value == true && !hasFinished) {
            startAutoSlide()
        }
        btnMusic.alpha = if (MusicManager.isMusicPlaying()) 1f else 0.5f
        updateMusicUI()
    }

    override fun onPause() {
        super.onPause()
        stopAutoSlide()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAutoSlide()
        handler.removeCallbacksAndMessages(null)
        // 注意：这里不停止音乐，让它在后台继续播放
        // MusicManager.release() // 注释掉，音乐保留给EndingActivity
    }

    // ViewPager2 适配器
    inner class EffectPagerAdapter : RecyclerView.Adapter<EffectViewHolder>() {
        private var effects: List<Effect> = emptyList()

        fun submitList(newEffects: List<Effect>) {
            effects = newEffects
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EffectViewHolder {
            val effectView = when (EffectType.values()[viewType]) {
                EffectType.HEART_RAIN -> HeartRainEffect(parent.context)
                EffectType.FIREWORK -> FireworkEffect(parent.context)
                EffectType.STARRY_SKY -> StarrySkyEffect(parent.context)
                EffectType.PETAL_FALL -> PetalFallEffect(parent.context)
                EffectType.BUBBLE_FLOAT -> BubbleFloatEffect(parent.context)
                EffectType.TYPEWRITER -> TypewriterEffect(parent.context)
                EffectType.HEART_PULSE -> HeartPulseEffect(parent.context)
                EffectType.RIPPLE -> RippleEffect(parent.context)
                EffectType.SNOW_FALL -> SnowFallEffect(parent.context)
                EffectType.METEOR_SHOWER -> MeteorShowerEffect(parent.context)
                EffectType.BUTTERFLY -> ButterflyEffect(parent.context)
                EffectType.AURORA -> AuroraEffect(parent.context)
            }
            effectView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            return EffectViewHolder(effectView)
        }

        override fun onBindViewHolder(holder: EffectViewHolder, position: Int) {
            holder.bind(effects[position])
        }

        override fun getItemCount() = effects.size

        override fun getItemViewType(position: Int) = effects[position].type.ordinal
    }

    inner class EffectViewHolder(private val effectView: BaseEffectView) :
        RecyclerView.ViewHolder(effectView) {
        fun bind(effect: Effect) {
            effectView.bindEffect(effect)
        }
    }
}
