package com.example.finlog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.finlog.databinding.ItemRecordBinding

class RecordAdapter(private val records: List<Record>) : RecyclerView.Adapter<RecordAdapter.RecordViewHolder>() {

    class RecordViewHolder(private val binding: ItemRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: Record) {
            binding.recordDate.text = record.date
            binding.recordCategory.text = record.category
            binding.recordAmount.text = "$${String.format("%.2f", record.amount)}"

            // Handle display for transfers
            if (record.type == "Transfer") {
                binding.recordAccountDetails.visibility = View.VISIBLE
                binding.recordAccountDetails.text = "${record.account} → ${record.toAccount}"
                binding.recordAmount.setTextColor(0xFF5555FF.toInt()) // Blue for transfers
            } else {
                binding.recordAccountDetails.visibility = View.GONE
                binding.recordAmount.setTextColor(
                    if (record.amount >= 0) 0xFF55FF55.toInt() else 0xFFFF5555.toInt()
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size
}