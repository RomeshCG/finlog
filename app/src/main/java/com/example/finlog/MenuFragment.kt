package com.example.finlog

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.io.File

class MenuFragment : Fragment() {

    private lateinit var dataManager: DataManager
    private lateinit var spinnerBackups: Spinner
    private var backupFiles: List<String> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_menu, container, false)

        dataManager = DataManager(requireContext())

        val btnBackup: Button = view.findViewById(R.id.btn_backup)
        val btnRestore: Button = view.findViewById(R.id.btn_restore)
        val btnSupport: Button = view.findViewById(R.id.btn_support)
        val btnPrivacyPolicy: Button = view.findViewById(R.id.btn_privacy_policy)
        spinnerBackups = view.findViewById(R.id.spinner_backups)

        // Load backup files into the spinner
        refreshBackupList()

        btnBackup.setOnClickListener {
            try {
                val backupFileName = dataManager.createBackup()
                val backupFile = File(requireContext().filesDir, backupFileName)
                if (backupFile.exists()) {
                    Log.d("BackupVerification", "Backup file created at: ${backupFile.absolutePath}")
                    Log.d("BackupVerification", "Backup file contents: ${backupFile.readText()}")
                    Toast.makeText(
                        requireContext(),
                        "Backup created: ${dataManager.getBackupTimestamp(backupFileName)}",
                        Toast.LENGTH_SHORT
                    ).show()
                    refreshBackupList() // Refresh the spinner after creating a new backup
                } else {
                    Log.e("BackupVerification", "Backup file not found after creation")
                    Toast.makeText(
                        requireContext(),
                        "Backup creation failed: File not found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("BackupVerification", "Backup creation failed: ${e.message}")
                Toast.makeText(
                    requireContext(),
                    "Failed to create backup: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnRestore.setOnClickListener {
            if (backupFiles.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "No backups available to restore",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            try {
                val selectedDisplayName = spinnerBackups.selectedItem.toString()
                // Extract the actual filename from the backupFiles list based on the selected position
                val selectedBackup = backupFiles[spinnerBackups.selectedItemPosition]
                // Verify the file exists before attempting to restore
                val backupFile = File(requireContext().filesDir, selectedBackup)
                if (!backupFile.exists()) {
                    Log.e("RestoreVerification", "Selected backup file does not exist: $selectedBackup")
                    Toast.makeText(
                        requireContext(),
                        "Failed to restore backup: Backup file not found: $selectedBackup",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val recordsBefore = dataManager.getRecords()
                val cardsBefore = dataManager.getCards()
                Log.d("RestoreVerification", "Records before restore: $recordsBefore")
                Log.d("RestoreVerification", "Cards before restore: $cardsBefore")

                dataManager.restoreBackup(selectedBackup)

                val recordsAfter = dataManager.getRecords()
                val cardsAfter = dataManager.getCards()
                Log.d("RestoreVerification", "Records after restore: $recordsAfter")
                Log.d("RestoreVerification", "Cards after restore: $cardsAfter")

                Toast.makeText(
                    requireContext(),
                    "Backup restored from $selectedDisplayName",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.e("RestoreVerification", "Restore failed: ${e.message}")
                Toast.makeText(
                    requireContext(),
                    "Failed to restore backup: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnSupport.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SupportFragment())
                .addToBackStack(null)
                .commit()
        }

        btnPrivacyPolicy.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PrivacyPolicyFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    private fun refreshBackupList() {
        backupFiles = dataManager.getBackupFiles()
        if (backupFiles.isEmpty()) {
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                listOf("No backups available")
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerBackups.adapter = adapter
        } else {
            val displayNames = backupFiles.map { "Backup from ${dataManager.getBackupTimestamp(it)}" }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                displayNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerBackups.adapter = adapter
        }
    }
}