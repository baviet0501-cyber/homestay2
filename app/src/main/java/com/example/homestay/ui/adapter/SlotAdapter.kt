package com.example.homestay.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.homestay.R
import com.example.homestay.data.entity.Slot

class SlotAdapter : ListAdapter<Slot, SlotAdapter.SlotViewHolder>(SlotDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_slot, parent, false)
        return SlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSlotName: TextView = itemView.findViewById(R.id.tv_slot_name)
        private val tvSlotNumber: TextView = itemView.findViewById(R.id.tv_slot_number)
        private val tvSlotStatus: TextView = itemView.findViewById(R.id.tv_slot_status)

        fun bind(slot: Slot) {
            tvSlotName.text = slot.slotName
            tvSlotNumber.text = "Slot #${slot.slotNumber}"
            
            if (slot.isAvailable) {
                tvSlotStatus.text = "Có sẵn"
                tvSlotStatus.setTextColor(itemView.context.getColor(R.color.home_primary))
            } else {
                tvSlotStatus.text = "Đã đặt"
                tvSlotStatus.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
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
}

