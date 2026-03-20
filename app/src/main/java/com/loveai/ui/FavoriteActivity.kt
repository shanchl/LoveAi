package com.loveai.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.loveai.R
import com.loveai.model.Effect
import com.loveai.model.EffectType
import com.loveai.ui.effects.*
import com.loveai.viewmodel.LoveViewModel

class FavoriteActivity : AppCompatActivity() {

    private lateinit var viewModel: LoveViewModel
    private lateinit var gridView: GridView
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        viewModel = ViewModelProvider(this)[LoveViewModel::class.java]
        initViews()
        observeData()
        viewModel.loadFavorites()
    }

    private fun initViews() {
        gridView = findViewById(R.id.gridView)
        tvEmpty = findViewById(R.id.tvEmpty)
        
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun observeData() {
        viewModel.favoriteEffects.observe(this) { effects: List<Effect> ->
            if (effects.isEmpty()) {
                gridView.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
            } else {
                gridView.visibility = View.VISIBLE
                tvEmpty.visibility = View.GONE
                val adapter = FavoriteAdapter(effects)
                gridView.adapter = adapter
            }
        }
    }

    inner class FavoriteAdapter(private val effects: List<Effect>) :
        android.widget.BaseAdapter() {

        override fun getCount() = effects.size
        override fun getItem(position: Int) = effects[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup?): View {
            val effect = effects[position]
            val effectView: BaseEffectView = when (effect.type) {
                EffectType.HEART_RAIN -> HeartRainEffect(this@FavoriteActivity)
                EffectType.FIREWORK -> FireworkEffect(this@FavoriteActivity)
                EffectType.STARRY_SKY -> StarrySkyEffect(this@FavoriteActivity)
                EffectType.PETAL_FALL -> PetalFallEffect(this@FavoriteActivity)
                EffectType.BUBBLE_FLOAT -> BubbleFloatEffect(this@FavoriteActivity)
                EffectType.TYPEWRITER -> TypewriterEffect(this@FavoriteActivity)
                EffectType.HEART_PULSE -> HeartPulseEffect(this@FavoriteActivity)
                EffectType.RIPPLE -> RippleEffect(this@FavoriteActivity)
                EffectType.SNOW_FALL -> SnowFallEffect(this@FavoriteActivity)
                EffectType.METEOR_SHOWER -> MeteorShowerEffect(this@FavoriteActivity)
                EffectType.BUTTERFLY -> ButterflyEffect(this@FavoriteActivity)
                EffectType.AURORA -> AuroraEffect(this@FavoriteActivity)
            }
            effectView.bindEffect(effect)
            effectView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                400
            )
            return effectView
        }
    }
}
