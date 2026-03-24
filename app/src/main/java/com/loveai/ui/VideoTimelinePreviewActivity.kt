package com.loveai.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.loveai.R
import java.io.File

class VideoTimelinePreviewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FILE_PATH = "file_path"
    }

    private lateinit var tvPlanName: TextView
    private lateinit var tvSummary: TextView
    private lateinit var tvTags: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var rvScenes: RecyclerView
    private lateinit var adapter: SceneAdapter
    private var filePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_timeline_preview)

        tvPlanName = findViewById(R.id.tvTimelinePlanName)
        tvSummary = findViewById(R.id.tvTimelineSummary)
        tvTags = findViewById(R.id.tvTimelineTags)
        tvEmpty = findViewById(R.id.tvTimelineEmpty)
        rvScenes = findViewById(R.id.rvTimelineScenes)
        adapter = SceneAdapter(
            onShorter = { scene -> updateSceneDuration(scene.order, scene.durationMs - 5000L) },
            onLonger = { scene -> updateSceneDuration(scene.order, scene.durationMs + 5000L) }
        )

        rvScenes.layoutManager = LinearLayoutManager(this)
        rvScenes.adapter = adapter
        findViewById<Button>(R.id.btnBackTimeline).setOnClickListener { finish() }

        loadPreview()
    }

    private fun loadPreview() {
        val path = intent.getStringExtra(EXTRA_FILE_PATH)
        filePath = path
        val preview = path?.let { VideoStoryboardExporter.parsePreview(File(it)) }
        if (preview == null) {
            tvPlanName.text = "\u65e0\u6cd5\u6253\u5f00\u9884\u89c8"
            tvSummary.text = "\u811a\u672c\u5305\u4e0d\u5b58\u5728\u6216\u5185\u5bb9\u65e0\u6548"
            tvTags.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
            rvScenes.visibility = View.GONE
            return
        }

        tvPlanName.text = preview.planName
        tvSummary.text = buildString {
            append(preview.title)
            if (preview.subtitle.isNotBlank()) {
                append("\n")
                append(preview.subtitle)
            }
            if (!preview.songName.isNullOrBlank()) {
                append("\n\u97f3\u4e50\uff1a")
                append(preview.songName)
            }
            append("\n\u573a\u666f\uff1a")
            append(preview.scenes.size)
            append(" \u6bb5")
        }
        tvTags.visibility = if (preview.tags.isEmpty()) View.GONE else View.VISIBLE
        tvTags.text = preview.tags.joinToString("  #", prefix = "#")
        adapter.submitList(preview.scenes)
        val hasScenes = preview.scenes.isNotEmpty()
        rvScenes.visibility = if (hasScenes) View.VISIBLE else View.GONE
        tvEmpty.visibility = if (hasScenes) View.GONE else View.VISIBLE
    }

    private fun updateSceneDuration(order: Int, durationMs: Long) {
        val path = filePath ?: return
        val file = File(path)
        if (VideoStoryboardExporter.updateSceneDuration(file, order, durationMs)) {
            loadPreview()
        }
    }

    private class SceneAdapter(
        private val onShorter: (VideoStoryboardExporter.StoryboardScene) -> Unit,
        private val onLonger: (VideoStoryboardExporter.StoryboardScene) -> Unit
    ) : RecyclerView.Adapter<SceneAdapter.ViewHolder>() {
        private var items: List<VideoStoryboardExporter.StoryboardScene> = emptyList()

        fun submitList(newItems: List<VideoStoryboardExporter.StoryboardScene>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_timeline_scene, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvOrder.text = "${item.order}"
            holder.tvTitle.text = item.title
            holder.tvMeta.text = buildString {
                append(item.effectType)
                append(" \u00b7 ")
                append(item.durationMs / 1000)
                append("s")
                item.assetName?.let {
                    append(" \u00b7 ")
                    append(it)
                }
                if (item.subtitle.isNotBlank()) {
                    append("\n")
                    append(item.subtitle)
                }
            }
            holder.btnShorter.isEnabled = item.durationMs > 5000L
            holder.btnShorter.setOnClickListener { onShorter(item) }
            holder.btnLonger.setOnClickListener { onLonger(item) }
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvOrder: TextView = view.findViewById(R.id.tvTimelineOrder)
            val tvTitle: TextView = view.findViewById(R.id.tvTimelineSceneTitle)
            val tvMeta: TextView = view.findViewById(R.id.tvTimelineSceneMeta)
            val btnShorter: Button = view.findViewById(R.id.btnTimelineShorter)
            val btnLonger: Button = view.findViewById(R.id.btnTimelineLonger)
        }
    }
}
