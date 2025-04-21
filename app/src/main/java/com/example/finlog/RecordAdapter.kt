package com.example.finlog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.finlog.databinding.ItemRecordBinding

class RecordAdapter(
    private val records: List<Record>,
    private val listener: OnRecordClickListener
) : RecyclerView.Adapter<RecordAdapter.RecordViewHolder>() {

    interface OnRecordClickListener {
        fun onRecordClick(record: Record, position: Int)
        fun onRecordEdit(record: Record, position: Int)
        fun onRecordDelete(record: Record, position: Int)
    }

    class RecordViewHolder(
        private val binding: ItemRecordBinding,
        private val listener: OnRecordClickListener
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: Record, position: Int) {
            binding.recordDate.text = record.date
            binding.recordCategory.text = record.category
            binding.recordAmount.text = "$${String.format("%.2f", record.amount)}"

            // Handle display for transfers
            if (record.type == "Transfer") {
                binding.recordAccountDetails.visibility = android.view.View.VISIBLE
                binding.recordAccountDetails.text = "${record.account} → ${record.toAccount}"
                binding.recordAmount.setTextColor(0xFF5555FF.toInt()) // Blue for transfers
            } else {
                binding.recordAccountDetails.visibility = android.view.View.GONE
                binding.recordAmount.setTextColor(
                    if (record.amount >= 0) 0xFF55FF55.toInt() else 0xFFFF5555.toInt()
                )
            }

            // Click listener for the entire item (to edit)
            binding.root.setOnClickListener {
                listener.onRecordEdit(record, position)
            }

            // Long click to delete
            binding.root.setOnLongClickListener {
                listener.onRecordDelete(record, position)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecordViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(records[position], position)
    }

    override fun getItemCount(): Int = records.size
}