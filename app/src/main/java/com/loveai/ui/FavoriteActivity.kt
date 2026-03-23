package com.loveai.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.loveai.R
import com.loveai.manager.MusicManager
import com.loveai.model.FavoriteSequence
import com.loveai.repository.FavoriteRepository
import com.loveai.viewmodel.LoveViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FavoriteActivity : AppCompatActivity() {

    private lateinit var viewModel: LoveViewModel
    private lateinit var repository: FavoriteRepository
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: FavoriteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        viewModel = ViewModelProvider(this)[LoveViewModel::class.java]
        repository = FavoriteRepository(this)
        MusicManager.ensurePlaylist(this)

        initViews()
        observeData()
        viewModel.loadFavorites()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.rvFavorites)
        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = FavoriteAdapter(
            resolveSongName = { songKey ->
                MusicManager.getPlaylist().firstOrNull { it.key == songKey }?.name
            },
            onOpen = { favorite ->
                startActivity(
                    Intent(this, MainActivity::class.java).apply {
                        putExtra(MainActivity.EXTRA_FAVORITE_ID, favorite.id)
                    }
                )
            },
            onDelete = { favorite ->
                repository.deleteFavorite(favorite.id)
                viewModel.loadFavorites()
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun observeData() {
        viewModel.favoriteSequences.observe(this) { favorites ->
            val hasData = favorites.isNotEmpty()
            recyclerView.visibility = if (hasData) View.VISIBLE else View.GONE
            tvEmpty.visibility = if (hasData) View.GONE else View.VISIBLE
            adapter.submitList(favorites)
        }
    }

    private class FavoriteAdapter(
        private val resolveSongName: (String?) -> String?,
        private val onOpen: (FavoriteSequence) -> Unit,
        private val onDelete: (FavoriteSequence) -> Unit
    ) : RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder>() {

        private val favorites = mutableListOf<FavoriteSequence>()
        private val timeFormatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

        fun submitList(items: List<FavoriteSequence>) {
            favorites.clear()
            favorites.addAll(items)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_favorite_sequence, parent, false)
            return FavoriteViewHolder(view)
        }

        override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
            holder.bind(favorites[position])
        }

        override fun getItemCount(): Int = favorites.size

        inner class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvFavoriteName: TextView = itemView.findViewById(R.id.tvFavoriteName)
            private val tvFavoriteSummary: TextView = itemView.findViewById(R.id.tvFavoriteSummary)
            private val btnOpenFavorite: Button = itemView.findViewById(R.id.btnOpenFavorite)
            private val btnDeleteFavorite: Button = itemView.findViewById(R.id.btnDeleteFavorite)

            fun bind(favorite: FavoriteSequence) {
                tvFavoriteName.text = favorite.name

                val songName = resolveSongName(favorite.songKey) ?: "\u672a\u7ed1\u5b9a\u97f3\u4e50"
                val createdTime = timeFormatter.format(Date(favorite.createdAt))
                tvFavoriteSummary.text = buildString {
                    append(favorite.effectVariantIds.size)
                    append(" \u9875\u52a8\u6001")
                    append(" \u00b7 ")
                    append(songName)
                    append(" \u00b7 ")
                    append(createdTime)
                    if (favorite.title.isNotBlank()) {
                        append("\n")
                        append(favorite.title)
                        if (favorite.subtitle.isNotBlank()) {
                            append(" | ")
                            append(favorite.subtitle)
                        }
                    }
                }

                btnOpenFavorite.setOnClickListener { onOpen(favorite) }
                btnDeleteFavorite.setOnClickListener { onDelete(favorite) }
            }
        }
    }
}
