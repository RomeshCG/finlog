package com.example.finlog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.finlog.databinding.FragmentSupportBinding // Assuming you have view binding

class SupportFragment : Fragment() {

    private var _binding: FragmentSupportBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSupportBinding.inflate(inflater, container, false)

        // --- Add Your Support Content Here ---
        val supportContent = """
        Need help with FinLog?

        Frequently Asked Questions:

        Q: How do I add a new transaction?
        A: Tap the large '+' button on the bottom navigation bar.

        Q: How do I set my monthly budget?
        A: Navigate Home > Tap 'All Budgets' in the budget section.

        Q: How do I manage spending categories?
        A: Navigate Home > Tap 'Statistics' in the categories section.

        Q: How do backups work?
        A: Use the 'Create Backup' and 'Restore Backup' options in the Menu tab to save and load your data to internal storage.

        Contact Us:

        If you need further assistance or have feedback, please reach out:

        Email: support@finlogapp.example.com (Replace with actual email)
        Website: www.finlogapp.example.com (Replace with actual site)
        """

        binding.supportContentTextView.text = supportContent

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}