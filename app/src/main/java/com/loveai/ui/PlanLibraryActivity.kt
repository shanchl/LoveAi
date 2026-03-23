package com.loveai.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlanLibraryActivity : AppCompatActivity() {

    private lateinit var rvPlans: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvStats: TextView
    private lateinit var btnCreate: Button
    private lateinit var etSearchPlan: EditText
    private lateinit var spThemeFilter: Spinner
    private lateinit var spSortMode: Spinner
    private lateinit var repository: PlanRepository
    private lateinit var adapter: PlanAdapter

    private val themeFilterOptions = listOf(null) + PlanTheme.values().toList()
    private val sortOptions = listOf(
        PlanRepository.SortMode.RECENT,
        PlanRepository.SortMode.CREATED,
        PlanRepository.SortMode.NAME
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan_library)

        repository = PlanRepository(this)
        MusicManager.ensurePlaylist(this)

        initViews()
        initList()
        initFilters()
    }

    override fun onResume() {
        super.onResume()
        loadPlans()
    }

    private fun initViews() {
        rvPlans = findViewById(R.id.rvPlans)
        tvEmpty = findViewById(R.id.tvEmptyPlans)
        tvStats = findViewById(R.id.tvPlanStats)
        btnCreate = findViewById(R.id.btnCreatePlan)
        etSearchPlan = findViewById(R.id.etSearchPlan)
        spThemeFilter = findViewById(R.id.spThemeFilter)
        spSortMode = findViewById(R.id.spSortMode)

        btnCreate.setOnClickListener {
            startActivity(Intent(this, PlanEditorActivity::class.java))
        }

        etSearchPlan.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                loadPlans()
            }
        })
    }

    private fun initFilters() {
        val themeLabels = themeFilterOptions.map { theme ->
            theme?.label ?: "\u5168\u90e8\u4e3b\u9898"
        }
        spThemeFilter.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            themeLabels
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val sortLabels = listOf("\u6700\u8fd1\u4f7f\u7528", "\u6700\u65b0\u521b\u5efa", "\u6309\u540d\u79f0")
        spSortMode.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            sortLabels
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spThemeFilter.setSelection(0)
        spSortMode.setSelection(0)
        spThemeFilter.onItemSelectedListener = SimpleItemSelectedListener { loadPlans() }
        spSortMode.onItemSelectedListener = SimpleItemSelectedListener { loadPlans() }
    }

    private fun initList() {
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
            onDuplicate = { plan ->
                val copy = repository.duplicatePlan(plan.id)
                loadPlans()
                Toast.makeText(
                    this,
                    if (copy != null) "\u5df2\u590d\u5236\u65b9\u6848" else "\u590d\u5236\u5931\u8d25",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDelete = { plan ->
                repository.deletePlan(plan.id)
                loadPlans()
                Toast.makeText(this, "\u65b9\u6848\u5df2\u5220\u9664", Toast.LENGTH_SHORT).show()
            }
        )

        rvPlans.layoutManager = LinearLayoutManager(this)
        rvPlans.adapter = adapter
    }

    private fun loadPlans() {
        val themeKey = themeFilterOptions.getOrNull(spThemeFilter.selectedItemPosition)?.key
        val sortMode = sortOptions.getOrElse(spSortMode.selectedItemPosition) {
            PlanRepository.SortMode.RECENT
        }
        val plans = repository.queryPlans(
            keyword = etSearchPlan.text.toString(),
            themeKey = themeKey,
            sortMode = sortMode
        )
        adapter.submitList(plans)
        tvEmpty.visibility = if (plans.isEmpty()) View.VISIBLE else View.GONE
        tvStats.text = "\u5171 ${plans.size} \u4e2a\u65b9\u6848"
    }

    private class PlanAdapter(
        private val resolveSongName: (String?) -> String?,
        private val onOpen: (LovePlan) -> Unit,
        private val onEdit: (LovePlan) -> Unit,
        private val onDuplicate: (LovePlan) -> Unit,
        private val onDelete: (LovePlan) -> Unit
    ) : RecyclerView.Adapter<PlanAdapter.ViewHolder>() {

        private var items: List<LovePlan> = emptyList()
        private val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

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
            val theme = PlanTheme.fromKey(plan.themeKey)
            val themeLabel = theme?.label ?: "\u81ea\u5b9a\u4e49"
            val songLabel = resolveSongName(plan.songKey) ?: "\u672a\u7ed1\u5b9a\u66f2\u76ee"

            holder.tvPlanTheme.text = themeLabel
            holder.tvPlanCoverTitle.text = plan.title
            holder.tvName.text = plan.name
            holder.tvSummary.text =
                "${plan.effectTypes.size} \u4e2a\u7279\u6548 \u00b7 ${themeLabel} \u00b7 ${songLabel}"
            holder.tvMeta.text = buildString {
                append("\u521b\u5efa\u4e8e ")
                append(formatter.format(Date(plan.createdAt)))
                append(" \u00b7 \u64ad\u653e ")
                append(plan.playCount)
                append(" \u6b21")
                if (plan.lastOpenedAt > 0L) {
                    append(" \u00b7 \u6700\u8fd1\u4f7f\u7528 ")
                    append(formatter.format(Date(plan.lastOpenedAt)))
                }
            }

            holder.layoutPlanCover.background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                coverColors(theme)
            ).apply {
                cornerRadius = 24f
            }

            holder.btnOpen.setOnClickListener { onOpen(plan) }
            holder.btnEdit.setOnClickListener { onEdit(plan) }
            holder.btnDuplicate.setOnClickListener { onDuplicate(plan) }
            holder.btnDelete.setOnClickListener { onDelete(plan) }
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val layoutPlanCover: LinearLayout = view.findViewById(R.id.layoutPlanCover)
            val tvPlanTheme: TextView = view.findViewById(R.id.tvPlanTheme)
            val tvPlanCoverTitle: TextView = view.findViewById(R.id.tvPlanCoverTitle)
            val tvName: TextView = view.findViewById(R.id.tvPlanName)
            val tvSummary: TextView = view.findViewById(R.id.tvPlanSummary)
            val tvMeta: TextView = view.findViewById(R.id.tvPlanMeta)
            val btnOpen: Button = view.findViewById(R.id.btnOpenPlan)
            val btnEdit: Button = view.findViewById(R.id.btnEditPlan)
            val btnDuplicate: Button = view.findViewById(R.id.btnDuplicatePlan)
            val btnDelete: Button = view.findViewById(R.id.btnDeletePlan)
        }

        companion object {
            private fun coverColors(theme: PlanTheme?): IntArray {
                return when (theme) {
                    PlanTheme.CONFESSION -> intArrayOf(
                        Color.parseColor("#FF6F91"),
                        Color.parseColor("#FF9671")
                    )
                    PlanTheme.ANNIVERSARY -> intArrayOf(
                        Color.parseColor("#6A67CE"),
                        Color.parseColor("#C06C84")
                    )
                    PlanTheme.BIRTHDAY -> intArrayOf(
                        Color.parseColor("#F9C74F"),
                        Color.parseColor("#F3722C")
                    )
                    PlanTheme.LONG_DISTANCE -> intArrayOf(
                        Color.parseColor("#277DA1"),
                        Color.parseColor("#577590")
                    )
                    null -> intArrayOf(
                        Color.parseColor("#5C4B8A"),
                        Color.parseColor("#A55C9A")
                    )
                }
            }
        }
    }
}

private class SimpleItemSelectedListener(
    private val onSelected: () -> Unit
) : AdapterView.OnItemSelectedListener {
    override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
    ) {
        onSelected()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) = Unit
}
