package com.loveai.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.loveai.R
import com.loveai.model.EffectType
import com.loveai.repository.PlanRepository

class PlanEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PLAN_ID = "plan_id"
    }

    private lateinit var etPlanName: EditText
    private lateinit var etTitle: EditText
    private lateinit var etSubtitle: EditText
    private lateinit var rvSelected: RecyclerView
    private lateinit var rvAvailable: RecyclerView
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private lateinit var selectedAdapter: SelectedEffectsAdapter
    private lateinit var availableAdapter: AvailableEffectsAdapter
    private lateinit var planRepository: PlanRepository

    private val selectedTypes = mutableListOf<EffectType>()
    private val allTypes = EffectType.values().toList()
    private var editingPlanId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan_editor)

        planRepository = PlanRepository(this)
        initViews()
        initLists()
        loadPlanIfNeeded()
    }

    private fun initViews() {
        etPlanName = findViewById(R.id.etPlanName)
        etTitle = findViewById(R.id.etTitle)
        etSubtitle = findViewById(R.id.etSubtitle)
        rvSelected = findViewById(R.id.rvSelectedEffects)
        rvAvailable = findViewById(R.id.rvAvailableEffects)
        btnSave = findViewById(R.id.btnSavePlan)
        btnCancel = findViewById(R.id.btnCancelEdit)

        btnSave.setOnClickListener { savePlan() }
        btnCancel.setOnClickListener { finish() }
    }

    private fun initLists() {
        selectedAdapter = SelectedEffectsAdapter(
            onMoveUp = { position ->
                if (position > 0) {
                    selectedTypes.add(position - 1, selectedTypes.removeAt(position))
                    refreshLists()
                }
            },
            onMoveDown = { position ->
                if (position < selectedTypes.lastIndex) {
                    selectedTypes.add(position + 1, selectedTypes.removeAt(position))
                    refreshLists()
                }
            },
            onRemove = { position ->
                selectedTypes.removeAt(position)
                refreshLists()
            }
        )

        availableAdapter = AvailableEffectsAdapter { type ->
            if (selectedTypes.size >= 8) {
                Toast.makeText(this, "\u6700\u591a\u53ea\u80fd\u9009\u62e9 8 \u4e2a\u7279\u6548", Toast.LENGTH_SHORT).show()
            } else if (type !in selectedTypes) {
                selectedTypes += type
                refreshLists()
            }
        }

        rvSelected.layoutManager = LinearLayoutManager(this)
        rvSelected.adapter = selectedAdapter
        rvAvailable.layoutManager = LinearLayoutManager(this)
        rvAvailable.adapter = availableAdapter
        refreshLists()
    }

    private fun loadPlanIfNeeded() {
        editingPlanId = intent.getStringExtra(EXTRA_PLAN_ID)
        val plan = editingPlanId?.let { planRepository.getPlanById(it) } ?: return
        etPlanName.setText(plan.name)
        etTitle.setText(plan.title)
        etSubtitle.setText(plan.subtitle)
        selectedTypes.clear()
        selectedTypes.addAll(plan.effectTypes)
        refreshLists()
    }

    private fun refreshLists() {
        selectedAdapter.submitList(selectedTypes.toList())
        availableAdapter.submitList(allTypes, selectedTypes.toSet(), selectedTypes.size >= 8)
    }

    private fun savePlan() {
        if (selectedTypes.size != 8) {
            Toast.makeText(this, "\u8bf7\u521a\u597d\u9009\u62e9 8 \u4e2a\u7279\u6548", Toast.LENGTH_SHORT).show()
            return
        }

        val plan = planRepository.savePlan(
            name = etPlanName.text.toString().trim(),
            title = etTitle.text.toString().trim(),
            subtitle = etSubtitle.text.toString().trim(),
            effectTypes = selectedTypes,
            existingId = editingPlanId
        )

        startActivity(
            Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_PLAN_ID, plan.id)
            }
        )
        finish()
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
