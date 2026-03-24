package com.loveai.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.loveai.R
import com.loveai.model.GenerationTone
import com.loveai.model.PlanTheme
import com.loveai.repository.SmartPlanGenerator

class SmartGenerateActivity : AppCompatActivity() {

    private lateinit var spTheme: Spinner
    private lateinit var spTone: Spinner
    private lateinit var spCount: Spinner
    private lateinit var etKeywords: EditText
    private lateinit var etRelation: EditText
    private lateinit var tvPreview: TextView
    private lateinit var btnGenerate: Button
    private lateinit var btnPreview: Button
    private lateinit var btnBack: Button

    private val themes = listOf<PlanTheme?>(null) + PlanTheme.values().toList()
    private val tones = GenerationTone.values().toList()
    private val pageCounts = listOf(5, 6, 7, 8)
    private val generator = SmartPlanGenerator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_smart_generate)

        spTheme = findViewById(R.id.spSmartTheme)
        spTone = findViewById(R.id.spSmartTone)
        spCount = findViewById(R.id.spSmartCount)
        etKeywords = findViewById(R.id.etSmartKeywords)
        etRelation = findViewById(R.id.etSmartRelation)
        tvPreview = findViewById(R.id.tvSmartPreview)
        btnGenerate = findViewById(R.id.btnGeneratePlan)
        btnPreview = findViewById(R.id.btnPreviewGenerate)
        btnBack = findViewById(R.id.btnBackSmart)

        spTheme.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            themes.map { it?.label ?: "\u81ea\u5b9a\u4e49\u4e3b\u9898" }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spTone.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            tones.map { "${it.label} \u00b7 ${it.description}" }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spCount.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            pageCounts.map { "$it \u9875" }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spCount.setSelection(pageCounts.lastIndex)

        btnBack.setOnClickListener { finish() }
        btnPreview.setOnClickListener { renderPreview() }
        btnGenerate.setOnClickListener { generateAndOpenEditor() }
    }

    private fun renderPreview() {
        val draft = buildDraft() ?: return
        tvPreview.text = buildString {
            append(draft.name)
            append("\n")
            append(draft.title)
            append("\n")
            append(draft.subtitle)
            append("\n\n")
            append("\u6807\u7b7e\uff1a")
            append(draft.tags.joinToString(" / "))
            append("\n")
            append("\u9875\u6570\uff1a")
            append(draft.effectTypes.size)
            append(" \u9875")
        }
    }

    private fun generateAndOpenEditor() {
        val draft = buildDraft() ?: return
        startActivity(
            Intent(this, PlanEditorActivity::class.java).apply {
                putExtra(PlanEditorActivity.EXTRA_GENERATED_NAME, draft.name)
                putExtra(PlanEditorActivity.EXTRA_GENERATED_TITLE, draft.title)
                putExtra(PlanEditorActivity.EXTRA_GENERATED_SUBTITLE, draft.subtitle)
                putExtra(PlanEditorActivity.EXTRA_GENERATED_THEME_KEY, draft.themeKey)
                putExtra(PlanEditorActivity.EXTRA_GENERATED_COVER_KEY, draft.coverKey)
                putStringArrayListExtra(
                    PlanEditorActivity.EXTRA_GENERATED_TAGS,
                    ArrayList(draft.tags)
                )
                putStringArrayListExtra(
                    PlanEditorActivity.EXTRA_GENERATED_EFFECTS,
                    ArrayList(draft.effectTypes.map { it.name })
                )
                putStringArrayListExtra(
                    PlanEditorActivity.EXTRA_GENERATED_PAGE_TITLES,
                    ArrayList(draft.pageTexts.map { it.title })
                )
                putStringArrayListExtra(
                    PlanEditorActivity.EXTRA_GENERATED_PAGE_SUBTITLES,
                    ArrayList(draft.pageTexts.map { it.subtitle })
                )
            }
        )
    }

    private fun buildDraft() = runCatching {
        generator.generate(
            theme = themes.getOrNull(spTheme.selectedItemPosition),
            tone = tones.getOrElse(spTone.selectedItemPosition) { GenerationTone.SWEET },
            keywords = etKeywords.text.toString(),
            relation = etRelation.text.toString(),
            pageCount = pageCounts.getOrElse(spCount.selectedItemPosition) { 8 }
        )
    }.getOrElse {
        Toast.makeText(this, "\u751f\u6210\u5931\u8d25", Toast.LENGTH_SHORT).show()
        null
    }
}
