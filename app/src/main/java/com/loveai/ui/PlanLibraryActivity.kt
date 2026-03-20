package com.loveai.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.loveai.R
import com.loveai.manager.MusicManager
import com.loveai.model.LovePlan
import com.loveai.model.PlanTheme
import com.loveai.repository.PlanRepository

class PlanLibraryActivity : AppCompatActivity() {

    private lateinit var rvPlans: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnCreate: Button
    private lateinit var repository: PlanRepository
    private lateinit var adapter: PlanAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan_library)

        repository = PlanRepository(this)
        MusicManager.ensurePlaylist(this)

        rvPlans = findViewById(R.id.rvPlans)
        tvEmpty = findViewById(R.id.tvEmptyPlans)
        btnCreate = findViewById(R.id.btnCreatePlan)

        adapter = PlanAdapter(
            resolveSongName = { songKey ->
                MusicManager.getPlaylist().firstOrNull { it.key == songKey }?.name
            },
            onOpen = { plan ->
                startActivity(Intent(this, MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_PLAN_ID, plan.id)
                })
            },
            onEdit = { plan ->
                startActivity(Intent(this, PlanEditorActivity::class.java).apply {
                    putExtra(PlanEditorActivity.EXTRA_PLAN_ID, plan.id)
                })
            },
            onDelete = { plan ->
                repository.deletePlan(plan.id)
                loadPlans()
                Toast.makeText(this, "\u65b9\u6848\u5df2\u5220\u9664", Toast.LENGTH_SHORT).show()
            }
        )

        rvPlans.layoutManager = LinearLayoutManager(this)
        rvPlans.adapter = adapter
        btnCreate.setOnClickListener {
            startActivity(Intent(this, PlanEditorActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadPlans()
    }

    private fun loadPlans() {
        val plans = repository.getAllPlans()
        adapter.submitList(plans)
        tvEmpty.visibility = if (plans.isEmpty()) View.VISIBLE else View.GONE
    }

    private class PlanAdapter(
        private val resolveSongName: (String?) -> String?,
        private val onOpen: (LovePlan) -> Unit,
        private val onEdit: (LovePlan) -> Unit,
        private val onDelete: (LovePlan) -> Unit
    ) : RecyclerView.Adapter<PlanAdapter.ViewHolder>() {

        private var items: List<LovePlan> = emptyList()

        fun submitList(newItems: List<LovePlan>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_plan, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val plan = items[position]
            holder.tvName.text = plan.name

            val themeLabel = PlanTheme.fromKey(plan.themeKey)?.label ?: "\u81ea\u5b9a\u4e49"
            val songLabel = resolveSongName(plan.songKey) ?: "\u672a\u7ed1\u5b9a\u66f2\u76ee"
            holder.tvSummary.text =
                "${plan.effectTypes.size} \u4e2a\u7279\u6548 \u00b7 ${themeLabel} \u00b7 ${songLabel}"

            holder.btnOpen.setOnClickListener { onOpen(plan) }
            holder.btnEdit.setOnClickListener { onEdit(plan) }
            holder.btnDelete.setOnClickListener { onDelete(plan) }
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvPlanName)
            val tvSummary: TextView = view.findViewById(R.id.tvPlanSummary)
            val btnOpen: Button = view.findViewById(R.id.btnOpenPlan)
            val btnEdit: Button = view.findViewById(R.id.btnEditPlan)
            val btnDelete: Button = view.findViewById(R.id.btnDeletePlan)
        }
    }
}
