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
import com.loveai.model.VideoExportTask
import com.loveai.repository.VideoExportRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoExportCenterActivity : AppCompatActivity() {

    private lateinit var repository: VideoExportRepository
    private lateinit var rvTasks: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_export_center)

        repository = VideoExportRepository(this)
        rvTasks = findViewById(R.id.rvVideoTasks)
        tvEmpty = findViewById(R.id.tvEmptyVideoTasks)
        adapter = TaskAdapter()

        rvTasks.layoutManager = LinearLayoutManager(this)
        rvTasks.adapter = adapter
        findViewById<View>(R.id.btnBackVideoExportCenter).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        val tasks = repository.getAllTasks()
        adapter.submitList(tasks)
        val hasData = tasks.isNotEmpty()
        rvTasks.visibility = if (hasData) View.VISIBLE else View.GONE
        tvEmpty.visibility = if (hasData) View.GONE else View.VISIBLE
    }

    private class TaskAdapter : RecyclerView.Adapter<TaskAdapter.ViewHolder>() {
        private val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        private var items: List<VideoExportTask> = emptyList()

        fun submitList(newItems: List<VideoExportTask>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_video_export_task, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTitle.text = "${item.planName} \u00b7 ${item.status.label}"
            holder.tvMeta.text = buildString {
                append(formatter.format(Date(item.createdAt)))
                if (item.finishedAt > 0L) {
                    append(" \u00b7 ")
                    append(formatter.format(Date(item.finishedAt)))
                }
                if (!item.outputPath.isNullOrBlank()) {
                    append("\n")
                    append(item.outputPath)
                }
                if (item.note.isNotBlank()) {
                    append("\n")
                    append(item.note)
                }
            }
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tvVideoTaskTitle)
            val tvMeta: TextView = view.findViewById(R.id.tvVideoTaskMeta)
        }
    }
}
