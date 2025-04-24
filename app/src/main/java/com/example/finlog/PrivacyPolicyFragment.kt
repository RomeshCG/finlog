package com.example.finlog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.finlog.databinding.FragmentPrivacyPolicyBinding // Assuming view binding

class PrivacyPolicyFragment : Fragment() {

    private var _binding: FragmentPrivacyPolicyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrivacyPolicyBinding.inflate(inflater, container, false)


        val privacyPolicyContent = """
        Privacy Policy for FinLog

        Last Updated: [Insert Date]

        1. Information We Collect:
           - FinLog stores all financial data (records, accounts, cards, budgets, categories) locally on your device using SharedPreferences.
           - We do not collect, transmit, or store any of your personal or financial data on external servers.
           - Backup files are created and stored locally in the app's private directory on your device.

        2. How We Use Information:
           - Data is used solely within the app for tracking finances, generating reports (future feature), and providing budget insights.
           - Local backups allow you to restore your data on the same device.

        3. Data Security:
           - Data is stored in the app's private internal storage, protected by Android's security model.
           - We recommend securing your device with a screen lock.
           - Backup files are not encrypted by default.

        4. Data Sharing:
           - We do not share your financial data with any third parties.

        5. Permissions:
           - Storage Permissions (READ/WRITE_EXTERNAL_STORAGE - potentially legacy): May be requested for backup/restore functionality, although internal storage is preferred and often doesn't require explicit permission on newer Android versions.
           - Notifications (POST_NOTIFICATIONS): Required on Android 13+ to show budget alerts.

        6. Changes to This Policy:
           - We may update this policy. We will notify you of any changes by posting the new policy in the app.

        7. Contact Us:
           - If you have questions, contact us at privacy@finlogapp.example.com (Replace with actual email).
        """

        binding.privacyPolicyContentTextView.text = privacyPolicyContent // Assuming TextView ID is privacy_policy_content_text_view
        // Add this TextView to fragment_privacy_policy.xml if needed.

        return binding.root
    }

     override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}