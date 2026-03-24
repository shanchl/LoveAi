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
import com.loveai.model.ExportRecord
import com.loveai.repository.ExportRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportCenterActivity : AppCompatActivity() {

    private lateinit var repository: ExportRepository
    private lateinit var rvExports: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnClear: Button
    private lateinit var adapter: ExportAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_center)

        repository = ExportRepository(this)
        rvExports = findViewById(R.id.rvExports)
        tvEmpty = findViewById(R.id.tvEmptyExports)
        btnClear = findViewById(R.id.btnClearExports)
        adapter = ExportAdapter()

        rvExports.layoutManager = LinearLayoutManager(this)
        rvExports.adapter = adapter

        findViewById<View>(R.id.btnBackExportCenter).setOnClickListener { finish() }
        btnClear.setOnClickListener {
            repository.clearAll()
            loadRecords()
        }
    }

    override fun onResume() {
        super.onResume()
        loadRecords()
    }

    private fun loadRecords() {
        val records = repository.getAllRecords()
        adapter.submitList(records)
        val hasData = records.isNotEmpty()
        rvExports.visibility = if (hasData) View.VISIBLE else View.GONE
        tvEmpty.visibility = if (hasData) View.GONE else View.VISIBLE
    }

    private class ExportAdapter : RecyclerView.Adapter<ExportAdapter.ViewHolder>() {
        private val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        private var items: List<ExportRecord> = emptyList()

        fun submitList(newItems: List<ExportRecord>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_export_record, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTitle.text = "${item.exportType.label} \u00b7 ${item.planName}"
            holder.tvMeta.text = "${formatter.format(Date(item.createdAt))} \u00b7 ${item.outputPath}"
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tvExportTitle)
            val tvMeta: TextView = view.findViewById(R.id.tvExportMeta)
        }
    }
}
