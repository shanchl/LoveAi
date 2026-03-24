package com.loveai.ui

import android.content.Intent
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
import com.loveai.model.LovePlan
import com.loveai.model.PlanStatus
import com.loveai.repository.PlanRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VersionChainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ROOT_PLAN_ID = "root_plan_id"
    }

    private lateinit var repository: PlanRepository
    private lateinit var tvTitle: TextView
    private lateinit var tvSummary: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var rvChain: RecyclerView
    private lateinit var adapter: ChainAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_version_chain)

        repository = PlanRepository(this)
        tvTitle = findViewById(R.id.tvVersionChainTitle)
        tvSummary = findViewById(R.id.tvVersionChainSummary)
        tvEmpty = findViewById(R.id.tvVersionChainEmpty)
        rvChain = findViewById(R.id.rvVersionChain)
        adapter = ChainAdapter(
            onOpen = { plan ->
                startActivity(Intent(this, MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_PLAN_ID, plan.id)
                })
            },
            onEdit = { plan ->
                startActivity(Intent(this, PlanEditorActivity::class.java).apply {
                    putExtra(PlanEditorActivity.EXTRA_PLAN_ID, plan.id)
                })
            }
        )
        rvChain.layoutManager = LinearLayoutManager(this)
        rvChain.adapter = adapter
        findViewById<Button>(R.id.btnBackVersionChain).setOnClickListener { finish() }

        loadChain()
    }

    private fun loadChain() {
        val rootPlanId = intent.getStringExtra(EXTRA_ROOT_PLAN_ID).orEmpty()
        val plans = repository.getRelatedPlans(rootPlanId)
            .sortedWith(compareBy<LovePlan> { it.currentVersion }.thenBy { it.createdAt })
        if (plans.isEmpty()) {
            tvTitle.text = "\u7248\u672c\u6811"
            tvSummary.text = "\u6682\u65e0\u53ef\u7528\u7248\u672c\u94fe\u6570\u636e"
            tvEmpty.visibility = View.VISIBLE
            rvChain.visibility = View.GONE
            return
        }

        val draftCount = plans.count { it.status == PlanStatus.DRAFT }
        val publishedCount = plans.count { it.status == PlanStatus.PUBLISHED }
        tvTitle.text = "\u7248\u672c\u6811 ${shortId(rootPlanId.ifBlank { plans.first().id })}"
        tvSummary.text = "\u5171 ${plans.size} \u4e2a\u7248\u672c \u00b7 \u8349\u7a3f $draftCount \u00b7 \u5df2\u53d1\u5e03 $publishedCount"
        tvEmpty.visibility = View.GONE
        rvChain.visibility = View.VISIBLE
        adapter.submitList(plans)
    }

    private class ChainAdapter(
        private val onOpen: (LovePlan) -> Unit,
        private val onEdit: (LovePlan) -> Unit
    ) : RecyclerView.Adapter<ChainAdapter.ViewHolder>() {

        private val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        private var items: List<LovePlan> = emptyList()

        fun submitList(newItems: List<LovePlan>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_version_chain, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvBadge.text = "V${item.currentVersion}"
            holder.tvName.text = item.name
            holder.tvMeta.text = buildString {
                append(item.status.label)
                append(" \u00b7 ")
                append(if (item.parentPlanId.isNullOrBlank()) "\u4e3b\u7248\u672c" else "\u6e90\u81ea ${shortId(item.parentPlanId)}")
                append(" \u00b7 \u66f4\u65b0 ")
                append(formatter.format(Date(item.updatedAt)))
                if (item.publishedAt > 0L) {
                    append("\n\u53d1\u5e03 ")
                    append(formatter.format(Date(item.publishedAt)))
                }
            }
            holder.tvRoute.text = "\u94fe\u8def\uff1a${shortId(item.rootPlanId ?: item.id)} -> ${shortId(item.id)}"
            holder.btnOpen.setOnClickListener { onOpen(item) }
            holder.btnEdit.setOnClickListener { onEdit(item) }
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvBadge: TextView = view.findViewById(R.id.tvVersionBadge)
            val tvName: TextView = view.findViewById(R.id.tvVersionName)
            val tvMeta: TextView = view.findViewById(R.id.tvVersionMeta)
            val tvRoute: TextView = view.findViewById(R.id.tvVersionRoute)
            val btnOpen: Button = view.findViewById(R.id.btnVersionOpen)
            val btnEdit: Button = view.findViewById(R.id.btnVersionEdit)
        }
    }
}

private fun shortId(id: String?): String = id?.take(6).orEmpty()
