package com.loveai.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
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
import com.loveai.model.ExportType
import com.loveai.model.LovePlan
import com.loveai.model.PlanCover
import com.loveai.model.PlanStatus
import com.loveai.model.PlanTheme
import com.loveai.model.VideoAspectPreset
import com.loveai.repository.ExportRepository
import com.loveai.repository.PlanRepository
import com.loveai.repository.VideoExportRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlanLibraryActivity : AppCompatActivity() {

    private lateinit var rvPlans: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvStats: TextView
    private lateinit var btnCreate: Button
    private lateinit var btnOpenExportCenter: Button
    private lateinit var btnOpenSmartGenerate: Button
    private lateinit var btnOpenVideoCenter: Button
    private lateinit var etSearchPlan: EditText
    private lateinit var spThemeFilter: Spinner
    private lateinit var spStatusFilter: Spinner
    private lateinit var spSortMode: Spinner
    private lateinit var repository: PlanRepository
    private lateinit var exportRepository: ExportRepository
    private lateinit var videoExportRepository: VideoExportRepository
    private lateinit var adapter: PlanAdapter

    private val themeFilterOptions = listOf(null) + PlanTheme.values().toList()
    private val statusFilterOptions = listOf<PlanStatus?>(null, PlanStatus.DRAFT, PlanStatus.PUBLISHED)
    private val sortOptions = listOf(
        PlanRepository.SortMode.RECENT,
        PlanRepository.SortMode.CREATED,
        PlanRepository.SortMode.NAME
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan_library)

        repository = PlanRepository(this)
        exportRepository = ExportRepository(this)
        videoExportRepository = VideoExportRepository(this)
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
        btnOpenExportCenter = findViewById(R.id.btnOpenExportCenter)
        btnOpenSmartGenerate = findViewById(R.id.btnOpenSmartGenerate)
        btnOpenVideoCenter = findViewById(R.id.btnOpenVideoCenter)
        etSearchPlan = findViewById(R.id.etSearchPlan)
        spThemeFilter = findViewById(R.id.spThemeFilter)
        spStatusFilter = findViewById(R.id.spStatusFilter)
        spSortMode = findViewById(R.id.spSortMode)

        btnCreate.setOnClickListener {
            startActivity(Intent(this, PlanEditorActivity::class.java))
        }
        btnOpenExportCenter.setOnClickListener {
            startActivity(Intent(this, ExportCenterActivity::class.java))
        }
        btnOpenSmartGenerate.setOnClickListener {
            startActivity(Intent(this, SmartGenerateActivity::class.java))
        }
        btnOpenVideoCenter.setOnClickListener {
            startActivity(Intent(this, VideoExportCenterActivity::class.java))
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

        val statusLabels = listOf(
            "\u5168\u90e8\u72b6\u6001",
            PlanStatus.DRAFT.label,
            PlanStatus.PUBLISHED.label
        )
        spStatusFilter.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            statusLabels
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spThemeFilter.setSelection(0)
        spStatusFilter.setSelection(0)
        spSortMode.setSelection(0)
        spThemeFilter.onItemSelectedListener = SimpleItemSelectedListener { loadPlans() }
        spStatusFilter.onItemSelectedListener = SimpleItemSelectedListener { loadPlans() }
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
            onNewVersion = { plan ->
                val copy = repository.createNextVersionPlan(plan.id)
                loadPlans()
                Toast.makeText(
                    this,
                    if (copy != null) "\u5df2\u521b\u5efa\u65b0\u7248\u672c" else "\u521b\u5efa\u65b0\u7248\u672c\u5931\u8d25",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDelete = { plan ->
                repository.deletePlan(plan.id)
                loadPlans()
                Toast.makeText(this, "\u65b9\u6848\u5df2\u5220\u9664", Toast.LENGTH_SHORT).show()
            },
            onExport = { plan ->
                exportPlanArtwork(plan)
            },
            onShare = { plan ->
                sharePlanArtwork(plan)
            },
            onPreviewCover = { plan ->
                previewCover(plan)
            },
            onVideoTask = { plan ->
                createVideoTask(plan)
            }
        )

        rvPlans.layoutManager = LinearLayoutManager(this)
        rvPlans.adapter = adapter
    }

    private fun loadPlans() {
        val themeKey = themeFilterOptions.getOrNull(spThemeFilter.selectedItemPosition)?.key
        val status = statusFilterOptions.getOrNull(spStatusFilter.selectedItemPosition)
        val sortMode = sortOptions.getOrElse(spSortMode.selectedItemPosition) {
            PlanRepository.SortMode.RECENT
        }
        val plans = repository.queryPlans(
            keyword = etSearchPlan.text.toString(),
            themeKey = themeKey,
            status = status,
            sortMode = sortMode
        )
        adapter.submitList(plans)
        tvEmpty.visibility = if (plans.isEmpty()) View.VISIBLE else View.GONE
        val draftCount = plans.count { it.status == PlanStatus.DRAFT }
        val publishedCount = plans.count { it.status == PlanStatus.PUBLISHED }
        tvStats.text = "\u5171 ${plans.size} \u4e2a\u65b9\u6848 \u00b7 \u8349\u7a3f $draftCount \u00b7 \u5df2\u53d1\u5e03 $publishedCount"
    }

    private fun exportPlanArtwork(plan: LovePlan) {
        runCatching {
            val songName = MusicManager.getPlaylist().firstOrNull { it.key == plan.songKey }?.name
            val exported = PlanArtworkExporter.exportPoster(this, plan, songName)
            exportRepository.addRecord(
                planId = plan.id,
                planName = plan.name,
                exportType = ExportType.POSTER,
                outputPath = exported.file.absolutePath
            )
            Toast.makeText(
                this,
                "\u5df2\u5bfc\u51fa\u5230\uff1a${exported.file.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
        }.onFailure {
            Toast.makeText(this, "\u5bfc\u51fa\u5931\u8d25", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sharePlanArtwork(plan: LovePlan) {
        runCatching {
            val songName = MusicManager.getPlaylist().firstOrNull { it.key == plan.songKey }?.name
            val exported = PlanArtworkExporter.exportPoster(this, plan, songName)
            exportRepository.addRecord(
                planId = plan.id,
                planName = plan.name,
                exportType = ExportType.SHARE_COVER,
                outputPath = exported.file.absolutePath
            )
            val uri = PlanArtworkExporter.buildShareUri(this, exported.file)
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, plan.name)
                        putExtra(Intent.EXTRA_TEXT, plan.title)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "\u5206\u4eab\u65b9\u6848\u5c01\u9762"
                )
            )
        }.onFailure {
            Toast.makeText(this, "\u5206\u4eab\u5931\u8d25", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createVideoTask(plan: LovePlan) {
        val presets = VideoAspectPreset.values()
        AlertDialog.Builder(this, R.style.Theme_App_Dialog)
            .setTitle("\u9009\u62e9\u89c6\u9891\u6bd4\u4f8b")
            .setItems(presets.map { it.label }.toTypedArray()) { _, which ->
                createVideoTaskWithPreset(plan, presets[which])
            }
            .show()
    }

    private fun createVideoTaskWithPreset(plan: LovePlan, aspectPreset: VideoAspectPreset) {
        val queued = videoExportRepository.enqueue(plan.id, plan.name, aspectPreset)
        val running = queued.copy(
            status = com.loveai.model.VideoExportStatus.RUNNING,
            note = "\u6b63\u5728\u751f\u6210 ${aspectPreset.label} \u89c6\u9891\u811a\u672c\u5305"
        )
        videoExportRepository.update(running)

        runCatching {
            val exported = VideoStoryboardExporter.export(this, plan, aspectPreset)
            videoExportRepository.update(
                running.copy(
                    status = com.loveai.model.VideoExportStatus.COMPLETED,
                    outputPath = exported.file.absolutePath,
                    finishedAt = System.currentTimeMillis(),
                    note = "\u5df2\u751f\u6210 ${aspectPreset.label} \u00b7 ${exported.sceneCount} \u9875\u89c6\u9891\u811a\u672c\u5305"
                )
            )
            Toast.makeText(this, "\u89c6\u9891\u4efb\u52a1\u5df2\u521b\u5efa", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, VideoExportCenterActivity::class.java))
        }.onFailure {
            videoExportRepository.update(
                running.copy(
                    status = com.loveai.model.VideoExportStatus.FAILED,
                    finishedAt = System.currentTimeMillis(),
                    note = "\u89c6\u9891\u811a\u672c\u5305\u751f\u6210\u5931\u8d25"
                )
            )
            Toast.makeText(this, "\u89c6\u9891\u4efb\u52a1\u521b\u5efa\u5931\u8d25", Toast.LENGTH_SHORT).show()
        }
    }

    private fun previewCover(plan: LovePlan) {
        runCatching {
            val songName = MusicManager.getPlaylist().firstOrNull { it.key == plan.songKey }?.name
            val exported = PlanArtworkExporter.exportPoster(this, plan, songName)
            startActivity(
                Intent(this, CoverPreviewActivity::class.java).apply {
                    putExtra(CoverPreviewActivity.EXTRA_IMAGE_PATH, exported.file.absolutePath)
                    putExtra(CoverPreviewActivity.EXTRA_TITLE, "${plan.name} \u5c01\u9762\u9884\u89c8")
                }
            )
        }.onFailure {
            Toast.makeText(this, "\u5c01\u9762\u9884\u89c8\u5931\u8d25", Toast.LENGTH_SHORT).show()
        }
    }

    private class PlanAdapter(
        private val resolveSongName: (String?) -> String?,
        private val onOpen: (LovePlan) -> Unit,
        private val onEdit: (LovePlan) -> Unit,
        private val onNewVersion: (LovePlan) -> Unit,
        private val onDelete: (LovePlan) -> Unit,
        private val onExport: (LovePlan) -> Unit,
        private val onShare: (LovePlan) -> Unit,
        private val onPreviewCover: (LovePlan) -> Unit,
        private val onVideoTask: (LovePlan) -> Unit
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
            val tagsLabel = if (plan.tags.isEmpty()) {
                "\u672a\u8bbe\u7f6e\u6807\u7b7e"
            } else {
                plan.tags.joinToString("  #", prefix = "#")
            }

            holder.tvPlanTheme.text = themeLabel
            holder.tvPlanStatus.text = plan.status.label
            holder.tvPlanCoverTitle.text = plan.title
            holder.tvName.text = plan.name
            val assetCount = plan.pageTexts.count { !it.assetUri.isNullOrBlank() }
            holder.tvSummary.text =
                "${plan.effectTypes.size} \u4e2a\u7279\u6548 \u00b7 \u56fe\u7247 $assetCount \u5f20 \u00b7 ${songLabel}"
            holder.tvTags.text = tagsLabel
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
            holder.tvVersion.text = buildString {
                append("V")
                append(plan.currentVersion)
                append(" \u00b7 \u66f4\u65b0 ")
                append(formatter.format(Date(plan.updatedAt)))
                if (plan.publishedAt > 0L) {
                    append(" \u00b7 \u53d1\u5e03 ")
                    append(formatter.format(Date(plan.publishedAt)))
                }
            }

            holder.layoutPlanCover.background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                coverColors(theme, PlanCover.fromKey(plan.coverKey))
            ).apply {
                cornerRadius = 24f
            }

            holder.layoutPlanCover.setOnClickListener { onPreviewCover(plan) }
            holder.btnOpen.setOnClickListener { onOpen(plan) }
            holder.btnEdit.setOnClickListener { onEdit(plan) }
            holder.btnNewVersion.setOnClickListener { onNewVersion(plan) }
            holder.btnDelete.setOnClickListener { onDelete(plan) }
            holder.btnExport.setOnClickListener { onExport(plan) }
            holder.btnShare.setOnClickListener { onShare(plan) }
            holder.btnVideoTask.setOnClickListener { onVideoTask(plan) }
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val layoutPlanCover: LinearLayout = view.findViewById(R.id.layoutPlanCover)
            val tvPlanTheme: TextView = view.findViewById(R.id.tvPlanTheme)
            val tvPlanStatus: TextView = view.findViewById(R.id.tvPlanStatus)
            val tvPlanCoverTitle: TextView = view.findViewById(R.id.tvPlanCoverTitle)
            val tvName: TextView = view.findViewById(R.id.tvPlanName)
            val tvSummary: TextView = view.findViewById(R.id.tvPlanSummary)
            val tvTags: TextView = view.findViewById(R.id.tvPlanTags)
            val tvMeta: TextView = view.findViewById(R.id.tvPlanMeta)
            val tvVersion: TextView = view.findViewById(R.id.tvPlanVersion)
            val btnOpen: Button = view.findViewById(R.id.btnOpenPlan)
            val btnEdit: Button = view.findViewById(R.id.btnEditPlan)
            val btnNewVersion: Button = view.findViewById(R.id.btnNewVersionPlan)
            val btnDelete: Button = view.findViewById(R.id.btnDeletePlan)
            val btnExport: Button = view.findViewById(R.id.btnExportPlan)
            val btnShare: Button = view.findViewById(R.id.btnSharePlan)
            val btnVideoTask: Button = view.findViewById(R.id.btnVideoTask)
        }

        companion object {
            private fun coverColors(theme: PlanTheme?, cover: PlanCover?): IntArray {
                if (cover != null) {
                    return intArrayOf(
                        Color.parseColor(cover.startColor),
                        Color.parseColor(cover.endColor)
                    )
                }
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
