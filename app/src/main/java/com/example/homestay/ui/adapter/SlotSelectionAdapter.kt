package com.example.homestay.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.homestay.R
import com.example.homestay.data.entity.Slot
import java.text.NumberFormat
import java.util.Locale

class SlotSelectionAdapter(
    private val onSlotSelected: (Slot?) -> Unit
) : ListAdapter<Slot, SlotSelectionAdapter.SlotViewHolder>(SlotDiffCallback()) {

    private var selectedSlotId: Long? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_slot_selection, parent, false)
        return SlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSlotName: TextView = itemView.findViewById(R.id.tv_slot_name)
        private val tvSlotNumber: TextView = itemView.findViewById(R.id.tv_slot_number)
        private val tvSlotPrice: TextView = itemView.findViewById(R.id.tv_slot_price)
        private val tvSlotStatus: TextView = itemView.findViewById(R.id.tv_slot_status)
        private val imgSelected: ImageView = itemView.findViewById(R.id.img_selected)
        private val cardView: com.google.android.material.card.MaterialCardView = itemView as com.google.android.material.card.MaterialCardView

        fun bind(slot: Slot) {
            tvSlotName.text = slot.slotName
            tvSlotNumber.text = "Slot #${slot.slotNumber}"

            // Hiển thị giá slot nếu có
            if (slot.price != null && slot.price > 0) {
                val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
                val formattedPrice = formatter.format(slot.price.toLong())
                tvSlotPrice.text = "$formattedPrice đ / đêm"
                tvSlotPrice.visibility = View.VISIBLE
            } else {
                tvSlotPrice.visibility = View.GONE
            }

            // Hiển thị trạng thái
            if (slot.isAvailable) {
                tvSlotStatus.text = "Có sẵn"
                tvSlotStatus.setTextColor(itemView.context.getColor(R.color.home_primary))
                itemView.isEnabled = true
                itemView.alpha = 1.0f
            } else {
                tvSlotStatus.text = "Đã đặt"
                tvSlotStatus.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
                itemView.isEnabled = false
                itemView.alpha = 0.5f
            }

            // Highlight nếu được chọn
            val isSelected = selectedSlotId == slot.id
            imgSelected.visibility = if (isSelected) View.VISIBLE else View.GONE
            if (isSelected) {
                cardView.strokeColor = itemView.context.getColor(R.color.home_primary)
            } else {
                cardView.strokeColor = itemView.context.getColor(R.color.home_divider)
            }

            // Click listener
            itemView.setOnClickListener {
                if (slot.isAvailable) {
                    if (selectedSlotId == slot.id) {
                        // Deselect
                        selectedSlotId = null
                        onSlotSelected(null)
                    } else {
                        // Select
                        selectedSlotId = slot.id
                        onSlotSelected(slot)
                    }
                    notifyDataSetChanged()
                }
            }
        }
    }

    class SlotDiffCallback : DiffUtil.ItemCallback<Slot>() {
        override fun areItemsTheSame(oldItem: Slot, newItem: Slot): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Slot, newItem: Slot): Boolean {
            return oldItem == newItem
        }
    }

    fun getSelectedSlotId(): Long? = selectedSlotId

    fun clearSelection() {
        selectedSlotId = null
        notifyDataSetChanged()
    }
}

