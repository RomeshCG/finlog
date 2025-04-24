package com.example.finlog

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.finlog.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize DataManager
        dataManager = DataManager(requireContext())

        // Display initial data
        updateUI()

        // Handle account dropdown click
        binding.accountDropdown.setOnClickListener {
            showAccountSelectionDialog()
        }

        // Handle account item clicks
        binding.accountTotal.setOnClickListener {
            dataManager.setSelectedAccount(dataManager.getAccounts()[0]) // Total
            updateUI()
        }
        binding.accountCreditCard.setOnClickListener {
            dataManager.setSelectedAccount(dataManager.getAccounts()[1]) // Credit Card
            updateUI()
        }
        binding.accountDebitCard.setOnClickListener {
            dataManager.setSelectedAccount(dataManager.getAccounts()[2]) // Debit Card
            updateUI()
        }
        binding.accountCash.setOnClickListener {
            dataManager.setSelectedAccount(dataManager.getAccounts()[3]) // Cash
            updateUI()
        }

        // Handle menu icon click (placeholder)
        binding.menuIcon.setOnClickListener {
            // Add menu functionality later
        }

        // Handle "All Budgets" click (placeholder)
        binding.allBudgets.setOnClickListener {
            // Add navigation to all budgets screen later
        }

        // Handle "Statistics" click (placeholder)
        binding.statistics.setOnClickListener {
            // Add navigation to statistics screen later
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        // Update date
        binding.dateText.text = dataManager.getCurrentDate()

        // Update total balance
        val selectedAccount = dataManager.getSelectedAccount()
        binding.totalBalance.text = "$${String.format("%.2f", selectedAccount.balance)}"

        // Update budget section
        val budgetLeft = dataManager.getBudgetLeft()
        val budgetSpent = dataManager.getTotalBudgetSpent()
        val budgetAllocated = dataManager.getTotalBudgetAllocated()
        binding.budgetLeft.text = "$${String.format("%.2f", budgetLeft)} left"
        binding.budgetSpent.text = "$${String.format("%.2f", budgetSpent)} spent"

        // Update budget categories
        val categories = dataManager.getBudgetCategories()
        binding.entertainmentSpent.text = "$${String.format("%.2f", categories[0].spent)}"
        binding.foodSpent.text = "$${String.format("%.2f", categories[1].spent)}"
        binding.fuelSpent.text = "$${String.format("%.2f", categories[2].spent)}"

        // Update expense chart
        binding.expenseChart.text = "Expense\n$${String.format("%.2f", budgetSpent)}"
        val expenseBreakdown = categories.joinToString("\n") { category ->
            "${category.name}: $${String.format("%.2f", category.spent)}"
        }
        binding.expenseBreakdown.text = expenseBreakdown

        // Update last records
        val records = dataManager.getLastRecords()
        if (records.isNotEmpty()) {
            binding.record1Date.text = records[0].date
            binding.record1Category.text = records[0].category
            binding.record1Amount.text = "$${String.format("%.2f", records[0].amount)}"
            binding.record1Amount.setTextColor(
                if (records[0].amount >= 0) 0xFF5AF2A2.toInt() else 0xFFFF5252.toInt()
            )
        }
        if (records.size > 1) {
            binding.record2Date.text = records[1].date
            binding.record2Category.text = records[1].category
            binding.record2Amount.text = "$${String.format("%.2f", records[1].amount)}"
            binding.record2Amount.setTextColor(
                if (records[1].amount >= 0) 0xFF5AF2A2.toInt() else 0xFFFF5252.toInt()
            )
        }
        if (records.size > 2) {
            binding.record3Date.text = records[2].date
            binding.record3Category.text = records[2].category
            binding.record3Amount.text = "$${String.format("%.2f", records[2].amount)}"
            binding.record3Amount.setTextColor(
                if (records[2].amount >= 0) 0xFF5AF2A2.toInt() else 0xFFFF5252.toInt()
            )
        }

        // Update account list
        val accounts = dataManager.getAccounts()
        binding.accountTotalName.text = accounts[0].name
        binding.accountTotalBalance.text = "$${String.format("%.2f", accounts[0].balance)}"
        binding.accountCreditCardName.text = accounts[1].name
        binding.accountCreditCardBalance.text = "$${String.format("%.2f", accounts[1].balance)}"
        binding.accountDebitCardName.text = accounts[2].name
        binding.accountDebitCardBalance.text = "$${String.format("%.2f", accounts[2].balance)}"
        binding.accountCashName.text = accounts[3].name
        binding.accountCashBalance.text = "$${String.format("%.2f", accounts[3].balance)}"
    }

    private fun showAccountSelectionDialog() {
        val accounts = dataManager.getAccounts()
        val accountNames = accounts.map { it.name }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Select Account")
            .setItems(accountNames) { _, which ->
                dataManager.setSelectedAccount(accounts[which])
                updateUI()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}