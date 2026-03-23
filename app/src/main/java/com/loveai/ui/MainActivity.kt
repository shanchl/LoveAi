package com.loveai.ui

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.loveai.R
import com.loveai.manager.MusicManager
import com.loveai.model.Effect
import com.loveai.model.EffectType
import com.loveai.repository.PlanRepository
import com.loveai.ui.effects.AuroraEffect
import com.loveai.ui.effects.BaseEffectView
import com.loveai.ui.effects.BubbleFloatEffect
import com.loveai.ui.effects.ButterflyEffect
import com.loveai.ui.effects.FireworkEffect
import com.loveai.ui.effects.HeartPulseEffect
import com.loveai.ui.effects.HeartRainEffect
import com.loveai.ui.effects.MeteorShowerEffect
import com.loveai.ui.effects.PetalFallEffect
import com.loveai.ui.effects.RippleEffect
import com.loveai.ui.effects.SnowFallEffect
import com.loveai.ui.effects.StarrySkyEffect
import com.loveai.ui.effects.TypewriterEffect
import com.loveai.viewmodel.LoveViewModel

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PLAN_ID = "plan_id"
        const val EXTRA_FAVORITE_ID = "favorite_id"
        private const val ENDING_PAGE_MIN_STAY_MS = 10_000L
    }

    private lateinit var viewModel: LoveViewModel
    private lateinit var planRepository: PlanRepository
    private lateinit var favoriteRepository: com.loveai.repository.FavoriteRepository
    private lateinit var viewPager: ViewPager2
    private lateinit var indicatorContainer: LinearLayout
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnFavorite: ImageButton
    private lateinit var btnFavorites: ImageButton
    private lateinit var btnMusic: ImageButton
    private lateinit var btnPlanEditor: Button
    private lateinit var btnPlanLibrary: Button
    private lateinit var replayContainer: LinearLayout
    private lateinit var btnReplay: Button
    private lateinit var adapter: EffectPagerAdapter
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
        planRepository = PlanRepository(this)
        favoriteRepository = com.loveai.repository.FavoriteRepository(this)

        initMusic()
        initViews()
        loadRequestedPlanIfPresent()
        observeData()
        startAutoSlide()
    }

    private fun initMusic() {
        if (!MusicManager.hasInitializedPlayback() || !MusicManager.isReady() || MusicManager.getPlaylist().isEmpty()) {
            MusicManager.initAutoPlaylist(this)
            MusicManager.setPlayMode(MusicManager.PlayMode.LOOP)
            MusicManager.play()
        }
    }

    private fun initViews() {
        viewPager = findViewById(R.id.viewPager)
        indicatorContainer = findViewById(R.id.indicatorContainer)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnFavorite = findViewById(R.id.btnFavorite)
        btnFavorites = findViewById(R.id.btnFavorites)
        btnMusic = findViewById(R.id.btnMusic)
        btnPlanEditor = findViewById(R.id.btnPlanEditor)
        btnPlanLibrary = findViewById(R.id.btnPlanLibrary)
        replayContainer = findViewById(R.id.replayContainer)
        btnReplay = findViewById(R.id.btnReplay)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        btnPlayPauseMusic = findViewById(R.id.btnPlayPauseMusic)
        btnPlaylist = findViewById(R.id.btnPlaylist)
        tvSongName = findViewById(R.id.tvSongName)

        adapter = EffectPagerAdapter()
        viewPager.adapter = adapter

        (viewPager.getChildAt(0) as? RecyclerView)?.itemAnimator = null
        viewPager.offscreenPageLimit = 1
        PageTransitionManager.applyRandomTransition(viewPager)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.setCurrentIndex(position)
                updateIndicators(position)
                checkIfFinished(position)

                if (viewModel.isPlaying.value == true && !hasFinished) {
                    stopAutoSlide()
                    autoSlideRunnable?.let { handler.postDelayed(it, viewModel.getAutoSlideInterval()) }
                }
            }
        })

        btnPlayPause.setOnClickListener {
            viewModel.togglePlayPause()
            if (viewModel.isPlaying.value == true) startAutoSlide() else stopAutoSlide()
        }

        btnFavorite.setOnClickListener {
            val isFavorite = viewModel.toggleCurrentFavorite()
            Toast.makeText(
                this,
                if (isFavorite) "\u5df2\u6536\u85cf\u5f53\u524d\u52a8\u6001" else "\u5df2\u53d6\u6d88\u6536\u85cf",
                Toast.LENGTH_SHORT
            ).show()
            animateFavoriteButton()
        }

        btnFavorites.setOnClickListener {
            startActivity(Intent(this, FavoriteActivity::class.java))
        }

        btnMusic.setOnClickListener {
            val isPlaying = MusicManager.toggle()
            btnMusic.alpha = if (isPlaying) 1f else 0.5f
            updateMusicUI()
            Toast.makeText(
                this,
                if (isPlaying) "\u97f3\u4e50\u5df2\u5f00\u542f" else "\u97f3\u4e50\u5df2\u5173\u95ed",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnPlanEditor.setOnClickListener {
            startActivity(Intent(this, PlanEditorActivity::class.java).apply {
                viewModel.getCurrentPlan()?.id?.let { putExtra(PlanEditorActivity.EXTRA_PLAN_ID, it) }
            })
        }

        btnPlanLibrary.setOnClickListener {
            startActivity(Intent(this, PlanLibraryActivity::class.java))
        }

        btnReplay.setOnClickListener { replay() }

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

        btnPlaylist.setOnClickListener { showPlaylistDialog() }

        btnMusic.alpha = if (MusicManager.isMusicPlaying()) 1f else 0.5f
        updateMusicUI()
        viewPager.alpha = 1f
    }

    private fun loadRequestedPlanIfPresent() {
        val favoriteId = intent.getStringExtra(EXTRA_FAVORITE_ID)
        if (!favoriteId.isNullOrBlank()) {
            favoriteRepository.getFavoriteById(favoriteId)?.let { favorite ->
                viewModel.loadFavoriteSequence(favorite)
                applyPlanMusic(favorite.songKey)
                return
            }
        }

        val planId = intent.getStringExtra(EXTRA_PLAN_ID) ?: return
        val plan = planRepository.getPlanById(planId) ?: return
        planRepository.markPlanOpened(plan.id)
        viewModel.loadPlan(plan)
        applyPlanMusic(plan.songKey)
    }

    private fun applyPlanMusic(songKey: String?) {
        val targetSongKey = songKey ?: return
        MusicManager.ensurePlaylist(this)
        if (MusicManager.playByKey(targetSongKey)) {
            btnMusic.alpha = 1f
            updateMusicUI()
        }
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
            Toast.makeText(this, "\u6682\u65e0\u64ad\u653e\u5217\u8868", Toast.LENGTH_SHORT).show()
            return
        }

        stopAutoSlide()
        viewPager.isDrawingCacheEnabled = false

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_playlist, null)
        val rvPlaylist = dialogView.findViewById<RecyclerView>(R.id.rvPlaylist)
        val tvPlayMode = dialogView.findViewById<TextView>(R.id.tvPlayMode)
        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)

        updatePlayModeText(tvPlayMode)

        rvPlaylist.layoutManager = LinearLayoutManager(this)
        val playlistAdapter = PlaylistAdapter(playlist, MusicManager.getCurrentSongIndex()) { position ->
            MusicManager.playByIndex(position)
            updateMusicUI()
            viewPager.requestLayout()
        }
        rvPlaylist.adapter = playlistAdapter

        val dialog = AlertDialog.Builder(this, R.style.Theme_App_Dialog)
            .setView(dialogView)
            .create()

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener {
            updateMusicUI()
            viewPager.isDrawingCacheEnabled = true
            if (viewModel.isPlaying.value == true) {
                startAutoSlide()
            }
        }

        dialog.show()
    }

    private fun updatePlayModeText(tvPlayMode: TextView) {
        tvPlayMode.text = when (MusicManager.getPlayMode()) {
            MusicManager.PlayMode.LOOP -> "\u5217\u8868\u5faa\u73af"
            MusicManager.PlayMode.SINGLE -> "\u5355\u66f2\u5faa\u73af"
            MusicManager.PlayMode.RANDOM -> "\u968f\u673a\u64ad\u653e"
        }
    }

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
            holder.tvSongName.alpha = if (position == currentIndex) 1f else 0.7f

            holder.itemView.setOnClickListener {
                currentIndex = position
                onItemClick(position)
                notifyDataSetChanged()
            }
        }

        override fun getItemCount(): Int = songs.size
    }

    private fun observeData() {
        viewModel.effects.observe(this) { effects ->
            adapter.submitList(effects)
            setupIndicators(effects.size)
            hasFinished = false
            replayContainer.visibility = View.GONE
            updateFavoriteButtonState()
        }

        viewModel.currentIndex.observe(this) { index ->
            if (viewPager.currentItem != index) {
                viewPager.currentItem = index
            }
            updateFavoriteButtonState()
            checkIfFinished(index)
        }

        viewModel.isPlaying.observe(this) { isPlaying ->
            btnPlayPause.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        }
    }

    private fun checkIfFinished(position: Int) {
        val effects = viewModel.effects.value ?: return
        if (position == effects.size - 1 && !hasFinished) {
            hasFinished = true
            stopAutoSlide()
            viewModel.stopAutoSlide()
            handler.postDelayed({ startEndingActivity() }, ENDING_PAGE_MIN_STAY_MS)
        }
    }

    private fun startEndingActivity() {
        val endingIntent = Intent(this, EndingActivity::class.java)
        viewModel.getCurrentPlan()?.id?.let { endingIntent.putExtra(EXTRA_PLAN_ID, it) }
        viewModel.getCurrentFavoriteSequence()?.id?.let {
            endingIntent.putExtra(EXTRA_FAVORITE_ID, it)
        }
        startActivity(endingIntent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun replay() {
        viewModel.replayCurrentSequence()
        hasFinished = false

        replayContainer.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(300)
            .withEndAction { replayContainer.visibility = View.GONE }
            .start()

        viewPager.setCurrentItem(0, false)
        PageTransitionManager.applyRandomTransition(viewPager)
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
        btnFavorite.setImageResource(R.drawable.ic_heart)
        btnFavorite.alpha = if (viewModel.isCurrentSequenceFavorite()) 1f else 0.6f
    }

    private fun animateFavoriteButton() {
        if (viewModel.isCurrentSequenceFavorite()) {
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
                viewPager.setCurrentItem(nextIndex, false)
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
        stopAutoSlide()
        autoSlideRunnable = object : Runnable {
            override fun run() {
                if (viewModel.isPlaying.value == true && !hasFinished) {
                    val currentIndex = viewPager.currentItem
                    val effects = viewModel.effects.value ?: return
                    if (currentIndex < effects.size - 1) {
                        switchToNextPageWithFade(currentIndex + 1)
                        handler.postDelayed(this, viewModel.getAutoSlideInterval())
                    } else {
                        checkIfFinished(currentIndex)
                    }
                }
            }
        }
        autoSlideRunnable?.let { handler.postDelayed(it, viewModel.getAutoSlideInterval()) }
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
    }

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
