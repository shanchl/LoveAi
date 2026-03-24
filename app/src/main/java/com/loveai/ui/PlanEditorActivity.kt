package com.loveai.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.loveai.R
import com.loveai.manager.MusicManager
import com.loveai.model.EffectType
import com.loveai.model.PlanCover
import com.loveai.model.PlanPageText
import com.loveai.model.PlanStatus
import com.loveai.model.PlanTheme
import com.loveai.model.PlanVersion
import com.loveai.repository.PlanRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlanEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PLAN_ID = "plan_id"
    }

    private lateinit var etPlanName: EditText
    private lateinit var etTitle: EditText
    private lateinit var etSubtitle: EditText
    private lateinit var etTags: EditText
    private lateinit var spTheme: Spinner
    private lateinit var spCover: Spinner
    private lateinit var spSong: Spinner
    private lateinit var tvPlanStatusSummary: TextView
    private lateinit var tvVersionSummary: TextView
    private lateinit var tvThemeHint: TextView
    private lateinit var tvCoverHint: TextView
    private lateinit var tvVersionEmpty: TextView
    private lateinit var rvSelected: RecyclerView
    private lateinit var rvAvailable: RecyclerView
    private lateinit var rvPageTexts: RecyclerView
    private lateinit var rvVersions: RecyclerView
    private lateinit var btnSaveDraft: Button
    private lateinit var btnPublish: Button
    private lateinit var btnCancel: Button
    private lateinit var btnApplyTheme: Button
    private lateinit var btnImportSong: Button

    private lateinit var selectedAdapter: SelectedEffectsAdapter
    private lateinit var availableAdapter: AvailableEffectsAdapter
    private lateinit var pageTextAdapter: PageTextAdapter
    private lateinit var versionAdapter: VersionAdapter
    private lateinit var planRepository: PlanRepository

    private val selectedTypes = mutableListOf<EffectType>()
    private val pageTexts = mutableListOf<PlanPageText>()
    private val allTypes = EffectType.values().toList()
    private val themes = listOf<PlanTheme?>(null) + PlanTheme.values().toList()
    private val covers = listOf<PlanCover?>(null) + PlanCover.values().toList()
    private val minEffectCount = 5
    private val maxEffectCount = 8
    private var songs: List<MusicManager.Song> = emptyList()
    private var editingPlanId: String? = null
    private var currentStatus: PlanStatus = PlanStatus.DRAFT

    private val importSongLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        handleImportedSong(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan_editor)

        planRepository = PlanRepository(this)
        MusicManager.ensurePlaylist(this)

        initViews()
        initLists()
        initThemeSelector()
        initCoverSelector()
        initSongSelector()
        loadPlanIfNeeded()
    }

    private fun initViews() {
        etPlanName = findViewById(R.id.etPlanName)
        etTitle = findViewById(R.id.etTitle)
        etSubtitle = findViewById(R.id.etSubtitle)
        etTags = findViewById(R.id.etTags)
        spTheme = findViewById(R.id.spTheme)
        spCover = findViewById(R.id.spCover)
        spSong = findViewById(R.id.spSong)
        tvPlanStatusSummary = findViewById(R.id.tvPlanStatusSummary)
        tvVersionSummary = findViewById(R.id.tvVersionSummary)
        tvThemeHint = findViewById(R.id.tvThemeHint)
        tvCoverHint = findViewById(R.id.tvCoverHint)
        tvVersionEmpty = findViewById(R.id.tvVersionEmpty)
        rvSelected = findViewById(R.id.rvSelectedEffects)
        rvAvailable = findViewById(R.id.rvAvailableEffects)
        rvPageTexts = findViewById(R.id.rvPageTexts)
        rvVersions = findViewById(R.id.rvVersions)
        btnSaveDraft = findViewById(R.id.btnSaveDraft)
        btnPublish = findViewById(R.id.btnPublishPlan)
        btnCancel = findViewById(R.id.btnCancelEdit)
        btnApplyTheme = findViewById(R.id.btnApplyTheme)
        btnImportSong = findViewById(R.id.btnImportSong)

        btnSaveDraft.setOnClickListener { savePlan(PlanStatus.DRAFT, startPlayback = false) }
        btnPublish.setOnClickListener { savePlan(PlanStatus.PUBLISHED, startPlayback = true) }
        btnCancel.setOnClickListener { finish() }
        btnApplyTheme.setOnClickListener { applyCurrentTheme() }
        btnImportSong.setOnClickListener { importSongLauncher.launch(arrayOf("audio/*")) }

        val defaultTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                pageTextAdapter.notifyDataSetChanged()
            }
        }
        etTitle.addTextChangedListener(defaultTextWatcher)
        etSubtitle.addTextChangedListener(defaultTextWatcher)
    }

    private fun initThemeSelector() {
        val labels = themes.map { theme ->
            theme?.label ?: "\u4e0d\u4f7f\u7528\u6a21\u677f"
        }
        val themeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spTheme.adapter = themeAdapter
        spTheme.setSelection(0)
        spTheme.onItemSelectedListener = HintSelectedListener { updateThemeHint() }
        updateThemeHint()
    }

    private fun initCoverSelector() {
        val labels = covers.map { cover ->
            cover?.label ?: "\u8ddf\u968f\u4e3b\u9898\u9ed8\u8ba4"
        }
        val coverAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spCover.adapter = coverAdapter
        spCover.setSelection(0)
        spCover.onItemSelectedListener = HintSelectedListener { updateCoverHint() }
        updateCoverHint()
    }

    private fun initSongSelector(selectedSongKey: String? = null) {
        songs = listOf(
            MusicManager.Song(
                id = -1,
                key = "",
                name = "\u4e0d\u7ed1\u5b9a\uff08\u4f7f\u7528\u5f53\u524d\u64ad\u653e\u961f\u5217\uff09"
            )
        ) + MusicManager.getPlaylist()

        val songAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            songs.map { it.name }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spSong.adapter = songAdapter

        val songIndex = songs.indexOfFirst { it.key == (selectedSongKey ?: "") }
            .takeIf { it >= 0 } ?: 0
        spSong.setSelection(songIndex)
    }

    private fun initLists() {
        selectedAdapter = SelectedEffectsAdapter(
            onMoveUp = { position ->
                if (position > 0) {
                    selectedTypes.add(position - 1, selectedTypes.removeAt(position))
                    pageTexts.add(position - 1, pageTexts.removeAt(position))
                    refreshLists()
                }
            },
            onMoveDown = { position ->
                if (position < selectedTypes.lastIndex) {
                    selectedTypes.add(position + 1, selectedTypes.removeAt(position))
                    pageTexts.add(position + 1, pageTexts.removeAt(position))
                    refreshLists()
                }
            },
            onRemove = { position ->
                selectedTypes.removeAt(position)
                pageTexts.removeAt(position)
                refreshLists()
            }
        )

        availableAdapter = AvailableEffectsAdapter { type ->
            if (selectedTypes.size >= maxEffectCount) {
                Toast.makeText(
                    this,
                    "\u6700\u591a\u53ef\u9009 $maxEffectCount \u4e2a\u7279\u6548",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (type !in selectedTypes) {
                selectedTypes += type
                pageTexts += PlanPageText()
                refreshLists()
            }
        }

        pageTextAdapter = PageTextAdapter(
            getDefaultTitle = { etTitle.text.toString().trim() },
            getDefaultSubtitle = { etSubtitle.text.toString().trim() },
            onPageTextChanged = { index, title, subtitle ->
                if (index in pageTexts.indices) {
                    pageTexts[index] = PlanPageText(title = title, subtitle = subtitle)
                }
            }
        )

        versionAdapter = VersionAdapter { version ->
            restoreVersion(version)
        }

        rvSelected.layoutManager = LinearLayoutManager(this)
        rvSelected.adapter = selectedAdapter
        rvSelected.isNestedScrollingEnabled = false

        rvAvailable.layoutManager = LinearLayoutManager(this)
        rvAvailable.adapter = availableAdapter
        rvAvailable.isNestedScrollingEnabled = true

        rvPageTexts.layoutManager = LinearLayoutManager(this)
        rvPageTexts.adapter = pageTextAdapter
        rvPageTexts.isNestedScrollingEnabled = false

        rvVersions.layoutManager = LinearLayoutManager(this)
        rvVersions.adapter = versionAdapter
        rvVersions.isNestedScrollingEnabled = false

        refreshLists()
        updateEditorSummary()
    }

    private fun loadPlanIfNeeded() {
        editingPlanId = intent.getStringExtra(EXTRA_PLAN_ID)
        val plan = editingPlanId?.let { planRepository.getPlanById(it) } ?: return

        etPlanName.setText(plan.name)
        etTitle.setText(plan.title)
        etSubtitle.setText(plan.subtitle)
        etTags.setText(plan.tags.joinToString("\uff0c"))
        currentStatus = plan.status

        selectedTypes.clear()
        selectedTypes.addAll(plan.effectTypes)
        pageTexts.clear()
        pageTexts.addAll(plan.pageTexts)
        while (pageTexts.size < selectedTypes.size) {
            pageTexts += PlanPageText()
        }
        refreshLists()

        val themeIndex = themes.indexOfFirst { it?.key == plan.themeKey }.takeIf { it >= 0 } ?: 0
        spTheme.setSelection(themeIndex)
        updateThemeHint()

        val coverIndex = covers.indexOfFirst { it?.key == plan.coverKey }.takeIf { it >= 0 } ?: 0
        spCover.setSelection(coverIndex)
        updateCoverHint()

        initSongSelector(plan.songKey)
        loadVersionHistory()
        updateEditorSummary(plan)
    }

    private fun applyCurrentTheme() {
        val theme = getSelectedTheme() ?: run {
            Toast.makeText(this, "\u8bf7\u5148\u9009\u62e9\u4e00\u4e2a\u4e3b\u9898\u6a21\u677f", Toast.LENGTH_SHORT).show()
            return
        }

        if (etPlanName.text.isNullOrBlank()) {
            etPlanName.setText(theme.defaultPlanName)
        }
        etTitle.setText(theme.defaultTitle)
        etSubtitle.setText(theme.defaultSubtitle)
        if (etTags.text.isNullOrBlank()) {
            etTags.setText(theme.label)
        }

        selectedTypes.clear()
        selectedTypes.addAll(theme.recommendedEffects)
        pageTexts.clear()
        repeat(selectedTypes.size) { pageTexts += PlanPageText() }

        val suggestedCover = when (theme) {
            PlanTheme.CONFESSION -> PlanCover.BLUSH
            PlanTheme.ANNIVERSARY -> PlanCover.BLOOM
            PlanTheme.BIRTHDAY -> PlanCover.SUNSET
            PlanTheme.LONG_DISTANCE -> PlanCover.OCEAN
        }
        val coverIndex = covers.indexOfFirst { it == suggestedCover }.takeIf { it >= 0 } ?: 0
        spCover.setSelection(coverIndex)

        refreshLists()
        updateThemeHint()
        updateCoverHint()
    }

    private fun getSelectedTheme(): PlanTheme? = themes.getOrNull(spTheme.selectedItemPosition)

    private fun getSelectedCover(): PlanCover? = covers.getOrNull(spCover.selectedItemPosition)

    private fun getSelectedSongKey(): String? {
        val song = songs.getOrNull(spSong.selectedItemPosition) ?: return null
        return song.key.ifBlank { null }
    }

    private fun refreshLists() {
        while (pageTexts.size < selectedTypes.size) {
            pageTexts += PlanPageText()
        }
        while (pageTexts.size > selectedTypes.size) {
            pageTexts.removeAt(pageTexts.lastIndex)
        }

        selectedAdapter.submitList(selectedTypes.toList())
        availableAdapter.submitList(
            allTypes,
            selectedTypes.toSet(),
            selectedTypes.size >= maxEffectCount
        )
        pageTextAdapter.submitList(selectedTypes.toList(), pageTexts.toList())
    }

    private fun updateEditorSummary(plan: com.loveai.model.LovePlan? = null) {
        val statusLabel = currentStatus.label
        tvPlanStatusSummary.text = if (editingPlanId == null) {
            "\u65b0\u65b9\u6848\u00b7\u5f53\u524d\u72b6\u6001\uff1a$statusLabel"
        } else {
            "\u7f16\u8f91\u4e2d\u00b7\u5f53\u524d\u72b6\u6001\uff1a$statusLabel"
        }

        val activePlan = plan ?: editingPlanId?.let { planRepository.getPlanById(it) }
        tvVersionSummary.text = if (activePlan == null) {
            "\u9996\u6b21\u4fdd\u5b58\u540e\u4f1a\u751f\u6210 V1 \u7248\u672c\u5feb\u7167"
        } else {
            val publishedLabel = if (activePlan.publishedAt > 0L) {
                "\u00b7 \u53d1\u5e03\u4e8e ${formatTime(activePlan.publishedAt)}"
            } else {
                ""
            }
            "\u5f53\u524d V${activePlan.currentVersion} \u00b7 \u66f4\u65b0\u4e8e ${formatTime(activePlan.updatedAt)} $publishedLabel".trim()
        }
    }

    private fun loadVersionHistory() {
        val planId = editingPlanId
        if (planId.isNullOrBlank()) {
            versionAdapter.submitList(emptyList())
            tvVersionEmpty.visibility = View.VISIBLE
            rvVersions.visibility = View.GONE
            return
        }
        val versions = planRepository.getPlanVersions(planId)
        versionAdapter.submitList(versions)
        tvVersionEmpty.visibility = if (versions.isEmpty()) View.VISIBLE else View.GONE
        rvVersions.visibility = if (versions.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun restoreVersion(version: PlanVersion) {
        etPlanName.setText(version.name)
        etTitle.setText(version.title)
        etSubtitle.setText(version.subtitle)
        etTags.setText(version.tags.joinToString("\uff0c"))
        currentStatus = version.status

        selectedTypes.clear()
        selectedTypes.addAll(version.effectTypes)
        pageTexts.clear()
        pageTexts.addAll(version.pageTexts)
        while (pageTexts.size < selectedTypes.size) {
            pageTexts += PlanPageText()
        }

        val themeIndex = themes.indexOfFirst { it?.key == version.themeKey }.takeIf { it >= 0 } ?: 0
        val coverIndex = covers.indexOfFirst { it?.key == version.coverKey }.takeIf { it >= 0 } ?: 0
        spTheme.setSelection(themeIndex)
        spCover.setSelection(coverIndex)
        initSongSelector(version.songKey)
        refreshLists()
        updateThemeHint()
        updateCoverHint()
        updateEditorSummary()
        Toast.makeText(this, "\u5df2\u5c06 V${version.version} \u6062\u590d\u5230\u7f16\u8f91\u533a", Toast.LENGTH_SHORT).show()
    }

    private fun updateThemeHint() {
        val theme = getSelectedTheme()
        tvThemeHint.text = if (theme == null) {
            "\u4e0d\u5957\u7528\u9884\u8bbe\uff0c\u4f60\u53ef\u4ee5\u81ea\u7531\u7f16\u6392\u6587\u6848\u548c\u7279\u6548\u3002"
        } else {
            "${theme.label} \u00b7 ${theme.description}"
        }
    }

    private fun updateCoverHint() {
        val cover = getSelectedCover()
        tvCoverHint.text = if (cover == null) {
            "\u4e0d\u5355\u72ec\u6307\u5b9a\u5c01\u9762\u65f6\uff0c\u65b9\u6848\u5e93\u4f1a\u6839\u636e\u4e3b\u9898\u81ea\u52a8\u751f\u6210\u5c01\u9762\u914d\u8272\u3002"
        } else {
            "${cover.label} \u00b7 ${cover.description}"
        }
    }

    private fun parseTags(): List<String> {
        return etTags.text.toString()
            .split(',', '\uff0c')
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun savePlan(status: PlanStatus, startPlayback: Boolean) {
        currentFocus?.clearFocus()
        if (selectedTypes.size !in minEffectCount..maxEffectCount) {
            Toast.makeText(
                this,
                "\u8bf7\u9009\u62e9 $minEffectCount \u5230 $maxEffectCount \u4e2a\u7279\u6548",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        currentStatus = status
        val plan = planRepository.savePlan(
            name = etPlanName.text.toString().trim(),
            title = etTitle.text.toString().trim(),
            subtitle = etSubtitle.text.toString().trim(),
            effectTypes = selectedTypes,
            pageTexts = pageTexts.toList(),
            themeKey = getSelectedTheme()?.key,
            coverKey = getSelectedCover()?.key,
            tags = parseTags(),
            songKey = getSelectedSongKey(),
            status = status,
            existingId = editingPlanId
        )
        editingPlanId = plan.id
        loadVersionHistory()
        updateEditorSummary(plan)

        if (startPlayback) {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_PLAN_ID, plan.id)
                }
            )
            finish()
        } else {
            Toast.makeText(this, "\u8349\u7a3f\u5df2\u4fdd\u5b58", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleImportedSong(uri: Uri) {
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, takeFlags)
        val songName = resolveDisplayName(uri) ?: "\u672c\u5730\u97f3\u4e50"
        val key = MusicManager.registerExternalSong(this, uri, songName)
        initSongSelector(key)
        Toast.makeText(this, "\u5df2\u5bfc\u5165\u97f3\u4e50\uff1a$songName", Toast.LENGTH_SHORT).show()
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
        }
    }

    private fun formatTime(time: Long): String {
        return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(time))
    }

    private class SelectedEffectsAdapter(
        private val onMoveUp: (Int) -> Unit,
        private val onMoveDown: (Int) -> Unit,
        private val onRemove: (Int) -> Unit
    ) : RecyclerView.Adapter<SelectedEffectsAdapter.ViewHolder>() {

        private var items: List<EffectType> = emptyList()

        fun submitList(newItems: List<EffectType>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_selected_effect, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = "${position + 1}. ${effectLabel(item)}"
            holder.btnUp.isEnabled = position > 0
            holder.btnDown.isEnabled = position < items.lastIndex
            holder.btnUp.setOnClickListener { onMoveUp(position) }
            holder.btnDown.setOnClickListener { onMoveDown(position) }
            holder.btnRemove.setOnClickListener { onRemove(position) }
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvEffectName)
            val btnUp: Button = view.findViewById(R.id.btnMoveUp)
            val btnDown: Button = view.findViewById(R.id.btnMoveDown)
            val btnRemove: Button = view.findViewById(R.id.btnRemoveEffect)
        }
    }

    private class AvailableEffectsAdapter(
        private val onAdd: (EffectType) -> Unit
    ) : RecyclerView.Adapter<AvailableEffectsAdapter.ViewHolder>() {

        private var items: List<EffectType> = emptyList()
        private var selectedSet: Set<EffectType> = emptySet()
        private var full = false

        fun submitList(newItems: List<EffectType>, selected: Set<EffectType>, isFull: Boolean) {
            items = newItems
            selectedSet = selected
            full = isFull
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_available_effect, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val selected = item in selectedSet
            holder.tvName.text = effectLabel(item)
            holder.btnAdd.isEnabled = !selected && !full
            holder.btnAdd.text = if (selected) "\u5df2\u9009" else "\u6dfb\u52a0"
            holder.btnAdd.setOnClickListener { onAdd(item) }
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvAvailableEffectName)
            val btnAdd: Button = view.findViewById(R.id.btnAddEffect)
        }
    }

    private class PageTextAdapter(
        private val getDefaultTitle: () -> String,
        private val getDefaultSubtitle: () -> String,
        private val onPageTextChanged: (Int, String, String) -> Unit
    ) : RecyclerView.Adapter<PageTextAdapter.ViewHolder>() {

        private var types: List<EffectType> = emptyList()
        private var items: List<PlanPageText> = emptyList()

        fun submitList(newTypes: List<EffectType>, newItems: List<PlanPageText>) {
            types = newTypes
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_page_text, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(position, types[position], items.getOrNull(position) ?: PlanPageText())
        }

        override fun getItemCount(): Int = minOf(types.size, items.size)

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvPageLabel: TextView = view.findViewById(R.id.tvPageLabel)
            private val etPageTitle: EditText = view.findViewById(R.id.etPageTitle)
            private val etPageSubtitle: EditText = view.findViewById(R.id.etPageSubtitle)

            fun bind(position: Int, type: EffectType, pageText: PlanPageText) {
                tvPageLabel.text = "\u7b2c ${position + 1} \u9875\u00b7${effectLabel(type)}"

                etPageTitle.setOnFocusChangeListener(null)
                etPageSubtitle.setOnFocusChangeListener(null)
                etPageTitle.setText(pageText.title)
                etPageSubtitle.setText(pageText.subtitle)
                etPageTitle.hint = if (getDefaultTitle().isBlank()) {
                    "\u8be5\u9875\u4e3b\u6807\u9898\uff08\u53ef\u9009\uff09"
                } else {
                    "\u9ed8\u8ba4\uff1a${getDefaultTitle()}"
                }
                etPageSubtitle.hint = if (getDefaultSubtitle().isBlank()) {
                    "\u8be5\u9875\u526f\u6807\u9898\uff08\u53ef\u9009\uff09"
                } else {
                    "\u9ed8\u8ba4\uff1a${getDefaultSubtitle()}"
                }

                val saveBack = {
                    onPageTextChanged(
                        position,
                        etPageTitle.text.toString().trim(),
                        etPageSubtitle.text.toString().trim()
                    )
                }
                etPageTitle.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) saveBack() }
                etPageSubtitle.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) saveBack() }
            }
        }
    }

    private class VersionAdapter(
        private val onRestore: (PlanVersion) -> Unit
    ) : RecyclerView.Adapter<VersionAdapter.ViewHolder>() {

        private var items: List<PlanVersion> = emptyList()
        private val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

        fun submitList(newItems: List<PlanVersion>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_plan_version, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTitle.text = "V${item.version} \u00b7 ${item.status.label}"
            holder.tvMeta.text = "${item.note} \u00b7 ${formatter.format(Date(item.savedAt))}"
            holder.tvSummary.text = "${item.effectTypes.size} \u9875\u00b7${item.name}"
            holder.btnRestore.setOnClickListener { onRestore(item) }
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tvVersionTitle)
            val tvMeta: TextView = view.findViewById(R.id.tvVersionMeta)
            val tvSummary: TextView = view.findViewById(R.id.tvVersionSummary)
            val btnRestore: Button = view.findViewById(R.id.btnRestoreVersion)
        }
    }
}

private fun effectLabel(type: EffectType): String {
    return when (type) {
        EffectType.HEART_RAIN -> "\u7231\u5fc3\u96e8"
        EffectType.FIREWORK -> "\u70df\u82b1"
        EffectType.STARRY_SKY -> "\u661f\u7a7a"
        EffectType.PETAL_FALL -> "\u82b1\u74e3\u96e8"
        EffectType.BUBBLE_FLOAT -> "\u6ce1\u6ce1\u6f02\u6d6e"
        EffectType.TYPEWRITER -> "\u6253\u5b57\u673a"
        EffectType.HEART_PULSE -> "\u5fc3\u8df3"
        EffectType.RIPPLE -> "\u6d9f\u6f2a"
        EffectType.SNOW_FALL -> "\u96ea\u82b1"
        EffectType.METEOR_SHOWER -> "\u6d41\u661f\u96e8"
        EffectType.BUTTERFLY -> "\u8774\u8776"
        EffectType.AURORA -> "\u6781\u5149"
    }
}

private class HintSelectedListener(
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
