package com.example.finlog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finlog.databinding.FragmentRecordsBinding

class RecordsFragment : Fragment() {

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
        updateRecords()
    }

    override fun onResume() {
        super.onResume()
        updateRecords() // Refresh records when the fragment is resumed
    }

    private fun updateRecords() {
        val records = dataManager.getRecords().sortedByDescending { it.date }
        binding.recordsRecyclerView.adapter = RecordAdapter(records)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}