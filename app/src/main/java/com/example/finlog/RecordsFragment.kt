package com.example.finlog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finlog.databinding.FragmentRecordsBinding

class RecordsFragment : Fragment(), RecordAdapter.OnRecordClickListener {

    private var _binding: FragmentRecordsBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize DataManager
        dataManager = DataManager(requireContext())

        // Setup RecyclerView
        binding.recordsRecyclerView.layoutManager = LinearLayoutManager(context)
        loadRecords()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this fragment
        loadRecords()
    }

    private fun loadRecords() {
        // Get records from DataManager
        val records = dataManager.getRecords().sortedByDescending { it.date }

        // Update empty state visibility
        binding.emptyState.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
        binding.recordsRecyclerView.visibility = if (records.isEmpty()) View.GONE else View.VISIBLE

        // Update RecyclerView
        binding.recordsRecyclerView.adapter = RecordAdapter(records, this)
    }

    override fun onRecordClick(record: Record, position: Int) {
        // Not used in this case, but can be implemented for viewing details
    }

    override fun onRecordEdit(record: Record, position: Int) {
        // Navigate to AddRecordFragment for editing
        val fragment = AddRecordFragment.newInstance(record)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onRecordDelete(record: Record, position: Int) {
        showDeleteConfirmationDialog(record, position)
    }

    private fun showDeleteConfirmationDialog(record: Record, position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_record)
            .setMessage(R.string.delete_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteRecord(record, position)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteRecord(record: Record, position: Int) {
        val records = dataManager.getRecords().toMutableList()
        records.removeAt(position)
        dataManager.saveRecords(records) // We'll add this method to DataManager
        loadRecords()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}