package com.example.myapplication.ui.detail

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemPropertyBinding

data class Property(
    val key: String,
    val value: String,
    val isBoolean: Boolean = false,
    val boolValue: Boolean = false
)

class PropertyAdapter(
    initItems: List<Property>,
    private val onClick: ((Property) -> Unit)? = null,
    private val onToggle: ((Property, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<PropertyAdapter.ViewHolder>() {

    private val items: MutableList<Property> = initItems.toMutableList()

    fun updateItems(newItems: List<Property>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPropertyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size



    inner class ViewHolder(private val binding: ItemPropertyBinding) : RecyclerView.ViewHolder(binding.root) {
        private var currentProp: Property? = null

        init {
            // Use touch listener instead of OnCheckedChangeListener to prevent programmatic triggers
            binding.switchToggle.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    currentProp?.let { prop ->
                        binding.root.post {
                            val checked = binding.switchToggle.isChecked
                            onToggle?.invoke(prop, checked)
                        }
                    }
                }
                false // let the switch handle the touch normally
            }
        }

        fun bind(prop: Property) {
            currentProp = prop
            binding.textKey.text = prop.key
            binding.textValue.text = prop.value

            if (prop.isBoolean) {
                binding.textValue.visibility = android.view.View.GONE
                binding.switchToggle.visibility = android.view.View.VISIBLE
                binding.switchToggle.isChecked = prop.boolValue
            } else {
                binding.textValue.visibility = android.view.View.VISIBLE
                binding.switchToggle.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onClick?.invoke(prop) }
        }
    }
}
